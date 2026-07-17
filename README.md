# Nucleoid Mito Classifier

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

### Important dimensionality limitation

The code validates the number of channels but does not currently reject Z-stacks or time series. Several processing steps use the active 2D processor. Until this is corrected and tested, use only single-plane, single-time-point 2D images.

## Installation from a release JAR

1. Close Fiji.
2. Remove older matching plugin `.jar`, `.java`, and `.class` files from `Fiji.app/plugins/`.
3. Copy `Nucleoid_Mito_Classifier_v0_1c.jar` into `Fiji.app/plugins/`.
4. Restart Fiji or use **Help → Refresh Menus**.
5. Run **Plugins → Analyze → Nucleoid Mito Classifier v0.1c**.

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

See [`docs/TECHNICAL_AUDIT.md`](docs/TECHNICAL_AUDIT.md). The most important unresolved items are:

- unit validation for calibrated areas;
- explicit dimensionality validation or stack/time-series support;
- reliable StarDist input handling in debug mode;
- avoiding destructive changes to a pre-existing ROI Manager or unrelated ImageJ windows;
- consistent CSV schema for images with zero detected objects;
- complete logging of all analysis parameters;
- automated tests with non-sensitive example images.

## Repository policy

Do not commit raw microscopy data, generated TIFF/CSV outputs, secrets, patient identifiers, or other confidential material. Small anonymized test fixtures may be stored under `examples/`.

Compiled JAR files should normally be attached to a GitHub Release rather than committed to the source tree.

## Version history

See [`CHANGELOG.md`](CHANGELOG.md).

## Citation

Citation metadata has not yet been completed. Add verified author names, author order, ORCID identifiers, affiliations, repository URL, license, and DOI only after confirmation by all relevant contributors.

## License

This project is licensed under the BSD 3-Clause License. See the [`LICENSE`](LICENSE) file for the full license text.

Copyright © 2026 Nikol Volfová.

