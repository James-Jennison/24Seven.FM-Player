# Legacy Privacy Notice Surface Inventory

This non-public inventory records the known locations of the legacy combined
Player and tester-program notice before a revised public privacy page can be
published. It does not itself retire, redirect, or replace any page.

## Evidence boundary

- **Legacy source commit:** `c00c2f4982232e7c197aa0e35d174d7c3478436e`
  (`codex/onboarding-portal-production`).
- **Native source commit:** `56dc94694d158132befc22f7e97113aefc28af93`
  (`main`).
- **Read-only public observation:** August 28, 2026 at 11:27 AM PDT
  (UTC-07:00). `https://player.jamesjennison.net/privacy/` returned HTTP 200
  and contained the legacy 30-day and 90-day retention promises.
- **Provenance limit:** There is no current deployment manifest tying that
  public response to a source commit. The public observation proves the page
  content was served; it does not prove which commit produced it.
- **Current-source supersession:** Historical commit `618f159` recorded a
  network-wide numerical retention policy. The later native privacy revision
  `11e2ee0` replaced those station-side numerical claims with “retention
  periods are currently unknown”; that wording remains in the current native
  authority. The historical adoption cannot silently override the later pinned
  privacy source.

## Confirmed source and generated surfaces

| Surface | Evidence | Required disposition before a public privacy update |
| --- | --- | --- |
| Portal root `PRIVACY.md` | The legacy commit contains the combined notice, including the no-background-polling assertion, station 30/90/one-year claims, and the tester-program 90-day promise. | Remove it from the portal authority path or replace it with an explicit, reviewed supersession pointer. |
| Generated `privacy-site/privacy/index.md` | `scripts/prepare-project-site.sh` creates this untracked page from the portal root `PRIVACY.md`; it is the source route for `/privacy/`. | Generate it only from the approved native notice and separately approved tester-program addendum. |
| Public `/privacy/` | Read-only observation above confirmed the legacy day-count claims remain served. | Deploy the reviewed replacement through the atomic release process, then verify the public response and record its deployment manifest. |

At the pinned portal source, a content search for the contradictory polling
language and the 30-day/90-day promises found only the root `PRIVACY.md`.
Other pages link to `/privacy/` but do not carry an independent copy of the
notice at that source pin.

## Remaining checks before retirement

1. Identify any prior deployment directories, generated artifacts, cached
   response variants, mirrors, or external copies that are still in scope for
   the public notice.
2. Record the exact reviewed replacement source(s), their digests, and the
   named operational attestation for every non-code retention/contact claim.
3. Atomically replace the public artifact only after the authority contract is
   reviewed, then record the source commit, artifact digest, verification, and
   rollback reference in a deployment manifest.

This inventory does not establish an operational retention policy and must not
be used to infer one.

## Interim-response boundary

The current observation proves that the numerical claims are being served; it
does **not** prove that they are operationally false. The native notice says
station retention is unknown, which makes the legacy numbers unverified—not a
substitute finding that the numbers are inaccurate. A takedown, redirect, or
replacement with non-numeric language is itself a user-facing policy change
and requires a separately approved production release with exact replacement
wording and rollback evidence.

If the named operational owner confirms that any served claim is inaccurate,
that confirmation creates an urgent interim-remediation path: neutralize only
the confirmed-inaccurate claim while the fully reviewed replacement remains
subject to the complete publication gate. Without that confirmation, this
record preserves the full-review gate and prevents either source from silently
overriding the other.

The owner must set and record an expedited response deadline for the live
30-day and 90-day claims. No deadline is inferred from source history or this
inventory; a deadline expresses an operational commitment rather than a fact
that can be derived from code.

The owner approved **September 1, 2026 at 5:00 PM PDT (UTC-07:00)** as that
deadline. The deadline is recorded in `WEBSITE_FACTS_CONTRACT.json` and is not
an attestation of the numerical claims. The served notice stays unchanged until
the evidence-backed review and separately approved public release occur.

If that attestation identifies a specific served claim as inaccurate, the
repository's narrow interim-correction validator may prepare only that claim's
correction. It still requires the exact wording, separate production approval,
validation, and rollback plan, but it does not wait for unrelated full-notice
review work.
