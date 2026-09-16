# Glowdeo future wish list

Glowdeo is the selected working product name. Availability has not been checked.

## Watch-party video panels

Status: future idea, not part of the initial Team Mode / projector-pairing milestone.

Friends join a private room and stream their faces into mapped panels on the wall around the TV, like a shared video chat room. Each person watches their own channel or stream; Glowdeo carries participant video/chat rather than rebroadcasting the game.

Suggested first experience:
- Host creates a room and invites friends with a link or code.
- Friends join from their phone with a camera/microphone preview and explicit join controls.
- Host assigns participant tiles to saved projection panels. Keep the TV picture unobstructed.
- Start with a small group; determine the participant limit from actual projector performance tests.
- Provide mute, camera-off, leave, host removal and clear connection status. No recording by default.
- Use a separate camera for participant video; camera hardware and audio routing are deferred. The projector displays remote faces. Built-in projector camera access is not required.
- Keep fantasy overlays optional so Team Mode and general watch parties can use the same feature.

Design questions for later:
- Audio routing and TV echo: one active room microphone/speaker path, headset support, and echo-control testing.
- Optional reaction-only / muted-video mode while watching a game.
- Video transport, relay costs, latency and hardware decoding support need a technical spike before selecting an implementation.
- Different TV streams have different delays; joining a room does not imply synchronized games.

Dependencies: reliable pairing, saved panel layouts, fullscreen playback and device capability reporting. Build the core player first.
