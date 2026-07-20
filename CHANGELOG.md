# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

### Added

* BSD 3-Clause License.
* `CITATION.cff` citation metadata.
* Verified author information.
* Tested Fiji, ImageJ, Java, and macOS environment.
* Installation instructions for the GitHub Release JAR.
* Usage instructions for batch analysis.
* Automated Maven build using GitHub Actions.
* Preventive validation of the supported input format:

  * exactly two channels;
  * one Z slice;
  * one time point;
  * valid positive pixel dimensions;
  * spatial calibration in micrometres (`µm`).

### Changed

* Corrected ImageJ plugin packaging by removing the invalid `plugins.config` file.
* Corrected the documented Fiji menu location to **Plugins → Nucleoid Mito Classifier v0.1c**.

### Validated

* The GitHub Actions build completes successfully.
* The workflow-generated JAR was successfully installed and tested in Fiji.
* The original and modified plugin versions produced byte-identical per-object CSV output on the same test image.
* The compared output contained 583 objects and 63 columns.
* Object classifications were identical:

  * 394 colocalized;
  * 142 out of mitochondria;
  * 47 borderline;
  * 0 filtered.

### Remaining before a stable public release

* Verify StarDist input handling when debug mode is enabled.
* Avoid affecting unrelated ImageJ windows or an existing ROI Manager.
* Use an identical CSV schema when no C1 objects are detected.
* Add non-sensitive example input data and documented expected outputs.
* Add reproducible regression testing for representative outputs.

## [0.1c] - 2026-07-17

### Fixed

* Close temporary ImageJ windows with `changes = false`.
* Reduce interruption by StarDist temporary-window save prompts during batch runs.
* Save required QC images and CSV outputs explicitly before cleanup.
* Preserve input calibration on channel images, masks, label images, and QC TIFFs.

### Added

* Calibrated area calculations based on pixel width and pixel height.
* `PixelWidth`, `PixelHeight`, `Unit`, and `PluginVersion` output fields.

### Notes

* This version was released as a pre-release hotfix for uninterrupted batch analysis.
* Input images must be spatially calibrated in micrometres (`µm`).
* The supplied JAR targets Java 21.
* The release JAR was successfully installed and tested in the environment documented in `README.md`.
