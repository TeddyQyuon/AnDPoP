# AnDPoP v1.5 Android UI handoff

**Goal.** Bring the charging icon and visual refinements from the corrected prototype into the existing Kotlin/Jetpack Compose app. Teddy wants the same controls in the same places, including settings below the fold. AirPods Pro 2 USB-C on a Samsung SM-S948B is the primary review device. Also preserve AirPods Max support.

## Start here

- Repository: `TeddyQyuon/AnDPoP`. Android source inspected at `7b7da486c6bd5cb74f228e8e1ed74deed596c5d8` on `main`. Gradle declares `versionName = "1.5.0"` and `versionCode = 7`.
- [Open the corrected interactive prototype](AnDPoP-Charging-Prototype.html) by downloading the HTML and opening it in a browser. The images and code are embedded. GitHub's file viewer displays the source.
- [Copy the next-agent prompt](NEXT-AGENT-PROMPT.md). Give the new account access to this repository if necessary. Downloading a ZIP works for review. Clone the repository for development.
- This handoff commit contains documentation and the HTML reference. The next coding task is the native UI port. The existing testing APK predates the corrected prototype.

## What was done and verified

| Area | Work already completed |
| --- | --- |
| Android baseline | The existing code includes real Bluetooth battery data and listening commands, a native settings screen, dashboard control groups, six appearance palettes, profile-aware settings, a guest flow and account screens. Preserve the source as the functional baseline. |
| Battery prototype | Kept the three-column photo card. Replaced the charging caption visually with a green battery and central dark bolt under the charging component. Battery details identifies charging in text. Switching the sample demonstrates both buds, right bud only, case only, low charge, last-seen case, unavailable readings, disconnected and Max. |
| Dashboard prototype | Restored AirPods controls, independent left/right press-and-hold rows and About below Sound. Reworked outline icons, fixed the sound switch width and the split word “Transparency”, and kept a single green check for the selected listening mode. |
| Settings prototype | Restored the full-height page with its back arrow, appearance, widget, both popups, conversation controls, automatic connection conditions, advanced options, troubleshooting, credits and device information. Grouped source and photo attribution under Credits and licenses. |
| Checks so far | Browser checks covered six palette combinations, 320/360/412 CSS-pixel widths, charging-state placement, all 12 Settings switches, both volume slider limits, settings subpage navigation and changing one bud’s long-press action independently. The earlier Android baseline had six instrumented checks pass. The native port still needs its own build, tests and physical AirPods check. |

## Design contract

Keep the single-column dashboard. The top bar has AnDPoP, Account and a round sliders button. Below it are the device name, model and connection status. Use real model artwork, light-edged translucent panels, restrained gradients and readable secondary text. The Bolt and listening-mode check have different meanings and positions.

| Order | Dashboard content and interaction |
| --- | --- |
| 1. Battery | Heading with refresh, then left bud, right bud and case. For each, show photo, percentage, device label, then charge icon or status. Battery details opens from the bottom right. AirPods Max gets its own device reading. Keep native freshness, missing-data, estimated-level and disconnected presentation wherever supported. |
| 2. Listening Mode | Cancel noise, Transparency, Adaptive and Off where supported. Keep the shield/minus, sound arcs, waveform and slashed-circle icons. Put a small plain green check at the upper-right of the selected tile; keep its surface consistent with neighboring tiles. Show the selected mode description below. Follow device-reported mode, including changes made on the stem. |
| 3. Sound | Conversation Awareness label, explanation and compact switch. Preserve the model/capability gate. |
| 4. AirPods controls | Name with its current value, Automatic wear detection, Microphone with current mode, Accessibility. Retain the native destinations and updates. |
| 5. Press and Hold AirPods | Separate Left bud and Right bud rows with current action. Each opens its own action picker. Preserve Listening Mode, Digital Assistant and native listening-mode cycle rules. Honor model capability. |
| 6. About | Actual Model Number, Firmware version with its detail screen, and More device controls. Keep the full existing native advanced screen and every existing destination. The prototype shows representative advanced controls. |

### Charging glyph

Use the prototype’s `chargingIcon(level)` as the drawing reference. It uses a 48 × 25 view box and appears about 44 × 24 CSS pixels. Include a horizontal outlined rounded battery, right-hand terminal, proportional green fill (#69D447) and a centered dark bolt (#101713) edged in pale ink. Use a Compose Canvas or vector plus level-dependent fill. Share the glyph and condition between the dashboard and detail view.

    showChargingIcon = item.level != null && item.charging && !item.lastSeen

Keep the percentage visible. Evaluate the condition separately for each component. A 100% reading alone cannot establish charging. Keep the level and flags from the native model. Align the three percentages, reserve status space to avoid jumps, and retain screen reader output such as “Right bud, 48 percent, charging.” Decorative artwork and bolt can use a null content description when the row already conveys this information.

### Appearance and layout

Use the user’s separate light/dark palette preferences. Dark defaults to Glacier; light defaults to Iris. The appearance switch offers System, Light and Dark. The palette picker changes the active appearance and remembers the other choice. Preserve existing profile preference keys and migration behavior.

| Appearance | Palette | Page | Glass base | Primary text | Accent |
| --- | --- | --- | --- | --- | --- |
| Dark | Glacier | #0F2932 | #28434A | #F2FAFB | #B9E0E9 |
| Dark | Iris | #211C39 | #38324F | #F5F1FF | #E0D2FF |
| Dark | Guava | #341E2A | #4E3341 | #FFF1F4 | #FFD1DD |
| Light | Glacier | #DEF3F6 | #F1FCFD | #122D34 | #245460 |
| Light | Iris | #EEE9FF | #F9F6FF | #241E35 | #584477 |
| Light | Guava | #FFE9E4 | #FFF5F1 | #352326 | #754452 |

Use the HTML’s computed styles for gradients, edges and spacing. The phone layout starts with about 16 units of horizontal padding and 23-unit panel corners. Give Transparency extra width. Four mode tiles fit on the tested 360 and 412 widths. The prototype uses a two-column layout at 345 CSS pixels and below. In Compose account for actual available width, language and font scaling. Keep labels readable and touch targets at least 48 dp. Preserve focus order, checked states, TalkBack names and an operable Back action.

## Settings inventory

The sliders button opens a full-height scrollable Settings page. Keep the existing order and native preference handlers. The last column records how the supplied sample appears; real user and device preferences take precedence.

| Section | Controls | Prototype sample |
| --- | --- | --- |
| Appearance | System, Light, Dark. Glacier, Iris, Guava for the active appearance | Opens in Glacier dark |
| Widget | Show phone battery in widget | On |
| Popup Animations | Bottom sheet popup; Dynamic Island popup | Both on |
| Conversation Awareness | Pause Music; Relative volume; Conversation Awareness Volume slider from 10% to 85% | Off, on, 44% |
| Connect by AirPods status | Disconnected, Idle, Playing media, On call | Four independent switches, initially off |
| Connect by phone activity | Receiving a call, Starting media playback | Both off |
| Advanced Options | Use alternate head tracking packets | On |
| Help and credits | Troubleshooting; Credits and licenses | Retain native attribution, photo licenses and open-source links |
| Device information | Manufacturer, Model Number, Build ID, Version, Xposed available, App enabled in Xposed | Read from the device in Android |

Keep permission-, model-, build- and capability-dependent rows already present in Android. For example, the native settings source conditionally exposes disconnect-when-not-wearing. Preserve its original condition. Keep all nested native settings even where the HTML has a shorter example. Back from a detail should return to the parent control with its value intact.

## Where to work in Android

Source root: `app/src/main/java/me/kavishdevar/librepods/`.

| File under source root | Responsibility |
| --- | --- |
| `presentation/screens/AndpopBattery.kt` | `DashboardBattery`, `dashboardBattery`, `BatterySection`, `BatteryCell`, `BatteryDetail`, `BatteryGlyph` and `BatteryQuickView`. Inspect the cell and quick-view paths before replacing glyphs. |
| `data/CaseBatterySnapshot.kt` | Case ownership, session and freshness handling. Preserve this data behavior. |
| `presentation/screens/AndpopDashboard.kt`, `AndpopDeviceArt.kt` in the same directory | Dashboard composition, device artwork and navigation. Reuse the packaged drawables. |
| `presentation/screens/AndpopListeningModes.kt` | Mode options, capability filtering, glyphs, layout and selection callback. |
| `presentation/screens/AndpopDeviceSettings.kt` | Native Sound, AirPods controls, Press and Hold and About groups. Use their existing callbacks. |
| `presentation/screens/AppSettingsScreen.kt` | Full preference screen, widget/popups, conversation settings, connection rules, advanced switches, troubleshooting and credits. |
| `presentation/screens/AirPodsSettingsScreen.kt`, `AccessibilitySettingsScreen.kt`, `PressAndHoldSettingsScreen.kt`, `VersionInfoScreen.kt` | Native detail screens. Preserve every reachable control, supported feature and real device value. |
| `presentation/theme/AndpopPalette.kt` and `Theme.kt` | Appearance, tint, selected check and shared glass treatment. |
| `account/` | Preserve native guest, email sign-up/sign-in, recovery, account removal, remembered-device and per-profile settings. Follow existing provider availability. See [account setup](../ACCOUNT_SETUP.md). |

Keep labels in Android string resources. Start with a small native diff around the charging indicator and mode layout. Compare the remainder against the HTML and adjust styling where needed. The HTML’s sample scenarios, in-memory profiles and static phone/firmware values exist for design review. Shipping uses native readings, native callbacks and the existing account backend. Keep configuration and signing credentials outside the handoff.

## Build and acceptance

From the repository root, use Java 21 and the checked-in Gradle wrapper. The current workflow pins Android SDK `platforms;android-37.0` on channel 3, NDK `29.0.14206865` and CMake `3.22.1`. See [Android release instructions](../ANDROID_RELEASE.md).

```sh
./gradlew :app:assembleFossDebug :app:assembleFossDebugAndroidTest :app:bundleFossDebug
# With an emulator or Android device connected
./gradlew :app:connectedFossDebugAndroidTest
```

1. Exercise the existing six checks in `app/src/androidTest/java/me/kavishdevar/librepods/AppearanceRegressionTest.kt`. Add coverage for charging placement and stale/missing suppression if the current tests miss them.
2. Compare all six themes, including switching back to System and switching profiles. Check normal and enlarged text, “Transparency”, one checked mode, switch hit areas and the full settings navigation.
3. Exercise independent left/right/case charging, full but unplugged, low, last seen, unknown, disconnected and Max. Confirm dashboard and battery details agree. On actual hardware, compare a charging bud against one out of the case. Report emulator and hardware results separately.
4. Change a bud action, microphone and conversation volume. Reopen the screens and restart the native app. Confirm expected persistence and isolation between guest and user profiles. Exercise existing account forms using test credentials configured outside the repository.
5. For the subsequent implementation, provide the commit SHA, testing APK, requested AAB and observed test results. The existing workflow `.github/workflows/publish-testing-apk.yml` publishes a GitHub prerelease. A local build suffices for UI verification. Keep Play publication a separate action.

**Completion boundary.** This handoff records the current v1.5 app and corrected prototype. Browser checks demonstrate UI interactions. Live charging, Bluetooth commands, OS popups, widgets, sign-in providers and device persistence require verification in the Android build.
