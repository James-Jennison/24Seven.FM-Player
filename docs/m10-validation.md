# M10 validation

Validated July 14, 2026 against a physical Android 15 (API 35) Motorola Razr 2023.

- The live public queue displayed requester identity and its separate message without merging either into track metadata.
- The authenticated StreamingSoundtracks.com post-request form was inspected read-only before controlled live validation.
- The native confirmation dialog renders the optional message field only for the verified station, enforces the published 80-character limit, and emits the value only on explicit confirmation.
- The first live message attempt did not appear in Queue. The adapter was corrected to mirror `msg`, `send`, and `remLen`, preserve the referer, and upgrade only the station's same-host legacy HTTP redirect to its verified HTTPS equivalent.
- A second administrator-approved Razr attempt queued the selected song and displayed `Requested by MorgHubby`, but no message. The app reported an indeterminate song-response result. Inspection confirmed that the station accepted the one-shot song mutation while the client was still waiting for its response, so the previous sequence exited before reaching the separate message POST.
- A subsequent live app attempt also queued its song without a message. The form-loading recovery hypothesis was disproved by a fresh, controlled browser workflow rather than another speculative app submission.
- The fresh browser request selected `Just Testing` by Maurice Jarre from *Soleil Rouge*. The request link used station song ID `263260`, while the resulting server-generated message form used a distinct message-record ID, `2055716`. Submitting `M10 fresh browser workflow` through that fresh form returned `Your message has been saved`, and the public Queue displayed the exact message beside `Requested by MorgHubby`.
- This proves the previous adapter's root defect: it reused the song ID as the message form's `id`. The corrected adapter extracts the action from the accepted response, requires the exact same-origin Album `submitmessage` route, matching album ID, numeric server-generated message ID, and the `msg`, `send`, and `remLen` controls, then posts once with the actual response URL as referer.
- The live response also exposed a legacy `http://www.streamingsoundtracks.com` redirect. The adapter accepts only that verified SST alias, canonicalizes it to `https://streamingsoundtracks.com`, and keeps the other stations restricted to their configured hosts.
- If the song response is unreadable, the app no longer guesses a message ID or attempts a message POST. The song request is never retried. The read timeout is 60 seconds to accommodate the slow station response while preserving this safety rule.
- Station success pages are treated as reported, not independently verified. A complete optional-message form without an explicit request-success phrase is rejected, and the UI directs the listener to confirm Queue before requesting again.
- Final native verification used the VIP account's explicit least-played suggestion flow. `Clipped Ears` by Nicholas Pike from *For the Love of Spock* entered the public Queue with `Requested by MorG` and the exact message `M10-VIP-least-2-20260714`. The same requester and message rendered independently in the native Queue on the Razr.
- Unit tests, Android lint, and release assembly pass.
- All 10 instrumentation tests pass on the Razr. AGP 8.13's UTP profile writer could not create a filename containing a wireless ADB serial's colon, so the already-built debug and test APKs were installed and the same AndroidJUnitRunner suite was invoked directly on the device.

M10 is complete. Previously confirmed requests were not retried and must not be resubmitted for validation.
