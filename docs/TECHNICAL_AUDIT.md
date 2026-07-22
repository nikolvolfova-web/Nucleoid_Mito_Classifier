# Technical audit: Nucleoid Mito Classifier v0.2.0

Audit date: 2026-07-22  
Audited repository state: `main` at commit `8f96c75f5e8ce8445e572924feadd53777dff796`  
Release reviewed: `v0.2.0`

## Scope and limitations

This follow-up audit reviewed the current Java source, Maven configuration, GitHub Actions workflow, documentation, citation metadata, licences, and the contents of the reproducible example archive.

The audit also compared the current repository with the findings recorded for `v0.1c`. It did not independently rerun the Fiji analysis or reproduce the numerical reference results. Statements about successful Fiji execution and numerical validation therefore rely on the validation record documented in `README.md`, `CHANGELOG.md`, and `examples/README.md`.

## Result

No unresolved blocking finding from the original `v0.1c` audit was identified in the current `v0.2.0` source and repository metadata.

Version `v0.2.0` resolves the original findings concerning calibration validation, supported dimensionality, licensing, StarDist input handling, destructive ImageJ cleanup, zero-object CSV schema, and reproducible Maven builds. The repository also contains a redistributable example dataset with documented expected results.

The remaining findings mainly concern provenance logging, input/output handling, reporting accuracy, automated regression testing, Java 8 compatibility assurance, and maintainability. They should be addressed in future development, but they do not invalidate the documented `v0.2.0` release results.

## Status of findings from the v0.1c audit

| Original finding | Status in v0.2.0 | Evidence or remaining work |
| --- | --- | --- |
| Calibration unit was not validated | Resolved | Inputs are rejected unless the unit is recognized as micrometres and both pixel dimensions are finite and positive. |
| Supported dimensionality was undefined | Resolved | Inputs are rejected unless they contain exactly two channels, one Z slice, and one time point. |
| Ownership and licensing were unresolved | Resolved | The repository contains `LICENSE`, `CITATION.cff`, author metadata, ORCID, affiliations, and a separate licence for the example data. |
| Debug mode could prevent StarDist input from being shown | Resolved | The StarDist input is shown whenever it has no image window, independently of debug mode. The documented validation reports matching results with debug mode enabled. |
| Existing ImageJ state could be modified destructively | Resolved | Cleanup tracks plugin-created images and restores pre-existing ROI Manager content. The documented validation includes unrelated-window and ROI preservation checks. |
| Empty per-object CSV had a different schema | Resolved | The zero-object path writes the same documented 63-column header with no data rows. |
| Build was not reproducible | Resolved for the documented build | Maven dependencies and build plugins are versioned, GitHub Actions runs `mvn clean verify`, and the workflow publishes the generated JAR as an artifact. |
| Analysis log omitted fallback settings | Open | Several parameters that can influence mask generation or QC are still absent from the analysis log. See High priority finding 1. |
| Derived-output filename filtering was fragile | Open | Matching remains case-sensitive and based on broad substrings. See High priority finding 2. |
| Skipped files counted as processed | Open | `processOneImage` returns without a status and the caller increments `processed` after validation skips. See Medium priority finding 3. |
| Summary fractions included filtered objects | Open or requires explicit definition | The denominator remains all detected C1 objects. See Medium priority finding 5. |
| No regression-test dataset | Partially resolved | A documented fixture and reference archive now exist, but comparison remains manual. See Medium priority finding 4. |
| Version was duplicated in code and filenames | Open | Version text remains embedded in several source and build locations. See Medium priority finding 6. |

## Blocking

No blocking finding was identified within the stated audit scope.

## High priority

### 1. The analysis log does not record every result-affecting setting

**Problem:** `writeSettingsLog` records the main thresholds and several StarDist settings but omits values used by strict, local Phansalkar, and soft mitochondrial-mask fallbacks. It also omits QC drawing settings and `debugMode`.

The missing analytical settings include at least:

- `mitoStrictRolling`;
- `mitoStrictSigma`;
- `mitoStrictMethod`;
- `mitoStrictDoOpen`;
- `mitoLocalRolling`;
- `mitoLocalSigma`;
- `mitoLocalRadius`;
- `mitoSoftRolling`;
- `mitoSoftSigma`;
- `mitoSoftMethod`.

**Why it matters:** when a fallback is triggered, the saved log is not sufficient to reconstruct the exact mask-generation configuration. This weakens reproducibility and makes unexpected results harder to investigate.

**How to fix:** write every field in `Settings` that can affect analysis, classification, output generation, QC rendering, or diagnostic behaviour. Consider centralizing setting serialization so that adding a new setting cannot silently omit it from the log.

**File:** `src/main/java/Nucleoid_Mito_Classifier_v0_2_0.java`.

**How to verify:** run an image that triggers each fallback path, compare every displayed setting with the saved `*-Analysis_Log.txt`, and confirm that no configurable field is missing.

### 2. Derived-output filtering remains case-sensitive and overly broad

**Problem:** input filtering calls `isDerivedOutputName(name)`, but the method uses case-sensitive `String.contains` checks on the original filename. Some patterns, such as `-Nucleoids`, are broad enough to occur in a legitimate biological filename. The interface recommends a separate output folder but does not enforce it.

**Why it matters:** a legitimate input may be skipped, or an output with different capitalization may be accepted as a future input. Reprocessing derived images can produce misleading results and unnecessary output growth.

**How to fix:** require different canonical input and output directories, or ask for explicit confirmation when they are the same. Replace broad substring matching with case-insensitive, exact suffix patterns for files actually generated by the plugin.

**File:** `src/main/java/Nucleoid_Mito_Classifier_v0_2_0.java`.

**How to verify:** test legitimate filenames containing words such as `nucleoids`, generated output names with varied capitalization, and attempts to select the same directory for input and output. Confirm that valid source images are processed and derived outputs are rejected deterministically.

## Medium priority

### 3. Skipped inputs are reported as processed

**Problem:** `processOneImage` returns normally when an image cannot be opened or fails channel, Z, time, or calibration validation. The caller then increments `processed`.

**Why it matters:** the final message can overstate how many images were successfully analysed, which may conceal invalid inputs in a batch.

**How to fix:** return an explicit status such as `PROCESSED`, `SKIPPED`, or `FAILED`, maintain separate counters, and log a final summary for each category.

**File:** `src/main/java/Nucleoid_Mito_Classifier_v0_2_0.java`.

**How to verify:** run a batch containing one valid image, one invalid-dimension image, one invalid-calibration image, and one unreadable file. Confirm exact processed, skipped, and failed totals.

### 4. The reproducible example is not checked automatically

**Problem:** the repository contains a useful example fixture and reference output, but the GitHub Actions workflow only compiles and packages the plugin. It does not compare analytical outputs against the reference.

**Why it matters:** compilation can succeed even when a code change alters object counts, classifications, the CSV schema, measurements, or resource handling.

**How to fix:** retain the documented manual Fiji release check and, where technically feasible, add a headless or otherwise automated regression check for deterministic outputs. At minimum, automate checks that do not require the full GUI workflow, such as CSV schema validation, checksum validation, and isolated pure-logic tests.

**Files:** `.github/workflows/maven-build.yml`, future files under `src/test/`, and potentially scripts under a dedicated test directory.

**How to verify:** deliberately change a known expected count, column name, or deterministic calculation and confirm that CI fails with a clear message.

### 5. The denominator of class fractions is not explicit in the output schema

**Problem:** `Colocalized_FractionPct`, `Borderline_FractionPct`, and `OutOfMito_FractionPct` use all detected C1 objects as the denominator, including filtered objects. The intended scientific interpretation is not stated in the CSV header.

**Why it matters:** readers may assume that percentages refer only to objects passing the configured filters. When filtered objects exist, the three reported class fractions do not sum to 100%.

**How to fix:** document the denominator explicitly, or provide both fractions of all detected objects and fractions of analysed non-filtered objects. Preserve backward compatibility by adding clearly named columns rather than silently changing existing meanings.

**Files:** `src/main/java/Nucleoid_Mito_Classifier_v0_2_0.java`, `README.md`, and `CHANGELOG.md` if the output schema changes.

**How to verify:** use a fixture with filtered objects and independently calculate every reported percentage.

### 6. Java 8 compatibility is not directly exercised in CI

**Problem:** the Maven configuration targets Java 8 bytecode, but GitHub Actions builds only with Java 21. Using `source` and `target` alone does not prevent accidental use of APIs introduced after Java 8.

**Why it matters:** a future change could compile successfully in CI yet fail in a Java 8 runtime, despite the documented compatibility target.

**How to fix:** add a Java 8 build job or matrix entry, or build with an appropriate Java API compatibility mechanism. Keep the existing Java 21 build if it represents the tested Fiji environment. Any workflow change must be coordinated with the required branch-protection check name.

**Files:** `pom.xml` and `.github/workflows/maven-build.yml`.

**How to verify:** build from a clean checkout under both Java 8 and Java 21 and install the release candidate in the intended Fiji environment before release.

### 7. Version information remains duplicated

**Problem:** version `0.2.0` or `v0.2.0` is embedded in the Java class name, source filename, log output, CSV values, Maven version, final JAR name, and manifest entries.

**Why it matters:** preparing a release requires coordinated edits in many locations and creates a risk of inconsistent metadata or filenames.

**How to fix:** plan a separately tested refactor that reads the implementation version from the JAR manifest where possible and uses a version-neutral implementation class. Do not change the current plugin entry point without verifying Fiji installation and menu discovery.

**Files:** `src/main/java/Nucleoid_Mito_Classifier_v0_2_0.java`, `pom.xml`, and release documentation.

**How to verify:** build a future release candidate and confirm that the displayed version, log, CSV, JAR filename, Maven coordinates, manifest, README, citation metadata, tag, and release title all agree.

## Low priority

### 8. The reference archive contains macOS metadata entries

**Problem:** `examples/example_output_reference.zip` contains `__MACOSX/` and `._*` entries in addition to the intended reference outputs.

**Why it matters:** these files do not affect scientific results, but they add noise and reduce cross-platform cleanliness.

**How to fix:** recreate the ZIP without Finder metadata while preserving the verified reference files and their internal checksums. Record the archive replacement in `CHANGELOG.md`; do not alter analytical outputs merely for cosmetic consistency.

**File:** `examples/example_output_reference.zip`.

**How to verify:** list the new archive contents, confirm that no `__MACOSX/` or `._*` entries remain, verify the included `SHA256SUMS.txt`, and compare the scientific files byte-for-byte with the retained reference set.

## Repository documentation and security status

The following repository-level controls and documents are present in the audited `main` branch:

- BSD 3-Clause software licence;
- separate CC BY 4.0 example-data licence;
- `README.md`, `CHANGELOG.md`, `CITATION.cff`, `CONTRIBUTING.md`, and `SECURITY.md`;
- Maven build workflow with read-only repository permissions;
- Dependabot configuration for Maven and GitHub Actions;
- branch protection requiring the Maven build before merging;
- private vulnerability reporting, dependency graph, Dependabot alerts, and Dependabot security updates, as configured in the repository settings.

Pull request and issue templates are not present in the audited tree. Adding project-specific templates is recommended to make the existing contribution, validation, privacy, and security requirements visible at the point where contributors submit changes or reports.

## Recommended next implementation order

1. Complete the analysis settings log.
2. Make input/output directory handling and derived-output filtering deterministic.
3. Report processed, skipped, and failed inputs separately.
4. Add pull request and issue templates.
5. Add a Java 8 CI compatibility check without disrupting the protected required check.
6. Introduce automated regression checks incrementally.
7. Clarify class-fraction denominators before any output-schema change.
8. Refactor version handling only as a separately validated release task.

## Recommended release gate for a future version

A future stable release should require:

1. a clean Maven build from the tagged commit;
2. successful required GitHub Actions checks;
3. installation and functional testing of the workflow-generated JAR in the intended Fiji environment;
4. comparison with the reproducible example, including counts, classifications, the 63-column schema, numerical measurements, logs, and QC outputs;
5. targeted validation of every intentionally changed analytical path;
6. verified consistency across `pom.xml`, plugin version output, `README.md`, `CHANGELOG.md`, `CITATION.cff`, the Git tag, GitHub Release, checksums, and Zenodo metadata;
7. the release JAR and `SHA256SUMS.txt` attached to the GitHub Release;
8. confirmed licences and redistribution permission for every included fixture.
