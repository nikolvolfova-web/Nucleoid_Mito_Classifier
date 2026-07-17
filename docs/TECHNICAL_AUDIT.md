# Technical audit: Nucleoid Mito Classifier v0.1c

Audit scope: supplied Java source, compiled JAR, and initial README. No representative microscopy images or expected-output fixtures were supplied, so analytical correctness was not experimentally validated.

## Blocking

### 1. Calibration unit is not validated

**Problem:** calibrated areas are written to columns named `Area_um2_manual` and `C3_MitoMask_OverlapArea_um2`, while the calculation simply multiplies by the image's current pixel width and height. The input unit may be pixels, nanometres, millimetres, or another unit.

**Why it matters:** the column name can falsely imply square micrometres and lead to scientifically incorrect interpretation.

**Fix:** either validate and normalize supported units to micrometres, or use unit-neutral column names plus an explicit squared-area unit field.

**File:** `src/main/java/Nucleoid_Mito_Classifier_v0_1c.java`.

**Verification:** test calibrated images in `µm`, `nm`, and uncalibrated pixels; compare output against independently calculated areas.

### 2. Supported dimensionality is undefined

**Problem:** the code verifies two channels but does not verify `NSlices` or `NFrames`. Multiple operations use a single active `ImageProcessor`.

**Why it matters:** Z-stacks or time series may be processed only partially or inconsistently without a clear error.

**Fix:** either reject inputs unless `NChannels=2`, `NSlices=1`, and `NFrames=1`, or explicitly implement and test Z/T iteration.

**File:** `src/main/java/Nucleoid_Mito_Classifier_v0_1c.java`.

**Verification:** test 2D, Z-stack, time-series, and Z+T inputs and confirm deterministic documented behavior.

### 3. Ownership and licensing are unresolved

**Problem:** no author list, ownership statement, contributor agreement, or software license is present.

**Why it matters:** public redistribution and downstream reuse are legally unclear, especially if colleagues contributed.

**Fix:** confirm authorship and rights with all contributors, then add an approved `LICENSE`, verified author metadata, and `CITATION.cff`.

**Files:** `LICENSE`, `README.md`, `CITATION.cff`.

**Verification:** obtain contributor confirmation and validate the final citation file.

## High priority

### 4. Debug mode can prevent StarDist input from being shown

**Problem:** the StarDist input image is shown only when `debugMode` is false, although the comment states that StarDist requires an open image title.

**Why it matters:** enabling debug mode may cause detection to fail or use an unintended image.

**Fix:** ensure the StarDist input is shown whenever it has no window; keep it open after processing only when debug mode requires that behavior.

**Verification:** run the same image with debug mode both on and off and compare ROI count and label output.

### 5. Existing ImageJ state can be modified destructively

**Problem:** the plugin closes any image titled `Label Image`, resets the global ROI Manager, and calls `Close All` after an exception.

**Why it matters:** unrelated user images or ROIs may be lost or closed.

**Fix:** isolate plugin-owned windows and ROIs, track them explicitly, and never close unrelated global state.

**Verification:** open unrelated images and populate ROI Manager before a successful and a deliberately failing run; confirm they remain unchanged.

### 6. Empty per-object CSV has a different schema

**Problem:** when zero objects are detected, `writeEmptyResults` writes only a small subset of columns.

**Why it matters:** downstream concatenation and scripted analysis can fail or silently produce missing fields.

**Fix:** define one canonical output schema and use it for both populated and empty result files.

**Verification:** compare headers for an empty image and an image with detected objects; they must be identical.

### 7. Build is not fully reproducible

**Problem:** the supplied JAR has only a minimal manifest and was compiled directly with Java 21. No original build configuration was supplied.

**Why it matters:** users cannot reliably reproduce the binary or determine dependency/build settings.

**Fix:** use Maven, pin build dependencies, include `plugins.config`, record implementation version, and build in CI.

**Verification:** build from a clean checkout and compare behavior against the supplied JAR using the same fixtures.

### 8. Analysis log omits hidden fallback settings

**Problem:** strict, local-Phansalkar, and soft fallback parameters are not all written to the analysis log.

**Why it matters:** a run cannot be reconstructed fully from saved outputs.

**Fix:** log every parameter that can affect segmentation, classification, QC, drawing, and fallback selection.

**Verification:** compare every `Settings` field with the saved log; no analytical setting should be missing.

### 9. Derived-output filename filtering is fragile

**Problem:** matching is case-sensitive and includes broad words such as `-Nucleoids`, which may occur in legitimate input names.

**Why it matters:** valid images can be silently skipped, while lower-case derived outputs may be reprocessed.

**Fix:** use case-insensitive, exact output suffix patterns and preferably require different input and output directories.

**Verification:** test representative filenames including mixed case and biological terms.

## Medium priority

### 10. Skipped files count as processed

`processOneImage` returns normally for unreadable or non-two-channel inputs, after which the outer loop increments the processed count. Return a status value and report processed, skipped, and failed counts separately.

### 11. Summary fractions include filtered objects in the denominator

Clarify whether class fractions are intended relative to all detected objects or only objects passing filters. Consider writing both forms and an `Analyzed_C1_Objects` count.

### 12. No regression-test dataset

Add small anonymized 2D fixtures with expected ROI counts, class counts, mask QC status, output headers, and calibrated-area checks. Do not publish confidential research data.

### 13. Version is duplicated throughout code and filenames

Move the version into one constant or build-generated manifest field. A version-neutral class name would simplify future releases, but this refactor requires compatibility testing.

## JAR inspection

- SHA-256: `65ab2d5bb4188509db77b27b74f206420e5880f19679d155903fa0a806e08c81`
- Java class major version: `65` (Java 21)
- Manifest `Created-By`: `21.0.10 (Debian)`
- Included classes: main plugin class plus `Settings`, `MitoMaskQC`, and `MitoMaskResult` nested classes.
- No bundled third-party libraries were detected.

## Recommended release gate

A public release should require:

1. blocking findings resolved;
2. clean build from the repository;
3. successful tests on a clean Fiji installation;
4. representative fixtures checked manually and automatically;
5. verified authorship, license, citation, and release notes;
6. JAR attached to a tagged GitHub Release with a published SHA-256 checksum.
