# Operational retention attestation — August 28, 2026

This non-public record preserves the designated owner's operational statement
without rewriting the public privacy notice.

## Statement received

In response to the request to confirm whether the live 30-day and 90-day
station and tester-program retention claims are accurate, the designated owner
stated: **“yes, they are accurate.”**

This establishes only the following exact served claims as owner-attested:

1. `/privacy/`: personally identifiable account/profile data is deleted or
   anonymized within 30 days after a verified deletion request.
2. `/privacy/`: favorites and account preferences are deleted with the account
   within the same 30-day period.
3. `/privacy/`: ordinary song-request, operational-log, Chat/forum, and normal
   diagnostic/server/IP-log records are retained no more than 90 days.
4. `/privacy/`: encrypted backups are retained no more than 90 days.
5. `/privacy/`: private tester intake, assignments, feedback, and invitation
   correspondence are deleted or anonymized within 90 days of the stated
   withdrawal, deletion, rejection, or program-close events.

The exact source wording and route for each item are stored in
`attested_accurate_claims` in `WEBSITE_FACTS_CONTRACT.json`. The one-year
investigation statement is not included because the owner was asked only about
the live 30-day and 90-day claims. It is separately recorded as
`pending_owner_attestation` in `unattested_served_claims`, so it cannot be
silently treated as confirmed or omitted from the remaining review.

## Deliberate boundary

The statement did not independently confirm the supported privacy-request
contact or its handling process. That field remains
`pending_confirmation` in the authority contract. This is a partial
attestation, not a public-release approval, a policy rewrite, or a deployment
authorization.

The full public privacy-release validator therefore remains blocked. The
separate interim-correction path is not applicable because the owner did not
identify an inaccurate served claim.
