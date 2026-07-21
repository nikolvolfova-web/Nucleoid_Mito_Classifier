# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

No changes have been documented since version `0.2.0`.

## [0.2.0] - 2026-07-21

### Added

* BSD 3-Clause License for the software.
* `CITATION.cff` citation metadata.
* Verified author name, ORCID, and affiliations.
* Installation and batch-processing instructions.
* Documentation of the tested Fiji, ImageJ, Java, and macOS environment.
* Automated Maven build using GitHub Actions.
* Java 8-compatible Maven build configuration.
* Maven coordinates:

  * `io.github.nikolvolfovaweb:Nucleoid_Mito_Classifier:0.2.0`
* Preventive validation of the supported input format:

  * exactly two channels;
  * one Z slice;
  * one time point;
  * valid positive pixel width and pixel height;
  * spatial calibration in micrometres.
* Non-sensitive reproducible example input image.
* Reference output archive with documented expected results.
* SHA-256 checksums for reference outputs.
* Separate CC BY 4.0 licence and attribution information for the example dataset.
* Clear ImageJ log message when no C1 objects are detected.

### Changed

* Updated the plugin class name to `Nucleoid_Mito_Classifier_v0_2_0`.
* Updated the displayed plugin version to `v0.2.0`.
* Updated the generated JAR filename to `Nucleoid_Mito_Classifier_v0_2_0.jar`.
* Updated the Maven project version and JAR manifest version to `0.2.0`.
* Updated README installation, usage, build, citation, licence, and reproducibility documentation.
* Corrected ImageJ plugin packaging by removing the invalid `plugins.config` file.
* Corrected the documented Fiji menu location.
* Standardized the per-object CSV schema to 63 columns for both non-empty and zero-object results.
* Restricted cleanup to temporary images created and owned by the plugin.
* Preserved unrelated ImageJ image windows during processing.
* Preserved ROI Manager content that existed before the analysis started.
* Preserved diagnostic plugin windows when debug mode is enabled.

### Fixed

* Ensured that StarDist always receives the intended preprocessed C1 image, including when debug mode is enabled.
* Removed global ImageJ `Close All` behaviour from plugin cleanup and error handling.
* Prevented the plugin from closing unrelated ImageJ windows.
* Prevented the plugin from permanently deleting pre-existing ROI Manager content.
* Prevented inconsistent shortened CSV headers when no C1 objects are detected.
* Prevented unsupported Z-stacks, time series, channel counts, and invalid spatial calibration from being processed.

### Validated

* GitHub Actions Maven builds completed successfully.
* Workflow-generated JAR files were installed and run successfully in Fiji.
* The reproducible example image produced:

  * 583 detected C1 objects;
  * 394 colocalized objects;
  * 47 borderline objects;
  * 142 out-of-mitochondria objects;
  * 0 filtered objects.
* Per-object numerical measurements and classifications remained unchanged after the input-validation, debug-mode, resource-cleanup, and CSV-schema corrections.
* Comparisons included all 583 data rows and all numerical output columns.
* The only expected difference in comparisons using renamed test images was the text value in the `Image` column.
* A zero-C1 test image produced:

  * the complete standard 63-column CSV header;
  * zero data rows.
* A pre-existing unrelated ImageJ image remained open after analysis.
* A pre-existing ROI remained present in the ROI Manager after analysis.

### Notes

* The reproducible example supports manual regression and installation checks.
* The example dataset is not presented as a biological benchmark.
* The final workflow-generated `v0.2.0` JAR must be tested once more before the GitHub Release is published.
* The existing `v0.1c` tag and Release must remain unchanged.

## [0.1c] - 2026-07-17

### Fixed

* Closed temporary ImageJ windows with `changes = false`.
* Reduced interruption by StarDist temporary-window save prompts during batch runs.
* Saved required QC images and CSV outputs explicitly before cleanup.
* Preserved input calibration on channel images, masks, label images, and QC TIFFs.

### Added

* Calibrated area calculations based on pixel width and pixel height.
* `PixelWidth`, `PixelHeight`, `Unit`, and `PluginVersion` output fields.

### Notes

* This version was released as a pre-release hotfix for uninterrupted batch analysis.
* Input images must be spatially calibrated in micrometres (`µm`).
* The supplied JAR targets Java 21.
* The Release JAR was successfully installed and tested in the environment documented in `README.md`.
