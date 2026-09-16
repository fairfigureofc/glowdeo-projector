# Glowdeo Projector Player

Standalone repository for the Android projector receiver. **Glowdeo** is the selected working product name. Brand/domain availability has not been checked; the repository folder remains `projector-player`.

## Status

This initial import preserves the custom native camera helper, loopback bridge and Android bridge startup code from the HY310X prototype. It also includes its sanitized Mac companion. It is **not yet a complete Gradle app or a distributable APK**. The installed v3 was assembled from a Chromium/AOSP Browser2-derived shell; the next milestone is a clean, reproducible Android source build with reviewed third-party notices.

No compiled APKs, pairing credentials, private room photos, sports footage, or room-specific scenes are tracked. Existing local APKs are unchanged and should not be offered as a general-purpose friend release.

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

A GitHub workflow checks commit messages and repository tooling. It **does not build or test Android yet**; Android build/lint/device jobs belong in the first clean-app milestone.

See [quality and testing](docs/quality-and-testing.md), [friend release](docs/friend-release.md), and [migration plan](docs/migration.md).

Future product ideas: [Glowdeo wish list](docs/future-wishlist.md).
