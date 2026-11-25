# AnDPoP 2.0 (`versionCode 8`) — Google Play Release Checklist

**Target application ID:** `app.andpop.android`  
**Target SDK / Min SDK:** Android 17 (`compileSdk = 37`, `targetSdk = 37`) / Android 13 (`minSdk = 33`)  
**Release artifacts verified:** `app/build/outputs/bundle/playRelease/app-play-release.aab` (`9,752,358` bytes) and `app/build/outputs/apk/play/release/app-play-release-unsigned.apk` (`8,451,436` bytes)

---

## Verification Matrix (`PASS` / `NEEDS-USER`)

| Area | Requirement | Status | Evidence |
| --- | --- | --- | --- |
| Baseline & Audit | Security, permission, Xposed, billing, storage, and localization inventory | **PASS** | [AUDIT.md](AUDIT.md) (Sections 1–11), committed on `feature/play-phase-0-audit` |
| 2.0 Design System | Bundled Roboto Flex + OFL license, 7 color themes, `AndpopUi` components | **PASS** | `res/font/roboto_flex.ttf`, `assets/licenses/RobotoFlex-OFL.txt`, `AndpopPalette.kt`, `AndpopUi.kt` |
| Account & Onboarding | Single Welcome (`"I already have an account"`), guest default, `Screen.Account` + predictive back, per-permission Allow buttons | **PASS** | `OnboardingScreen.kt`, `PermissionsPage.kt`, `AppNavGraph.kt`; verified by `PlayReadinessRegressionTest` |
| Device & App Settings | Regrouped All device settings + disconnect dialog, 7-tap Developer options (hidden in `play`), Equalizer sliders, Segmented speeds, 840 dp two-pane layout | **PASS** | `AirPodsSettingsScreen.kt`, `DeveloperOptionsScreen.kt`, `EqualizerScreen.kt`, `AccessibilitySettingsScreen.kt`, `NavigationRoot.kt` |
| System Surfaces | Single informative connected notification + silent service channel, Glance 1.1.1 widgets, Compose overlays, Quick Settings tile | **PASS** | `AirPodsService.kt`, `AirPodsNotificationFormatter.kt`, `BatteryWidget.kt`, `NoiseControlWidget.kt`, `PopupWindow.kt`, `IslandWindow.kt`, `AirPodsQSService.kt` |
| `play` Flavor Separation | `PLAY_BUILD = true`, `src/play` no-op Xposed stubs, `libl2c_fcr_hook.so` & `META-INF/xposed/*` excluded, Hearing Aid routes gated out, Billing removed | **PASS** | `app/build.gradle.kts`, `app/src/play/`, ZIP scan of `app-play-release.aab` (`0` Xposed/hook entries) |
| Manifest & Security | Protected permissions removed in `play`, `AirPodsService` `exported="false"`, `RECEIVER_NOT_EXPORTED`, `network_security_config.xml` (no cleartext), vault/settings excluded from backup, R8 strips `Log.v/d/i` | **PASS** | `app/src/play/AndroidManifest.xml`, `network_security_config.xml`, `backup_rules.xml`, `data_extraction_rules.xml`, `app/proguard-rules.pro` |
| Localization | Leftover LibrePods/Linux strings scrubbed across locales; `andpop_*` strings in resources | **PASS** | `values-*/strings.xml`, `andpop_v2_strings.xml`, `andpop_strings.xml`, translation plan in [AUDIT.md](AUDIT.md) |
| Lint | `:app:lintFossDebug` & `:app:lintPlayRelease` zero errors | **PASS** | Both tasks exit `0` (`0` errors) |
| Automated Tests | Existing 16 + 6 new Play readiness tests (`AppearanceRegressionTest`, `DashboardRegressionTest`, `ScreenshotTour`, `PlayReadinessRegressionTest`) pass on both `foss` and `play` | **PASS** | `:app:connectedFossDebugAndroidTest` (`22/22`) & `:app:connectedPlayDebugAndroidTest` (`22/22`) on `Pixel_8_Pro` API 37 |
| R8 Release & 200% Font Scale | R8 `playRelease` launches, navigates Welcome -> Privacy at 100% & 200% font scale without crash | **PASS** | Debug-signed R8 `playRelease` emulator smoke test (`0` crashes after adding `RoomDatabase` rule in `app/proguard-rules.pro`) |
| 16 KB Page Size | All `.so` files in `app-play-release.aab` (`arm64-v8a`, `x86_64`) have `LOAD` align `0x4000`; release APKs pass `zipalign -P 16` | **PASS** | `llvm-readelf -l` (`0x4000` on all 4 `.so`s) and `zipalign -c -P 16 -v 4` (`Verification successful`) |
| Store Listing & Graphics | Title, descriptions, release notes, policy questionnaire drafts, FGS video script, Health Apps analysis, 512px icon, 1024×500 feature graphic, 6× `1080×2340` screenshots | **PASS** | [LISTING.md](LISTING.md), [PLAY_CONSOLE.md](PLAY_CONSOLE.md), [PRIVACY_POLICY.md](PRIVACY_POLICY.md), [DATA_SAFETY.md](DATA_SAFETY.md), `docs/play/graphics/` |
| Upload Key Signing | Release AAB signed with your private upload key | **NEEDS-USER** | `app-play-release.aab` is currently unsigned (`jarsigner`: `jar is unsigned.`); see Step 2 below |
| Hosted Legal URLs | Public HTTPS Privacy Policy URL & Web Account Deletion URL wired into `PrivacyLinks.kt` | **NEEDS-USER** | `PrivacyLinks.PRIVACY_POLICY_URL` & `ACCOUNT_DELETION_URL` currently empty `TODO` constants |
| Real Hardware & Scanner | Physical Samsung Galaxy S26 Ultra + AirPods Pro 2 USB-C + AirPods Max Bluetooth test & Accessibility Scanner audit | **NEEDS-USER** | Checklist prepared in [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) |
| Play Console Closed Test | Closed test with `>= 12` opted-in testers for `>= 14` consecutive days (personal accounts created on/after 13 Nov 2023) | **NEEDS-USER** | Required by [Play Console app testing rules](https://support.google.com/googleplay/android-developer/answer/14151465) |

---

## What You Must Do Yourself

Complete these steps in order before publishing `AnDPoP` to Google Play:

1. **Create and verify the Google Play developer account**
   - Register and complete identity/payment verification in [Google Play Console](https://play.google.com/console), and choose your public developer contact email (`TODO` in [LISTING.md](LISTING.md)).

2. **Create the upload keystore (outside Git)**
   - Generate `andpop-upload.jks` outside this repository using the `keytool -genkeypair` command in [ANDROID_RELEASE.md](../ANDROID_RELEASE.md#2-create-your-upload-key-outside-git), store a secure backup, and add `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` to your git-ignored root `local.properties`.

3. **Host the privacy policy and account-deletion pages**
   - Review [PRIVACY_POLICY.md](PRIVACY_POLICY.md) (especially legal/jurisdiction wording) and publish both the Privacy Policy and the companion Web Account Deletion HTML page over HTTPS.
   - Update `PRIVACY_POLICY_URL` and `ACCOUNT_DELETION_URL` in [PrivacyLinks.kt](../../app/src/main/java/me/kavishdevar/librepods/presentation/PrivacyLinks.kt) and your Store Listing (`LISTING.md`).

4. **Test on a real phone with AirPods**
   - Install a release-signed build on your Galaxy S26 Ultra and work through every item in [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) with AirPods Pro 2 (USB-C) and AirPods Max — especially BR/EDR L2CAP socket connectivity on Android 17, battery/charging/case broadcasts, notifications, Quick Settings tile, Glance widgets, and the **Accessibility Scanner** pass.
   - If you run Supabase accounts (`ACCOUNT_SETUP.md`), test sign-up, reset, and both in-app & web account deletion end to end.

5. **Run the closed test with at least 12 testers for 14 days**
   - *(Applies to personal Play Console developer accounts created on or after **13 November 2023**; still enforced per [Google Play Console Help](https://support.google.com/googleplay/android-developer/answer/14151465). Organization accounts and older personal accounts are exempt.)*
   - Invite at least 12 testers via **Testing -> Closed testing**, share the opt-in link, and keep at least 12 testers opted in continuously for 14 days while exercising real Bluetooth/AirPods scenarios.

6. **Fill in the Play Console forms**
   - Complete **Policy > App content** (`App access` reviewer instructions using `"Try demo mode"`, `Ads`, `IARC Content rating`, `Target audience`, `Data safety` per [DATA_SAFETY.md](DATA_SAFETY.md), `Health apps` = none, `News`/`Government`/`Financial` = No).
   - Record and upload the unlisted `<= 60s` demonstration video for `FOREGROUND_SERVICE_CONNECTED_DEVICE` using the script in [PLAY_CONSOLE.md](PLAY_CONSOLE.md#6-foreground-service-permissions-foreground_service_connected_device).
   - Paste copy and upload graphics from [LISTING.md](LISTING.md) and `docs/play/graphics/`.

7. **Upload the signed AAB**
   - Run `.\gradlew.bat :app:bundlePlayRelease`, verify it with `jarsigner -verify app/build/outputs/bundle/playRelease/app-play-release.aab`, enroll in **Play App Signing**, and upload `app-play-release.aab` to your Play Console release track.

8. **Apply for production access**
   - Once the 14-day closed test requirement and device tests are complete with no regressions, answer the Production Access readiness questions in Play Console and submit `2.0.0 (8)` for review.
