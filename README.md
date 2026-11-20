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

## Accounts and guest profiles

Choose **Continue as guest** to use AnDPoP offline. Open **Account** on the dashboard to see the current profile and save a connected pair of AirPods. The app keeps a separate set of supported preferences and a remembered device for each profile.

On a fresh install, choose **Set up online sign-in** to connect a Supabase project before using the account forms. See the [account setup guide](docs/ACCOUNT_SETUP.md) for email, Google, Apple, password recovery, and deletion. See the [Android release guide](docs/ANDROID_RELEASE.md) for signed bundles, Play Console internal testing, and device checks.

## Credits and license

Based on [LibrePods v1.0.4](https://github.com/librepods-org/librepods/tree/v1.0.4), commit `89356c08cea3db0b6b54792d9329d09a87cc85fa`. Original Android and native code copyright © 2025 LibrePods contributors. AnDPoP changes are available under GPL version 3 or later. See the included LICENSE. AirPods, AirPods Pro, and AirPods Max are Apple trademarks.

The AACP battery record layout is documented by [apple-wireshark](https://github.com/pabloaul/apple-wireshark). Apple publishes the [AirPods model numbers](https://support.apple.com/en-sg/109525).

AirPods Pro 2 artwork is cropped and color-adjusted from [AirPods Pro (2nd generation).jpg by Hajoon0102](https://commons.wikimedia.org/wiki/File:AirPods_Pro_(2nd_generation).jpg). These three WebP cutouts are licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
AirPods Max artwork is a crop of the [first-generation AirPods Max photo](https://commons.wikimedia.org/wiki/File:Apple_airpods_max_1.jpg) by [Arne Müseler / arne-mueseler.com](https://www.arne-mueseler.com/), licensed under [CC BY-SA 3.0 DE](https://creativecommons.org/licenses/by-sa/3.0/de/deed.en). It represents the Max family in the battery views.

