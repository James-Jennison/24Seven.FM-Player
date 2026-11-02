# TV companion pairing: mobile half (design, not approved)

Status: draft for Owner review, 2026-11-02. Nothing here is implemented or approved. Source of the idea: a tester suggestion relayed by the Owner to the station operator on 2026-11-01; the TV-side evidence is in `docs/CLAUDE_TV_HANDOFF.md` in the Android TV repository.

## Scope chosen by the Owner

The Owner said the first version should need nothing done server-side. That selects the **paired mobile companion**: the TV searches the public catalog (typed or dictated through the Android TV keyboard) and asks for confirmation; the **phone holds the station session and submits the request**. Independent TV sign-in needs station-issued device authorization, which is a server-side change owned by the station operator, so it is out of this version. Cookie copying and a hosted proxy are rejected (credential custody).

## Fixed constraints

- The Player stays a consumer of station data. A request still goes through `MainViewModel.confirmSongRequest`, which checks `communitySafety.canContributeCommunityContent` before `NetworkSongRequestRepository.confirmRequest`. The companion handler must call the same path, never the repository directly.
- A request changes station state. Never retry a submission automatically; an uncertain result stays uncertain and points the listener to the Queue.
- The phone confirms station, account and the exact selected track, and the listener approves each request on the phone or explicitly pre-approves a narrow permission for the paired TV.
- Credentials and cookies never leave the phone. No raw URL-fetch proxy.
- Both apps target SDK 36, so the local-network runtime permission is not enforced yet. Do not add it until the target moves; design for denial and for discovery that works through a system-mediated path.
- Both apps keep their own package IDs.

## Proposed flow (first version, foreground phone)

1. TV shows a short-lived QR code with a one-time pairing secret and its own name.
2. The phone opens the Player's pairing screen, scans the code, and shows the TV name, the station, the signed-in account and the narrow permission ("search results from this TV may ask this phone to submit song requests"). The listener approves.
3. TV and phone exchange messages over an authenticated, encrypted channel. Messages carry a protocol version, an operation ID, expiry, and the station and account they were approved for.
4. TV searches the public catalog and shows the selected album and track. The listener confirms on the TV; the phone shows its own confirmation with the same track.
5. The phone checks the safety gate and submits once. The result (accepted, refused with the station's reason, or uncertain) returns to the TV.
6. Either side can revoke the pairing; pairing expires after a bounded idle period. Signing out on the phone ends it.

## Requirements from the TV-side review (2026-11-02)

Reviewed by the TV writer session; review only, no TV code written, and TV pairing scope is not yet assigned by the Owner.

**Pairing payload and channel**
- QR is the primary route. It carries: protocol version, TV name, TV LAN address and port, a 128-bit one-time secret, a short expiry, and the SHA-256 of a per-pairing TV public key.
- The channel is TLS (or Noise) pinned to that key, so a LAN attacker cannot intercept after the scan. Plain HTTP is not acceptable.
- Fallback: a 6-digit code plus the TV's IP shown under the QR. mDNS discovery is optional and not relied on, because guest Wi-Fi and client isolation break it and it adds a permission story.
- The TV listens only while its pairing or requests screen is open, never from a background service. The phone is foreground-only, so both sides match.
- Every message carries an operation ID and is idempotent. The TV never infers success from silence.

**Decisions taken as defaults (reversible, flagged for Owner review)**
- The TV generates the key, per pairing.
- The TV owns the pairing-code expiry clock (it issues the secret); both sides also enforce a bounded idle expiry on an established pairing.
- A change of station or signed-in account on the phone invalidates any pending operation and returns the TV to the unpaired state.
- The TV shows only a phone-supplied label for the account, never tokens or an email address.

**States both sides must cover**
1. Unpaired: QR with countdown, expiry and regenerate.
2. Paired-idle: phone name, station and account label, Revoke.
3. Search: dictation or keyboard, loading, empty, error, offline.
4. Confirm: the exact track, Confirm and Cancel, then "waiting for phone approval".
5. Result: accepted; refused with the station's reason; **phone unreachable before submit** (nothing was submitted); **unknown after submit** (check the Queue, no automatic retry). These two failure codes are distinct.
6. Revoked, expired or phone signed out: back to state 1.
- Everything is D-pad and Back navigable, and playback continues while these screens are open.

**Unverified and to spike on the physical Streamer before the protocol is fixed**
- Network-security and manifest review for an inbound listener (TV is HTTPS-only today).
- QR rendering library or encoder, with its Gradle verification-metadata entry.
- Multicast on Ethernet versus Wi-Fi, and keyboard dictation behavior.
- Sleep and wake: the listener must rebind and the phone must reconnect.

## Open decisions for the Owner

1. **Transport. Settled by the Owner 2026-11-02: LAN only.** LAN discovery with a direct channel (reachable Ethernet TV and Wi-Fi phone, but guest networks and VPNs break it), or a relay (needs hosting and new privacy obligations). Recommendation: LAN only for the first version.
2. **Phone availability. Settled by the Owner 2026-11-02: foreground only.** Foreground-only first, or a background service. Recommendation: foreground-only; a sleeping phone shows a clear "open the Player on your phone" error.
3. **Which stations and accounts. Settled by the Owner 2026-11-02 (recommendation accepted).** Recommendation: all five stations, only the account currently signed in on the phone, each request approved on the phone.
4. **Pairing entry. Settled by the Owner 2026-11-02: camera QR scan, with the typed code as the fallback.** Implemented with Google Play services' on-device code scanner, so the Player is not granted camera access and requests no camera permission.
5. **Release.** See below.

## 1.0.3 impact (the part to decide first)

1.0.3 is otherwise complete and has not been submitted. Adding this to it changes what must be approved and reviewed:

- `PRIVACY.md`, `docs/m23-data-safety.md` and the Play listing need wording for pairing data (pairing secret, paired TV name, local-network traffic). Those edits need Owner approval.
- Release 1.0.2 took 21 days in Play review, so a larger change can slow the whole release.
- The mobile half cannot be tested end to end until the TV half exists. Until then only unit tests and a fake TV peer are possible.

Recommended way to keep it optional: ship the pairing screen and protocol behind a build flag that is **off in 1.0.3**, with no new permission and no data collected until the flag is on and the TV app supports it. Then 1.0.3 can be submitted on its current schedule, and enabling pairing becomes a small later change.

## Work, in order

1. Owner decisions above.
2. Protocol document and fake TV peer for tests.
3. Pairing and approval screens, revocation, behind the flag.
4. Companion request handler that reuses the existing safety path, with unit tests for refusal, expiry, duplicate operation IDs and ambiguous results.
5. `PRIVACY.md` / Data Safety / listing text for Owner approval before the flag is turned on.
6. Joint physical acceptance with the TV app on the Streamer and a phone.
