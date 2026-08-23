# M11 Adaptive Alpha UI preservation plan

Audited July 14, 2026 on `agent/initial-android-scaffold` before presentation-layer changes.
The worktree was clean at audit time. This milestone evolves the existing native UI; it does not replace the application, its architecture, or its network and playback implementations.

## Architecture found

- One native Android application module, `:app`, using Kotlin, Jetpack Compose Material 3, MVVM, and Media3.
- `MainActivity` lifecycle-collects the immutable `MainUiState` exposed by `MainViewModel`.
- `MainViewModel` depends only on repository and controller contracts: station, playback, now playing, queue, authentication, chat, and song requests.
- Application-scoped repositories and the Media3 playback service remain the sources of truth. Composables receive state and emit actions.
- A single service-owned ExoPlayer and MediaSession provide playback, notification and lock-screen controls, audio focus, headset/Bluetooth controls, and background continuity.
- Existing destinations are Player, Chat, Queue, and More. Phone navigation uses a bottom bar, wider layouts use a navigation rail, and non-Player destinations retain a mini-player.
- Authentication is station-scoped and stored using Android Keystore-backed AES-GCM. Chat content and request state are transient.

## Milestones 1–10 feature-preservation matrix

| Existing feature | Current implementation | Current behavior and UI | Existing coverage | M11 preservation |
| --- | --- | --- | --- | --- |
| Buildable native baseline | Gradle wrapper, `:app`, CI, and Linux validation scripts | API 35 target, API 36 compile platform, Java 17 | Build and workflow validation documented in M01 | Keep toolchain, module, caches, and validators unchanged |
| Five-station catalog | `BootstrapStationRepository`, station domain models | SST, 1980s.FM, Adagio.FM, Death.FM, and Entranced.FM selectable from every destination | Catalog and capability unit tests | Present all five in an accessible station carousel/list and retain the global selector path |
| Station capabilities | `StationCapabilities` and repository contracts | Authentication, chat, requests, queue, and history vary behind capability flags; request messages are SST-only | Catalog, ViewModel, and UI tests | Render only controls supported by the selected station; do not hard-code station behavior in screens |
| Primary and fallback streams | Bundled PLS resources and Media3 playback preparation | Each station uses its verified primary stream, then its verified fallback | Playback mapping/service tests and M02/M03 device validation | Do not alter URLs or fallback ordering; describe fallback attempts without exposing URLs |
| Live playback and station switching | `PlaybackController`, `Media3PlaybackController`, `RadioPlaybackService` | Play, pause, stop, switch station; service owns player | Controller, ViewModel, and service tests | Reuse existing actions; add previous/next *station* UI as deterministic wrapped station selection, never Media3 item skipping |
| Background/system playback | `RadioPlaybackService`, MediaSession, notification | Continues in background and task removal; notification, lock screen, audio focus, headset and Bluetooth commands work | Service instrumentation plus M03 physical/emulator validation | Leave service/controller untouched; both full and mini players observe the same state |
| Now-playing metadata | `NowPlayingRepository`, playback metadata mapping | ICY song/artist text and event-driven same-station artwork; fallback when unavailable | Metadata/repository tests and UI artwork tests | Promote title/artist/station, retain raw metadata fallback, use branded fallback art, and handle long/missing text |
| Queue and history | `QueueRepository`, parsers, polling repository | Native upcoming/recent lists, artwork, duration, requester/message; shared 60-second rate; up to 30 extended rows and 10 compact Death.FM rows | Parser/polling/ViewModel/UI tests | Keep Queue route, rate policy, list limits, stable identities, attribution, messages, and refresh behavior |
| Authentication | `AuthRepository`, station clients, `AuthSessionStore` | Native station-scoped login with alphanumeric CAPTCHA; encrypted retained session and sign out | Parser/repository/session/ViewModel/UI tests | Keep full account flow in More with clearer status and unchanged protected storage |
| Community chat | `ChatRepository`, polling/post transports | Native authenticated read and explicit post, 30-second shared read rate, 255-character post limit; history not persisted | Parser/post/polling/ViewModel/UI tests | Keep Chat primary destination and native composer; preserve memory-only history and rate behavior |
| Song requests | `SongRequestRepository`, request transports | Native search/browse, album drill-in, random and least-played suggestions, explicit confirmation, no retry | Parser/repository/transport/ViewModel/UI tests | Keep complete request flow under More, with clearer hierarchy and capability/auth messaging |
| Request attribution/messages | M10 queue parsing and request-message workflow | Requester/message appear in Queue; SST supports an optional exact 80-character message | M10 parser/transport/UI regression tests | Preserve attribution rendering and SST-only message field and confirmation semantics |
| Navigation and persistent mini-player | `RadioApp`, destination state in `MainViewModel` | Player/Chat/Queue/More routes; bottom bar or rail; mini-player outside Player | `RadioAppTest` and ViewModel destination tests | Retain routes and state; modernize chrome and keep one playback state/controller |
| Connection/error states | Playback state model and `RadioApp` | Idle, connecting, buffering, playing, paused, fallback retrying, error | Playback mapping and UI tests | Give every state distinct accessible copy; keep detailed failures out of the primary presentation |

## Features investigated but not implemented

Favorites, sleep timer, share, Cast or audio-output selection, user settings, diagnostics, general station-selection persistence, and native Private Messages do not exist in the current code or tests. M11 will not add inactive controls or speculative backends for them. Private Messages are now tracked as M47 and remain deferred because of known website/server issues.

An older paragraph in `docs/future-scope.md` describes request messages as future work. The later M10 research, validation, commits, working code, and tests establish that SST request messages are now implemented; that working behavior takes precedence.

## Focused migration plan

### Reuse without behavioral changes

- `MainViewModel`, all domain models, repositories, parsers, transports, playback controller/service, MediaSession, protected session store, and network configuration.
- Existing Chat, Queue, authentication, and song-request action contracts and their navigation routes.
- Existing lifecycle-aware state collection and application-scoped sources of truth.

### Focused refactoring

- Split the large `RadioApp` presentation into reusable adaptive shell, player, station selector, status, artwork, mini-player, and supporting-card composables.
- Replace scattered default Material colors with centralized app and station presentation tokens.
- Refine existing Queue, Chat, and More surfaces without moving network, playback, or persistence logic into Compose.
- Centralize compact, medium, and expanded width behavior using available-window constraints and Material width breakpoints. The layout will respond to the space remaining after system bars, display cutouts, folds, multi-window, and freeform resizing rather than testing a device model.

### New presentation components

- Dark-first/light-capable `TwentyFourSevenTheme` and centralized per-station palettes.
- Atmospheric now-playing artwork treatment with the selected 24Seven.FM logo as trustworthy branded fallback.
- Accessible playback status pill and connection/error messaging.
- Wrapped previous/next station controls, large play/pause action, and horizontally scrolling compact station cards.
- Expanded two-pane player/supporting-content layout and persistent responsive mini-player.
- Double-back exit gate: first back warns, the second within the brief window presents a confirmation dialog; confirmed exit stops playback and closes the task.
- Previews for compact, landscape/medium, expanded, playback states, missing artwork, and long metadata.

### State and configuration behavior

No UI-local playback state will be introduced. Full player and mini-player consume the same immutable `MainUiState`. Station and destination remain in the existing application-scoped/ViewModel flows, playback remains service-owned, and layout changes only select a presentation arrangement. Existing configuration-change behavior therefore remains intact.

### Expected edits

- `MainActivity.kt` for app theme and the exit flow.
- `ui/RadioApp.kt` plus focused files under `ui/components` and `ui/theme` for the adaptive presentation.
- Android resources and manifest entries for the selected app icon and theme colors.
- Focused unit/instrumentation tests and previews.
- README and M11 validation documentation after verification.

Playback, repository, protocol, network-security, bundled PLS, Gradle/toolchain, package, and module files will remain untouched unless a presentation compile defect proves a narrowly scoped change is required.
