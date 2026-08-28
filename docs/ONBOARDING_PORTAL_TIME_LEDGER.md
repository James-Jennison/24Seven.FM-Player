# Onboarding Portal Time Ledger

Last updated: `August 28, 2026 at 1:23:48 PM PDT (UTC-07:00)`

This is the canonical time-accounting record for the protected 24Seven.FM Player onboarding portal: the Tester workspace, Coordinator workspace, onboarding wizard, assignment and reporting flows, and their safe website releases. It is deliberately independent of the Player Android and Play-operation milestones in `MILESTONE_TIME_LEDGER.md`.

Do not record portal-design, portal-maintenance, portal-validation, or portal-release work in any Player milestone ledger. Record each authorized portal interval here with its actual start and end, active/automated-wait/user-blocked time, scope, evidence, model, and forecast when supplied. Never infer an unmeasured interval.

## Cumulative portal totals

| Measure | Value | Qualification |
| --- | ---: | --- |
| Completed measured portal intervals | 30 | Includes the completed retention-attestation tracking phase. |
| Active portal time | 3.39 h | Completed intervals only. |
| Automated wait | 0.35 h | Measured release-gate/build and workflow waits during portal changes. |
| User-blocked time | 0.00 h | Completed intervals only. |
| Counted portal time | 3.74 h | Active plus automated wait. |

## Portal work records

### Phase 24 Owner retention attestation and claim tracking

- **Authorization and interval:** Authorized and started `August 28, 2026 at 1:17:49 PM PDT (UTC-07:00)`; completed `August 28, 2026 at 1:23:48 PM PDT (UTC-07:00)`.
- **Measured time:** 0.10 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.10 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Record the designated owner’s confirmation of the currently served 30-day and 90-day station and tester-program retention claims as exact route-and-wording evidence. Preserve the unconfirmed one-year investigation term and the supported privacy-request contact/process as explicit pending items. Preserve every public privacy page, portal workflow, deployment, release, tester record, and unrelated working change; no push, deployment, public copy, service, data, or external-account action is in scope.
- **Evidence:** The non-public attestation record maps five exact `/privacy/` claims to the owner’s confirmation. The one-year investigation-retention claim is separately tracked as `pending_owner_attestation`, and the request contact/process as `pending_confirmation`; neither is inferred. The validator rejects duplicate or conflicting exact claim keys across confirmed, inaccurate, and pending lists. Normal contract validation, Python compilation, JSON parsing, and whitespace validation passed. The public-release validator remains correctly blocked by incomplete legacy-surface disposition, while the interim-correction validator remains correctly unavailable because no claim was identified as inaccurate. Claude independently returned PASS after reviewing the exact positive-claim binding and explicit one-year pending state. No public text, deployment, push, data, or unrelated working change was altered.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 23 Pinned privacy-source review

- **Authorization and interval:** Authorized and started `August 28, 2026 at 1:03:39 PM PDT (UTC-07:00)`; completed `August 28, 2026 at 1:06:46 PM PDT (UTC-07:00)`.
- **Measured time:** 0.05 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.06 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Review the exact pinned native privacy notice and separately extract the legacy tester-program addendum into non-public source records, without changing any served policy, portal behavior, deployment configuration, data, or unrelated working changes.
- **Evidence:** `56dc946...:PRIVACY.md` now has a validator-verified exact-file digest and a native behavior review which retains station retention as unknown. `c00c2f...:PRIVACY.md` lines 52–62 now have a validator-verified extraction digest and reviewed tester-program boundary; broader 90-day retention remains explicitly `pending_owner_attestation`. The full privacy-release validator correctly advances to, and remains blocked by, legacy-surface disposition. Claude first caught and then approved correction of a false-readiness phrase; normal validation, Python compilation, JSON parsing, and whitespace validation passed. No public text, deployment, push, data, or unrelated working change was altered.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 22 Owner-attestation deadline and public-privacy gate

- **Authorization and interval:** Authorized and started `August 28, 2026 at 11:41:53 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 11:47:44 AM PDT (UTC-07:00)`.
- **Measured time:** 0.07 h active, 0.02 h automated wait, 0.00 h user-blocked, 0.09 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Record the owner-approved two-business-day deadline for evidence-backed station and tester-program operational attestation, and add an explicit validator mode for a future public privacy release. Preserve every public claim, live artifact, portal record, mail path, deployment process, and unrelated working change; no deployment, public policy change, data mutation, push, or Play/Cloudflare action is in scope.
- **Evidence:** The authority contract records September 1, 2026 at 5:00 PM PDT (UTC-07:00) as a deadline—not an attestation. `scripts/validate-website-facts-contract.py --require-public-privacy-ready` fails closed until both source reviews, legacy-surface disposition, approved replacement source, and a named owner’s evidence-backed attestation are complete. A separate `--require-interim-privacy-correction-ready` path prevents a specifically owner-disproven served claim from being held behind unrelated full-notice work, but requires exact served route and wording, checked-in replacement wording, separate production approval, validation, and rollback. Every interim correction route-and-claim pair must exactly match the owner attestation's `attested_inaccurate_claims` list. Normal contract validation and Python compilation passed; both specialized modes correctly failed in the current pending state. Claude reviewed the final exact-match implementation and returned PASS. No public text, deployment, push, data, or unrelated working change was altered.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 21 Legacy privacy surface inventory

- **Authorization and interval:** Authorized and started `August 28, 2026 at 11:29:12 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 11:35:07 AM PDT (UTC-07:00)`.
- **Measured time:** 0.08 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.09 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Inventory the pinned portal legacy privacy source, its generated `/privacy/` route, and a read-only public observation so retirement/supersession can be verified before a public change. Preserve the public page, hosting, cache, portal records, mail, and unrelated working changes; no deployment, purge, Cloudflare action, or policy change is in scope.
- **Evidence:** The pinned portal source carries the combined notice only in root `PRIVACY.md`; the generator creates the `/privacy/` source page from it. Public HTTPS returned 200 and contained the 30-day and 90-day legacy promises. There is no deployment manifest tying the response to a commit. Historical `618f159` adopted numerical policy, but the later current-native source `11e2ee0` replaced it with unknown retention; the inventory records that source supersession and requires an owner-set expedited attestation deadline rather than inventing one.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 20 Privacy-source peer review and source-integrity gate

- **Authorization and interval:** Authorized and started `August 28, 2026 at 11:21:35 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 11:25:27 AM PDT (UTC-07:00)`.
- **Measured time:** 0.05 h active, 0.02 h automated wait, 0.00 h user-blocked, 0.07 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Obtain an independent Claude critique of the non-public privacy reconciliation, validate the exact native and portal source pins, classify the portal 90-day retention mechanism, and strengthen the release gate so an unavailable pinned source cannot be mistaken for evidence. Preserve all public privacy text, live artifacts, portal data, mail, release state, and unrelated working changes; no deployment, policy publication, or data mutation is in scope.
- **Evidence:** Claude returned PASS after the reconciliation added the legacy-notice retirement gate, named operational attestations, and the chat-only retention classification. The native `main` pin was fetched without merge/rebase and resolves `PRIVACY.md`; the reconciled one-minute foreground-monitor wording was independently re-read from that exact file. The validator now resolves every authority commit and cited file with `git cat-file`, and the portal workflow explicitly fetches `main` before its gate runs. Local contract validation, Python compilation, workflow YAML parsing, and whitespace validation passed.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 19 Privacy-source reconciliation

- **Authorization and interval:** Authorized and started `August 28, 2026 at 11:13:23 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 11:14:06 AM PDT (UTC-07:00)`.
- **Measured time:** 0.01 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.01 h counted.
- **Scope:** Separate the pinned native privacy source from the portal's distinct tester-program policy, document app-behavior drift and operational-policy conflicts, and require explicit review before a public privacy update. Preserve every current public claim, portal workflow, tester record, mail path, production artifact, and unrelated working change; no privacy-page generation, deployment, external policy change, or data mutation is in scope.
- **Evidence so far:** The pinned native notice documents the optional foreground Chat monitor and unknown station retention; the portal-branch notice says no background polling and makes specific station retention promises while embedding the tester-program policy. The native monitor service exists in the pinned application tree.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 18 Play Console availability evidence

- **Authorization and interval:** Authorized and started `August 28, 2026 at 11:10:29 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 11:12:19 AM PDT (UTC-07:00)`.
- **Measured time:** 0.03 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.03 h counted.
- **Scope:** Record a read-only Google Play Console observation as availability evidence only, distinguish Alpha 07 available-to-testers from Alpha 08 in-review, and retain the separate artifact-provenance gate for code-content claims. Preserve every public page, release artifact, Console setting, tester/portal record, and unrelated working change; no publish, release, rollout, upload, track edit, or deployment is in scope.
- **Evidence so far:** Read-only Console release overview shows Alpha 07/version code 16 available to Closed Testing — Alpha testers and Alpha 08/version code 17 in review. Claude independently confirmed availability and byte-level provenance are separate evidence gates.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 17 Release-manifest gate

- **Authorization and interval:** Authorized and started `August 28, 2026 at 11:04:01 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 11:04:50 AM PDT (UTC-07:00)`.
- **Measured time:** 0.01 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.01 h counted.
- **Scope:** Add a checked-in, non-public Alpha 08 release-manifest template that makes the exact artifact, source commit, Play track, availability observation, and evidence reference mandatory before a public release claim can become ready. Preserve the existing public site, privacy text, user-facing version/status, portal behavior, Play Console, production artifact, and unrelated working changes.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 16 Release-status evidence ranking

- **Authorization and interval:** Authorized and started `August 28, 2026 at 11:02:19 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 11:03:03 AM PDT (UTC-07:00)`.
- **Measured time:** 0.01 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.01 h counted.
- **Scope:** Record the version-exact Alpha 08 release record as the release-status authority, classify the unversioned project-status statements as non-authoritative context, and preserve all public wording, prior documentation, release behavior, tester data, and deployment state. No privacy-copy synchronization, version/availability claim, deployment, push, branch rewrite, external Console action, or historical-document rewrite is in scope.
- **Evidence so far:** The pinned Alpha 08 release record identifies version code 17 and states submission to Closed testing review with tester availability still pending. GitHub Releases has no published assets. The competing plan, forecast, and ledger statements name no version or artifact and include conflicting status language. Claude independently agreed that they cannot govern a public claim.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 15 Website authority-contract remediation

- **Authorization and interval:** Authorized and started `August 28, 2026 at 10:51:48 AM PDT (UTC-07:00)`; completed `August 28, 2026 at 10:54:04 AM PDT (UTC-07:00)`.
- **Measured time:** 0.04 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.04 h counted.
- **Scope:** Establish a non-public, exact-commit authority record for the native behavior, privacy, release-candidate, protected portal, and live-artifact provenance boundaries before changing any user-facing privacy or version claim. Preserve the existing portal, tester data, mail, release behavior, production artifact, and unrelated local work. No user-facing copy, release version, deployment, push, branch rewrite, service, database, DNS, or Cloudflare change is in scope.
- **Evidence so far:** Read-only comparison recorded the current default native `main` pin, the portal pin, the Alpha 08 release record, and the fact that the release record still requires Play review and tester-availability confirmation. The independent Claude review agreed that portal code cannot define native privacy/release facts and that live provenance remains unconfirmed without a deployment manifest. The checked-in contract and its validator make each unresolved authority field explicit.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 14 Explicit Re-test Closeout

- **Authorization and interval:** Authorized and started `August 23, 2026 at 5:51:57 AM PDT (UTC-07:00)`; completed `August 23, 2026 at 5:56:39 AM PDT (UTC-07:00)`.
- **Measured time:** 0.13 h active, 0.03 h automated wait, 0.00 h user-blocked, 0.16 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Add a dedicated Coordinator-only closeout decision after a separate re-test assignment has been fully submitted and reviewed. Link the exact original Issue/Blocked PT report to its re-test result, require a checked confirmation and bounded auditable note before recording either Closed, Needs reproduction, or Known issue as a new immutable triage event, and preserve every original report, assignment, re-test assignment, review event, scope, and Tester-visible evidence. Do not auto-close, infer a passing re-test, create or modify any task, send mail, retry a notification, create external work, or expose private Coordinator notes in the Tester workspace.
- **Evidence:** Merged commit `2895dd1` through PR #83 adds one unique linked closeout record plus a dedicated Coordinator review route. It only permits Closed, Needs reproduction, or Known issue after the separate re-test is fully reported and explicitly completed by the Coordinator; it writes a new immutable original-report triage event with a bounded note. The complete 56-file portal gate and GitHub’s required portal check passed, and the staged artifact matched the merged local manifest (SHA-256 `b9215dc1b1bfdc09e4a8a15083291cc37b01227008cfcac4dbc3ad751babee51`) before atomic promotion. Rollback: `player.jamesjennison.net.rollback-portal-phase14-2895dd1`. Origin HTTPS returned 200 for Issue triage, Re-test handoff, Re-test closeout, Work Review, and Tester Tasks. No browser tab was touched and no signed-in visual acceptance is claimed.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 13 Explicit Re-test Notification

- **Authorization and interval:** Authorized and started `August 23, 2026 at 5:36:39 AM PDT (UTC-07:00)`; completed `August 23, 2026 at 5:47:22 AM PDT (UTC-07:00)`.
- **Measured time:** 0.15 h active, 0.03 h automated wait, 0.00 h user-blocked, 0.18 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Add one explicit Coordinator-only re-test notification action for a previously created separate re-test assignment whose notification state is Not sent. Preview the exact PT case, preserved scope, Tester-visible re-test instruction, and protected Tester-portal destination before a checked confirmation submits the existing mail transport. Archive only the prepared message and transport result, clearly label transport acceptance as not proving inbox delivery or reading, and never auto-send, auto-retry, change an assignment, create a task, alter original evidence, or notify a non-re-test assignment through this action.
- **Evidence:** Merged commit `53264d5` through PR #81 adds the dedicated Coordinator preview and checked one-time notification action for an existing separate re-test assignment. It retains the exact original PT case, preserved scope, Tester-visible instruction, protected Tester-portal destination, and a distinct `retest_notification` archive type. The action is unavailable for ordinary assignments, its generic assignment-email path rejects re-tests, and an atomic zero-attempt claim prevents duplicate concurrent notifications; the resulting transport outcome is explicitly not inbox-delivery or reading proof and no automatic retry is offered. A data-preserving SQLite migration rebuilds the existing mail archive with its rows and indexes intact so the new constrained message type is valid. The isolated local contract and complete 56-file portal release gate passed; GitHub's required `Validate reviewed portal artifact` and build checks passed. The exact local/staged-server manifests matched all 56 files (SHA-256 `5d1fc4532fab9982eaba9127352d8191257734ceac33a7d54acbc0812649d6c5`) before atomic promotion. The prior live artifact remains at `player.jamesjennison.net.rollback-portal-phase13-53264d5`. Origin and public HTTPS each returned 200 for Issue triage, Re-test handoff, Coverage Catalog, Work Review, Tester Tasks, and the shared stylesheet. No browser tab was touched and no signed-in visual acceptance is claimed for this release.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 12 Explicit Re-test Handoff

- **Authorization and interval:** Authorized and started `August 23, 2026 at 5:14:49 AM PDT (UTC-07:00)`; completed `August 23, 2026 at 5:27:58 AM PDT (UTC-07:00)`.
- **Measured time:** 0.18 h active, 0.03 h automated wait, 0.00 h user-blocked, 0.21 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Add a deliberate Coordinator re-test handoff only for an Issue or Blocked PT report whose latest immutable triage event is Ready for re-test. Preserve the original assignment and report as history, require a dedicated Coordinator review screen and a bounded Tester-visible re-test instruction before creating a new focused assignment, and constrain the new assignment to the exact originally reported PT case and original approved scope. Do not automatically assign, email, notify a Tester, create an external ticket, change the original task/report/triage event, or infer a completed re-test. The new assignment remains subject to the existing Tester report and Coordinator-review gates.
- **Evidence:** Merged commit `e87c265f` through PR #79 adds an additive, one-per-report `tester_retest_handoffs` record and a dedicated Coordinator Re-test handoff review screen. It accepts only a current Issue/Blocked report whose latest immutable triage event is Ready for re-test and whose original assignment is resolved; it preserves that original assignment and report, carries its approved station/device scope, requires a bounded Tester-visible instruction, and creates one new assignment limited to the original exact PT case. The isolated SQLite contract proves the linked assignment is new, exact-case-only, starts with assignment-email status `not_sent`, rejects duplicate handoffs, and keeps its instruction out of shared Activity. Controlled re-tests additionally require new explicit Coordinator authorization. The exact merged commit passed the complete 56-file portal release gate and GitHub's required `Validate reviewed portal artifact` check; local and staged-server manifests matched all 56 files (SHA-256 `20c2527fcb3a83c42ad611aaf734ad01c39a51a77f7a8e857e63599496a2e5f9`) before atomic promotion. The prior live artifact remains at `player.jamesjennison.net.rollback-portal-phase12-e87c265f`. Origin and public HTTPS each returned 200 for Issue triage, Re-test handoff, Coverage Catalog, Work Review, Tester Tasks, and the shared stylesheet. No browser tab was touched and no signed-in visual acceptance is claimed for this release.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 11 Issue Triage and Re-test Queue

- **Authorization and interval:** Authorized and started `August 23, 2026 at 4:57:05 AM PDT (UTC-07:00)`; completed `August 23, 2026 at 5:06:23 AM PDT (UTC-07:00)`.
- **Measured time:** 0.12 h active, 0.02 h automated wait, 0.00 h user-blocked, 0.14 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Add a Coordinator-only issue triage queue derived from existing PT-case reports whose outcome is Issue or Blocked. Record a bounded Coordinator triage state—needs reproduction, known issue, ready for re-test, or closed—with an auditable note. Link each entry to its existing Tester, assignment, exact PT case, and report. Preserve report evidence, task/review behavior, role boundaries, privacy, mail behavior, and atomic release process. Never create a task assignment, send mail, notify a Tester, create an external ticket, or infer a retest; any future retest remains a separately explicit assignment.
- **Evidence:** Merged commit `bc961832` through PR #77 adds the Coordinator-only Issue triage rail route and additive immutable `tester_issue_triage_events` record. Its bounded states are Needs reproduction, Known issue, Ready for re-test, and Closed; every event requires a Coordinator note and retains the originating Tester, assignment, exact PT case, and report. The isolated SQLite contract proves a triage event cannot change the assignment count, rejects an empty note, preserves immutable history, and does not expose the private Coordinator note in the shared Activity timeline. The exact merged commit passed the complete 56-file portal release gate and GitHub's required `Validate reviewed portal artifact` check; the local and staged-server manifests matched all 56 files (SHA-256 `7a81daa2b9f7bb57fbd5b68df00763e3f7cc337603f2fd3bcef8b4e541fe0ea6`) before atomic promotion. The prior live artifact remains at `player.jamesjennison.net.rollback-portal-phase11-bc961832`. Origin and public HTTPS each returned 200 for Issue triage, Coverage Catalog, Work Review, Tester Tasks, and the shared stylesheet. No browser tab was touched and no signed-in visual acceptance is claimed for this release.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 10 Coordinator Coverage Catalog

- **Authorization and interval:** Authorized and started `August 23, 2026 at 4:45:16 AM PDT (UTC-07:00)`; completed `August 23, 2026 at 4:53:13 AM PDT (UTC-07:00)`.
- **Measured time:** 0.11 h active, 0.02 h automated wait, 0.00 h user-blocked, 0.13 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Added a read-only Coordinator Coverage Catalog derived only from existing protected Tester profiles and assignment records. It surfaces aggregate coverage by Android version, device form factor, station, network, audio/accessory, and accessibility capability, plus assignment readiness and active-work capacity. Each aggregate links only to existing protected Coordinator records. Role boundaries, privacy, onboarding, recommendations, task/review behavior, mail behavior, database contents, and the atomic release process remain unchanged. No assignment, mail, Tester-data collection, database table, or external integration was added.
- **Evidence:** Merged commit `f8cb898` through PR #75 adds the separate Coordinator Coverage Catalog route and rail item, using only existing active Tester profile fields and assignment status. The isolated SQLite contract proves it preserves the recorded coverage dimensions and readiness/capacity calculations without creating assignments; the complete portal release gate and GitHub required `Validate reviewed portal artifact` check passed at the exact merged commit. The normalized local/server staging manifests matched all 56 files (SHA-256 `8d8c287f8962ce2bc412e7d9529d2e2b06a46a4efd398b3163ab2515cffa3a47`) before atomic promotion. The prior live artifact remains at `player.jamesjennison.net.rollback-portal-phase10-f8cb898`. Origin and public HTTPS each returned 200 for Coverage Catalog, Work Review, Tester Tasks, and the shared stylesheet. No browser tab was touched and no signed-in visual acceptance is claimed for this release.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 9 Coordinator Work Review inbox

- **Authorization and interval:** Authorized and started `August 22, 2026 at 9:59:03 PM PDT (UTC-07:00)`; completed `August 22, 2026 at 10:16:08 PM PDT (UTC-07:00)`.
- **Measured time:** 0.22 h active, 0.03 h automated wait, 0.00 h user-blocked, 0.25 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Create a dedicated Coordinator review inbox for tester-submitted assignments. Show the exact assignment, PT-case evidence, scope, and report outcomes, then make final completion, return-for-clarification, and blocked-work decisions explicit. Reflect the decision and its next action in the Tester workspace and activity evidence. Preserve the existing per-case reporting gate, role boundaries, protected data, mail behavior, and atomic release process. No bulk action or delivery inference was added. The narrow backward-compatible requirement is met with protected, idempotently created review-decision and clarification-response storage; it neither broadens existing data collection nor changes mail behavior.
- **Evidence:** Merged commit `13e56c6` through PR #73 adds the Coordinator Work Review inbox, immutable Coordinator decision history (complete, return for clarification, or blocked), and dedicated Tester clarification/resubmission handling. It keeps incomplete PT-case reporting from being approved, requires a Tester-visible note for return/block actions, and keeps decision notes, report contents, and clarification bodies out of shared Activity. GitHub's required `Validate reviewed portal artifact` check and the independent build passed; the exact merged commit then passed the full 56-file local portal release gate. The normalized local/server staging manifests matched all 56 files (SHA-256 `d5ae873d0ab4dd5151ed427d49f64d5565f8a6cb5b82d122ba406ca8f0fa01ab`) before an atomic document-root promotion. The prior live directory remains at `player.jamesjennison.net.rollback-portal-phase9-13e56c6`. Origin and public HTTPS each returned 200 for Coordinator Work Review, Tester tasks, and the changed Coordinator/Activity assets. No browser tab was touched and no signed-in visual acceptance is claimed for this release.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 8 activity clarity refinement

- **Authorization and interval:** Authorized and started `August 22, 2026 at 5:22:29 PM PDT (UTC-07:00)`; completed `August 22, 2026 at 5:35:35 PM PDT (UTC-07:00)`.
- **Measured time:** 0.18 h active, 0.02 h automated wait, 0.00 h user-blocked, 0.20 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Improve existing Activity records with browser-local time presentation plus UTC context, client-side filters, and concise grouping of generic Coordinator mail handoffs. Preserve every underlying record, privacy boundary, permissions, cache configuration, and workflow; no timezone is persisted or collected and no schema migration is planned.
- **Evidence:** Merged commit `4dbbb33` through PR #71 adds browser-local formatting with explicit UTC context, client-side All/Onboarding/Work/Communication filters, and concise summaries for repeated generic Coordinator mail handoffs while retaining individual orientation, assignment, and smoke-test evidence. The exact commit passed the complete 56-file portal release gate. The local and server staging manifests matched exactly (SHA-256 `763a3a01b9235d5d4a81a6f2f02980b65ff2f9e0202b453b2733f0802a999094`) before atomic promotion; the prior release remains at `player.jamesjennison.net.rollback-portal-phase8-4dbbb33`. Origin and public HTTPS each returned 200 for Tester Activity, a Coordinator tester-record route, and the new Activity script. No existing Tester Activity tab was claimed or navigated during final verification, so no post-release signed-in visual acceptance is claimed.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 7 activity timeline cache-resilience fix

- **Authorization and interval:** Authorized and started `August 22, 2026 at 12:39:50 PM PDT (UTC-07:00)`; completed `August 22, 2026 at 12:46:03 PM PDT (UTC-07:00)`.
- **Measured time:** 0.09 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.10 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Keep the existing Activity evidence readable in Tester and Coordinator workspaces when a shared static stylesheet is stale. Preserve all timeline data, privacy boundaries, portal workflows, permissions, deployment safeguards, and cache configuration; no new data collection, schema migration, or Cloudflare change is planned.
- **Evidence:** Merged commit `e7e583c` through PR #69 adds only the Activity component's structural layout rules to both protected HTML responses, while retaining the shared stylesheet as the normal presentation source. The exact merged commit passed the dedicated activity smoke, full portal release gate, and GitHub portal release gate. The 55-file artifact (manifest SHA-256 `8eec98d899ea81c08b1daaf26056b81e749a15178a05a51568d2f3dae792ebdd`) matched server staging before atomic promotion; the prior release remains at `player.jamesjennison.net.rollback-portal-phase7-cache-e7e583c`. Origin and public HTTPS returned 200 for Tester Activity and a Coordinator tester record. A signed-in Tester browser retaining the earlier stale shared stylesheet state reloaded successfully and computed the Activity event rows as a grid with block title and detail elements. The Coordinator administrator session had expired before its post-release reload, so no signed-in Coordinator visual acceptance is claimed; no sign-in or data action was attempted.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 7 shared activity timeline

- **Authorization and interval:** Authorized and started `August 22, 2026 at 11:38:50 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 12:10:57 PM PDT (UTC-07:00)`.
- **Measured time:** 0.35 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.36 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Add a role-safe, read-only activity timeline derived from the existing onboarding, assignment, PT-case report, Coordinator-review, and mail-handoff evidence. Preserve all records, permissions, task/report workflows, and deployment safeguards; no new data collection or schema migration is planned.
- **Evidence:** Merged commit `1f864bd` through PR #67 adds the Tester Activity route and matching Coordinator record timeline. Entries come only from existing protected records and explicitly omit Coordinator notes, report and mail contents, and inferred delivery; accepted mail transport is labeled as not proving inbox delivery or reading. The dedicated isolated-SQLite privacy smoke and complete portal release gate passed at the merged commit. The reviewed 55-file artifact (manifest SHA-256 `0ed7d539b5e8c84b13588523dd23e629d922224e8000a1d1f58550521e2a0af4`) matched the server-side staging manifest before an atomic Player document-root promotion. The prior release remains at `player.jamesjennison.net.rollback-portal-phase7-1f864bd`. Origin HTTP/HTTPS and public HTTPS returned 200 for Tester Activity, Coordinator operations, and the shared stylesheet. Signed-in Chrome acceptance confirmed the Activity rail item and timeline in both Tester and Coordinator workspaces, using read-only navigation only.
- **Model and forecast:** Current approved model and original forecast were not supplied; recorded as Unknown.

### Phase 6 PT-checklist runtime activation fix

- **Authorization and interval:** Authorized and started `August 22, 2026 at 11:28:19 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 11:33:35 AM PDT (UTC-07:00)`.
- **Measured time:** 0.07 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.08 h counted. The unmeasured remainder of the wall-clock interval is excluded.
- **Scope:** Correct the in-portal assigned-checklist control after signed-in acceptance showed its button did not activate the dialog. Preserve the exact assigned-case boundary, existing task and report data, all permissions, and the atomic Player website release process.
- **Evidence:** Merged commit `fb5ed30` uses delegated click handling for checklist open, explicit close, and dialog-backdrop close behavior; no task, report, database, mail, permission, or routing behavior changed. The exact merged commit passed the local 55-file artifact gate and the GitHub portal release gate. The 55-file staging artifact matched the local SHA-256 manifest before atomic promotion to the verified Player document root; the prior release remains at `player.jamesjennison.net.rollback-portal-phase6-fb5ed30`. Origin and public HTTPS each returned 200 for Tester tasks, Coordinator operations, and both dialog assets. Signed-in Chrome acceptance reloaded the assigned Tester task, opened the dialog, confirmed only PT-09 and PT-10, then closed the dialog without submitting or changing a record.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.

### Phase 5 in-portal PT checklist

- **Authorization and interval:** Authorized and started `August 22, 2026 at 10:42:20 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 10:54:21 AM PDT (UTC-07:00)`.
- **Measured time:** 0.17 h active, 0.03 h automated wait, 0.00 h user-blocked, 0.20 h counted.
- **Scope:** Keep the Tester in the protected portal by opening the assigned PT checklist in an accessible in-portal dialog rather than navigating to the separate Developer checklist catalog. Preserve the exact assigned-case boundary and all task, report, onboarding, and permission contracts; no migration is planned.
- **Evidence:** Merged commit `f29b7b9` replaces the external Developer-page handoff with a keyboard-accessible in-portal dialog. The dialog derives detailed checklist markup from the canonical PT checklist source and filters it to the exact PT IDs on that Tester assignment; the reporting path, assigned scope, task permissions, and database remain unchanged. The full portal release gate passed at the merged commit. A 55-file hash-verified staged artifact was atomically promoted to the verified Player document root with the prior live release retained at `player.jamesjennison.net.rollback-portal-phase5-f29b7b9`; origin and public HTTPS returned 200 for Tester tasks, Coordinator operations, and both new dialog assets. A signed-in pre-release read-only check captured the former external handoff. The browser controller no longer had the signed-in tab after deployment, so post-release interactive-click acceptance is explicitly not claimed.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.

### Phase 4 Coordinator queue discovery

- **Authorization and interval:** Authorized and started `August 22, 2026 at 10:17:53 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 10:25:19 AM PDT (UTC-07:00)`.
- **Measured time:** 0.11 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.12 h counted.
- **Scope:** Add protected, server-derived Coordinator saved views for operational attention, assignment readiness, smoke-test follow-up, submitted reports, active work, and closed work. Preserve all existing evidence, task, reporting, mail, and permission contracts; no migration is planned.
- **Evidence:** Commit `6f43211` adds the seven Coordinator saved views (All testers, Needs attention, Ready to assign, Smoke test outstanding, Reports awaiting review, Active assignments, and Completed / blocked), derives every count and roster reason from existing protected onboarding and assignment records, and leaves Tester permissions, task gates, reporting, mail, and database schema unchanged. The complete portal release gate passed at the exact commit. The staged artifact matched every release file hash before an atomic swap into the verified Player document root; the prior live release remains at `player.jamesjennison.net.rollback-portal-phase4-6f43211`. Origin and public HTTPS both returned 200 for the Coordinator default, attention, and report-review routes plus the Tester portal.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.

### Phase 3 assignment lifecycle and handoff workbench

- **Authorization and interval:** Authorized and started `August 22, 2026 at 9:57:13 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 10:05:50 AM PDT (UTC-07:00)`.
- **Measured time:** 0.14 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.15 h counted.
- **Scope and evidence:** Commit `06b679a` adds the same evidence-backed assignment lifecycle to Tester and Coordinator task records: assigned, PT-case evidence, Coordinator review, and final status. Tester task actions now open the exact report queue with that assignment preselected; the Coordinator record links to the matching read-only Tester handoff. The change derives only from existing assignment/report timestamps and status records—no migration, new data collection, gate change, or permission change. The full portal release gate passed at the exact commit. A hash-verified staged artifact was atomically swapped into the active Player document root with the prior live release retained at `player.jamesjennison.net.rollback-portal-phase3-06b679a`; origin and public HTTPS returned 200 for the changed Tester task/report and Coordinator record routes. Signed-in acceptance confirmed the Tester report queue and Coordinator record load cleanly; the current signed-in tester has no active assignment, so no synthetic assignment was created merely to make lifecycle evidence appear.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.

### Route-based Tester workspace

- **Authorization and interval:** Authorized `August 22, 2026 at 7:29:35 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 7:41:56 AM PDT (UTC-07:00)`.
- **Measured time:** 0.21 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.21 h counted.
- **Scope and evidence:** Retained the protected wizard, session, database, access controls, task reporting, and atomic rollback while separating the completed Tester experience into Dashboard, Onboarding, Profile & Device, My Tasks, Report results, and Support routes.
- **Model and forecast:** GPT-5. Reasoning strength and original forecast were not supplied and are recorded as Unknown.

### Shared workspace-shell alignment

- **Authorization and interval:** Authorized `August 22, 2026 at 7:57:23 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 8:07:06 AM PDT (UTC-07:00)`.
- **Measured time:** 0.16 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.16 h counted.
- **Scope and evidence:** Commit `f4e8f53` put the Tester portal in the same persistent application shell and left rail as Coordinator Operations while preserving protected route, wizard, task, report, and support behavior. The portal gates, atomic swap, rollback retention, and authenticated role acceptance passed.
- **Model and forecast:** GPT-5. Reasoning strength and original forecast Unknown.

### Workspace-rail legibility

- **Authorization and interval:** Authorized `August 22, 2026 at 8:14:41 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 8:19:40 AM PDT (UTC-07:00)`.
- **Measured time:** 0.08 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.08 h counted.
- **Scope and evidence:** Commit `069d20e` made both desktop rails readable icon-and-label rows while retaining the compact mobile rail. The portal gates, atomic swap, rollback retention, and authenticated Coordinator/Tester acceptance passed.
- **Model and forecast:** GPT-5. Reasoning strength and original forecast Unknown.

### Coordinator workspace-chrome parity

- **Authorization and interval:** Authorized `August 22, 2026 at 8:39:00 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 8:41:42 AM PDT (UTC-07:00)`.
- **Measured time:** 0.05 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.05 h counted.
- **Scope and evidence:** Commit `b830173` made Coordinator use the cache-versioned shared stylesheet and same icon-and-label rail markup as the Tester portal. The portal gates, atomic swap, rollback retention, and authenticated role acceptance passed.
- **Model and forecast:** GPT-5. Reasoning strength and original forecast Unknown.

### Phase 1 Frappe/Backstage-inspired workspace redesign

- **Authorization and interval:** Authorized and started `August 22, 2026 at 8:47:21 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 8:59:21 AM PDT (UTC-07:00)`.
- **Measured time:** 0.19 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.20 h counted.
- **Scope and evidence:** Commit `40b52b8` creates shared page anatomy, role-specific Tester next-action cards, and Coordinator attention/record queues, replacing the rendered freeform panel canvas. Protected routes, wizard gating, assignment, reporting, mail, Coordinator actions, and deployment safeguards remain unchanged. The exact merged commit passed the full portal release gate, then a hash-verified atomic document-root swap deployed it with the previous live directory retained for rollback. Origin HTTP/HTTPS and public HTTPS returned 200 for both protected portal routes. Signed-in acceptance confirmed the common header, primary workbench, and three attention cards for both roles; Tester retains six routes and Coordinator exposes one stable record-queue grid with no freeform canvas.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.

### Portal CI trigger isolation

- **Authorization and interval:** Authorized and started `August 22, 2026 at 9:17:07 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 9:19:28 AM PDT (UTC-07:00)`.
- **Measured time:** 0.04 h active, 0.00 h automated wait, 0.00 h user-blocked, 0.04 h counted.
- **Scope and evidence:** Commit `d15f6b7` limits pull-request Android CI to Android modules, Gradle/wrapper configuration, and changes to its own workflow while keeping `main` push validation unchanged. It also makes the protected portal gate run when the Android CI workflow is edited. PR #57 passed the portal artifact gate and merged; Android CI ran for that PR only because the Android workflow itself changed. The follow-up portal-ledger-only PR will provide the direct no-Android-CI trigger proof.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.

### Phase 2 record-page workbench

- **Authorization and interval:** Authorized and started `August 22, 2026 at 9:29:22 AM PDT (UTC-07:00)`; completed `August 22, 2026 at 9:38:00 AM PDT (UTC-07:00)`.
- **Measured time:** 0.14 h active, 0.01 h automated wait, 0.00 h user-blocked, 0.15 h counted.
- **Scope and evidence:** Commit `9c9ce17` changes only record-page presentation: Tester task records now show scope, per-case reporting progress, detailed checklist and report actions; Tester reports open with an explicit evidence queue; Coordinator tester records use the shared record header and lifecycle context. The protected task/assignment, onboarding, email, reporting, recommendation, and security contracts passed; the full portal release gate passed. A hash-verified atomic deployment retained the previous live directory for rollback. Origin HTTP/HTTPS and public HTTPS returned 200 for the changed Tester task/report and Coordinator routes. Signed-in Tester acceptance confirmed the task workbench and evidence queue. After the owner confirmed the active Coordinator session, read-only acceptance on a tester record confirmed the Coordinator record header, lifecycle summary, role label, and record-workbench section; no Coordinator data was changed.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.
