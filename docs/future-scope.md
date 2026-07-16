# Future product scope

## Completed scope reference — Request messages

StreamingSoundtracks.com's exact optional-message form is implemented and verified behind a station capability. The other four stations remain disabled until their own post-request forms are independently verified.

## M17 — Private Messages

Signed-in members should be able to use a fully native station-scoped Private Messages experience: Inbox, message reading, composing, replying, explicit user-initiated sending, and receiving/refreshing. Least-privileged protocol research and use are authorized, but implementation remains deferred until the underlying website/server issues are fixed. Production refresh limits, notification behavior, local retention, deletion semantics, attachment scope, and cross-station account behavior must be defined before implementation. Message bodies must not be logged, committed, or retained outside the minimum product requirement; protected station sessions remain in Android-protected storage.

## M25 — Chat Mention Notifications

After the M24 Alpha publication gate, add an opt-in notification path for exact mentions of the signed-in member's station display identity in Chat. The experience must remain station-scoped, ignore locally blocked authors, suppress duplicates, expose per-station controls and a dedicated Android notification channel, and deep-link to the originating station's Chat without bypassing the age, Terms, or mature-content gates.

M25 begins with delivery research rather than Android UI implementation. The current application has no developer-operated backend and intentionally observes Chat only while Chat is selected. True push therefore requires an authorized station-side event source, webhook, or privacy-compatible relay. Perpetual background polling, misleadingly periodic WorkManager checks, or forwarding protected station sessions to a relay are not acceptable substitutes. Before implementation, document payload minimization, identity matching, authentication, retention/deletion, abuse controls, outage and duplicate behavior, traffic/battery impact, station support differences, and the resulting privacy and Google Play Data Safety declarations.
