# M32 Session, Controller, Network, and Supply-Chain Security Validation

Date: 2026-07-18
Milestone: M32
Result: Complete

## Accepted security decisions

M32 closes the local security gate without adding a WebView, a second player, a background poller, an administrator capability, or a new station endpoint. The Player continues to use service-owned Media3 playback, repository-owned protected network access, and independently stored station sessions.

### MediaSession controller authority

Controller privileges are explicit and least-privileged:

| Controller | Player commands | Sleep Timer | Media replacement |
| --- | --- | --- | --- |
| Player application package | Existing full app command set | Set and cancel | Allowed through the established station flow |
| Android-trusted system controller | Approved Play/Pause, Prepare, Stop, and read-only state commands | Cancel an active timer; cannot set one | Denied |
| Untrusted foreign controller | Approved Play/Pause, Prepare, Stop, and read-only state commands | Cannot set or cancel | Denied |

The service rechecks controller authority when custom commands or media additions arrive. A separate, non-shipping `security-harness` Android test application proves the foreign-package boundary using a different package and UID. The harness has no runtime dependency from `:app`, supplies no launcher activity, is absent from Player release artifacts, and is removed from the test device after the connected test.

### Protected station sessions

- One `StationAuthSessionCoordinator` owns the in-memory cookie manager for each canonical station and exact HTTPS origin.
- Authentication, Chat, Favorites, Listener Activity, and Song Requests share that station-scoped coordinator; no cookie manager is shared across stations.
- Every protected response is allowed to rotate or delete cookies. Updated state is persisted immediately.
- Cookie lifetimes are stored as absolute expiry instants. Loading an unchanged cookie no longer restarts its `Max-Age`; only a cookie returned by the server receives a new lifetime.
- A protected response that deletes the authenticated cookie, an expired restored cookie set, or a typed authentication-required response converges on the explicit `Expired` state.
- Expiry clears in-memory Favorites, Listener Activity, and any pending request for only the affected station. Other station accounts and protected state remain intact.
- Sign-out and expiry remove only the selected station's protected session.

### Canonical station identity

The supported station identifiers are now `sst`, `1980s`, `afm`, `dfm`, and `efm`. Device-local preferences, community-notification opt-ins, local block state, notification navigation, remote lookup boundaries, and Android-protected session keys use those values. Existing `adagio`, `death`, and `entranced` values migrate to `afm`, `dfm`, and `efm`; unsupported notification station values are rejected rather than opened.

### Network navigation

Queue and artwork retrieval no longer rely on automatic redirect following. Redirects are resolved manually with a maximum of five hops and must retain the exact expected HTTPS origin. HTTPS downgrade, cross-origin hosts, unexpected ports, user information, malformed locations, and excessive chains fail closed. Protected station sources also reject origins containing user information. The previously verified SST request upgrade remains narrowly scoped to its established canonical-origin behavior.

### Build and dependency integrity

- GitHub Actions are pinned to full commit SHAs with readable release comments.
- Dependabot is configured to propose weekly GitHub Actions and Gradle updates.
- The Gradle wrapper distribution is protected by its SHA-256 checksum.
- Gradle dependency verification records SHA-256 checksums in `gradle/verification-metadata.xml` and remains enforced during normal builds.
- Repository resolution continues to fail when a project adds an undeclared repository.
- Existing least-privilege workflow permissions and the guarded same-repository auto-merge path remain unchanged.

## Adversarial and regression coverage

Automated coverage includes:

- local, trusted-system, and foreign controller command matrices;
- genuine foreign-package media-injection and timer-command rejection;
- cookie rotation, deletion, absolute expiry, restoration, and per-station invalidation;
- encrypted legacy session-key migration and canonical preference migration;
- station-isolated auth observation and protected in-memory state clearing;
- same-origin redirects plus cross-origin, downgrade, port, user-information, malformed, and excessive-redirect rejection;
- canonical station identifiers across repositories, themes, notifications, safety state, and existing product behavior.

## Validation evidence

| Gate | Result |
| --- | --- |
| `./gradlew test lint assembleDebug --no-daemon --console=plain` with strict dependency verification | Passed; 157 JVM unit tests, debug lint, and debug APK assembly |
| Focused `:app:connectedDebugAndroidTest` on physical Motorola Razr 2023 / Android 16 | Passed; 20/20 session, migration, controller-policy, and playback-service tests |
| `:app:installDebug :security-harness:connectedDebugAndroidTest` on the same Razr | Passed; 1/1 genuine foreign-package controller test |
| Dependency verification generation followed by a normal strict build | Passed; no verification bypass remains in the accepted build |
| `git diff --check` | Passed |

The final debug APK is approximately 31.3 MB. The security harness is test-only and not part of that artifact.

## Visual and privacy review

M32 changes security boundaries and persistence behavior without a meaningful visual redesign, so an unrelated screenshot was not added. Existing signed-out and expired-session UI states remain the user-visible contract. No credentials, cookies, tokens, private endpoints, authenticated captures, tester identities, or administrator data were added to source, tests, evidence, or diagnostics.

## Acceptance

M32 is accepted. Its security controls must be rerun if a later milestone changes MediaSession commands, protected-session ownership, station identity, redirect behavior, repositories, Gradle dependency resolution, or GitHub Actions.
