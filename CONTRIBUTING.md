# Contributing to Nucleoid Mito Classifier

Thank you for your interest in improving Nucleoid Mito Classifier.

This repository contains research software for Fiji/ImageJ. Contributions must preserve technical correctness, reproducibility, data safety, and clear documentation.

## Ways to contribute

You may contribute by:

* reporting a reproducible software bug;
* proposing a new feature;
* improving documentation;
* improving installation or build instructions;
* adding or improving automated tests;
* proposing a scientifically justified methodological change;
* contributing a legally redistributable test image or validation dataset;
* reviewing Pull Requests.

Security vulnerabilities and reports containing sensitive information must not be submitted as public Issues. See `SECURITY.md`.

## Before opening an Issue

Search existing Issues before creating a new one.

For software bugs, include:

* plugin version;
* Fiji/ImageJ2 version;
* ImageJ1 version;
* Java version;
* operating system;
* exact steps needed to reproduce the problem;
* expected behaviour;
* observed behaviour;
* relevant messages from the ImageJ log;
* relevant parts of `Analysis_Log.txt`;
* whether the problem occurs with the reproducible example dataset.

When possible, include a minimal reproducible example that can legally and ethically be shared.

Do not upload:

* patient-identifiable data;
* confidential or unpublished data without permission;
* access tokens, passwords, API keys, SSH keys, or credentials;
* personal information;
* internal network paths or sensitive institutional information;
* microscopy data that you are not authorised to redistribute.

## Development setup

### Requirements

* Git;
* JDK 8 or later;
* Apache Maven;
* Fiji/ImageJ with the dependencies documented in `README.md`.

### Clone the repository

```bash
git clone https://github.com/nikolvolfova-web/Nucleoid_Mito_Classifier.git
cd Nucleoid_Mito_Classifier
```

### Build the project

```bash
mvn clean package
```

The generated JAR is written to the `target/` directory.

### Run automated tests

```bash
mvn clean test
```

All available automated tests must pass before a Pull Request is merged.

## Branch workflow

Do not work directly on `main`.

Create a separate branch from the latest `main`:

```bash
git switch main
git pull
git switch -c <BRANCH_NAME>
```

Recommended branch names:

```text
fix/<short-description>
feature/<short-description>
docs/<short-description>
test/<short-description>
release/<version>
```

Examples:

```text
fix/preserve-image-calibration
feature/add-summary-output
docs/update-installation
test/add-csv-schema-test
```

Keep each branch focused on one logical change.

## Commit messages

Use short, clear, imperative commit messages.

Examples:

```text
Fix zero-object CSV schema
Add input calibration test
Update Fiji installation instructions
Document validation limitations
```

Avoid vague commit messages such as:

```text
Update
Changes
Fix stuff
New version
```

## Pull Requests

All routine changes should be submitted through a Pull Request targeting `main`.

A Pull Request should explain:

* what was changed;
* why the change is needed;
* which files were modified;
* how the change was tested;
* whether numerical results changed;
* whether object classifications changed;
* whether the CSV schema changed;
* whether reference outputs changed;
* whether documentation or citation metadata must be updated.

The Maven build must pass before merging.

Resolve all review comments and discussions before merging.

## Scientific and algorithmic changes

Changes affecting segmentation, measurement, classification, thresholds, filtering, preprocessing, masks, or output values require additional documentation.

The Pull Request must include:

* the scientific or technical reason for the change;
* the previous behaviour;
* the new behaviour;
* the affected parameters;
* a comparison of previous and new outputs;
* an explanation of whether the change is backward compatible;
* updated tests;
* updated reference outputs when applicable;
* an update to `CHANGELOG.md`;
* an update to `README.md` when user-visible behaviour changes.

Do not change scientific defaults without documenting the reason and expected consequences.

Do not describe a change as biologically validated unless it has been evaluated using a documented representative validation dataset and predefined metrics.

## Output compatibility

Before merging a change, verify whether it affects:

* CSV column names;
* CSV column order;
* number formats;
* classification labels;
* output filenames;
* output directory structure;
* QC image content;
* analysis logs;
* plugin version fields;
* installation instructions.

Breaking output changes must be clearly documented and should normally require a new minor or major version.

## Testing with the reproducible example

When a change can affect analysis results, run the plugin using the documented example input.

Compare at least:

* total detected object count;
* classification counts;
* number and order of CSV columns;
* object identifiers;
* numerical output values;
* mitochondrial mask status and method;
* zero-object behaviour where relevant.

The current reference results are documented in `README.md` and `examples/README.md`.

Expected differences must be explicitly documented. Unexpected differences must be investigated before merging.

## Fiji testing

A workflow-generated JAR intended for release must be installed and tested in Fiji.

Before a release, verify:

* the plugin appears in the expected Fiji menu;
* the plugin starts successfully;
* the documented example input runs successfully;
* expected CSV and QC outputs are created;
* unrelated ImageJ windows remain open;
* pre-existing ROI Manager content is preserved;
* no unexpected save prompts appear;
* the output plugin version matches the intended release version.

## Documentation changes

Update documentation whenever a contribution changes:

* installation;
* usage;
* inputs;
* outputs;
* dependencies;
* parameters;
* scientific interpretation;
* limitations;
* licensing;
* citation;
* supported versions.

Relevant files may include:

```text
README.md
CHANGELOG.md
CITATION.cff
examples/README.md
docs/TECHNICAL_AUDIT.md
CONTRIBUTING.md
SECURITY.md
```

Do not invent test results, DOI values, ORCID identifiers, affiliations, licences, or author information.

## Citation metadata

Changes to `CITATION.cff` must preserve valid CFF/YAML formatting.

After editing, verify:

* indentation;
* list markers;
* version;
* release date;
* author name;
* ORCID;
* affiliations;
* licence;
* repository URL;
* DOI identifiers.

Never copy formatted rich text into `CITATION.cff`. Use plain text and validate the file before creating a Release.

## Data contributions

Only contribute images or datasets that may legally and ethically be redistributed.

Before adding data, confirm:

* ownership;
* redistribution permission;
* absence of personal identifiers;
* absence of confidential information;
* appropriate licence;
* complete metadata;
* documented channel order and calibration;
* checksums for distributed files.

Large datasets should not normally be committed directly to the Git repository. They should be archived in an appropriate research-data repository and linked using a persistent identifier.

Small reproducible test fixtures may be stored in `examples/` when their redistribution rights are documented.

## Dependencies

Do not add a new dependency unless it is necessary.

A Pull Request adding a dependency must explain:

* why it is needed;
* its licence;
* its version;
* its compatibility with Fiji/ImageJ;
* its compatibility with the configured Java bytecode target;
* whether Fiji users must enable another update site.

Avoid unpinned or unnecessary dependencies.

## Security and secrets

Never commit:

* passwords;
* access tokens;
* API keys;
* private SSH keys;
* credentials;
* private certificates;
* confidential configuration files.

If a secret is committed:

1. revoke or rotate it immediately;
2. remove it from the repository;
3. assess whether Git history must be cleaned;
4. report the incident using the private process described in `SECURITY.md`.

Deleting a secret from the latest commit is not sufficient if it remains in Git history.

## Licences

Software contributions are accepted under the repository's BSD 3-Clause License.

Example data and reference outputs may use a separate licence. Do not assume that the software licence applies to datasets.

By submitting a contribution, you confirm that you have the right to provide it under the applicable repository licence.

## Release preparation

A release Pull Request should verify consistency across:

* source-code version;
* Java class and filename;
* Maven version;
* generated JAR filename;
* JAR manifest;
* README;
* CHANGELOG;
* `CITATION.cff`;
* Git tag;
* GitHub Release title and notes;
* Zenodo metadata.

A release must not be published until:

* the Maven build passes;
* the final workflow-generated JAR has been checked;
* required Fiji testing is complete;
* release assets are correct;
* checksum files match the attached assets;
* citation metadata are valid.

## Questions

Use a GitHub Discussion or Issue for general development questions when the content is suitable for public discussion.

Do not use public Issues for vulnerabilities, credentials, personal data, confidential images, or other sensitive information.
