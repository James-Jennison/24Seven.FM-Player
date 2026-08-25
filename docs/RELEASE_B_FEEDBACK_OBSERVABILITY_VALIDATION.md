# Release B — Feedback and local observability validation

Status: In progress — feedback-reporting slice validated on an Android emulator; broader Release B acceptance remains open.

## Delivered feedback slice

- **More → Report a problem** is a separate native form, not the community-abuse-report flow. It offers Playback, Station switching, Song request, Chat, Account or profile, UI or display, and Other categories with an optional bounded description.
- **Review email draft** is the only export action. It prepares a local Android email draft addressed to the existing monitored Player contact; the user can review, edit, cancel, or explicitly send it in their chosen email app. The Player never sends the feedback itself or confirms delivery.
- The draft includes selected category, selected station, and its preparation timestamp, so a tester does not need to manually determine that context.
- **Include privacy-safe diagnostics** is unchecked by default and requires a separate user action. When chosen, it adds the existing allowlisted snapshot: app/build version, Android/API, coarse device model, selected station, playback/error category, bounded last stream-start or station-switch duration, validated-network availability, broad audio-output category, and at most five recent non-sensitive playback transitions.
- The feedback path does not add analytics, crash reporting, a developer-operated backend, background upload, persistent feedback storage, account data, messages, request/report content, URLs, stable device identifiers, raw errors, or logs.
- The stream-start/switch duration is held only in the in-memory playback state. It is reset when a new station is selected and is clipped to ten minutes when rendered in a diagnostic snapshot.

## Emulator validation

| Check | Result |
| --- | --- |
| Debug Kotlin and Android-test Kotlin compilation | Pass |
| Full `:app:testDebugUnitTest` suite | Pass |
| `:app:lintDebug` | Pass, 0 errors |
| Focused `RadioAppTest#feedbackDraftUsesSelectedCategoryAndOnlyIncludesDiagnosticsAfterConsent` on `24Seven_API_35` (Android 15) | Pass, 1/1 |
| Full `:app:connectedDebugAndroidTest` on `24Seven_API_35` (Android 15) | 67/73 pass; six pre-existing Release A baseline failures, no new failure from this branch |

The focused connected test selects **Station switching**, enters an optional description, verifies the first review action receives no diagnostic text, then explicitly selects the diagnostics checkbox and verifies the second review action receives the redacted snapshot. It injects a private-looking URL/token and a personal Bluetooth route label into playback state and verifies neither reaches the included snapshot.

The full connected run retained the following already-recorded Release A failures: `stationAccountsExposeFiveIndependentStatusesAndTargetActions`, `maximumFontAndDisplayScaleKeepMediumNavigationAndPlayerReachable`, `compactMorePrioritizesSelectedAccountAndTogglesDisclosures`, `cutoutConstrainedCoverWindowHidesAppNavigationAndKeepsPlaybackControlsVisible`, `favoritesShowAccessibleStoplightsAndReuseRequestConfirmation`, and `artworkPlayerControlsRemainReachableInShortWideLayout`. The Release B feedback test passed in that same run. These baseline failures are not changed in this branch.

## Open Release B gates

- Extend the local, privacy-safe observability contract only where each additional event has a bounded non-identifying representation and a clear tester/support purpose; do not add silent telemetry or a developer-operated data service.
- Run the relevant Release B connected suite on the emulator after each additional slice.
- Reconfirm the final Release B candidate on the attached Razr when it is available, including the Android email-draft handoff and TalkBack/large-text reachability.
