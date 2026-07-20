# Nucleoid Mito Classifier
[![Maven build](https://github.com/nikolvolfova-web/Nucleoid_Mito_Classifier/actions/workflows/maven-build.yml/badge.svg?branch=main)](https://github.com/nikolvolfova-web/Nucleoid_Mito_Classifier/actions/workflows/maven-build.yml)

Fiji/ImageJ 1.x Java plugin for classifying C1 nucleoid objects according to their fractional overlap with a mitochondria-derived C3 mask.

> **Project status:** pre-release research software. Version `v0.1c` is a batch-processing hotfix and should be validated with representative test images before public scientific use.

## Methodological principle

C1 nucleoid objects are segmented independently of the mitochondrial mask. Morphology and C1 intensity are measured from each complete C1 ROI. The C3 mitochondrial mask is generated independently from channel C2 and is used only for overlap-based classification; it must not clip, trim, split, or otherwise alter C1 ROI geometry.

## Current classification rule

For every detected C1 ROI:

- `COLOCALIZED`: C3 overlap is at least `minMitoOverlapPctForColoc` (default `20%`).
- `OUT_OF_MITO`: C3 overlap is at most `maxMitoOverlapPctForOut` (default `5%`).
- `BORDERLINE`: overlap is between the two thresholds.
- `FILTERED`: the ROI fails the configured area or C1-intensity filters.

## Requirements

- Fiji/ImageJ.
- StarDist Fiji plugin for C1 object detection.
- `CSBDeep`, `StarDist`, and `TensorFlow` Fiji update sites enabled for StarDist.
- Auto Local Threshold plugin when the optional local Phansalkar fallback is enabled and triggered.

The supplied `v0.1c` JAR was compiled with Java 21. A Maven build configuration is included to produce Java 8-compatible bytecode, but that build must be verified in both the intended Fiji installation and a clean test installation before release.

## Tested environment

Version `v0.1c` was successfully installed and tested in the following environment:

* Fiji / ImageJ2: `2.18.0`
* ImageJ1: `1.54p`
* Java: `21.0.7` (64-bit)
* Operating system: macOS Sequoia `15.7.7` (build `24G720`)

The plugin JAR was downloaded from the GitHub Release, copied into `Fiji.app/plugins/`, and successfully used to run the analysis workflow.

## Input

The current implementation expects a folder of `.tif` or `.tiff` images with exactly two channels:

- C1: nucleoids.
- C2: mitochondria.

### Input validation

Before analysis, the plugin verifies that every input image:

* contains exactly two channels;
* contains exactly one Z slice;
* contains exactly one time point;
* has valid positive pixel-width and pixel-height values;
* uses a supported spatial calibration unit in micrometres.

Images that do not meet these requirements are skipped and a corresponding message is written to the ImageJ log.

## Installation from a release JAR

1. Open the **Releases** section of this repository.
2. Select the required release and download `Nucleoid_Mito_Classifier_v0_1c.jar` from **Assets**. Do not download the automatically generated source-code archives for plugin installation.
3. Close Fiji.
4. Remove older matching plugin `.jar`, `.java`, and `.class` files from `Fiji.app/plugins/`.
5. Copy `Nucleoid_Mito_Classifier_v0_1c.jar` into `Fiji.app/plugins/`.
6. Restart Fiji.
7. Run **Plugins → Nucleoid Mito Classifier v0.1c**.

## Usage

1. Prepare a folder containing the original two-channel `.tif` or `.tiff` images.
2. Verify in Fiji that every input image:

   * contains exactly two channels;
   * uses C1 for nucleoids and C2 for mitochondria;
   * is a single-plane, single-time-point 2D image;
   * is spatially calibrated in micrometres (`µm`).
3. Start the plugin using **Plugins → Nucleoid Mito Classifier v0.1c**.
4. Select the input folder containing the original TIFF images.
5. Select or create a separate output folder for CSV files and QC images.
6. Review the analysis parameters in the plugin dialog.
7. Click **OK** to start batch processing.
8. After processing, review the decision QC images, mitochondrial-mask QC files, per-object CSV files, and the combined summary CSV.

Use a separate output folder and do not place generated outputs in the input folder.

## Building from source

Requirements:

- JDK 8 or later.
- Apache Maven.

Build:

```bash
mvn clean package
```

Expected artifact:

```text
target/Nucleoid_Mito_Classifier_v0_1c.jar
```

The `pom.xml` currently contains `TODO.GROUP_ID`; replace it with a verified group identifier before publication.

## Main outputs

For each input image, the plugin writes:

- C1 and C2 QC TIFF images.
- C3 mitochondrial-mask QC TIFF and QC CSV.
- StarDist input QC TIFF.
- all-C1 label image.
- per-object morphology and classification CSV.
- class-specific binary object masks.
- decision QC image with C3 and class outlines.
- analysis settings log.

A combined summary CSV is also written for all images in the selected batch.

## Spatial calibration requirement

All input images must be spatially calibrated in micrometres (`µm`) before analysis.

The plugin calculates calibrated areas as:

* `Area_um2_manual = Area_px_manual × PixelWidth × PixelHeight`
* `C3_MitoMask_OverlapArea_um2 = C3_MitoMask_OverlapArea_px × PixelWidth × PixelHeight`

Therefore, `PixelWidth` and `PixelHeight` must be expressed in micrometres. Under this required input condition, the resulting area values are reported in square micrometres (`µm²`).

Version `v0.1c` records `PixelWidth`, `PixelHeight`, and the calibration unit in the output CSV files. Users must verify the spatial calibration in Fiji before starting the analysis.


## Known limitations

See [`docs/TECHNICAL_AUDIT.md`](docs/TECHNICAL_AUDIT.md) for the detailed technical review. The most important unresolved items are:

* reliable StarDist input handling when debug mode is enabled;
* avoiding destructive changes to a pre-existing ROI Manager or unrelated ImageJ windows;
* consistent CSV schema for images with zero detected objects;
* complete logging of all analysis parameters;
* automated regression testing of representative outputs.

A non-sensitive example input and documented reference outputs are available in the [`examples/`](examples/) directory. These support manual reproducibility checks but do not yet constitute a fully automated test suite.

## Repository policy

Do not commit raw microscopy data, generated TIFF/CSV outputs, secrets, patient identifiers, or other confidential material. Small anonymized test fixtures may be stored under `examples/`.

Compiled JAR files should normally be attached to a GitHub Release rather than committed to the source tree.

## Version history

See [`CHANGELOG.md`](CHANGELOG.md).

## Author

Nikol Volfová

## Reproducible example dataset

A small reproducible example dataset is available in the [`examples/`](examples/) directory.

It contains:

* a two-channel TIFF test image;
* a reference output archive generated by the plugin;
* documented image dimensions, channel order, pixel calibration, and analysis parameters;
* expected object and classification counts;
* SHA-256 checksums for verifying the integrity of the reference outputs.

The example is intended to verify installation and reproduce a known plugin result. It should not be interpreted as a biological benchmark or as validation across different microscopy systems, sample types, or acquisition conditions.

### Licences

The software source code is distributed under the BSD 3-Clause License.

The example input image and its reference outputs are distributed under CC BY 4.0, as documented in [`examples/README.md`](examples/README.md). The software and example data therefore have separate licences.

## Citation

Citation metadata are provided in [`CITATION.cff`](CITATION.cff).

**Author:** Nikol Volfová
**ORCID:** [0000-0002-8040-2610](https://orcid.org/0000-0002-8040-2610)

**Affiliations:**

1. Department of Paediatrics and Inherited Metabolic Disorders, Charles University and General University Hospital in Prague, Prague, Czech Republic
2. Department of Biochemistry, Cell and Molecular Biology, Third Faculty of Medicine, Charles University, Prague, Czech Republic

A DOI will be added after a stable GitHub Release has been archived through Zenodo.

## License

This project is licensed under the BSD 3-Clause License. See the [`LICENSE`](LICENSE) file for the full license text.

Copyright © 2026 Nikol Volfová.

