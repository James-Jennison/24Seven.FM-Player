# 24Seven.FM Player

An unofficial, community-built native Android client for the 24seven.FM internet-radio network.

The application is written in Kotlin with Jetpack Compose and Jetpack Media3. It does not use a WebView and is not affiliated with or endorsed by 24seven.FM or its stations.

## Supported stations

- StreamingSoundtracks.com
- 1980s.FM
- Adagio.FM
- Death.FM
- Entranced.FM

## Current progress

M1 through M9 are complete. M10 is in final live validation: requester attribution is working, and the optional-message flow now handles an accepted song request whose response cannot be read without retrying that song. The corrected build is installed on the physical Android 15 Motorola Razr 2023, and automated unit, lint, release-build, and connected-device coverage is green.

| Milestone | Status | Delivered |
| --- | --- | --- |
| M1 | Complete | Buildable native Android baseline, Gradle wrapper, CI, and Windows setup validation |
| M2 | Complete | Verified playback for all five stations, atomic switching, and one-step stream fallback |
| M3 | Complete | Media3 background playback, notification/lock-screen controls, audio focus, and route-change handling |
| M4 | Complete | Live ICY titles plus station-hosted current-track artwork in Compose and MediaSession metadata |
| M5 | Complete | Native Player, Chat, Queue, and More destinations; persistent mini-player; adaptive phone/tablet navigation |
| M6 | Complete | Live Queue and History with artwork and a shared 60-second limit; up to 30 rows on extended station interfaces and 10 on Death.FM's compact feed |
| M7 | Complete | Native station-scoped authentication, alphanumeric security challenge, and Android-protected session restoration |
| M8 | Complete | Native station chat reading and protected-session posting, with memory-only history and a 30-second read limit |
| M9 | Complete | Native catalog search, album/eligibility browsing, explicit confirmation, and one-shot song requests with no automatic retries |
| M10 | In progress | Explicit requester identity/message display and an exact 80-character StreamingSoundtracks.com message form with safe indeterminate-response handling; final queued-message confirmation remains |

The app is fully native and uses immutable Compose UI state, repository boundaries, and station capability flags. It includes play, pause, stop, live metadata and artwork, a persistent mini-player, capability-aware screens, native loading/error/empty states, and Android Keystore-backed account sessions. Remote data stays bounded to the documented station interfaces and their approved refresh rules.

## Screenshots

These captures are from the physical API 35 Razr using live StreamingSoundtracks.com data, so track and chat content will naturally change over time.

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/player.png" alt="Native player with live artwork and playback controls" width="300"><br><strong>Player and live artwork</strong></td>
    <td align="center"><img src="docs/screenshots/queue.png" alt="Native upcoming queue with artwork, requester attribution, and request message" width="300"><br><strong>Queue, requester message, and mini-player</strong></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/chat.png" alt="Native station chat" width="300"><br><strong>Live native chat</strong></td>
    <td align="center"><img src="docs/screenshots/requests.png" alt="Native station library search" width="300"><br><strong>Song-request library search</strong></td>
  </tr>
</table>

Audio stream addresses come from station-provided playlists and remain subject to device verification. Remote interfaces are added only after source verification and permission review. See the milestone research and validation documents under [docs](docs) for authorization, protocol evidence, limits, and device results.

Planned follow-up work includes final live request-message confirmation and a native Private Messages experience after its authorization and behavior are settled. See [docs/future-scope.md](docs/future-scope.md).

## Building

Open the repository in a current Android Studio release with JDK 17. The project targets Android 15 (API 35), matching the primary Motorola Razr 2023 test device, and compiles against API 36 as required by its AndroidX dependencies. It supports Android 8.0 (API 26) and newer.

From PowerShell, validate the project with:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease
```

If the repository is inside a OneDrive-synced directory and Gradle stalls on file operations, direct app build outputs to a local directory before running Gradle:

```powershell
$env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR="$env:TEMP\24seven-android-build"
```

See [CONTRIBUTING.md](CONTRIBUTING.md) before contributing.

For migration to another Windows development machine, follow [docs/CODEX_HANDOFF.md](docs/CODEX_HANDOFF.md) and run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-windows.ps1` after cloning.

