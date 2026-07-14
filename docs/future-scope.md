# Future product scope

## Request messages

StreamingSoundtracks.com's exact optional-message form is implemented behind a station capability, but the corrected legacy-redirect handling still needs one queued-message confirmation. The other four stations remain disabled until their own post-request forms are independently verified.

## Private Messages

Signed-in members should be able to use a fully native station-scoped Private Messages experience: Inbox, message reading, composing, replying, sending, and receiving/refreshing. This requires explicit authorization and least-privileged protocol research for the authenticated PM interfaces before implementation. Define production refresh limits, notification behavior, local retention, deletion semantics, attachment scope, and cross-station account behavior first. Message bodies must not be logged, committed, or retained outside the minimum product requirement; protected station sessions remain in Android-protected storage.
