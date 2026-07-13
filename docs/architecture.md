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

Codec and bitrate remain unknown until stream response headers or device playback verify them. Do not infer AAC, MP3, or a bitrate from the `hi` hostname.


## Playback ownership

`RadioPlaybackService` owns the single `ExoPlayer` and `MediaSession`. The application connects through a Media3 `MediaController` adapter implementing the domain-facing `PlaybackController` interface. Switching stations stops the current item and atomically replaces the playlist with the selected station's ordered primary and fallback streams; two stations can never play simultaneously. The service advances to the fallback once when the primary stream fails.

## Authentication

Authentication will live behind `AuthRepository`. Composables never read or store cookies. If the network uses legacy form login, its CSRF, redirect, and session-cookie behavior will be handled by the data layer. No credentials or captured session values belong in the repository.

## Chat

Chat will depend on a replaceable transport contract. Network inspection must determine whether each station uses WebSocket, server-sent events, long polling, or ordinary polling before an implementation is selected.

## Initial modules

The project begins as one Android application module organized by package. Modules should be split only when boundaries are stable enough to justify the additional Gradle complexity.
