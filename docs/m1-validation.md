# M1 build validation

Validation performed on July 12, 2026:

- Branch: `agent/initial-android-scaffold`
- JDK: Microsoft OpenJDK 17.0.19
- Android Gradle Plugin: 8.13.2
- Gradle wrapper: 8.13
- Compile SDK: Android 36.1
- Target SDK: Android 36
- Build Tools: 36.1.0

The repository is stored in a OneDrive-synced directory. To avoid Windows file-locking delays, app build outputs were redirected to `%TEMP%\24seven-android-build` with `TWENTYFOURSEVEN_ANDROID_BUILD_DIR`.

The following command completed successfully:

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR="$env:TEMP\24seven-android-build"
.\gradlew.bat test lint assembleDebug --console=plain --no-daemon --max-workers=2 --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process'
```

Results:

- Debug and release unit tests passed.
- Android lint passed and produced no issues.
- Debug APK assembly passed.
- The debug APK was produced successfully.

Environment warnings:

- The installed command-line SDK tooling understands SDK XML through version 3 while the Android 36.1 package contains version 4 metadata. This did not affect compilation, tests, lint, or packaging.
- The debug packaging task kept `libandroidx.graphics.path.so` unstripped. This is informational for the debug artifact.

No Android device or AVD was available on this machine, so installation and launch remain the final local M1 verification step.
