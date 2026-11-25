# AnDPoP 2.0 Physical Device & Accessibility Test Plan

**Target hardware:** Samsung Galaxy S26 Ultra + Apple AirPods Pro 2 (USB-C) + Apple AirPods Max  
**APK / AAB under test:** Built from `feature/play-phase-3-quality` / `playRelease` (plus `fossRelease` for Xposed-specific checks)

> Record the Android version / security patch level, One UI version, AnDPoP `versionName` + `versionCode`, and each AirPods firmware string next to your results before promoting from Closed Testing to Production.

---

## 1. Fresh Install, Permissions & Onboarding

| # | Case | Steps | Expected Result | Pass / Notes |
| --- | --- | --- | --- | --- |
| 1 | Cold start & welcome | Clear app data; launch AnDPoP | Native splash transitions smoothly; **one** welcome screen appears with **Get started** and **I already have an account** | |
| 2 | Privacy summary | Tap **Get started** | Shows "Your data stays on your phone" with optional account note; tapping **Continue** advances to Permissions | |
| 3 | Per-permission Allow buttons | On Permissions page, check initial state then tap **Allow** next to **Nearby devices** | **Continue** stays disabled until **Nearby devices** is granted. Granting Nearby devices unlocks **Continue**; Notifications / Phone / Display over other apps remain optional | |
| 4 | Overlay permission resume check | Tap **Allow** on **Display over other apps**, toggle permission ON in Android Settings, press Back | Tile flips immediately to **Allowed** on resume without restarting onboarding | |

---

## 2. AirPods Pro 2 (USB-C) Core Hardware Checks

| # | Case | Steps | Expected Result | Pass / Notes |
| --- | --- | --- | --- | --- |
| 5 | Component battery & charging bolt | Connect AirPods Pro 2 via Bluetooth; place Right bud in charging case while wearing Left bud | Left, Right, and Case cells update; charging green fill + bolt appears **only** on Right bud (`activelyCharging = level != null && charging && !lastSeen`) | |
| 6 | Live case broadcast (lid open) | With one bud in the case, open the case lid near the S26 Ultra and tap **Refresh** | Case percentage updates live (`~` removed when direct AACP/BLE fresh, shown if estimated); closing lid preserves last-seen reading cleanly until expired | |
| 7 | Battery details bottom sheet | Tap **Battery details** on hero card | Blurred scrim + tonal sheet opens showing progress bars, charging labels, note explaining `~` & last-seen; tap `X` to dismiss | |
| 8 | Listening mode switching | Tap Off → Transparency → Adaptive → Noise Cancellation on dashboard control | Physical AirPods chime and switch modes reliably; green check and sliding indicator stay synced if changed by stem squeeze | |
| 9 | Press and hold (stem tiles & minimum rule) | Tap **Left bud** tile on dashboard; try unchecking until only 2 modes remain checked | Unchecking a 3rd mode blocks with checkbox shake + error helper ("Keep at least two modes selected"); changes persist on AirPods | |
| 10 | Conversation Awareness | Toggle Conversation Awareness ON in dashboard Sound card and speak | AirPods lower media volume / increase transparency while speaking, then restore smoothly afterward | |
| 11 | In-ear detection & Rename | Remove one bud; rename AirPods in Name screen (`Save`, clear button, reset button) | Media pauses/resumes; Bluetooth alias & hero title update after Save | |
| 12 | Head gestures (nod / shake) | Open **All device settings → Calls → Head gestures**; tap **Try it**; also test incoming call | **Try it** clearly announces Yes/No or timeout; incoming call accepts on nod / declines on shake (when Phone permission granted) | |
| 13 | Reconnect from disconnected card | Put AirPods away / disconnect L2CAP, reopen app, tap **Reconnect** | Pulsing status pill transitions cleanly back to Connected | |

---

## 3. AirPods Max Hardware Checks

| # | Case | Steps | Expected Result | Pass / Notes |
| --- | --- | --- | --- | --- |
| 14 | Single headphones battery card | Connect AirPods Max and open dashboard | Shows single wide AirPods Max photo well + `Headphones` percentage (`no case card`, no crash on battery sheet) | |
| 15 | AirPods Max mode capabilities | Check dashboard Listening mode control | Displays supported modes (`Off`, `Transparency`, `Noise Cancellation`), hiding unsupported options cleanly | |

---

## 4. System Surfaces (Notifications, QS Tile, Widgets, Overlays)

| # | Case | Steps | Expected Result | Pass / Notes |
| --- | --- | --- | --- | --- |
| 16 | Single informative notification (Decision 6) | Inspect notification shade while connected vs disconnected | **While disconnected:** only silent minimized `Background service` notification (`AnDPoP is running · Tap to hide this notification`). **While connected:** single `AirPods battery and controls` notification (ID 1) showing device name, mode, L/R/Case readings, **Switch mode** and **Open** actions; zero "aacpSocket?" errors | |
| 17 | Notification Switch mode & Reconnect | Tap **Switch mode** action in notification shade | Listening mode cycles immediately without opening app | |
| 18 | Quick Settings tile | Pull down QS panel, add AnDPoP tile, tap once (cycle mode & dialog mode) | Tile shows monochrome AirPods glyph, active mode label + device name; toggles mode cleanly | |
| 19 | Home screen Glance widgets (Decision 7) | Pin both **Battery** and **Listening mode** widgets to home screen; resize | Widgets use active app color palette, render circular battery rings + charging bolt badge, and switch listening modes directly | |
| 20 | Connection popup & Dynamic Island | Open case lid with overlay permission granted (and trigger audio handover if multi-device) | Bottom connection sheet shows product photos + battery bars + **Open AnDPoP** button; top Dynamic Island capsule shows device + status + battery ring (or **Undo**), with zero "Linux" text | |

---

## 5. Accounts, Themes, All Device Settings & Offline Resilience

| # | Case | Steps | Expected Result | Pass / Notes |
| --- | --- | --- | --- | --- |
| 21 | All 7 color themes & System/Light/Dark | Switch Wallpaper, Glacier, Iris, Guava, Sage, Apricot, Graphite across Light/Dark/System | Dashboard, detail screens, widgets, and popups update consistently without hardcoded mismatches | |
| 22 | Offline Guest + Account round-trip | Toggle Airplane mode / Wi-Fi off in Guest mode; sign into Supabase account with Wi-Fi on, remember AirPods, switch to Guest, sign back in, and delete test account | Guest dashboard/Bluetooth works 100% offline; account switch restores per-profile theme & remembered device; Delete account clears online auth + local profile | |
| 23 | Play build check (`playRelease`) | Install signed/test `playRelease` build on S26 Ultra | Developer options stays hidden after 7 taps on App version; Hearing Aid / audiogram screens not exposed; `"Try demo mode"` works when unpaired | |

---

## 6. Accessibility Scanner & TalkBack Manual Checklist

Run **Google Accessibility Scanner** and **TalkBack** across: Onboarding (Welcome, Privacy, Permissions), Dashboard (Pro 2 + Max + Pairing Guide), Battery Sheet, All Device Settings, Press and Hold, Equalizer, Troubleshooting, Account, and Settings.

- [ ] **Touch targets:** Zero interactive items flagged below `48 dp × 48 dp` (mode pills, checkboxes, radio rows, sliders, refresh/close/avatar/settings icons).
- [ ] **Content labels:** Zero unlabelled clickable icons; every battery cell announces a single merged TalkBack node (e.g., `"Right bud, 48%, Charging"`); no icon on Permissions announces `"bluetooth"` for Notifications/Phone/Overlay.
- [ ] **Live regions & state cues:** Listening mode selection announces `Selected` state + non-color check icon; locked 2-mode checkbox rejection shakes and updates error text; battery refresh status uses polite live region.
- [ ] **200% Font Scale & Narrow Width (`320–360 dp`):** Enable **Settings → Display → Font size → 200%** on S26 Ultra — verify Listening mode control wraps cleanly to `2×2` or single column, battery cells stack into rows, row values drop below titles (`>= 1.6x`), and zero buttons clip or overlap.
- [ ] **Contrast:** Zero Accessibility Scanner text/icon contrast warnings (< 4.5:1) across both Glacier Dark and Sage/Apricot Light themes.
