# Onboarding Portal Time Ledger

Last updated: `August 22, 2026 at 8:59:21 AM PDT (UTC-07:00)`

This is the canonical time-accounting record for the protected 24Seven.FM Player onboarding portal: the Tester workspace, Coordinator workspace, onboarding wizard, assignment and reporting flows, and their safe website releases. It is deliberately independent of the Player Android and Play-operation milestones in `MILESTONE_TIME_LEDGER.md`.

Do not record portal-design, portal-maintenance, portal-validation, or portal-release work in any Player milestone ledger. Record each authorized portal interval here with its actual start and end, active/automated-wait/user-blocked time, scope, evidence, model, and forecast when supplied. Never infer an unmeasured interval.

## Cumulative portal totals

| Measure | Value | Qualification |
| --- | ---: | --- |
| Completed measured portal intervals | 5 | Route-based Tester workspace, shared shell/rail refinements, and Phase 1 workbench redesign. |
| Active portal time | 0.69 h | Completed intervals only. |
| Automated wait | 0.01 h | Measured GitHub workflow wait during the Phase 1 redesign. |
| User-blocked time | 0.00 h | Completed intervals only. |
| Counted portal time | 0.70 h | Active plus automated wait. |

## Portal work records

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

- **Authorization and start:** Authorized and started `August 22, 2026 at 9:17:07 AM PDT (UTC-07:00)`.
- **Status:** In progress. Scope is limited to making Android CI skip portal-only pull requests while retaining Android validation for Android modules, Gradle configuration/wrapper changes, and its own workflow edits. This work is recorded here, not in the Player milestone ledger.
- **Model and forecast:** GPT-5.6 Terra High; original forecast Unknown.
