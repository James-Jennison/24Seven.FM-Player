# Windows development handoff

## Resume status — July 15, 2026

- Current milestone: M23 Alpha Test Distribution Readiness. M18–M22 and all five station gates are complete; M17 remains deferred for server repair.
- Last completed milestone: M22 Entranced.FM certification in `docs/m22-entranced-certification.md`, with focused implementation commit `ec5fb20`.
- Latest successful validation: debug/release compile, all 110 unit tests, lint, Windows validation, standalone wireless debug install, and 21/21 Android 16 Razr instrumentation tests after the M22 final station gate (131 tests total); live Entranced playback also reconfirmed Media3 `PLAYING`, artwork/metadata, AAC/128 kbps, corrected punctuation boundaries, and restored device settings.
- Architecture: one native Compose app module; immutable state/actions; station-scoped repository contracts; one Media3 service-owned player/session; Android Keystore-backed per-station sessions.
- Decisions: queued and recently played tracks share visible red `Track Recently Played`; reasons remain distinct internally. Available tracks use green `Request Now`. Other restrictions retain accurate separate labels. Revalidation must fail closed before mutation.
- Known blockers: Queue rows lack stable track IDs; M17 Private Messages remains deferred for server fixes. M23 upload signing, off-PC recovery, public privacy hosting, Play App Signing, objective declarations, public support contact, and Music & Audio classification are complete. The owner selected an 18+ audience consistent with the adult station community and existing chat; Console still requires least-privileged reviewer credentials before that declaration can be saved.
- Roadmap model: M13–M17 shared features, M18–M22 individual station certification, and M23–M24 distribution/publication. Certification milestones harden the shared app and must not create station-specific forks.
- Next concrete task: supply reusable least-privileged Play reviewer credentials through Console, save the selected 18+ target audience, then complete content rating/Data Safety and prepare the internal-test release.
- Likely next surfaces: Play Console Sign in details, Target audience, Content rating, Data Safety, Internal testing, and the existing M23 release/privacy/checklist documentation.
- Branch: `agent/initial-android-scaffold`.
- Latest implementation commit: `ec5fb20`.
- Latest production implementation/evidence checkpoints are `ec5fb20` and the pending M22 certification documentation commit. No stream URL or trust-policy expansion was introduced by M22.
- Required planning documents: `CURRENT_STATE_AUDIT.md`, `NETWORK_FEATURE_MATRIX.md`, `ENDPOINT_INVENTORY.md`, `AUTHENTICATION_MATRIX.md`, and `IMPLEMENTATION_PLAN.md`.

## Mission and repository

Complete **24Seven.FM Player**, an unofficial, fully native Android client for the 24seven.FM radio network. The app uses Kotlin, Jetpack Compose, MVVM, and Jetpack Media3. It must not use a WebView.

- Repository: `https://github.com/codeframe78/24Seven.FM-Player`
- Working branch: `agent/initial-android-scaffold`
- Draft pull request: `https://github.com/codeframe78/24Seven.FM-Player/pull/1`

Clone the current handoff branch on the new PC:

```powershell
git clone https://github.com/codeframe78/24Seven.FM-Player.git
Set-Location '24Seven.FM-Player'
git switch agent/initial-android-scaffold
git status
```

Read `AGENTS.md`, `README.md`, `CONTRIBUTING.md`, `docs/architecture.md`, `docs/m2-validation.md`, and `docs/m3-validation.md` before editing. Preserve the existing station-first domain model and dependency boundaries.

## Required Windows environment

- A current Android Studio release
- JDK 17
- Android SDK Platform 35
- Android SDK Platform 36
- Android SDK Build Tools 36.1.0
- Android SDK Platform Tools
- Optional Google APIs x86_64 Android Emulator images for API 35 and API 36

The app compiles against API 36 because the selected AndroidX Activity and Media3 versions require it. It targets API 35 and is currently validated on the primary Motorola Razr 2023 running Android 16. Minimum SDK is API 26.

Install SDK packages from Android Studio's SDK Manager or with `sdkmanager`:

```powershell
sdkmanager "platforms;android-35" "platforms;android-36" "build-tools;36.1.0" "platform-tools" "system-images;android-35;google_apis;x86_64" "system-images;android-36;google_apis;x86_64"
```

Do not commit `local.properties`. If Android Studio does not discover the SDK automatically, set `ANDROID_HOME` for the current PowerShell session:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
```

The complete Gradle wrapper is committed. Do not install a separate Gradle distribution. Validate the new PC from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-windows.ps1
```

The script checks JDK and SDK prerequisites, redirects build output to `%TEMP%`, and runs `test lint assembleDebug`. The equivalent manual command is documented in `README.md`.

## Current implementation

M1 is complete and CI builds the project with JDK 17 and the committed Gradle wrapper. M2 currently includes:

- five-station catalog with station-provided primary and source-fallback URLs;
- immutable playback state behind a domain `PlaybackController` contract;
- Activity-to-service Media3 controller connection;
- one service-owned `ExoPlayer` and `MediaSession`;
- play, pause, stop, and atomic station switching;
- one-step fallback from the primary relay to the source stream;
- Android 13+ notification permission request;
- foreground/background playback with system media pause and resume;
- station metadata in the media notification;
- notification controls that do not expose the internal fallback playlist.

M4 now includes:

- verified ICY titles on all five stations and both relay types;
- immutable station-scoped now-playing state behind a domain repository contract;
- raw title display without heuristic artist/album/composer parsing;
- matching live titles in Compose and Android MediaSession metadata;
- protocol-verified AAC and advertised 128 kbps quality labels.

M5 now includes:

- immutable Player, Chat, Queue, and More destination state;
- working native destinations with capability-aware unavailable states;
- a persistent mini-player on secondary destinations;
- bottom navigation below 600 dp and a navigation rail on wider layouts;
- unit and Compose device coverage for navigation and adaptive branch selection.

M6 now includes:

- a station-scoped `QueueRepository` domain contract;
- immutable unavailable, loading, ready, and error states;
- native upcoming-queue and recently-played rendering with refresh actions;
- explicit title and artist fields plus station-hosted cover artwork;
- one request per selected station with a shared 60-second automatic/manual limit;
- polling only while Queue is selected and collected in the foreground;
- unit, Compose, and API 35 device coverage;
- sanitized source research and authorization in `docs/m6-queue-research.md`.

Live M6 data is authorized by a 24seven.FM administrator for this unofficial, non-commercial Android app across all five stations. Access is unauthenticated, requester display and cover artwork are permitted, and polling may occur no more than once every 60 seconds. Preserve those limits. Do not add cookies or broaden the response parser to community, birthday, rating, or unrelated HTML fields.

The latest successful build validation ran unit tests for debug and release, Android lint, `assembleDebug`, instrumentation APK assembly, and API 35 connected tests. M3 through M6 device validation is complete. See `docs/m1-validation.md`, `docs/m2-validation.md`, `docs/m3-validation.md`, `docs/m4-metadata-research.md`, `docs/m5-validation.md`, and `docs/m6-validation.md` for exact evidence.

## Physical Razr setup

The primary device is a Motorola Razr 2023:

- Android 16 (app target remains API 35)
- device model reported by ADB: `motorola_razr_2023`
- physical display: 1080 x 2640 at 420 dpi

ADB trust keys are specific to each development PC. The new PC must pair again; do not copy old ADB keys. On the phone:

1. Enable Developer options.
2. Open **Settings > System > Developer options > Wireless debugging**.
3. Choose **Pair device with pairing code**.
4. Keep the pairing screen open while running `adb pair <phone-ip>:<pairing-port>` on the new PC.
5. Enter the six-digit code shown by the phone.
6. Return to the main Wireless debugging screen and run `adb connect <phone-ip>:<debug-port>`.
7. Confirm the connection with `adb devices -l`.

The pairing port and debugging port are different and can change. Never commit the phone's LAN/Tailscale address, pairing code, device serial, or screenshots of the Wireless debugging screen. If both PCs and the phone use Tailscale, the phone's current Tailscale address can be used explicitly with `adb pair` and `adb connect`; mDNS discovery is not required.

## Verified device behavior

The Razr has verified:

- primary-stream playback for all five stations;
- two consecutive switching cycles covering all five stations while playback remained active;
- background continuation with a foreground media service;
- notification permission and visible media notification;
- system media pause/resume;
- in-app stop;
- correct session metadata and audio focus;
- no user-facing Next action for the internal fallback item.

Primary-to-source fallback was also verified under a controlled primary-only network failure on a local API 35 emulator. The test left both verified production URLs unchanged and removed its temporary emulator firewall rule afterward.

Before testing, use `adb devices -l` and pass `-s <device>` to ADB commands when an emulator is also running.

## M3 through M6 completion

M3 background-playback hardening is complete. It verified foreground-to-background playback, lock and idle behavior, task removal, notification continuity, system and real Bluetooth media commands, transient and permanent audio-focus policy, real Bluetooth route-disconnect pausing, protected noisy-output broadcast handling, and automated service stop/reconnect behavior. The transient-focus policy automatically resumes after focus returns; permanent focus loss remains paused until the user explicitly resumes playback.

No physical wired or USB-C accessory was available. Do not claim that physical test occurred. The system headset-hook command, Android's protected noisy-output broadcast, and real Bluetooth disconnect cover the relevant application and Media3 paths; physical wired coverage is non-blocking and can be added later.

M4 Now Playing is complete. A continuous physical-device run verified that a changed raw title updated both Compose and the application MediaSession while playback remained healthy. A controlled API 35 emulator run verified that the source fallback item continued publishing a non-empty ICY title after the expected primary failure. Metadata fields and artwork that are not present in the verified ICY source remain intentionally unavailable, and playback does not depend on metadata.

M5 Native Navigation is complete. Player, Chat, Queue, and More are real native destinations, secondary destinations retain a persistent mini-player, and wider layouts select a navigation rail. Chat and Queue intentionally show capability-aware unavailable states until their later protocol milestones; M5 does not guess or implement remote endpoints.

M6 Queue and History is complete. The administrator-authorized public interfaces supply live data for all five stations without authentication. The app makes one request only while Queue is selected, enforces the authorized 60-second minimum interval across automatic and manual refresh, preserves explicit track fields, and displays only station-hosted cover artwork. Extended adapters for StreamingSoundtracks.com, 1980s.FM, Adagio.FM, and Entranced.FM show up to 30 upcoming and 30 played tracks. Death.FM retains the compact feed (up to 10 per list) because its extended interface was unreliable during verification. See `docs/m6-queue-research.md` and `docs/m6-validation.md`.

M7 Authentication is complete. A station-scoped repository contract and immutable states drive a fully native
challenge, sign-in, restoration, and sign-out flow for the verified matching public interfaces across all five
stations. Passwords and case-sensitive alphanumeric security-code answers remain transient. Cookies and display
identity are encrypted with Android Keystore AES-GCM, normalized to the exact station domain, restored as
HTTPS-only, checked online when reachable, and cleared on anonymous restoration or sign-out. Network failure
preserves the protected cached identity and never blocks public playback.

On July 13, 2026, a station administrator authorized the public-facing login and session interfaces, user-entered
credential submission, Android-protected session retention, research, and least-privileged testing. A native
least-privileged StreamingSoundtracks.com sign-in, real process-restart restoration, sign-out, and signed-out
restart were verified on the Motorola Razr 2023 running API 35. Unit tests, lint, debug and release compilation,
Windows validation, and all six connected tests pass. Natural server expiry was not waited out; online restoration
uses the strict signed-in classifier to clear an expired anonymous session. Accounts remain station-specific until
shared behavior is verified. Chat and request submission remain outside the authorization. See
`docs/m7-auth-research.md` and `docs/m7-validation.md`.

M8 Chat is complete. On July 13, 2026, a station administrator authorized authenticated chat reads,
participant metadata display, and least-privileged harmless test-message posting across all five stations, with a
documented production rate and no persisted history. All five public message views returned the same 15-row,
ISO-8859-1 HTML shape and reload every 30 seconds. The native repository uses that same minimum scheduled/manual
read interval only while Chat is selected, retains at most 50 parsed messages in memory, renders remote content as
plain text, and stops collection when the destination changes.

Protected-session posting is implemented with exact same-origin form validation, a 255-character limit, and
ISO-8859-1 encodability checks. Station-issued posting material stays transient in the data layer and is never
persisted or logged. One clearly identified harmless post succeeded with the least-privileged
StreamingSoundtracks.com account. The native app then completed a protected-session post on the Razr, confirmed
the message in the public feed, and restored both the signed-in composer and live chat after a real force-stop and
relaunch. Debug and release unit tests, lint, assembly, and all seven API 35 connected tests pass. See
`docs/m8-chat-research.md` and `docs/m8-validation.md`.

M9 Native Requests is complete. The administrator authorized
public catalog browsing/searching and explicit user-initiated requests across all five stations. The shared native
flow searches by title, album, artist, or genre; opens station album listings; preserves server-derived per-track
availability; requires a protected station session and visible confirmation; sends at most one request; and never
retries. Search and album reads are user initiated with no polling. The single approved Razr submission requested
`Kung` from *Bulletproof Monk* on StreamingSoundtracks.com and was verified at position 22 in the public queue. The
station accepted it before the confirmation response failed; the app now treats that outcome as indeterminate,
directs the listener to Queue, and suppresses immediate resubmission. See `docs/m9-request-research.md` and
`docs/m9-validation.md`.

See `docs/m4-metadata-research.md` for per-relay ICY headers, field constraints, implementation evidence, and device results.

M17 includes a native authenticated Private Messages inbox/read/compose/reply/send experience. Authorization for least-privileged research and explicit user-initiated use is recorded, but implementation remains deferred until the server issues, production limits, retention, deletion, attachment, and station-account behavior are settled. See `docs/future-scope.md`.

M10 request attribution and optional request messages is complete. The public extended Queue/History interface exposes
an explicit requester profile link and, when supplied, a separate italic request message. Both are parsed into bounded
domain fields and rendered independently of track metadata. Death.FM's compact feed does not expose these fields.
StreamingSoundtracks.com's authenticated post-request form uses a separate 80-character `msg` POST only after the
song request has been reported accepted. The native dialog implements that two-step contract, keeps the message
transient, never retries the song mutation, and gates the field behind a station capability so the other four stations
are not assumed compatible.

Live investigation established three legacy requirements: the browser submits `msg`, `send`, and `remLen`; the request
response redirects through the public `www` HTTP alias; and the message form's server-generated record ID is distinct
from the song ID. The adapter parses and strictly validates the form action, matching album and numeric record ID,
canonicalizes only the verified SST alias to `https://streamingsoundtracks.com`, posts with the actual response referer,
and stops reading once the complete form or saved-message acknowledgement has arrived. If the accepted response is
unreadable, the adapter does not guess an ID, post a message, or retry the song. It also requires an explicit request-
success phrase and presents station acknowledgements as pending until Queue confirms the result.

Final native validation used the VIP account and the least-played suggestion flow. `Clipped Ears` by Nicholas Pike
entered the public Queue with `Requested by MorG` and the exact message `M10-VIP-least-2-20260714`; the native Queue
rendered the same requester and message separately on the API 35 Razr. Unit, lint, release, and all 10 Razr
instrumentation tests pass. See `docs/m10-request-attribution-research.md` and `docs/m10-validation.md`.

An API 35 instrumentation test connects through the real `MediaSessionService`, checks that fallback navigation remains hidden, stops the running service, and reconnects after recreation. Run it against an explicit emulator serial with `ANDROID_SERIAL=<emulator>` and `./gradlew connectedDebugAndroidTest` when both an emulator and physical device are connected.

The current Windows SDK includes compile platforms 35, 36, and 36.1 plus Google APIs x86_64 emulator images for API 35 and API 36. A temporary API-35 audio-focus helper compiled successfully after Platform 35 was installed; it was uninstalled and deleted after testing.

M11 Adaptive Alpha UI is complete and published in `4735f13`. It preserves the M1–M10 architecture and
behavior while adding the selected 24Seven.FM launcher/fallback artwork, centralized system light/dark and per-station
palettes, a responsive compact/two-pane Player, wrapped previous/next station controls, an unobstructed five-station
carousel, an updated persistent mini-player, accessible connection-state copy, multi-size previews, and a double-Back
exit confirmation that stops playback only after explicit confirmation. Chat, Queue, account, and Requests retain their
existing contracts and routes. Unit tests, lint, debug assembly, and all 11 Razr instrumentation tests pass. See
`docs/m11-ui-preservation-plan.md` and `docs/m11-validation.md`. The original no-publish restriction was later
superseded by explicit milestone publishing authorization.

Early M23 Alpha distribution preparation is preserved and published at version `0.1.0-alpha01` / version code 2. Privacy disclosure,
native privacy-notice access, tester instructions, release notes, permission review, Play upload-signing guardrails,
unsigned release APK/AAB builds, debug-signature verification, an in-place debug upgrade from version code 1 to 2,
and the latest 21 Razr tests are complete. Google Play internal/closed testing with Play App Signing is selected; Google
approved the developer account on July 14, 2026, and app/signing setup is now the active M23 work. Never distribute the machine-local debug APK, and
never commit signing files, passwords, aliases, or local signing-property paths. See `PRIVACY.md`,
`docs/alpha-testing.md`, `docs/play-console-checklist.md`, `docs/releases/0.1.0-alpha01.md`, and
`docs/m23-alpha-readiness.md`. M15–M22 are complete, so these artifacts are now being refreshed and revalidated.

The final upload identity is protected by the current-user DPAPI envelope and an independently verified PBKDF2/AES-256-GCM package on the owner-controlled off-PC volume. GitHub Pages publishes the canonical notice at `https://codeframe78.github.io/24Seven.FM-Player/`; the URL, public support email, Music & Audio category, and four evidence-backed objective declarations are saved in Play Console but have not been sent for review. The owner selected an 18+ target audience; its Console form is blocked by the required reviewer-access declaration. Do not store station reviewer credentials in Git or conversation history, and do not select child audience bands without a separate Families-compliance milestone.

The published Alpha branch also adds native authenticated Favorites browsing for all five stations. The
adapter discovers the signed-in member's numeric Favorites-list identifier from each station's own authenticated
Favorites page; no username or member ID is hard-coded. Lists are loaded only when the user opens Favorites or taps
Refresh, remain memory-only, and are cleared from the interface on sign-out. Requestable rows expose a green stoplight
and `Request Now`, while currently queued and recently played rows expose a red stoplight and `Track Recently Played`;
other restrictions retain accurate separate labels. Eligible selections reuse the existing explicit request-review
dialog and one-shot request path. SST live validation loaded all 1,500 tracks for the signed-in account on the API 35
Razr; no request was submitted during this validation. Unit tests, affected-module lint, debug assembly, and all 13 API
35 emulator instrumentation tests pass. See `README.md`, `PRIVACY.md`, `docs/alpha-testing.md`, and
`docs/screenshots/favorites.png`.

M15 Request history, cooldown, and membership is complete in `b19d5fe`. The station-scoped
`ListenerActivityRepository` loads only while More is selected or the listener explicitly refreshes, keeps all results
in memory, and clears a station's state on sign-out. Representative authenticated SST evidence supplies the exact
last-ten request table, a same-origin VIP request timer, and a profile-scoped VIP badge separately from administrator
rank. The parser bounds rows and text, rejects cross-origin/unrecognized discovery links, and reports missing evidence
as unknown. Only SST is enabled; completed M19–M22 certification did not expose a reliable equivalent for 1980s.FM,
Adagio.FM, Death.FM, or Entranced.FM. No mutation or polling was
introduced. Unit tests, lint, debug install, and all 19 wired Android 16 Razr instrumentation tests pass. The physical
fresh-install screenshot shows the explicit signed-out card; a fresh standard-tier production refresh requires a
user-entered CAPTCHA and is recorded as an M18 interaction limit rather than being guessed. See `docs/m15-request-activity-research.md`, `docs/m15-validation.md`, and
`docs/screenshots/m15-request-activity.png`.

M16 Secondary community/content access is complete in `90a7f98`. Immutable station-page models and a
`supportsSecondaryContent` capability supply an accessible More-screen directory below existing native account and
request tools. SST, 1980s.FM, Adagio.FM, and Entranced.FM expose exact observed HTTPS home, forums, members, stats,
Top 100, contact, and membership routes, with verified SST/1980s extras. `StationPageTrustPolicy` accepts only an
exact catalog member on the selected station's HTTPS origin and rejects unlisted, cross-origin, cleartext,
credential-bearing, fragment-bearing, malformed, and nonstandard-port targets. Android Custom Tabs use independent
browser sessions; no WebView, polling, parsing, or mutation was added. Death.FM was explicitly unavailable at M16
because its configured HTTPS origin failed modern TLS; M21 later reverified and enabled its exact HTTPS routes under
the same trust policy. Unit tests, lint, debug install, all 21 wired Android 16 Razr
instrumentation tests, and a physical Chrome Custom Tab/Back round trip pass. See
`docs/m16-secondary-content-research.md`, `docs/m16-validation.md`, and
`docs/screenshots/m16-secondary-content.png`.

M18 StreamingSoundtracks.com certification is complete in `docs/m18-sst-certification.md`. The gate reconciles the
existing least-privileged native sign-in/restore/logout, Chat, request, and Favorites evidence with the separate VIP
request-message and listener-activity evidence. A fresh wired Android 16 Razr run silently reached live SST playback
with a current title, artist, same-station cover, AAC/128 kbps, and all five navigation destinations; public Queue and
Chat loaded without error, Favorites retained its signed-out boundary, and no fatal exception occurred. Playback was
paused and the original media volume restored. The full unit suite, lint, debug install, and 21/21 instrumentation
tests pass. No production mutation, stream change, or station-specific fork was introduced. Native Private Messages
remain explicitly outside M18 under the M17 server-repair deferral. See
`docs/screenshots/m18-sst-certification.png`.

M19 1980s.FM certification is complete. The public/device evidence retains live primary/fallback playback,
structured metadata/artwork, extended Queue/History, Chat, requests, and trusted Games/Awards routes. The user then
entered a representative MorG login and CAPTCHA directly in the app. The independent 1980s session restored after a
forced process restart without Keystore errors; Entranced Favorites remained signed out until 1980s was selected;
authenticated Favorites loaded a valid empty list; Chat exposed its composer; and least-played browsing exposed one
green `Request Now` track without mutation. Explicit `Sign out of 1980s` cleared only that station and remained
cleared after another restart. Request messages and listener activity stay `Not verified`. See
`docs/m19-1980s-certification.md`, `docs/screenshots/m19-1980s-certification.png`, and
`docs/screenshots/m19-1980s-authenticated.png`.

M20 Adagio.FM certification is complete. The unchanged primary relay reached live playback on the physical Razr,
and both station-supplied relays delivered live audio bytes. A fresh classical ICY sample rendered the composer plus
complete work/ensemble title and same-station artwork without changing the raw metadata contract. The bounded public
Queue/Played feed returned HTTP 200, native Chat loaded, Favorites and request submission retained their signed-out
boundaries, a least-played suggestion returned album/track content without mutation, and the native alphanumeric
CAPTCHA challenge loaded. Forums opened at `adagio.fm` in a Chrome Custom Tab and Back returned. A focused contract
test prevents Adagio from inheriting SST-only request-message or listener-activity flags. A representative MorG
session signed in natively, restored after a forced process restart without Keystore errors, remained isolated from
the other stations, loaded authenticated empty Favorites, exposed the Chat composer and one green requestable
least-played track without mutation, and cleared through station-only logout across another restart. Request messages,
listener activity, and membership remain explicitly unverified. See `docs/m20-adagio-certification.md`,
`docs/screenshots/m20-adagio-certification.png`, and `docs/screenshots/m20-adagio-authenticated.png`.

M21 Death.FM certification is complete. The public/device checkpoint retains both unchanged live relays, sparse
metadata/artwork, the compact ten-row Queue/History contract, Chat, request browsing, exact HTTPS pages, and the
Death-specific RIP membership browser route. A representative Morgue session signed in natively, restored after a
forced process restart without Keystore errors, remained isolated from the other stations, loaded authenticated empty
Favorites, exposed the Chat composer and one green requestable least-played track without mutation, and cleared
through station-only logout across another restart. RIP membership remains separately trusted browser content;
request messages, listener activity, and native membership remain explicitly unverified. See
`docs/m21-death-certification.md`, `docs/screenshots/m21-death-certification.png`,
`docs/screenshots/m21-death-rip-pages.png`, and `docs/screenshots/m21-death-authenticated.png`.

M22 Entranced.FM certification is complete. Both unchanged station relays delivered live `audio/aacp` bytes at
128 kbps; the extended Queue/History, Chat, request browsing, artwork interface, exact seven trusted pages, and
station capability contract pass. A representative MorG session signed in natively, restored after a forced process
restart without Keystore errors, remained isolated from the other stations, loaded authenticated empty Favorites,
exposed the Chat composer and one green requestable least-played track without mutation, and cleared through
station-only logout across another restart. The playback boundary now maps only defined Windows-1252 C1 punctuation
from legacy ICY titles and preserves valid Unicode; focused tests and live installed playback show no replacement or
control glyph. Request messages, listener activity, and native membership remain explicitly unverified. See
`docs/m22-entranced-certification.md`, `docs/screenshots/m22-entranced-certification.png`, and
`docs/screenshots/m22-entranced-authenticated.png`.

## Station stream evidence

The exact URLs below came from station-provided PLS files. The trailing semicolon is part of each supplied URL and must not be removed without testing and documentation.

| Station | Primary relay | Source fallback |
| --- | --- | --- |
| StreamingSoundtracks.com | `http://hi5.streamingsoundtracks.com/;` | `http://hi.streamingsoundtracks.com/;` |
| 1980s.FM | `http://hi5.1980s.fm/;` | `http://hi.1980s.fm/;` |
| Adagio.FM | `http://hi5.adagio.fm/;` | `http://hi.adagio.fm/;` |
| Death.FM | `http://hi5.death.fm/;` | `http://hi.death.fm/;` |
| Entranced.FM | `http://hi5.entranced.fm/;` | `http://hi.entranced.fm/;` |

All ten relays advertise `audio/aacp` and 128 kbps through ICY headers. Cleartext is disabled globally and allowed only for the five station domains. Do not broaden the network security policy or guess HTTPS/API endpoints.

## Architectural invariants

- No WebView.
- Compose receives immutable state and emits actions upward.
- ViewModels depend on repository/controller interfaces, never Media3 or network implementations.
- ExoPlayer remains exclusively service-owned.
- Every station-specific operation accepts or derives a `StationId`.
- Unsupported station features use capability flags.
- Only one station may play at a time.
- Public audio playback must not depend on login, chat, or metadata availability.
- Never commit credentials, cookies, tokens, private endpoints, private network addresses, pairing codes, HAR files, screenshots containing device identifiers, SDK paths, APKs, or build output.

## Git workflow

Continue on `agent/initial-android-scaffold` while PR #1 remains the active draft. Before editing, fetch and confirm the branch has not advanced elsewhere:

```powershell
git fetch origin
git status
git log --oneline --decorate -5
```

Keep commits focused. Do not force-push. Run `git diff --check` and `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-windows.ps1` before publishing implementation changes. The bypass applies only to that PowerShell process and does not change the PC's saved execution policy.
