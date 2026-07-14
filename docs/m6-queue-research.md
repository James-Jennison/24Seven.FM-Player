# M6 queue and history research

Observed and authorized on July 13, 2026. This note contains no cookies, credentials, private messages, device identifiers, or track values.

## Administrator authorization

A 24seven.FM site administrator authorized this unofficial, non-commercial Android app to use the public queue/history interfaces across all five stations. Access is unauthenticated, polling must occur no more than once every 60 seconds, and requester names/messages and cover artwork may be displayed.

The implementation applies a stricter traffic policy: it polls only while Queue is the selected native destination, makes one request for the selected station, shares the same 60-second limit with manual refresh, and stops polling when the destination or foreground lifecycle is no longer collected.

## Public interface evidence

All five station-owned `player.php` pages visibly offer Queue and History. Their legacy network-player launchers redirect to those pages. The player makes a same-page, read-only request with a station code and returns JSON containing `queue_html` and `played_html` fragments.

Initial command-line checks without the player request headers returned server errors on four stations. Repeating the request with the public player's `Referer`, JSON `Accept`, and `X-Requested-With` headers returned HTTP 200 on all five. An honest app-identifying user agent also succeeded; the app does not impersonate a browser.

The queue/history fragments do not require the current album identifier. Omitting that unrelated lookup reduced each poll from two requests to one. A sanitized verification found:

- StreamingSoundtracks.com: 10 queue rows and 10 history rows;
- 1980s.FM: 10 queue rows and 10 history rows;
- Adagio.FM: 9 queue rows and 10 history rows at the observation time;
- Death.FM: 10 queue rows and 10 history rows;
- Entranced.FM: 10 queue rows and 10 history rows;
- no authentication cookies were sent or stored;
- each row consistently exposed an ordering marker, station-hosted cover image, artist text, and track-title text;
- album, duration, and per-row requester fields were not present and are not inferred.

## Implementation decision

The authorized interface is suitable with a defensive adapter. The data layer owns the five public domain/station-code mappings, required request headers, response-size and timeout limits, JSON extraction, and HTML-fragment parsing. Compose and `MainViewModel` never depend on network or parser types.

The parser accepts only rows with a nonblank explicit track-title element, preserves artist and title text without heuristic splitting, ignores scripts and inline event handlers, and accepts artwork only from the selected station domain or its subdomains. Remote errors are replaced by a generic user-facing message.

All five stations have verified queue and history capability flags. If a station changes or removes the public structure, its adapter or capabilities can be changed without affecting playback.

## Extended queue visibility (2026-07-13)

The administrator requested a native display ceiling of 30 tracks so a newly requested song can be verified farther down the queue. The public `Queue_Played` interface exposes longer, explicit queue and played tables for StreamingSoundtracks.com, 1980s.FM, Adagio.FM, and Entranced.FM. At verification time StreamingSoundtracks.com supplied 25 upcoming and 25 played rows; the other stations supplied as many rows as were currently available. The app parses these tables defensively and caps each list at 30.

Death.FM's extended interface did not respond reliably during verification, so its adapter deliberately retains the established compact player feed (up to 10 rows per list). This avoids a second fallback request and preserves the authorization limit: one public read per selected-station refresh, no more often than once every 60 seconds.

Extended rows provide explicit position, duration, album, artist, title, and station-hosted artwork. Requester text is not stored in the current queue domain model and is excluded from track titles.
