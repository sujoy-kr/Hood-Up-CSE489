# Build, sign and install Hood Up

## Requirements

Use the checked-in Gradle wrapper, SDK platform 37, and the complete JetBrains JDK 25 selected by the project's daemon criteria. Android Studio's JBR is `/opt/android-studio/jbr` in this workspace. `local.properties` supplies the SDK path to Gradle. Keep private `.env` values local; both debug and release APKs embed the configured BuildConfig values and must be treated accordingly.

The current SDK Build Tools directory here is `/home/hellfire/Android/Sdk/build-tools/36.0.0`. For the commands below set the path to **your installed** Build Tools version:

```sh
export JAVA_HOME=/opt/android-studio/jbr
SDK_TOOLS=/home/hellfire/Android/Sdk/build-tools/36.0.0
```

`SDK_TOOLS` is a task-specific shell variable; it does not replace your SDK or home-directory configuration. On another machine, update these two example paths.

## Automated checks

From the project root:

```sh
./gradlew test
./gradlew assembleDebug assembleRelease lint
```

`test` aggregates the configured JVM test tasks. This project's current Android plugin exposes `testDebugUnitTest`; it does not expose a separate release JVM test task. The unit suite executes without Firebase credentials, an Android device or a Gemini request. The release APK is separately compiled/optimized by `assembleRelease`.

Reports:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/test-results/testDebugUnitTest/` (JUnit XML)
- `app/build/reports/lint-results-debug.html`
- `build/prompt14-checks.log` (the recorded verification command output)

Tests cover commission/Plus half-cent rounding and invalid inputs; all 36 status pairs and nulls; keyword phrases, duplicate-keyword scoring and unknown symptoms; valid/malformed provider JSON, hidden thought parts, fragmented visible response parts, alias mapping and unknown/wrongly typed IDs; slot durations, exact boundaries, occupancy locks, adjacency, closed days and overnight hours. Additional tests cover rating averages, reminder timing, privacy, browsing and validation.

## Debug APK: signed automatically

```sh
./gradlew assembleDebug
```

Output: **`app/build/outputs/apk/debug/app-debug.apk`**. Gradle signs it with the local Android debug certificate automatically. This is suitable for a course demo, not a production signing identity.

Verify it:

```sh
"$SDK_TOOLS/apksigner" verify --min-sdk-version 24 app/build/outputs/apk/debug/app-debug.apk
```

## Release variant

```sh
./gradlew assembleRelease
```

The project deliberately does not commit a release key or silently sign production releases with the debug key. Default output is **`app/build/outputs/apk/release/app-release-unsigned.apk`**. The release variant has optimization enabled and is not debuggable. The unsigned APK needs a signature before installation.

### Installable demo release using the existing debug certificate

A release variant may be signed with the debug certificate for direct demonstration. This keeps the same signing identity as the local debug APK while using release compilation/optimization. It is clearly named `app-release-demo.apk` and must not be treated as an owner-signed production release.

After building debug and release, verify alignment and sign a **copy**:

```sh
"$SDK_TOOLS/zipalign" -c -P 16 4 app/build/outputs/apk/release/app-release-unsigned.apk
"$SDK_TOOLS/apksigner" sign \
  --ks "$HOME/.android/debug.keystore" \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android --key-pass pass:android \
  --out app/build/outputs/apk/release/app-release-demo.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
"$SDK_TOOLS/apksigner" verify --min-sdk-version 24 \
  app/build/outputs/apk/release/app-release-demo.apk
"$SDK_TOOLS/zipalign" -c -P 16 4 \
  app/build/outputs/apk/release/app-release-demo.apk
```

The `android` passwords above are the standard public Android **debug** defaults, not private release credentials. If `ANDROID_USER_HOME` relocates your debug keystore, use that location instead. `./gradlew signingReport` can show the configured debug store and public certificate fingerprint. Do not change your normal debug key merely to follow these instructions.

This demo-signed release APK was produced and verified during Prompt 14. Running `clean` removes generated APKs, so repeat signing after a clean build. The unsigned original remains available.

### Owner-signed release in Android Studio

1. Choose **Build → Generate Signed App Bundle / APK → APK**, module `app`.
2. Select your own existing release keystore, or create one in a private location outside this repository. Choose your key alias and enter its passwords in the local signing dialog.
3. Select the `release` variant and an output directory; finish the build. Record the exact output shown by Studio (the chosen directory controls its location).
4. Preserve the release key securely for future updates. Do not add keystores, passwords or signing configuration containing credentials to version control. APK distribution and Play App Signing can use different signing certificates; configure Firebase/API restrictions for the certificate actually installed on the phone.

No owner-controlled release key was created or used during this task. The signing wizard is an alternative to the CLI below, not a required step for the already signed demo APK.

### Owner-signed release from the CLI

Supply your **own** private keystore path and alias. Do not paste real passwords into commands or documentation; apksigner can prompt locally for them when you run this later:

```sh
"$SDK_TOOLS/zipalign" -P 16 -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
"$SDK_TOOLS/apksigner" sign \
  --ks /private/path/hood-up-release.jks \
  --ks-key-alias hood-up \
  --out app/build/outputs/apk/release/app-release.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
"$SDK_TOOLS/apksigner" verify --min-sdk-version 24 \
  app/build/outputs/apk/release/app-release.apk
```

Alignment must occur **before** signing; changing the APK afterward invalidates its signature. See the official [command-line build guide](https://developer.android.com/build/building-cmdline) and [apksigner reference](https://developer.android.com/tools/apksigner).

## Install on a phone

### USB / adb

Enable Developer options and USB debugging on an Android 7.0+ phone, connect it and accept the computer's authorization prompt. Put platform-tools on PATH or use your SDK's full adb path.

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For the demo release instead:

```sh
adb install -r app/build/outputs/apk/release/app-release-demo.apk
adb shell am start -n com.example.carworkshopbookingapp/.ui.SplashActivity
```

Use `adb -s DEVICE_SERIAL install -r ...` if more than one device is connected. `-r` keeps data for an update signed with the same certificate. An owner-signed release cannot replace a debug-signed installation in place; either keep using the matching key or explicitly uninstall first after preserving required local data. Uninstalling removes chat, caches and the offline queue. Do not try to install `app-release-unsigned.apk`.

### File transfer

Copy a signed APK over USB/file transfer, open it in the phone's Files app and permit installation from that source when asked (Android 7 has a broader unknown-sources setting; Android 8+ uses per-source permission). Confirm **Install**, then open **Hood Up**. Disable that source's installation permission afterward if it is no longer needed. Transferring an APK does not transfer `.env`, Firebase rules or console configuration; credentials are already embedded in that APK's BuildConfig.

## API 24 verification and limits

| Check | Result / reason |
| --- | --- |
| Manifest of both variants | Merged manifest declares minSdk 24, targetSdk 37. The built release APK's badging also reports minSdk 24. |
| Android lint | Completed with no errors and no `NewApi`/`InlinedApi` findings. No API-check suppressions were added. |
| Notification channels / channel-aware builder | SDK >=26 checks; legacy builder exists for API 24–25. |
| Start foreground service / service type | SDK >=26 / >=29 branches; older platforms use the compatible started-Service path. |
| Notification runtime permission | Requested/checked only on SDK >=33. |
| SigningInfo / certificate lookup | SDK >=28 branch and legacy signature lookup fallback. |
| Disabled-animation query | `ValueAnimator.areAnimatorsEnabled` only on SDK >=26. |
| Networking and alarms | Default NetworkCallback is available at API 24; `setAndAllowWhileIdle` is available since API 23. |
| Java library calls | Used collection/default methods and comparators, `Double.isFinite` and `Math.floorDiv` are supported at API 24; no unguarded java.time/String.join/List.of calls were found. |
| Platform callback declarations | `Service.onTimeout` is an override invoked by newer Android versions; older Android does not call it and no unguarded call into a missing superclass method is made. |
| APK signatures / alignment | Debug and demo release pass apksigner verification with minimum SDK 24; the demo release also passes zipalign verification. |
| Android 7 runtime | **Not run**: no connected device or API 24 system image was available. Static checks and signature verification are not a claim of successful API 24 installation or full runtime behavior. |

The installed virtual-device configuration is API 37; it cannot establish API 24 compatibility. Before the demo, install on a real/API 24 test device and run the main customer/admin, offline sync, dial/maps, notification and chatbot paths. On Android 13+ separately verify notification permission behavior.

## Prompt 14 verification record

`./gradlew test assembleDebug assembleRelease lint --no-daemon` completed successfully with Android Studio's JDK. The configured JVM suite ran **41 tests**, with zero failures/errors. Both debug and unsigned release builds completed, including release R8/resource optimization; lint had zero errors and 90 non-blocking warnings. The debug-signed release copy was subsequently signed and verified. No real release-signing credentials were required or invented.

No phone was connected, so adb installation, API 24 execution, real Firebase/Gemini behavior and release-build UI checks remain recommended manual work. Use [MANUAL_QA.md](MANUAL_QA.md) and [SECURITY_NOTES.md](SECURITY_NOTES.md), especially owner approval, published Firestore rules and API signing-certificate restrictions.
