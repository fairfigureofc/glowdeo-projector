# Android quality gates

Recommended Kotlin toolchain for the clean Android application:

- Android Lint: Android-specific compatibility, resource, accessibility and lifecycle problems. Make release lint errors fail the build.
- ktlint: Kotlin formatting and style. Use one formatting tool and configuration; avoid duplicate formatter rules in detekt.
- detekt: Kotlin complexity and suspicious-code analysis. Add after the first Kotlin module exists and pin a compatible version.
- For retained C code: compiler warnings and clang-tidy/static analysis, plus host-side protocol/parser tests. Android/Kotlin tools do not cover C or smali.

Pin compatible Gradle, Android Gradle Plugin, Kotlin and tool versions. Commit the wrapper and dependency locks/verification metadata; never use floating `latest` versions in CI.

## Per change

1. Formatter check, detekt and Android lint.
2. Local unit tests: normalized mapping transforms, saved scene parsing, pairing/command expiry, duplicate commands, reconnect backoff and capability fallback.
3. Build debug and release variants. Build success alone is not a device test.
4. Emulator/instrumented tests: first launch, pairing and revocation, remote D-pad focus, Back/Home/resume, configuration persistence, camera-denied/no-camera operation, unavailable controller and malformed messages. Use Espresso or Compose UI tests to match the chosen UI toolkit; use UI Automator where system dialogs/remote interaction require it.
5. Run changes involving rendering, storage, Wi-Fi or lifecycle on an actual projector.

Planned commands after Gradle plugins are wired: `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` and `./gradlew connectedDebugAndroidTest`. These tasks do not exist in this prototype import yet.

## Release candidate checks

- Fresh install and update from previous signed release, preserving saved maps and pairing where appropriate.
- Install/run with developer options off. No ADB required for normal operation.
- Android TV remote-only setup as well as touch/mouse when available.
- Supported minimum and current Android versions; real 32-bit ARM HY310X and 64-bit hardware where support is claimed.
- 720p and 1080p rendering, different WebView versions, keystone changes and constrained memory.
- Wi-Fi loss/reconnect, phone lock, controller unavailable, projector reboot, expired pairing and reset/re-pair.
- At least one full game-length playback session for heat, frame pacing and memory growth. Record conditions and findings; do not call untested devices supported.
- Camera permission failures cannot block manual mapping. Camera capture must require an explicit user action and clear indication.
- Verify signed release APK certificate, non-debuggable manifest, versionCode, SHA-256 and installation on a clean device.

References:
- https://developer.android.com/studio/write/lint
- https://developer.android.com/training/testing/fundamentals
- https://detekt.dev/docs/intro/
- https://github.com/pinterest/ktlint
- https://developer.android.com/studio/publish/app-signing
