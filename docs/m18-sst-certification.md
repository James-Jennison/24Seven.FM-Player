# M17 StreamingSoundtracks.com certification

Certified July 14, 2026 on `agent/initial-android-scaffold`. M17 is an evidence and hardening gate for the
existing shared native implementation; it did not require a station-specific code fork or a protocol change.

## Task assessment

- Task Complexity Level: 2 — Feature Logic & API Integration
- T-shirt size: S
- Estimated active duration: 2–4 hours
- Primary confidence variable: distinguishing ordinary-member behavior from VIP-only behavior after a fresh
  install removed the app's protected production session

## Scope decision

This certification covers the current Alpha capability set. Native Private Messages are deliberately excluded
from M17 because the administrator explicitly deferred that shared feature to M47 pending repair and
reverification of the legacy website/server behavior. M17 does not represent Private Messages as implemented or
certified.

No production request or chat mutation was repeated for certification. Existing least-privileged and VIP live
evidence already proves those one-shot paths, and repeating them would add traffic without increasing confidence.
No credential, cookie, CAPTCHA value, account identifier, private page, message content, or captured HTML was
stored.

## Certification matrix

| Capability | Authoritative implementation/test evidence | Live and wired-device evidence | Result |
| --- | --- | --- | --- |
| Primary playback and fallback | Two ordered bundled SST relays, atomic station switching, one controlled fallback, Media3 service/session tests | M02 sustained SST playback on the Razr; M02 controlled primary-only failure reached the unchanged source relay; M17 silently reached live playing again | Certified |
| Background/system media | Service-owned player, MediaSession, focus, noisy-route, notification, and media-command coverage | M03 verified background, lock/idle, notification, Bluetooth controls, focus, and route loss on the Razr | Certified |
| Metadata and artwork | Event-driven ICY title plus bounded same-station artwork enrichment; missing artwork never blocks playback | M04 verified title transitions and MediaSession parity; M17 displayed a current structured title, artist, same-station cover, `LIVE`, and AAC/128 kbps without a fatal error | Certified |
| Queue and history | Extended parser caps each list at 30, rejects foreign artwork, separates requester/message, and fixes nested-table ownership; one shared 60-second limiter | M06 live Queue/History and nested-table correction; M10 requester/message display; M17 loaded the current native `Up next` state with no Queue error | Certified |
| Authentication and isolation | Strict same-origin challenge/result parsing, Keystore-encrypted station records, per-station restore/logout/expiry tests, five-card account dashboard | M07 least-privileged native sign-in, restart restoration, and logout; M13 wired all-five signed-out dashboard; current install remains correctly signed out | Certified |
| Community Chat | Bounded memory-only parser, 30-second shared read limit, same-origin form discovery, transient post material, no automatic repost | M08 live public read and one authorized least-privileged native post/restoration; M17 loaded native SST Chat without an error | Certified |
| Server Favorites | Authenticated discovery of the current member's list ID, memory-only state, station-scoped clearing, filtering, and exact accessible availability labels | M12 loaded 1,500 SST Favorites on the Razr and showed live green/red eligibility without submitting a request; M17 confirmed the signed-out gate and account action | Certified |
| Catalog, random suggestions, and song requests | Same-origin search/album/suggestion parsing, structured availability, fresh Queue/album revalidation, explicit confirmation, and no retry | M09 least-privileged native search and one accepted request; M10 VIP least-played request with exact optional message confirmed in Queue | Certified |
| Request activity and membership | Exact SST history/timer/profile allowlist, ten-entry cap, manual refresh only, explicit VIP/standard/unknown states, no mutation | M15 read-only VIP structure evidence and wired rendering/action tests. A fresh standard-tier production refresh remains CAPTCHA-interaction-blocked, so no tier privilege is inferred from missing evidence | Certified with recorded interaction limit |
| Secondary station pages | Immutable catalog entries plus exact HTTPS/same-host trust policy; browser and protected app sessions remain separate | M16 opened the SST website in a Chrome Custom Tab and returned to the native app; the unchanged SST catalog and action semantics passed the complete M17 suite | Certified |
| Navigation and accessibility | Five primary destinations, persistent mini-player, stable semantics/test tags, signed-out and error states, adaptive phone/rail coverage | M17 accessibility inspection found Player, Favorites, Chat, Queue, and More simultaneously; the complete 21-test Razr suite passed | Certified |

## M17 physical smoke test

The current debug build was installed on the wired Motorola Razr 2023 running Android 16. With media volume
temporarily set to zero, SST reached the live playing state, supplied non-placeholder metadata and current album
artwork, retained all five navigation targets, and produced no fatal application exception. Playback was paused
and the original media volume was restored. Public Queue and Chat loaded without their error states, and Favorites
showed the correct station-sign-in boundary after the fresh install.

The screenshot was captured immediately after the live check was paused; its title and cover came from that live
session rather than a fixture.

![Certified SST Player state on the wired Razr](screenshots/m18-sst-certification.png)

## Validation

The following incremental checks passed without `clean`:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest :app:installDebug
```

- Full debug unit suite: passed.
- Debug lint: passed.
- Wired Android 16 Razr instrumentation: **21/21 passed**, zero skipped and zero failed.
- Debug installation: passed.
- Working tree and stream catalog: no production-code or stream-address change was needed for M17.

## Recorded limits

- The current SST Queue length is station-controlled; the app supports and tests a maximum of 30 visible upcoming
  and 30 recently played rows but does not fabricate rows when fewer are supplied.
- The standard-member listener-activity production refresh was not repeated after the fresh install because it
  requires user-entered credentials and an alphanumeric CAPTCHA. Missing standard-tier evidence remains explicit
  and does not inherit VIP status or cooldown rules.
- Primary-to-source failure was not re-induced on the physical phone. The unchanged relay order and controlled M02
  fallback proof remain authoritative.
- Native Private Messages remain M47 deferred and are not part of this certification.
