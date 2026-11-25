# AnDPoP Play Release Progress

**Last updated:** 2026-11-25  
**Active branch:** `feature/play-phase-6-handoff` (all Phases 0–6 complete; ready for user Play Console steps)  
**Target comparison remote:** `https://github.com/TeddyQyuon/AndPoP_V_1.6.0.git`

---

## Phase Checklist

- [x] **Phase 0: Baseline & Audit** (`feature/play-phase-0-audit`)
  - Environment, `:app:assembleFossDebug`, `:app:assembleFossRelease`, `:app:lintFossDebug`, and 16 KB `zipalign`/`llvm-readelf` verified.
  - Full inventory and second-review addendum written to [AUDIT.md](AUDIT.md).
- [x] **Phase 1: Finish 2.0 UI** (`feature/play-phase-1-ui`)
  - [x] 1.1 Bundled Roboto Flex font (`res/font/roboto_flex.ttf`), missing `AndpopIcons`, adaptive icon radial gradient, shared `AndpopUi` segmented/slider/radio/checkbox primitives
  - [x] 1.2 Account as normal `Screen.Account` + single Welcome with `"I already have an account"` (Decision 1)
  - [x] 1.3 Per-permission Allow buttons & TalkBack fixes in `PermissionsPage.kt` + `NotSupportedPage.kt`
  - [x] 1.4 Regrouped `All device settings` + Disconnect confirmation (Decision 2)
  - [x] 1.5 `DeveloperOptionsScreen.kt` (7-tap App version unlock; Account service form, compatibility bypass, demo mode, Apple vendor hook, velocity plot) (Decision 4)
  - [x] 1.6 Detail screens: `RenameScreen`, `PressAndHoldSettingsScreen`, `VersionInfoScreen`, `AccessibilitySettingsScreen`, `EqualizerScreen` (Decision 9), `HeadTrackingScreen`, `TroubleshootingScreen` + non-root `LogRecorder`, `HearingAidScreen`, `HearingAidAdjustmentsScreen`, `TransparencySettingsScreen`, `UpdateHearingTestScreen`, `OpenSourceLicensesScreen`, and PUA glyph cleanup
  - [x] 1.7 Dashboard loading skeleton while service binds + `"Try demo mode"` entry
  - [x] 1.8 System surfaces: single informative connected notification + silent deferred FGS notification (Decision 6), Jetpack Glance 1.1.1 widgets (Decision 7), Compose `PopupWindow` & `IslandWindow` overlays, shared `ListeningModeCycle` QS tile & themed QS dialog
  - [x] 1.9 Large-screen two-pane layout (`>= 840 dp`)
  - [x] 1.10 Updated implementation status table in `docs/v2-ui/README.md`
- [x] **Phase 2: Play Build & Policy Compliance** (`feature/play-phase-2-policy`)
  - [x] Added `play` flavor (`PLAY_BUILD = true`, `applicationId = "app.andpop.android"`) and clean `src/foss` vs `src/play` Xposed source-set split excluding `libl2c_fcr_hook.so` and `META-INF/xposed/*`
  - [x] Hardened manifest (`tools:node="remove"` for privileged permissions in `play`, removed unused legacy `BLUETOOTH`/`BLUETOOTH_ADMIN`/`BLUETOOTH_ADVERTISE`, `AirPodsService` `exported="false"`, `RECEIVER_NOT_EXPORTED`, API 37 `disconnect()` guard, `BootReceiver` guard, `network_security_config.xml`)
  - [x] Drafted [PRIVACY_POLICY.md](PRIVACY_POLICY.md) (with web deletion HTML) and [DATA_SAFETY.md](DATA_SAFETY.md); updated [AUDIT.md](AUDIT.md) with permission justification & 12-locale translation plan
  - [x] Scrubbed localization branding (`values-it` `app_name`, `Linux` remote island strings across 13 locales, `ja`/`ko` LibrePods strings, `MissingTranslation` suppressions on `andpop_*` files)
  - [x] Excluded `settings.xml`, `packet_logs.xml`, and `logs/` from `backup_rules.xml` & `data_extraction_rules.xml`; hardened `proguard-rules.pro` (`NativeBridge` keep + stripping `Log.v/d/i`)
- [x] **Phase 3: Quality** (`feature/play-phase-3-quality`)
  - [x] Zero lint errors on `:app:lintFossDebug` & `:app:lintPlayRelease` (fixed `LocalResources` in `AppSettingsScreen.kt`, explicit `setPackage` in `AirPodsService.kt`, and `tools:ignore` for `tools:node="remove"` entries in `app/src/play/AndroidManifest.xml`)
  - [x] Existing 16 + 6 new Play readiness tests (`22/22` passed on `Pixel_8_Pro` API 37 for both `foss` and `play` with `espresso-core:3.7.0` / `runner:1.7.0` scoped to `androidTest`)
  - [x] R8 `playRelease` smoke test passed (caught and fixed `RoomDatabase` `WorkDatabase_Impl.<init>()` stripping via `app/proguard-rules.pro`; verified `100%` and `200%` font scale renders with 0 crashes) + created [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md)
- [x] **Phase 4: Signing & Release Bundle** (`feature/play-phase-4-signing`)
  - [x] Updated [ANDROID_RELEASE.md](../ANDROID_RELEASE.md) (`keytool` upload key outside repo, `local.properties` properties, Play App Signing, `:app:bundlePlayRelease` & `:app:bundleFossRelease`, `jarsigner`/`apksigner`, monotonic `versionCode`)
  - [x] Built `:app:bundlePlayRelease` (`9,752,358` bytes, unsigned pending user upload key) & `:app:bundleFossRelease` (`10,297,073` bytes); verified `0` Xposed entries, `0x4000` (16 KB) `LOAD` alignment via `llvm-readelf -l` on all 4 `.so` libraries in `app-play-release.aab`, and `zipalign -c -P 16 -v 4` `Verification successful` on both release APKs
- [x] **Phase 5: Store Listing & Graphics** (`feature/play-phase-5-store`)
  - [x] Refreshed `docs/v2-ui/app/app-01`..`12` reference screenshots from verified `foss` tour
  - [x] Created [LISTING.md](LISTING.md) (`AnDPoP` 6/30, short 66/80, full 2,080/4,000, release notes 400/500, category, tags, contact placeholders) and [PLAY_CONSOLE.md](PLAY_CONSOLE.md) (all questionnaires, FGS video script, Health Apps medical-device policy & `PLAY_BUILD` Hearing Aid exclusion, L2CAP hidden-API risk note)
  - [x] Generated and visually verified `docs/play/graphics/`: `icon-512.png` (`512x512`), `feature-graphic-1024x500.png` (`1024x500`), and 6 `1080x2340` phone screenshots (`phone-01`..`phone-06`) from real `play` tour captures
- [x] **Phase 6: Final Verification & Handoff** (`feature/play-phase-6-handoff`)
  - [x] Updated implementation status & Play readiness tables in [docs/v2-ui/README.md](../v2-ui/README.md)
  - [x] Produced [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) (`PASS` / `NEEDS-USER` matrix + ordered steps 1–8 for Play launch, including 12-tester / 14-day closed testing rule verification)
  - [x] Final lint & git hygiene verification passed; all `feature/play-phase-*` branches and `main` prepared for push to `https://github.com/TeddyQyuon/AndPoP_V_1.6.0.git` (`antigravity`)

---

## Decisions Recorded

| Date | Decision | Source |
| --- | --- | --- |
| 2026-11-25 | Decisions 1–10 in `docs/v2-ui/README.md` approved | `PLAY_RELEASE_PROMPT.md` |
| 2026-11-25 | Autonomous execution through Phases 0–6 authorized; bundle local `RobotoFlex-Regular.ttf` from Android Studio layoutlib (`OFL-1.1`), add Glance `1.1.1` for widgets, remove legacy `PurchaseScreen`/billing code | User prompt + Plan |
| 2026-11-25 | Keep `applicationId = "app.andpop.android"` across both `foss` and `play` flavors; never commit secrets/keystores or alter `local.properties` | Plan / `AGENTS.md` |
| 2026-11-25 | Push final verified work and branches only to `https://github.com/TeddyQyuon/AndPoP_V_1.6.0.git` (remote `antigravity`), never to `origin` | User prompt |
| 2026-11-25 | Pin `espresso-core:3.7.0` and `runner:1.7.0` in `androidTestImplementation` (with `AndroidTest`-only `androidx.concurrent:1.1.0` & `error_prone_annotations:2.15.0` alignment) so instrumentation passes on API 37 without altering production APK dependencies | Execution plan |

---

## Open Questions / User Action Items (Deferred to Release Checklist)
- Provide hosted HTTPS URLs for Privacy Policy and Web Account Deletion (currently marked with a single `TODO` constant in `PrivacyLinks.kt`).
- Generate upload keystore via `keytool` and populate local `local.properties` before uploading to Play Console.
- Run physical Galaxy S26 Ultra + AirPods Pro 2 / AirPods Max checks (`docs/play/DEVICE_TEST_PLAN.md`).

---

## Last Build & Test Results (Phase 4 Complete)
- `:app:bundlePlayRelease` — **PASS** (`9,752,358` bytes; `jarsigner`: unsigned pending upload keystore; `0` Xposed/hook entries; `llvm-readelf -l`: all 4 `.so` libs `0x4000`)
- `:app:bundleFossRelease` — **PASS** (`10,297,073` bytes)
- `zipalign -c -P 16 -v 4` on `app-play-release-unsigned.apk` (`8,451,436` bytes) & `app-foss-release-unsigned.apk` (`9,111,948` bytes) — **Verification successful**
- `:app:lintFossDebug` & `:app:lintPlayRelease` — **PASS** (`0 errors`)
- `:app:connectedFossDebugAndroidTest` & `:app:connectedPlayDebugAndroidTest` — **PASS** (`22/22` tests on `Pixel_8_Pro(AVD) - 17`, 14 screenshot PNGs per flavor)
- Debug-signed R8 `playRelease` emulator smoke test (100% & 200% font scale) — **PASS** (`0` crashes)

---

## Exact Next Step
Follow **"What you must do yourself"** steps 1–8 in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md): generate your external upload keystore, host `PRIVACY_POLICY.md` + web account deletion URL (updating `PrivacyLinks.kt`), run [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) on real hardware with AirPods, run the 14-day closed test, and submit the signed `app-play-release.aab` in Play Console.
