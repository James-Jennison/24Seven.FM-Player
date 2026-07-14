# M10 validation

Validated July 14, 2026 against a physical Android 15 (API 35) Motorola Razr 2023.

- The live public queue displayed requester identity and its separate message without merging either into track metadata.
- The authenticated StreamingSoundtracks.com post-request form was inspected read-only before controlled live validation.
- The native confirmation dialog renders the optional message field only for the verified station, enforces the published 80-character limit, and emits the value only on explicit confirmation.
- The first live message attempt did not appear in Queue. The adapter was corrected to mirror `msg`, `send`, and `remLen`, preserve the referer, and upgrade only the station's same-host legacy HTTP redirect to its verified HTTPS equivalent.
- A second administrator-approved Razr attempt queued the selected song and displayed `Requested by MorgHubby`, but no message. The app reported an indeterminate song-response result. Inspection confirmed that the station accepted the one-shot song mutation while the client was still waiting for its response, so the previous sequence exited before reaching the separate message POST.
- The corrected sequence now allows a non-blank, explicitly confirmed message to be posted once after an indeterminate song response while never retrying the song mutation. Blank messages still make no second request, and a focused network-adapter test verifies exactly one song GET followed by exactly one message POST when the first response times out.
- Unit tests, Android lint, and release assembly pass.
- All 10 instrumentation tests pass on the Razr. AGP 8.13's Windows UTP profile writer cannot create a filename containing a wireless ADB serial's colon, so the already-built debug and test APKs were installed and the same AndroidJUnitRunner suite was invoked directly on the device.

M10 remains in progress until the corrected sequence's message is visible on a future eligible real queued request.
The confirmed request was not retried and must not be resubmitted for validation.
