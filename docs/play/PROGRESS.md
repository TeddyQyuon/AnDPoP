# AnDPoP Play Release Progress

**Last updated:** 2026-11-25  
**Active branch:** `feature/play-phase-2-policy` (ready for `feature/play-phase-3-quality`)  
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
- [ ] **Phase 3: Quality** (`feature/play-phase-3-quality`)
  - [ ] Zero lint errors on `:app:lintFossDebug` & `:app:lintPlayRelease`
  - [ ] Existing 15 + new Play readiness instrumented tests passing on `Pixel_8_Pro` emulator for both `foss` and `play`
  - [ ] R8 release APK smoke test + 200% font scale check + `DEVICE_TEST_PLAN.md`
- [ ] **Phase 4: Signing & Release Bundle** (`feature/play-phase-4-signing`)
  - [ ] Updated `docs/ANDROID_RELEASE.md` (keytool, `local.properties`, Play App Signing, versionCode strategy)
  - [ ] Verified `:app:bundlePlayRelease` & `:app:bundleFossRelease` + 16 KB check
- [ ] **Phase 5: Store Listing & Graphics** (`feature/play-phase-5-store`)
  - [ ] `docs/play/LISTING.md`, `docs/play/PLAY_CONSOLE.md`, `docs/play/graphics/` (512px icon, 1024x500 feature graphic, emulator phone screenshots)
- [ ] **Phase 6: Final Verification & Handoff** (`feature/play-phase-6-handoff`)
  - [ ] `docs/play/RELEASE_CHECKLIST.md`, full verification pass, clean commit & push to `https://github.com/TeddyQyuon/AndPoP_V_1.6.0.git`

---

## Decisions Recorded

| Date | Decision | Source |
| --- | --- | --- |
| 2026-11-25 | Decisions 1–10 in `docs/v2-ui/README.md` approved | `PLAY_RELEASE_PROMPT.md` |
| 2026-11-25 | Autonomous execution through Phases 0–6 authorized; bundle local `RobotoFlex-Regular.ttf` from Android Studio layoutlib (`OFL-1.1`), add Glance `1.1.1` for widgets, remove legacy `PurchaseScreen`/billing code | User prompt + Plan |
| 2026-11-25 | Keep `applicationId = "app.andpop.android"` across both `foss` and `play` flavors; never commit secrets/keystores or alter `local.properties` | Plan / `AGENTS.md` |
| 2026-11-25 | Push final verified work and branches only to `https://github.com/TeddyQyuon/AndPoP_V_1.6.0.git` (remote `antigravity`), never to `origin` | User prompt |

---

## Open Questions / User Action Items (Deferred to Release Checklist)
- Provide hosted HTTPS URLs for Privacy Policy and Web Account Deletion (currently marked with a single `TODO` constant in `PrivacyLinks.kt`).
- Generate upload keystore via `keytool` and populate local `local.properties` before uploading to Play Console.
- Run physical Galaxy S26 Ultra + AirPods Pro 2 / AirPods Max checks (`docs/play/DEVICE_TEST_PLAN.md`).

---

## Last Build & Test Results (Phase 2 Complete)
- `:app:assembleFossDebug`, `:app:assemblePlayDebug`, `:app:assembleFossRelease`, `:app:assemblePlayRelease` — **PASS** (`BUILD SUCCESSFUL in 4m 26s`)

---

## Exact Next Step
Branch `feature/play-phase-3-quality` from `feature/play-phase-2-policy` and run `:app:lintFossDebug :app:lintPlayRelease`, add `PlayReadinessRegressionTest.kt`, boot `Pixel_8_Pro`, run full instrumented suites on both flavors, and smoke-test the R8 Play release APK.
