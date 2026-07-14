# M10 request attribution research

## Authorization

The existing administrator authorization permits requester names and request messages from the public Queue and
History interfaces to be displayed in this unofficial, non-commercial Android app. The existing 60-second shared
automatic/manual refresh limit remains unchanged.

## Verified public representation

On July 14, 2026, a read-only check of the public `Queue_Played` interface confirmed that an attributed row exposes
one `req-text` element. The requester is explicit profile-link text following `Request By:`. When a member supplied
a message, it appeared as a separate italic child after the requester. Rows without a request have no attribution
element. No cookie or authenticated value is needed to read this data.

The parser therefore keeps attribution separate from the track title, accepts only the explicit `req-text`
structure, bounds the requester to 80 characters and the optional message to 240 characters, and renders both as
plain text. It does not infer a requester from other links or community data. Death.FM continues to use its compact
feed, which does not expose per-row attribution.

## Optional message submission

On July 14, 2026, the administrator exposed the authenticated StreamingSoundtracks.com screen shown immediately
after a request is accepted. Its visible confirmation states that the request has already been delivered, then
offers a separate optional message form. The form uses `POST` to the same-origin Album module's `submitmessage`
action with the verified album and numeric song identifiers. It names the message field `msg`, includes the submit
value `send=Send`, and its published counter truncates input at 80 characters.

The native confirmation dialog mirrors that 80-character limit. A single explicit confirmation first performs the
existing one-shot song request; only after the station returns an accepted result does a non-blank message produce
one same-origin authenticated form post. Blank messages produce no second request. A message-post failure never
retries the song request and is reported separately from the already accepted request. The message remains
transient and is neither logged nor persisted.

The first live attempt queued the song but did not display its message. Follow-up inspection found two legacy-form
details missing from the initial adapter: the browser also submits the read-only remaining-character control, and
the station redirects the accepted HTTPS request to a same-host HTTP message page. The corrected adapter submits
all three successful controls (`msg`, `send`, and `remLen`) and upgrades only that same-host legacy redirect back to
the independently verified HTTPS form. Protected cookies are never sent over HTTP. Final queue confirmation of the
corrected build remains outstanding, so M10 stays in progress.

Only StreamingSoundtracks.com advertises the request-message capability because that is the station whose exact
authenticated form contract was inspected. The other four stations retain native song requesting without the
message field until their post-request forms are independently verified. No additional live song request was
submitted during this research.
