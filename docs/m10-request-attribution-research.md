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

The authenticated album page and its request link do not publish a message input, parameter name, or length rule.
No additional live request was submitted during research. Adding a message to a new request remains deferred until
the station supplies or exposes an authoritative request field contract; the app will not guess a mutation
parameter.
