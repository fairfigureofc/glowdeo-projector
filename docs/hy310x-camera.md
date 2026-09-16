# HY310X camera calibration alpha

Glowdeo 0.2.0 targets the Magcubic HY310X. Other Magcubic models are not yet verified. The app does not use ADB, root, a Mac server, or a network connection for camera capture or calibration.

## On the projector

1. Open **Camera / room calibration**. Close the older Projection Player and let the projector's autofocus finish.
2. Choose **Take camera photo · 3 2 1**, allow Camera access, and wait for the countdown and white illumination. A photo is captured one second after the white screen appears.
3. For mapping, pause the TV on black, dim the room, and choose **Calibrate room with projected patterns**. The scan covers the full projected image, including areas outside the saved wall and TV masks. Keep the projector and room still; Back or leaving the app cancels.
4. After the scan, select the wall, TV blackout, or featured panel. Use the remote arrows to place each corner on its visible edge in the photo. OK advances corners. A mouse/touch tap places the selected corner. Choose **Save / grid**, or press Menu, to apply the surface and inspect the projected grid.
5. Refine with the existing manual corner controls if needed. Repeat for the other surfaces. Saved room calibration and projection corners survive app restarts.

Recalibrate after moving the projector or changing its optical direction, focus, or keystone. A display resolution change also requires recalibration. Automatic motion detection and automatic TV/object recognition are not part of this alpha.

## Capture and data

The native module opens `/dev/video0` as the application's ordinary user and uses V4L2 streaming. It reads the current YUYV format without changing the manufacturer's autofocus configuration. Official kernel structs replace the prototype's hand-sized structs, and the APK includes ARM32, ARM64 and x86_64 builds. The module closes the camera on completion, failure, or cancellation. Native frame waits have a six-second overall deadline.

Capture requires an explicit foreground user action and the app's Camera permission. There is no background service or Internet permission. The latest photo and one active room calibration stay in app-private storage. **Delete saved room photo and calibration** removes camera data without removing the manually saved projection corners. Uninstalling or clearing app storage removes all app data.

The scan uses black/white references and nine Gray-code bits per axis, each with an inverse frame: 38 captures. Low-contrast or ambiguous camera pixels are rejected. Photo corners snap only to nearby valid decoded samples; dark/unseen regions are not extrapolated through a guessed wall plane. A failed or cancelled scan does not replace the previous saved room. Scan data and its reference photo are saved together atomically.

## Firmware caveat

The earlier HY310X prototype captured YUYV frames successfully as a normal app user. The new APK has a different package identity and newer Android target level. Firmware permissions must be checked on the actual HY310X before this release is described as hardware-verified. If access is denied, Glowdeo reports the native error and preserves manual mapping; it does not loosen device permissions or silently reuse an old capture.

## Build and validation

JDK 17, Android SDK 35, NDK 28.2.13676358 and CMake 3.22.1 are pinned. The Android workflow builds the native camera module for every packaged ABI and runs the Kotlin/Android checks. Unit tests cover Gray-code decoding, low contrast, incomplete and out-of-order scans, coordinate lookup and unmapped regions. Device tests cover native library loading, room persistence/corrupt data, camera controls without hardware, and existing manual mapping.

Real HY310X acceptance: capture a new photo; run a complete scan; mark all three surfaces; check projected alignment; cancel a scan; deny permission; close/reopen and reboot with developer mode off. These hardware checks must be observed, not inferred from emulator results.
