# AnDPoP account setup

AnDPoP connects to its Supabase project automatically on a fresh install. Choose **Create an account** to test email sign-up with a project team email, or **Continue as guest** to use AirPods battery and controls. The bundled project uses Supabase’s default email service for personal testing. Google and Apple are hidden for the bundled project until those providers are configured and enabled in the app. Existing installations keep any project they previously configured.

## Connect a different project

1. Create or select a Supabase project dedicated to AnDPoP testing. Copy its HTTPS project URL, such as `https://your-project.supabase.co`, and its publishable key from the project’s **Connect** dialog. AnDPoP also accepts a legacy `anon` key. Keep secret keys, service-role keys, and provider credentials out of the app and repository. [Supabase API keys](https://supabase.com/docs/guides/api/api-keys)
2. In **Authentication → URL Configuration**, add `app.andpop.android://auth/callback/*` to **Redirect URLs**. AnDPoP adds a different single-use identifier after `/callback/` for each request. OAuth and email links use this same allow-list entry. [Supabase redirect URLs](https://supabase.com/docs/guides/auth/redirect-urls)
3. Enable **Email** and allow new sign-ups in Supabase Auth. Keep email confirmation enabled. Use the default confirmation and recovery templates, or preserve their generated confirmation URLs if you customize them. Configure custom SMTP to send messages to testers outside the Supabase project team. [Supabase SMTP setup](https://supabase.com/docs/guides/auth/auth-smtp)
4. Open **Account → Account service settings** while signed out in AnDPoP. Enter the project URL and publishable key, then choose **Save connection**. The connection is saved on that installation. Test a sign-in to confirm it reaches the intended project.

## Enable Google and Apple

| Provider | Configuration |
| --- | --- |
| Google | Create a **Web application** OAuth client in Google Cloud. Register the callback URL shown on the Google provider page in Supabase, typically `https://your-project.supabase.co/auth/v1/callback`. Enter the client ID and secret in Supabase and enable the provider. If the OAuth consent screen is in testing, add the Google accounts you will test with. [Supabase Google setup](https://supabase.com/docs/guides/auth/social-sign-in/auth-google) |
| Apple | Configure Sign in with Apple in Apple Developer with a Services ID and the Supabase provider callback as the return URL. Enter the Services ID and generated OAuth secret in Supabase and enable the provider. Renew the Apple OAuth secret before its six-month expiry. [Supabase Apple setup](https://supabase.com/docs/guides/auth/social-sign-in/auth-apple) |

Google and Apple sign-in open in the Android browser and return to AnDPoP. The Supabase callback registered with the providers is an HTTPS URL. The Android return URL belongs in Supabase’s redirect allow list.

## Use and delete profiles

- For password recovery, choose **Forgot password?** from **Log in** or **Change password** from **Your account**. Open the reset link on the phone that asked for it. After saving the new password, sign in again.
- To save a device, connect your AirPods and choose **Account → Remember connected AirPods**. The profile stores the device name and model and a separate set of supported preferences such as Appearance and popup behavior. Android manages pairing. AnDPoP stores this profile data on the phone without account-based cloud sync.
- **Sign out to guest** switches to the guest profile. Signing back into the same account on that installation restores its saved device and supported preferences. Session credentials are encrypted using Android Keystore and kept outside Android backups.
- For **Delete account** to work, run [delete-account.sql](../account-setup/delete-account.sql) in the intended AnDPoP project. The function deletes the signed-in account. On success, the app removes that account’s saved profile from the current installation and switches to guest. Use a disposable test account to verify deletion.

## Troubleshoot sign-in

| Symptom | Check |
| --- | --- |
| Only the guest profile is available | Open **Account service settings** and check the saved connection. |
| A provider opens an error page | Enable the provider in Supabase and check its credentials, exact HTTPS callback URL, and tester access. |
| No confirmation or reset email arrives | Check spam, the Email provider settings, and the project’s SMTP delivery logs. |
| The browser or email leaves you outside AnDPoP | Check the Android callback entry in Supabase and open a fresh link on the same phone that started the request. |
| Delete account reports that it is unavailable | Check that the SQL function was installed in the connected project and that the test user is signed in. |
