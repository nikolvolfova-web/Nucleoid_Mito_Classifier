import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * Nucleoid_Mito_Classifier_v0_1c
 *
 * Fiji/ImageJ IJ1 plugin.
 *
 * Biological / analytical rule:
 *   - C1 nucleoid ROIs are detected independently of the mitochondrial mask.
 *   - Whole C1 ROIs are used for morphology and C1 intensity measurements.
 *   - The mitochondrial C3 mask is generated from C2 and used only for classification
 *     by fractional overlap with the full C1 ROI.
 *   - C3 must never clip, trim, split, or otherwise alter C1 ROI geometry.
 *
 * Dependencies at runtime:
 *   - Fiji/ImageJ
 *   - StarDist Fiji plugin, if StarDist-based C1 detection is used
 *   - Auto Local Threshold plugin, only if LOCAL_PHANSALKAR fallback is enabled and needed
 *
 * Install/test:
 *   Fiji > Plugins > New > Plugin, paste this code, save as Nucleoid_Mito_Classifier_v0_1c.java
 *   or copy this file into Fiji.app/plugins/ and run Plugins > Compile and Run...
 */
public class Nucleoid_Mito_Classifier_v0_1c implements PlugIn {

    private static final Locale CSV_LOCALE = Locale.US;

    static class Settings {
        String inputDir;
        String outputDir;

        // C3 mitochondrial mask from C2
        double mitoRolling = 20;
        double mitoSigma = 0.7;
        double mitoOutlierRadius = 1.5;
        double mitoOutlierThreshold = 50;

        double mitoMaskMinGoodPercent = 0.20;
        double mitoMaskMaxGoodPercent = 15.00;
        double mitoLargestComponentMaxPct = 70.00;

        double mitoStrictRolling = 12;
        double mitoStrictSigma = 0.4;
        String mitoStrictMethod = "Triangle";
        boolean mitoStrictDoOpen = false;

        boolean useMitoLocalPhansalkarFallback = true;
        double mitoLocalRolling = 12;
        double mitoLocalSigma = 0.4;
        int mitoLocalRadius = 15;

        double mitoSoftRolling = 35;
        double mitoSoftSigma = 0.7;
        String mitoSoftMethod = "Otsu";

        int mitoDilationsForClassification = 0;

        // classification thresholds
        double maxMitoOverlapPctForOut = 5;
        double minMitoOverlapPctForColoc = 20;

        // C1 preprocessing for StarDist detection only
        double c1Rolling = 8;
        double c1Sigma = 0.05;

        // StarDist parameters
        double stardistPercentileBottom = 1.0;
        double stardistPercentileTop = 99.8;
        double stardistProbThresh = 0.25;
        double stardistNmsThresh = 0.4;
        int stardistExcludeBoundary = 0;

        // object filters
        double minC1Max = 0;
        double minRoiAreaPx = 0;
        double maxRoiAreaPx = 999999999;

        // QC drawing
        int decisionQCLineWidth = 2;
        int borderlineIDFontSize = 10;

        boolean debugMode = false;
    }

    static class MitoMaskQC {
        double maskAreaPct;
        double largestComponentPct;
        int objectCount;
        int maskPixelCount;
    }

    static class MitoMaskResult {
        ImagePlus mask;
        String methodUsed;
        String qcStatus;
        String warning;
        MitoMaskQC qc;
    }

    static class RoiManagerState {
        boolean existed;
        boolean visible;
        Roi[] rois;
    }

    @Override
    public void run(String arg) {
        Settings s = askSettings();
        if (s == null) return;

        File inDir = new File(s.inputDir);
        File outDir = new File(s.outputDir);
        if (!outDir.exists() && !outDir.mkdirs()) {
            IJ.error("Cannot create output folder:\n" + outDir.getAbsolutePath());
            return;
        }

        File[] files = inDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".tif") || lower.endsWith(".tiff"))) return false;
            return !isDerivedOutputName(name);
        });
        if (files == null || files.length == 0) {
            IJ.error("No input .tif/.tiff images found in:\n" + inDir.getAbsolutePath());
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));

        IJ.log("\\Clear");
        IJ.log("Nucleoid_Mito_Classifier_v0_1c");
        IJ.log("Input: " + inDir.getAbsolutePath());
        IJ.log("Output: " + outDir.getAbsolutePath());
        IJ.log("Files: " + files.length);

        File summary = new File(outDir, "Summary_AllImages_ColocBorderOut.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(summary, false))) {
            pw.println("Image,All_C1_Objects,Colocalized,Borderline,OutOfMito,Filtered,C3_MaskAreaPct,C3_LargestComponentPct,C3_ObjectCount,MitoMask_MethodUsed,MitoMask_QCStatus,MitoMask_Warning,Colocalized_FractionPct,Borderline_FractionPct,OutOfMito_FractionPct");
        } catch (IOException e) {
            IJ.error("Cannot write summary CSV:\n" + e.getMessage());
            return;
        }

        // Note: Fiji/ImageJ Java plugin execution does not require macro batch mode.
        // IJ.setBatchMode(boolean) is not available in some ImageJ 1.x builds, so we avoid it here.

        int processed = 0;
        for (File f : files) {
            RoiManagerState roiManagerState = captureRoiManagerState();
            try {
                processOneImage(f, outDir, s, summary);
                processed++;
            } catch (Throwable t) {
                IJ.log("ERROR while processing " + f.getName() + ": " + t.getMessage());
                t.printStackTrace();
            } finally {
                restoreRoiManagerState(roiManagerState);
            }
        }

        IJ.log("DONE. Processed images: " + processed + " / " + files.length);
    }

    private Settings askSettings() {
        DirectoryChooser dc = new DirectoryChooser("Input folder with original 2-channel TIF images: C1=nucleoids, C2=mitochondria");
        String input = dc.getDirectory();
        if (input == null) return null;

        DirectoryChooser oc = new DirectoryChooser("Output folder for CSV and QC TIFF files");
        String output = oc.getDirectory();
        if (output == null) return null;

        Settings s = new Settings();
        s.inputDir = input;
        s.outputDir = output;

        GenericDialog gd = new GenericDialog("Nucleoid Mito Classifier v0.1c");
        gd.addMessage("Core rule: C3 mask classifies full C1 nucleoid ROIs only. C3 never clips C1 morphology.");

        gd.addMessage("C3 mitochondrial mask from C2");
        gd.addNumericField("mitoRolling", s.mitoRolling, 1);
        gd.addNumericField("mitoSigma", s.mitoSigma, 2);
        gd.addNumericField("mitoOutlierRadius", s.mitoOutlierRadius, 1);
        gd.addNumericField("mitoOutlierThreshold", s.mitoOutlierThreshold, 1);
        gd.addNumericField("mitoMaskMinGoodPercent", s.mitoMaskMinGoodPercent, 2);
        gd.addNumericField("mitoMaskMaxGoodPercent", s.mitoMaskMaxGoodPercent, 2);
        gd.addNumericField("mitoLargestComponentMaxPct", s.mitoLargestComponentMaxPct, 2);
        gd.addCheckbox("useMitoLocalPhansalkarFallback", s.useMitoLocalPhansalkarFallback);
        gd.addNumericField("mitoDilationsForClassification", s.mitoDilationsForClassification, 0);

        gd.addMessage("C1 StarDist detection");
        gd.addNumericField("c1Rolling", s.c1Rolling, 1);
        gd.addNumericField("c1Sigma", s.c1Sigma, 2);
        gd.addNumericField("stardistPercentileBottom", s.stardistPercentileBottom, 1);
        gd.addNumericField("stardistPercentileTop", s.stardistPercentileTop, 1);
        gd.addNumericField("stardistProbThresh", s.stardistProbThresh, 3);
        gd.addNumericField("stardistNmsThresh", s.stardistNmsThresh, 3);
        gd.addNumericField("stardistExcludeBoundary", s.stardistExcludeBoundary, 0);

        gd.addMessage("Object classification and filters");
        gd.addNumericField("maxMitoOverlapPctForOut", s.maxMitoOverlapPctForOut, 1);
        gd.addNumericField("minMitoOverlapPctForColoc", s.minMitoOverlapPctForColoc, 1);
        gd.addNumericField("minRoiArea_px", s.minRoiAreaPx, 1);
        gd.addNumericField("maxRoiArea_px", s.maxRoiAreaPx, 1);
        gd.addNumericField("minC1Max", s.minC1Max, 1);
        gd.addCheckbox("debugMode_show_windows", s.debugMode);
        gd.showDialog();
        if (gd.wasCanceled()) return null;

        s.mitoRolling = gd.getNextNumber();
        s.mitoSigma = gd.getNextNumber();
        s.mitoOutlierRadius = gd.getNextNumber();
        s.mitoOutlierThreshold = gd.getNextNumber();
        s.mitoMaskMinGoodPercent = gd.getNextNumber();
        s.mitoMaskMaxGoodPercent = gd.getNextNumber();
        s.mitoLargestComponentMaxPct = gd.getNextNumber();
        s.useMitoLocalPhansalkarFallback = gd.getNextBoolean();
        s.mitoDilationsForClassification = (int) Math.round(gd.getNextNumber());

        s.c1Rolling = gd.getNextNumber();
        s.c1Sigma = gd.getNextNumber();
        s.stardistPercentileBottom = gd.getNextNumber();
        s.stardistPercentileTop = gd.getNextNumber();
        s.stardistProbThresh = gd.getNextNumber();
        s.stardistNmsThresh = gd.getNextNumber();
        s.stardistExcludeBoundary = (int) Math.round(gd.getNextNumber());

        s.maxMitoOverlapPctForOut = gd.getNextNumber();
        s.minMitoOverlapPctForColoc = gd.getNextNumber();
        s.minRoiAreaPx = gd.getNextNumber();
        s.maxRoiAreaPx = gd.getNextNumber();
        s.minC1Max = gd.getNextNumber();
        s.debugMode = gd.getNextBoolean();

        if (s.maxMitoOverlapPctForOut >= s.minMitoOverlapPctForColoc) {
            IJ.error("Threshold error", "maxMitoOverlapPctForOut must be smaller than minMitoOverlapPctForColoc.");
            return null;
        }
        return s;
    }

    private void processOneImage(File inputFile, File outDir, Settings s, File summaryCsv) throws Exception {
        String base = stripExtension(inputFile.getName());
        IJ.log("----------------------------------------");
        IJ.log("Processing: " + inputFile.getName());

        ImagePlus input = IJ.openImage(inputFile.getAbsolutePath());
        if (input == null) {
            IJ.log("SKIPPED - cannot open image: " + inputFile.getName());
            return;
        }
        int channels = input.getNChannels();
        int slices = input.getNSlices();
        int frames = input.getNFrames();

        if (channels != 2 || slices != 1 || frames != 1) {
            IJ.log(
                    "SKIPPED - expected exactly 2 channels, 1 Z slice, and 1 time point; found " +
                    "channels=" + channels +
                    ", slices=" + slices +
                    ", frames=" + frames +
                    " in " + inputFile.getName()
            );
            closeImage(input);
            return;
        }

        int width = input.getWidth();
        int height = input.getHeight();
        Calibration cal = input.getCalibration();
        double pixelWidth = cal.pixelWidth;
        double pixelHeight = cal.pixelHeight;
        String unit = cal.getUnit();

        String normalizedUnit = unit == null
                ? ""
                : unit.trim().toLowerCase(Locale.ROOT);

        boolean unitIsMicrometres =
                normalizedUnit.equals("µm") ||
                normalizedUnit.equals("μm") ||
                normalizedUnit.equals("um") ||
                normalizedUnit.equals("micron") ||
                normalizedUnit.equals("microns") ||
                normalizedUnit.equals("micrometer") ||
                normalizedUnit.equals("micrometers") ||
                normalizedUnit.equals("micrometre") ||
                normalizedUnit.equals("micrometres");

        boolean pixelSizeIsValid =
                pixelWidth > 0.0 &&
                pixelHeight > 0.0 &&
                !Double.isNaN(pixelWidth) &&
                !Double.isNaN(pixelHeight) &&
                !Double.isInfinite(pixelWidth) &&
                !Double.isInfinite(pixelHeight);

        if (!unitIsMicrometres || !pixelSizeIsValid) {
            IJ.log(
                    "SKIPPED - expected spatial calibration in micrometres with valid pixel dimensions; found " +
                    "PixelWidth=" + pixelWidth +
                    ", PixelHeight=" + pixelHeight +
                    ", Unit=" + unit +
                    " in " + inputFile.getName()
            );
            closeImage(input);
            return;
        }

        RoiManager rm = RoiManager.getInstance2();
        if (rm == null) rm = new RoiManager(false);
        rm.reset();

        ImagePlus[] ch = ChannelSplitter.split(input);
        ImagePlus c1 = ch[0];
        ImagePlus c2 = ch[1];
        c1.setTitle(uniqueTitle(base + "_NUCLEOIDS_ORIG"));
        c2.setTitle(uniqueTitle(base + "_MITOS_RAW"));
        c1.setCalibration(cal);
        c2.setCalibration(cal);
        closeImage(input);

        if (s.debugMode) {
            c1.show();
            c2.show();
        }

        // Save raw QC copies before analysis changes.
        ImagePlus c1qc = new Duplicator().run(c1);
        c1qc.setTitle(base + "-QC_C1_NUCLEOIDS_ORIG");
        IJ.run(c1qc, "Enhance Contrast...", "saturated=0.35");
        saveTiff(c1qc, new File(outDir, base + "-QC_C1_NUCLEOIDS_ORIG.tif"));
        closeImage(c1qc);

        ImagePlus c2qc = new Duplicator().run(c2);
        c2qc.setTitle(base + "-QC_C2_MITOS_RAW");
        IJ.run(c2qc, "Enhance Contrast...", "saturated=0.35");
        saveTiff(c2qc, new File(outDir, base + "-QC_C2_MITOS_RAW.tif"));
        closeImage(c2qc);

        // C3 mask generation from C2.
        MitoMaskResult mito = generateMitoMask(c2, s, base);
        mito.mask.setCalibration(cal);
        ImagePlus c3qc = asBinaryDuplicate(mito.mask, base + "-QC_C3_MITOS_THRESH");
        saveTiff(c3qc, new File(outDir, base + "-QC_C3_MITOS_THRESH.tif"));
        closeImage(c3qc);
        writeMitoQC(outDir, base, mito, s);

        // Classification mask = duplicate of final C3 mask, optionally dilated.
        ImagePlus c3class = new Duplicator().run(mito.mask);
        c3class.setTitle(uniqueTitle(base + "_C3_CLASSIFICATION_MASK"));
        c3class.setCalibration(cal);
        for (int d = 0; d < s.mitoDilationsForClassification; d++) {
            c3class.getProcessor().dilate();
        }

        // C1 preprocessing for detection only.
        ImagePlus c1prep = new Duplicator().run(c1);
        c1prep.setTitle(uniqueTitle(base + "_NUCLEOIDS_FOR_STARDIST"));
        c1prep.setCalibration(cal);
        if (s.c1Rolling > 0) IJ.run(c1prep, "Subtract Background...", "rolling=" + fmt(s.c1Rolling) + " sliding");
        if (s.c1Sigma > 0) IJ.run(c1prep, "Gaussian Blur...", "sigma=" + fmt(s.c1Sigma));
        ImagePlus c1prepQC = new Duplicator().run(c1prep);
        c1prepQC.setTitle(base + "-QC_C1_FOR_STARDIST_HIGH_SENSITIVITY");
        saveTiff(c1prepQC, new File(outDir, base + "-QC_C1_FOR_STARDIST_HIGH_SENSITIVITY.tif"));
        closeImage(c1prepQC);

        ImageStatistics prepStats = c1prep.getStatistics(Measurements.MIN_MAX | Measurements.STD_DEV);
        IJ.log("C1 StarDist input max = " + prepStats.max);
        IJ.log("C1 StarDist input std = " + prepStats.stdDev);
        IJ.log("C1 detection: rolling=" + s.c1Rolling + ", sigma=" + s.c1Sigma + ", StarDist prob=" + s.stardistProbThresh + ", nms=" + s.stardistNmsThresh + ", top=" + s.stardistPercentileTop);

        ImagePlus colocMask = new ImagePlus("COLOCALIZED_OBJECT_MASK", new ByteProcessor(width, height));
        ImagePlus borderMask = new ImagePlus("BORDERLINE_OBJECT_MASK", new ByteProcessor(width, height));
        ImagePlus outMask = new ImagePlus("OUT_OF_MITO_OBJECT_MASK", new ByteProcessor(width, height));
        ImagePlus filtMask = new ImagePlus("FILTERED_OBJECT_MASK", new ByteProcessor(width, height));
        colocMask.setCalibration(cal);
        borderMask.setCalibration(cal);
        outMask.setCalibration(cal);
        filtMask.setCalibration(cal);

        int allCount = 0;
        int colocCount = 0;
        int borderCount = 0;
        int outCount = 0;
        int filteredCount = 0;

        int[] imageIdsBeforeStarDist = snapshotOpenImageIds();
        if (prepStats.max <= 0 || prepStats.stdDev <= 0) {
            IJ.log("SKIPPED StarDist - C1 image is empty or flat.");
        } else {
            rm.reset();
            if (c1prep.getWindow() == null) c1prep.show(); // StarDist needs an open image title as input.
            runStarDist(c1prep, s);
        }

        Roi[] rois = new Roi[0];
        rm = RoiManager.getInstance2();
        if (rm != null) rois = rm.getRoisAsArray();
        allCount = rois.length;
        IJ.log("All C1 objects detected = " + allCount);

        ImagePlus labels = findNewImageByTitlePrefix(imageIdsBeforeStarDist, "Label Image");
        if (labels != null) {
            labels.setTitle("ALL_C1_LABELS");
            saveTiff(labels, new File(outDir, base + "-AllC1_LabelImage.tif"));
        } else {
            ImagePlus emptyLabels = new ImagePlus("ALL_C1_LABELS", new ByteProcessor(width, height));
            emptyLabels.setCalibration(cal);
            saveTiff(emptyLabels, new File(outDir, base + "-AllC1_LabelImage.tif"));
            closeImage(emptyLabels);
        }

        ResultsTable rt = new ResultsTable();
        int measurementFlags = Measurements.AREA | Measurements.MEAN | Measurements.STD_DEV | Measurements.MIN_MAX |
                Measurements.INTEGRATED_DENSITY | Measurements.CENTROID | Measurements.CENTER_OF_MASS |
                Measurements.PERIMETER | Measurements.RECT | Measurements.ELLIPSE |
                Measurements.SHAPE_DESCRIPTORS | Measurements.FERET;
        Analyzer analyzer = new Analyzer(c1, measurementFlags, rt);

        ColorProcessor decision = makeDecisionQCBase(c1, c3class, s.decisionQCLineWidth);
        ByteProcessor c3bp = (ByteProcessor) c3class.getProcessor().convertToByte(false);

        for (int i = 0; i < allCount; i++) {
            Roi roi = rois[i];
            int areaPx = countRoiPixels(roi, width, height);
            int overlapPx = countOverlapPixels(roi, c3bp, width, height);
            double overlapPct = areaPx > 0 ? 100.0 * overlapPx / areaPx : 0.0;
            double nonOverlapPct = 100.0 - overlapPct;
            double overlapAreaUm2 = overlapPx * pixelWidth * pixelHeight;
            double areaUm2 = areaPx * pixelWidth * pixelHeight;

            ImageStatistics c1Stats = statsUnderRoi(c1, roi);
            ImageStatistics c2Stats = statsUnderRoi(c2, roi);

            int classCode;
            String className;
            int isColoc = 0, isBorder = 0, isOut = 0, isFiltered = 0;
            boolean passesFilters = true;
            if (areaPx < s.minRoiAreaPx) passesFilters = false;
            if (areaPx > s.maxRoiAreaPx) passesFilters = false;
            if (c1Stats.max < s.minC1Max) passesFilters = false;

            if (!passesFilters) {
                classCode = 9;
                className = "FILTERED";
                isFiltered = 1;
                filteredCount++;
                fillRoi((ByteProcessor) filtMask.getProcessor(), roi, width, height, 255);
                drawRoi(decision, roi, Color.MAGENTA, s.decisionQCLineWidth);
            } else if (overlapPct >= s.minMitoOverlapPctForColoc) {
                classCode = 1;
                className = "COLOCALIZED";
                isColoc = 1;
                colocCount++;
                fillRoi((ByteProcessor) colocMask.getProcessor(), roi, width, height, 255);
                drawRoi(decision, roi, Color.YELLOW, s.decisionQCLineWidth);
            } else if (overlapPct <= s.maxMitoOverlapPctForOut) {
                classCode = 3;
                className = "OUT_OF_MITO";
                isOut = 1;
                outCount++;
                fillRoi((ByteProcessor) outMask.getProcessor(), roi, width, height, 255);
                drawRoi(decision, roi, Color.GREEN, s.decisionQCLineWidth);
            } else {
                classCode = 2;
                className = "BORDERLINE";
                isBorder = 1;
                borderCount++;
                fillRoi((ByteProcessor) borderMask.getProcessor(), roi, width, height, 255);
                drawRoi(decision, roi, Color.CYAN, s.decisionQCLineWidth);
                drawBorderlineId(decision, roi, i + 1, s.borderlineIDFontSize);
            }

            c1.setRoi(roi);
            analyzer.measure();
            int row = rt.getCounter() - 1;
            rt.setValue("Image", row, base);
            rt.setValue("Object_ID_AllC1", row, i + 1);
            rt.setValue("Class", row, className);
            rt.setValue("ClassCode", row, classCode);
            rt.setValue("Is_Colocalized", row, isColoc);
            rt.setValue("Is_Borderline", row, isBorder);
            rt.setValue("Is_OutOfMito", row, isOut);
            rt.setValue("Is_Filtered", row, isFiltered);
            rt.setValue("Area_px_manual", row, areaPx);
            rt.setValue("Area_um2_manual", row, areaUm2);
            rt.setValue("C3_MitoMask_OverlapPct", row, overlapPct);
            rt.setValue("C3_MitoMask_NonOverlapPct", row, nonOverlapPct);
            rt.setValue("C3_MitoMask_OverlapArea_px", row, overlapPx);
            rt.setValue("C3_MitoMask_OverlapArea_um2", row, overlapAreaUm2);
            rt.setValue("C1_NUCLEOIDS_Mean_manual", row, c1Stats.mean);
            rt.setValue("C1_NUCLEOIDS_StdDev_manual", row, c1Stats.stdDev);
            rt.setValue("C1_NUCLEOIDS_Min_manual", row, c1Stats.min);
            rt.setValue("C1_NUCLEOIDS_Max_manual", row, c1Stats.max);
            rt.setValue("C2_MITOS_RAW_Mean", row, c2Stats.mean);
            rt.setValue("C2_MITOS_RAW_StdDev", row, c2Stats.stdDev);
            rt.setValue("C2_MITOS_RAW_Min", row, c2Stats.min);
            rt.setValue("C2_MITOS_RAW_Max", row, c2Stats.max);
            rt.setValue("Threshold_Out_MaxOverlapPct", row, s.maxMitoOverlapPctForOut);
            rt.setValue("Threshold_Coloc_MinOverlapPct", row, s.minMitoOverlapPctForColoc);
            rt.setValue("MitoMask_MethodUsed", row, mito.methodUsed);
            rt.setValue("MitoMask_QCStatus", row, mito.qcStatus);
            rt.setValue("MitoMask_Warning", row, mito.warning);
            rt.setValue("MitoMask_AreaPct", row, mito.qc.maskAreaPct);
            rt.setValue("MitoMask_LargestComponentPct", row, mito.qc.largestComponentPct);
            rt.setValue("MitoMask_ObjectCount", row, mito.qc.objectCount);
            rt.setValue("PixelWidth", row, pixelWidth);
            rt.setValue("PixelHeight", row, pixelHeight);
            rt.setValue("Unit", row, unit);
            rt.setValue("PluginVersion", row, "v0.1c");
        }
        c1.deleteRoi();
        c2.deleteRoi();

        if (rt.getCounter() > 0) {
            rt.save(new File(outDir, base + "-AllC1_ColocBorderOut_Morphology.csv").getAbsolutePath());
        } else {
            IJ.log("No C1 objects detected; writing header-only per-object CSV.");
            writeEmptyResults(outDir, base);
        }

        saveTiff(colocMask, new File(outDir, base + "-Colocalized_ObjectMask.tif"));
        saveTiff(borderMask, new File(outDir, base + "-Borderline_ObjectMask.tif"));
        saveTiff(outMask, new File(outDir, base + "-OutOfMito_ObjectMask.tif"));
        saveTiff(filtMask, new File(outDir, base + "-Filtered_ObjectMask.tif"));

        ImagePlus decisionImp = new ImagePlus("DecisionQC_C1gray_C3red_ClassOutlines", decision);
        decisionImp.setCalibration(cal);
        saveTiff(decisionImp, new File(outDir, base + "-DecisionQC_C1gray_C3red_ClassOutlines.tif"));

        double colocFraction = allCount > 0 ? 100.0 * colocCount / allCount : 0.0;
        double borderFraction = allCount > 0 ? 100.0 * borderCount / allCount : 0.0;
        double outFraction = allCount > 0 ? 100.0 * outCount / allCount : 0.0;
        try (PrintWriter pw = new PrintWriter(new FileWriter(summaryCsv, true))) {
            pw.println(csv(base) + "," + allCount + "," + colocCount + "," + borderCount + "," + outCount + "," + filteredCount + "," +
                    fmt(mito.qc.maskAreaPct) + "," + fmt(mito.qc.largestComponentPct) + "," + mito.qc.objectCount + "," +
                    csv(mito.methodUsed) + "," + csv(mito.qcStatus) + "," + csv(mito.warning) + "," +
                    fmt(colocFraction) + "," + fmt(borderFraction) + "," + fmt(outFraction));
        }

        writeSettingsLog(outDir, base, s, pixelWidth, pixelHeight, unit);
        IJ.log("COLOCALIZED objects = " + colocCount);
        IJ.log("BORDERLINE objects = " + borderCount);
        IJ.log("OUT_OF_MITO objects = " + outCount);
        IJ.log("Filtered objects = " + filteredCount);

        // Keep the diagnostic input and label windows open only in debug mode.
        if (!s.debugMode) {
            closeImage(c1);
            closeImage(c2);
            closeImage(c1prep);
            if (labels != null) closeImage(labels);
        }

        closeImage(mito.mask);
        closeImage(c3class);
        closeImage(colocMask);
        closeImage(borderMask);
        closeImage(outMask);
        closeImage(filtMask);
        closeImage(decisionImp);
        if (rm != null) rm.reset();
    }

    private MitoMaskResult generateMitoMask(ImagePlus c2Raw, Settings s, String base) {
        MitoMaskResult result = new MitoMaskResult();
        result.methodUsed = "STANDARD_BG20_TRIANGLE";
        result.mask = createGlobalThresholdMask(c2Raw, base + "_MITOS_THRESH", s.mitoRolling, s.mitoSigma, "Triangle", false, s);
        result.qc = evaluateMask(result.mask, s);
        result.qcStatus = classifyInitialQC(result.qc, s, "STANDARD");
        result.warning = "";
        IJ.log("C3 QC candidate A: area=" + fmt(result.qc.maskAreaPct) + "%, largestComponent=" + fmt(result.qc.largestComponentPct) + "%, objects=" + result.qc.objectCount + ", status=" + result.qcStatus);

        if ("OVERSEGMENTED_STANDARD".equals(result.qcStatus)) {
            IJ.log("C3 QC: oversegmented standard mask, trying STRICT_BG_TRIANGLE fallback.");
            closeImage(result.mask);
            result.methodUsed = "STRICT_BG12_TRIANGLE";
            result.mask = createGlobalThresholdMask(c2Raw, base + "_MITOS_THRESH", s.mitoStrictRolling, s.mitoStrictSigma, s.mitoStrictMethod, s.mitoStrictDoOpen, s);
            result.qc = evaluateMask(result.mask, s);
            if (isMaskQCGood(result.qc, s)) {
                result.qcStatus = "RESCUED_OVER_STRICT";
                result.warning = "standard_oversegmented_strict_used";
            } else {
                result.qcStatus = "OVERSEGMENTED_STRICT";
                result.warning = "strict_still_outside_qc";
            }
            IJ.log("C3 QC candidate B: area=" + fmt(result.qc.maskAreaPct) + "%, largestComponent=" + fmt(result.qc.largestComponentPct) + "%, objects=" + result.qc.objectCount + ", status=" + result.qcStatus);
        }

        if ("OVERSEGMENTED_STRICT".equals(result.qcStatus) && s.useMitoLocalPhansalkarFallback) {
            IJ.log("C3 QC: strict mask still oversegmented, trying LOCAL_PHANSALKAR fallback.");
            closeImage(result.mask);
            result.methodUsed = "LOCAL_PHANSALKAR";
            result.mask = createLocalPhansalkarMask(c2Raw, base + "_MITOS_THRESH", s);
            result.qc = evaluateMask(result.mask, s);
            if (isMaskQCGood(result.qc, s)) {
                result.qcStatus = "RESCUED_OVER_LOCAL";
                result.warning = "standard_and_strict_oversegmented_local_used";
            } else {
                result.qcStatus = "QC_FAIL_OVER_LOCAL_USED";
                result.warning = "c3_still_suspicious_after_local_fallback";
            }
            IJ.log("C3 QC candidate C: area=" + fmt(result.qc.maskAreaPct) + "%, largestComponent=" + fmt(result.qc.largestComponentPct) + "%, objects=" + result.qc.objectCount + ", status=" + result.qcStatus);
        }

        if ("UNDERSEGMENTED_STANDARD".equals(result.qcStatus)) {
            IJ.log("C3 QC: undersegmented standard mask, trying SOFT_BG_OTSU fallback.");
            closeImage(result.mask);
            result.methodUsed = "SOFT_BG35_OTSU";
            result.mask = createGlobalThresholdMask(c2Raw, base + "_MITOS_THRESH", s.mitoSoftRolling, s.mitoSoftSigma, s.mitoSoftMethod, false, s);
            result.qc = evaluateMask(result.mask, s);
            if (isMaskQCGood(result.qc, s)) {
                result.qcStatus = "RESCUED_UNDER_SOFT";
                result.warning = "standard_undersegmented_soft_used";
            } else {
                result.qcStatus = "QC_FAIL_UNDER_SOFT_USED";
                result.warning = "c3_still_suspicious_after_soft_fallback";
            }
            IJ.log("C3 QC candidate D: area=" + fmt(result.qc.maskAreaPct) + "%, largestComponent=" + fmt(result.qc.largestComponentPct) + "%, objects=" + result.qc.objectCount + ", status=" + result.qcStatus);
        }

        IJ.log("FINAL MITOS_THRESH method = " + result.methodUsed);
        IJ.log("FINAL C3 QC status = " + result.qcStatus);
        IJ.log("FINAL C3 area [% image] = " + fmt(result.qc.maskAreaPct));
        IJ.log("FINAL C3 largest component [% mask] = " + fmt(result.qc.largestComponentPct));
        IJ.log("FINAL C3 object count = " + result.qc.objectCount);
        if (result.warning != null && !result.warning.isEmpty()) IJ.log("FINAL C3 warning = " + result.warning);
        return result;
    }

    private ImagePlus createGlobalThresholdMask(ImagePlus raw, String title, double rolling, double sigma, String method, boolean doOpen, Settings s) {
        ImagePlus mask = new Duplicator().run(raw);
        mask.setTitle(uniqueTitle(title));
        if (rolling > 0) IJ.run(mask, "Subtract Background...", "rolling=" + fmt(rolling) + " sliding");
        if (sigma > 0) IJ.run(mask, "Gaussian Blur...", "sigma=" + fmt(sigma));
        IJ.run(mask, "8-bit", "");
        IJ.setAutoThreshold(mask, method + " dark");
        IJ.run(mask, "Convert to Mask", "");
        if (doOpen) IJ.run(mask, "Open", "");
        IJ.run(mask, "Remove Outliers...", "radius=" + fmt(s.mitoOutlierRadius) + " threshold=" + fmt(s.mitoOutlierThreshold) + " which=Bright");
        forceBinary(mask);
        return mask;
    }

    private ImagePlus createLocalPhansalkarMask(ImagePlus raw, String title, Settings s) {
        ImagePlus mask = new Duplicator().run(raw);
        mask.setTitle(uniqueTitle(title));
        if (s.mitoLocalRolling > 0) IJ.run(mask, "Subtract Background...", "rolling=" + fmt(s.mitoLocalRolling) + " sliding");
        if (s.mitoLocalSigma > 0) IJ.run(mask, "Gaussian Blur...", "sigma=" + fmt(s.mitoLocalSigma));
        IJ.run(mask, "8-bit", "");
        IJ.run(mask, "Auto Local Threshold", "method=Phansalkar radius=" + s.mitoLocalRadius + " parameter_1=0 parameter_2=0 white");
        IJ.run(mask, "Remove Outliers...", "radius=" + fmt(s.mitoOutlierRadius) + " threshold=" + fmt(s.mitoOutlierThreshold) + " which=Bright");
        forceBinary(mask);
        return mask;
    }

    private void forceBinary(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor().convertToByte(false);
        ByteProcessor bp = (ByteProcessor) ip;
        byte[] px = (byte[]) bp.getPixels();
        for (int i = 0; i < px.length; i++) {
            int v = px[i] & 0xff;
            px[i] = (byte) (v > 0 ? 255 : 0);
        }
        imp.setProcessor(bp);
        imp.updateAndDraw();
    }

    private ImagePlus asBinaryDuplicate(ImagePlus imp, String title) {
        ImagePlus dup = new Duplicator().run(imp);
        dup.setTitle(title);
        forceBinary(dup);
        return dup;
    }

    private MitoMaskQC evaluateMask(ImagePlus mask, Settings s) {
        ByteProcessor bp = (ByteProcessor) mask.getProcessor().convertToByte(false);
        int w = bp.getWidth();
        int h = bp.getHeight();
        byte[] p = (byte[]) bp.getPixels();
        int n = w * h;
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];

        int white = 0;
        for (byte b : p) if ((b & 0xff) > 0) white++;

        int objectCount = 0;
        int largest = 0;
        int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};

        for (int idx = 0; idx < n; idx++) {
            if (visited[idx] || (p[idx] & 0xff) == 0) continue;
            objectCount++;
            int head = 0, tail = 0;
            queue[tail++] = idx;
            visited[idx] = true;
            int count = 0;
            while (head < tail) {
                int cur = queue[head++];
                count++;
                int x = cur % w;
                int y = cur / w;
                for (int k = 0; k < 8; k++) {
                    int nx = x + dx[k];
                    int ny = y + dy[k];
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                    int ni = ny * w + nx;
                    if (!visited[ni] && (p[ni] & 0xff) > 0) {
                        visited[ni] = true;
                        queue[tail++] = ni;
                    }
                }
            }
            if (count > largest) largest = count;
        }

        MitoMaskQC qc = new MitoMaskQC();
        qc.maskPixelCount = white;
        qc.maskAreaPct = 100.0 * white / n;
        qc.objectCount = objectCount;
        qc.largestComponentPct = white > 0 ? 100.0 * largest / white : 0.0;
        return qc;
    }

    private String classifyInitialQC(MitoMaskQC qc, Settings s, String tag) {
        if (qc.maskAreaPct > s.mitoMaskMaxGoodPercent || qc.largestComponentPct > s.mitoLargestComponentMaxPct) {
            return "OVERSEGMENTED_" + tag;
        }
        if (qc.maskAreaPct < s.mitoMaskMinGoodPercent) {
            return "UNDERSEGMENTED_" + tag;
        }
        return "OK_" + tag;
    }

    private boolean isMaskQCGood(MitoMaskQC qc, Settings s) {
        return qc.maskAreaPct <= s.mitoMaskMaxGoodPercent &&
                qc.maskAreaPct >= s.mitoMaskMinGoodPercent &&
                qc.largestComponentPct <= s.mitoLargestComponentMaxPct;
    }

    private void runStarDist(ImagePlus input, Settings s) {
        String t = input.getTitle();
        String args = "command=[de.csbdresden.stardist.StarDist2D], " +
                "args=['input':'" + escapeForCommandFromMacro(t) + "', " +
                "'modelChoice':'Versatile (fluorescent nuclei)', " +
                "'normalizeInput':'true', " +
                "'percentileBottom':'" + fmt(s.stardistPercentileBottom) + "', " +
                "'percentileTop':'" + fmt(s.stardistPercentileTop) + "', " +
                "'probThresh':'" + fmt(s.stardistProbThresh) + "', " +
                "'nmsThresh':'" + fmt(s.stardistNmsThresh) + "', " +
                "'outputType':'Both', " +
                "'nTiles':'1', " +
                "'excludeBoundary':'" + s.stardistExcludeBoundary + "', " +
                "'roiPosition':'Automatic', " +
                "'verbose':'false', " +
                "'showCsbdeepProgress':'false', " +
                "'showProbAndDist':'false'], process=[false]";
        IJ.run("Command From Macro", args);
    }

    private ImageStatistics statsUnderRoi(ImagePlus imp, Roi roi) {
        imp.setRoi(roi);
        return imp.getStatistics(Measurements.MEAN | Measurements.STD_DEV | Measurements.MIN_MAX | Measurements.INTEGRATED_DENSITY | Measurements.AREA);
    }

    private int countRoiPixels(Roi roi, int width, int height) {
        Rectangle b = roi.getBounds();
        int xmin = Math.max(0, b.x);
        int ymin = Math.max(0, b.y);
        int xmax = Math.min(width - 1, b.x + b.width - 1);
        int ymax = Math.min(height - 1, b.y + b.height - 1);
        int count = 0;
        for (int y = ymin; y <= ymax; y++) {
            for (int x = xmin; x <= xmax; x++) {
                if (roi.contains(x, y)) count++;
            }
        }
        return count;
    }

    private int countOverlapPixels(Roi roi, ByteProcessor mask, int width, int height) {
        Rectangle b = roi.getBounds();
        int xmin = Math.max(0, b.x);
        int ymin = Math.max(0, b.y);
        int xmax = Math.min(width - 1, b.x + b.width - 1);
        int ymax = Math.min(height - 1, b.y + b.height - 1);
        int count = 0;
        for (int y = ymin; y <= ymax; y++) {
            for (int x = xmin; x <= xmax; x++) {
                if (roi.contains(x, y) && mask.get(x, y) > 0) count++;
            }
        }
        return count;
    }

    private void fillRoi(ByteProcessor bp, Roi roi, int width, int height, int value) {
        Rectangle b = roi.getBounds();
        int xmin = Math.max(0, b.x);
        int ymin = Math.max(0, b.y);
        int xmax = Math.min(width - 1, b.x + b.width - 1);
        int ymax = Math.min(height - 1, b.y + b.height - 1);
        for (int y = ymin; y <= ymax; y++) {
            for (int x = xmin; x <= xmax; x++) {
                if (roi.contains(x, y)) bp.set(x, y, value);
            }
        }
    }

    private ColorProcessor makeDecisionQCBase(ImagePlus c1, ImagePlus c3mask, int lineWidth) {
        ImageProcessor gray = c1.getProcessor().convertToByte(true);
        int w = gray.getWidth();
        int h = gray.getHeight();
        ColorProcessor cp = new ColorProcessor(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = gray.get(x, y);
                cp.set(x, y, (v << 16) | (v << 8) | v);
            }
        }
        drawMaskOutline(cp, (ByteProcessor) c3mask.getProcessor().convertToByte(false), Color.RED, lineWidth);
        return cp;
    }

    private void drawMaskOutline(ColorProcessor cp, ByteProcessor mask, Color color, int lineWidth) {
        int w = mask.getWidth();
        int h = mask.getHeight();
        int rgb = color.getRGB();
        int radius = Math.max(0, lineWidth - 1);
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (mask.get(x, y) == 0) continue;
                boolean edge = mask.get(x - 1, y) == 0 || mask.get(x + 1, y) == 0 || mask.get(x, y - 1) == 0 || mask.get(x, y + 1) == 0;
                if (!edge) continue;
                for (int yy = Math.max(0, y - radius); yy <= Math.min(h - 1, y + radius); yy++) {
                    for (int xx = Math.max(0, x - radius); xx <= Math.min(w - 1, x + radius); xx++) {
                        cp.set(xx, yy, rgb);
                    }
                }
            }
        }
    }

    private void drawRoi(ColorProcessor cp, Roi roi, Color color, int lineWidth) {
        cp.setColor(color);
        cp.setLineWidth(lineWidth);
        cp.draw(roi);
    }

    private void drawBorderlineId(ColorProcessor cp, Roi roi, int id, int fontSize) {
        Rectangle b = roi.getBounds();
        cp.setColor(Color.CYAN);
        cp.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        cp.drawString(Integer.toString(id), b.x + b.width / 2, b.y + b.height / 2);
    }

    private void writeMitoQC(File outDir, String base, MitoMaskResult mito, Settings s) throws IOException {
        File f = new File(outDir, base + "-MitoMask_QC.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            pw.println("Image,MitoMask_MethodUsed,MitoMask_QCStatus,MitoMask_Warning,C3_MaskAreaPct,C3_LargestComponentPct,C3_ObjectCount,MinGoodPct,MaxGoodPct,LargestComponentMaxPct");
            pw.println(csv(base) + "," + csv(mito.methodUsed) + "," + csv(mito.qcStatus) + "," + csv(mito.warning) + "," +
                    fmt(mito.qc.maskAreaPct) + "," + fmt(mito.qc.largestComponentPct) + "," + mito.qc.objectCount + "," +
                    fmt(s.mitoMaskMinGoodPercent) + "," + fmt(s.mitoMaskMaxGoodPercent) + "," + fmt(s.mitoLargestComponentMaxPct));
        }
    }

    private void writeEmptyResults(File outDir, String base) throws IOException {
        File f = new File(outDir, base + "-AllC1_ColocBorderOut_Morphology.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            pw.println(" ,Area,Mean,StdDev,Min,Max,X,Y,XM,YM,Perim.,BX,BY,Width,Height,Major,Minor,Angle,Circ.,Feret,IntDen,RawIntDen,FeretX,FeretY,FeretAngle,MinFeret,AR,Round,Solidity,Image,Object_ID_AllC1,Class,ClassCode,Is_Colocalized,Is_Borderline,Is_OutOfMito,Is_Filtered,Area_px_manual,Area_um2_manual,C3_MitoMask_OverlapPct,C3_MitoMask_NonOverlapPct,C3_MitoMask_OverlapArea_px,C3_MitoMask_OverlapArea_um2,C1_NUCLEOIDS_Mean_manual,C1_NUCLEOIDS_StdDev_manual,C1_NUCLEOIDS_Min_manual,C1_NUCLEOIDS_Max_manual,C2_MITOS_RAW_Mean,C2_MITOS_RAW_StdDev,C2_MITOS_RAW_Min,C2_MITOS_RAW_Max,Threshold_Out_MaxOverlapPct,Threshold_Coloc_MinOverlapPct,MitoMask_MethodUsed,MitoMask_QCStatus,MitoMask_Warning,MitoMask_AreaPct,MitoMask_LargestComponentPct,MitoMask_ObjectCount,PixelWidth,PixelHeight,Unit,PluginVersion");
        }
    }

    private void writeSettingsLog(File outDir, String base, Settings s, double pixelWidth, double pixelHeight, String unit) throws IOException {
        File f = new File(outDir, base + "-Analysis_Log.txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            pw.println("Nucleoid_Mito_Classifier_v0_1c");
            pw.println("Core rule: C1 ROIs are measured whole; C3 is used only for classification.");
            pw.println("pixelWidth=" + pixelWidth);
            pw.println("pixelHeight=" + pixelHeight);
            pw.println("unit=" + unit);
            pw.println("mitoRolling=" + s.mitoRolling);
            pw.println("mitoSigma=" + s.mitoSigma);
            pw.println("mitoOutlierRadius=" + s.mitoOutlierRadius);
            pw.println("mitoOutlierThreshold=" + s.mitoOutlierThreshold);
            pw.println("mitoMaskMinGoodPercent=" + s.mitoMaskMinGoodPercent);
            pw.println("mitoMaskMaxGoodPercent=" + s.mitoMaskMaxGoodPercent);
            pw.println("mitoLargestComponentMaxPct=" + s.mitoLargestComponentMaxPct);
            pw.println("useMitoLocalPhansalkarFallback=" + s.useMitoLocalPhansalkarFallback);
            pw.println("mitoDilationsForClassification=" + s.mitoDilationsForClassification);
            pw.println("c1Rolling=" + s.c1Rolling);
            pw.println("c1Sigma=" + s.c1Sigma);
            pw.println("stardistPercentileBottom=" + s.stardistPercentileBottom);
            pw.println("stardistPercentileTop=" + s.stardistPercentileTop);
            pw.println("stardistProbThresh=" + s.stardistProbThresh);
            pw.println("stardistNmsThresh=" + s.stardistNmsThresh);
            pw.println("stardistExcludeBoundary=" + s.stardistExcludeBoundary);
            pw.println("maxMitoOverlapPctForOut=" + s.maxMitoOverlapPctForOut);
            pw.println("minMitoOverlapPctForColoc=" + s.minMitoOverlapPctForColoc);
            pw.println("minRoiAreaPx=" + s.minRoiAreaPx);
            pw.println("maxRoiAreaPx=" + s.maxRoiAreaPx);
            pw.println("minC1Max=" + s.minC1Max);
        }
    }

    private boolean isDerivedOutputName(String name) {
        return name.contains("-COMPOSITE") || name.contains("-QC_") || name.contains("-DecisionQC") ||
                name.contains("-ObjectMask") || name.contains("-AllC1") || name.contains("-Summary") ||
                name.contains("-Nucleoids") || name.contains("-Colocalized") || name.contains("-Borderline") ||
                name.contains("-OutOfMito") || name.contains("-MitoAssociated") || name.contains("-MitoMask_QC") ||
                name.contains("-Filtered");
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String fmt(double x) {
        return String.format(CSV_LOCALE, "%.6f", x);
    }

    private static String csv(String s) {
        if (s == null) s = "";
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static String escapeForCommandFromMacro(String s) {
        return s.replace("'", "\\'");
    }

    private static String uniqueTitle(String base) {
        String title = base;
        int n = 1;
        while (WindowManager.getImage(title) != null) {
            title = base + "_" + n;
            n++;
        }
        return title;
    }

    private static void saveTiff(ImagePlus imp, File file) {
        new FileSaver(imp).saveAsTiff(file.getAbsolutePath());
        if (imp != null) imp.changes = false;
    }

    private static RoiManagerState captureRoiManagerState() {
        RoiManagerState state = new RoiManagerState();
        RoiManager rm = RoiManager.getInstance2();
        state.existed = rm != null;
        state.visible = rm != null && rm.isVisible();
        state.rois = cloneRois(rm == null ? null : rm.getRoisAsArray());
        return state;
    }

    private static void restoreRoiManagerState(RoiManagerState state) {
        if (state == null) return;
        RoiManager rm = RoiManager.getInstance2();

        if (!state.existed) {
            if (rm != null) {
                try {
                    rm.reset();
                    rm.close();
                } catch (Throwable ignored) {
                }
            }
            return;
        }

        if (rm == null) {
            rm = state.visible ? new RoiManager() : new RoiManager(false);
        }
        rm.reset();
        for (Roi roi : state.rois) {
            if (roi != null) rm.addRoi((Roi) roi.clone());
        }
        try {
            rm.setVisible(state.visible);
        } catch (Throwable ignored) {
        }
    }

    private static Roi[] cloneRois(Roi[] rois) {
        if (rois == null || rois.length == 0) return new Roi[0];
        Roi[] copies = new Roi[rois.length];
        for (int i = 0; i < rois.length; i++) {
            copies[i] = rois[i] == null ? null : (Roi) rois[i].clone();
        }
        return copies;
    }

    private static int[] snapshotOpenImageIds() {
        int[] ids = WindowManager.getIDList();
        return ids == null ? new int[0] : ids.clone();
    }

    private static boolean containsImageId(int[] ids, int id) {
        if (ids == null) return false;
        for (int candidate : ids) {
            if (candidate == id) return true;
        }
        return false;
    }

    private static ImagePlus findNewImageByTitlePrefix(int[] previousIds, String prefix) {
        int[] currentIds = WindowManager.getIDList();
        if (currentIds == null) return null;
        for (int i = currentIds.length - 1; i >= 0; i--) {
            int id = currentIds[i];
            if (containsImageId(previousIds, id)) continue;
            ImagePlus imp = WindowManager.getImage(id);
            if (imp != null && imp.getTitle() != null && imp.getTitle().startsWith(prefix)) {
                return imp;
            }
        }
        return null;
    }

    /**
     * Close ImageJ windows created by this plugin without asking whether to save changes.
     * All required QC images and CSV files are saved explicitly before this method is called.
     */
    private static void closeImage(ImagePlus imp) {
        if (imp == null) return;
        try {
            imp.changes = false;
            if (imp.getWindow() != null) imp.getWindow().close();
            else imp.close();
        } catch (Throwable t) {
            try {
                imp.changes = false;
                imp.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
