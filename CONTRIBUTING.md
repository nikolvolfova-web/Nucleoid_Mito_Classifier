# Contributing to Nucleoid Mito Classifier

Thank you for your interest in improving Nucleoid Mito Classifier. Contributions that improve correctness, reproducibility, documentation, testing, or compatibility with Fiji/ImageJ are welcome.

## Before you begin

Please check the existing issues and pull requests before starting work. For a substantial change, open an issue first so that the proposed scope and scientific implications can be discussed before implementation.

By contributing, you agree that your contribution may be distributed under the repository's BSD 3-Clause License.

## Reporting bugs

When reporting a bug, include enough information to reproduce it whenever possible:

- a clear description of the observed and expected behaviour;
- the plugin version or commit used;
- Fiji/ImageJ, ImageJ1, Java, and operating-system versions;
- relevant input-image properties, including dimensions, channel order, and spatial calibration;
- the analysis parameters used;
- relevant ImageJ log messages or stack traces;
- a minimal anonymized and redistributable example, if one can be shared legally and ethically.

Do not submit patient-identifiable, confidential, or unpublished microscopy data without permission.

## Reporting security vulnerabilities

Do not disclose suspected security vulnerabilities in a public issue. Use GitHub's **Private vulnerability reporting** feature in the repository's **Security** section instead.

Do not include passwords, access tokens, private keys, credentials, or other secrets in an issue, pull request, commit, or shared log.

## Development requirements

To build the project, you need:

- JDK 8 or later;
- Apache Maven.

Functional validation also requires Fiji/ImageJ with the dependencies documented in [`README.md`](README.md), including the required StarDist update sites.

## Setting up a contribution

1. Fork the repository on GitHub.
2. Clone your fork:

   ```bash
   git clone https://github.com/<YOUR_GITHUB_USERNAME>/Nucleoid_Mito_Classifier.git
   cd Nucleoid_Mito_Classifier
   ```

3. Create a focused branch from the current `main` branch:

   ```bash
   git switch main
   git pull --ff-only origin main
   git switch -c <TYPE>/<SHORT-DESCRIPTION>
   ```

   Examples: `fix/calibration-validation`, `docs/installation-notes`, or `test/csv-schema`.

4. Make the smallest coherent change that resolves the issue.
5. Do not commit generated build output, routine analysis output, large raw datasets, or local IDE files.

## Building and checking the project

Run the Maven build from the repository root:

```bash
mvn clean package
```

The build must complete successfully before a pull request is submitted. The expected JAR path is documented in [`README.md`](README.md).

The repository does not currently provide a fully automated image-analysis regression test. For changes that can affect scientific results or Fiji behaviour, also perform relevant manual validation in the intended Fiji environment. When applicable:

- use the reproducible dataset in [`examples/`](examples/);
- compare object counts, classifications, the 63-column CSV schema, numerical measurements, logs, and QC outputs with the documented reference;
- test zero-object handling if CSV creation or object processing changes;
- verify preservation of unrelated ImageJ windows and pre-existing ROI Manager content if resource handling changes;
- record the Fiji/ImageJ, Java, and operating-system versions used.

Do not update reference outputs merely to make an unexplained result change appear successful. Explain and justify any intentional scientific-output change in the pull request.

## Scientific and implementation principles

Contributions must preserve the documented methodological principle unless a deliberate methodological change has first been discussed and clearly documented:

- C1 objects are segmented independently of the C3 mitochondrial mask;
- morphology and C1 intensity are measured from the complete C1 ROI;
- the C3 mask is used only for overlap-based classification and must not alter C1 ROI geometry.

Changes affecting segmentation, calibration, measurements, thresholds, classification, filtering, CSV columns, or QC outputs must describe their scientific effect and any compatibility implications.

## Documentation and data

- Keep implementation, `README.md`, `CHANGELOG.md`, examples, and citation or release metadata consistent when a change affects them.
- Use clear units and distinguish pixel-based from calibrated measurements.
- Do not invent validation results, DOI values, author details, affiliations, funding information, or other metadata.
- Small test fixtures under `examples/` must be anonymized, redistributable, and accompanied by appropriate provenance and licensing information.
- Software source contributions are covered by the BSD 3-Clause License. Example data and reference outputs are separately licensed as described in [`examples/LICENSE.md`](examples/LICENSE.md).

## Commits

Write concise, imperative commit messages that describe one logical change. For example:

```text
Validate anisotropic pixel calibration
```

Avoid mixing unrelated formatting, documentation, and functional changes in one commit or pull request.

## Pull requests

Open the pull request against `main`. In the description, include:

- the problem and the proposed solution;
- the related issue, if any;
- whether scientific results or output formats can change;
- the checks and manual validation performed;
- the test environment, when Fiji validation was required;
- documentation, example, or reference-output changes;
- any known limitations or follow-up work.

The required GitHub checks, including the Maven build, must pass. Address review comments with additional commits; do not force-push during active review unless it is necessary and clearly communicated.

Repository maintainers may request changes or decline contributions that cannot be reproduced, conflict with the documented methodology, introduce unsupported data, or lack sufficient validation.

## Release and version metadata

Do not change the project version, create a tag or release, update a DOI, or modify archived reference metadata unless the contribution is specifically preparing an agreed release. Release metadata must remain consistent across the Maven configuration, plugin naming, `README.md`, `CHANGELOG.md`, `CITATION.cff`, GitHub Release, and Zenodo record.

## Questions

For general questions or proposed improvements, open a GitHub issue. Use private vulnerability reporting for security-sensitive matters.
