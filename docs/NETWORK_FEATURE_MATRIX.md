# 24seven.FM network feature matrix

Audited July 15, 2026. `Implemented` means present in native code; `verified` distinguishes live evidence from structural similarity. Every account/session remains station-specific. StreamingSoundtracks.com completed its current-Alpha certification in M18; Private Messages remain separately deferred as M17.

M19 public/device evidence now directly verifies 1980s.FM playback, metadata/artwork, Queue/History, public Chat,
signed-out Favorites/request boundaries, least-played request browsing, native sign-in challenge loading, and trusted
Games/secondary pages. Authenticated account behavior and membership/request-activity differences remain pending.

M20 public/device evidence now directly verifies Adagio.FM playback and both relay sources, truthful classical
metadata/artwork, Queue/History, public Chat, signed-out Favorites/request boundaries, least-played request browsing,
native sign-in challenge loading, and trusted Forums/secondary pages. Its authenticated account and
membership/request-activity differences remain pending.

| Feature | StreamingSoundtracks.com | 1980s.FM | Adagio.FM | Death.FM | Entranced.FM | Native possible | Auth required | VIP required | Structured endpoint | Browser fallback | Status | Notes/blockers |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Live playback | Yes | Yes | Yes | Yes | Yes | Yes | No | No | PLS/ICY | No | Implemented/verified | Two ordered AAC relays per station |
| Now-playing title | Yes | Yes | Yes | Yes | Yes | Yes | No | No | ICY | No | Implemented/verified | Composite ICY title preserved |
| Current artwork | Yes | Yes | Yes | Yes | Yes | Yes | No | No | JSON + station image | No | Implemented/verified | Event-driven, station-hosted only |
| Upcoming queue | Extended | Extended | Extended | Compact | Extended | Yes | No | No | HTML/JSON | No | Implemented/verified | 60-second shared minimum; up to 30 rows on extended feeds |
| Played history | Extended | Extended | Extended | Compact | Extended | Yes | No | No | HTML/JSON | No | Implemented/verified | Current app shows recent rows, not full archive pagination |
| Request attribution/message display | Yes | Observed structure not message-verified | Same | Compact feed lacks fields | Same | Yes | No | No | Extended queue HTML | No | Implemented where supplied | SST message verified end to end |
| Library search/album tracks | Yes | Yes | Yes | Yes | Yes | Yes | No for browse | No | Public HTML | No | Implemented; SST live verified | Station-specific catalog context retained |
| Track requests | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Station rules may vary | Public HTML mutation with protected session | No | Implemented; M12 hardening complete | Explicit confirmation, no retry |
| Random/least-played suggestion | Yes | Shared site structure | Shared | Shared | Shared | Yes | No for browse | No | Request HTML | No | Implemented | User initiated only |
| Optional request message | Yes | Not verified | Not verified | Not verified | Not verified | Yes for SST | Yes | No | Post-acceptance form | No | SST implemented only | 80 characters; separate message record ID |
| Personal request history/cooldown | Last 10 + VIP timer verified | Similar navigation only | Similar navigation only | Similar navigation only | Similar navigation only | Yes where verified | Yes | Rules vary | Authenticated legacy HTML | No | SST implemented in M15 | Manual refresh only, memory-only, no mutation; other stations disabled pending certification |
| Request availability | Yes | Yes | Yes | Yes | Yes | Yes | Browse no; submit yes | Rules vary | Request/Favorites HTML + Queue | No | M12 implemented/validated | Structured station-scoped resolution, exact accessible labels, stale-data fail-closed behavior, and pre-submit revalidation |
| Native login | Yes | Yes | Yes | Yes | Yes | Yes | N/A | No | Legacy form/CAPTCHA | No | Implemented; SST physical validation | Sessions isolated by station ID and host |
| Server favorite tracks | Yes | Same module | Same module | Same module | Same module | Yes | Yes | No known requirement | Favorites discovery/list HTML | No | Implemented; SST live verified | Other four share audited module and adapter contract; live account verification remains |
| Community chat | Yes | Same module | Same module | Same module | Same module | Yes | Read public; post authenticated | No | ClearChat HTML | No | Implemented; SST live verified | 30-second minimum, history memory-only |
| Private Messages | Yes | Yes | Yes | Yes | Yes | Potentially | Yes | No | Legacy HTML | Possibly | M17 deferred | Underlying website/server issues and production limits must be resolved first |
| Forums/profiles/members | HTTPS route verified | HTTPS route verified | HTTPS route verified | HTTPS route reverified in M21 | HTTPS route verified | Native directory only | Browser-dependent | Some areas | Legacy HTML | Exact allowlisted Custom Tab | M16 plus M21 Death.FM recovery | Browser and protected app sessions remain separate |
| News/announcements | Station home available | Station home available | Station home available | HTTPS station home reverified in M21 | Station home available | No parser | No for public home | No | Legacy HTML | Exact allowlisted Custom Tab | Browser access verified for all five | No native content parsing, polling, or persistence |
| Contests/downloads/galleries/interviews | SST soundtrack page exposed | Games and awards exposed | Not exposed in M16 | No extra route exposed in M21 | Not exposed in M16 | Selective | Browser-dependent | Mixed | Mostly legacy HTML | Exact allowlisted Custom Tab | Selected verified routes only | No speculative or unverified station modules |
| Membership/VIP status | VIP verified | VIP unverified | VIP unverified | RIP route verified; account status unverified | VIP unverified | Yes if reliably exposed | Yes | N/A | Profile-scoped legacy HTML | Exact membership-management Custom Tab on all five | SST native status in M15; M16/M21 browser management | Explicit badge only; administrative rank never implies membership; browser and app sessions are not bridged |
| Local favorites/preferences | N/A | N/A | N/A | N/A | N/A | Yes | No | No | Local persistence | No | Planned | Clearly separate from server favorites |
| Sleep Timer | N/A | N/A | N/A | N/A | N/A | Yes | No | No | Local playback/service state | No | Implemented and Razr-validated in M24 | Service-owned deadline; presets/custom duration, adjust/cancel, persistence, system cancellation state, and deterministic expiry without another player |
| Cast / audio-output selection | Existing verified stream | Same | Same | Same | Same | Current route state only | No for listening | No | Android system Output Switcher; optional future authorized Cast receiver path | No | Dedicated Android output path implemented in M25 | Device/Bluetooth/wired/USB and supported system routes use the existing Media3 session; no scan, relay, proxy, or new endpoint. Google Cast remains capability-gated |
| In-App Diagnostics | N/A | N/A | N/A | N/A | N/A | Yes | No | No | Deliberately generated local snapshot | No | Planned M26 | Redacted output only; exclude credentials, sessions, CSRF values, community content, private endpoints, signing data, and raw logs |
| Community push notifications | Chat mention source requires authorization; PM events also depend on M17 repair | Same | Same | Same | Same | Potentially | Yes for signed-in identity and PM | No | Authorized event source or privacy-compatible relay not yet established | No | Planned M27, discovery first | True push only; exact-name Chat mentions before Alpha, plus new PM alerts once M17 works; separate controls, minimal payloads, no session forwarding or perpetual polling |
