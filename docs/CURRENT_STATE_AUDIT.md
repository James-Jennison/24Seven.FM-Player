# Current state audit

Updated July 15, 2026 on `agent/initial-android-scaffold` after completing the M20 Adagio.FM authenticated gate, with all existing tracked and untracked work preserved.

## Repository and environment

- Expected and configured remote: `https://github.com/codeframe78/24Seven.FM-Player.git` for fetch and push.
- Active branch: `agent/initial-android-scaffold`.
- Latest implementation commit: `90a7f98` (`Add trusted secondary station pages`), published with the accompanying M16 documentation checkpoint.
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
- Queue and Chat polling are lifecycle-selected and rate-limited to 60 and 30 seconds respectively. Favorites, request catalog, and SST listener-activity reads are user initiated.
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
| M14 | Device-local fixed/last startup station, safe restoration, explicit local-data UI, `m14-validation.md` | Complete and pushed |
| M15 | SST last-ten request history, cooldown/readiness, explicit membership, `m15-*` | Complete and pushed |
| M16 | Capability-aware native directory and exact same-station HTTPS Custom Tabs, `m16-*` | Complete and pushed |
| M18 | SST ordinary/VIP capability reconciliation plus fresh wired playback/public-read proof, `m18-sst-certification.md` | Complete and pushed |
| M23 (prepared early) | Alpha version/privacy/signing guardrails plus Favorites integration | Preserved for refresh after M15–M22; Play account approved, with signing and Console setup still pending |

## Existing screens and navigation

- Player: responsive Now Playing dashboard, station carousel, previous/next and play/pause controls.
- Favorites: authenticated server-favorite list with filter and request entry point.
- Chat: native station chat read/post surface.
- Queue: upcoming queue and recently played lists with artwork, requester, and message fields where supplied.
- More: capability summary, five-station independent Accounts dashboard, SST listener activity, device preferences, privacy notice, request search/suggestions/album tracks, and a capability-aware directory of trusted station pages.
- A persistent mini-player remains present away from Player; compact layouts use bottom navigation and wider layouts use a rail.

## Current data sources and persistence

- Five bundled PLS resources provide the verified primary and fallback streams.
- ICY metadata is playback-event driven; current artwork comes from the station JSON interface.
- Queue/History uses the extended public Queue page on four stations and the compact player JSON feed on Death.FM.
- Authentication, chat posting, requests, Favorites, and SST listener activity reuse station-specific protected sessions.
- Queue, chat, request catalog results, Favorites lists, request messages, and listener activity are in memory. The last/fixed startup station persists as non-sensitive device-local preferences, while authentication sessions persist encrypted. There is no Room database, local listening history, or local track-favorites collection.

## Tests and latest baseline

- 129 `@Test` declarations currently cover parsers, repositories, station definitions and certification contracts, session isolation primitives, startup preference restoration, listener activity, station-page trust policy, playback metadata, ViewModel behavior, Compose navigation/actions, protected storage, and the MediaSession service.
- Latest full local evidence: debug compile, all 108 debug unit tests, lint, Windows validation, wireless debug install, and 21/21 Android 16 Razr instrumentation tests pass after the M21 public/device gate (129 tests total).
- M18–M20 are complete; the M21 public/device checkpoint is recorded in its station evidence document.

## Known defects and technical debt

- Queue and history models do not preserve stable station track IDs. Safe fallback matching therefore needs title, artist/composer, and album—not title alone.
- Station origins are repeated across several adapters; consolidation is desirable when it can be done without destabilizing their separate trust policies.
- Sleep timer and local track favorites are not implemented. Request history/membership is currently enabled only for SST; the other stations await certification evidence.
- Live authentication differences across Death.FM and Entranced.FM still need representative user-entered account/CAPTCHA verification during M21–M22.
- M19 1980s.FM certification is complete. A representative MorG session proved native sign-in, Android-protected process-restart restoration, cross-station isolation, authenticated empty Favorites loading, Chat composer availability, green request eligibility, explicit station-only logout, and persistent signed-out state after another restart. No credentials, CAPTCHA, or session material was retained.
- M20 Adagio.FM certification is complete. A representative MorG session proved native sign-in, Android-protected process-restart restoration, cross-station isolation, authenticated empty Favorites loading, Chat composer availability, green request eligibility, explicit station-only logout, and persistent signed-out state after another restart. Request messages, listener activity, and membership remain explicitly unverified rather than inferred.
- M21's Death.FM public/device gate passes, including live sparse metadata/artwork, both live relay probes, compact Queue/History, Chat, Favorites/request boundaries, native login challenge, and trusted RIP membership route; representative authenticated evidence remains pending.
- M17 Private Messages is deliberately deferred because of known legacy server issues.
- Death.FM's configured HTTPS website recovered during M21. Its exact common routes plus station-specific RIP membership route were independently reverified and enabled under the unchanged same-origin HTTPS trust policy.
- Google approved the Play developer account on July 14, 2026. External Alpha distribution remains sequenced after M15–M22 and still requires Play App Signing setup, a securely held upload key, final release validation, and explicit publication authorization.

## Public five-station audit summary

A conservative read-only audit confirmed common major listener modules across the station family. M16 verified StreamingSoundtracks.com, 1980s.FM, Adagio.FM, and Entranced.FM; M21 later reverified Death.FM after its HTTPS service recovered. Exact public website, forums, members, statistics, Top 100, contact, and membership routes are allowlisted for all five; SST additionally exposes its soundtrack module, 1980s.FM exposes games and awards, and Death.FM uses RIP membership. Native support continues to follow verified capability and protocol evidence rather than the presence of a legacy link alone.
