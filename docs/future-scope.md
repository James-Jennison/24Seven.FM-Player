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

## Completed M26 — In-App Diagnostics

The native More screen now provides an explicit, user-reviewed diagnostic snapshot and Android copy/share flow. A fixed allowlist covers app/build, Android/API and coarse device class, selected station, bounded playback/error category, validated-network availability, broad route type, and at most five enum-only recent transitions. Raw errors and human-readable route names are reduced to categories, and credentials, sessions, CSRF values, account/community/report content, endpoints, signing data, identifiers, and raw logs have no report input path. The formatter/redaction suite, full 141-test unit suite, lint, focused physical-Razr Compose test, clipboard confirmation, and system Share chooser pass. See [M26 validation](m26-diagnostics-validation.md).

## M27 — Community Push Notifications

M27.1 is complete for the existing actively observed Chat feed. The app offers station-scoped opt-in controls, performs case-insensitive exact-display-name matching on-device, establishes the first snapshot as a no-alert baseline, ignores locally blocked authors and the signed-in user's own posts, suppresses duplicate snapshots with bounded memory-only fingerprints, posts through a dedicated private Android notification channel without message text, and routes a tap to the originating station's gated Chat screen. It does not increase the existing 30-second polling cadence or observe Chat outside the existing Chat-selected lifecycle.

Before M28 Alpha publication, add an opt-in notification path for exact mentions of the signed-in member's station display identity in Chat. The experience must remain station-scoped, ignore locally blocked authors, suppress duplicates, expose per-station controls and dedicated event channels, and deep-link to the originating station's Chat without bypassing the age, Terms, or mature-content gates.

The same architecture must support opt-in new-Private-Message notifications once M17 is repaired and implemented. If M17 ships before M28, live new-message push becomes part of M27's completion gate. If M17 remains deferred, the PM event type, controls, deep-link contract, and tests remain capability-gated with M17 and do not expose a nonfunctional Inbox.

M27.2 begins with delivery research. The current application has no developer-operated backend and intentionally observes Chat only while Chat is selected. True push therefore requires an authorized station-side event source, webhook, or privacy-compatible relay. Perpetual background polling, misleadingly periodic WorkManager checks, or forwarding protected station sessions to a relay are not acceptable substitutes. Before implementation, document payload minimization, Chat/PM event separation, lock-screen privacy, identity matching, authentication, retention/deletion, abuse controls, outage and duplicate behavior, traffic/battery impact, station support differences, and the resulting privacy and Google Play Data Safety declarations.

## M28 — Alpha Publication

After M23–M27 close, rebuild and revalidate the signed candidate, declarations, device coverage, reviewer instructions, pre-launch results, and Play-delivered install/update behavior. Publication remains an explicit owner-authorized internal or closed testing action, never an automatic consequence of a green local build.
