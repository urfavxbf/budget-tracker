# Budget Tracker release process

## Release prerequisites

The app has package ID `com.urfavxbf.budgettracker`. For v0.4.1 it builds with Android compile/target SDK **36**, Java 17, AGP 8.13.2 / Gradle 8.13 (or CoGo AGP 9.3.1 / Gradle 9.6.1).

**Do not generate a new keystore for every build.** Keep the original release upload key backed up privately. The same signing certificate is required to upgrade sideloaded APKs while preserving on-device SQLite financial records. An APK signed with the release key **cannot** be installed as an update over an earlier debug-signed APK. Do not uninstall a debug APK containing important personal finance data; this app does not yet have a verified export/migration procedure.

## One-time signing key creation

Generate an upload key on a trusted, private machine; save it in a secure offline backup (and separately back up its passwords):

```bash
keytool -genkeypair -v \
  -keystore budget-tracker-upload.jks \
  -alias budget-tracker \
  -keyalg RSA -keysize 3072 -validity 10000
```

Create a Base64 string locally without committing the key or encoding:

```bash
base64 < budget-tracker-upload.jks | tr -d '\n'
```

In GitHub: **Settings → Secrets and variables → Actions → New repository secret**, configure:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Complete single-line Base64 contents of the persistent `.jks` file |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias (example: `budget-tracker`) |
| `ANDROID_KEY_PASSWORD` | Private key password |

**Never** put passwords or private keystore contents in commits, issues, PRs, workflow inputs or GitHub Release assets. A Base64 keystore is **not encryption**.

## Candidate verification and delivery

1. Merge release PR only after both GitHub Actions CI matrix jobs pass.
2. Complete the manual Android 16 and data-retention checklist in [TESTING_CHECKLIST.md](TESTING_CHECKLIST.md).
3. Ensure `versionName` and `versionCode` are updated once per published version. For this candidate they are `0.4.1` and `5`.
4. Add a signed Git tag from the tested `main` commit (example `v0.4.1`) and push it. The tag must match `versionName`.
5. GitHub Actions `Android Signed Release` rejects missing credentials, mismatched version tags and failed tests. It generates a **signed APK** and **signed AAB**, verifies APK/AAB signatures, computes SHA-256 and creates a GitHub Release with all three files. It will not upload to Google Play automatically.
6. Download the APK from GitHub Releases to install directly. Retain the signed AAB for Play Console internal testing and staged rollout once all policy/UX checks pass.

The workflow can also be rerun manually via **Actions → Android Signed Release → Run workflow**, specifying an **existing** `vX.Y.Z` tag. Avoid reusing a published versionCode or rewriting existing release tags.

## Local private release build (alternative)

Use the exact same four environment variables as the release workflow, where `ANDROID_KEYSTORE_PATH` is a filesystem path to the `.jks` file (not Base64). Then run:

```bash
gradle :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease
```

Never confuse a successful *unsigned* `assembleRelease` build with a distributable installable APK. Both APK and AAB must be cryptographically signed. Direct APK installs and Play App Signing follow different signing/distribution lifecycles.

## Non-automated release blockers

A passing GitHub build does not constitute physical-device QA or Play Store approval. Before public rollout, test all money and notification flows on real devices, review the Google Play Data safety form and privacy policy, and ensure the Play developer account is ready. **Do not upload or publish to Google Play as part of this automation.**
