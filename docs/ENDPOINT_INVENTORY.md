# Endpoint inventory

Sanitized inventory updated August 11, 2026. No cookies, credentials, anti-spam answers, authentication headers, personal identifiers, or message IDs belong in this document.

All relative HTTPS paths below are resolved independently against one of these five station origins: `streamingsoundtracks.com`, `1980s.fm`, `adagio.fm`, `death.fm`, or `entranced.fm`. Requests validate the expected origin before sending station sessions.

| Station(s) | Purpose | Endpoint/protocol | Format | Authentication | Stability/refresh/cache | Error behavior | Authorization/status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| All five | Live audio | Station-provided `https://<station-domain>/live` | HTTPS-proxied Icecast AAC+ stream, 192 kbps | None | Continuous Media3 playback; reverse proxy handles `hi5` primary and `hi` backup failover | Playback error when the redundant proxy cannot serve audio | Administrator-directed redundant Icecast architecture; verified HTTPS 200 on August 10, 2026 |
| All five | ICY metadata | Audio response headers/metadata | ICY text | None | Event driven; no independent poll | Playback continues without metadata | Implemented/verified |
| All five | Current artwork | `/soap/FM24sevenJSON.php?action=GetCurrentlyPlaying` | Bounded JSON | None | Once per distinct ICY title; no interval poll | Omit artwork and retain station fallback | Implemented/verified |
| All five | Cover station logos | Station-specific `/images/logos/` 200×200 PNG URL in the bootstrap station contract | PNG | None | Requested only while the folded cover selector is visible; Coil cache applies | Fall back to the app logo | Owner-provided official paths; all five verified HTTPS 200 on August 11, 2026 |
| SST, 1980s, Adagio, Entranced | Queue and recent history | `/modules/Queue_Played/Queue_Played-gen.php`, Queue page referer | Bounded ISO-8859-1 HTML | None | No more than once per 60 seconds; memory last-known | Preserve prior ready state or show refresh error | Administrator-authorized; implemented |
| Death.FM | Compact queue/history | `/player.php?ajax_action=get_db_info&station=dfm&asin=` | Bounded JSON containing HTML fragments | None | Same shared 60-second limit | Same queue repository error policy | Administrator-authorized; implemented |
| All five | Login challenge/form discovery | `/modules.php?name=Your_Account`; server-provided same-origin form action | Bounded HTML + text anti-spam prompt | Credentials and answer transient on POST | User initiated; restored session revalidated | New challenge and safe error | Administrator-directed text anti-spam migration; implemented |
| All five | Logout | `/modules.php?name=Your_Account&op=logout` | HTML/redirect | Station session | User initiated | Local protected session cleared even on remote failure | Implemented |
| All five | Chat read | `/modules/ClearChat/block-files/view.php?username=&sort=desc` | Bounded ISO-8859-1 HTML | No | 30-second minimum; memory-only | Prior state/error, no persisted history | Administrator-authorized; implemented |
| All five | Chat post-form discovery | Station root plus server form at `/modules/ClearChat/block-files/input.php` | HTML form | Station session | User initiated | No mutation without validated form/session | Administrator-authorized; implemented |
| All five | Chat post | Validated ClearChat form action | Form POST | Station session | Explicit user action; one confirmation read | No automatic repost | Administrator-authorized; implemented |
| All five | Request search | `/modules.php?name=Requests` with station search parameters | Bounded HTML | No | User initiated; no polling | Clear loading/error/empty state | Administrator-authorized; implemented |
| All five | Album/request eligibility | `/modules.php?name=Album&asin=<validated-id>` | Bounded HTML | No for browse | User initiated and re-read immediately before mutation | Reject untrusted/malformed actions or changed eligibility before submission | Implemented with M12 queue-aware resolver |
| All five | Random suggestions | `/modules.php?name=Requests` with verified `random` or `randomleast` parameter | Bounded HTML | No for browse | User initiated only | Empty/error state | Implemented |
| All five | Song request mutation | `/modules.php?name=Req&asin=<validated-id>&songID=<validated-id>` | HTML/redirect | Station session | Explicit confirmation; never automatically retried | Indeterminate results direct user to Queue | Administrator-authorized; implemented |
| SST only | Optional request message | Server-returned same-origin `/modules.php?name=Album&action=submitmessage...` form | Form POST | SST session | Only after explicit accepted request; 80 chars | Message failure never repeats song request | Administrator-authorized and verified |
| All five | Favorites discovery | `/modules.php?name=Favorites` | Bounded authenticated HTML | Station session | On destination open/manual refresh; no polling | Sign-in-specific or generic load error | Administrator-authorized; implemented |
| All five | Favorites list | Strict same-origin `/modules/Favorites/thelist.php?user2view=<discovered-id>` | Bounded HTML | Station session | Loaded after discovery; memory-only | Reject cross-origin/malformed list URL | Implemented; SST live verified |
| SST only | Listener request history | `/modules.php?name=Your_Requests` | Bounded ISO-8859-1 HTML | SST session | More destination/manual refresh only; memory-only; up to 10 rows | Sign-in-specific or generic load error | Administrator-authorized; M15 implemented and live researched |
| SST only | Request cooldown/readiness | Page-discovered exact `/modules/VIP_Subscribe/vip_req_timer.php` | Bounded HTML | SST session | At most once per explicit listener-activity refresh | Unknown when trusted evidence is absent | M15 implemented; representative VIP evidence verified |
| SST only | Membership badge | Page-discovered same-origin forum profile with bounded numeric member identifier | Bounded HTML | SST session | At most once per explicit listener-activity refresh | Unknown when profile or explicit badge is absent | M15 implemented; VIP evidence verified; rank ignored |
| All five | Player Contact handoff | Fixed monitored `mailto:` recipient; station-specific subject/body generated locally | Android email draft | No station session or email credentials | Explicit native-card tap; user reviews, cancels, or sends in email app | Reject non-catalogued recipients, URI query/fragment injection, and missing email handler | Current M31 catalog; no station browser page |

## Caching and safety policy

- Queue, chat, request catalog, Favorites, and listener request-activity data are memory-only today. Queue retains its last ready state when a later refresh fails.
- Artwork URL acceptance is restricted to the selected station host. Authentication, request, chat, and Favorites actions require same-origin validation.
- Public queue polling must remain at or below once per 60 seconds per selected station. Chat reads must remain at or below once per 30 seconds.
- M12 fails closed when queue or fresh eligibility cannot be established before a request; it never infers `Request Now` solely from stale display state.
- M15 performs no polling or mutations. It allows only exact same-origin SST history/timer/profile reads, caps history at ten, and reports unknown rather than inferring membership or cooldown.
- M31 narrows M16-derived public access to the fixed Contact email handoff. Public VIP/RIP pages were reverified as
  digital subscription and registration surfaces and removed. Membership also fails closed in the trust policy. No
  station browser page, payment, registration, recovery, management, or deletion route is currently catalogued.
- M17 certified the existing SST inventory without adding or changing an endpoint, stream address, request limit, polling interval, mutation, or session rule.
- M18 reconfirmed the existing 1980s.FM extended Queue endpoint at HTTP 200 with separate bounded Queue/Played tables and exercised its existing read-only least-played suggestion and login-challenge paths without adding an endpoint or mutation.
- M19 reconfirmed the existing Adagio.FM extended Queue endpoint at HTTP 200 with separate bounded Queue/Played tables, both unchanged station-supplied audio relays, and the existing read-only artwork, Chat, least-played suggestion, login-challenge, and trusted Forums paths without adding an endpoint or mutation.
