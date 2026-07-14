# M7 authentication validation

## Result

M7 native station authentication is complete. The implementation remains fully native and station-scoped,
and it does not expand authorization to chat or song-request submission.

## Automated coverage

- Login-page parsing accepts the verified form fields and same-origin challenge image while rejecting missing,
  duplicate, cross-origin, oversized, or malformed content.
- Signed-in parsing requires the expected welcome identity, absence of the anonymous password field, and a
  same-origin logout action. Unrelated account-derived links are ignored.
- Repository tests cover challenge loading, sign-in transitions, failures, protected-session restoration, and
  sign-out state clearing.
- Compose tests cover the authentication states and native account controls.
- The Android Keystore test covers encrypted cookie and display-identity round-trip, host-only cookie
  normalization to the exact station domain, HTTPS-only restoration, and clearing.
- Unit tests for debug and release, Android lint, debug assembly, and the six API 35 connected tests pass.

## Physical-device evidence

On July 13, 2026, a least-privileged StreamingSoundtracks.com account completed native sign-in on the Motorola
Razr 2023 running Android 15 / API 35. The administrator entered the username, password, and case-sensitive
alphanumeric security code directly on the device. The app showed the signed-in identity without persisting the
password or security-code answer.

After a real application process stop and restart, the encrypted station session restored the signed-in state.
After native sign-out, the account area returned to a fresh sign-in state; another real process stop and restart
remained signed out. No credentials, challenge answers, cookie values, or account-derived URL values were
captured in test output or committed.

Natural server expiry was not waited out. The expiry path uses the same strict signed-in response parser during
online restoration: a reachable anonymous response clears protected session state, while a temporary network
failure preserves the protected cached identity and leaves public playback available. A future supervised run
may record the server's natural session lifetime without changing the M7 boundary.

## Remaining boundaries

- Accounts and sessions remain station-specific until shared behavior is explicitly verified.
- Registration and password recovery remain website flows.
- Chat and song-request interfaces require separate authorization and protocol milestones.
