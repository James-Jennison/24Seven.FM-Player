# 24Seven.FM Player

An unofficial, community-built native Android client for the 24seven.FM internet-radio network.

The application is written in Kotlin with Jetpack Compose and Jetpack Media3. It does not use a WebView and is not affiliated with or endorsed by 24seven.FM or its stations.

## Supported stations

- StreamingSoundtracks.com
- 1980s.FM
- Adagio.FM
- Death.FM
- Entranced.FM

## Alpha status

Milestones M1–M12 are complete and published on the development branch. M13 Independent Accounts UX is next and has not started. Shared feature work continues through M17, each station receives an explicit M18–M22 certification milestone, and Alpha distribution/publication remain last at M23–M24.

The current Alpha provides a responsive native player, service-owned Media3 playback, five-station navigation, live metadata and artwork, queue/history, station-scoped authentication, chat, song requests, and signed-in favorite-track browsing. Request availability is conservatively revalidated against fresh station and Queue data before submission. Unit tests, lint, debug assembly, 13/13 API 35 emulator tests, and physical Razr inspection are green.

## Project Roadmap

This is the single milestone sequence for the project. Detailed completion gates, estimates, dependencies, and confidence risks are maintained in [the implementation plan](docs/IMPLEMENTATION_PLAN.md).

### Phase 1 — Completed foundation

| Milestone | Status | Completed | Commit | Outcome |
| --- | --- | --- | --- | --- |
| M1 Buildable baseline | ✅ Complete | 2026-07-12 | [`9184afd`](https://github.com/codeframe78/24Seven.FM-Player/commit/9184afd) | Native Gradle/Compose baseline and Windows build path |
| M2 Five-station playback | ✅ Complete | 2026-07-13 | [`5cdade0`](https://github.com/codeframe78/24Seven.FM-Player/commit/5cdade0) | Verified streams, atomic switching, and fallback |
| M3 Background/system media | ✅ Complete | 2026-07-13 | [`4206fee`](https://github.com/codeframe78/24Seven.FM-Player/commit/4206fee) | Service-owned playback, MediaSession, focus, and route controls |
| M4 Now playing | ✅ Complete | 2026-07-13 | [`0d9e6b3`](https://github.com/codeframe78/24Seven.FM-Player/commit/0d9e6b3) | Live ICY metadata and station artwork |
| M5 Native navigation | ✅ Complete | 2026-07-13 | [`8133695`](https://github.com/codeframe78/24Seven.FM-Player/commit/8133695) | Adaptive native destinations and mini-player |
| M6 Queue and history | ✅ Complete | 2026-07-13 | [`3babdf9`](https://github.com/codeframe78/24Seven.FM-Player/commit/3babdf9) | Bounded station-scoped Queue/History polling |
| M7 Authentication | ✅ Complete | 2026-07-13 | [`a897794`](https://github.com/codeframe78/24Seven.FM-Player/commit/a897794) | Native station login and protected sessions |
| M8 Chat | ✅ Complete | 2026-07-13 | [`f99635d`](https://github.com/codeframe78/24Seven.FM-Player/commit/f99635d) | Native memory-only station chat |
| M9 Song requests | ✅ Complete | 2026-07-13 | [`e748f27`](https://github.com/codeframe78/24Seven.FM-Player/commit/e748f27) | Native search, confirmation, and one-shot submission |
| M10 Request attribution/messages | ✅ Complete | 2026-07-14 | [`73a0d89`](https://github.com/codeframe78/24Seven.FM-Player/commit/73a0d89) | Queue attribution and verified SST request messages |
| M11 Adaptive Alpha UI | ✅ Complete | 2026-07-14 | [`4735f13`](https://github.com/codeframe78/24Seven.FM-Player/commit/4735f13) | Responsive branded UI, previews, accessibility, and exit flow |
| M12 Queue-aware request availability | ✅ Complete | 2026-07-14 | [`4735f13`](https://github.com/codeframe78/24Seven.FM-Player/commit/4735f13) | Conservative station-scoped availability, accessible exact labels, and fresh pre-submit checks |

### Phase 2 — Shared feature completion

| Milestone | Size | Estimate | Status | Outcome |
| --- | --- | --- | --- | --- |
| M13 Independent Accounts UX | L | 4–8 hours | 🚧 Current; preflight ready | Five separately visible account states with pairwise session/logout/expiration isolation |
| M14 Local personalization | M | 2–4 hours | ⏳ Planned | Persist default/last station and clearly distinguish local preferences from station-owned data |
| M15 Request history and membership | L | 4–8 hours | ⏳ Planned | Station-specific request history, cooldown, VIP/RIP, and membership presentation where verified |
| M16 Secondary content access | M | 2–4 hours | ⏳ Planned | Capability-aware native or Custom Tab access to selected verified public modules |
| M17 Private Messages | L provisional | 4–8 hours after server repair | 🧊 Deferred | Native station-isolated inbox, read, compose, reply, refresh, and explicit user-initiated send |

### Phase 3 — Station certification

These milestones harden and certify the shared app against each station; they do not create five forks. Station-specific behavior remains behind capability flags and repository contracts.

| Milestone | Size | Estimate | Status | Certification emphasis |
| --- | --- | --- | --- | --- |
| M18 StreamingSoundtracks.com | S | 2–4 hours | ⏳ Planned | VIP/non-admin behavior, 30-row Queue, request messages, Favorites, chat, and authenticated workflows |
| M19 1980s.FM | M | 4–7 hours | ⏳ Planned | Independent account/session behavior, station rules, requests, Favorites, chat, metadata, and fallback |
| M20 Adagio.FM | M | 4–7 hours | ⏳ Planned | Classical metadata presentation, independent account/session behavior, requests, Favorites, chat, and fallback |
| M21 Death.FM | L | 6–10 hours | ⏳ Planned | Compact Queue feed, sparse metadata/artwork, RIP membership behavior, requests, chat, and fallback |
| M22 Entranced.FM | M | 4–7 hours | ⏳ Planned | Independent account/session behavior, station rules, requests, Favorites, chat, metadata, and fallback |

Each station gate covers playback and fallback, metadata/artwork, Queue/history, authentication, chat, Favorites, requests, membership differences, Private Messages when available, wired-device smoke testing, documentation, tests, and remote publication. Unblocked certification work may proceed while M17 is deferred, but final station completion requires every in-scope capability to pass or an explicit scope decision.

### Phase 4 — Distribution

| Milestone | Size | Estimate | Status | Outcome |
| --- | --- | --- | --- | --- |
| M23 Alpha distribution readiness | M | 1–2 focused days | ⏸ Deferred | Refresh and finalize privacy, signing, tester, bundle, and Play-readiness work after M13–M22 |
| M24 Alpha publication | M | 1–3 hours after activation | ⛔ Blocked | Verify the signed Play bundle and publish the authorized internal/closed test release |

Early M23 preparation is preserved on this branch, but it will be refreshed only after feature and station certification work. Google Play activation remains an external dependency for M23–M24.

The app is fully native and uses immutable Compose UI state, repository boundaries, and station capability flags. It includes play, pause, stop, live metadata and artwork, a persistent mini-player, signed-in favorite-track browsing/filtering, capability-aware screens, native loading/error/empty states, and Android Keystore-backed account sessions. Remote data stays bounded to the documented station interfaces and their approved refresh rules.

## Screenshots

These captures are from the physical API 35 Razr using live StreamingSoundtracks.com data, so track and chat content will naturally change over time. The Player and Queue captures show the M11 Alpha shell; Chat and Requests retain their already-working native M8–M10 content presentation.

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/player.png" alt="Adaptive native player with 24Seven.FM artwork, station carousel, and live controls" width="300"><br><strong>Adaptive Player and station carousel</strong></td>
    <td align="center"><img src="docs/screenshots/queue.png" alt="Native upcoming queue with artwork and the persistent M11 mini-player" width="300"><br><strong>Queue and persistent mini-player</strong></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/chat.png" alt="Native station chat" width="300"><br><strong>Live native chat</strong></td>
    <td align="center"><img src="docs/screenshots/requests.png" alt="Native station library search" width="300"><br><strong>Song-request library search</strong></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/favorites.png" alt="Native favorite-track list with accessible green available and red unavailable stoplights" width="300"><br><strong>Favorite tracks and request availability</strong></td>
    <td align="center"><img src="docs/screenshots/vip-request-message-success.png" alt="Verified native VIP request with requester attribution and exact message in Queue" width="300"><br><strong>Verified request attribution and message</strong></td>
  </tr>
</table>

Audio stream addresses come from station-provided playlists and remain subject to device verification. Remote interfaces are added only after source verification and permission review. See the milestone research and validation documents under [docs](docs) for authorization, protocol evidence, limits, and device results.

M17 tracks the native Private Messages experience, which remains deferred until the website's underlying server issues and production behavior are settled. See [docs/future-scope.md](docs/future-scope.md).

Alpha testers and distributors should read [the privacy notice](PRIVACY.md), [Alpha testing guide](docs/alpha-testing.md), [release notes](docs/releases/0.1.0-alpha01.md), [Play Console checklist](docs/play-console-checklist.md), and [M23 signing handoff](docs/m23-alpha-readiness.md). Development debug APKs are not intended for external distribution.

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

