# Clean Android app milestone

1. Choose the product name and application ID. Keep this repository named projector-player until then.
2. Create a Kotlin/Gradle Android app with fullscreen local playback and remote-friendly navigation. Establish minimum Android/WebView support through tests, not the projector's settings label alone.
3. Move generic scene rendering and calibration into versioned assets; isolate hardware-specific camera code behind an optional capability interface.
4. Add explicit controller setup and an expiring pairing flow against the Laravel API contract. No shared keys or personal addresses in code.
5. Add durable room profiles, offline asset cache, acknowledged scene commands and controlled reconnect behavior.
6. Wire Android Lint, ktlint, detekt, unit tests, emulator tests and signed release build in CI. Keep release credentials separate and never give untrusted pull requests access to them.
7. Complete a real-projector release checklist, then publish a private friends alpha.

The existing prototype remains available locally while this migration is developed. Source ownership/third-party notices must be reviewed before importing any decompiled shell, bundled library or artwork.
