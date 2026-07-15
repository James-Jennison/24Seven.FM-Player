# Authentication matrix

Updated July 14, 2026. The five account systems are independent even where their legacy pages and field names are similar. No credential, active cookie, CAPTCHA token, private identifier, or authentication header is recorded here.

| Station | Login discovery | Registration | Password recovery | Account management | Logout | Form/session behavior | Native suitability and limitations |
| --- | --- | --- | --- | --- | --- | --- | --- |
| StreamingSoundtracks.com (`sst`) | HTTPS root supplies the current same-origin login form and alphanumeric CAPTCHA | `/modules.php?name=Your_Account&op=new_user` | Public account flow; exact recovery route not yet separately verified | `/modules.php?name=Your_Account` and `op=edituser` | `op=logout` | `username`, transient password, `gfx_check`, station challenge token; station cookies; same-origin redirects; encrypted restored session | Native login implemented and physically verified; VIP status is not yet modeled |
| 1980s.FM (`1980s`) | Independent 1980s.FM root/form/CAPTCHA | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout | Separate station ID, expected host, cookie manager entry, encrypted storage key, and auth state | Native adapter implemented; requires live account verification independent of SST |
| Adagio.FM (`adagio`) | Independent Adagio.FM root/form/CAPTCHA | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout | Fully station-scoped session and host validation | Native adapter implemented; requires independent live account verification |
| Death.FM (`death`) | Independent Death.FM root/form/CAPTCHA | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout | Fully station-scoped session and host validation | Native adapter implemented; membership branding differs (`RIP`) and is not yet modeled |
| Entranced.FM (`entranced`) | Independent Entranced.FM root/form/CAPTCHA | Same station-relative registration module | Exact recovery route not yet separately verified | Same station-relative account/edit module | Same station-relative logout | Fully station-scoped session and host validation | Native adapter implemented; requires independent live account verification |

## Isolation implementation

- `StationId` keys every authentication state, challenge, cookie manager, encrypted session record, and encrypted display identity.
- Each network call resolves an exact station origin and rejects cross-origin authentication destinations.
- Protected cookies are filtered to the expected domain before storage and again during restore.
- Signing out clears only the selected station's cookie manager and protected record. Other station sessions remain intact.
- Passwords and CAPTCHA answers are transient method parameters and are not written to Room, DataStore, preferences, logs, resources, build files, or documentation.
- Chat, requests, and Favorites load only the selected station's protected session for that station's expected host.
- The native Accounts surface renders all five station states at once. Every refresh, sign-in, and sign-out action carries an explicit `StationId` and does not implicitly target the playback selection.
- A successful protected-session restore remains signed in during a temporary network failure, while a successful station response that proves the saved session invalid produces an explicit `Expired` state and clears only that station.

## M13 verification

- Unit tests cover all-five restoration, explicit-station actions, expiration, one-station logout, and preservation of another signed-in station.
- In-memory and Android Keystore tests cover one-station clear behavior without disturbing another protected session.
- Compose instrumentation covers all five cards, independent status semantics, station-qualified controls, compact scrolling, and explicit action routing.
- All 15 connected instrumentation tests pass on the wired Motorola Razr 2023 running Android 16; physical inspection reached every station card after a fresh install.

## Current gaps

- Registration, recovery, and account-management actions are not yet native destinations or documented Custom Tab actions.
- Membership/VIP status and per-station request cooldown state are not modeled.
- Live sign-in and restored-session behavior still require representative accounts and user-entered security challenges during M18–M22 station certification; no credentials or challenge answers belong in test fixtures.
- Required future security coverage: station-scoped Favorites/request history and station-scoped queue effects as those features evolve.
