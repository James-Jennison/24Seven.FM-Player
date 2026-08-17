# 24seven.FM network feature matrix

Audited July 15, 2026; current scope reconciled July 19, 2026. `Implemented` means present in native code; `verified` distinguishes live evidence from structural similarity. Every account/session remains station-specific. StreamingSoundtracks.com completed its current-Alpha certification in M17; Private Messages remain separately deferred as M47.

M18 public/device evidence directly verifies 1980s.FM playback, metadata/artwork, Queue/History, public Chat,
signed-out Favorites/request boundaries, least-played request browsing, and native sign-in challenge loading. The
current shipped secondary catalog is limited to Contact; historical Membership, Games, and Awards evidence is not a
current product capability. M31 removed every VIP/RIP purchase and registration handoff from the global Play candidate.

M19 public/device evidence now directly verifies Adagio.FM playback and both relay sources, truthful classical
metadata/artwork, Queue/History, public Chat, signed-out Favorites/request boundaries, least-played request browsing,
native sign-in challenge loading, and historical secondary-page evidence. Its authenticated account and
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
| Private Messages | Yes | Yes | Yes | Yes | Yes | Potentially | Yes | No | Legacy HTML | Possibly | M47 deferred | Underlying website/server issues and production limits must be resolved first |
| Contact handoff | Contact | Contact | Contact | Contact | Contact | Native directory + Android email handoff | Email app required | No | Fixed monitored email | No browser route | Current shipped catalog | One reviewed `ACTION_SENDTO` draft; the user controls final send and protected sessions are not transferred |
| Forum access | Retired | Retired | Retired | Retired | Retired | No | N/A | N/A | Historical route research only | No | Permanently excluded | The Player will not expose Forum links, browser fallback, native retrieval, participation, or notifications |
| Other legacy pages | Not shipped | Not shipped | Not shipped | Not shipped | Not shipped | No current surface | Unknown | Unknown | Not approved | No | Explicitly unavailable | Games, Awards, news, stats, profiles, and other historical routes are not current capabilities |
| Membership/VIP status | VIP verified | VIP unverified | VIP unverified | RIP route historically verified; account status unverified | VIP unverified | Yes if reliably exposed | Yes | N/A | Profile-scoped legacy HTML | No current purchase or management route | SST read-only native status in M15; future commerce is M58–M60 | Explicit badge only; administrative rank never implies membership; M31 removed browser purchase/registration routes |
| Local favorites/preferences | N/A | N/A | N/A | N/A | N/A | Yes | No | No | Local persistence | No | Planned | Clearly separate from server favorites |
| Sleep Timer | N/A | N/A | N/A | N/A | N/A | Yes | No | No | Local playback/service state | No | Implemented and Razr-validated in M24 | Service-owned deadline; presets/custom duration, adjust/cancel, persistence, system cancellation state, and deterministic expiry without another player |
| System audio-output selection | Existing verified stream | Same | Same | Same | Same | Current route state only | No for listening | No | Android system Output Switcher | No | Dedicated Android output path implemented in M25 | Device/Bluetooth/wired/USB and supported system routes use the existing Media3 session; no scan, relay, proxy, new endpoint, or Google Cast implementation. |
| In-App Diagnostics | N/A | N/A | N/A | N/A | N/A | Yes | No | No | Deliberately generated local snapshot | No | Implemented and Razr-validated in M26 | Fixed allowlist; explicit preview/copy/share; bounded enum-only transitions; excludes credentials, sessions, CSRF values, account/community/report content, endpoints, route names, identifiers, raw errors/logs, and automatic transfer |
| Community push notifications | Local Chat mention detection uses the existing selected-Chat feed; closed-app source requires authorization; PM events also depend on M47 repair | Same | Same | Same | Same | Yes for local station opt-in; future relay TBD | Yes for signed-in identity and future PM | No | Existing Chat source for M27; no authorized event source or privacy-compatible relay is established for the deferred M36–M38 program | No additional M27 traffic | M27 implemented and Razr-validated; M36–M38 deferred unless JERIC authorizes official station-app work | Exact-name matching, baseline, blocked-author filtering, bounded memory-only dedupe, private no-message-text alert, and gated Chat navigation are local; no session forwarding or perpetual polling |
