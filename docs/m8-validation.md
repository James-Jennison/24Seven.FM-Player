# M8 chat validation

## Result

M8 native station chat is complete. Chat reading is enabled across all five stations under the documented
administrator authorization. Posting is exposed only when the selected station has a protected signed-in session.
No WebView, persisted chat history, captured session material, or live message fixture was added.

## Automated coverage

- The parser reads only explicit message rows, authors, messages, displayed timestamps, and smiley `alt` text.
- Malformed and unrelated markup is ignored, lengths are bounded, and at most 50 messages remain in memory.
- The posting-form parser requires one exact same-origin public input frame and rejects missing, duplicate, or
  cross-origin account material. Its string representation is redacted.
- Repository tests verify immediate reads, the shared 30-second automatic/manual limit, destination cancellation,
  station switching, message sending, generic errors, and ISO-8859-1 validation before transport.
- Compose coverage verifies native message rendering, composer input, and upward send actions.
- Debug and release unit tests, Android lint, debug assembly, and all seven API 35 connected tests pass.

## Live protocol evidence

On July 13, 2026, sanitized read-only requests to StreamingSoundtracks.com, 1980s.FM, Adagio.FM, Death.FM, and
Entranced.FM each returned HTTP 200, ISO-8859-1 HTML, 15 explicit message rows, and the public interface's
30-second reload behavior. No authenticated values were used for these reads.

One clearly identified harmless browser-form test established the posting form shape and its legacy character-set
behavior. One subsequent ASCII-only post from the native app verified protected-session sharing, exact same-origin
form discovery, transient station-issued account material, submission, and confirmation reading. No credentials,
cookies, posting values, captured chat HTML, or participant message content were retained in the repository.

## Physical-device evidence

The debug build ran on the Motorola Razr 2023 with Android 15 / API 35. The native Chat destination displayed the
live author, message, timestamp, and smiley text fields; retained the existing mini-player and navigation; and
showed a sign-in notice while signed out.

After the administrator completed native sign-in directly on the phone, the composer appeared and the authorized
test post reached the public feed. A real application force-stop and relaunch restored the protected session,
reloaded the live feed, displayed the submitted message, and restored the composer without another login.

## Preserved boundaries

- Scheduled and manual reads share a 30-second minimum interval and run only while Chat is selected and collected.
- A user post adds one form-discovery request, one submission, and one confirmation read.
- Messages remain memory-only; leaving the process discards the in-memory list.
- Unsupported Unicode is rejected rather than silently corrupted by the legacy ISO-8859-1 interface.
- Song-request submission remains a separate, unauthorized future milestone.
