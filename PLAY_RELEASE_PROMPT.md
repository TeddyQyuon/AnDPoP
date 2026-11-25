# Mission: finish AnDPoP 2.0 and make it ready to publish on Google Play

You are a senior Android engineer finishing this project. The goal is a Play-ready release:
- every feature in the approved 2.0 design is implemented
- the app complies with current Google Play policy
- there's a signed release bundle (AAB)
- all store listing material is drafted
- I get a clear list of the steps only I can do

Work carefully. Correctness matters more than speed.

## 1. Read these before you do anything
1. `AGENTS.md`: project facts, file map, rules, build commands, remaining work.
2. `docs/v2-ui/README.md`: the approved design, decisions 1–10, the implementation status table and the Play readiness checklist. This is the source of truth for UI.
3. `docs/ANDROID_RELEASE.md` and `docs/ACCOUNT_SETUP.md`.
4. `docs/v2-ui/AnDPoP-v2-Prototype.html` is the clickable prototype. It's large, so don't load it into context; ask me for screenshots if you need visuals. The PNGs in `docs/v2-ui/screens/` and `docs/v2-ui/app/` show the target and the current state.
5. If `docs/play/PROGRESS.md` exists, read it and resume from there.

## 2. Ground rules
- **Approved design:** I approved all ten decisions in `docs/v2-ui/README.md`. Implement them as described unless something here overrides them.
- **Keep native behavior:**
  - Bluetooth/AACP logic, account flows and preference keys keep working.
  - No WebView rewrite.
  - Keep GPL-3.0 licensing and the LibrePods and photo credits.
- **UI building blocks:** build UI from the 2.0 components in `presentation/components/AndpopUi.kt` and colors from `LocalAndpopPalette.current`. No hard-coded hex colors in screens. All user-visible text goes in string resources.
- **Accessibility:** 48 dp touch targets, TalkBack labels, layouts that work at 200% font size, and WCAG AA contrast.
- **Tests:** keep every existing instrumented test passing. Add tests for new behavior. If a test must change, say why.
- **Git:**
  - Use one feature branch per phase, for example `feature/play-phase-1-ui`.
  - Never work directly on `main`.
  - Keep commits small, with messages like `feat:`, `fix:` or `docs:`.
- **Build after every meaningful change:** `.\gradlew.bat :app:assembleFossDebug`. Once Phase 2 adds the play flavor, also build `:app:assemblePlayDebug`. Report errors with file and line.
- **Ask me first before:**
  - deleting files
  - changing Gradle, AGP, Kotlin, SDK or dependency versions
  - touching signing config, `local.properties` or keystores
  - adding GitHub secrets or workflows that use them
  - pushing to GitHub
  - removing a user-facing feature
  - any Play policy judgment call
- **Never:**
  - print, commit or invent secrets, passwords or keystores
  - invent URLs, legal claims or policy facts
  - claim something works without having built or tested it
- **Don't guess policy:** if a Play requirement matters, check it against developer.android.com or support.google.com and cite the page in your report.
- **Kiro sandbox:** if Gradle can't download dependencies or write to `%USERPROFILE%\.gradle`, stop and tell me (I can run `/sandbox off`). Don't work around the sandbox yourself.
- **Big items:** if you're running in Kiro, create a spec with `/spec new <name>` for multi-screen work (Phase 1 items 1, 3 and 7) and show me the requirements before you implement.

## 3. Progress file (required)
Create and maintain `docs/play/PROGRESS.md`:
- the phase checklist with status
- decisions I made, with dates
- open questions
- the last build and test results
- the exact next step

Update it after every completed item. Commit it with the work.

## 4. Phases

### Phase 0: baseline and audit (change no app code)
1. **Environment:** report `git status`, the branch, `java -version` (expect 21), whether `sdk.dir` or `ANDROID_HOME` is set (don't print `local.properties`), and `.\gradlew.bat --version`.
2. **Builds and lint:** build `:app:assembleFossDebug` and `:app:assembleFossRelease` (R8 on), and run `:app:lintFossDebug`. Summarize the lint errors and the important warnings.
3. **Tests:** if `adb devices` lists a device, run `:app:connectedFossDebugAndroidTest`.
4. **16 KB page-size check:** every `.so` in the APK must have LOAD segments aligned to 2**14 or more. Use `%ANDROID_HOME%\ndk\29.0.14206865\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-readelf.exe -l <lib.so>`, and `zipalign -c -P 16 -v 4 <apk>` from build-tools 35 or newer. Report each library.
5. **Inventory, written to `docs/play/AUDIT.md`:**
   - every manifest permission with its use and Play risk; in particular BLUETOOTH_PRIVILEGED, MODIFY_PHONE_STATE, LOCAL_MAC_ADDRESS, INTERACT_ACROSS_USERS, SYSTEM_ALERT_WINDOW, the phone permissions, RECEIVE_BOOT_COMPLETED and FOREGROUND_SERVICE_CONNECTED_DEVICE
   - exported components
   - Xposed/root-dependent code
   - billing code and dependencies
   - network endpoints (Supabase auth)
   - what data is stored and where, plus backup rules
   - the number of hard-coded user-visible strings
   - leftover LibrePods branding
6. **Plan:** propose a numbered plan for Phases 1–5 in `PROGRESS.md`, flag every decision you need from me, and wait for my approval.

### Phase 1: finish the 2.0 UI
Do these in order. Each one is done only when it builds, the tests pass, new tests cover it where practical, and the `ScreenshotTour` output matches the design.
1. **Account as a normal screen, with one welcome (decision 1).**
   - Today `MainActivity` swaps `NavigationRoot` for `AccountScreen`, and on first run the account screen appears before onboarding.
   - Make account a `Screen.Account` destination opened from the dashboard avatar, with predictive back.
   - Guest is the default first-run path. Add "I already have an account" on the welcome page.
   - Keep sign-in, sign-up, recovery, deletion, the remembered device and the per-profile preferences working.
   - Keep `AccountScreen(state, model, null)` working for `AppearanceRegressionTest`.
2. **Permissions page.**
   - Replace "Grant all" with an Allow button per permission, each with its reason.
   - Continue unlocks once Nearby devices is granted. Re-check the overlay permission on resume.
   - Fix the icons whose TalkBack labels are "bluetooth".
   - Move all text into resources.
3. **All device settings (decision 2).**
   - Regroup into Audio, Hearing health, Calls, Connection and charging, and About.
   - Remove the controls already on the dashboard, the FOSS premium banner and "Unlock advanced features".
   - Disconnect asks for confirmation.
   - Every destination stays reachable.
4. **Developer options (decision 4).**
   - Tapping App version seven times reveals them.
   - They contain the account service form (moved from the account screen), the compatibility bypass, demo mode and "Act as an Apple device".
   - The demo mode gesture moves here too.
5. **Detail screens, as in the prototype:**
   - Name: explicit Save, a counter and a clear button.
   - Press and hold: show the "keep at least two modes" rule.
   - Firmware: tap a value to copy it.
   - Accessibility: one-tap segmented speed pickers.
   - Equalizer: Low, Mid and High sliders with a curve preview (decision 9).
   - Head gestures: a clear test result.
   - Troubleshooting: a three-step recorder with saved logs.
6. **Dashboard loading skeleton** while the service binds.
7. **System surfaces:**
   - one informative notification (battery, mode, actions) plus a silent, minimized service notification (decision 6)
   - widgets that match the design, using Jetpack Glance if I approve the new dependency (decision 7)
   - popup and Dynamic Island layouts that match the prototype
   - a QS tile that shows the mode and device
8. **Font (decision 8):** bundle Roboto Flex. Ask me to provide the font file, or tell me exactly where to download the official one.
9. **Large screens:** a two-pane layout from 840 dp (dashboard plus detail), or a documented decision to defer it.
10. **Implementation status:** update the table in `docs/v2-ui/README.md`.

### Phase 2: Play build and policy compliance
1. **`play` product flavor.** Add it with `buildConfigField PLAY_BUILD = true`. Keep `foss`. Confirm the applicationId strategy with me.
2. **Play build contents.** It must not ship developer tools, the compatibility bypass, the account service form, Xposed/root features or root wording.
   - Propose how reviewers can still see the app without AirPods, for example a visible "Try a demo" entry. Write the reviewer instructions.
3. **Play manifest.**
   - Remove permissions the play flavor doesn't need, using a flavor manifest with `tools:node="remove"`.
   - Justify every permission you keep in `AUDIT.md`.
   - Check that the foreground service declares the `connectedDevice` type, and draft the Play Console foreground-service declaration text.
4. **Billing.** The app has no in-app purchases. Propose removing the purchase screen, the billing code and the billing dependency from the play build, and do it after I approve.
5. **Privacy.**
   - The in-app privacy summary must match the real data handling.
   - Add a link to the hosted privacy policy URL. I'll provide it; until then use a clearly marked TODO in one place.
   - Account deletion must work in-app, and you need to draft the web deletion page.
   - Draft `docs/play/PRIVACY_POLICY.md` for me to review and host. Mark anything that needs legal review.
6. **Data safety.** Draft `docs/play/DATA_SAFETY.md` from the audit: what's collected, why, sharing, encryption in transit and deletion.
7. **Trademark.**
   - App name "AnDPoP".
   - Mention "AirPods" only to describe compatibility.
   - Keep the "not affiliated with Apple" notice.
   - No Apple logos.
8. **Strings and localization.**
   - Move all remaining hard-coded user-visible strings to resources.
   - Propose a translation plan for the English-only AnDPoP strings (the 13 inherited locales).
9. **Requirements.** Confirm the target SDK is 36 or higher (it's 37), confirm 16 KB compliance, and check the backup rules: the account vault must never be backed up.
10. **Release hardening.**
    - Make the R8 release build work, and add keep rules only where needed (serialization, reflection, Xposed, AboutLibraries).
    - No cleartext traffic.
    - No debug logging of personal data in release.

### Phase 3: quality
1. **Tests and lint.** All instrumented tests pass on an emulator for both flavors. Add tests for:
   - account navigation
   - permission gating
   - developer options hidden in the play build
   - notification content where testable
   - theme migration

   Lint has zero errors; fix or justify the important warnings.
2. **Accessibility pass.** Check TalkBack order and labels, 200% font size, touch targets and contrast. Give me a manual checklist for Accessibility Scanner on a phone.
3. **Device check.** Smoke-test the R8 release build on an emulator: launch, onboarding, dashboard demo data, settings, themes and account screens. Fix any crashes.
4. **Physical-device test plan.** Write `docs/play/DEVICE_TEST_PLAN.md` for testing with real AirPods (Pro 2 USB-C and Max) on a Samsung phone. Cover battery for each component, charging, the case broadcast, mode switching, conversation awareness, press and hold, reconnect, popups, widgets, notifications, QS tile and head gestures.

### Phase 4: signing and release build
1. **Upload key.** Give me the exact `keytool` command to create an upload key. The keystore stays outside the repo. Explain the existing `local.properties` keys: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Explain Play App Signing.
2. **Bundle.** Build `:app:bundlePlayRelease`. Verify the bundle is signed and versioned. Propose a versionCode strategy.
3. **Optional CI.** A workflow that builds the play AAB using GitHub encrypted secrets. Ask me before creating it, and never commit key material.

### Phase 5: store listing material, in `docs/play/`
1. **`LISTING.md`:**
   - title (30 characters max)
   - short description (80 max)
   - full description (4000 max), including supported models and Android versions, privacy, open source and the Apple disclaimer
   - release notes
   - category, contact details placeholder, tags
2. **Graphics.**
   - 512×512 icon PNG exported from the adaptive icon.
   - 1024×500 feature graphic.
   - At least 4 phone screenshots at 1080×2340 or similar, from a `ScreenshotTour` variant on a 1080p emulator with demo data.
   - Optional tablet screenshots.

   Put the graphics in `docs/play/graphics/`.
3. **`PLAY_CONSOLE.md`:** step-by-step answers for:
   - content rating
   - target audience (not for children)
   - ads (none)
   - app access (reviewer instructions)
   - foreground service declaration and video instructions
   - Data safety
   - news app (no), government app (no), financial features (none)
   - health features (hearing aid features: research and state what Play requires; don't guess)

### Phase 6: final verification and handoff
Produce `docs/play/RELEASE_CHECKLIST.md`: every requirement marked pass, fail or needs-user, with evidence (command output or file). End with **"What you must do yourself"**, in order:
1. Create and verify the Play developer account.
2. Create the upload keystore.
3. Host the privacy policy and account-deletion pages.
4. Test on a real phone with AirPods.
5. Run the closed test with at least 12 testers for 14 days. This applies to personal accounts created after 13 Nov 2023; confirm it still applies.
6. Fill in the Play Console forms.
7. Upload the AAB.
8. Apply for production access.

## 5. Definition of done
- Every Phase 1 item is implemented, or explicitly deferred by me, and recorded in `PROGRESS.md`.
- `:app:bundlePlayRelease` builds; it's signed once I've provided the upload key.
- All instrumented tests pass, lint has no errors, and the 16 KB check passes.
- The play build contains no developer, root or Xposed features and no unjustified permissions.
- `docs/play/` contains AUDIT, PRIVACY_POLICY (draft), DATA_SAFETY, LISTING, graphics, PLAY_CONSOLE, DEVICE_TEST_PLAN, RELEASE_CHECKLIST and an up-to-date PROGRESS.
- `docs/v2-ui/README.md` shows the final implementation status.

## 6. How to report
After each item, reply with:
- what changed (files)
- build and test results
- screenshots, if the UI changed
- decisions you need from me
- the next step

Keep reports short and factual. Start now with Phase 0, and stop after the plan for my approval.
