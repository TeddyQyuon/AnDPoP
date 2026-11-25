# AnDPoP Android Release Guide

| Build setting | Value in 2.0.0 |
| --- | --- |
| Application ID | `app.andpop.android` |
| Version name and code | `2.0.0` and `8` |
| Product flavors | `play` (`PLAY_BUILD = true`, Google Play) and `foss` (`PLAY_BUILD = false`, GitHub/F-Droid style direct builds with optional Xposed hooks) |
| Minimum Android API | `33` (Android 13) |
| Compile and target API | `37` |
| Toolchain | JDK 21, Gradle `9.4.1`, AGP `9.2.1`, NDK `29.0.14206865`, CMake `3.22.1` |

---

## 1. Local Debug & Verification Builds

Install Android SDK Platform 37, NDK `29.0.14206865`, and CMake `3.22.1` in Android Studio, then run from the repository root (`./gradlew` on macOS/Linux, `.\gradlew.bat` on Windows PowerShell):

```powershell
# Google Play flavor debug APK + lint
.\gradlew.bat :app:assemblePlayDebug :app:lintPlayRelease

# FOSS flavor debug APK + lint
.\gradlew.bat :app:assembleFossDebug :app:lintFossDebug
```

Debug APKs are written to `app/build/outputs/apk/play/debug/` and `app/build/outputs/apk/foss/debug/`. Uninstalling a release or CI-signed APK (`adb uninstall app.andpop.android`) is required before switching signing keys.

---

## 2. Create Your Upload Key (Outside Git)

Never store keystores, `.jks`/`.keystore` files, or credentials inside this repository. Keep your upload keystore in a backed-up location outside the project directory (for example `~/keystores/andpop-upload.jks` or `%USERPROFILE%\keystores\andpop-upload.jks`).

Generate a 4096-bit RSA upload key valid for 10,000 days (~27 years):

```sh
keytool -genkeypair -v \
  -keystore ~/keystores/andpop-upload.jks \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -alias upload
```

On Windows PowerShell, replace `~/keystores/andpop-upload.jks` with `"$env:USERPROFILE\keystores\andpop-upload.jks"` and make sure `"$env:USERPROFILE\keystores"` exists first. Store the keystore and both passwords in a password manager and offline backup.

### Wire signing credentials in git-ignored `local.properties`

`app/build.gradle.kts` reads four signing properties from the root git-ignored `local.properties` file:

```properties
sdk.dir=...
RELEASE_STORE_FILE=/absolute/path/to/andpop-upload.jks
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_ALIAS=upload
RELEASE_KEY_PASSWORD=your-key-password
```

On Windows, escape backslashes in `.properties` files (for example `C:\\Users\\you\\keystores\\andpop-upload.jks`) or use forward slashes (`C:/Users/you/keystores/andpop-upload.jks`). When all four properties are non-blank, `:app:bundlePlayRelease`, `:app:assemblePlayRelease`, `:app:bundleFossRelease`, and `:app:assembleFossRelease` automatically sign their outputs; otherwise Gradle produces `*-unsigned.apk` / unsigned `.aab` artifacts for local verification.

---

## 3. Google Play App Signing

When creating the app in Play Console, opt in to **Play App Signing** (mandatory for new Android App Bundles):

- **App signing key:** Managed and safeguarded by Google in Google Play infrastructure and used to sign APKs served to end users from `.aab` uploads.
- **Upload key:** Your local `andpop-upload.jks` key used only to authenticate your `.aab` uploads to Play Console. If the upload key is ever lost or compromised, you can request an upload key reset in Play Console without losing your app identity or existing users.
- Reference: [Use Play App Signing](https://developer.android.com/studio/publish/app-signing).

---

## 4. Build and Verify Signed Release Bundles (`play` and `foss`)

Check that `applicationId` is `app.andpop.android` in [app/build.gradle.kts](../app/build.gradle.kts) and `versionCode` is strictly greater than any build previously uploaded to Play Console, then build and verify:

```powershell
# Google Play release AAB (R8 minification + resource shrinking enabled)
.\gradlew.bat :app:bundlePlayRelease :app:bundleFossRelease
```

Verify bundle signatures before uploading:

```sh
jarsigner -verify -verbose -certs app/build/outputs/bundle/playRelease/app-play-release.aab
```

For direct-distributed release APKs (`:app:assembleFossRelease`), verify APK v2/v3 signatures and 16 KB page-size alignment:

```sh
apksigner verify --print-certs app/build/outputs/apk/foss/release/app-foss-release.apk
zipalign -c -P 16 -v 4 app/build/outputs/apk/foss/release/app-foss-release.apk
```

Also verify every native `.so` inside `app/build/outputs/bundle/playRelease/app-play-release.aab` has `LOAD` alignment `0x4000` (16 KB) via `llvm-readelf -l`, and that zero Xposed artifacts (`libl2c_fcr_hook.so`, `META-INF/xposed/*`) exist in the `play` bundle.

---

## 5. `versionCode` Strategy

Maintain a strictly monotonic integer `versionCode` across all Play tracks (`Internal`, `Closed`, `Production`) in `app/build.gradle.kts` (`versionCode = 8` for `2.0.0`):

1. Increment `versionCode` by `1` for every build uploaded to Play Console (never reuse a code, even on a failed/discarded draft release).
2. Keep `foss` GitHub releases aligned at the same `versionCode` as the corresponding `play` tag (for example `2.0.0` -> `8`, `2.0.1` -> `9`, `2.1.0` -> `10`), or reserve `foss`-only hotfixes using a clear `versionName` suffix while keeping Play `versionCode` monotonic.
3. Wait to set `PrivacyLinks.PRIVACY_POLICY_URL` and `PrivacyLinks.ACCOUNT_DELETION_URL` before tagging `versionCode = 9` (or rebuild `versionCode = 8` prior to your first Play upload once those hosted URLs exist).

---

## 6. Device & Account Verification Before Public Rollout

- Complete the physical phone + AirPods checklist in [play/DEVICE_TEST_PLAN.md](play/DEVICE_TEST_PLAN.md).
- Configure account flows per [ACCOUNT_SETUP.md](ACCOUNT_SETUP.md).
- Host [play/PRIVACY_POLICY.md](play/PRIVACY_POLICY.md) and its companion account deletion page over HTTPS, wire the URLs in `PrivacyLinks.kt`, and complete the Play Console declarations in [play/PLAY_CONSOLE.md](play/PLAY_CONSOLE.md).
- For personal Play developer accounts created on or after November 13, 2023, run a closed test with at least 12 opted-in testers continuously for 14 days before applying for production access ([Play Console app testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)).
