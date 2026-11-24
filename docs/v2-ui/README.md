# AnDPoP 2.0 UI/UX prototype

**Status: prototype for review. No app code has changed.** This folder has the proposed redesign: the interactive prototype, screenshots and this spec. Once the design is approved, it will be ported to the existing Kotlin/Jetpack Compose app. Bluetooth, account and preference behavior stay native.

| File | What it is |
| --- | --- |
| [`AnDPoP-v2-Prototype.html`](AnDPoP-v2-Prototype.html) | A single offline file with images and code embedded. Download it and open it in Chrome, Edge or Firefox. You click through it like the app. A side panel switches the theme, palette, device state, screen width (360, 384 or 412 dp), font scale (100 to 200%) and profile. The notes panel explains every screen. |
| [`screens/`](screens) | 31 PNG exports taken from the prototype |
| This README | Design decisions, tokens, screen inventory, Play Store checklist and port plan |

The prototype uses sample data. It does not talk to Bluetooth, Supabase or Android settings, so any real system action shows a short message instead.

<img src="screens/00-prototype-workbench.png" width="900" alt="The prototype open in a browser: controls on the left, the phone in the middle, design notes on the right">

Screens are listed in the left column. Use the gesture bar or <kbd>Esc</kbd> to go back. On a narrow window, open the notes with the “Design notes” button.

## Starting point

These sources describe the current app:
- the v1.5 dashboard (`AndpopDashboard.kt`, `AndpopBattery.kt`, `AndpopListeningModes.kt`, `AndpopDeviceSettings.kt`);
- the older LibrePods screens behind “More device controls” and Settings;
- onboarding, the account flow, overlays, widgets and notifications;
- the [v1.5 handoff](../v1.5-ui/IMPLEMENTATION.md).

The redesign keeps these from v1.5:
- the single-column dashboard and its section order;
- the real product photos;
- the Glacier, Iris and Guava names (now retuned, and joined by four new themes);
- the per-component charging bolt;
- one green check on the selected listening mode;
- every setting and destination.

The problems it fixes:

1. **Two visual languages.** The Andpop screens use glass `devicePanel` cards. Every LibrePods screen uses `StyledList` segmented groups, and buttons are a mix of raw M3 and Styled* components. A third, iOS-style “Apple” branch in every component never runs, because `m3eEnabled` is always true.
2. **Heavy hierarchy.** Bold headline-size section titles compete with the content. Every panel has a gradient and a border.
3. **Duplicated screens.** “More device controls” repeats the dashboard: battery, name, listening mode, press and hold, microphone and accessibility.
4. **Two welcome screens.** On first run, the account screen (“Welcome to AnDPoP”) appears before onboarding (also “Welcome to AnDPoP”). It also takes over the whole app instead of being a navigation destination.
5. **Developer tools visible to users.** These include the Supabase URL and key form, “Bypass compatibility check”, the hidden 5-tap demo mode, and the Xposed and root wording in Troubleshooting.
6. **Play readiness gaps.** See the [checklist](#google-play-readiness).

## Screens

| | | | |
| --- | --- | --- | --- |
| <img src="screens/01-dashboard.png" width="200"><br>Dashboard | <img src="screens/03-dashboard-charging.png" width="200"><br>Right bud and case charging | <img src="screens/04-battery-details.png" width="200"><br>Battery details sheet | <img src="screens/05-dashboard-light-iris.png" width="200"><br>Light · Iris |
| <img src="screens/06-airpods-max.png" width="200"><br>AirPods Max | <img src="screens/07-disconnected.png" width="200"><br>Disconnected | <img src="screens/08-no-airpods-yet.png" width="200"><br>No AirPods yet | <img src="screens/24-guava-light-360dp.png" width="190"><br>Guava light · 360 dp |
| <img src="screens/09-welcome.png" width="200"><br>Welcome | <img src="screens/10-privacy.png" width="200"><br>Privacy summary | <img src="screens/11-permissions.png" width="200"><br>Permissions | <img src="screens/12-press-and-hold.png" width="200"><br>Press and hold |
| <img src="screens/15-account-guest.png" width="200"><br>Account · guest | <img src="screens/16-account-signed-in.png" width="200"><br>Account · signed in | <img src="screens/17-sign-up.png" width="200"><br>Create account | <img src="screens/19-widgets.png" width="200"><br>Widgets |
| <img src="screens/20-notifications-qs.png" width="200"><br>Notification and QS tile | <img src="screens/21-connection-popup.png" width="200"><br>Connection popup | <img src="screens/22-dynamic-island.png" width="200"><br>Dynamic Island popup | <img src="screens/18-troubleshooting.png" width="200"><br>Troubleshooting |

| | | | |
| --- | --- | --- | --- |
| <img src="screens/28-settings-appearance-dark.png" width="200"><br>Appearance · Glacier dark | <img src="screens/29-settings-appearance-light.png" width="200"><br>Appearance · Sage light | <img src="screens/30-settings-appearance-wallpaper.png" width="200"><br>Appearance · Wallpaper | <img src="screens/14-settings.png" width="200"><br>Settings (full page) |

Full-length pages: [dashboard](screens/02-dashboard-full.png), [all device settings](screens/13-all-device-settings.png), [settings](screens/14-settings.png), [200% font scale](screens/23-large-text-200.png).
Boards: [design system](screens/25-design-system.png), [Play Store assets](screens/26-play-store-assets.png), [color themes](screens/27-color-themes.png).

The prototype also covers name, microphone, accessibility, firmware, equalizer, adaptive audio, head gestures, hearing aid and its adjustments, transparency mode, hearing protection, call controls, credits, open-source licenses, developer options, log in with error state, password reset, the splash screen, the loading skeleton and the remaining battery states (low, last seen, estimated, no reading).

## Decisions that need your approval

These go beyond restyling. Each can be reverted on its own.

| # | Change | Why | Keep the current behavior instead? |
| --- | --- | --- | --- |
| 1 | One welcome screen. Account becomes a normal screen reached from the avatar, and guest is the default first-run path. | Removes the double welcome. Predictive back works. Play reviewers can reach the app without signing in. | The account screen stays the first screen and keeps replacing `NavigationRoot` |
| 2 | “More device controls” becomes **All device settings**, grouped as Audio, Hearing health, Calls, Connection and charging, and About. Controls already on the dashboard are not repeated. | Removes six duplicates. Every destination stays reachable. | Keep the full legacy list |
| 3 | The six automatic-connection switches become two filter-chip groups. | One compact card in place of six rows | Keep the switches with their descriptions |
| 4 | **Developer options** (tap App version 7 times) hold the account service form, compatibility bypass, demo mode and “Act as an Apple device”. | Keeps testing tools out of a Play build | Leave them where they are |
| 5 | New launcher icon: two generic earbud stems with a “pop” dot, plus a themed (monochrome) layer | The current five mint bars read as an equalizer. The new shapes avoid Apple’s outlines. | Keep the current icon |
| 6 | One notification: battery and mode, with actions. The foreground-service notification is silent and minimized. | Two notifications are noisy. One channel currently shows the code text `BluetoothConnectionManager.aacpSocket?` in system settings. | Keep both |
| 7 | Widgets move to Jetpack Glance with app colors and rings | Rounded, themed, resizable | Keep the RemoteViews layouts and only restyle them |
| 8 | Bundle Roboto Flex instead of downloading it through Google Play services | Same type on every phone, including phones without GMS | Keep the downloadable font |
| 9 | The equalizer curve is driven by Low, Mid and High sliders | Works with TalkBack and is easier to hit | Keep the draggable canvas only |
| 10 | New color system: seven themes from one tonal scale, chosen once for both light and dark. Saved `andpop_dark_tint` / `andpop_light_tint` values migrate to a single `andpop_palette` (the dark choice wins). | Cleaner, more modern surfaces; the “Light keeps Iris” rule confused people | Keep separate dark and light palettes |

## Design system

Built on Material 3 Expressive, which the app already loads through `MaterialExpressiveTheme`. One component set replaces both the Andpop and the Styled* families, and the unused Apple (liquid glass) branch is deleted. See the [design system board](screens/25-design-system.png).

### Color themes

<img src="screens/27-color-themes.png" width="900" alt="All seven color themes on the dashboard, dark above and light below">

Seven themes: **Wallpaper**, **Glacier** (default), **Iris**, **Guava**, **Sage**, **Apricot** and **Graphite**. You choose one in Settings › Color and it applies to both light and dark.

- **One tonal scale for every theme.** Each role has a fixed lightness and chroma in OKLCH, and only the hue changes per theme. Text contrast is therefore the same in every theme. The lowest text pair is 4.8:1, which passes WCAG AA.
- **Clean surfaces.** Dark mode uses near-neutral graphite surfaces with a slight tint of the theme hue; the old palettes used saturated teal, violet or plum backgrounds. Light mode uses white cards on a faintly tinted background.
- **One accent per theme.** It colors the primary buttons, switches, selected states and links. Battery, success, warning and error colors stay the same in every theme.
- **Graphite** is a monochrome theme with zero chroma.
- **Wallpaper** uses `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12 and later. The prototype shows it with a blue sample wallpaper. On a phone without dynamic color, it falls back to Glacier.

The prototype’s CSS variables map to `ColorScheme` roles in `AndpopPalette.kt`:

**Dark**

| Role (CSS) | Compose | Wallpaper | Glacier | Iris | Guava | Sage | Apricot | Graphite |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `--bg` | background, surfaceContainerLowest | `#0C1015` | `#0A1113` | `#100F15` | `#150E0F` | `#0C110E` | `#140F0B` | `#101010` |
| `--s1` | surface, surfaceContainerLow | `#141A20` | `#111B1D` | `#191820` | `#201718` | `#141C17` | `#1F1813` | `#191919` |
| `--s2` | surfaceContainer | `#1C232A` | `#192427` | `#22212A` | `#2A1F20` | `#1C251F` | `#29201A` | `#222222` |
| `--s3` | surfaceContainerHigh | `#252C35` | `#202E31` | `#2B2A34` | `#342829` | `#242E27` | `#332922` | `#2B2B2B` |
| `--s4` | surfaceContainerHighest | `#2E3640` | `#29383C` | `#353440` | `#3F3133` | `#2D3931` | `#3E332B` | `#353535` |
| `--on` | onSurface, onBackground | `#EFF2F6` | `#EDF3F4` | `#F2F1F6` | `#F6F0F1` | `#EFF3F0` | `#F5F1EE` | `#F2F2F2` |
| `--on2` | onSurfaceVariant | `#B7C0CB` | `#B3C3C6` | `#BFBDCA` | `#CABBBD` | `#B7C3BA` | `#C9BDB4` | `#BFBFBF` |
| `--on3` | outline | `#939CA8` | `#8E9FA3` | `#9B99A7` | `#A79699` | `#929F96` | `#A5998F` | `#9B9B9B` |
| `--pri` | primary | `#99C8FE` | `#68D6EC` | `#C5B9FE` | `#FEA8B7` | `#8DD8A9` | `#F8B37F` | `#E8E8E8` |
| `--onpri` | onPrimary | `#091E34` | `#002228` | `#1E1832` | `#301218` | `#062313` | `#2E1603` | `#121212` |
| `--pc` | primaryContainer, secondaryContainer | `#213B57` | `#00414B` | `#3A3356` | `#532B33` | `#1D422D` | `#503117` | `#3D3D3D` |
| `--onpc` | onPrimaryContainer | `#D6E8FE` | `#C9EEF6` | `#E6E2FE` | `#FEDCE1` | `#D3EEDC` | `#FCE0CD` | `#E6E6E6` |
| `--glow` | top glow (not a ColorScheme role) | `#1A2F46` | `#04343C` | `#2E2945` | `#432329` | `#183524` | `#402713` | `#2E2E2E` |

**Light**

| Role (CSS) | Compose | Wallpaper | Glacier | Iris | Guava | Sage | Apricot | Graphite |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `--bg` | background, surfaceContainerLowest | `#F5F8FC` | `#F3F9FA` | `#F8F7FC` | `#FCF6F7` | `#F5F9F6` | `#FBF7F4` | `#F8F8F8` |
| `--s1` | surface, surfaceContainerLow | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| `--s2` | surfaceContainer | `#EAF0F7` | `#E7F1F3` | `#EFEEF6` | `#F6ECEE` | `#EAF1EC` | `#F5EDE8` | `#EFEFEF` |
| `--s3` | surfaceContainerHigh | `#DEE6EF` | `#DBE8EB` | `#E5E4EF` | `#EFE1E3` | `#DEE8E1` | `#EDE3DC` | `#E5E5E5` |
| `--s4` | surfaceContainerHighest | `#D2DCE7` | `#CEDEE2` | `#DBD9E6` | `#E6D6D8` | `#D2DED6` | `#E4D8CF` | `#DBDBDB` |
| `--on` | onSurface, onBackground | `#12181E` | `#0F191B` | `#17161E` | `#1E1416` | `#111914` | `#1D1510` | `#171717` |
| `--on2` | onSurfaceVariant | `#47515C` | `#425357` | `#504E5B` | `#5B4B4D` | `#46544B` | `#594D44` | `#505050` |
| `--on3` | outline | `#5F6973` | `#5B6B6E` | `#676672` | `#726365` | `#5F6B63` | `#71655C` | `#676767` |
| `--pri` | primary | `#1566B1` | `#007182` | `#6852AD` | `#A33A55` | `#057846` | `#965002` | `#1F1F1F` |
| `--onpri` | onPrimary | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| `--pc` | primaryContainer, secondaryContainer | `#D5E8FF` | `#C5EFF8` | `#E6E2FF` | `#FFDCE1` | `#D0EFDA` | `#FEE0C9` | `#DEDEDE` |
| `--onpc` | onPrimaryContainer | `#052C51` | `#01323A` | `#2D224F` | `#4B1624` | `#00351C` | `#442101` | `#2B2B2B` |
| `--glow` | top glow (not a ColorScheme role) | `#D6E8FE` | `#C9EEF6` | `#E6E2FE` | `#FEDCE1` | `#D3EEDC` | `#FCE0CD` | `#E6E6E6` |

Shared semantic colors, dark / light. Success (connected, selected check) is `#7BE0A0` / `#1B7A43`. Warning (low battery) is `#FFB68A` / `#A3470F`. Error is `#FFB4AB` / `#B3261E`. The charging fill is `#69D447` / `#3AAA35`, and the bolt is `#0E1A12` with a pale edge.

The page background adds a soft radial glow from the top (`--glow`). Panels use flat tonal surfaces with a 1 dp hairline at 7% instead of a gradient and border.

### Type, shape, spacing

| Style | Size / line height / weight | Used for |
| --- | --- | --- |
| Display | 32/40 · 650 | Onboarding titles only |
| Headline | 28/35 · 600 | Large page titles on detail screens |
| Title large | 22/28 · 620 | Device name, sheet and dialog titles |
| Title medium | 16/24 · 600 | Top bar, tile titles |
| Body large / medium | 16/23 · 400, 14/20 · 400 | Row titles / supporting text |
| Label large | 14/20 · 600 | Section headers, in sentence case and the on-surface-variant color |
| Battery value | 24/28 · 700, tabular figures | Percentages |

- Page margin 16 dp, content max width 560 dp. Two panes from 840 dp: dashboard on the left, detail on the right.
- Radii: hero card 28 dp, list groups 24 dp outside and 6 dp between rows with a 2 dp gap (the Android 16 Settings style), tiles 20–22 dp, controls full pill.
- Minimum touch target 48 dp. Rows are at least 56 dp.
- Shadows only on the sheet, dialogs, popup and widgets.

### Components

| Component | Replaces | Notes |
| --- | --- | --- |
| `AndpopTopBar` | `StyledScaffold` top bar, dashboard header row | A large title in the content collapses into the bar on scroll. Edge-to-edge behind the status bar. |
| `HeroDeviceCard` | `DeviceHeader` and the `BatterySection` panel | Name, model, status pill, refresh, battery cells, “Updated…”, Battery details |
| `BatteryCell`, `BatteryGlyph` | Same names in `AndpopBattery.kt` | Glyph rule unchanged: `level != null && charging && !lastSeen`, per component |
| `ListeningModeControl` | `DashboardListeningModes` | Sliding indicator in one row; 2 × 2 grid at font scale ≥ 1.3 or when the labels don’t fit; single column at ≥ 1.8 |
| `ListGroup`, `ListRow` | `ControlsGroup`, `DeviceSettingRow`, `StyledList`, `StyledListItem`, `StyledToggle` | One row with slots: leading icon, title, supporting text, value, and a trailing switch, radio, checkbox or chevron. At font scale ≥ 1.6 the value moves under the title. |
| `AndpopSwitch` | M3 `Switch`, `StyledSwitch` | M3 switch with a check icon in the thumb |
| `SegmentedChoice`, `FilterChips` | Radio lists for speeds and durations, connection switches | |
| `AndpopSlider` | `StyledSlider`, raw `Slider` | Value label, end labels, optional icons |
| `AndpopTextField` | `OutlinedTextField`, `StyledInputField` | Leading icon, show/hide, live rules, error text |
| `InfoBanner` | Hard-coded Play banner, case hint text | Info, warning, error |
| Buttons | `StyledButton`, raw M3 buttons | Filled, tonal, outlined, text and danger, 48 dp; 56 dp for primary page actions |

### Motion and haptics

| Interaction | Motion | Haptic |
| --- | --- | --- |
| Select listening mode | Indicator springs (dampingRatio 0.7, medium stiffness); check pops in | `SEGMENT_FREQUENT_TICK` |
| Toggle | Thumb grows 16 → 24 dp with a check | `TOGGLE_ON` / `TOGGLE_OFF` |
| Navigate | Shared-axis X, 300 ms. Predictive back scales the page to 0.9. | none |
| Refresh battery | Icon spins, values shimmer, numbers count up | `CONFIRM` |
| Blocked action (unchecking one of the last two cycle modes) | Checkbox shakes and the helper text turns to the error color | `REJECT` |
| Remove animations is on | 150 ms cross-fades only | unchanged |

## Screen inventory and where each lands in Compose

Paths are relative to `app/src/main/java/me/kavishdevar/librepods/`.

| Screen | File(s) | Main changes |
| --- | --- | --- |
| Splash | `MainActivity.kt`, `res/values/themes.xml` | `installSplashScreen()` held until the profile and theme load |
| Welcome, Privacy, Permissions, Not supported | `presentation/screens/onboarding/*` | Three steps. Per-permission Allow buttons replace “Grant all”. The privacy text mentions the optional account. Strings move to resources. |
| Dashboard | `AndpopDashboard.kt`, `AndpopBattery.kt`, `AndpopListeningModes.kt`, `AndpopDeviceSettings.kt` | Hero card, tonal groups, avatar, L/R tiles, disconnected / unpaired / loading states |
| Battery details | `AndpopBattery.kt` (`BatteryQuickView`) | Rows with photo, status text, level bar, glyph and an explanation of ~ and last seen |
| Name, Microphone, Press and hold, Accessibility, Firmware | `RenameScreen.kt`, `MicrophoneSettingsScreen.kt`, `PressAndHoldSettingsScreen.kt`, `AccessibilitySettingsScreen.kt`, `VersionInfoScreen.kt` | Explicit Save for the name. The “two modes minimum” rule is now visible. Segmented speeds. Copyable versions. |
| All device settings | `AirPodsSettingsScreen.kt` and `components/*Settings.kt` | Grouped and de-duplicated. Premium banner and unlock button removed. Disconnect asks for confirmation. |
| Equalizer, Adaptive audio, Head gestures, Hearing aid, Adjustments, Transparency, Hearing protection, Call controls | matching `*Screen.kt` files | Shared slider and list templates. Head-gesture test shows a clear result; the velocity plot moves to developer options. |
| Settings, Credits | `AppSettingsScreen.kt`, `theme/AndpopPalette.kt` | Live preview cards for System, Light and Dark. Seven color swatches. overlay-permission banner, filter chips, privacy policy, app version, collapsible phone info, trademark notice. Credits stay a dialog with OK. |
| Troubleshooting | `TroubleshootingScreen.kt` | Three-step recorder, saved logs with share and delete plus Undo, common fixes. Xposed steps only when Xposed is present. |
| Developer options | new `DeveloperOptionsScreen.kt` | Hidden by default. Hidden entirely when `BuildConfig.PLAY_BUILD` is true. |
| Account, Log in, Create account, Reset password | `account/AccountScreen.kt`, `AppNavGraph.kt` (new `Screen.Account`) | Normal destination. Avatar entry. Live password rules. Explained delete. Account service form removed from here. |
| Connection popup, Dynamic Island | `overlays/PopupWindow.kt`, `overlays/IslandWindow.kt` | Photo battery cells replace the video. Ring and Undo in the island. “Linux” strings removed. |
| Notification, QS tile | `services/AirPodsService.kt`, `services/AirPodsQSService.kt` | One informative notification, a silent service notification, renamed channels |
| Widgets | `presentation/widgets/*`, `res/layout/*widget*.xml` | Themed rings, segmented mode widget, Glance (decision 7) |
| Release notes, Open-source licenses, Loading | `ReleaseNotesScreen.kt`, `OpenSourceLicensesScreen.kt`, `LoadingScreen.kt` | Shared components. Release notes link to this repository instead of `kavishdevar/librepods`. |

The legacy `PurchaseScreen`, `BillingProvider*` UI strings and the FOSS premium banner can be removed. `isPremium` is always true and the README promises no in-app purchases.

Not changed by this redesign: the hearing test entry form (`UpdateHearingTestScreen.kt`) gets the shared text field but keeps its layout. The commented-out camera control stays out. The privileged “Disconnect when not wearing” row keeps its `BLUETOOTH_PRIVILEGED` condition.

### Navigation

```mermaid
flowchart LR
  Splash --> Welcome --> Privacy --> Permissions --> Dashboard
  Permissions -. unsupported phone .-> NotSupported[Not supported]
  Welcome -. I have an account .-> Login[Log in]
  Dashboard --> Account --> Login & SignUp[Create account]
  Login --> Reset[Reset password]
  Dashboard --> Settings --> Troubleshooting & Credits & DevOptions[Developer options]
  Dashboard --> BatterySheet[Battery details sheet]
  Dashboard --> Name & Microphone & Accessibility & PressHold[Press and hold L / R] & Firmware
  Dashboard --> AllSettings[All device settings]
  AllSettings --> Equalizer & Adaptive[Adaptive audio] & HearingAid[Hearing aid] & HeadGestures[Head gestures] & Calls[Call controls]
  Accessibility --> Transparency[Transparency mode]
```

## Accessibility

- Every interactive element is at least 48 dp and has a TalkBack name. Each battery cell merges into one node, for example “Right bud, 48 percent, charging”.
- Listening modes are a radio group with Selected state. The check mark gives a selection cue that doesn’t depend on color.
- Layout adapts to font scale: one row → 2 × 2 → single column for modes, cells → rows for battery, values move under titles. See the [200% export](screens/23-large-text-200.png).
- Text colors meet WCAG AA (4.5:1) against the surfaces they sit on, in all fourteen theme and appearance combinations. The lowest pair is the hint color (`--on3`) on a surface container, at 4.8:1.
- Errors and refresh status use polite live regions.

## Google Play readiness

**UI** items are part of this redesign. **Code** items are outside the UI but block or risk publication. **Store** items are Play Console work.

| | Item | Status in this proposal |
| --- | --- | --- |
| UI | In-app privacy summary that matches the Data safety form, including the optional account | Designed (Privacy, Settings) |
| UI | In-app account deletion, plus a web deletion link for the listing | Designed (Account · signed in); the web page is still needed |
| UI | Request permissions in context with a reason; nothing chained | Designed (Permissions) |
| UI | Hide compatibility bypass, account service form, demo mode and Xposed options from Play users | Designed (Developer options) |
| UI | Remove LibrePods purchase banner, “Unlock advanced features”, “Connected to Linux” and other leftover strings | Listed above |
| UI | Edge-to-edge, predictive back and font scale up to 200% | Designed. The two-pane large-screen layout is specified but not drawn. |
| UI | Trademark notice: “AnDPoP is not affiliated with Apple Inc.” | Designed (Settings footer, listing) |
| Code | Add a `play` product flavor with `PLAY_BUILD = true`. Only `foss` exists today, so every Play-specific condition in the code is currently dead. | To do |
| Code | Protected permissions in the manifest (`BLUETOOTH_PRIVILEGED`, `MODIFY_PHONE_STATE`, `LOCAL_MAC_ADDRESS`, `INTERACT_ACROSS_USERS`) and Xposed hooks. Expect review questions. Strip them from the Play flavor if they are not needed there. | To decide |
| Code | Foreground service type `connectedDevice` needs the Play Console declaration and a short video | To do |
| Code | Move hard-coded English strings (permissions, privacy, account, head tracking, troubleshooting) to resources | To do |
| Code | Rename the notification channel that shows `BluetoothConnectionManager.aacpSocket?` | To do |
| Store | Icon 512 px, feature graphic 1024 × 500, 4–8 phone screenshots | Drafted on the [store board](screens/26-play-store-assets.png); final exports come from the native build |
| Store | Title without third-party marks: “AnDPoP”. Describe AirPods compatibility in the descriptions. | Drafted |
| Store | Hosted privacy policy URL and account deletion URL | Needed from you |
| Store | Data safety: email collected for account management, optional, not shared, encrypted in transit, deletable | Drafted |

## Existing tests the port must keep passing

`AppearanceRegressionTest` and `DashboardRegressionTest` rely on these:
- the texts “Adaptive”, “Transparency”, “Guava”, “Log in”, “Forgot password?”, “Send reset link”, “Create an account”, “Continue as guest”, “Battery details” and “Right bud”;
- the content descriptions “Settings”, “Charging” and “Close battery view”;
- the credits dialog containing “Based on LibrePods” with an OK button;
- a `ProgressBarRangeInfo` on each live gauge;
- “Transparency” on one line at 288 dp and 1.6× font scale;
- Settings showing “Appearance” after the dashboard’s Settings button.

The prototype keeps all of them. Three need care:
- The account entry changes from a text button to an avatar. Keep its content description “Account”.
- Account becomes a nav destination (decision 1). Its test can keep calling `AccountScreen(state, model, null)` directly.
- The appearance test loops over Glacier, Iris and Guava in dark and light, and writes `andpop_dark_tint` / `andpop_light_tint`. With one color for both modes (decision 10), the loop becomes seven themes × two appearances and checks `andpop_palette` plus the migration from the old keys. “Guava” stays a clickable, selectable swatch.

## Port plan (after approval)

1. **Foundation:** tokens in `AndpopPalette.kt`, bundled Roboto Flex, the shared components and the motion spec. Delete the Apple branch.
2. **Dashboard and battery:** hero card, listening control, groups, tiles, sheet, empty and loading states. Run the existing instrumented tests.
3. **Detail screens:** All device settings and every sub-screen on the shared templates.
4. **Settings, troubleshooting, developer options.**
5. **First run and account:** splash, onboarding, account as a destination.
6. **System surfaces:** popup, island, notification, QS tile, widgets.
7. **Play flavor and store:** `play` flavor, manifest review, strings, final store graphics from the build.

Each step builds with `./gradlew :app:assembleFossDebug` and runs `connectedFossDebugAndroidTest` before the next one starts.

## Open questions

1. Do you approve decisions 1–10, or should any keep the current behavior?
2. What are the URLs for the hosted privacy policy and the account deletion page?
3. Should the Play build keep Xposed features, or should they stay FOSS-only?
4. Do you want the new icon, or only a refresh of the current one?

---

Product photos: AirPods Pro 2 by Hajoon0102 (CC BY-SA 4.0) and AirPods Max by Arne Müseler (CC BY-SA 3.0 DE), as credited in the main README. AirPods is a trademark of Apple Inc.
