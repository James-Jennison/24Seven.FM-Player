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

On July 13, 2026, the read-only portion was exercised on the Motorola Razr 2023 running API 35 against
StreamingSoundtracks.com. A native title search returned live catalog rows with the correct track, album, and year;
opening a result replaced the search page with its album tracks, artists, durations, and server-derived availability.
The run exposed and fixed legacy outer-table year attribution and initially buried album tracks; sanitized regression
tests now cover both transitions.

The administrator then selected `Kung` from *Bulletproof Monk* for the single approved live request. The signed-in
native flow submitted it once, did not retry, and the track appeared at position 22 in the public queue. The station
accepted the request before the client failed to read its confirmation response, producing a false failure message.
The corrected indeterminate state now tells the listener to check Queue before trying again, clears the confirmation,
and suppresses immediate resubmission. A regression test covers this accepted-but-unconfirmed path. M9 live
validation is complete.
