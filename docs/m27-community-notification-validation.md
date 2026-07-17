# M27.1 local Chat-mention notification validation

Date: July 16, 2026

Status: **Complete for the existing actively observed Chat feed. Reliable closed-app push remains M27.2.**

## Delivered boundary

- Station-scoped, device-local opt-in control in More → Community notifications.
- Case-insensitive exact signed-in-display-name matching with letter, number, and underscore boundaries.
- First-snapshot baselining so enabling the feature never alerts for already-visible Chat history.
- Suppression of the signed-in user's own messages, locally blocked authors, and duplicate snapshots.
- At most 200 SHA-256 message fingerprints retained per enabled station in memory; message text is never persisted.
- Dedicated `Chat mentions` Android notification channel with private lock-screen visibility.
- Notification copy includes the station and sender but never the Chat message.
- Notification taps select the originating station and open its native Chat destination without bypassing age, Terms,
  mature-content, authentication, or block gates.
- Station opt-ins survive app recreation and remain excluded from Android backup/device transfer under the existing
  app-private backup rules.
- The opt-in checkbox exposes an explicit TalkBack description instead of an unlabeled actionable node.

## Intentional limitations

The implementation consumes only the Chat snapshot already loaded while the Chat destination is selected. It neither
increases the verified 30-second polling cadence nor observes Chat from other screens, background services, periodic
workers, or a developer backend. Consequently it can demonstrate correct local mention classification and Android
delivery, but it cannot receive a new event while the app is closed, backgrounded away from Chat, or asleep.

M27.2 requires an authorized station-side event source, webhook, or privacy-compatible relay before reliable push can
be implemented. Protected station sessions must not be forwarded to a relay, and periodic/background polling must not
be represented as push. Private Message events remain capability-gated with M17's server repair.

## Verification

- `./gradlew :app:testDebugUnitTest :app:lintDebug` — passed; 146/146 unit tests and zero lint errors.
- `./gradlew :app:compileDebugAndroidTestKotlin` — passed.
- Current-head accessibility regression — 146/146 unit tests, 48/48 API 35 Tablet connected tests, and debug lint pass.
  Exact and case-insensitive synthetic `MorG` mentions alert once, while `MorgHubby`, embedded-name, self-authored,
  blocked-author, historical, and duplicate cases remain suppressed.
- Focused physical Motorola Razr 2023 / Android 16 instrumentation — 2/2 passed:
  - station opt-in persistence, first-snapshot baseline, exact matching, and duplicate suppression;
  - signed-in adult More-menu opt-in rendering and action propagation.
- Privacy/Data Safety review — updated the in-app privacy notice, Data Safety worksheet, architecture, network matrix,
  implementation plan, future scope, and README. No new endpoint, SDK, permission, background worker, or message-text
  persistence was introduced.

## Remaining M27.2 checks

- Obtain and document authorization for the true event source.
- Define minimal authenticated Chat/PM event payloads, retention/deletion, abuse controls, outage behavior, and key
  rotation without transmitting protected station sessions.
- Validate closed/background/idle delivery, duplicate behavior across reinstalls and token rotation, multi-station
  identity mapping, battery/traffic impact, lock-screen behavior, notification permission denial, and all target APIs.
- Reconcile the final delivery provider and payload against Google Play Data Safety before M28 publication.
