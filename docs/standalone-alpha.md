# Standalone alpha 0.1.0

## What this build does

- Native Kotlin Android app, fullscreen landscape Canvas rendering.
- Runs fully offline; no network or camera permission.
- Remote D-pad and touch/mouse controls.
- Alignment grid and labeled sample Team Mode demo.
- Independently saved four-corner wall boundary, TV blackout and featured-player panel.
- Rejects crossed, collapsed or offscreen saved mappings and falls back to safe defaults.
- Pause animation, discard edits and confirmed reset.

## First run

Install the APK through your device's normal package installer (you may need to allow that file manager to install apps). Open Glowdeo from the normal or Android TV launcher. Select Map wall boundary, then Map TV blackout, then Map featured panel. Arrows move the selected corner; OK selects the next corner. Back opens Save/options. Save each surface. Choose Play offline demo. Press OK or tap to reopen the menu.

When mapping the lower corners, the temporary controls may overlap the projection; they disappear during playback. Android Home always leaves the app. Changes are persisted only when saved. Returning to the app opens the controls. The grid also honors the TV blackout.

## Boundaries

This alpha does not connect to the Laravel API, pair with a phone, load live sports data, access the camera, autofocus, or import custom media. The existing Mac-controlled Projection Player remains a separate installation. The new APK does not replace it.

## Build

Use JDK 17 and Android SDK platform 35 / build tools 35.0.0. Set ANDROID_HOME or local.properties (ignored by Git). The Gradle wrapper pins Gradle 8.11.1 and verifies its distribution SHA-256.

```sh
./gradlew ktlintFormat
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug assembleRelease
./gradlew connectedDebugAndroidTest
```

Debug: `app/build/outputs/apk/debug/app-debug.apk`, package `com.glowdeo.player.debug`. Release: unsigned `app/build/outputs/apk/release/app-release-unsigned.apk`, package `com.glowdeo.player`. A stable private release key must sign the release APK before distributing it; never publish that key. The chosen application ID is provisional until branding/ownership is settled.

CI builds debug and unsigned release variants, runs analysis/unit tests, then emulator persistence/remote-mapping tests. Debug CI artifacts are for testing and can require uninstalling an older debug build if the signing certificate differs. Use a consistently signed release APK for friend updates.

## Physical device acceptance still required

Verify installation, launcher visibility, D-pad operation, TV blackout alignment, save/reboot persistence, and a sustained playback session on each supported projector. Emulator success does not establish HY310X or universal hardware compatibility. Add tested models to this document only after observation.
