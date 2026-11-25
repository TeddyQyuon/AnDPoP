# Google Play Console Declarations & Review Guide (AnDPoP 2.0.0)

This document prepares exact, factual answers for every **Policy > App content** questionnaire in Google Play Console for the `play` build (`app.andpop.android`, `versionCode = 8`, `BuildConfig.PLAY_BUILD = true`).

> Always verify live forms in Play Console at submission time, as Google Play periodically updates wording or adds regional compliance questions.

---

## 1. App Access

- **Selection:** *All or some functionality is restricted* -> **No** — wait: select **"All functionality in my app is available without any access restrictions"** (or if you want to explain optional account sign-in: explain that an account is optional and guest + `"Try demo mode"` unlock every feature without credentials).
- *Recommended wording if Play asks how reviewers should test Bluetooth hardware functionality:*

```text
No login or AirPods hardware is required to review all screens and UI controls:
1. Launch AnDPoP and tap "Get started" on the Welcome screen (or "I already have an account" if testing optional Supabase email sign-in).
2. Read the Privacy screen and tap "Continue".
3. On the Permissions screen, grant "Nearby devices" (or tap "Continue" / skip optional permissions).
4. On the Pairing Guide dashboard card ("No AirPods connected"), tap "Try demo mode".
5. This opens a full interactive demo AirPods Pro 2 (USB-C) session displaying live earbud/case battery, listening mode switching, Conversation Awareness, press-and-hold stem configuration, 3-band equalizer, accessibility controls, and Appearance settings without needing physical Apple hardware.
```

---

## 2. Ads

- **Does your app contain ads?** -> **No, my app does not contain ads.**
- **Verification:** `app/build.gradle.kts` and AndroidManifest contain zero ad SDKs (no AdMob, Unity Ads, Meta Audience Network, or `AD_ID` permission).

---

## 3. Content Rating (IARC)

- **Category:** **Utility, Productivity, Communication, or Other** (`Tools` / `Audio utility`).
- **Questionnaire answers:**
  - Violence / Fear / Sexuality / Nudity / Crude Humor / Gambling / Profanity / Controlled Substances -> **No**
  - User-generated content / public social chat -> **No** (email account is personal profile/remembered-device sync only)
  - Shares user's current physical location with other users -> **No** (`BLUETOOTH_SCAN` uses `neverForLocation` and location permissions are absent)
  - Digital purchases / loot boxes -> **No** (billing code and dependencies are completely removed)
- **Expected rating:** **Everyone** / **PEGI 3** / **USK 0**.

---

## 4. Target Audience and Content

- **Target age groups:** **18 and over** (or **13–15, 16–17, 18 and over** if desired; avoid `< 13` so Families Policy requirements do not apply).
- **Could your store listing unintentionally appeal to children?** -> **No** (neutral hardware-companion iconography, technical battery/audio controls, technical store copy).
- **Policy reference:** [Target audience and app content](https://support.google.com/googleplay/android-developer/answer/9867159).

---

## 5. Data Safety

Cross-reference [DATA_SAFETY.md](DATA_SAFETY.md) directly when filling the Play Console Data safety form:

- **Collects or shares any required user data types?** -> **Yes (optional only)** if an account is created; Bluetooth companion features work 100% offline with zero data sent off-device.
- **Data type collected:** *Personal info -> Email address* (Optional; Account management only; not shared with third parties; encrypted in transit over HTTPS; user-deletable in-app and via your hosted web deletion page).
- **Account deletion requirement:** Because AnDPoP supports in-app account creation, provide `https://teddyqyuon.github.io/AndPoP_V_1.6.0/delete-account.html` (`docs/delete-account.html`, also set as `PrivacyLinks.ACCOUNT_DELETION_URL`) in Play Console ([Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)).

---

## 6. Foreground Service Permissions (`FOREGROUND_SERVICE_CONNECTED_DEVICE`)

- **Policy references:**
  - [Understanding foreground service and full-screen intent requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
  - [Android 14 foreground service types — connectedDevice](https://developer.android.com/about/versions/14/changes/fgs-types-required#connected-device)
- **Declared manifest permission & service:** `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE` on `me.kavishdevar.librepods.services.AirPodsService` (`android:foregroundServiceType="connectedDevice"`, `android:exported="false"`).
- **Play Console text — Describe how this foreground service is used:**

```text
AnDPoP starts AirPodsService (foregroundServiceType="connectedDevice") only while communicating with the user's connected Bluetooth AirPods/Beats headset over RFCOMM/L2CAP (AACP) and Bluetooth audio profiles. It receives live battery/charging packets, listening mode changes (Noise Cancellation / Transparency / Adaptive / Off), in-ear detection state, and head-gesture/stem control events, and updates the single ongoing status notification, home-screen widgets, and Quick Settings tile in real time.
```

- **What is the impact if this task is deferred?**

```text
Deferring the connection prevents Android from maintaining the live Bluetooth AACP socket and profile listeners after the user puts the phone in their pocket or switches apps, causing delayed or missing battery readings, broken stem-gesture triggers, and stale listening mode controls.
```

- **What is the impact if the system interrupts the service?**

```text
Interrupting the service severs the active AACP Bluetooth control socket to the user's earbuds, immediately stopping live battery updates, in-ear pause/resume detection, Conversational Awareness controls, and Quick Settings/notification listening mode switching while audio is actively playing.
```

- **Video walkthrough script (`<= 60 seconds`, upload as unlisted link for Play Console):**
  1. *(0–10s)* Start on Android Settings or AnDPoP onboarding; show granting **Nearby devices** (`BLUETOOTH_CONNECT`). Do **not** frame anything around location or proximity tracking.
  2. *(10–25s)* Put AirPods Pro in-ear / connect over Bluetooth; open **AnDPoP**, showing live Left/Right/Case battery percentages and Noise Cancellation status on the dashboard.
  3. *(25–40s)* Pull down the Android notification shade to visibly show the single ongoing `AnDPoP` connection notification (`Connected · Noise Cancellation · Left ...% · Right ...%`) and switch listening modes using the notification action button and the Quick Settings tile while the app is in the background.
  4. *(40–55s)* Open **Device settings -> Disconnect** (confirm dialog) or place AirPods back in the closed case, visibly showing the connection notification returning to the silent idle status or disappearing when stopped.

---

## 7. News, Government & Financial Features

| Console Form | Answer |
| --- | --- |
| News apps | **No**, AnDPoP is not a news app. |
| Government apps | **No**, AnDPoP is not affiliated with any government entity. |
| Financial features | **No**, AnDPoP does not provide any financial features. |

---

## 8. Health Apps Declaration (Medical & Hearing-Aid Analysis)

- **Policy reference:** [Health Content and Services policy & Health apps declaration](https://support.google.com/googleplay/android-developer/answer/14738291).
- **What Google Play requires:** Any app offering clinical/diagnostic health tools or functioning as a regulated medical device (including software-based **hearing aids** or audiogram-calibrated hearing enhancement) must declare **Medical device apps** in **Policy > App content > Health Apps**, comply with regional medical-device regulations (e.g., FDA in the U.S., EU MDR 2017/745), and be ready to furnish regulatory clearance, electronic Instructions for Use (eIFU), and UDI-DI on request.
- **How AnDPoP `play` complies:**
  - In `BuildConfig.PLAY_BUILD == true` (`play` flavor), **all Hearing Aid, Audiogram, and Transparency amplification UI & navigation routes are stripped at compile and navigation time** (`AirPodsSettingsScreen.kt` checks `!BuildConfig.PLAY_BUILD` and `AppNavGraph.kt` blocks `Screen.HearingAid`, `Screen.HearingAidAdjustments`, `Screen.TransparencySettings`, and `Screen.UpdateHearingTest`).
  - The remaining **Hearing protection** screen (`HearingProtectionScreen.kt`) is strictly a non-medical consumer headphone volume-reduction / loud-sound attenuation toggle forwarded to AirPods Pro 2 firmware, analogous to standard phone headphone loudness limiters, with zero audiogram input, diagnosis, treatment, or medical marketing.
- **Play Console selection:** Under **Policy > App content > Health Apps**, select **"My app does not provide any health features"** (do **not** add medical, hearing-aid, or health-benefit claims anywhere in your Play Store listing text or screenshots).

---

## 9. Technical Review Note: `BluetoothSocket` L2CAP Hidden-API Reflection (`libbluetooth_socket.so`)

- **What the code does (`app/src/main/cpp/bluetooth_socket.cpp` & `SystemAPIUtils.kt`):** Standard Android public `BluetoothDevice.createInsecureL2capChannel(psm)` targets BLE CoC channels, whereas Apple's Accessory Control Protocol (AACP) runs over **Bluetooth Classic BR/EDR L2CAP** on PSM `0x1001`. AnDPoP opens that BR/EDR L2CAP socket via native/reflective `BluetoothSocket` wiring when the user has already paired the earbuds and granted `BLUETOOTH_CONNECT`.
- **Android 13–17 runtime behavior:** Because non-SDK / hidden platform interfaces (`Greylist` / restricted reflection) can behave differently or fail across OEMs (Samsung One UI, Pixel, Android 14–17), `BluetoothConnectionManager` and `AirPodsService` catch connection/reflection failures cleanly without crashing the app, and show the Troubleshooting / Reconnect guidance if a handset blocks BR/EDR L2CAP creation.
- **Play Console check:** Google Play enforces `targetSdk >= 35/36` (AnDPoP targets **API 37**) and strict foreground service / runtime permission rules; while hidden-API reflection does not trigger an automatic binary rejection, **strictly validate BR/EDR L2CAP socket creation on your physical Galaxy S26 Ultra** ([DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md)) before shipping to production, because Play reviewers or users on locked-down Android builds will fail to talk to AirPods if the OS blocks the hidden socket path.
