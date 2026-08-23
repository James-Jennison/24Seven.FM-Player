# M02 playback validation

Validation performed on July 13, 2026.

Implemented and build-verified:

- domain-facing `PlaybackController` with immutable playback state;
- Media3 `MediaController` connection to the service-owned player;
- play, pause, and stop actions;
- atomic station switching;
- ordered primary/source playlists and one-step fallback on playback error;
- station metadata supplied to the media session notification;
- UI states for idle, connecting, buffering, playing, paused, retrying, and error.

The current Linux validation command is:

```bash
./gradlew test lint :app:assembleDebug --console=plain --no-daemon --max-workers=2 --no-problems-report -Pkotlin.compiler.execution.strategy=in-process
```

Results:

- Debug and release unit tests passed.
- Android lint passed.
- Debug APK assembly passed.

No Android device or AVD was connected. Playback of all five live streams, primary-to-source fallback timing, audio focus, notification controls, and station switching under real network conditions still require device validation before M02 is complete.

## Motorola Razr 2023 follow-up

On July 13, 2026, a Motorola Razr 2023 was connected through wireless ADB over Tailscale and identified as Android 15 (API 35), 1080 x 2640 at 420 dpi. The project runtime target was aligned to API 35 while retaining compile SDK 36, which is required by Activity 1.11 and Media3 1.10.

The full `test lint assembleDebug` validation passed with this configuration. The debug APK installed successfully on the Razr, Android reported `minSdk=26 targetSdk=35`, and `MainActivity` completed a successful cold launch.

On-device playback verification covered:

- sustained primary-stream playback for StreamingSoundtracks.com and Adagio.FM;
- atomic station switching from StreamingSoundtracks.com to Adagio.FM while playing;
- background continuation with a foreground `MediaSessionService` and correct station metadata;
- notification permission request and a visible Android 15 media notification;
- system media pause and resume commands;
- in-app stop returning the session to idle;
- notification controls that do not expose the internal fallback stream as a user-selectable next item.

No playback exceptions were recorded during these checks.

## M02 completion follow-up

On July 13, 2026, the validated debug APK was installed on the Motorola Razr 2023 from the secondary development host. The previous debug installation first had to be removed because Android debug keys are machine-specific and its signature did not match the new build.

The remaining live stations were then validated on the Razr:

- 1980s.FM sustained Media3 `PLAYING` state with matching session metadata and no playback error;
- Death.FM sustained Media3 `PLAYING` state with matching session metadata and no playback error;
- Entranced.FM sustained Media3 `PLAYING` state with matching session metadata and no playback error.

Two consecutive switching cycles covered all five stations. All ten switches returned to `PLAYING` with metadata matching the selected station. Android reported one application MediaSession throughout the test, and no ExoPlayer, HTTP data-source, or fatal application error was recorded.

Primary-to-source fallback was exercised on the local API 35 emulator without changing either production URL. An emulator-local firewall rule rejected only the resolved address of the 1980s.FM primary relay while leaving the source relay reachable. Media3 progressed from buffering item 0 to buffering item 1 and then `PLAYING` on item 1. The expected primary connection failure was present in the log, the source fallback played successfully, and the temporary firewall rule was removed immediately after the check.

Playback was stopped on both devices after validation. These results complete the planned M02 playback validation.
