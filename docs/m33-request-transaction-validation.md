# M33 Request Transaction Integrity Validation

Date: 2026-07-18
Milestone: M33
Result: Complete

## Boundaries accepted for this milestone

M33 hardens the existing native request workflow without adding a station endpoint, browser surface, background work, or a retry path. A request stays station-scoped and requires one explicit user confirmation.

- Opening confirmation captures an immutable station, signed-in display name, and track snapshot.
- The dialog visibly identifies the station, account, and track. Sending is disabled if the selected station or account no longer matches the captured identity.
- Confirmation refreshes Queue state and, where the certified station supports it, Listener Activity. The repository independently rejects a station mismatch, signed-out or swapped account, stale/non-ready Queue, unknown membership, non-ready cooldown state, or positive wait.
- The album is reloaded immediately before submission. The same song ID alone is insufficient: stable album/title and available artist/album metadata must still agree.
- Queue and recent-play matching remains fail-closed. A queued track cannot show `Request Now`.
- The request transport is invoked at most once. Submitted, indeterminate, and classified rejection results add a bounded in-memory block. That block is applied to both Library and Favorites, so a stale Favorites row cannot reopen the same transaction.
- Cooldown, request-limit, membership, and authentication rejection blocks are station/account-wide for the current in-memory session. Track-specific submitted/indeterminate blocks are retained until sign-out, expiry, or process loss. No numeric request limit is invented; only fresh station availability or an authoritative response can establish it.
- Sign-out, natural expiry, and station changes clear or cancel only the affected station's request transaction state.

## Automated evidence

| Gate | Result |
| --- | --- |
| `./gradlew :app:compileDebugKotlin` | Passed |
| Focused request repository and ViewModel JVM suites | Passed |
| `./gradlew :app:testDebugUnitTest` | Passed; 163 JVM unit tests |
| `./gradlew :app:lintDebug` | Passed |
| `./gradlew :app:compileDebugAndroidTestKotlin` | Passed |
| Focused `RadioAppTest#songRequestRequiresExplicitConfirmation` on Motorola Razr 2023 / Android 16 | Passed; fixture-only station/account identity, no sign-in and no live request |

The new adversarial cases cover account swap/stale Queue rejection, required Listener Activity membership readiness, changed fresh album metadata, and suppression of a second prepare after an indeterminate result.

## Physical and privacy boundary

The physical-device check used a deterministic Compose fixture (`Listener`) and did not authenticate, inspect protected data, or submit a song request. It verified the compact Razr confirmation presentation includes the full station identity and signed-in account identity alongside the selected track.

No credentials, cookies, tokens, protected HTML, private endpoint information, request attribution, tester identity, administrator behavior, or live mutation evidence was added.

## Acceptance condition

Sol reviewed and accepted implementation commit `572d419`. M34 still owns the broader device/accessibility matrix and M29/M30/M35 retain their independent gates.
