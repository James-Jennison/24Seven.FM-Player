# Privacy Reconciliation Record

This non-public record separates app-privacy facts from the protected
tester-program policy before the public privacy notice is changed.

## Designated sources

| Scope | Candidate authority | State |
| --- | --- | --- |
| Native Player behavior and app privacy | `PRIVACY.md` at `main` commit `56dc94694d158132befc22f7e97113aefc28af93` | Pinned; content review pending. |
| Tester-program privacy addendum | Existing `Closed-test tester-interest form` section in the portal branch notice | Must be extracted into a separately reviewed addendum; not yet authoritative as a standalone source. |
| Public privacy page | Generated site output | Must not choose between, omit, or merge the two sources without the reviews below. |

## Verified app-behavior drift

The pinned native source describes an optional, user-started, visible
foreground Chat monitor for one signed-in station. The current portal-branch
notice says there is no background polling. The pinned native source also
documents the related foreground special-use service, app-feedback draft, and
additional bounded diagnostic timing detail. These are native behavior claims,
so the pinned native source governs their eventual public wording.

## Operational-policy conflict requiring review

The pinned native source says station-side retention periods are unknown and
directs station-data requests to the network-authorized contact. The existing
portal-branch notice instead makes specific 30-day, 90-day, and one-year
retention/deletion promises. Those are controller/operations facts, not facts
that can be derived from app code, and neither source may silently override the
other.

Before publication, the responsible station/network owner must confirm the
actual retention/deletion policy and the supported request contact. The
reviewer must also confirm the tester-program addendum's separate 90-day
retention policy before it is extracted from the legacy combined notice.

## Publication sequence

1. Record the reviewed digest of the pinned native notice.
2. Extract and review the tester-program addendum without changing its scope.
3. Record the owner-confirmed station retention/contact policy.
4. Generate the public page from those approved sources and validate it before
   any deployment.

This record does not alter a public privacy statement, production artifact, or
tester data.
