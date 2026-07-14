# M10 validation

Validated July 14, 2026 against a physical Android 15 (API 35) Motorola Razr 2023.

- The live public queue displayed requester identity and its separate message without merging either into track metadata.
- The authenticated StreamingSoundtracks.com post-request form was inspected read-only before controlled live validation.
- The native confirmation dialog renders the optional message field only for the verified station, enforces the published 80-character limit, and emits the value only on explicit confirmation.
- The first live message attempt did not appear in Queue. The adapter was corrected to mirror `msg`, `send`, and `remLen`, preserve the referer, and upgrade only the station's same-host legacy HTTP redirect to its verified HTTPS equivalent.
- A second administrator-approved Razr attempt queued the selected song and displayed `Requested by MorgHubby`, but no message. The app reported an indeterminate song-response result. Inspection confirmed that the station accepted the one-shot song mutation while the client was still waiting for its response, so the previous sequence exited before reaching the separate message POST.
- A subsequent live attempt after the first timeout fallback was installed also queued the song without its message. Read-only inspection confirmed that the authenticated website loads the per-track `writemessage` form before the separate `submitmessage` POST. The timeout fallback had skipped that form load, making it the remaining verified mismatch with the browser workflow.
- The corrected sequence now performs exactly one song attempt, one authenticated form read, and one message POST after an indeterminate song response. It validates the expected `msg`, `send`, and `remLen` controls before posting and never retries the song mutation. Blank messages still make no second request.
- Unit tests, Android lint, and release assembly pass.
- All 10 instrumentation tests pass on the Razr. AGP 8.13's Windows UTP profile writer cannot create a filename containing a wireless ADB serial's colon, so the already-built debug and test APKs were installed and the same AndroidJUnitRunner suite was invoked directly on the device.

M10 remains in progress until this form-loading recovery sequence's message is visible on a future eligible real
queued request. Previously confirmed requests were not retried and must not be resubmitted for validation.
