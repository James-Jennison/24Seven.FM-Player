# Current state audit

Updated July 14, 2026 on `agent/initial-android-scaffold` after M14 implementation commit `81c2c4e`, with all existing tracked and untracked work preserved.

## Repository and environment

- Expected and configured remote: `https://github.com/codeframe78/24Seven.FM-Player.git` for fetch and push.
- Active branch: `agent/initial-android-scaffold`.
- Latest implementation commit: `81c2c4e` (`Persist local startup station preferences`), published with the accompanying M14 documentation checkpoint.
- One Android application module, `:app`; application ID `com.codeframe78.twentyfourseven.player`.
- Kotlin 2.2.21, AGP 8.13.2, JDK 17, Compose BOM 2026.06.00, Media3 1.10.1, minSdk 26, targetSdk 35, compileSdk 36.
- The current environment has unrestricted filesystem access, network access, no sandbox, and approval mode `never`.

## Protected uncommitted work

At the time of this audit, the working tree contained the complete M11 adaptive UI, early M23 Alpha distribution preparation, and authenticated five-station Favorites browsing. It included modified and untracked Kotlin, tests, artwork, screenshots, release/privacy documentation, and signing-validation scripts. This work predated the M12 queue-status milestone and was preserved intact.

## Architecture

- Native Kotlin/Compose MVVM with immutable `MainUiState` and actions emitted to `MainViewModel`.
- Domain repository interfaces separate Compose/ViewModels from network and Media3 implementations.
- `RadioPlaybackService` owns the single ExoPlayer and MediaSession; `Media3PlaybackController` is the app-facing adapter.
- `BootstrapStationRepository` owns the five-station catalog, stable IDs, ordered streams, and capability flags.
- Public and authenticated transports are station-scoped and validate expected HTTPS origins. Stream cleartext exceptions remain limited to the five verified relay domains.
- Authentication cookies and display identities are keyed by `StationId`, domain-filtered, and AES-GCM encrypted through Android Keystore-backed storage. Passwords and CAPTCHA answers remain transient.
- Queue and Chat polling are lifecycle-selected and rate-limited to 60 and 30 seconds respectively. Favorites and request catalog reads are user initiated.
- Application wiring remains explicit in `RadioApplication.AppContainer`; no dependency-injection framework or additional module has been introduced.

## Completed milestone evidence

| Milestone | Repository evidence | Current status |
| --- | --- | --- |
| M1 | Gradle wrapper, CI/build scripts, `m1-validation.md` | Complete and pushed |
| M2 | Five verified PLS catalogs, atomic switching/fallback, `m2-validation.md` | Complete and pushed |
| M3 | Media3 service/session, focus/route controls, `m3-validation.md` | Complete and pushed |
| M4 | ICY metadata and station artwork, `m4-metadata-research.md` | Complete and pushed |
| M5 | Native navigation and persistent mini-player, `m5-validation.md` | Complete and pushed |
| M6 | Native Queue/History, bounded 60-second polling, `m6-*` | Complete and pushed |
| M7 | Station-scoped native login and protected restoration, `m7-*` | Complete and pushed |
| M8 | Native memory-only chat and posting, `m8-*` | Complete and pushed |
| M9 | Native catalog search and explicit one-shot requests, `m9-*` | Complete and pushed |
| M10 | Request attribution/messages and random suggestions, `m10-*` | Complete and pushed |
| M11 | Adaptive UI, theme, logo, previews, double-Back exit, `m11-*` | Complete and pushed |
| M12 | Queue-aware request eligibility, fresh pre-submit checks, `m12-validation.md` | Complete and pushed |
| M13 | Five-station Accounts dashboard, expiration state, pairwise session isolation, `m13-validation.md` | Complete and pushed |
| M14 | Device-local fixed/last startup station, safe restoration, explicit local-data UI, `m14-validation.md` | Complete locally; publication in progress |
| M23 (prepared early) | Alpha version/privacy/signing guardrails plus Favorites integration | Preserved for refresh after M15–M22; Play account approved, with signing and Console setup still pending |

## Existing screens and navigation

- Player: responsive Now Playing dashboard, station carousel, previous/next and play/pause controls.
- Favorites: authenticated server-favorite list with filter and request entry point.
- Chat: native station chat read/post surface.
- Queue: upcoming queue and recently played lists with artwork, requester, and message fields where supplied.
- More: capability summary, five-station independent Accounts dashboard, privacy notice, request search/suggestions/album tracks.
- A persistent mini-player remains present away from Player; compact layouts use bottom navigation and wider layouts use a rail.

## Current data sources and persistence

- Five bundled PLS resources provide the verified primary and fallback streams.
- ICY metadata is playback-event driven; current artwork comes from the station JSON interface.
- Queue/History uses the extended public Queue page on four stations and the compact player JSON feed on Death.FM.
- Authentication, chat posting, requests, and Favorites reuse station-specific protected sessions.
- Queue, chat, request catalog results, Favorites lists, and request messages are in memory. The last/fixed startup station persists as non-sensitive device-local preferences, while authentication sessions persist encrypted. There is no Room database, local listening history, or local track-favorites collection.

## Tests and latest baseline

- 109 `@Test` declarations currently cover parsers, repositories, station definitions, session isolation primitives, startup preference restoration, playback metadata, ViewModel behavior, Compose navigation/actions, protected storage, and the MediaSession service.
- Latest full local evidence: debug compile, debug unit tests, lint, and debug assembly pass; 18/18 wired Android 16 Razr instrumentation tests pass.
- M14's full validation and physical-device evidence are recorded in `m14-validation.md`.

## Known defects and technical debt

- Queue and history models do not preserve stable station track IDs. Safe fallback matching therefore needs title, artist/composer, and album—not title alone.
- Station origins are repeated across several adapters; consolidation is desirable but not required for M12.
- Sleep timer, local favorites, request history, and membership status are not implemented.
- Live authentication differences across 1980s.FM, Adagio.FM, Death.FM, and Entranced.FM still need representative user-entered account/CAPTCHA verification during their certification milestones.
- M17 Private Messages is deliberately deferred because of known legacy server issues.
- Google approved the Play developer account on July 14, 2026. External Alpha distribution remains sequenced after M15–M22 and still requires Play App Signing setup, a securely held upload key, final release validation, and explicit publication authorization.

## Public five-station audit summary

A conservative read-only audit confirmed that all five public sites expose the same major listener modules: account, Favorites, Private Messages, forums, request history, chat log, member list, library/requests, queue/played, played history, album pages, stats, Top 100, contact, listening help, news, and membership/donation areas. Station-specific secondary modules differ (for example SST soundtrack features, 1980s games/awards, Death.FM RIP membership). Native support must continue to follow verified capability and protocol evidence rather than the presence of a web link alone.
