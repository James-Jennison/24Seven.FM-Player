# 24Seven.FM Player

An unofficial, community-built native Android client for the 24seven.FM internet-radio network.

The application is written in Kotlin with Jetpack Compose and Jetpack Media3. It does not use a WebView and is not affiliated with or endorsed by 24seven.FM or its stations.

## Planned stations

- StreamingSoundtracks.com
- 1980s.FM
- Adagio.FM
- Death.FM
- Entranced.FM

## Current milestone

- Station-first domain model
- MVVM and unidirectional data flow
- Compose phone navigation shell
- Media3 `MediaSessionService` for background playback
- Play, pause, stop, atomic station switching, and one-step stream fallback
- Immutable playback state exposed through a domain-facing controller
- Repository boundaries for future metadata, authentication, chat, and requests

Audio stream addresses come from station-provided playlists and remain subject to device verification. API endpoints are intentionally not guessed and will be added only after verification and review of supported integration options.

## Building

Open the repository in a current Android Studio release with JDK 17. The project targets Android API 36, compiles against Android 36.1, and supports Android 8.0 (API 26) and newer.

From PowerShell, validate the project with:

```powershell
.\gradlew.bat test lint assembleDebug
```

If the repository is inside a OneDrive-synced directory and Gradle stalls on file operations, direct app build outputs to a local directory before running Gradle:

```powershell
$env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR="$env:TEMP\24seven-android-build"
```

See [CONTRIBUTING.md](CONTRIBUTING.md) before contributing.

