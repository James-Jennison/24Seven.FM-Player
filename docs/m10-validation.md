# M10 validation

Validated July 14, 2026 against a physical Android 15 (API 35) Motorola Razr 2023.

- The live public queue displayed requester identity and its separate message without merging either into track metadata.
- The authenticated StreamingSoundtracks.com post-request form was inspected read-only; no second live song request or message was submitted.
- The native confirmation dialog renders the optional message field only for the verified station, enforces the published 80-character limit, and emits the value only on explicit confirmation.
- The first live message attempt did not appear in Queue. The corrected adapter now mirrors `msg`, `send`, and `remLen`, preserves the referer, and upgrades only the station's same-host legacy HTTP redirect to its verified HTTPS equivalent.
- A local network-adapter test verifies the redirect upgrade followed by exactly one form-encoded message POST with the authoritative path and all successful controls.
- Unit tests, Android lint, and release assembly pass.
- All 10 instrumentation tests pass on the Razr. AGP 8.13's Windows UTP profile writer cannot create a filename containing a wireless ADB serial's colon, so the already-built debug and test APKs were installed and the same AndroidJUnitRunner suite was invoked directly on the device.

M10 remains in progress until the corrected build's message is visible on a real queued request. The app never
automatically retries the song mutation.
