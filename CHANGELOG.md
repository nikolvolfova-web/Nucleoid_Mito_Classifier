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
* Explicit requirement that input images must be spatially calibrated in micrometres (`µm`).

### Remaining before a stable public release

* Enforce the supported single-plane, single-time-point 2D input format in the code.
* Verify StarDist input handling when debug mode is enabled.
* Avoid affecting unrelated ImageJ windows or an existing ROI Manager.
* Use an identical CSV schema when no C1 objects are detected.
* Add regression tests using non-sensitive example images.
* Optionally add automatic validation of the input calibration unit.

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
