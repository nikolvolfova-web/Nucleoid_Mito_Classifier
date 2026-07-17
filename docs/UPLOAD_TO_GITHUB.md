# Upload this project to GitHub using a web browser

## Create the repository

1. Sign in to GitHub.
2. Select **+ → New repository**.
3. Recommended repository name: `nucleoid-mito-classifier`.
4. Suggested description: `Fiji/ImageJ plugin for overlap-based classification of nucleoid objects relative to a mitochondrial mask.`
5. Select **Private** for the initial review.
6. Do not initialize the repository with a README, `.gitignore`, or license because those files are already included in this package.
7. Create the repository.

## Upload the source package

1. Open the new empty repository.
2. Select **uploading an existing file** or **Add file → Upload files**.
3. Unzip `Nucleoid-Mito-Classifier-repository.zip` on the Mac.
4. Upload the contents inside the extracted folder, not the outer folder itself.
5. Confirm that `README.md`, `pom.xml`, `src/`, and `docs/` appear at the repository root.
6. Do not upload the compiled JAR into the source tree.
7. Commit message: `Initial project import`.

## Add the binary later as a release asset

After review and testing:

1. Create a tag such as `v0.1c`.
2. Create a GitHub Release from that tag.
3. Upload `Nucleoid_Mito_Classifier_v0_1c.jar` and `SHA256SUMS.txt` as release assets.
4. Mark the release as a pre-release until the blocking audit findings are resolved.

## Final checks before making the repository public

- No confidential images, patient data, credentials, or raw analysis outputs.
- Author and ownership information confirmed.
- License selected and added.
- Calibration and dimensionality issues resolved.
- Installation tested on a clean Fiji installation.
- README and release notes match actual behavior.
