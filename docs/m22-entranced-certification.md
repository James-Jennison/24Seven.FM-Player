# M21 Entranced.FM certification — complete

Public, physical-device, and representative authenticated certification was completed July 15, 2026 on
`agent/initial-android-scaffold`.

## Task assessment

- Task Complexity Level: 2 — Feature Logic & API Integration
- T-shirt size: M
- Estimated active duration: 4–7 hours
- Rationale: Entranced.FM uses the shared extended Queue/History, authentication, Chat, Favorites, request, and
  secondary-page contracts, but its live ICY catalog exposed a legacy Windows-1252 punctuation byte that required a
  narrowly scoped playback-boundary correction and regression coverage.
- Primary confidence variable: resolved with a representative Entranced.FM account, user-entered CAPTCHA, live relay
  evidence, and deterministic punctuation tests

## Completed public and device evidence

| Capability | Evidence | Result |
| --- | --- | --- |
| Primary playback | The unchanged primary relay reached Media3 `PLAYING` on the wireless Android 16 Razr while media volume was temporarily reduced | Pass |
| Source fallback | Both unchanged station-supplied relays returned live `audio/aacp` bytes at 128 kbps; the shared ordered Media3 fallback behavior remains covered by M02 and unit evidence | Pass without destructive failure injection |
| Metadata/artwork | A fresh live run presented the Entranced.FM title and artist separately, loaded same-station *Syro* artwork, showed `LIVE`, and reported AAC/128 kbps | Pass |
| Legacy punctuation | An earlier live title exposed Windows-1252 byte `0x92` as a C1 control/replacement glyph. The ICY boundary now maps only defined Windows-1252 C1 punctuation while preserving unrelated Unicode. The installed build showed neither replacement nor C1 characters. | Pass; exact apostrophe mapping is deterministic in unit evidence because the live track changed |
| Queue/History | The exact bounded public extended endpoint returned 37 table rows; native Queue/History retained the shared 60-second minimum interval | Pass |
| Public Chat | Native Entranced.FM Chat loaded without error and retained the shared 30-second, memory-only behavior | Pass |
| Request browsing | Public least-played browsing returned a catalog choice without mutation | Pass |
| Secondary pages | The exact seven common HTTPS pages and same-origin trust policy were independently reverified | Pass |
| Capability differences | Request messages and listener activity remain explicit `Not verified`; they do not inherit SST-only flags | Pass |
| Navigation/accessibility | Player, Favorites, Chat, Queue, and More remain station-qualified and retain the persistent mini-player | Pass |

## Representative authenticated evidence

| Check | Physical Razr result |
| --- | --- |
| Native sign-in | MorG signed in through Entranced.FM's own native username/password/alphanumeric-CAPTCHA form. |
| Protected restoration | A forced app stop/relaunch changed the process and restored only the Entranced.FM session; no Android Keystore error was observed. |
| Station isolation | 1980s.FM, Adagio.FM, and Death.FM remained visibly signed out while Entranced.FM was signed in. |
| Favorites | The authenticated Entranced.FM Favorites surface loaded a valid empty list, retained its filter, and showed no error. |
| Chat | The authenticated Entranced.FM Chat composer and Send action became available; no test message was posted. |
| Requests | Least-played browsing returned one green requestable track and enabled `Request Now`; no request was submitted. |
| Logout | `Sign out of Entranced` cleared the station immediately. After another forced stop/relaunch, `Load Entranced sign in` remained visible and all station accounts reported signed out. |

Natural server-side session expiry was not induced. The shared authentication implementation and tests retain the
expiration classification coverage; explicit logout avoided waiting for or manipulating a production session.

No production Chat post, song request, membership action, stream change, or account mutation beyond sign-in/logout
was performed. No credentials, CAPTCHA value, session material, private response, participant content, or captured
HTML was stored.

## Focused hardening

`RadioPlaybackService` now normalizes only the defined Windows-1252 punctuation code points that Media3 can expose as
ISO-8859-1 C1 characters in legacy ICY titles. It does not transcode the full title or alter valid Unicode.

Focused tests prove:

- `0x92`/U+0092 becomes the typographic apostrophe `’`;
- non-Latin Unicode remains unchanged;
- Entranced.FM retains authentication, Chat, Favorites, extended Queue/History, requests, and exact secondary pages;
- SST-only request-message and listener-activity capabilities remain disabled.

## Validation

- `:app:compileDebugKotlin` passed from a fresh shell using the ignored project SDK fallback.
- Both focused test classes passed.
- All 110 debug unit tests passed with zero failures or skips.
- `:app:lintDebug` passed.
- All 21 connected instrumentation tests passed on the wireless Android 16 Razr (131 tests total).
- The validator passed JDK 17/SDK checks, debug and release unit tests, lint, and `assembleDebug`.
- The standalone debug APK reinstalled and launched successfully.
- Live MediaSession evidence reached `PLAYING`, then paused cleanly. Media volume returned to its exact original speaker index and auto-rotate was restored.

## Physical-device evidence

The live screenshot records the installed corrected build with same-station artwork and no replacement glyph. The
account screenshot records the representative Entranced.FM session while the other visible station accounts remain
signed out. It contains only the administrator-approved test identity and no credentials, CAPTCHA, or session data.

![Entranced.FM live Player evidence](screenshots/m22-entranced-certification.png)

![Independent Entranced.FM account signed in on the Razr](screenshots/m22-entranced-authenticated.png)

## Certified limits

Entranced.FM request messages, listener request activity, and native membership state remain explicitly `Not verified`.
They are not inferred from StreamingSoundtracks.com or similar page structure. Native Private Messages
remain deferred under M47.
