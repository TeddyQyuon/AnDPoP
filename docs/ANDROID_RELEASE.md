# AnDPoP Android release

| Build setting | Value in 1.4.0 |
| --- | --- |
| Application ID | `app.andpop.android` |
| Version name and code | `1.4.0` and `6` |
| Minimum Android API | 33 |
| Compile and target API | 37 |
| Toolchain | JDK 21, NDK `30.0.14904198`, CMake `3.22.1` |

## Build an APK for local testing

Install the SDK and toolchain above in Android Studio. From the repository root, run the following command.

```sh
./gradlew :app:assembleFossDebug :app:lintVitalFossRelease
```

On Windows, use `gradlew.bat` in place of `./gradlew`. The APK is under `app/build/outputs/apk/foss/debug/`. An APK update on the phone needs the same application ID and signing certificate as the installed APK.

## Build and upload a signed AAB

1. Use the upload key registered for this app in Play Console. If this is the first upload, follow Play App Signing setup for the app. [Android app signing](https://developer.android.com/studio/publish/app-signing)
2. Set `RELEASE_STORE_FILE` to the absolute keystore path in your local `local.properties`. Set `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` there as well. Keep the keystore and credentials outside Git. Back up the upload key securely.
3. Check that `applicationId` in [app/build.gradle.kts](../app/build.gradle.kts) matches the Play Console app and that `versionCode` is higher than the codes already uploaded for it. Then build the bundle.

   ```sh
   ./gradlew :app:bundleFossRelease
   jarsigner -verify app/build/outputs/bundle/fossRelease/app-foss-release.aab
   ```

4. In Play Console, open **Testing → Internal testing**, add the testers, then choose **Create new release**. On **Create internal testing release**, use **Upload** to select `app-foss-release.aab`. Add release notes, choose **Next**, and resolve validation errors before starting the rollout. Share the test opt-in link with testers so they can install through Google Play. [Prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348) · [Set up an internal test](https://support.google.com/googleplay/android-developer/answer/9845334)

## Check the build on a phone

| Area | Expected result |
| --- | --- |
| Guest onboarding | A fresh install can continue as guest, reach the dashboard, and use it with internet access disabled. Nearby devices access is handled by Android. |
| Battery and controls | On the Galaxy S26 Ultra, check AirPods Pro 2 left and right readings and refresh the case with its lid open and a bud inside. Check the single AirPods Max reading, supported listening modes, and reconnect behavior. |
| Guest and account preferences | Set different appearances and saved devices for guest and an account. Switch between them and relaunch the app. Each restores its own supported preferences and remembered device. |
| Email sign-up and recovery | Use a fresh address. Confirm sign-up on the same phone, sign in, then request a reset and verify that the new password works. |
| Google and Apple | Run each provider flow with a test account. Check that the approved flow returns to AnDPoP and the cancelled flow leaves the guest profile available. |
| Account deletion | Delete a disposable account, confirm it is gone from Supabase Auth, and check that its saved local profile was cleared. Guest settings should still be available. |

Use the [account setup guide](ACCOUNT_SETUP.md) before running online checks. Record the APK or AAB version and the phone and AirPods firmware versions with the results.

## Before distributing an account-enabled build publicly

Publish the AnDPoP privacy policy at a publicly accessible URL and include it in the app and Play Console. Complete Data safety against the actual app and deployed account service. With account creation enabled, use the supplied SQL function for in-app deletion and provide an external deletion-request page linked in Play Console. [Google Play user data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
