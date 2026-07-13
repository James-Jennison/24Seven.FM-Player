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

M1 through M3 are complete. M4 Now Playing is in progress with verified live ICY titles and stream-quality evidence.

- Station-first domain model
- MVVM and unidirectional data flow
- Compose phone navigation shell
- Media3 `MediaSessionService` for background playback
- Play, pause, stop, atomic station switching, and one-step stream fallback
- Immutable playback state exposed through a domain-facing controller
- Station-scoped immutable now-playing state behind a repository contract
- Raw live ICY titles in Compose and Android MediaSession metadata
- Evidence-backed AAC / 128 kbps stream quality
- Repository boundaries for future authentication, chat, and requests

Audio stream addresses come from station-provided playlists and remain subject to device verification. API endpoints are intentionally not guessed and will be added only after verification and review of supported integration options.

## Building

Open the repository in a current Android Studio release with JDK 17. The project targets Android 15 (API 35), matching the primary Motorola Razr 2023 test device, and compiles against API 36 as required by its AndroidX dependencies. It supports Android 8.0 (API 26) and newer.

From PowerShell, validate the project with:

```powershell
.\gradlew.bat test lint assembleDebug
```

If the repository is inside a OneDrive-synced directory and Gradle stalls on file operations, direct app build outputs to a local directory before running Gradle:

```powershell
$env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR="$env:TEMP\24seven-android-build"
```

See [CONTRIBUTING.md](CONTRIBUTING.md) before contributing.

For migration to another Windows development machine, follow [docs/CODEX_HANDOFF.md](docs/CODEX_HANDOFF.md) and run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-windows.ps1` after cloning.

