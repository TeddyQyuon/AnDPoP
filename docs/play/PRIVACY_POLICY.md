# Privacy Policy for AnDPoP (Draft)

> **Hosted static HTML:** Static GitHub Pages HTML versions of this Privacy Policy and the Web Account Deletion page are ready in [`docs/privacy.html`](../privacy.html) and [`docs/delete-account.html`](../delete-account.html) and wired in `PrivacyLinks.kt` (`https://teddyqyuon.github.io/AndPoP_V_1.6.0/privacy.html` and `.../delete-account.html`). Enable **Settings -> Pages -> Deploy from a branch -> `main` / `/docs`** on GitHub so those HTTPS URLs serve live.
>
> **[NEEDS LEGAL REVIEW]** Have a legal advisor review jurisdiction-specific disclosures (GDPR, CCPA/CPRA, etc.) prior to commercial or broad public distribution.

**Effective date:** 2026-11-25  
**App name:** AnDPoP (`app.andpop.android`)  
**Open-source license:** GNU General Public License v3.0 or later (GPL-3.0-or-later)

---

## 1. Overview

AnDPoP is a companion app for viewing AirPods battery status, switching listening modes, and configuring device settings on Android. You can use AnDPoP offline as a guest without creating an account.

AnDPoP contains **no advertisements**, **no third-party analytics SDKs**, **no crash telemetry uploads**, and **no in-app purchases**.

---

## 2. Information Stored Locally on Your Device

To provide Bluetooth companion functionality, AnDPoP stores the following data strictly on your device inside app-private storage (`shared_prefs/settings.xml`, `shared_prefs/packet_logs.xml`, and `filesDir/logs/`):

- **Device & Bluetooth metadata:** Connected AirPods device name, model number, firmware strings, serial numbers, Bluetooth MAC address, and Bluetooth proximity/encryption keys (`IRK`, `ENC_KEY`) needed for local BLE battery updates.
- **Preferences:** Listening mode preferences, stem press actions, Accessibility settings, equalizer tuning, theme/appearance choices, and per-profile snapshots.
- **Optional diagnostic logs:** If you explicitly start a recording in **Settings → Troubleshooting**, AnDPoP captures its own process Bluetooth logs to `filesDir/logs/` with Bluetooth addresses truncated to their last two bytes. Logs stay on your phone until you choose to share or delete them.

AnDPoP explicitly excludes `settings.xml`, `packet_logs.xml`, and `logs/` from Android cloud backup and device-to-device transfer (`backup_rules.xml` and `data_extraction_rules.xml`).

---

## 3. Optional Online Account

Signing in is completely optional. If you choose to create or sign into an AnDPoP account (using Email/Password or optional configured OAuth providers through the configured Supabase Auth service):

- **Data collected:** Your email address, authentication tokens, and internal user identifier.
- **Purpose:** Account authentication, password recovery emails, and maintaining separate local profiles/remembered device preferences on your device. (Device settings and battery readings stay on your phone and are not synced to the cloud.)
- **Encryption & storage on device:** Session tokens, user ID, and email are encrypted at rest with **Android Keystore (`AES/GCM/NoPadding`)** inside `context.noBackupFilesDir/andpop-account-v1`, which is excluded from Android backups.
- **Encryption in transit:** All communication with the authentication service uses **HTTPS (TLS)** exclusively (`cleartextTrafficPermitted="false"`).
- **Third-party sharing:** AnDPoP does not sell or share your email address or personal data with advertisers, data brokers, or unrelated third parties. **[NEEDS LEGAL REVIEW]** *Sub-processor note: Authentication requests are processed by the configured Supabase project backend (default project `btfbwwnbkarlpcqmkzzb.supabase.co`).*

---

## 4. Permissions Explained

- **Nearby devices (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` with `neverForLocation`):** Required to communicate with your AirPods over Bluetooth and read battery/listening-mode state. Never used to infer physical location.
- **Notifications (`POST_NOTIFICATIONS`):** Optional; displays active AirPods battery and quick mode controls in the notification shade.
- **Phone (`READ_PHONE_STATE`, `ANSWER_PHONE_CALLS`):** Optional; only used if enabled to support call audio handover and nod/shake call controls.
- **Display over other apps (`SYSTEM_ALERT_WINDOW`):** Optional; used only if you enable the connection popup or Dynamic Island overlay.

---

## 5. Account & Data Deletion

- **In-app deletion:** Open **Account → Delete account** while signed in. Confirming permanently deletes your online account via the `delete_my_andpop_account` RPC function and erases the associated local profile on your phone (returning the app to Guest mode).
- **Local data deletion:** Clear app data or uninstall AnDPoP in Android Settings to remove all locally stored device settings and logs, or delete individual recordings in **Troubleshooting**.
- **Web account deletion request (draft HTML for hosting at `ACCOUNT_DELETION_URL`):**

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>AnDPoP — Delete Account</title>
</head>
<body>
  <main style="max-width: 640px; margin: 2rem auto; font-family: system-ui, sans-serif; line-height: 1.6;">
    <h1>Delete Your AnDPoP Account</h1>
    <p>AnDPoP accounts store only your email address and authentication record. Device settings stay on your phone and are not synced to the cloud.</p>
    <h2>Option 1: Immediate In-App Deletion</h2>
    <ol>
      <li>Open <strong>AnDPoP</strong> and tap the <strong>Account</strong> icon.</li>
      <li>Select <strong>Delete account</strong> and confirm. Your online authentication record and on-device profile are deleted immediately.</li>
    </ol>
    <h2>Option 2: Request Deletion Online</h2>
    <p>If you no longer have access to the app, submit a deletion request to the developer support contact below with the email address registered to your AnDPoP account. Requests are processed within 30 days, and all authentication records associated with the email address are permanently erased.</p>
    <p><strong>Support contact:</strong> [TODO: INSERT SUPPORT EMAIL]</p>
  </main>
</body>
</html>
```

---

## 6. Children's Privacy

AnDPoP is not directed to children under 13 and does not knowingly collect personal information from children. **[NEEDS LEGAL REVIEW]**

---

## 7. Contact Us

For privacy questions or data deletion requests, contact: **[TODO: INSERT SUPPORT EMAIL / REPOSITORY CONTACT URL]**  
AnDPoP is not affiliated with Apple Inc. AirPods is a trademark of Apple Inc., registered in the U.S. and other countries.
