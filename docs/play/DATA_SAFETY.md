# Google Play Data Safety Form Mapping (`app.andpop.android`)

This document maps AnDPoP's verified code behavior (Play flavor, `PLAY_BUILD = true`) directly to the Google Play Console **Data safety** questionnaire.

---

## 1. Overview Questions

| Question | Answer | Evidence / Notes |
| --- | --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** | AnDPoP has an optional account feature (`AccountViewModel` / `SupabaseAccountClient`) that collects the user's **Email address** and internal user ID when the user chooses to sign up or log in. Guest users trigger zero network data collection. |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | `SupabaseAccountClient.kt` connects exclusively via `https://` (`HttpsURLConnection`, redirects disabled) and `res/xml/network_security_config.xml` sets `cleartextTrafficPermitted="false"`. |
| Do you provide a way for users to request that their data is deleted? | **Yes** (both in-app and via web link) | In-app: **Account → Delete account** calls Supabase RPC `delete_my_andpop_account` and wipes the local profile. Web: `https://teddyqyuon.github.io/AnDPoP/delete-account.html` (`docs/delete-account.html`). |

---

## 2. Data Types Collected

Only **Account info** applies in the `play` build:

| Category | Data Type | Collected? | Shared? | Processed Ephemerally? | Required or Optional? | Purpose |
| --- | --- | --- | --- | --- | --- | --- |
| **Personal info** | **Email address** | **Yes** | **No** | No (stored while account exists) | **Optional** (Guest mode works fully without an account) | **Account management** (sign-in, email verification, password reset) |
| **Personal info** | **User IDs** | **Yes** | **No** | No | **Optional** (Supabase Auth UUID created only for signed-in accounts) | **Account management** (scoping local on-device profiles per account) |

### Data Types Explicitly NOT Collected / NOT Transmitted
- **Location:** **No** (`BLUETOOTH_SCAN` uses `android:usesPermissionFlags="neverForLocation"`).
- **Contacts / Messages / Photos & Videos / Audio Files:** **No**. (Diagnostic log recording in `TroubleshootingScreen.kt` writes own-PID Bluetooth logs only to local app files with masked MAC addresses and is never uploaded automatically.)
- **App activity / Analytics / Crashlytics / Device or other IDs for ads:** **No** (zero analytics or ad SDKs).
- **Purchase history / Financial info:** **No** (all billing code and dependencies removed).
- **Health & Fitness info:** **No** in the `play` build (`HearingAidScreen` and audiogram test storage are omitted from the Play flavor, and no Bluetooth audio/battery telemetry ever leaves the device).

---

## 3. Data Handling & Local Storage Summary

- **On-device Bluetooth & preference data (`settings.xml`, `packet_logs.xml`, `filesDir/logs/`):** Never transmitted off the device; excluded from cloud backup (`backup_rules.xml`) and device-to-device extraction (`data_extraction_rules.xml`).
- **On-device account session credentials (`noBackupFilesDir/andpop-account-v1`):** Encrypted with `AndroidKeyStore` (`AES/GCM/NoPadding`), automatically excluded from cloud backups.
- **Release logs:** ProGuard strips `Log.v`, `Log.d`, and `Log.i` in `playRelease` builds.
