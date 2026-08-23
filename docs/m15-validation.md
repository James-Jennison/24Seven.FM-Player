# M15 validation — request history, cooldown, and membership

Validated July 14, 2026 from implementation commit `b19d5fe` on `agent/initial-android-scaffold`.

## Delivered behavior

- Added an immutable, station-scoped `ListenerActivityRepository` contract and ViewModel state.
- Added conservative authenticated SST reads for the last ten requests, station-reported cooldown/readiness, and explicit VIP/RIP membership branding.
- Added a native More-screen card with signed-out, loading, error, unknown, empty, ready, and waiting presentation plus accessible semantics.
- Kept request history in memory, cleared it on station sign-out, and introduced no polling or mutation.
- Enabled the capability only for SST; all other stations remain explicitly unverified pending their certification milestones.

## Automated validation

The following incremental validators passed without `clean`:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

Coverage includes trusted discovery URLs, cross-origin rejection, the ten-entry ceiling, bounded combined history summaries, ready/waiting/unknown cooldown states, VIP/RIP/standard membership, expired authentication, repository errors/clear, lifecycle-scoped ViewModel refresh, station capability isolation, explicit action routing, and Compose accessibility semantics. The complete connected suite passed **19/19** tests on the wired Motorola Razr 2023 running Android 16.

## Live and physical evidence

A read-only signed-in browser session confirmed the representative SST request-history, timer, and profile structures described in [m15-request-activity-research.md](m15-request-activity-research.md). No request or other mutation was made.

The fresh debug install replaced the prior app data, so protected station sessions were intentionally absent during physical UI inspection. The Razr rendered the new capability row, signed-out request-activity card, refresh-disabled state, persistent mini-player, and surrounding account/request content correctly. An authenticated ready/wait/history state was rendered and action-tested by the physical-device Compose suite; repeating a production sign-in requires a user-entered CAPTCHA and is therefore left for SST certification.

![SST request activity signed-out state on the wired Razr](screenshots/m15-request-activity.png)

## Known limits

- SST is the only station enabled from representative authenticated evidence in M15.
- The legacy history table exposes a combined album/track/artist summary, so the app deliberately does not invent separate fields.
- Membership purchase/management is not native and remains outside this read-only status surface.
- Production authenticated refresh after a fresh install still requires the listener to complete the station CAPTCHA; no credentials or session artifacts are included in tests.
