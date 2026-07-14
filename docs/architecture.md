# Architecture

24Seven.FM Player is a native Kotlin/Compose application. It uses MVVM with unidirectional data flow and keeps Android UI, playback, and remote protocols independent.

## Dependency direction

```text
Compose UI -> ViewModel -> domain repository interface
                               ^
                               |
                         data implementation

Compose UI -> ViewModel -> playback interface -> Media3 implementation
```

The UI must not import Retrofit, OkHttp, cookies, WebSockets, or ExoPlayer. A screen receives immutable UI state and emits actions to its ViewModel.

## Station scope

`StationId` is required for station-specific metadata, chat, queues, history, and requests. Do not assume that every station supports every feature. `StationCapabilities` controls which destinations and actions are shown.

The bundled catalog contains identities and public website addresses only. Stream URLs and protocol endpoints must come from a reviewed source rather than guesses embedded in UI code.

The initial stream addresses were extracted from station-provided PLS playlists. Each catalog entry keeps the primary `hi5` relay first and the `hi` source stream as a fallback. Because those playlists use HTTP, Android cleartext access is permitted only for the five explicit station domains; cleartext remains disabled globally.

All ten committed relay URLs advertise `audio/aacp` and 128 kbps through ICY headers. The catalog therefore records `StreamFormat.Aac` and 128 kbps based on protocol evidence, not hostname inference. Revalidate this evidence before changing the labels.

Live ICY titles flow from the service-owned player through a station-scoped `NowPlayingRepository` contract. Compose receives only immutable domain state. ICY supplies one composite title, so the application displays it unchanged and does not guess artist, album, composer, duration, or artwork fields.


## Playback ownership

`RadioPlaybackService` owns the single `ExoPlayer` and `MediaSession`. The application connects through a Media3 `MediaController` adapter implementing the domain-facing `PlaybackController` interface. Switching stations stops the current item and atomically replaces the playlist with the selected station's ordered primary and fallback streams; two stations can never play simultaneously. The service advances to the fallback once when the primary stream fails.

## Native navigation

The selected Player, Chat, Queue, or More destination is immutable `MainUiState` owned by `MainViewModel`. Destination composables emit navigation and playback actions upward. Secondary destinations reuse the same domain playback and now-playing state through a persistent mini-player; they never connect to Media3 directly.

The phone shell uses bottom navigation below 600 dp. Wider layouts use a navigation rail and content pane. Chat remains a navigable unavailable destination. Queue content is controlled by verified station capability flags and repository state.

## Queue and history

`QueueRepository` is station-scoped and exposes immutable unavailable, loading, ready, and error state. `MainViewModel` observes it only while Queue is selected, switches the flow when `StationId` changes, and cancels observation when leaving Queue. Compose only renders the resulting state or emits a refresh action upward.

`PollingQueueRepository` performs one request immediately and then no more than once every 60 seconds. Manual refresh uses the same limiter. The remote adapter supplies bounded I/O, station mapping, and defensive parsing for each station's public queue source. Extended sources display at most 30 upcoming and 30 recently played tracks; compact sources display the rows they provide. The parser preserves explicit track fields, does not guess unavailable fields, and only accepts artwork hosted by the selected station domain. See `docs/m6-queue-research.md`.

`SongRequestRepository` owns station-scoped transient search, album, eligibility, confirmation, and submission
state. Catalog reads are user initiated and never polled. The remote adapter accepts only same-origin HTTPS album
and request actions, and submission requires the M7 protected station session. Compose receives immutable state
and emits search, album, prepare, cancel, and confirm actions upward. A state-changing request is made only after
two explicit actions and is never retried. See `docs/m9-request-research.md`.

## Authentication

Authentication lives behind the station-scoped `AuthRepository`; Compose receives immutable state and emits
actions without reading cookies. The data layer owns the verified legacy form challenge, same-origin redirects,
bounded responses, session cookies, and signed-in response classification. Passwords and security-code answers
remain transient.

Session cookies and display identity are encrypted with an Android Keystore AES-GCM key. Restored sessions are
revalidated when the station is reachable; an anonymous response clears them, while a network failure preserves
the protected cached identity so public playback remains available offline. Sign-out clears both the in-memory
cookie manager and protected storage even if the remote logout request fails. See `docs/m7-auth-research.md` and
`docs/m7-validation.md`.

## Chat

Chat depends on a station-scoped `ChatRepository`. `MainViewModel` observes it only while Chat is selected and
cancels collection on destination or station changes. The verified public interface uses a same-origin HTML view
with a 30-second browser reload, so the repository applies the same minimum interval to scheduled and manual
reads. Parsed messages are bounded and memory-only. Compose renders plain immutable author, message, and displayed
timestamp values.

Posting is available only with a protected station session. The data layer obtains the current same-origin form,
keeps its station-issued account material transient, enforces the 255-character ISO-8859-1 boundary, submits the
user's message, and performs one confirmation read. Cookies and posting material never reach the ViewModel or UI.
See `docs/m8-chat-research.md`.

## Initial modules

The project begins as one Android application module organized by package. Modules should be split only when boundaries are stable enough to justify the additional Gradle complexity.
