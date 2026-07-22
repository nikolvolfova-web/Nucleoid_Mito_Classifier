# Security Policy

## Supported versions

Security updates are currently considered for the latest stable release.

| Version        | Supported |
| -------------- | --------- |
| `0.2.0`        | Yes       |
| `0.1c`         | No        |
| Older versions | No        |

Users are encouraged to install the latest stable release from the GitHub Releases page.

This support policy may be updated when a new stable version is published.

## Reporting a security vulnerability

Please do not report security vulnerabilities through a public GitHub Issue, Discussion, Pull Request, or social-media post.

Use GitHub's private vulnerability reporting feature:

1. Open the repository's **Security** page.
2. Open **Advisories**.
3. Select **Report a vulnerability**.
4. Provide the requested details privately.

Do not include patient-identifiable data, confidential microscopy images, credentials, access tokens, private keys, personal information, or other sensitive material unless a secure exchange method has first been agreed upon.

## Information to include

A useful report should include:

* the affected plugin version;
* the affected file, component, dependency, or workflow;
* Fiji/ImageJ version;
* Java version;
* operating system;
* a clear description of the vulnerability;
* steps needed to reproduce it;
* the possible impact;
* whether the problem requires a specially prepared input;
* any known workaround;
* relevant logs with sensitive information removed.

Where possible, include a minimal proof of concept that does not contain confidential data or credentials.

## Examples of security issues

Security-related reports may include:

* unintended reading, overwriting, or deletion of files outside the selected input or output location;
* unsafe handling of filenames, paths, archives, or symbolic links;
* execution of unintended code or commands;
* use of a dependency with a known exploitable vulnerability;
* exposure of credentials or sensitive information;
* unsafe GitHub Actions permissions;
* manipulation of release artifacts or checksums;
* a vulnerability that allows analysis outputs to be altered without an appropriate warning;
* disclosure of private data through logs, generated files, or error messages.

## Issues that are normally not security vulnerabilities

The following should generally be reported through a normal GitHub Issue, provided that no sensitive information is included:

* incorrect segmentation;
* incorrect classification;
* unexpected object counts;
* numerical differences in outputs;
* Fiji compatibility problems;
* unclear documentation;
* installation problems;
* feature requests;
* requests for additional input formats;
* scientific or methodological disagreements.

A scientific correctness problem may still need to be reported privately when public disclosure could expose confidential data or create a significant risk for current users.

## Responsible disclosure

Please allow the maintainer a reasonable opportunity to investigate and address a reported vulnerability before publicly disclosing technical details.

The maintainer may:

* request additional information;
* reproduce and assess the report;
* prepare a private fix;
* create a GitHub security advisory;
* publish a corrected release;
* request a CVE when appropriate;
* acknowledge the reporter, subject to the reporter's preference.

Submitting a report does not guarantee that it will be classified as a security vulnerability.

## Handling sensitive data

Do not attach the following to a vulnerability report unless a secure transfer method has been agreed upon:

* patient-identifiable images or metadata;
* confidential research data;
* unpublished data without redistribution permission;
* credentials;
* passwords;
* access tokens;
* API keys;
* private SSH keys;
* private certificates;
* institutional network information.

Redact sensitive paths, usernames, hostnames, and identifiers from screenshots and logs whenever they are not necessary to reproduce the vulnerability.

## Compromised credentials or secrets

If a credential or secret has been committed to the repository:

1. revoke or rotate it immediately;
2. do not rely only on deleting the latest file or commit;
3. determine whether the secret remains in Git history;
4. remove it from the repository and history when necessary;
5. review logs and services for possible unauthorised use;
6. report the incident privately.

Never publish the compromised secret in a public Issue as evidence.

## Dependencies

Potential vulnerabilities in Maven dependencies, Fiji components, StarDist, TensorFlow, CSBDeep, ImageJ, or GitHub Actions should be reported with the affected version and, where available, the relevant advisory or CVE identifier.

A vulnerability in an upstream dependency may need to be addressed by updating the dependency, documenting a workaround, or waiting for an upstream fix.

## Research-use limitation

Nucleoid Mito Classifier is research software.

The existence of this security policy does not imply clinical, diagnostic, regulatory, or biological validation. Users remain responsible for reviewing QC outputs and validating the method for their own data and intended research use.

## Public discussion after resolution

After a vulnerability has been fixed and users have had a reasonable opportunity to update, the maintainer may publish:

* a GitHub security advisory;
* release notes;
* a corrected version;
* mitigation instructions;
* credit to the reporter, when permission has been given.

Sensitive information that is not needed for public understanding should remain private.
