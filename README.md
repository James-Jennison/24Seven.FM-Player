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

Milestones M1–M16 and M18–M22 are complete on the development branch. M17 Private Messages remains deferred for legacy server repair. All five station-certification gates now pass; the expanded M23 Play-testing readiness program is in progress and publication remains at M24.

The current Alpha provides a responsive native player, service-owned Media3 playback, five-station navigation, device-local startup/last-station preferences, live metadata and artwork, queue/history, an independent five-station account dashboard with Android-protected sessions, chat, song requests, signed-in favorite-track browsing, a verified SST request-history/cooldown/membership surface, and a trusted browser directory for selected public station content. The media notification body reopens the existing player task, station-library search supports title/album/artist/genre, Library results can be sorted by station order or play state, and Favorites can be sorted by number, track name, album, artist, genre, year, length, or play state. Request availability is conservatively revalidated against fresh station and Queue data before submission, with indexed and stable-state resolution keeping a full 1,500-track VIP/RIP Favorites list responsive; requests sent from Favorites now show the station's success, rejection, or indeterminate-confirmation result without leaving the list. Public community content is now hidden behind an adult age screen, versioned Terms acceptance, and a separate mature-content reveal, with native report-content/report-user/block-user actions and station-scoped local block management. Navigation, Player station cards, and account headers reflow at Android's 2× text setting with an enlarged display while preserving explicit destination semantics. An offline terminal playback failure now presents a clear waiting state and performs one automatic Media3 prepare when validated connectivity returns; pause, stop, or a station change cancels that recovery. All 132 unit tests, debug lint, release bundle checks, the current 40/40 connected suite on the API 35 Pixel Tablet at normal settings, the prior 39/39 suite at maximum tested accessibility settings, prior 36/36 connected suites on API 26, API 36, the API 35 16 KB runtime, Pixel Fold open, and Pixel Tablet landscape, focused current-head phone/Fold/accessibility checks, live phone and Tablet network cut/restore cycles, the earlier 21/21 physical Razr suite, Windows validation, and a complete physical Razr open/tabletop/closed/reopened playback cycle are green. Startup, memory, and large-Favorites diagnostics are recorded in [the M23 performance validation](docs/m23-performance-validation.md), with the adaptive device contract in [the M23 compatibility matrix](docs/m23-device-compatibility.md).

## Project Roadmap

This is the single milestone sequence for the project. Detailed completion gates, estimates, dependencies, and confidence risks are maintained in [the implementation plan](docs/IMPLEMENTATION_PLAN.md).

### Current progression

- **Completed:** M1–M16, all five station certifications (M18–M22), M23.3 Android 16 / API 36 readiness, and M23.7 adaptive launcher/store polish.
- **Active:** M23 Play-testing readiness, with 2 of 7 reviewable sub-milestones complete.
- **Active focus:** M23.2 UGC safety and moderation. Administrator authorization is recorded and the [native implementation](docs/m23-ugc-safety-validation.md) is test-green; completion is waiting only for confirmation that the single harmless report reached the moderator inbox and reconciliation of that server response.
- **Remaining release gates:** M23.1 version-code eligibility and Play-delivered validation after its [protected current-head build](docs/m23-release-candidate-audit.md), M23.2 report-receipt reconciliation, M23.4 Play declarations, M23.5 rights evidence, M23.6 Play delivery/update and pre-launch coverage, then the explicitly authorized M24 Alpha publication.
- **Deferred:** M17 Private Messages remains excluded from the shipping build until the reproduced server-side delivery failure is repaired and verified.

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
| M13 Independent Accounts UX | L | 4–8 hours | ✅ Complete ([`9ef1f1c`](https://github.com/codeframe78/24Seven.FM-Player/commit/9ef1f1c)) | Five separately visible account states with pairwise session/logout/expiration isolation |
| M14 Local personalization | M | 2–4 hours | ✅ Complete ([`81c2c4e`](https://github.com/codeframe78/24Seven.FM-Player/commit/81c2c4e)) | Persist default/last station and clearly distinguish local preferences from station-owned data |
| M15 Request history and membership | L | 4–8 hours | ✅ Complete ([`b19d5fe`](https://github.com/codeframe78/24Seven.FM-Player/commit/b19d5fe)) | Memory-only SST last-ten history, cooldown/readiness, and explicit membership with conservative unknown states |
| M16 Secondary content access | M | 2–4 hours | ✅ Complete ([`90a7f98`](https://github.com/codeframe78/24Seven.FM-Player/commit/90a7f98)) | Allowlisted same-station HTTPS Custom Tabs for selected public modules; the original Death.FM TLS boundary was safely deferred and later reverified in M21 |
| M17 Private Messages | L provisional | 4–8 hours after server repair | 🧊 Deferred | Inbox and Sent Box discovery succeeded, but New Message selection remains suspect and a profile-originated MorgHubby test was not delivered; the site owner has the reproduced result |

### Phase 3 — Station certification

These milestones harden and certify the shared app against each station; they do not create five forks. Station-specific behavior remains behind capability flags and repository contracts.

| Milestone | Size | Estimate | Status | Certification emphasis |
| --- | --- | --- | --- | --- |
| M18 StreamingSoundtracks.com | S | 2–4 hours | ✅ Complete ([evidence](docs/m18-sst-certification.md)) | VIP/non-admin behavior, 30-row Queue, request messages, Favorites, chat, and authenticated workflows |
| M19 1980s.FM | M | 4–7 hours | ✅ Complete ([evidence](docs/m19-1980s-certification.md)) | Independent sign-in/restore/logout, authenticated Favorites/Chat/requests, station rules, metadata, Queue, and fallback |
| M20 Adagio.FM | M | 4–7 hours | ✅ Complete ([evidence](docs/m20-adagio-certification.md)) | Classical metadata presentation, independent sign-in/restore/logout, authenticated Favorites/Chat/requests, Queue, and fallback |
| M21 Death.FM | L | 6–10 hours | ✅ Complete ([evidence](docs/m21-death-certification.md)) | Compact Queue feed, sparse metadata/artwork, independent sign-in/restore/logout, authenticated Favorites/Chat/requests, RIP boundaries, and fallback |
| M22 Entranced.FM | M | 4–7 hours | ✅ Complete ([evidence](docs/m22-entranced-certification.md)) | Independent sign-in/restore/logout, authenticated Favorites/Chat/requests, extended Queue, fallback, and legacy ICY punctuation hardening |

Each station gate covers playback and fallback, metadata/artwork, Queue/history, authentication, chat, Favorites, requests, membership differences, Private Messages when available, physical-device smoke testing, documentation, tests, and remote publication. Unblocked certification work may proceed while M17 is deferred, but final station completion requires every in-scope capability to pass or an explicit scope decision.

### Phase 4 — Distribution

| Milestone | Size | Estimate | Status | Outcome |
| --- | --- | --- | --- | --- |
| M23 Play-testing readiness program | XL, split below | 3–6 focused days plus owner/external input | 🚧 In progress | Preserve the completed distribution preparation while closing release-artifact, UGC, API 36, review, rights, device, and launcher-quality gates as separate reviewable milestones |
| M24 Alpha publication | M | 1–3 hours after Console setup | ⏳ Planned | Verify the signed Play bundle and publish the authorized internal/closed test release |

#### M23 reviewable sub-milestones

| Milestone | Size | Codex estimate | Status | Required outcome |
| --- | --- | --- | --- | --- |
| M23.1 Current-head signed release candidate | M | 2–4 hours | 🚧 Protected Linux build and signer verification green; Play pending ([evidence](docs/m23-release-candidate-audit.md)) | Commit `2086ab9` produced a signed AAB/APK from the authenticated recovery package entirely through the memory-backed Linux path; exact upload-certificate identity, release audit, dependencies/notices, backup exclusions, and 16 KB packaging pass. Confirm version-code availability and validate Play delivery/update |
| M23.2 UGC safety and moderation | L, split into research and implementation | 6–12 hours after station input | 🚧 Implementation green; receipt pending ([evidence](docs/m23-ugc-safety-validation.md)) | Authorized native age/Terms/reveal gates, separate report and block actions, local block management, privacy boundaries, and duplicate-safe indeterminate delivery are implemented; administrator receipt confirmation remains |
| M23.3 Android 16 / API 36 readiness | M | 2–4 hours | ✅ Complete ([evidence](docs/m23-api36-readiness.md)) | Targets API 36; 117 unit tests, lint/release bundle, 27/27 API 36 connected tests, and cold-launch/two-Back visual inspection pass |
| M23.4 Play review declarations | M | 2–4 hours plus owner input | 🚧 Local packet and video rehearsal prepared ([evidence](docs/m23-play-declaration-packet.md)) | Accurate listing, copy-ready foreground-service/reviewer instructions, and a frame-inspected notification video rehearsal are prepared; reusable reviewer credentials, final signed-candidate video/link, content-rating answers, station retention, and Console submission remain owner-controlled |
| M23.5 Brand and content-rights evidence | S | 1–2 hours plus external confirmation | 🚧 Request packet prepared ([owner response packet](docs/m23-owner-response-packet.md)) | Copy-ready authorization scope and private/sanitized evidence handling are defined for app/station names, logos, artwork/metadata, authorized stream access, screenshots, and Google Play testing/distribution; authorized written confirmation remains |
| M23.6 Release device matrix and pre-launch report | M | 3–6 hours | 🚧 Accessibility/network/performance/device checkpoints green ([evidence](docs/m23-device-compatibility.md)) | Emulator coverage passes across minimum API, API 36, 16 KB, Fold, and Tablet states; large-text reflow, one-shot network restoration, 1,500-track Favorites performance, and physical Razr hinge testing pass. Play delivery/update and Play pre-launch results remain |
| M23.7 Adaptive launcher/store polish | S | 1–2 hours | ✅ Complete ([evidence](docs/m23-launcher-polish.md)) | Preserves the established logo in density-specific legacy and masked adaptive icons, adds an Android 13 monochrome layer, and revalidates the existing store assets |

M23 is active on this branch. The Google Play developer account and app record are established, Play App Signing is selected, the no-ads/non-government/no-financial/no-health declarations are saved, the public [privacy notice](https://codeframe78.github.io/24Seven.FM-Player/) is live and recorded in Console, and the public support email plus Music & Audio category are saved. The owner selected an 18+ target audience consistent with the adult legacy-station community and existing freeform chat; Console entry remains locked behind reviewer-access setup. Play-ready graphics and device screenshots are prepared; compact/medium/expanded layouts, 2× text and enlarged-display reflow, one-shot playback recovery after validated network restoration, genuine emulator fold/tablet states, the physical Razr hinge cycle, notification-to-app navigation, multi-field library search, play-state sorting, full-list Favorites responsiveness, 16 KB runtime, and API 36 targeting are validated. Android CI uses the official Node 24-compatible checkout, Java, and Gradle action generations while retaining the open-source basic Gradle cache provider. Protected upload signing and recovery are green, and the current product candidate has now been rebuilt on Ubuntu with its exact upload-certificate identity verified; version-code eligibility and Play delivery remain. M23.2 now has administrator authorization plus native age/Terms/reveal gates, separate report-content/report-user/block-user actions, and station-scoped block management; its only open implementation evidence is reconciling the single harmless report with the administrator inbox. Foreground-service evidence, explicit brand/content rights, reviewer credentials, final audience/content/Data Safety declarations, tester setup, Play delivery/pre-launch results, and explicit publication authorization also remain gates. M17 Private Messages is not shipped and does not block Alpha testing.

The app is fully native and uses immutable Compose UI state, repository boundaries, and station capability flags. It includes play, pause, stop, live metadata and artwork, a persistent mini-player, signed-in favorite-track browsing/filtering, capability-aware screens, native loading/error/empty states, and Android Keystore-backed account sessions. Remote data stays bounded to the documented station interfaces and their approved refresh rules.

## Screenshots

Most captures are from the physical Razr and use live station data, so track and chat content will naturally change over time. The M15 request-activity capture intentionally shows the safe signed-out state after a fresh debug install. The final accessibility and network-recovery pairs are privacy-safe API 35 emulator evidence. The Player and Queue captures show the M11 Alpha shell; Chat and Requests retain their already-working native M8–M10 content presentation. The M16 capture shows the original trusted browser directory, while M18–M22 include station-certification evidence.

<table>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/player.png" alt="Adaptive native player with 24Seven.FM artwork, station carousel, and live controls" width="300"><br><strong>Adaptive Player</strong><br><sub>Live artwork, track metadata, station carousel, and playback controls.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/queue.png" alt="Native upcoming queue with artwork and the persistent M11 mini-player" width="300"><br><strong>Queue</strong><br><sub>Upcoming tracks alongside the persistent mini-player.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/chat.png" alt="Native station chat" width="300"><br><strong>Live Chat</strong><br><sub>Native station-scoped community conversation.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/requests.png" alt="Native station library search" width="300"><br><strong>Song Requests</strong><br><sub>Multi-field station-library search and request workflow.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/favorites.png" alt="Native favorite-track list with accessible green available and red unavailable stoplights" width="300"><br><strong>Favorite Tracks</strong><br><sub>Accessible request availability across large station-owned lists.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/vip-request-message-success.png" alt="Verified native VIP request with requester attribution and exact message in Queue" width="300"><br><strong>VIP Request Message</strong><br><sub>Verified requester attribution and message shown in Queue.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/accounts.png" alt="Native station accounts dashboard explaining independent protected sessions and showing the selected station account" width="300"><br><strong>Independent Accounts</strong><br><sub>Protected, separately visible sessions for each station.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/preferences.png" alt="Native device preferences card for resuming the last station or fixing the current station at startup" width="300"><br><strong>Startup Preferences</strong><br><sub>Device-local resume and default-station behavior.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m15-request-activity.png" alt="Native SST request activity card in its explicit signed-out state" width="300"><br><strong>Request Activity</strong><br><sub>Request history, cooldown, readiness, and membership state.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m16-secondary-content.png" alt="Native SST secondary-content directory with same-station browser cards and persistent player navigation" width="300"><br><strong>Trusted Station Content</strong><br><sub>Allowlisted same-station pages opened through a secure browser.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m18-sst-certification.png" alt="Certified SST Player state with live title, artist, album artwork, station carousel, and all five navigation destinations" width="300"><br><strong>M18 SST Certification</strong><br><sub>Certified live Player state and complete native navigation.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m19-1980s-authenticated.png" alt="Independent 1980s.FM account signed in as MorG while other visible station accounts remain signed out" width="300"><br><strong>M19 1980s.FM Certification</strong><br><sub>Verified independent 1980s.FM authentication.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m20-adagio-authenticated.png" alt="Independent Adagio.FM account signed in as MorG while other visible station accounts remain signed out" width="300"><br><strong>M20 Adagio.FM Certification</strong><br><sub>Verified independent Adagio.FM authentication.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m21-death-certification.png" alt="Death.FM Player with live title, artist, station artwork, station carousel, and playback controls" width="300"><br><strong>M21 Death.FM Player</strong><br><sub>Certified sparse-metadata playback and station navigation.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m21-death-rip-pages.png" alt="Death.FM trusted station-page directory including RIP membership" width="300"><br><strong>Death.FM RIP Pages</strong><br><sub>Trusted station pages and RIP membership access.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m21-death-authenticated.png" alt="Independent Death.FM account signed in as Morgue while other visible station accounts remain signed out" width="300"><br><strong>M21 Death.FM Authentication</strong><br><sub>Verified station-isolated Death.FM account session.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m22-entranced-certification.png" alt="Entranced.FM Player with live title, artist, same-station artwork, selected station card, and playback controls" width="300"><br><strong>M22 Entranced.FM Player</strong><br><sub>Certified artwork, metadata, playback, and station selection.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m22-entranced-authenticated.png" alt="Independent Entranced.FM account signed in as MorG while 1980s.FM, Adagio.FM, and Death.FM remain signed out" width="300"><br><strong>M22 Entranced.FM Authentication</strong><br><sub>Verified station-isolated Entranced.FM account session.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/play-store-assets/screenshots/phone-player-live-playing.png" alt="Compact physical Razr Player with live Entranced.FM artwork, metadata, playback controls, and bottom navigation" width="300"><br><strong>M23 Compact Layout</strong><br><sub>Physical Razr evidence with live playback and bottom navigation.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/play-store-assets/screenshots/tablet-landscape-player.png" alt="Expanded landscape Player with navigation rail, two-pane now-playing content, controls, and all five station cards" width="300"><br><strong>M23 Expanded Layout</strong><br><sub>Landscape two-pane Player with navigation rail and station carousel.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m23-large-text-player.png" alt="Compact native Player at two-times Android text scale with reachable playback controls, growing station cards, and unclipped icon navigation" width="300"><br><strong>M23 Large-Text Player</strong><br><sub>2× text and enlarged-display compact layout with reachable controls and station details.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m23-large-text-account.png" alt="Native account screen at two-times Android text scale with the station identity stacked above the signed-out status" width="300"><br><strong>M23 Large-Text Account</strong><br><sub>Account identity and status reflow instead of competing for one narrow row.</sub></td>
  </tr>
  <tr>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m23-network-offline.png" alt="Compact Player showing that playback is waiting for network and will resume automatically while the pause action remains available" width="300"><br><strong>M23 Offline State</strong><br><sub>Clear waiting status after both live-stream variants fail without connectivity.</sub></td>
    <td width="50%" align="center" valign="top"><img src="docs/screenshots/m23-network-recovered-tablet.png" alt="Expanded Tablet Player showing Playing live after connectivity restoration without another play action" width="300"><br><strong>M23 Automatic Recovery</strong><br><sub>Expanded Player returned to live playback after validated connectivity was restored.</sub></td>
  </tr>
</table>

Audio stream addresses come from station-provided playlists and remain subject to device verification. Remote interfaces are added only after source verification and permission review. See the milestone research and validation documents under [docs](docs) for authorization, protocol evidence, limits, and device results.

M17 tracks the native Private Messages experience, which remains deferred until the website's underlying server issues and production behavior are settled. See [docs/future-scope.md](docs/future-scope.md).

Alpha testers and distributors should read [the privacy notice](PRIVACY.md), [Alpha testing guide](docs/alpha-testing.md), [release notes](docs/releases/0.1.0-alpha01.md), [Play Console checklist](docs/play-console-checklist.md), and [M23 signing handoff](docs/m23-alpha-readiness.md). Development debug APKs are not intended for external distribution.

## Building

Open the repository in a current Android Studio release with JDK 17. The project targets and compiles against Android 16 (API 36), and is currently validated on the primary Motorola Razr 2023 running Android 16. It supports Android 8.0 (API 26) and newer.

From PowerShell, validate the project with:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease
```

For an Ubuntu Codex CLI and Android Studio workstation, install the pinned
project toolchain, both API 35/API 36 emulators, and persistent `ANDROID_HOME`
configuration with:

```bash
bash scripts/bootstrap-ubuntu.sh --accept-licenses
```

The script also installs the ChatGPT Codex CLI. Authenticate afterward with
`codex login`, or use `codex login --device-auth` on a headless host. See the
[Ubuntu development setup](docs/ubuntu-cli-setup.md) for installed components,
options, security boundaries, and verification commands.

If the repository is inside a OneDrive-synced directory and Gradle stalls on file operations, direct app build outputs to a local directory before running Gradle:

```powershell
$env:TWENTYFOURSEVEN_ANDROID_BUILD_DIR="$env:TEMP\24seven-android-build"
```

See [CONTRIBUTING.md](CONTRIBUTING.md) before contributing.

For migration to another Windows development machine, follow [docs/CODEX_HANDOFF.md](docs/CODEX_HANDOFF.md) and run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-windows.ps1` after cloning.
