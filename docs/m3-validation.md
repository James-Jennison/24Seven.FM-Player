# M3 background playback validation

Validation performed on July 13, 2026.

M3 is complete. The behavioral checks below passed without requiring a production-code change beyond the focused service integration test described below.

## Motorola Razr 2023

Playback was started on Entranced.FM and verified through each lifecycle transition:

- pressing Home preserved `PLAYING` state, matching session metadata, the application process, the foreground `RadioPlaybackService`, and the media notification;
- locking the device and entering idle preserved playback and the foreground service;
- removing the application task from Recents removed the task while playback, the process, service, session, and notification continued;
- system pause, play, and headset-hook commands updated the MediaSession between `PAUSED` and `PLAYING` as expected;
- transient audio-focus loss paused playback and automatically resumed it after focus returned;
- permanent audio-focus loss paused playback and left it paused after the competing owner abandoned focus, requiring an explicit user resume.

The audio-focus checks used a temporary local helper application compiled against Android API 35. The helper was uninstalled and its source and build output were removed after validation.

## API 35 emulator

A rooted local API 35 emulator was used to send Android's protected `AUDIO_BECOMING_NOISY` broadcast. Active 1980s.FM playback transitioned from `PLAYING` to `PAUSED`, confirming Media3's noisy-output handling. Playback was stopped after the check.

An Android instrumentation test now exercises the real `MediaSessionService` integration without opening a live stream. It connects a `MediaController`, confirms that play/pause remains available while the internal fallback playlist's Next and Previous commands remain hidden, releases the controller, explicitly stops the running service, and reconnects successfully after service recreation. The test passed on the local API 35 emulator.

The full debug and release unit-test suite, Android lint, debug APK assembly, instrumentation APK assembly, and the API 35 connected test passed after this coverage was added.

## Bluetooth hardware

A real Bluetooth A2DP accessory was connected to the Motorola Razr 2023 while StreamingSoundtracks.com was playing. The accessory's play/pause control changed the application MediaSession from `PLAYING` to `PAUSED`, and a second press returned it to `PLAYING`. Powering off or disconnecting the accessory while playback was active removed the A2DP route and automatically changed the application session to `PAUSED`. No playback or fatal application error was recorded. The application was stopped after validation.

## Wired-accessory limitation

A physical wired or USB-C headset was not available during validation, so the application was not represented as physically tested with one. Wired media-button handling was exercised through Android's system headset-hook command, and wired route-loss behavior uses the same Media3 noisy-output path exercised by Android's protected `AUDIO_BECOMING_NOISY` broadcast and confirmed through a real Bluetooth route disconnect. The Motorola Razr 2023's wired and USB-C audio playback capability is known independently of this application validation.

Physical wired-accessory coverage may be added later if suitable hardware becomes available, but it is not a blocker for M3 because the application-owned and platform integration paths have direct coverage.
