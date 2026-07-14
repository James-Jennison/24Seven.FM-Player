# M8 chat research

## Authorization

On July 13, 2026, a station administrator authorized this unofficial, non-commercial native Android app to use
the public-facing chat interfaces across all five stations. Authenticated message reading, participant metadata
display, and least-privileged harmless test-message posting are permitted. Production connection or polling
limits must be documented before chat is enabled, and chat history must not be persisted.

The implementation retains the original safe boundary:

- a station-scoped `ChatRepository` contract;
- immutable unavailable, loading, ready, and error states;
- a minimal immutable author, message, and displayed timestamp domain model;
- a replaceable data transport;
- ViewModel observation only while Chat is selected; and
- unit coverage for parsing, polling, validation, station switching, and lifecycle cancellation.

No credentials, cookies, station-issued posting material, live message captures, or account-derived URLs are
committed or logged.

## Verified public protocol

All five stations expose the same public, unauthenticated, same-origin message view under their ClearChat
interface. Sanitized read-only checks found:

- HTTP 200 and `text/html; charset=ISO-8859-1` on all five stations;
- 15 explicit message rows per response;
- separate author and message elements plus a displayed posting timestamp;
- smileys represented as images with textual `alt` values; and
- the public browser interface reloading the message view every 30 seconds.

The Android app therefore makes one read immediately when Chat is selected, no more than one scheduled or manual
read every 30 seconds for the selected station, and stops collection when Chat is left or the foreground UI is no
longer collecting. A user-initiated post performs one form discovery request, one submission, and one immediate
confirmation read, matching the browser interface's post-triggered refresh behavior.

The signed-in browser exposes a same-origin GET form with a 255-character message field and a station-issued
account value. A least-privileged StreamingSoundtracks.com account successfully submitted one clearly identified
harmless protocol-test message. The station-issued value is treated as transient secret material: it is parsed
only from the current authenticated response, used for that submission, never persisted, never logged, and never
exposed to Compose or the ViewModel.

The live test also confirmed that unsupported Unicode punctuation is corrupted by the legacy ISO-8859-1 form.
The native repository checks encodability before transport and reports a user-facing validation error instead of
silently changing the message.

## Implemented safeguards

- Keep Compose dependent only on immutable state and upward actions.
- Keep the ViewModel dependent only on `ChatRepository`.
- Keep authentication cookies inside the data layer and station scope.
- Bound retained messages, author/message lengths, response sizes, timeouts, and redirects.
- Render remote text as plain text; do not execute HTML or introduce a WebView.
- Retain at most 50 parsed messages in memory and never persist chat history.
- Require the exact same-origin public input-frame path and one username/token pair before posting.
- Keep station-issued posting material out of exception messages and object string representations.
- Keep public playback and queue/history independent of chat availability or authentication failure.

Song-request submission remains outside this authorization and is a separate future milestone.

## Validation

Automated, live-protocol, and physical-device evidence is recorded in `docs/m8-validation.md`.
