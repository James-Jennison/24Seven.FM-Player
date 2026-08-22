# Onboarding Portal Time Ledger

Last updated: `August 22, 2026 at 9:38:00 AM PDT (UTC-07:00)`

This is the canonical time-accounting record for the protected 24Seven.FM Player onboarding portal: the Tester workspace, Coordinator workspace, onboarding wizard, assignment and reporting flows, and their safe website releases. It is deliberately independent of the Player Android and Play-operation milestones in `MILESTONE_TIME_LEDGER.md`.

Do not record portal-design, portal-maintenance, portal-validation, or portal-release work in any Player milestone ledger. Record each authorized portal interval here with its actual start and end, active/automated-wait/user-blocked time, scope, evidence, model, and forecast when supplied. Never infer an unmeasured interval.

## Cumulative portal totals

| Measure | Value | Qualification |
| --- | ---: | --- |
| Completed measured portal intervals | 8 | Route-based Tester workspace, shared shell/rail refinements, Phase 1 workbench redesign, CI trigger isolation, Phase 2 record-page workbench, and Phase 3 assignment lifecycle handoffs. |
| Active portal time | 1.01 h | Completed intervals only. |
| Automated wait | 0.03 h | Measured release-gate/build and workflow waits during portal changes. |
| User-blocked time | 0.00 h | Completed intervals only. |
| Counted portal time | 1.04 h | Active plus automated wait. |

## Portal work records

### Phase 4 Coordinator queue discovery — in progress

- **Authorization and start:** Authorized and started `August 22, 2026 at 10:17:53 AM PDT (UTC-07:00)`.
- **Scope:** Add protected, server-derived Coordinator saved views for operational attention, assignment readiness, smoke-test follow-up, submitted reports, active work, and closed work. Preserve all existing evidence, task, reporting, mail, and permission contracts; no migration is planned.
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
