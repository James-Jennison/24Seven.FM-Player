# Release B — Feedback and local observability validation

Status: Implementation and automated acceptance complete. Manual Android email-draft handoff and TalkBack/large-text review remain available as tester checks; neither was represented as automated-device proof.

## Delivered feedback slice

- **More → Report a problem** is a separate native form, not the community-abuse-report flow. It offers Playback, Station switching, Song request, Chat, Account or profile, UI or display, and Other categories with an optional bounded description.
- **Review email draft** is the only export action. It prepares a local Android email draft addressed to the existing monitored Player contact; the user can review, edit, cancel, or explicitly send it in their chosen email app. The Player never sends the feedback itself or confirms delivery.
- The draft includes selected category, selected station, and its preparation timestamp, so a tester does not need to manually determine that context.
- **Include privacy-safe diagnostics** is unchecked by default and requires a separate user action. When chosen, it adds the existing allowlisted snapshot: app/build version, Android/API, coarse device model, selected station, playback/error category, bounded last stream-start or station-switch duration, validated-network availability, broad audio-output category, and at most five recent non-sensitive playback transitions.
- The feedback path does not add analytics, crash reporting, a developer-operated backend, background upload, persistent feedback storage, account data, messages, request/report content, URLs, stable device identifiers, raw errors, or logs.
- The stream-start/switch duration is held only in the in-memory playback state. It is reset when a new station is selected and is clipped to ten minutes when rendered in a diagnostic snapshot.
- When no track artwork has arrived, the player and mini-player use the selected station's verified official logo. Track artwork takes priority as soon as it is available. The folded-cover layout also keeps the station selector within the cutout-constrained window.

## Automated and visual validation

| Check | Result |
| --- | --- |
| Debug Kotlin and Android-test Kotlin compilation | Pass |
| Full `:app:testDebugUnitTest` suite | Pass |
| `:app:lintDebug` | Pass, 0 errors |
| Focused `RadioAppTest#feedbackDraftUsesSelectedCategoryAndOnlyIncludesDiagnosticsAfterConsent` on `24Seven_API_35` (Android 15) | Pass, 1/1 |
| Debug build visual check on `24Seven_API_35` (Android 15) | Pass — the selected StreamingSoundtracks official logo renders before track artwork is available |
| Focused `RadioAppTest` on the attached motorola razr 2026 (Android 16) | Pass, 43/43 |
| Full `:app:connectedDebugAndroidTest` on the attached motorola razr 2026 (Android 16) | Pass, 73/73 |

The focused connected test selects **Station switching**, enters an optional description, verifies the first review action receives no diagnostic text, then explicitly selects the diagnostics checkbox and verifies the second review action receives the redacted snapshot. It injects a private-looking URL/token and a personal Bluetooth route label into playback state and verifies neither reaches the included snapshot.

## Remaining manual tester checks

- Confirm the Android email chooser opens from **More → Report a problem → Review email draft**, and that the tester can inspect, cancel, or explicitly send the draft in their chosen mail app.
- Review the same flow with TalkBack and large text. This must remain a user-controlled email handoff; do not add silent telemetry or a developer-operated data service.
