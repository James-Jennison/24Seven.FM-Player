# M7 authentication research

## Authorization

On July 13, 2026, a station administrator authorized this unofficial, non-commercial native Android app to use
the public-facing login and session interfaces across all five stations. User-entered credentials may be
submitted, resulting sessions may be retained in Android-protected storage, and login research plus
least-privileged testing are permitted.

Accounts are currently station-specific, although the administrator noted that this may change. The app must
therefore keep authentication station-scoped while allowing a future shared-account implementation behind the
same repository contract.

This authorization does not yet cover chat reads/writes or song-request submission.

## Scope and boundary

M7 adds native account authentication under the authorization above. The earlier authorization for public
queue and history polling remains separate and does not authorize chat or request submission.

The implementation includes a station-scoped `AuthRepository`, immutable authentication states, a native
security challenge and credential form, strict same-origin transport, signed-in response classification, and
Android-protected session retention. No credentials, challenge answers, cookies, tokens, or captured traffic
are committed.

Public station pages state that registered members can use chat and song requests. Authentication does not
authorize those features, and account/session sharing between stations is not assumed.

## Public login protocol evidence

On July 13, 2026, the public home page for each station exposed the same login form shape:

| Station | Verified HTTPS origin | Method and public action |
| --- | --- | --- |
| StreamingSoundtracks.com | `https://streamingsoundtracks.com/` | `POST /modules.php?name=Your_Account` |
| 1980s.FM | `https://1980s.fm/` | `POST /modules.php?name=Your_Account` |
| Adagio.FM | `https://adagio.fm/` | `POST /modules.php?name=Your_Account` |
| Death.FM | `https://death.fm/` | `POST /modules.php?name=Your_Account` |
| Entranced.FM | `https://entranced.fm/` | `POST /modules.php?name=Your_Account` |

Each form uses `username`, `user_password`, `gfx_check`, a transient six-digit `random_num`, and the operation
`login`. The page renders a security-code image derived from the transient challenge, so native login must show
that image and require the user to enter its code. Challenge values must remain memory-only and must never be
logged or committed. A fresh anonymous page load did not set a cookie on any of the five origins.

The `www.streamingsoundtracks.com` HTTPS host presented a certificate-name mismatch to the Windows client. The
certificate-valid canonical origin is `https://streamingsoundtracks.com/`, which served the matching form. The
app must not disable certificate validation or send credentials to the mismatched `www` origin.

A least-privileged StreamingSoundtracks.com account was subsequently used through the visible in-app browser,
with credentials entered directly by the administrator. Successful login redirected to the account profile.
Both the profile and a later home-page navigation displayed the matching welcome identity and same-origin
logout action, while the anonymous password field was absent. These three conditions form the native success
rule; no cookie values or stored password data were inspected.

The legacy signed-in page also contained an unrelated link with an account-derived value in its URL. The app
must treat that value as sensitive, ignore unrelated links when classifying authentication, and never store,
log, or expose it.

On July 13, 2026, native sign-in was successfully completed on the Motorola Razr 2023 using the least-privileged
StreamingSoundtracks.com account. The administrator entered credentials and the case-sensitive alphanumeric
security code directly on the device. The app displayed the signed-in account state without retaining the
password or security-code input. No credential, challenge answer, cookie value, or account-derived URL value
was captured or committed.

The station's legacy session cookie was host-only and did not carry a `Secure` attribute. The first persistence
implementation correctly rejected it but therefore could not restore the session. The revised store accepts
cookies only from the station-isolated HTTPS client, normalizes host-only scope to the selected station's exact
domain, and upgrades the restored cookie to HTTPS-only before encrypting it. The signed-in display identity is
encrypted separately; passwords and security-code answers are never persisted.

After signing in with the revised build, a real application process stop and restart on the Razr restored the
signed-in identity automatically. The Android Keystore instrumentation test also verifies encrypted cookie and
identity round-trip, exact-domain enforcement, HTTPS-only restoration, and clearing.

Signing out returned the native account area to a fresh sign-in state. A subsequent real process stop and
restart remained signed out, confirming that the in-memory cookie manager and encrypted cookie plus identity
were cleared. Restored sessions are checked against the selected station when the network is available. A
reachable anonymous response clears stale protected state; a temporary network failure retains the encrypted
identity and does not block public playback.

## Confirmed and follow-up protocol questions

The administrator has confirmed login/session permission and station-specific accounts. Protocol research must
still determine:

1. Whether an app-specific API exists and is preferred over the legacy browser form.
2. Any login rate limit, CAPTCHA, multi-factor authentication, lockout, or automated-client restriction.
3. The server-defined natural session lifetime.
4. Whether disabled accounts have a distinct response from other anonymous sessions.
5. Whether registration and password recovery must remain external website flows.

Authorization for chat reads/writes and song requests will be documented separately in their own milestones.

## Implemented safeguards

- Compose owns only transient field state and emits sign-in and sign-out actions upward.
- `MainViewModel` depends only on `AuthRepository`.
- The data layer owns redirects, challenge handling, cookies, expiry detection, and response limits.
- Passwords and security-code answers are used only for the active sign-in operation and are not persisted or logged.
- Persisted session material uses Android platform-protected storage and is cleared on logout or invalidation.
- Public playback and public queue/history will continue working while signed out or after session failure.
- Station authentication capability flags are enabled for the five verified matching public form interfaces.

## Validation coverage

- Unit tests for state transitions, response classification, expiry, logout, and secret redaction.
- Integration tests against sanitized local fixtures; no captured live session values.
- Compose tests for validation, progress, errors, signed-in identity, and logout.
- A least-privileged physical-device login, process restoration, logout, and cleared-restart pass on the Motorola Razr 2023.
- Parser-backed invalidation for a reachable anonymous restoration response; natural server expiry remains follow-up evidence.
- Regression validation for public playback and queue/history while signed out.

## Validation

The native flow, protected session restoration, sign-out clearing, and regression coverage are recorded in
`docs/m7-validation.md`.
