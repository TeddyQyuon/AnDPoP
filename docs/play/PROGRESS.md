# AnDPoP Play Release Progress

**Last updated:** 2026-11-25  
**Active branch:** `feature/play-phase-0-audit`  
**Target comparison remote:** `https://github.com/TeddyQyuon/AndPoP_V_1.6.0.git`

---

## Phase Checklist

- [x] **Phase 0: Baseline & Audit** (`feature/play-phase-0-audit`)
  - Environment, `:app:assembleFossDebug`, `:app:assembleFossRelease`, `:app:lintFossDebug`, and 16 KB `zipalign`/`llvm-readelf` verified.
  - Full inventory written to [AUDIT.md](AUDIT.md).
- [ ] **Phase 1: Finish 2.0 UI** (`feature/play-phase-1-ui`)
  - [ ] 1.1 Bundled Roboto Flex font (`res/font/roboto_flex.ttf`), missing `AndpopIcons`, adaptive icon radial gradient, shared `AndpopUi` segmented/slider/radio/checkbox primitives
  - [ ] 1.2 Account as normal `Screen.Account` + single Welcome with `"I already have an account"` (Decision 1)
  - [ ] 1.3 Per-permission Allow buttons & TalkBack fixes in `PermissionsPage.kt` + `NotSupportedPage.kt`
  - [ ] 1.4 Regrouped `All device settings` + Disconnect confirmation (Decision 2)
  - [ ] 1.5 `DeveloperOptionsScreen.kt` (7-tap App version unlock; Account service form, compatibility bypass, demo mode, Apple vendor hook, velocity plot) (Decision 4)
  - [ ] 1.6 Detail screens: `RenameScreen`, `PressAndHoldSettingsScreen`, `VersionInfoScreen`, `AccessibilitySettingsScreen`, `EqualizerScreen` (Decision 9), `HeadTrackingScreen`, `TroubleshootingScreen` + non-root `LogCollector`, and remaining detail screens
  - [ ] 1.7 Dashboard loading skeleton while service binds
  - [ ] 1.8 System surfaces: single informative notification + silent service notification (Decision 6), Glance widgets (Decision 7), Compose `PopupWindow` & `IslandWindow`, updated `AirPodsQSService` & `QuickSettingsDialogActivity`
  - [ ] 1.9 Large-screen two-pane layout (`>= 840 dp`)
  - [ ] 1.10 Update implementation status table in `docs/v2-ui/README.md`
- [ ] **Phase 2: Play Build & Policy Compliance** (`feature/play-phase-2-policy`)
  - [ ] `play` flavor (`PLAY_BUILD = true`), Xposed/billing removal, reviewer `"Try demo mode"` entry
  - [ ] Play manifest (`tools:node="remove"` for privileged permissions), exported hardening, `network_security_config.xml`
  - [ ] Privacy links (`PRIVACY_POLICY_URL` / `ACCOUNT_DELETION_URL` TODOs), `PRIVACY_POLICY.md`, `DATA_SAFETY.md`
  - [ ] Localization cleanup (`values-it` `app_name`, Linux/LibrePods scrub, string resource extraction)
  - [ ] Backup rules exclusions (`settings.xml`, `packet_logs.xml`, `logs/`) & ProGuard/R8 release hardening
- [ ] **Phase 3: Quality** (`feature/play-phase-3-quality`)
  - [ ] Zero lint errors on `:app:lintFossDebug` & `:app:lintPlayRelease`
  - [ ] 15 existing + new Play readiness instrumented tests passing on `Pixel_8_Pro` emulator for both `foss` and `play`
  - [ ] R8 release APK smoke test + `DEVICE_TEST_PLAN.md`
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

## Last Build & Test Results (Phase 0 Baseline)
- `:app:assembleFossDebug` — **PASS** (`48,786,895` bytes)
- `:app:assembleFossRelease` — **PASS** (`8,246,122` bytes)
- `:app:lintFossDebug` — **125 errors, 250 warnings** (111 `MissingTranslation`, 13 `UseAppTint`, 1 `NewApi`)
- `zipalign -c -P 16 -v 4` — **PASS (`Verification successful`)**
- `llvm-readelf -l` (all 6 `.so` targets in `arm64-v8a` & `x86_64`) — **PASS (`Align = 0x4000`)**
- `:app:connectedFossDebugAndroidTest` — **Skipped in Phase 0** (no emulator booted yet; `Pixel_8_Pro` AVD will be booted in Phase 3)

---

## Exact Next Step
Create branch `feature/play-phase-1-ui` from `feature/play-phase-0-audit` and start **Phase 1.1** (JVM heap tuning in `gradle.properties`, `16384` link flags in `CMakeLists.txt`, bundled `roboto_flex.ttf`, `AndpopIcons`, adaptive icon gradient, and `AndpopUi` shared primitives).
