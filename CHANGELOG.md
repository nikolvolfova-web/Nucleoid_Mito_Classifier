# Changelog

All notable changes to this project should be documented in this file.

## [Unreleased]

### Required before public release

- Validate calibrated area units and rename or convert `_um2` fields safely.
- Define supported image dimensionality and enforce it in code.
- Add automated and manual regression tests.
- Complete authorship, license, and citation metadata.

## [0.1c] - 2026-07-08

### Fixed

- Close temporary ImageJ windows with `changes = false`.
- Reduce interruption by StarDist temporary-window save prompts during batch runs.
- Save required QC images and CSV outputs explicitly before cleanup.
- Preserve input calibration on channel images, masks, label images, and QC TIFFs.

### Added

- Calibrated area calculations based on pixel width and pixel height.
- `PixelWidth`, `PixelHeight`, `Unit`, and `PluginVersion` output fields.

### Notes

- This version was supplied as a hotfix for uninterrupted batch analysis.
- The original build JAR targets Java 21.
