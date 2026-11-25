# AnDPoP 2.0.0 Google Play Readiness & Technical Audit

**Audit date:** 2026-11-25  
**Branch:** `feature/play-phase-0-audit` (baseline commit `8a422a8` on `main`)  
**Scope:** Baseline build, lint, 16 KB page alignment, manifest permissions, exported components, Xposed/root/native code, billing, network endpoints, storage/backup rules, hardcoded strings, and leftover branding. No app source files were modified in Phase 0.

---

## 1. Environment & Toolchain

| Item | Observed Value |
| --- | --- |
| Git status & branch | `feature/play-phase-0-audit` (`main` at `8a422a8`, clean working tree except untracked `PLAY_RELEASE_PROMPT.md`) |
| Java / JDK | Android Studio JBR `openjdk version "25.0.4"` (`C:\Program Files\Android\Android Studio\jbr`); `compileOptions` Java 21 |
| `local.properties` | Absent (SDK passed via `ANDROID_HOME=C:\Users\waiya\AppData\Local\Android\Sdk`) |
| Gradle Wrapper | `9.4.1` (Kotlin `2.3.0` DSL, AGP `9.2.1`, Kotlin plugin `2.3.21`) |
| Android SDK | `compileSdk = 37`, `targetSdk = 37`, `minSdk = 33`, `build-tools 36.0.0`, `ndkVersion = "29.0.14206865"`, `cmake 3.22.1` |
| Connected ADB devices | None at Phase 0 start (`Pixel_8_Pro` AVD available locally targeting `android-37.0` `x86_64` with Google Play) |

---

## 2. Baseline Build, Lint, and 16 KB Page-Size Verification

### 2.1 Gradle Builds
- `:app:assembleFossDebug` — **PASS** (`app/build/outputs/apk/foss/debug/app-foss-debug.apk`, `48,786,895` bytes)
- `:app:assembleFossRelease` — **PASS** with R8 (`isMinifyEnabled = true`, `isShrinkResources = true`; `app/build/outputs/apk/foss/release/app-foss-release-unsigned.apk`, `8,246,122` bytes)

### 2.2 Lint (`:app:lintFossDebug`)
- **Result:** **FAIL** — `125 errors, 250 warnings`
- **Error breakdown:**
  - `MissingTranslation` (`111` errors): New `andpop_*` keys in `andpop_strings.xml`, `andpop_v2_strings.xml`, and `andpop_dashboard_status.xml` missing across the 12 non-English `values-*` locale folders.
  - `UseAppTint` (`13` errors): `android:tint` used instead of `app:tint` in `res/layout/battery_widget.xml` (8), `res/layout/listening_mode_widget.xml` (4), and `res/layout/island_window.xml` (1).
  - `NewApi` (`1` error): `services/AirPodsService.kt:2854` calls `BluetoothDevice.disconnect()` (API 37) without an API 37 runtime check (`minSdk = 33`).
- **Top warning categories:** `UnusedResources` (111), `ObsoleteSdkInt` (40), `Typos` (35), `GradleDependency` (15), `NewerVersionAvailable` (12), `TypographyEllipsis` (9), `Untranslatable` (5), `IconDuplicates` (4), `IconLocation` (3), `UseTomlInstead` (3), `AndroidGradlePluginVersion` (2), `UseKtx` (1), `StaticFieldLeak` (1), `PluralsCandidate` (1), `PrivateResource` (1).

### 2.3 16 KB Page-Size Check
- **`zipalign -c -P 16 -v 4`** (`build-tools/36.0.0/zipalign.exe`) on `app-foss-debug.apk` and `app-foss-release-unsigned.apk`: **PASS (`Verification successful`)** for all `.so` entries (`libandroidx.graphics.path.so`, `libbluetooth_socket.so`, `libl2c_fcr_hook.so` across `arm64-v8a` and `x86_64`).
- **`llvm-readelf.exe -l`** (`ndk/29.0.14206865`) on all merged `.so` files in `fossRelease`:
  - `arm64-v8a/libandroidx.graphics.path.so`: all `LOAD` segments `Align = 0x4000` (16384) — **PASS**
  - `arm64-v8a/libbluetooth_socket.so`: all `LOAD` segments `Align = 0x4000` (16384) — **PASS**
  - `arm64-v8a/libl2c_fcr_hook.so`: all `LOAD` segments `Align = 0x4000` (16384) — **PASS**
  - `x86_64/libandroidx.graphics.path.so`: all `LOAD` segments `Align = 0x4000` (16384) — **PASS**
  - `x86_64/libbluetooth_socket.so`: all `LOAD` segments `Align = 0x4000` (16384) — **PASS**
  - `x86_64/libl2c_fcr_hook.so`: all `LOAD` segments `Align = 0x4000` (16384) — **PASS**
- **Recommendation:** Explicitly add `-Wl,-z,max-page-size=16384` to `target_link_options` in `app/src/main/cpp/CMakeLists.txt` for both `bluetooth_socket` and `l2c_fcr_hook`.

---

## 3. Manifest Permissions Audit (`app/src/main/AndroidManifest.xml`)

18 active `<uses-permission>` entries (`<uses-feature android:name="android.hardware.telephony" android:required="false" />`):

| Permission | Code Usage (`file:line`) | Play Risk | Play Flavor Action & Justification |
| --- | --- | --- | --- |
| `BLUETOOTH_CONNECT` | `AirPodsService.kt` (L661, 730–745, 1803, 2247, 2555, 2603, 3051), `PermissionsPage.kt:94` | Standard runtime | **KEEP** — core AirPods Bluetooth connection and battery/mode control. |
| `BLUETOOTH_SCAN` | `BLEManager.kt:95, 149, 199, 213` (`neverForLocation`), `PermissionsPage.kt:95` | Standard runtime | **KEEP** — scans Apple BLE proximity broadcasts (`0x004C`) for case battery & lid state. |
| `FOREGROUND_SERVICE` | `MainActivity.kt:115`, `BootReceiver.kt:32`, `AirPodsService.kt:1749` | Requires FGS type | **KEEP** — maintains active companion connection while AirPods are in use. |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | `AndroidManifest.xml:119` (`AirPodsService`) | **Play Console FGS declaration + video required** | **KEEP** — continuous Bluetooth L2CAP/BLE accessory connection (`connectedDevice`). |
| `POST_NOTIFICATIONS` | `AirPodsService.kt:1749, 2054`, `PermissionsPage.kt:84` | Standard runtime | **KEEP** — shows battery/mode controls and silent FGS notification. |
| `RECEIVE_BOOT_COMPLETED` | `BootReceiver.kt:30-45`, `AndroidManifest.xml:142-151` | Low | **KEEP** — restores background AirPods listener after reboot/package update (guard by `onboarding_complete` & `BLUETOOTH_CONNECT`). |
| `SYSTEM_ALERT_WINDOW` | `PopupWindow.kt:75`, `IslandWindow.kt:250`, `AirPodsService.kt:1629, 1656`, `AppSettingsScreen.kt:179` | Special access (user-granted in Settings) | **KEEP (Optional)** — user-toggled connection sheet & audio switch pill overlays. |
| `READ_PHONE_STATE` | `AirPodsService.kt:610, 3091`, `PermissionsPage.kt:68` | Sensitive runtime | **KEEP (Optional)** — detects incoming call ringing state for call audio takeover / head gestures. |
| `ANSWER_PHONE_CALLS` | `AirPodsService.kt:2098, 2125`, `PermissionsPage.kt:69` | High-scrutiny phone permission | **KEEP (Optional)** — only requested when user grants Phone permission to accept/decline calls via head gestures. |
| `MODIFY_AUDIO_SETTINGS` | `AirPodsService.kt:560`, `MediaController.kt:38, 357`, `QuickSettingsDialogActivity.kt:326` | Normal | **KEEP** — adjusts media volume during Conversation Awareness. |
| `INTERNET` | `SupabaseAccountClient.kt:89` (`HttpsURLConnection`) | Normal | **KEEP** — optional Supabase account sign-in/sign-up/recovery/deletion. |
| `BLUETOOTH_ADVERTISE` | Unused (`PermissionsPage.kt:98` requests it; 0 functional usages) | Unnecessary | **REMOVE from `main`** — leftover from removed CrossDevice code. |
| `BLUETOOTH` | Unused on `minSdk = 33` (`PermissionsPage.kt:96`) | Legacy <= API 30 | **REMOVE from `main`** — obsolete above API 30. |
| `BLUETOOTH_ADMIN` | Unused on `minSdk = 33` (`PermissionsPage.kt:97`) | Legacy <= API 30 | **REMOVE from `main`** — obsolete above API 30. |
| `BLUETOOTH_PRIVILEGED` | `AirPodsService.kt:1813, 2288, 2839, 2914, 2975`, `AppSettingsScreen.kt:227`, `AirPodsViewModel.kt:808` | **Signature\|Privileged** | **REMOVE in `play`** via `tools:node="remove"` (ungrantable to Play apps; Xposed/system-only). |
| `MODIFY_PHONE_STATE` | `AirPodsService.kt:2859, 2941, 3016` | **Signature\|Privileged** | **REMOVE in `play`** via `tools:node="remove"`. |
| `LOCAL_MAC_ADDRESS` | `AirPodsService.kt:369` (`bluetoothAdapter.address` / `su` fallback) | **Signature\|Privileged** | **REMOVE in `play`** via `tools:node="remove"`. |
| `INTERACT_ACROSS_USERS` | `AirPodsService.kt:2201` (`sendBroadcastAsUser`) | **Signature\|Privileged** | **REMOVE in `play`** via `tools:node="remove"`. |

---

## 4. Exported Components Audit

| Component | `exported` | Findings & Action |
| --- | --- | --- |
| `.MainActivity` | `true` | Launcher + `app.andpop.android://auth/callback/` deep link (validated by per-request PKCE `nonce`). OK. |
| `.services.AirPodsService` | `true` | **Security gap:** `exported="true"` guarded only by normal runtime `BLUETOOTH_CONNECT`, plus dynamic `RECEIVER_EXPORTED` receivers (`L554, 620, 706, 719, 1316`). Set `android:exported="false"` and use `RECEIVER_NOT_EXPORTED` for internal broadcasts. |
| `.services.AirPodsQSService` | `true` | Quick Settings tile guarded by `BIND_QUICK_SETTINGS_TILE`. Hardcoded `android:label="ANC Mode"` and Apple `@drawable/airpods` icon -> localize & use v2 monochrome icon. |
| `.receivers.BootReceiver` | `true` | Listens for `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`. Must check `onboarding_complete` & `BLUETOOTH_CONNECT` before `startForegroundService`. |
| `.presentation.widgets.BatteryWidget` | `false` | **Bug:** `AppWidgetProvider` must be `android:exported="true"` so launchers/system host can deliver `APPWIDGET_UPDATE`. |
| `.presentation.widgets.NoiseControlWidget` | `false` | **Bug:** same `exported="false"` defect. |
| `.QuickSettingsDialogActivity` | `false` | Internal dialog opened by `AirPodsQSService`. |
| `androidx.core.content.FileProvider` | `false` | Shares `filesDir/logs/` (`res/xml/file_paths.xml`). OK. |

---

## 5. Xposed, Root, and Native C++ Inventory

| Item | Location | Dependency | Play Flavor Disposition |
| --- | --- | --- | --- |
| `libbluetooth_socket.so` | `cpp/bluetooth_socket.cpp`, `AirPodsService.kt:251` | None (rootless JNI `pthread` calling `VMRuntime.setHiddenApiExemptions` for L2CAP PSM `4097` / `31`) | Keep for rootless AACP L2CAP socket in `foss`; audit/review for `play`. |
| `libl2c_fcr_hook.so` | `cpp/l2c_fcr_hook.cpp`, `META-INF/xposed/native_init.list` | Xposed (`libxposed` API 101) | **Exclude in `play`** (packaging + `foss` source set). |
| `KotlinModule`, `XposedServiceHolder`, `XposedRemotePrefImpl` | `utils/KotlinModule.kt`, `utils/XposedServiceHolder.kt`, `data/XposedRemotePrefImpl.kt` | `io.github.libxposed:api` / `service` | **Move to `foss` source set**; provide no-op stubs in `play`. |
| `NativeBridge` | `utils/KotlinModule.kt:153` | JNI `Java_me_kavishdevar_librepods_utils_NativeBridge_setSdpHook` | **Missing ProGuard keep rule** in `app/proguard-rules.pro` (causes R8 obfuscation crash in `fossRelease` when Xposed SDP hook is toggled). |
| `LogCollector` | `utils/LogCollector.kt` | Currently uses `su -c` everywhere & hardcodes `dumpsys package me.kavishdevar.librepods` | **Rewrite** to capture own-PID `logcat` without `su` and use `context.packageName`; restart `AirPodsService` without root. |
| `RadareOffsetFinder` | `utils/RadareOffsetFinder.kt` | 100% dead root + GitHub download code (`HttpURLConnection`) | **Delete** unused file. |
| `RootlessSupport` | `utils/RootlessSupport.kt` | `SDK_INT >= 37`, Pixel 16 QPR2 (`CP1A`), Oppo/OnePlus/Realme 16+, or `"bypass_device_check.v2"` | Ignore `"bypass_device_check.v2"` and hide bypass UI when `BuildConfig.PLAY_BUILD == true`. |

---

## 6. Billing Code & Dependencies

- `gradle/libs.versions.toml` defines unused `billing = "8.3.0"` and `play = "2.0.2"` (`play-review`), though neither is in `app/build.gradle.kts` `dependencies`.
- Dead billing code present in `app/src/main/java/.../billing/` (`BillingManager.kt`, `BillingProvider.kt`, `BillingProviderFactory.kt`, `FOSSBillingProvider.kt`), `presentation/screens/PurchaseScreen.kt`, `presentation/viewmodel/PurchaseViewModel.kt`, `Screen.Purchase`, and `"Unlock advanced features"` buttons across 5 detail screens.
- `AirPodsSettingsScreen.kt:274-308` still renders a hardcoded `#32829B` Play refund banner (`mailto:billing@kavish.xyz`, `"Refund Request for LibrePods on Play Store"`).
- **Plan:** Remove billing code, `PurchaseScreen`, `PurchaseViewModel`, `Screen.Purchase`, unused TOML billing/play-review entries, and FOSS refund banner.

---

## 7. Network Endpoints

1. **Default Supabase Auth Project** (`account/AccountViewModel.kt:21-24`): `https://btfbwwnbkarlpcqmkzzb.supabase.co` (HTTPS only via `HttpsURLConnection` in `SupabaseAccountClient.kt` with `instanceFollowRedirects = false`):
   - `POST /auth/v1/token?grant_type=password`
   - `POST /auth/v1/signup?redirect_to=...`
   - `POST /auth/v1/recover?redirect_to=...`
   - Browser `ACTION_VIEW` `/auth/v1/authorize?provider={google|apple}&...` -> `app.andpop.android://auth/callback/<nonce>`
   - `POST /auth/v1/token?grant_type=pkce`
   - `POST /auth/v1/token?grant_type=refresh_token`
   - `PUT /auth/v1/user`
   - `POST /auth/v1/logout?scope=local`
   - `POST /rest/v1/rpc/delete_my_andpop_account`
2. **GMS Downloadable Fonts** (`presentation/theme/Type.kt:91-95`): `com.google.android.gms.fonts` (`"Roboto Flex"`). Replaced in Phase 1 by bundled `res/font/roboto_flex.ttf` (`Decision 8`), removing external font network/GMS dependency.
3. **Dead `RadareOffsetFinder.kt:43`**: GitHub archive download URL (file unused, to be deleted).

---

## 8. Data Storage & Backup Rules

- **`AccountVault`** (`account/AccountStorage.kt:25-88`): Stores Supabase URL/key, OAuth/JWT tokens, email, user ID, and PKCE state using `AndroidKeyStore` `AES/GCM/NoPadding` inside **`context.noBackupFilesDir/andpop-account-v1`**. **Excluded from backup automatically.**
- **Backup Rule Defect (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`)**: Both files contain **only commented-out sample XML**, while `AndroidManifest.xml:46` has `android:allowBackup="true"`. Consequently:
  - `shared_prefs/settings.xml` (containing BLE `IRK`, `ENC_KEY`, AirPods/device MACs, serial numbers, audiogram values, and per-profile snapshots) is **backed up in plaintext**.
  - `shared_prefs/packet_logs.xml` (up to 1,000 raw AACP hex packets) is **backed up in plaintext**.
  - `filesDir/logs/*.txt` (logcat dumps) is **backed up in plaintext**.
- **Fix in Phase 2:** Explicitly exclude `settings.xml`, `packet_logs.xml`, and `logs/` in both `backup_rules.xml` and `data_extraction_rules.xml` (`cloud-backup` and `device-transfer`).

---

## 9. Hardcoded Strings & Leftover LibrePods Branding

- **Hardcoded user-visible English strings:** **217 strings across 19 files** (top offenders: `AccountScreen.kt` 62, `TroubleshootingScreen.kt` 44, `AccountViewModel.kt` 24, `AirPodsService.kt` 20, `PermissionsPage.kt` 17, `QuickSettingsDialogActivity.kt` 8, `HeadTrackingScreen.kt` 7, `AirPodsQSService.kt` 7).
- **Critical leftover branding & string bugs:**
  1. `res/values-it/strings.xml:2` overrides `<string name="app_name">LibrePods</string>` (Italian devices show **"LibrePods"** instead of **"AnDPoP"**).
  2. `AirPodsService.kt:1762, 2059, 2782` contains broken refactor text `"BluetoothConnectionManager.aacpSocket? created, but not connected. Check logs"`.
  3. `res/values/strings.xml:45,47` and all 12 locale files contain `"Connected to Linux"` / `"Moved to Linux"`.
  4. `ReleaseNotesScreen.kt:120` links to `https://github.com/kavishdevar/librepods/releases/v$versionName`.
  5. `LogCollector.kt:70` runs `dumpsys package me.kavishdevar.librepods` instead of `app.andpop.android`.
  6. `PermissionsPage.kt:242, 280` sets `contentDescription = "bluetooth"` on Phone and Overlay icons.
  7. Apple SF Symbol Private Use Area characters (`\uDBC0\uDEA1`, `\uDBC3\uDC8E`, etc.) render as missing-glyph tofu boxes in `PopupWindow.kt`, `AccessibilitySettingsScreen.kt`, `AdaptiveStrengthScreen.kt`, `HearingAidAdjustmentsScreen.kt`, `TransparencySettingsScreen.kt`, and `BatteryView.kt`.

---

## 10. Addendum (second review, 2026-11-25)

A second pass checked the first audit against the code and current Android and Play documentation.

### 10.1 Corrections to sections 3–5

| Item | Correction |
| --- | --- |
| Widgets `exported="false"` (section 4) | Not a bug. The system's app-widget host delivers `APPWIDGET_UPDATE` from the system UID, which can reach non-exported receivers; Android Studio's widget template uses `exported="false"`. Keep as is. |
| `BOOT_COMPLETED` start of the `connectedDevice` service | Allowed. Android 15 blocks only `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` and `microphone` services from `BOOT_COMPLETED` receivers ([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15#fgs-boot-completed)). The real risk is that a `connectedDevice` service needs a Bluetooth runtime permission when it starts ([foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)). `BootReceiver` starts the service before onboarding grants Nearby devices; it must check `onboarding_complete` and `BLUETOOTH_CONNECT` first. |
| `MODIFY_AUDIO_SETTINGS` | Normal permission, used by `MediaController` for Conversation Awareness volume changes. Keep in both flavors. |
| `READ_PHONE_STATE`, `ANSWER_PHONE_CALLS` | Both are runtime permissions outside Play's restricted SMS and Call Log groups. Keep, requested in context on the permissions page with their reason (head gestures for calls). |

### 10.2 Items the first audit did not cover

| Area | Finding | Play risk | Action |
| --- | --- | --- | --- |
| Hidden-API exemption | `cpp/bluetooth_socket.cpp` calls `VMRuntime.setHiddenApiExemptions` (strings XOR-obfuscated) so the app can open a classic L2CAP socket to the AirPods. This is what makes AACP work without root. | Needs your judgment. Play's [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/13315670) policy prohibits unauthorized interference with the device or APIs; Google's pages do not say whether this pattern is allowed. Obfuscated strings may look like evasion to a reviewer. | Keep: removing it removes the core feature. Describe it plainly in the review notes (`PLAY_CONSOLE.md`). Recorded as "needs-user" in the release checklist. |
| Proximity keys | `IRK` and `ENC_KEY` are stored Base64 in `shared_prefs/settings.xml` and were included in backups. | Data safety: device identifiers stored on device. | Exclude `settings.xml` from cloud backup and device transfer (Phase 2). |
| Sensitive logging | About 58 of 436 `Log` calls print Bluetooth addresses or raw packets. | Release builds should not log personal data. | R8 strips `Log.v/d/i` in release (Phase 2); the new log recorder shortens addresses to their last two bytes. |
| Exported service | `AirPodsService` is `exported="true"`, guarded only by `BLUETOOTH_CONNECT`, so any app holding that permission could bind to it or start it. | Security. | Set `exported="false"` in both flavors; only the app binds to it. |
| Root log capture | `LogCollector` ran `su -c logcat` and `killall com.android.bluetooth`. | Root features are not allowed in the Play build. | Replaced by a non-root recorder that captures only AnDPoP's own process log. |
| Billing leftovers | `observeBilling()` in two view models disabled Conversation Awareness and head gestures whenever the billing provider reported "not premium"; a `PLAY_BUILD` branch in `loadSharedPreferences()` could expire "premium". | Would have broken features in a Play build. | Removed with the billing code (Phase 1). |
| Accessibility screen side effect | Opening Accessibility sent a phone/media EQ packet with default values every time, although the controls for it are commented out. | Could reset an EQ setting on the AirPods. | Removed with the screen rewrite. |
