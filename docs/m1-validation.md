# M01 build validation

Validation performed on July 12, 2026:

- Branch: `agent/initial-android-scaffold`
- JDK: Microsoft OpenJDK 17.0.19
- Android Gradle Plugin: 8.13.2
- Gradle wrapper: 8.13
- Compile SDK: Android 36.1
- Target SDK: Android 36
- Build Tools: 36.1.0

The current Linux validation command is:

```bash
./gradlew test lint :app:assembleDebug --console=plain --no-daemon --max-workers=2 --no-problems-report -Pkotlin.compiler.execution.strategy=in-process
```

Results:

- Debug and release unit tests passed.
- Android lint passed and produced no issues.
- Debug APK assembly passed.
- The debug APK was produced successfully.

Environment warnings:

- The installed command-line SDK tooling understands SDK XML through version 3 while the Android 36.1 package contains version 4 metadata. This did not affect compilation, tests, lint, or packaging.
- The debug packaging task kept `libandroidx.graphics.path.so` unstripped. This is informational for the debug artifact.

No Android device or AVD was available on this machine, so installation and launch remain the final local M01 verification step.

## Secondary-host revalidation

Revalidated on July 13, 2026 after cloning the branch on a second development host:

- Android Studio: 2026.1.1 (`AI-261.23567.138.2611.15646644`)
- JDK: Microsoft OpenJDK 17.0.19
- Gradle wrapper: 8.13
- Android Gradle Plugin: 8.13.2
- Compile SDK: Android 36
- Target SDK: Android 35
- Build Tools: 36.1.0
- Emulator: Pixel 7 profile with the Android 15 / API 35 Google APIs x86_64 image

The validation workflow completed with JDK 17. Debug and release unit tests passed, lint completed with 0 errors and 12 understood warnings, and `assembleDebug` produced the debug APK.

The APK installed successfully on the API 35 emulator. After granting the Android 13+ notification permission, `MainActivity` was confirmed as the top resumed activity, the application process remained alive, and no fatal exception was present in the post-launch log. A visual check confirmed that the native Compose Now Playing screen rendered correctly.
