# Future product scope

## Completed scope reference — Request messages

StreamingSoundtracks.com's exact optional-message form is implemented and verified behind a station capability. The other four stations remain disabled until their own post-request forms are independently verified.

## M17 — Private Messages

Signed-in members should be able to use a fully native station-scoped Private Messages experience: Inbox, message reading, composing, replying, explicit user-initiated sending, and receiving/refreshing. Least-privileged protocol research and use are authorized, but implementation remains deferred until the underlying website/server issues are fixed. Production refresh limits, notification behavior, local retention, deletion semantics, attachment scope, and cross-station account behavior must be defined before implementation. Message bodies must not be logged, committed, or retained outside the minimum product requirement; protected station sessions remain in Android-protected storage.

When M17 becomes available, new-message events must integrate with the opt-in M27 Community Push Notifications architecture. A notification may identify the station and sender only within the approved minimal payload, must respect blocked users and safety gates, and must deep-link through authentication into the originating Inbox without exposing message text on a lock screen unless the user explicitly chooses a more revealing privacy setting.

## Completed M24 — Sleep Timer

The service-owned sleep countdown is complete with accessible presets and custom duration, remaining-time state, adjust/cancel actions, MediaSession/system cancellation state, service-recreation recovery, and deterministic expiry through the existing player. Pause and station switches preserve the deadline; manual Stop cancels it; monotonic elapsed time includes device sleep; inconsistent post-reboot/wall-clock state fails safe. See [M24 validation](m24-sleep-timer-validation.md).

## Completed M25 — Cast / Audio-Output Selection

The dedicated Android audio-output path is complete. The Player exposes immutable current-route state and opens Android's native system chooser for device, Bluetooth, wired/USB, and system-managed remote routes while preserving the existing MediaSession and audio-focus owner. No active discovery scan, relay, proxy, or new endpoint was added. Google Cast remains intentionally capability-gated because its separate receiver compatibility and permitted stream use are not verified. Routing is not described as securing or proxying the source: Media3 still connects directly to the station-provided HTTP audio relays, while non-audio station traffic remains HTTPS. See [M25 validation](m25-audio-output-validation.md).

## M26 — In-App Diagnostics

Add an explicit, user-controlled diagnostic snapshot and copy/share flow for support. Candidate fields include app/build version, Android/device class, selected station, bounded playback state and error category, network availability, current route type, and recent non-sensitive state transitions. Never include credentials, cookies, CSRF values, Chat or Private Message bodies, abuse-report content, signing data, private endpoints, or raw network/log captures. Complete a redaction and privacy/Data Safety review before shipping.

## M27 — Community Push Notifications

Before M28 Alpha publication, add an opt-in notification path for exact mentions of the signed-in member's station display identity in Chat. The experience must remain station-scoped, ignore locally blocked authors, suppress duplicates, expose per-station controls and dedicated event channels, and deep-link to the originating station's Chat without bypassing the age, Terms, or mature-content gates.

The same architecture must support opt-in new-Private-Message notifications once M17 is repaired and implemented. If M17 ships before M28, live new-message push becomes part of M27's completion gate. If M17 remains deferred, the PM event type, controls, deep-link contract, and tests remain capability-gated with M17 and do not expose a nonfunctional Inbox.

M27 begins with delivery research rather than Android UI implementation. The current application has no developer-operated backend and intentionally observes Chat only while Chat is selected. True push therefore requires an authorized station-side event source, webhook, or privacy-compatible relay. Perpetual background polling, misleadingly periodic WorkManager checks, or forwarding protected station sessions to a relay are not acceptable substitutes. Before implementation, document payload minimization, Chat/PM event separation, lock-screen privacy, identity matching, authentication, retention/deletion, abuse controls, outage and duplicate behavior, traffic/battery impact, station support differences, and the resulting privacy and Google Play Data Safety declarations.

## M28 — Alpha Publication

After M23–M27 close, rebuild and revalidate the signed candidate, declarations, device coverage, reviewer instructions, pre-launch results, and Play-delivered install/update behavior. Publication remains an explicit owner-authorized internal or closed testing action, never an automatic consequence of a green local build.
