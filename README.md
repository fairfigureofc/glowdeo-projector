# Glowdeo Projector Player

Standalone repository for the Android projector receiver. **Glowdeo** is the selected working product name. Brand/domain availability has not been checked; the repository folder remains `projector-player`.

## Status

A clean Kotlin/Gradle Android app is now implemented under `app/`: offline demo playback, alignment grid, D-pad controls and saved manual mapping for wall, TV blackout and featured panel. Build and test results are documented with each release; source presence alone does not mean a build has passed.

See [standalone alpha setup and limitations](docs/standalone-alpha.md). The `prototype/` directory is reference-only and is not compiled into the new app. No private pairing keys, personal room photos or sports footage are included.

## Scope

The Android projector app pairs with a controller, receives scenes, renders fullscreen and persists mapping. iOS/Android phone controllers and the Laravel web/API are separate clients/projects. A projector without Android can eventually use an external supported Android player via HDMI; that path remains untested.

## Prototype source

- `prototype/native/bridge.c`: native helper, bound only to projector loopback.
- `prototype/native/capture.c`: V4L2 capture path, specific to tested hardware.
- `prototype/android/Bridge.smali`: custom helper startup from the old shell.
- `prototype/companion/server.py`: Mac companion reference. It still uses the original workspace-relative runtime directory; it is reference code, not the production pairing service.

These are migration references, not audited production networking components. Do not expose the native helper on the LAN. Shared compile-time secrets must be replaced by per-installation revocable credentials before distribution.

## Development process

Enable the local Conventional Commit hook:

```sh
git config core.hooksPath .githooks
python3 -m unittest discover -s tests -v
```

Examples: `feat(pairing): add code entry`, `fix(mapping): restore corners after restart`, `test(player): cover offline launch`. Use `!` and a `BREAKING CHANGE:` footer for incompatible changes.

GitHub workflows check Conventional Commits, Android Lint, ktlint, detekt and unit tests, build APKs, and run emulator mapping tests. Real-projector acceptance remains a separate release gate.

See [quality and testing](docs/quality-and-testing.md), [friend release](docs/friend-release.md), and [migration plan](docs/migration.md).

Future product ideas: [Glowdeo wish list](docs/future-wishlist.md).
