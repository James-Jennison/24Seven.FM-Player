# M9 native song-request research

## Authorization and limits

On July 13, 2026, a 24seven.FM site administrator authorized this unofficial, non-commercial native Android app
to research and use the public-facing song-request interfaces across all five stations. Authenticated browsing,
searching, and explicit user-initiated submissions are permitted. One clearly identified administrator-approved
request may be used for least-privileged validation. Existing station eligibility rules and cooldowns must be
preserved; automated, repeated, and bulk requests are prohibited.

## Verified public behavior

StreamingSoundtracks.com, 1980s.FM, Adagio.FM, Death.FM, and Entranced.FM expose the same native-adaptable flow:

1. `GET /modules.php?name=Requests` displays rules and public catalog search.
2. Search is a read-only GET to `/modules.php` with `name=Requests`, a search term, and one of the verified
   `title`, `album`, `artist`, or `genre` modes.
3. Search results lead to station-owned album detail pages.
4. Album track rows expose server-computed request availability. Available tracks have a same-origin request
   action carrying the album and numeric song identifiers; unavailable tracks have no action.
5. The request action itself is a state-changing GET. The Android app therefore adds an explicit native
   confirmation dialog before making exactly one authenticated request.

The sites state that an artist or album already in the queue cannot be requested and that the queue has a minimum
one-hour duration. Queue length, long tracks, membership level, prior requests, and station policy can also affect
eligibility. These rules remain server-authoritative; the app does not reproduce, weaken, or work around them.

## Production safety contract

- Catalog reads occur only after a user presses Search or opens an album. There is no polling or prefetch loop.
- A submission requires a signed-in station-specific protected session, an available track from the immediately
  parsed album page, a visible confirmation dialog, and a second explicit user action.
- Each confirmation produces at most one network request. Submission failures are never retried automatically.
- Same-origin HTTPS checks, bounded responses, timeouts, safe identifier validation, and plain-text rendering are
  enforced in the data layer.
- Cookies remain in Android-protected storage through the M7 session contract. Credentials, cookies, tokens,
  captured pages, request history, and browsing history are not logged or committed.
- Search results and album tracks are transient UI state. The app does not persist or batch them.

## Completed live validation

No song was submitted during protocol discovery. For the later authorized end-to-end check, the administrator chose
`Kung` from *Bulletproof Monk* on StreamingSoundtracks.com. The native app issued one request and did not retry. The
track was verified at position 22 in the public queue. Because the server accepted the mutation before the client
failed to read its confirmation, indeterminate outcomes now direct the listener to Queue and suppress immediate
resubmission. Sanitized tests cover parsing, confirmation gating, and both confirmed and indeterminate no-retry
outcomes.
