# M2 playback validation

Validation performed on July 13, 2026.

Implemented and build-verified:

- domain-facing `PlaybackController` with immutable playback state;
- Media3 `MediaController` connection to the service-owned player;
- play, pause, and stop actions;
- atomic station switching;
- ordered primary/source playlists and one-step fallback on playback error;
- station metadata supplied to the media session notification;
- UI states for idle, connecting, buffering, playing, paused, retrying, and error.

The following command completed successfully:

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR="$env:TEMP\24seven-android-build"
.\gradlew.bat test lint assembleDebug --console=plain --no-daemon --max-workers=2 --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process'
```

Results:

- Debug and release unit tests passed.
- Android lint passed.
- Debug APK assembly passed.

No Android device or AVD was connected. Playback of all five live streams, primary-to-source fallback timing, audio focus, notification controls, and station switching under real network conditions still require device validation before M2 is complete.
