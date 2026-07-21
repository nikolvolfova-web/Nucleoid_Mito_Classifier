# Nucleoid Mito Classifier

[![Maven build](https://github.com/nikolvolfova-web/Nucleoid_Mito_Classifier/actions/workflows/maven-build.yml/badge.svg?branch=main)](https://github.com/nikolvolfova-web/Nucleoid_Mito_Classifier/actions/workflows/maven-build.yml)

Fiji/ImageJ 1.x Java plugin for classifying C1 nucleoid objects according to their fractional overlap with a mitochondria-derived C3 mask.

> **Project status:** release candidate for version `v0.2.0`. The workflow-generated JAR must be tested in Fiji before the corresponding GitHub Release is published.

## Methodological principle

C1 nucleoid objects are segmented independently of the mitochondrial mask. Morphology and C1 intensity are measured from each complete C1 ROI.

The C3 mitochondrial mask is generated independently from channel C2 and is used only for overlap-based classification. It must not clip, trim, split, or otherwise alter C1 ROI geometry.

## Classification rule

For every detected C1 ROI:

* `COLOCALIZED`: C3 overlap is at least `minMitoOverlapPctForColoc` (default `20%`);
* `OUT_OF_MITO`: C3 overlap is at most `maxMitoOverlapPctForOut` (default `5%`);
* `BORDERLINE`: overlap is between the two thresholds;
* `FILTERED`: the ROI fails the configured area or C1-intensity filters.

## Requirements

* Fiji/ImageJ;
* StarDist Fiji plugin for C1 object detection;
* `CSBDeep`, `StarDist`, and `TensorFlow` Fiji update sites enabled;
* Auto Local Threshold plugin when the optional local Phansalkar fallback is enabled and triggered.

The Maven configuration produces Java 8-compatible bytecode. Release JARs must still be tested in the intended Fiji environment before scientific use.

## Tested environment

Version `v0.1c` and subsequent development builds leading to `v0.2.0` were successfully tested in:

* Fiji / ImageJ2: `2.18.0`;
* ImageJ1: `1.54p`;
* Java: `21.0.7` (64-bit);
* operating system: macOS Sequoia `15.7.7` (build `24G720`).

The final `v0.2.0` workflow artifact must be tested again before the release is published.

## Input requirements

The plugin accepts a folder containing `.tif` or `.tiff` images.

Every input image must have:

* exactly two channels;
* C1 containing the nucleoid signal;
* C2 containing the mitochondrial signal;
* exactly one Z slice;
* exactly one time point;
* valid positive pixel-width and pixel-height values;
* spatial calibration in micrometres.

### Input validation

Before analysis, the plugin verifies the number of channels, Z slices, time points, calibration unit, pixel width, and pixel height.

Images that do not meet the supported input requirements are skipped, and a corresponding message is written to the ImageJ log.

## Installation from a release JAR

1. Open the **Releases** section of this repository.
2. Select release `v0.2.0`.
3. Download `Nucleoid_Mito_Classifier_v0_2_0.jar` from **Assets**.
4. Do not download the automatically generated source-code archives for plugin installation.
5. Close Fiji.
6. Remove older matching plugin `.jar`, `.java`, and `.class` files from `Fiji.app/plugins/`.
7. Copy `Nucleoid_Mito_Classifier_v0_2_0.jar` into `Fiji.app/plugins/`.
8. Restart Fiji.
9. Run **Plugins → Nucleoid Mito Classifier v0.2.0**.

## Usage

1. Prepare a folder containing the original two-channel `.tif` or `.tiff` images.
2. Verify in Fiji that every input image satisfies the documented input requirements.
3. Start the plugin using **Plugins → Nucleoid Mito Classifier v0.2.0**.
4. Select the input folder.
5. Select or create a separate output folder.
6. Review the analysis parameters in the plugin dialog.
7. Click **OK** to start batch processing.
8. Review the generated CSV files, analysis logs, decision QC images, and mitochondrial-mask QC files.

Use a separate output folder. Do not place generated outputs in the input folder.

## Building from source

### Requirements

* JDK 8 or later;
* Apache Maven.

### Build command

```bash
mvn clean package
```

### Expected artifact

```text
target/Nucleoid_Mito_Classifier_v0_2_0.jar
```

Maven coordinates:

```text
io.github.nikolvolfovaweb:Nucleoid_Mito_Classifier:0.2.0
```

## Main outputs

For every successfully processed input image, the plugin writes:

* C1 and C2 QC TIFF images;
* C3 mitochondrial-mask QC TIFF and QC CSV;
* StarDist input QC TIFF;
* all-C1 label image;
* per-object morphology and classification CSV;
* class-specific binary object masks;
* decision QC image with C3 and class outlines;
* analysis settings log.

A combined summary CSV is also written for all images in the selected batch.

### Zero-object results

When no C1 objects are detected, the per-object CSV still contains the complete standard 63-column header and no data rows. This preserves a consistent output schema for automated processing.

## Spatial calibration

All input images must be spatially calibrated in micrometres before analysis.

The plugin calculates calibrated areas as:

* `Area_um2_manual = Area_px_manual × PixelWidth × PixelHeight`
* `C3_MitoMask_OverlapArea_um2 = C3_MitoMask_OverlapArea_px × PixelWidth × PixelHeight`

When the required input calibration is valid, area values are reported in square micrometres (`µm²`).

The plugin records `PixelWidth`, `PixelHeight`, and the calibration unit in its output CSV files.

## Reproducible example dataset

A reproducible example dataset is available in the [`examples/`](examples/) directory.

It contains:

* a two-channel TIFF test image;
* a reference output archive;
* documented image dimensions, channel order, pixel calibration, and analysis parameters;
* expected object and classification counts;
* SHA-256 checksums for verifying the reference outputs.

The documented reference result contains:

| Result              | Expected value |
| ------------------- | -------------: |
| All C1 objects      |            583 |
| Colocalized         |            394 |
| Borderline          |             47 |
| Out of mitochondria |            142 |
| Filtered            |              0 |

The example supports installation and reproducibility checks. It is not presented as a biological benchmark or as validation across different sample types, microscopes, acquisition settings, or laboratories.

## Resource safety

The plugin preserves unrelated ImageJ image windows and ROI Manager content that existed before analysis.

During normal processing, it closes only temporary resources created and owned by the plugin. Diagnostic plugin windows may remain open when debug mode is enabled.

## Known limitations

See [`docs/TECHNICAL_AUDIT.md`](docs/TECHNICAL_AUDIT.md) for the detailed technical review.

Remaining limitations include:

* the example dataset currently supports manual reproducibility checking rather than a fully automated image-analysis regression test;
* users should verify analysis parameters and QC outputs for their own microscopy system, sample type, and acquisition conditions;
* the plugin has been tested in a limited set of Fiji, Java, and operating-system environments.

## Repository policy

Do not commit:

* patient-identifiable data;
* confidential or unpublished microscopy data without permission;
* passwords, access tokens, private keys, or other credentials;
* large raw datasets or routine generated outputs.

Small anonymized and redistributable test fixtures may be stored under `examples/`.

Compiled JAR files should be attached to GitHub Releases rather than committed to the source tree.

## Version history

See [`CHANGELOG.md`](CHANGELOG.md).

## Author

**Nikol Volfová**

ORCID: [0000-0002-8040-2610](https://orcid.org/0000-0002-8040-2610)

Affiliations:

1. Department of Paediatrics and Inherited Metabolic Disorders, First Faculty of Medicine, Charles University and General University Hospital in Prague, Prague, Czech Republic
2. Department of Biochemistry, Cell and Molecular Biology, Third Faculty of Medicine, Charles University, Prague, Czech Republic

## Citation

Citation metadata are provided in [`CITATION.cff`](CITATION.cff).

A DOI will be added after the stable GitHub Release has been archived through Zenodo.

## Licences

The software source code is distributed under the BSD 3-Clause License. See [`LICENSE`](LICENSE).

The example input image and associated reference outputs in `examples/` are distributed separately under CC BY 4.0. See [`examples/LICENSE.md`](examples/LICENSE.md).

Copyright © 2026 Nikol Volfová.
