# Authentication matrix

Updated July 15, 2026. The five account systems are independent even where their legacy pages and field names are similar. No credential, active cookie, CAPTCHA token, private identifier, or authentication header is recorded here.

| Station | Login discovery | Registration | Password recovery | Account management | Logout | Form/session behavior | Native suitability and limitations |
| --- | --- | --- | --- | --- | --- | --- | --- |
| StreamingSoundtracks.com (`sst`) | HTTPS root supplies the current same-origin login form and alphanumeric CAPTCHA | `/modules.php?name=Your_Account&op=new_user` | Public account flow; exact recovery route not yet separately verified | `/modules.php?name=Your_Account` and `op=edituser` | `op=logout` | `username`, transient password, `gfx_check`, station challenge token; station cookies; same-origin redirects; encrypted restored session | M18 certified: least-privileged native sign-in/restore/logout plus separately bounded VIP request-message and listener-activity evidence |
| 1980s.FM (`1980s`) | Independent 1980s.FM root/form/CAPTCHA; representative native sign-in verified | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout verified | Separate station ID, expected host, cookie manager entry, encrypted storage key, and auth state; process-restart restoration verified without Keystore errors | M19 complete: MorG sign-in, protected restoration, station isolation, authenticated Favorites/Chat/request availability, and station-only logout verified |
| Adagio.FM (`adagio`) | Independent Adagio.FM root/form/CAPTCHA; representative native sign-in verified | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout verified | Separate station ID, expected host, cookie manager entry, encrypted storage key, and auth state; process-restart restoration verified without Keystore errors | M20 complete: MorG sign-in, protected restoration, station isolation, authenticated Favorites/Chat/request availability, and station-only logout verified |
| Death.FM (`death`) | Independent Death.FM root/form/CAPTCHA; native read-only challenge verified on wired device | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout | Fully station-scoped session and host validation | Native adapter implemented; `RIP_Subscribe` membership branding/route modeled; representative authenticated evidence pending |
| Entranced.FM (`entranced`) | Independent Entranced.FM root/form/CAPTCHA | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout | Fully station-scoped session and host validation | Native adapter implemented; requires independent live account verification |

## Isolation implementation

- `StationId` keys every authentication state, challenge, cookie manager, encrypted session record, and encrypted display identity.
- Each network call resolves an exact station origin and rejects cross-origin authentication destinations.
- Protected cookies are filtered to the expected domain before storage and again during restore.
- Signing out clears only the selected station's cookie manager and protected record. Other station sessions remain intact.
- Passwords and CAPTCHA answers are transient method parameters and are not written to Room, DataStore, preferences, logs, resources, build files, or documentation.
- Chat, requests, and Favorites load only the selected station's protected session for that station's expected host.
- SST listener activity uses the same protected station session for exact allowlisted request-history, timer, and profile reads; its memory-only state clears on SST sign-out.
- The native Accounts surface renders all five station states at once. Every refresh, sign-in, and sign-out action carries an explicit `StationId` and does not implicitly target the playback selection.
- A successful protected-session restore remains signed in during a temporary network failure, while a successful station response that proves the saved session invalid produces an explicit `Expired` state and clears only that station.

## M13 verification

- Unit tests cover all-five restoration, explicit-station actions, expiration, one-station logout, and preservation of another signed-in station.
- In-memory and Android Keystore tests cover one-station clear behavior without disturbing another protected session.
- Compose instrumentation covers all five cards, independent status semantics, station-qualified controls, compact scrolling, and explicit action routing.
- All 15 connected instrumentation tests pass on the wired Motorola Razr 2023 running Android 16; physical inspection reached every station card after a fresh install.

## M15 verification

- Representative signed-in SST pages explicitly exposed the last-ten request table, request readiness/wait minutes, and a profile-scoped VIP badge separately from administrative rank.
- Unit tests cover trusted discovery, cross-origin rejection, expired authentication, VIP/RIP/standard/unknown states, cooldown parsing, station-scoped refresh, and sign-out clearing.
- All 19 connected instrumentation tests pass on the wired Motorola Razr 2023 running Android 16; the fresh install physically rendered the explicit signed-out listener-activity state.

## M18 StreamingSoundtracks.com certification

- Existing least-privileged native sign-in, protected restart restoration, logout, Chat post, catalog request, and
  account-gated UI evidence was reconciled with the separate VIP request-message and listener-activity evidence.
- A fresh install remained correctly signed out; Favorites and request activity exposed explicit sign-in boundaries
  rather than leaking cached account data.
- Full unit/lint validation and 21/21 wired Android 16 Razr instrumentation tests passed. A silent physical smoke
  test reconfirmed public playback, metadata/artwork, Queue, Chat, and all five navigation targets.
- A fresh standard-tier production listener-activity refresh remains CAPTCHA-interaction-blocked and is recorded as
  such; the app does not infer VIP status or cooldown behavior from that missing evidence.

## Current gaps

- Registration, recovery, and account-management actions are not yet native destinations or documented Custom Tab actions.
- Membership and request cooldown are enabled only for SST; 1980s.FM, Adagio.FM, Death.FM RIP, and Entranced.FM require independent authenticated evidence before their certification milestones enable the capability.
- Live sign-in and restored-session behavior still require representative accounts and user-entered security challenges for M21–M22; M19–M20 are complete. No credentials or challenge answers belong in test fixtures.
- Required future security coverage: representative live listener-activity refresh for every station that is enabled during certification.
