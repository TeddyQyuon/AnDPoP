# Prompt for the next coding agent

Paste the following into the new account with access to `TeddyQyuon/AnDPoP`.

> Continue my AnDPoP v1.5 Android project. Clone https://github.com/TeddyQyuon/AnDPoP and start from the commit containing `docs/v1.5-ui/IMPLEMENTATION.md`, `docs/v1.5-ui/NEXT-AGENT-PROMPT.md` and `docs/v1.5-ui/AnDPoP-Charging-Prototype.html`. If I supplied a commit SHA, check out that exact snapshot on a working branch. Read the implementation guide and open the offline HTML before editing.
>
> Port the visual changes into the existing Kotlin/Jetpack Compose UI. Keep my teal dashboard layout, AirPods photos, all original settings, their order, model-specific controls, real Bluetooth behavior, account flows, per-user preferences and attribution. Use the prototype for appearance and the native app for behavior. Do not rebuild the app as a WebView or use Python.
>
> Draw the horizontal green battery with the dark lightning bolt under each component that is currently charging. Keep the percentage visible. Handle right-only charging, case-only charging, stale and missing readings accurately in the dashboard and details. Keep one green check on the active listening mode, fix Transparency wrapping and support Glacier, Iris and Guava in both appearances. Use Glacier as the dark default and Iris as the light default.
>
> Keep the full Settings page accessible from the top-right sliders. Below Sound, preserve AirPods controls, separate Left bud and Right bud press-and-hold rows, and About with firmware and More device controls. Preserve the real advanced and accessibility destinations even where the HTML only demonstrates part of a screen.
>
> Build and test the FOSS Android app using the checked-in Gradle wrapper. Run available instrumented checks, verify the new charge states and all six appearances, and distinguish emulator checks from physical AirPods checks. Commit and push the implementation to my repository, and give me the commit link, testing APK, AAB and a short verification report. Keep v1.5 as the baseline. Leave Play publication for a separate request.
