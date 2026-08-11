# M33 Request Transaction Integrity Validation

Date: 2026-07-18
Milestone: M33
Result: Complete

## Boundaries accepted for this milestone

M33 hardens the existing native request workflow without adding a station endpoint, browser surface, background work, or a retry path. A request stays station-scoped and requires one explicit user confirmation.

- Opening confirmation captures an immutable station, signed-in display name, and track snapshot.
- The dialog visibly identifies the station, account, and track. Sending is disabled if the selected station or account no longer matches the captured identity.
- Confirmation refreshes Queue state and, where the certified station supports it, Listener Activity. The repository independently rejects a station mismatch, signed-out or swapped account, stale/non-ready Queue, unknown membership, non-ready cooldown state, or positive wait.
- The album is reloaded immediately before submission. The station-assigned album and song IDs must still agree; its freshly returned eligibility remains authoritative. Display title, artist, and album text may be reformatted between responses and is not treated as a different track.
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

The adversarial cases cover account swap/stale Queue rejection, required Listener Activity membership readiness, harmless fresh-album metadata formatting changes, and suppression of a second prepare after an indeterminate result.

## Physical and privacy boundary

The physical-device check used a deterministic Compose fixture (`Listener`) and did not authenticate, inspect protected data, or submit a song request. It verified the compact Razr confirmation presentation includes the full station identity and signed-in account identity alongside the selected track.

No credentials, cookies, tokens, protected HTML, private endpoint information, request attribution, tester identity, administrator behavior, or live mutation evidence was added.

## Post-M33 station compatibility correction (2026-08-11)

The network’s current site markup invalidated the earlier assumption that a locally refreshed Queue or Listener Activity snapshot could safely gate a request. Those sources remain useful for display, but they are not requestability authority.

- The app now uses the freshly loaded station album row as the pre-submission requestability source. The station’s own request action remains final authority.
- Queue, membership, and countdown snapshots no longer reject a request locally or create local requestability blocks.
- After any submitted, rejected, or indeterminate result, the app re-reads the exact station album/track and displays that returned availability. It does not invent an unavailable state.
- A post-submission Queue verification is confirmation only. The visible Queue remains capped for UI performance; the verification reader examines up to 500 extended-queue rows so long station queues do not cause a false negative.
- Submission remains explicit and one-shot. No request is automatically retried.

## Acceptance condition

Sol reviewed and accepted implementation commit `572d419`. The broader device/accessibility matrix subsequently passed
under M34; M29, M30, and M35 retained their independent gates.
