# Friends alpha release

First target: manual mapping and offline demo playback on explicitly tested Android projectors. Camera-assisted calibration is optional. The HY310X camera access relies on its firmware and is not a portable Android camera guarantee.

Before sharing:
- The standalone alpha excludes the prototype bridge and has no network endpoint or shared build key. Future controller support requires per-device pairing and in-app revocation.
- Provide neutral bundled demo artwork; omit personal photos and unlicensed third-party sports footage.
- Show supported features and an actionable fallback when a camera, WebView or rendering capability is missing.
- Produce a signed release APK from the clean source build. Choose the production application ID and privately back up its signing key before the first alpha; future updates require a compatible signature and increasing versionCode.
- Complete the release tests in quality-and-testing.md. Document tested device models, Android versions, installation steps, known limitations and how to uninstall/reset.

Suggested distribution: a private GitHub Release with the signed APK, SHA-256, release notes and known issues once a remote exists. This is a distribution plan, not a release already created. Keep APKs in release assets rather than source history. Later evaluate Play testing or another managed tester channel.

The existing prototype APK is not the first friends release. No current signing key or embedded pairing key should be published.
