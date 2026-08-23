# M20 Death.FM certification — complete

Public, physical-device, and representative authenticated certification was completed July 15, 2026 on
`agent/initial-android-scaffold`.

> Historical validation: M31 preserves this station certification but removes the RIP purchase/registration browser
> route from the current global Play candidate. The screenshot and route observations below remain evidence of M20's
> completed work, not a description of the current station-page catalog.

## Task assessment

- Task Complexity Level: 2 — Feature Logic & API Integration
- T-shirt size: L
- Estimated active duration: 6–10 hours
- Rationale: Death.FM combines a compact HTML-fragment Queue contract, sparse live metadata/artwork, independently
  scoped account and request behavior, and RIP-specific membership presentation. Its previously unavailable HTTPS
  routes also required fresh protocol and trust-policy evidence before being enabled.
- Primary confidence variable: resolved with a representative Death.FM account and user-entered CAPTCHA; reliable
  authenticated RIP membership/request activity was not exposed and remains explicitly disabled

## High-level task breakdown

1. Reconfirm the unchanged primary/source relays, Media3 state, metadata, and artwork on the wired Razr.
2. Verify the compact Queue/History, public Chat, request catalog, Favorites gate, and sign-in challenge without mutation.
3. Reaudit Death.FM's recovered HTTPS pages and model the proven RIP membership difference behind station data.
4. Lock the capability and exact-route contract with focused repository tests.
5. Run unit, lint, connected-device, install, and Linux validators; preserve screenshots and update roadmap evidence.
6. Commit and publish the focused checkpoint while leaving unverified authenticated capabilities disabled.

## Completed public and device evidence

| Capability | Evidence | Current result |
| --- | --- | --- |
| Primary playback | The unchanged primary relay reached Media3 `PLAYING` on the wired Android 16 Razr with media volume temporarily muted | Pass |
| Source fallback | Both unchanged station-supplied relays delivered live audio bytes; the shared ordered Media3 fallback behavior remains covered by M02 and unit evidence | Pass without a destructive station-specific failure injection |
| Sparse metadata/artwork | A fresh live run presented the Death.FM title and artist separately, loaded same-station artwork, showed `LIVE`, and reported AAC/128 kbps | Pass; the raw ICY value remains preserved before presentation |
| Compact Queue/History | The exact public compact JSON feed returned HTTP 200 with ten upcoming and ten played rows; native `Up next` and `Recently played` loaded without inventing absent extended fields | Pass under the shared 60-second limiter |
| Public Chat | Native Death.FM Chat loaded current public messages without error and showed the correct signed-out posting boundary; shared 30-second/memory-only behavior remains intact | Pass |
| Favorites boundary | Native Favorites is reachable and shows the Death.FM-qualified sign-in requirement without leaking another station's data | Pass |
| Request browsing | The native least-played suggestion returned public catalog choices while retaining the signed-out submission boundary; no request was submitted | Pass |
| Authentication | Native username/password fields, same-station CAPTCHA image, alphanumeric security-code field, and new-code action loaded without error. A representative Morgue session signed in, restored after a forced process restart without Keystore errors, remained isolated from the other stations, and cleared through explicit station-only logout across another restart. | Pass |
| Capability differences | Request messages and listener activity remain explicit `Not verified`; compact Queue rows do not invent requester/message or extended metadata fields | Pass |
| Secondary pages | HTTPS recovered after M16. The trusted directory exposes the station website, Forums, Members, Stats, Top 100, Contact, and Death-specific RIP membership route; RIP membership opened at `death.fm` in a Chrome Custom Tab | Pass under the unchanged M16 trust policy |
| Navigation/accessibility | Player, Favorites, Chat, Queue, and More remain present with station-qualified semantics and the persistent mini-player on secondary destinations | Pass |

No production Chat post, song request, account mutation beyond sign-in/logout, or membership action was performed.
No credentials, CAPTCHA value, session material, private response, participant content, or captured HTML was stored.

## Representative authenticated evidence

| Check | Physical Razr result |
| --- | --- |
| Native sign-in | Morgue signed in through Death.FM's own native username/password/alphanumeric-CAPTCHA form. |
| Protected restoration | A forced app stop/relaunch produced a new process and restored only the Death.FM session; no Android Keystore error was observed. |
| Station isolation | 1980s.FM, Adagio.FM, and Entranced.FM remained visibly signed out while Death.FM was signed in. |
| Favorites | The authenticated Death.FM Favorites surface loaded a valid empty list, retained its filter, and showed no error. |
| Chat | The authenticated Death.FM Chat composer and Send action became available; no test message was needed or posted. |
| Requests | Least-played browsing returned one green requestable track and enabled `Request Now`; no request was submitted. |
| RIP boundary | RIP membership remains an exact trusted same-station browser route. The native authenticated session exposed no reliable membership or personal request-activity contract, so those capabilities remain disabled. |
| Logout | `Sign out of Death` cleared the station immediately. After another forced stop/relaunch, `Load Death sign in` remained visible and all visible station accounts reported signed out. |

Natural server-side session expiry was not induced. The app's expiration classification remains covered by the
shared authentication implementation and tests; this certification used explicit logout to avoid waiting for or
artificially manipulating a production session.

## Focused hardening

`BootstrapStationRepository` now enables Death.FM secondary content only after the configured HTTPS origin recovered
and its exact routes were reverified. The common immutable page catalog remains shared, while the station model supplies
the proven `RIP membership` title and `RIP_Subscribe` module instead of another station's VIP terminology.

`BootstrapStationRepositoryTest` locks the independently verified Death.FM capability contract:

- authentication, Chat, Favorites, compact Queue/History, requests, and trusted secondary content remain enabled;
- SST-only request messages and listener activity remain disabled;
- the HTTPS station origin and exact seven-page route set remain trusted catalog entries;
- the membership card is exactly `RIP membership` and targets `https://death.fm/modules.php?name=RIP_Subscribe`.

## Validation

- `:app:compileDebugKotlin` passed.
- All 108 debug unit tests passed.
- `:app:lintDebug` passed.
- All 21 connected instrumentation tests passed on the wired Android 16 Razr (129 total tests across both suites).
- The standalone debug APK was reinstalled after the connected-test harness and launched successfully.

## Physical-device evidence

The physical run left playback paused and restored the original media volume. No fatal application exception was
observed. The first screenshot records the live Death.FM title and same-station artwork after playback was paused.
The second records the exact Death.FM public directory, including RIP membership. Neither contains account data.

![Death.FM Player evidence on the wired Razr](screenshots/m21-death-certification.png)

![Death.FM trusted secondary pages and RIP membership](screenshots/m21-death-rip-pages.png)

The account screenshot shows the independent Death.FM session while the other visible station accounts remain signed
out. It contains only the administrator-approved test identity and no credentials, CAPTCHA, or session data.

![Independent Death.FM account signed in on the Razr](screenshots/m21-death-authenticated.png)

## Certified limits

Death.FM request messages, listener request activity, and native membership state remain explicitly `Not verified`.
The RIP membership browser card was separately verified during certification, but M31 removed it from the current
global Play candidate. Future purchase and activation work is gated by M58–M60. Native Private Messages remain
deferred under M47.
