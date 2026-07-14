# M9 validation status

M9 implements native catalog search, album browsing, server-derived track availability, protected-session request
submission, explicit confirmation, and a strict no-retry policy across the shared five-station contract.

Automated coverage verifies:

- same-origin search parsing and rejection of foreign album links;
- available and disabled track parsing from sanitized album fixtures;
- exact album/song identifier preservation;
- no background search or polling;
- submission cannot occur before a track is prepared for confirmation;
- repeated confirmation after completion cannot produce another submission;
- station request capabilities and immutable UI state wiring;
- a visible native confirmation dialog before the send action.

The remaining device check is intentionally blocked on choosing one administrator-approved station and track. It
must verify a real signed-in browse/search/album/confirm flow, one submission, the returned station notice, and the
resulting cooldown or queue state without sending a second request.

On July 13, 2026, the read-only portion was exercised on the Motorola Razr 2023 running API 35 against
StreamingSoundtracks.com. A native title search returned live catalog rows with the correct track, album, and year;
opening a result replaced the search page with its album tracks, artists, durations, and server-derived availability.
No song was submitted. The run exposed and fixed legacy outer-table year attribution and initially buried album
tracks; sanitized regression tests now cover both transitions. The full connected suite passed eight tests on the
same device.
