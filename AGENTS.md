# AnDPoP: instructions for AI coding agents

Read this whole file before you change anything. It is the handoff from the Kiro Web session that built AnDPoP 2.0. Kiro CLI and Codex load `AGENTS.md` automatically; other agents should be pointed at it.

## 1. Project facts

| Item | Value |
| --- | --- |
| App | AnDPoP, an Android companion app for AirPods (battery, listening modes, controls). Independent fork of LibrePods v1.0.4, GPL-3.0-or-later. Keep `LICENSE` and the credits in `README.md`. |
| Stack | Kotlin, Jetpack Compose with Material 3 Expressive, Navigation 3, Gradle Kotlin DSL, one module `app`, C++ via CMake (`app/src/main/cpp`). |
| Versions | Gradle wrapper 9.4.1, AGP 9.2.1, Kotlin 2.3.21, Compose BOM 2026.05.00, Material 3 1.5.0-alpha21 (`gradle/libs.versions.toml`). |
| Android | compileSdk and targetSdk 37, minSdk 33, Java 21, NDK 29.0.14206865, CMake 3.22.1. |
| IDs | Namespace `me.kavishdevar.librepods`, applicationId `app.andpop.android`, version 2.0.0 (versionCode 8). |
| Variants | One flavor, `foss`: `fossDebug`, `fossRelease`. `BuildConfig.PLAY_BUILD` is always false today. |
| Source | `app/src/main/java/me/kavishdevar/librepods/` |
| Tests | `app/src/androidTest/java/me/kavishdevar/librepods/`: `AppearanceRegressionTest`, `DashboardRegressionTest`, `ScreenshotTour` (15 tests, all passing at 2.0.0). |
| Design | `docs/v2-ui/README.md` is the source of truth: tokens, components, screens, decisions, implementation status. `docs/v2-ui/AnDPoP-v2-Prototype.html` is the clickable prototype; open it in a browser. |
| CI | `.github/workflows/verify-foss.yml` builds the APK and AAB and runs instrumented tests on an emulator. `.github/workflows/publish-testing-apk.yml` publishes the `v2.0.0-testing` prerelease. |
| Secrets | `local.properties` is git-ignored. It holds `sdk.dir` and optional release-signing keys. Never print, edit or commit it. |

## 2. Where the 2.0 code lives

| Concern | Files |
| --- | --- |
| Color themes and tokens | `presentation/theme/AndpopPalette.kt` (7 themes, `Tones`, `AndpopPalette`, `devicePanel`), `Theme.kt` (Wallpaper uses dynamic color) |
| Type and icons | `presentation/theme/AndpopType.kt`, `Type.kt` (Roboto Flex families), `AndpopIcons.kt` (generated from the prototype SVGs) |
| Shared 2.0 components | `presentation/components/AndpopUi.kt`: `AndpopSectionHeader`, `AndpopGroup`/`groupShape`, `AndpopRow`, `AndpopSwitchRow`, `AndpopSwitch`, `AndpopCard`, `StatusPill`, `InfoBanner` |
| Dashboard | `presentation/screens/AndpopDashboard.kt`, `AndpopBattery.kt`, `AndpopListeningModes.kt`, `AndpopDeviceSettings.kt` |
| Settings | `presentation/screens/AppSettingsScreen.kt` (appearance previews, color swatches, chips, privacy, credits) |
| Legacy screens | Use `StyledList`, `StyledListItem`, `StyledToggle` and `StyledScaffold`, already restyled to 2.0 in `presentation/components/` |
| Onboarding | `presentation/screens/onboarding/` (welcome and privacy redesigned; permissions not yet) |
| Theme preference | Key `andpop_palette` (`ANDPOP_PALETTE_KEY`). Old `andpop_dark_tint` migrates in `MainActivity.kt` and `account/AccountStorage.kt`. |
| Strings | New 2.0 strings: `app/src/main/res/values/andpop_v2_strings.xml` |

## 3. Rules

1. Follow `docs/v2-ui/README.md` and the existing code style. Build new UI from the components in `AndpopUi.kt` and colors from `LocalAndpopPalette.current`. Never hard-code hex colors in screens.
2. Keep native behavior: Bluetooth, AACP commands, account flows and preference keys. Do not turn the app into a WebView.
3. Put user-visible text in string resources (`andpop_v2_strings.xml` for new strings).
4. Keep every existing instrumented test passing. When a change requires a test change, explain why.
5. Keep touch targets at 48 dp or larger, keep TalkBack labels, and make layouts work at 200% font size.
6. Ask the user before you:
   - delete files
   - change Gradle, AGP, Kotlin, SDK or dependency versions
   - touch signing config or `local.properties`
   - push to GitHub
   - do anything outside this repository
7. Work on a feature branch, never directly on `main`. Keep commits small, with messages such as `feat: …`, `fix: …` or `docs: …`.

## 4. Build, install and test (Windows PowerShell; on macOS or Linux use `./gradlew`)

```powershell
.\gradlew.bat :app:assembleFossDebug                 # build the debug APK
adb install -r app\build\outputs\apk\foss\debug\app-foss-debug.apk
.\gradlew.bat :app:connectedFossDebugAndroidTest     # needs an emulator or phone; check with: adb devices
```

- The first build downloads Gradle and dependencies, which can take 5–15 minutes.
- If the CI testing APK is installed, run `adb uninstall app.andpop.android` first. The debug signing keys differ, and uninstalling clears the app's data.
- `ScreenshotTour` writes PNGs to `app\build\outputs\connected_android_test_additional_output\`. Use them to check UI changes.

## 5. What 2.0.0 already contains

- Seven color themes from one tonal scale, one choice for light and dark, and migration from older settings.
- Dashboard:
  - hero card with battery, refresh and Battery details
  - sliding listening-mode control
  - grouped controls and press-and-hold tiles
  - disconnected state with Reconnect, and a pairing guide
  - redesigned battery sheet
- Settings: live appearance previews, color swatches, connection chips, privacy summary, credits, app version and a trademark notice.
- 2.0 styling for every legacy screen through the shared Styled* components.
- Onboarding welcome and privacy pages.
- New launcher icon and splash screen, a renamed notification channel, and version 2.0.0 (8).

## 6. Remaining work, in this order

Decision numbers refer to "Decisions that need your approval" in `docs/v2-ui/README.md`. The user has approved all of them. For multi-screen items, start a Kiro spec (`/spec new <name>` in Kiro CLI).

1. **Account as a normal screen and one welcome (decision 1).**
   - Today `MainActivity` swaps `NavigationRoot` for `AccountScreen` and opens it before onboarding on first run.
   - Make account a `Screen.Account` destination reached from the dashboard avatar, so predictive back works.
   - Make guest the default first-run path, and add "I already have an account" on the welcome page.
   - Keep `AccountScreen(state, model, null)` working for `AppearanceRegressionTest`.
2. **Per-permission Allow buttons** in `onboarding/PermissionsPage.kt`, as in the prototype.
   - Replace "Grant all" with one Allow button per permission.
   - Enable Continue once Nearby devices is granted.
   - Fix the icons whose TalkBack labels are "bluetooth".
3. **Regroup All device settings (decision 2).**
   - In `AirPodsSettingsScreen.kt`, group the screen into Audio, Hearing health, Calls, Connection and charging, and About.
   - Remove the controls that are already on the dashboard, and remove the FOSS premium banner.
   - Disconnect asks for confirmation.
   - Every destination must stay reachable.
4. **Developer options (decision 4).**
   - Tap App version seven times to reveal them.
   - They contain the account service form, the compatibility bypass, demo mode and "Act as an Apple device".
   - Hide all of them when `PLAY_BUILD` is true.
5. **Equalizer sliders (decision 9)** and **segmented speed pickers** in Accessibility.
6. **Loading skeleton** on the dashboard while the service binds.
7. **System surfaces.**
   - One informative notification with a silent service notification (decision 6).
   - Glance widgets (decision 7).
   - Popup and island layouts that match the prototype.
8. **Bundle Roboto Flex (decision 8).** Ask the user to add the font file. Don't download it from inside the sandbox.
9. **Play readiness.**
   - Add a `play` product flavor with `PLAY_BUILD = true`.
   - Review the manifest permissions and protected permissions with the user.
   - Move the remaining hard-coded English strings into resources.
10. **The user tests Bluetooth behavior** on a real phone with AirPods. Ask them for results.

## 7. Agent steps

1. **Check the setup and report.** Change nothing in this step.
   - Run `git status` and `git branch --show-current`.
   - Run `java -version` and expect 21.
   - Check that `local.properties` has `sdk.dir` or that `ANDROID_HOME` is set, without printing the file.
   - Run `.\gradlew.bat --version`, then `.\gradlew.bat :app:assembleFossDebug`.
   - If the build fails on network or file access, the Kiro CLI sandbox is probably blocking Gradle. Tell the user the options in section 8. Don't work around the sandbox yourself.
2. **Branch.** If you're on `main`, propose a branch name such as `feature/v2-account-screen` and wait for approval.
3. **Pick the work.** Show the list in section 6 and ask which item to start. Suggest a spec for items 1, 3 and 7.
4. **Implement in small steps.** Build after each change. Run the instrumented tests when `adb devices` lists a device. Use the `ScreenshotTour` output to check visuals.
5. **Finish.**
   - Update the implementation status table in `docs/v2-ui/README.md`.
   - List the changed files, the build and test results, and what to check on a phone.
   - Ask before committing or pushing.

## 8. Kiro CLI notes for the user

These come from the Kiro docs.

- **Starting:** run `kiro-cli` in the project folder and choose **Trust, continue** on first launch. Trusting lets steering files and `AGENTS.md` load.
- **Sandbox:** a "sandboxed" session limits what commands can reach, and network is blocked by default. Gradle needs internet and `%USERPROFILE%\.gradle`. Either run `/sandbox off` for this project, or run `/sandbox setup` once as admin and then `/sandbox network` → `COMMON_DEPENDENCIES`. You can also run the build yourself in a normal terminal tab.
- **Approvals:** when Kiro asks to run `.\gradlew.bat`, choose **Trust**, pick the base-command tier, and press `s` to save the rule for the workspace.
- **Useful keys and commands:**
  - `@path` references a file.
  - `Ctrl+J` adds a new line.
  - `/spec new <name>` starts a spec.
  - `/context show` shows what loaded.
  - `Esc` cancels.
  - `/quit` exits.
