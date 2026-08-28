# Privacy Reconciliation Record

This non-public record separates app-privacy facts from the protected
tester-program policy before the public privacy notice is changed.

## Designated sources

| Scope | Candidate authority | State |
| --- | --- | --- |
| Native Player behavior and app privacy | `PRIVACY.md` at `main` commit `56dc94694d158132befc22f7e97113aefc28af93` | Pinned, digest-verified, and content-reviewed; it does not establish an operational station-retention term. |
| Legacy portal combined notice | `PRIVACY.md` at portal commit `c00c2f4982232e7c197aa0e35d174d7c3478436e` | Current legacy candidate; it cannot govern native facts and needs an explicit public disposition. |
| Tester-program privacy addendum | Existing `Closed-test tester-interest form` section in the portal legacy notice | Exact source extract and content review complete; its 90-day broader-program promise remains pending named-owner attestation because every covered record lacks an enforcing mechanism. |
| Public privacy page | Generated site output | Must not choose between, omit, or merge the two sources without the reviews below. |

## Verified app-behavior drift

The pinned native source describes an optional, user-started, visible
foreground Chat monitor for one signed-in station. It polls that station's Chat
about once a minute while active and shows a persistent Stop control. The
legacy portal notice instead says there is no background polling. These are
contradictory factual claims about the same app behavior. The native source
also documents the related foreground special-use service, app-feedback draft,
and additional bounded diagnostic timing detail. These are native behavior
claims, so the pinned native source governs their eventual public wording.

The legacy statement is not made harmless merely by identifying the native
source. Before a revised public privacy page is published, the reviewer must
inventory every public route or generated artifact carrying the legacy notice
and record its removal, redirect, or explicit supersession. Until then, it
remains a publication blocker.

The current confirmed source and public route are recorded in
`PRIVACY_LEGACY_SURFACE_INVENTORY.md`. That inventory is a bounded observation,
not proof that every cache, historical artifact, or external copy has already
been retired.

## Tester-program retention classification

The portal code sets `CHAT_RETENTION_DAYS` to 90 and calls
`chatPurgeExpired()` to delete expired chat messages and empty threads during
portal handling. That is evidence for the chat-message mechanism only; it is
not evidence of a scheduled clock-driven purge, nor does it establish timely
deletion or anonymization of tester intake, assignments, feedback, invitation
correspondence, or mailbox copies. The legacy statement that all such records
are removed or anonymized within 90 days is therefore an operational promise,
not a code-derived fact. It requires the same named owner attestation as the
station retention claims.

## Operational-policy conflict requiring review

The pinned native source says station-side retention periods are unknown and
directs station-data requests to the network-authorized contact. The existing
portal-branch notice instead makes specific 30-day, 90-day, and one-year
retention/deletion promises. Those are controller/operations facts, not facts
that can be derived from app code, and neither source may silently override the
other.

Before publication, the responsible station/network owner must confirm the
actual retention/deletion policy and the supported request contact. The named
tester-program owner must separately confirm the tester-program retention,
deletion, and mailbox-handling process. The reviewer must not infer either
attestation from a source file or a foreign-key cascade.

The absence of that attestation means the legacy numerical claims are
unverified; it does not establish that they are false. If an owner confirms a
served claim is inaccurate, an interim user-facing correction may be proposed
for that exact claim, but it still requires its own approved production release
and rollback plan. The complete replacement notice remains subject to the full
publication sequence below.

An earlier numerical-policy record does not resolve this review: its
station-side day counts were later replaced in the current native privacy
authority with “retention periods are currently unknown.” The current pin
therefore governs until a named owner records a newer attestation. The owner
must also set an expedited response deadline for the live numerical claims;
that deadline is an operational decision and cannot be invented from history.

## Approved attestation deadline

The designated operational owner, James Jennison, approved an expedited
deadline of **September 1, 2026 at 5:00 PM PDT (UTC-07:00)** to record the
operational attestation. This records a deadline only; it does not assert that
the existing 30-day, 90-day, or one-year claims are accurate, and it does not
change any public page.

The attestation must identify evidence for both of these scopes:

1. Station retention, deletion, and the supported privacy-request contact.
2. Tester-program retention, deletion or anonymization, and mailbox handling.

On August 28, 2026, the designated owner confirmed that the currently served
30-day and 90-day station and tester-program retention claims are accurate.
The separate supported privacy-request contact/process was not independently
confirmed in that statement, so it remains pending rather than being inferred
from the notice text.

The authority contract provides a dedicated
`--require-public-privacy-ready` gate. A public privacy release cannot pass it
until the native notice and tester-program addendum are reviewed, the legacy
surface disposition is complete, an approved replacement source exists, and
the named owner’s evidence-backed attestation marks the wording as verified.
If an attestation finds a served numerical claim inaccurate, that command
remains blocked: the exact interim correction must first be separately
reviewed and approved for production.

An urgent correction after a negative attestation uses the distinct
`--require-interim-privacy-correction-ready` validator instead. It is not held
behind the complete replacement review, but it fails closed unless each exact
affected claim has its served route and wording identified plus a checked-in
replacement-wording source, separate production-approval reference,
validation plan, and rollback plan. This narrow path never authorizes a broad
policy rewrite or deployment by itself. Each correction claim must exactly
match a route-and-wording entry in the owner attestation’s
`attested_inaccurate_claims` list; a negative attestation for one claim cannot
be reused to alter another.

## Publication sequence

1. Record a reviewed digest of the pinned native notice, including the
   foreground-monitor wording and the station-retention boundary.
2. Extract and review the tester-program addendum without changing its scope;
   classify every retention statement as code-enforced or owner-attested.
3. Record a named owner attestation for station retention/contact and, where
   applicable, the tester-program retention/deletion/mailbox process.
4. Record the full inventory and retirement, redirect, or supersession of the
   legacy combined notice at every public location.
5. Generate the public page from those approved sources and validate it before
   any deployment.

This record does not alter a public privacy statement, production artifact, or
tester data.
