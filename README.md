# AnDPoP for Android

AnDPoP is an independent GPL-licensed fork of the official LibrePods v1.0.4 Android FOSS release. The dashboard shows left, right, and case battery side by side, provides model-aware listening controls, and presents a single battery reading for AirPods Max. The app has no in-app purchases. Supported features depend on the AirPods model, firmware, and Android Bluetooth stack.

The device page groups battery readings, listening modes, AirPods controls, and firmware information. Battery details opens a frosted sheet. Settings → Appearance switches between System, Light, and Dark. The connection and battery-reading indicators reflect current Android service state.

## Try it on a Galaxy S26 Ultra

1. Install the AnDPoP APK and finish onboarding. Allow Nearby devices access when prompted.
2. Pair one set of AirPods in Android Bluetooth settings, then return to AnDPoP. Test one device at a time. Close LibrePods if it is already controlling the same AirPods.
3. For AirPods Pro 2, leave an earbud in the case, open the lid near your phone, and tap the refresh icon in the Battery panel. AnDPoP can use a verified proximity broadcast when the direct battery report omits the case. Estimated readings use a tilde (~). Last seen readings are stored for the current Bluetooth service session. For AirPods Max, wake and connect the headphones.
4. Test listening modes and check whether the selected mode updates. On Samsung, try One UI 9 / Android 17 for rootless operation. Features that use upstream Xposed integrations still require the appropriate setup.

## Build
The Kotlin Compose dashboard previews for light, dark, and AirPods Max are in `AndpopDashboard.kt`.

Open this directory in Android Studio with JDK 21 and install Android SDK Platform 37.0, NDK 30.0.14904198, and CMake 3.22.1. From this directory, run:

```sh
./gradlew :app:assembleFossDebug
```

On Windows, use `gradlew.bat :app:assembleFossDebug`. The APK is written to `app/build/outputs/apk/foss/debug/`. Release builds can use your own Android signing key.

## Credits and license

Based on [LibrePods v1.0.4](https://github.com/librepods-org/librepods/tree/v1.0.4), commit `89356c08cea3db0b6b54792d9329d09a87cc85fa`. Original Android and native code copyright © 2025 LibrePods contributors. AnDPoP changes are available under GPL version 3 or later. See the included LICENSE. AirPods, AirPods Pro, and AirPods Max are Apple trademarks.

The AACP battery record layout is documented by [apple-wireshark](https://github.com/pabloaul/apple-wireshark). Apple publishes the [AirPods model numbers](https://support.apple.com/en-sg/109525).

AirPods Pro 2 artwork is cropped and color-adjusted from [AirPods Pro (2nd generation).jpg by Hajoon0102](https://commons.wikimedia.org/wiki/File:AirPods_Pro_(2nd_generation).jpg). These three WebP cutouts are licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
AirPods Max artwork is a crop of the [first-generation AirPods Max photo](https://commons.wikimedia.org/wiki/File:Apple_airpods_max_1.jpg) by [Arne Müseler / arne-mueseler.com](https://www.arne-mueseler.com/), licensed under [CC BY-SA 3.0 DE](https://creativecommons.org/licenses/by-sa/3.0/de/deed.en). It represents the Max family in the battery views.

## Accounts and guest profiles

Open **Account** on the AirPods dashboard. Guests can use the app offline. Existing installations start in the guest profile with their current preferences. Profiles remember a selected connected AirPods name and model, plus local app preferences such as Appearance, conversation volume, and popup behavior. Use **Remember connected AirPods** on the profile page to save the current device. Android manages Bluetooth pairing, and the AirPods manage their own listening mode and on-device controls. Profile data is local to this phone.

### Enable online accounts

1. Create or select your AnDPoP [Supabase project](https://supabase.com/docs/guides/auth). Open **Account → Log in → Connect account service** and enter its HTTPS project URL and public publishable key. A legacy anon key is also accepted. Keep service-role keys and provider secrets on the server.
2. Enable Email in Authentication → Providers. Keep email confirmation enabled and configure [SMTP](https://supabase.com/docs/guides/auth/auth-smtp) for emails to testers outside your project team.
3. Add `app.andpop.android://auth/callback/*` to the [Auth redirect allow list](https://supabase.com/docs/guides/auth/redirect-urls). Each request generates a new single-use callback identifier and PKCE verifier. Open confirmation and recovery links on the phone that made the request. Use the default Supabase confirmation and recovery templates or preserve their confirmation URLs.
4. To enable the browser-based buttons, configure [Google](https://supabase.com/docs/guides/auth/social-login/auth-google) and [Apple](https://supabase.com/docs/guides/auth/social-sign-in/auth-apple) in Supabase Authentication. Register the Supabase provider callback URL with Google and Apple.
5. For in-app account deletion, run [delete-account.sql](account-setup/delete-account.sql) in your AnDPoP project. The function deletes only the account identified by the current access token. This app stores device profiles locally, and deletion clears that account’s profile from the current phone.

Session credentials stay in encrypted, excluded-from-backup storage. The app uses Android Keystore, system-browser OAuth, and a PKCE code exchange. A connected Supabase project is required for live account flows.
