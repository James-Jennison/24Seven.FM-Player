# M6 queue and history validation

Validated on July 13, 2026.

## Automated coverage

- the parser maps explicit track title, artist, cover, and row order without inventing album or duration fields;
- malformed rows are skipped;
- artwork outside the selected station domain is rejected;
- polling starts immediately only while Queue is observed;
- automatic and manual refresh share a hard 60-second minimum interval;
- station selection cancels the old station flow and observes the new station;
- leaving Queue cancels the active observation;
- initial failures expose a generic error without server response details;
- all five catalog entries expose only the administrator-authorized queue and history capabilities;
- Compose renders unavailable, loading, error, empty, queue, history, artist, and artwork-ready states natively.

## Physical-device evidence

The API 35 Motorola Razr 2023 received a fresh debug install over wireless ADB. Playback was not started.

A one-time on-device integration test made one authorized request per station. StreamingSoundtracks.com, 1980s.FM, Adagio.FM, Death.FM, and Entranced.FM each returned a non-empty queue, non-empty history, nonblank explicit titles, and station-hosted cover URLs. The temporary live-network test was removed afterward so the permanent test suite does not depend on external availability.

On 2026-07-13, the queue adapter was extended to display at most 30 upcoming and 30 played tracks from the public `Queue_Played` tables. Parser tests verify explicit field extraction, removal of requester text from titles, same-station artwork enforcement, and the 30-row ceiling. Death.FM retains its verified compact feed because its extended interface was unreliable; this keeps every refresh to one request and preserves the 60-second limiter.

The native StreamingSoundtracks.com Queue screen was visually inspected. It displayed ordered upcoming tracks, track title above artist, station cover artwork, refresh control, persistent mini-player, and phone bottom navigation. The inspection caught and corrected an initial field-semantic mismatch before publication. Temporary screenshots and UI hierarchy files were deleted and were not committed.

## Build gate

The final local validation passed debug and release unit tests, Android lint, debug APK assembly, and instrumentation APK assembly. The permanent four-test Compose and MediaSession suite also passed on the API 35 physical device. GitHub Android CI is verified after publication; see the PR checks for its final result.
