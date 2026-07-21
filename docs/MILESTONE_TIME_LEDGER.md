# Milestone real-world time ledger

Last updated: `July 20, 2026, 7:32:41 PM PDT (UTC-07:00)`

This is the permanent cumulative time record for the canonical M01–M60 roadmap. It separates measured work from
calendar passage and preserves uncertainty where older records did not contain execution timers. Historical commit,
pull-request, and workflow timestamps are evidence checkpoints; they are not silently converted into active work.

## Cumulative project totals

| Measure | Cumulative value |
| --- | --- |
| Total milestones planned | 60 canonical milestones |
| Total milestones completed | 33 (M01–M28 and M31–M35) |
| Total confirmed or reconstructed active hours | Unknown; no historical active-interval log exists |
| Total automated wait hours | Unknown; workflow runtimes exist, but historical blocking/wait intervals and local validator durations were not recorded |
| Total user-blocked hours | Unknown; several owner/external gates are documented without complete pause/resume timestamps |
| Total counted project hours | Unknown |
| Total elapsed hours across completed milestones | At least 300.49 h across 25 observable evidence windows; 8 completed milestones have unknown elapsed duration |
| Average counted hours per completed milestone | Unknown |
| Median counted hours per completed milestone | Unknown |
| Longest and shortest completed milestones | Unknown by counted time. Supplemental observable windows: M34 is longest at least 90.27 h; M31 is shortest at least 0.03 h |
| Overall forecast versus actual variance | Unknown; only M13, M14, and M17–M21 retained milestone-specific forecasts, and their actual counted time was not recorded |
| Confidence classifications | Confirmed: 0; Reconstructed: 0; Estimated: 28; Unknown: 32 |
| Ledger last updated | `July 20, 2026, 7:32:41 PM PDT (UTC-07:00)` |

The 300.49-hour figure is a sum of per-milestone minimum observable wall-clock windows. Milestones overlapped, so it
must not be interpreted as 300.49 unique calendar hours or counted project hours. The confidence counts cover all 60
canonical entries: 25 completed observable windows, the in-progress M29/M30 start proxies, and the M51 research-to-
retirement window are Estimated; every entry without a defensible historical duration is Unknown.

## Definitions and operating rules

- **Active execution time** is time spent examining, implementing, testing, documenting, or committing milestone work.
- **Automated wait time** is time spent waiting for builds, tests, installations, deployments, or GitHub Actions.
- **User-blocked time** is time spent waiting for approval, credentials, external access, decisions, or other user action.
- **Total elapsed time** is wall-clock time from actual milestone execution start through completion.
- **Counted project time** is active execution time plus automated wait time. User-blocked time is excluded unless a
  milestone entry explicitly says otherwise.
- Displayed totals round to two decimals. Timestamps use a 12-hour clock and retain the full date, seconds, timezone
  abbreviation, and UTC offset as the calculation source, for example `July 20, 2026, 7:32:41 PM PDT (UTC-07:00)`.
- For every newly authorized milestone, open an in-progress entry immediately with actual start time, approved model,
  reasoning strength, and the forecast that exists at authorization. Planning and reasoning-gate discussion before
  authorization is not execution time.
- Close active intervals whenever execution stops. Record automated and user-blocked pauses with exact start, reason,
  and resume timestamps. Do not classify a long response gap as active work.
- At completion, calculate category totals, counted time, total elapsed time, forecast difference in hours and percent,
  cumulative totals, variance explanation, and forecasting lessons before committing the milestone documentation.

## Historical reconstruction method

1. Canonical identity and status come from [ROADMAP.md](ROADMAP.md) and
   [MILESTONE_MIGRATION.md](MILESTONE_MIGRATION.md). GitHub Project #1 independently reports 60 canonical items and
   corroborates the 33 `Complete` states and their evidence checkpoints.
2. A timestamp marked with `*` is the earliest milestone-specific repository checkpoint found, not a recorded actual
   execution start. Its start-to-completion span is therefore a **minimum observable window**.
3. Completion uses the explicit completion/acceptance commit or merged milestone PR named by the current validation
   documents. When only one checkpoint exists, elapsed duration is Unknown rather than `0.00 h`.
4. GitHub Actions runs corroborate validation, but an asynchronous run does not prove that Codex or a contributor
   waited for its full runtime. Historical automated wait therefore remains Unknown unless a future entry records the
   wait explicitly.
5. Gaps between commits are not classified as active, automated, or user-blocked time without pause evidence.
6. No tags or GitHub Releases existed when this reconstruction was performed. GitHub had 19 PRs; #14–#19 were open
   Dependabot proposals and were not milestone completion records.
7. Historical model labels are recorded only where a milestone document names them. Later planning routes are not
   backdated as the model used for earlier execution.

Confidence classifications mean:

- **Confirmed** — actual execution start and completion timestamps are recorded.
- **Reconstructed** — reliable records support both endpoints and a defensible duration.
- **Estimated** — the available window is incomplete, normally because its start is an earliest-evidence proxy.
- **Unknown** — evidence cannot support a defensible duration.

## Milestone summary

`Unknown` never means zero work. `—` means the milestone is not completed or the field does not yet apply.

| Milestone | Status | Started | Completed | Forecast | Active | Automated Wait | User-Blocked | Counted Project Time | Total Elapsed | Variance | Confidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| M01 | Complete | `July 12, 2026, 9:29:06 PM PDT (UTC-07:00)`* | `July 13, 2026, 2:17:48 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥16.81 h | Unknown | Estimated |
| M02 | Complete | `July 13, 2026, 4:39:00 AM PDT (UTC-07:00)`* | `July 13, 2026, 2:48:48 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥10.16 h | Unknown | Estimated |
| M03 | Complete | `July 13, 2026, 3:09:06 PM PDT (UTC-07:00)`* | `July 13, 2026, 3:53:17 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.74 h | Unknown | Estimated |
| M04 | Complete | `July 13, 2026, 4:42:09 PM PDT (UTC-07:00)`* | `July 13, 2026, 5:25:46 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.73 h | Unknown | Estimated |
| M05 | Complete | Unknown | `July 13, 2026, 6:16:52 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M06 | Complete | `July 13, 2026, 6:47:29 PM PDT (UTC-07:00)`* | `July 13, 2026, 7:48:32 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥1.02 h | Unknown | Estimated |
| M07 | Complete | `July 13, 2026, 8:08:37 PM PDT (UTC-07:00)`* | `July 13, 2026, 9:27:20 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥1.31 h | Unknown | Estimated |
| M08 | Complete | `July 13, 2026, 9:38:35 PM PDT (UTC-07:00)`* | `July 13, 2026, 10:11:34 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.55 h | Unknown | Estimated |
| M09 | Complete | `July 13, 2026, 10:46:18 PM PDT (UTC-07:00)`* | `July 13, 2026, 11:03:44 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.29 h | Unknown | Estimated |
| M10 | Complete | `July 14, 2026, 5:48:13 AM PDT (UTC-07:00)`* | `July 14, 2026, 2:53:36 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥9.09 h | Unknown | Estimated |
| M11 | Complete | Unknown | `July 14, 2026, 8:41:24 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M12 | Complete | Unknown | `July 14, 2026, 8:41:24 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M13 | Complete | Unknown | `July 14, 2026, 10:07:42 PM PDT (UTC-07:00)` | 4–8 active h | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M14 | Complete | `July 14, 2026, 10:30:36 PM PDT (UTC-07:00)`* | `July 14, 2026, 10:32:47 PM PDT (UTC-07:00)` | 2–4 active h | Unknown | Unknown | Unknown | Unknown | ≥0.04 h | Unknown | Estimated |
| M15 | Complete | `July 14, 2026, 10:57:06 PM PDT (UTC-07:00)`* | `July 14, 2026, 11:05:19 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.14 h | Unknown | Estimated |
| M16 | Complete | `July 14, 2026, 11:29:27 PM PDT (UTC-07:00)`* | `July 14, 2026, 11:38:09 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.14 h | Unknown | Estimated |
| M17 | Complete | Unknown | `July 14, 2026, 11:58:33 PM PDT (UTC-07:00)` | 2–4 active h | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M18 | Complete | `July 15, 2026, 12:14:10 AM PDT (UTC-07:00)`* | `July 15, 2026, 5:53:22 AM PDT (UTC-07:00)` | 4–7 active h | Unknown | Unknown | Unknown | Unknown | ≥5.65 h | Unknown | Estimated |
| M19 | Complete | `July 15, 2026, 12:37:03 AM PDT (UTC-07:00)`* | `July 15, 2026, 6:07:04 AM PDT (UTC-07:00)` | 4–7 active h | Unknown | Unknown | Unknown | Unknown | ≥5.50 h | Unknown | Estimated |
| M20 | Complete | `July 15, 2026, 5:09:42 AM PDT (UTC-07:00)`* | `July 15, 2026, 9:57:52 AM PDT (UTC-07:00)` | 6–10 active h | Unknown | Unknown | Unknown | Unknown | ≥4.80 h | Unknown | Estimated |
| M21 | Complete | `July 15, 2026, 10:22:34 AM PDT (UTC-07:00)`* | `July 15, 2026, 10:25:07 AM PDT (UTC-07:00)` | 4–7 active h | Unknown | Unknown | Unknown | Unknown | ≥0.04 h | Unknown | Estimated |
| M22 | Complete | `July 15, 2026, 12:09:53 PM PDT (UTC-07:00)`* | `July 15, 2026, 8:59:21 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥8.82 h | Unknown | Estimated |
| M23 | Complete | Unknown | `July 15, 2026, 10:32:39 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M24 | Complete | `July 16, 2026, 4:46:35 PM PDT (UTC-07:00)`* | `July 16, 2026, 5:01:06 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.24 h | Unknown | Estimated |
| M25 | Complete | `July 16, 2026, 5:18:02 PM PDT (UTC-07:00)`* | `July 16, 2026, 5:50:41 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.54 h | Unknown | Estimated |
| M26 | Complete | `July 16, 2026, 6:07:48 PM PDT (UTC-07:00)`* | `July 16, 2026, 6:21:05 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.22 h | Unknown | Estimated |
| M27 | Complete | `July 16, 2026, 7:05:07 PM PDT (UTC-07:00)`* | `July 16, 2026, 7:09:16 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.07 h | Unknown | Estimated |
| M28 | Complete | `July 15, 2026, 10:32:07 PM PDT (UTC-07:00)`* | `July 18, 2026, 3:04:15 PM PDT (UTC-07:00)` | Not recorded individually | Unknown | Unknown | Unknown | Unknown | ≥64.54 h | Unknown | Estimated |
| M29 | Waiting externally | `July 15, 2026, 10:42:33 PM PDT (UTC-07:00)`* | — | Original not recorded; current 2–5 active d | Unknown | Unknown | Unknown | Unknown | Ongoing | — | Estimated |
| M30 | Waiting externally | `July 15, 2026, 10:59:35 PM PDT (UTC-07:00)`* | — | Original not recorded; current 0.5–1 active d | Unknown | Unknown | Unknown | Unknown | Ongoing | — | Estimated |
| M31 | Complete | `July 18, 2026, 5:30:30 PM PDT (UTC-07:00)`* | `July 18, 2026, 5:32:28 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.03 h | Unknown | Estimated |
| M32 | Complete | Unknown | `July 18, 2026, 6:21:38 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M33 | Complete | Unknown | `July 18, 2026, 7:43:32 PM PDT (UTC-07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M34 | Complete | `July 15, 2026, 12:09:53 PM PDT (UTC-07:00)`* | `July 19, 2026, 6:25:48 AM PDT (UTC-07:00)` | Not recorded individually | Unknown | Unknown | Unknown | Unknown | ≥90.27 h | Unknown | Estimated |
| M35 | Complete | `July 15, 2026, 10:57:12 PM PDT (UTC-07:00)`* | `July 19, 2026, 5:41:32 AM PDT (UTC-07:00)` | Not recorded individually | Unknown | Unknown | Unknown | Unknown | ≥78.74 h | Unknown | Estimated |
| M36 | Waiting externally | — | — | 1–3 active Sol d after trigger | — | — | — | — | — | — | Unknown |
| M37 | Planned after M36 | — | — | 5–10 active d | — | — | — | — | — | — | Unknown |
| M38 | Planned after M37 | — | — | 5–10 active d | — | — | — | — | — | — | Unknown |
| M39 | Planned | — | — | 1–2 active d | — | — | — | — | — | — | Unknown |
| M40 | Planned after M39 | — | — | 3–10 calendar d | — | — | — | — | — | — | Unknown |
| M41 | Planned after M40 | — | — | 0.5–1 active d | — | — | — | — | — | — | Unknown |
| M42 | Planned after M41 | — | — | 3–5 calendar wk | — | — | — | — | — | — | Unknown |
| M43 | Planned after M42 | — | — | 1–2 calendar wk | — | — | — | — | — | — | Unknown |
| M44 | Planned after M43 | — | — | 1–4 calendar wk | — | — | — | — | — | — | Unknown |
| M45 | Planned recurring | — | — | Monthly, quarterly, and release-triggered | — | — | — | — | — | — | Unknown |
| M46 | Planned post-Alpha | — | — | 1–3 active wk per approved slice | — | — | — | — | — | — | Unknown |
| M47 | Deferred | — | — | External repair; then 1–3 active wk | — | — | — | — | — | — | Unknown |
| M48 | Deferred after M47 | — | — | 1–2 active wk | — | — | — | — | — | — | Unknown |
| M49 | Deferred after M48 | — | — | 1–2 active wk | — | — | — | — | — | — | Unknown |
| M50 | Deferred after M49/M38 | — | — | 2–4 active wk | — | — | — | — | — | — | Unknown |
| M51 | Retired | `July 19, 2026, 7:37:21 AM PDT (UTC-07:00)`* | Retired `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)` | No forecast | Unknown | Unknown | Unknown | Unknown | ≥1.30 h decision window | — | Estimated |
| M52 | Retired | — | Retired `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)` | No forecast | — | — | — | — | — | — | Unknown |
| M53 | Retired | — | Retired `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)` | No forecast | — | — | — | — | — | — | Unknown |
| M54 | Retired | — | Retired `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)` | No forecast | — | — | — | — | — | — | Unknown |
| M55 | Planned research gate | — | — | 2–4 active wk; may be no-go | — | — | — | — | — | — | Unknown |
| M56 | Planned research gate | — | — | 1–3 active wk | — | — | — | — | — | — | Unknown |
| M57 | Planned research gate | — | — | 1–3 active wk | — | — | — | — | — | — | Unknown |
| M58 | Planned authorization gate | — | — | 2–4 active wk; contract time external | — | — | — | — | — | — | Unknown |
| M59 | Planned after M58 | — | — | 3–6 active wk | — | — | — | — | — | — | Unknown |
| M60 | Planned after M59 | — | — | 2–4 active wk | — | — | — | — | — | — | Unknown |

## Historical GitHub corroboration

The final linked Action is evidence that validation automation ran; its duration is not counted as historical wait
without a contemporaneous record that milestone execution was blocked on it. PR #1 contains the original M01–M23
development series. M24–M27 have focused milestone PRs. M28 and M31–M35 completed through direct commits.

| Milestone | Earliest evidence | Completion/decision evidence | PR | Relevant final Action |
| --- | --- | --- | --- | --- |
| M01 | `252716e` | `e8eb7da` | #1 | [run 29285804400](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29285804400) |
| M02 | `b92da5a` | `5cdade0` | #1 | [run 29287729689](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29287729689) |
| M03 | `ce5f0f9` | `4206fee` | #1 | [run 29291330588](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29291330588) |
| M04 | `f4b9c77` | `0d9e6b3` | #1 | [run 29295894553](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29295894553) |
| M05 | — | `8133695` | #1 | [run 29298161047](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29298161047) |
| M06 | `f7ef354` | `3babdf9` | #1 | [run 29301974459](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29301974459) |
| M07 | `bb416d0` | `a897794` | #1 | [run 29306021007](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29306021007) |
| M08 | `82119ca` | `f99635d` | #1 | [run 29307897241](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29307897241) |
| M09 | `e748f27` | `5c50582` | #1 | [run 29310250996](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29310250996) |
| M10 | `635139d` | `73a0d89` | #1 | [run 29371171780](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29371171780) |
| M11 | — | `4735f13` | #1 | [run 29387151240](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29387151240) |
| M12 | — | `4735f13` | #1 | [run 29387151240](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29387151240) |
| M13 | — | `9ef1f1c` | #1 | No commit-matched run found |
| M14 | `81c2c4e` | `9100f10` | #1 | [run 29391738519](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29391738519) |
| M15 | `b19d5fe` | `f16f408` | #1 | [run 29393212571](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29393212571) |
| M16 | `90a7f98` | `d69c76d` | #1 | [run 29394803688](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29394803688) |
| M17 | — | `02dffa2` | #1 | [run 29395841997](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29395841997) |
| M18 | `f877adc` | `11f1ee3` | #1 | [run 29416969934](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29416969934) |
| M19 | `50541ea` | `5e985ae` | #1 | [run 29417887444](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29417887444) |
| M20 | `5a4bdb7` | `6757680` | #1 | [run 29434576408](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29434576408) |
| M21 | `ec5fb20` | `1e20eea` | #1 | [run 29436424981](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29436424981) |
| M22 | `252e3a4` | `44632f1` | #1 | No commit-matched final run found |
| M23 | — | `924a38c` | #1 | [run 29474194397](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29474194397) |
| M24 | `b713783` | `feb24f7` | [#7](https://github.com/codeframe78/24Seven.FM-Player/pull/7) | [run 29543579299](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29543579299) |
| M25 | `85fbca3` | `3588c7d` | [#8](https://github.com/codeframe78/24Seven.FM-Player/pull/8) | PR and local validation; no completion-commit run match |
| M26 | `7aedbfc` | `32b7355` | [#9](https://github.com/codeframe78/24Seven.FM-Player/pull/9) | [run 29547213689](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29547213689) |
| M27 | `d902cbd` | `afd563a` | [#11](https://github.com/codeframe78/24Seven.FM-Player/pull/11) | PR Actions recorded; no merge-commit run match |
| M28 | `c6a9450` | `9abd69f` | — | [run 29662811708](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29662811708) |
| M29 | `4aa36d9` | Open | — | Multiple current evidence runs; no completion run |
| M30 | `c0e37ae` | Open | — | No completion run |
| M31 | `9e340b9` | `91794bb` | — | [run 29667203553](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29667203553) |
| M32 | — | `2bc45f7` | — | Later evidence commits/runs exist; no completion-commit run match |
| M33 | — | `572d419` | — | Later evidence commits/runs exist; no completion-commit run match |
| M34 | `252e3a4` | `6ea021f` | #13 contributed TalkBack evidence | Later acceptance sync runs exist; no completion-commit run match |
| M35 | `2e839cc` | `97b732d` | #12 contributed verification evidence | [run 29687462040](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29687462040) |
| M51 | `cb37b03` | retired by `8b9980f` | — | [run 29693823405](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29693823405) |

Repository commit links can be formed as `https://github.com/codeframe78/24Seven.FM-Player/commit/<full-sha>`; short
hashes above match the local repository. There were no relevant tags or releases.

## Detailed milestone records

Unless a record overrides it, historical active, automated-wait, and user-blocked intervals are Unknown; counted time
and forecast variance are therefore Unknown. The default variance explanation is that a commit span measures neither
active execution nor blocking wait. The default forecasting lesson is to record authorization/start, validator
begin/end, pauses/resumes, and acceptance in this ledger during execution.

### M01 — Buildable baseline

- **Objective:** Establish a buildable native Kotlin/Compose Android baseline with pinned tooling and verified Windows
  and Ubuntu development paths.
- **Timestamps and intervals:** Earliest repository evidence `July 12, 2026, 9:29:06 PM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 2:17:48 PM PDT (UTC-07:00)`. Active, automated-wait, and user-blocked intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted project time Unknown; minimum observable elapsed 16.81 h; forecast variance Unknown.
- **Evidence/confidence:** `252716e`, `9184afd`, `e8eb7da`, [m1-validation.md](m1-validation.md), PR #1, and the M01
  Action in the evidence index. **Estimated** because the initial commit is only a start proxy.
- **Variance explanation and lesson:** An overnight gap and later revalidation cannot be categorized. Future baseline
  work must open an execution interval before inspection or scaffolding begins.

### M02 — Five-station playback

- **Objective:** Deliver verified five-station Media3 playback, atomic switching, and bounded primary/source fallback.
- **Timestamps and intervals:** Earliest evidence `July 13, 2026, 4:39:00 AM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 2:48:48 PM PDT (UTC-07:00)`. Active, automated-wait, and user-blocked intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 10.16 h; variance Unknown.
- **Evidence/confidence:** `b92da5a`, `7c7351d`, `d517713`, `5cdade0`, [m2-validation.md](m2-validation.md), PR #1,
  and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** Device/network validation spans cannot be separated from other gaps. Record each
  device test and automated validator as its own interval.

### M03 — Background and system media

- **Objective:** Validate service-owned background playback, MediaSession controls, audio focus, and accessory behavior.
- **Timestamps and intervals:** Earliest evidence `July 13, 2026, 3:09:06 PM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 3:53:17 PM PDT (UTC-07:00)`. Historical category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.74 h; variance Unknown.
- **Evidence/confidence:** `ce5f0f9`, `66e401c`, `9263993`, `4206fee`, [m3-validation.md](m3-validation.md), PR #1,
  and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** The first commit follows unrecorded validation work. Start timing before the
  first lifecycle or hardware check.

### M04 — Now Playing

- **Objective:** Add and validate live ICY metadata with safe same-station artwork enrichment.
- **Timestamps and intervals:** Earliest evidence `July 13, 2026, 4:42:09 PM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 5:25:46 PM PDT (UTC-07:00)`. Historical category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.73 h; variance Unknown.
- **Evidence/confidence:** `f4b9c77`, `0d9e6b3`, later addenda `ae34a26`/`7aced76`,
  [m4-metadata-research.md](m4-metadata-research.md), PR #1, and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** Later artwork hardening is a post-completion addendum and is not folded into the
  original window. Track follow-up scope under a named milestone or explicit addendum interval.

### M05 — Native navigation

- **Objective:** Deliver adaptive native destinations and the persistent mini-player.
- **Timestamps and intervals:** Actual start Unknown; completed `July 13, 2026, 6:16:52 PM PDT (UTC-07:00)`. All category intervals are
  Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence/confidence:** `8133695`, [m5-validation.md](m5-validation.md), PR #1, and the indexed Action. **Unknown**;
  a single completion checkpoint does not establish duration.
- **Variance explanation and lesson:** Never infer zero duration from a one-commit milestone. Record execution start
  before the first UI change.

### M06 — Queue and History

- **Objective:** Implement bounded station-scoped Queue and History observation.
- **Timestamps and intervals:** Earliest evidence `July 13, 2026, 6:47:29 PM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 7:48:32 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 1.02 h; variance Unknown.
- **Evidence/confidence:** `f7ef354`, `3babdf9`, later presentation fixes `9a9ac02`/`dcae5a2`,
  [m6-validation.md](m6-validation.md), PR #1, and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** Later fixes are not assumed to extend the original milestone. Log follow-up work
  separately when it changes the accepted gate.

### M07 — Authentication

- **Objective:** Deliver native station login and Android-protected, station-isolated sessions.
- **Timestamps and intervals:** Earliest evidence `July 13, 2026, 8:08:37 PM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 9:27:20 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 1.31 h; variance Unknown.
- **Evidence/confidence:** `bb416d0` through `a897794`, [m7-validation.md](m7-validation.md), PR #1, and the indexed
  Action. **Estimated**.
- **Variance explanation and lesson:** The dense commit series is evidence of activity but not a stopwatch. Preserve
  validator and any credential/user-input wait boundaries prospectively.

### M08 — Chat

- **Objective:** Implement native, bounded, station-scoped Chat.
- **Timestamps and intervals:** Earliest evidence `July 13, 2026, 9:38:35 PM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 10:11:34 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.55 h; variance Unknown.
- **Evidence/confidence:** `82119ca`, `f99635d`, [m8-validation.md](m8-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Variance explanation and lesson:** Pre-commit research and validation are absent from the timing record. Start the
  ledger before protocol inspection.

### M09 — Song requests

- **Objective:** Add native search, explicit confirmation, and one-shot song-request submission.
- **Timestamps and intervals:** Earliest evidence `July 13, 2026, 10:46:18 PM PDT (UTC-07:00)`*; completed
  `July 13, 2026, 11:03:44 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.29 h; variance Unknown.
- **Evidence/confidence:** `e748f27`, `5c50582`, [m9-validation.md](m9-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Variance explanation and lesson:** The short window starts at an implementation commit, not actual execution.
  Record discovery, mutation-safety review, and test waits explicitly.

### M10 — Request attribution and messages

- **Objective:** Add Queue attribution and the verified SST optional request-message flow.
- **Timestamps and intervals:** Earliest evidence `July 14, 2026, 5:48:13 AM PDT (UTC-07:00)`*; completed
  `July 14, 2026, 2:53:36 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 9.09 h; variance Unknown.
- **Evidence/confidence:** `635139d` through `73a0d89`, [m10-validation.md](m10-validation.md), PR #1, and the indexed
  Action. **Estimated**.
- **Variance explanation and lesson:** The long checkpoint span includes unclassified gaps. Record live one-shot tests
  and recovery decisions without storing protected evidence.

### M11 — Adaptive Alpha UI

- **Objective:** Deliver the responsive Alpha shell, previews, semantics, and explicit exit flow while preserving the
  working native feature contracts.
- **Timestamps and intervals:** Actual start Unknown; completed `July 14, 2026, 8:41:24 PM PDT (UTC-07:00)`. Category intervals are
  Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence/confidence:** `4735f13`, [m11-validation.md](m11-validation.md),
  [m11-ui-preservation-plan.md](m11-ui-preservation-plan.md), PR #1, and the indexed Action. **Unknown**.
- **Variance explanation and lesson:** M11 and M12 landed together, and no independent start was recorded. Open separate
  intervals when one commit completes multiple milestone gates.

### M12 — Queue-aware request availability

- **Objective:** Add conservative fresh Queue/History eligibility and fail-closed pre-submit checks.
- **Timestamps and intervals:** Actual start Unknown; completed `July 14, 2026, 8:41:24 PM PDT (UTC-07:00)`. Category intervals are
  Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence/confidence:** `4735f13`, [m12-validation.md](m12-validation.md), PR #1, and the indexed Action. **Unknown**.
- **Variance explanation and lesson:** The shared completion commit does not reveal M12's start or effort share. Track
  concurrent milestone intervals independently.

### M13 — Independent Accounts UX

- **Objective:** Present five visible, independently actionable station-account states.
- **Timestamps and intervals:** Actual start Unknown; implementation completed `July 14, 2026, 10:07:42 PM PDT (UTC-07:00)`. Category
  intervals are Unknown.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. Original forecast 4–8 active h,
  task complexity level 2, size L.
- **Time and variance:** Counted and total elapsed time Unknown; forecast difference and percentage Unknown.
- **Evidence/confidence:** `9ef1f1c`, documentation `fc0a3c0`, [m13-validation.md](m13-validation.md), and PR #1.
  **Unknown** because there is no start checkpoint.
- **Variance explanation and lesson:** The forecast cannot be compared with a commit timestamp. Future forecasts must
  be snapshotted beside an actual start interval.

### M14 — Local personalization

- **Objective:** Add device-local startup/default-station preferences without mixing station-owned data.
- **Timestamps and intervals:** Earliest evidence `July 14, 2026, 10:30:36 PM PDT (UTC-07:00)`*; documentation completed
  `July 14, 2026, 10:32:47 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. Original forecast 2–4 active h,
  task complexity level 2, size M.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.04 h; forecast difference/percentage Unknown.
- **Evidence/confidence:** `81c2c4e`, `9100f10`, [m14-validation.md](m14-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Variance explanation and lesson:** A two-minute commit window cannot be compared with an active-hours forecast;
  it demonstrates that the execution start predates the proxy.

### M15 — Request activity and membership

- **Objective:** Add SST request history, cooldown/readiness, and explicit membership evidence.
- **Timestamps and intervals:** Earliest evidence `July 14, 2026, 10:57:06 PM PDT (UTC-07:00)`*; completed
  `July 14, 2026, 11:05:19 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.14 h; variance Unknown.
- **Evidence/confidence:** `b19d5fe`, `f16f408`, [m15-validation.md](m15-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Variance explanation and lesson:** The implementation commit is a late lower-bound proxy. Record protocol research
  and authenticated validation intervals prospectively.

### M16 — Secondary content access

- **Objective:** Add allowlisted same-station HTTPS Custom Tab access while retaining the no-WebView boundary.
- **Timestamps and intervals:** Earliest evidence `July 14, 2026, 11:29:27 PM PDT (UTC-07:00)`*; completed
  `July 14, 2026, 11:38:09 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.14 h; variance Unknown.
- **Evidence/confidence:** `90a7f98`, `d69c76d`, [m16-validation.md](m16-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Variance explanation and lesson:** M31 later narrowed the shipping catalog but did not erase M16. Track later policy
  corrections under their own milestone time.

### M17 — StreamingSoundtracks.com certification

- **Objective:** Certify public, authenticated, VIP, request, Favorites, and Chat behavior for SST.
- **Timestamps and intervals:** Actual start Unknown; certified `July 14, 2026, 11:58:33 PM PDT (UTC-07:00)`. Category intervals are
  Unknown.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. Original forecast 2–4 active h,
  task complexity level 2, size S.
- **Time and variance:** Counted and total elapsed time Unknown; forecast difference/percentage Unknown.
- **Evidence/confidence:** `02dffa2`, [m18-sst-certification.md](m18-sst-certification.md), PR #1, and the indexed Action.
  **Unknown** because only certification completion is timestamped.
- **Variance explanation and lesson:** Evidence-only certification can still consume live-test time; record the
  certification start even when no code change is expected.

### M18 — 1980s.FM certification

- **Objective:** Certify independent public, authenticated, and station-specific 1980s.FM behavior.
- **Timestamps and intervals:** Earliest evidence `July 15, 2026, 12:14:10 AM PDT (UTC-07:00)`*; completed
  `July 15, 2026, 5:53:22 AM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. Original forecast 4–7 active h,
  task complexity level 2, size M.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 5.65 h; forecast difference/percentage Unknown.
- **Evidence/confidence:** `f877adc`, `11f1ee3`, [m19-1980s-certification.md](m19-1980s-certification.md), PR #1, and
  the indexed Action. **Estimated**.
- **Variance explanation and lesson:** The observable wall-clock window falls inside the active forecast but cannot be
  treated as active time. Record user-entered CAPTCHA/account pauses separately.

### M19 — Adagio.FM certification

- **Objective:** Certify classical metadata and independent authenticated Adagio.FM workflows.
- **Timestamps and intervals:** Earliest evidence `July 15, 2026, 12:37:03 AM PDT (UTC-07:00)`*; completed
  `July 15, 2026, 6:07:04 AM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. Original forecast 4–7 active h,
  task complexity level 2, size M.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 5.50 h; forecast difference/percentage Unknown.
- **Evidence/confidence:** `50541ea`, `5e985ae`, [m20-adagio-certification.md](m20-adagio-certification.md), PR #1, and
  the indexed Action. **Estimated**.
- **Variance explanation and lesson:** As with M18, a matching wall-clock range is not an actual-time measurement.
  Explicitly time live account and device work in future certifications.

### M20 — Death.FM certification

- **Objective:** Certify compact Queue, sparse metadata, RIP boundaries, and independent authenticated Death.FM flows.
- **Timestamps and intervals:** Earliest evidence `July 15, 2026, 5:09:42 AM PDT (UTC-07:00)`*; completed
  `July 15, 2026, 9:57:52 AM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. Original forecast 6–10 active h,
  task complexity level 2, size L.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 4.80 h; forecast difference/percentage Unknown.
- **Evidence/confidence:** `5a4bdb7`, `6757680`, [m21-death-certification.md](m21-death-certification.md), PR #1, and
  the indexed Action. **Estimated**.
- **Variance explanation and lesson:** The minimum window is shorter than the active forecast because work before the
  first evidence commit is missing; it is not proof of an underestimate or overestimate.

### M21 — Entranced.FM certification

- **Objective:** Certify extended Queue, legacy ICY punctuation, and authenticated Entranced.FM workflows.
- **Timestamps and intervals:** Earliest evidence `July 15, 2026, 10:22:34 AM PDT (UTC-07:00)`*; completed
  `July 15, 2026, 10:25:07 AM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. Original forecast 4–7 active h,
  task complexity level 2, size M.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.04 h; forecast difference/percentage Unknown.
- **Evidence/confidence:** `ec5fb20`, `1e20eea`, [m22-entranced-certification.md](m22-entranced-certification.md), PR #1,
  and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** The very short final-commit window proves the start proxy is incomplete. Start
  the ledger before live relay and authenticated checks.

### M22 — Android 16 and API 36 readiness

- **Objective:** Validate API 36 targeting, edge-to-edge and predictive Back behavior, and platform regressions.
- **Timestamps and intervals:** Earliest relevant device/layout evidence `July 15, 2026, 12:09:53 PM PDT (UTC-07:00)`*; completed
  `July 15, 2026, 8:59:21 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded individually.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 8.82 h; variance Unknown.
- **Evidence/confidence:** `252e3a4`, `44632f1`, [m23-api36-readiness.md](m23-api36-readiness.md), PR #1, and related
  Actions. **Estimated**.
- **Variance explanation and lesson:** Device work overlaps M34's later acceptance program. Concurrent milestones need
  separate interval attribution instead of duplicating the same active minutes.

### M23 — Adaptive launcher and store polish

- **Objective:** Deliver and validate legacy, adaptive, and monochrome launcher resources.
- **Timestamps and intervals:** Actual start Unknown; completed `July 15, 2026, 10:32:39 PM PDT (UTC-07:00)`. Category intervals are
  Unknown.
- **Model/reasoning and original forecast:** Not recorded individually.
- **Time and variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence/confidence:** `924a38c`, [m23-launcher-polish.md](m23-launcher-polish.md), PR #1, and the indexed Action.
  **Unknown**.
- **Variance explanation and lesson:** Image/resource production time is absent from Git. Open the timer before asset
  inspection or generation.

### M24 — Sleep Timer

- **Objective:** Add a bounded, service-owned timer with restoration, adjustment, cancellation, and deterministic expiry.
- **Timestamps and intervals:** Earliest evidence `July 16, 2026, 4:46:35 PM PDT (UTC-07:00)`*; merged/completed
  `July 16, 2026, 5:01:06 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.24 h; variance Unknown.
- **Evidence/confidence:** `b713783`, `1a0516e`, merge `feb24f7`, [m24-sleep-timer-validation.md](m24-sleep-timer-validation.md),
  PR #7, and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** The first commit already contains implementation; PR duration is not total
  execution. Begin measurement before code examination and record local Gradle/device waits.

### M25 — System Audio-Output Selection

- **Objective:** Add Android-managed output selection and accurate audio-policy route state without adding Cast.
- **Timestamps and intervals:** Earliest evidence `July 16, 2026, 5:18:02 PM PDT (UTC-07:00)`*; merged/completed
  `July 16, 2026, 5:50:41 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.54 h; variance Unknown.
- **Evidence/confidence:** `85fbca3`, `183ccc0`, `68e5ab8`, merge `3588c7d`,
  [m25-audio-output-validation.md](m25-audio-output-validation.md), and PR #8. **Estimated**.
- **Variance explanation and lesson:** The physical correction is visible in commits, but device handoff/test wait is
  unmeasured. Time physical validation separately from implementation.

### M26 — In-App Diagnostics

- **Objective:** Add a privacy-safe, user-reviewed, fixed-allowlist diagnostics snapshot with explicit copy/share.
- **Timestamps and intervals:** Earliest evidence `July 16, 2026, 6:07:48 PM PDT (UTC-07:00)`*; merged/completed
  `July 16, 2026, 6:21:05 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.22 h; variance Unknown.
- **Evidence/confidence:** `7aedbfc`, `3aa0b74`, merge `32b7355`, [m26-diagnostics-validation.md](m26-diagnostics-validation.md),
  PR #9, and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** Implementation predates the start proxy. Record privacy review as active work and
  Jekyll/Gradle/device runs as automated waits when actually awaited.

### M27 — Local Chat-Mention Notifications

- **Objective:** Add opt-in exact-name notifications from the actively observed Chat feed without claiming closed-app push.
- **Timestamps and intervals:** Earliest evidence `July 16, 2026, 7:05:07 PM PDT (UTC-07:00)`*; merged/completed
  `July 16, 2026, 7:09:16 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.07 h; variance Unknown.
- **Evidence/confidence:** `d902cbd`, merge `afd563a`, [m27-community-notification-validation.md](m27-community-notification-validation.md),
  and PR #11. **Estimated**.
- **Variance explanation and lesson:** The four-minute commit-to-merge window excludes implementation. A focused PR is
  useful evidence but not a start timer.

### M28 — UGC Safety and Ongoing Moderation

- **Objective:** Add Terms/age/reveal safeguards, report and local block controls, a fixed-recipient user-reviewed email
  handoff, physical composer validation, and an owner-confirmed one-shot receipt check.
- **Timestamps and intervals:** Earliest implementation evidence `July 15, 2026, 10:32:07 PM PDT (UTC-07:00)`*; completed
  `July 18, 2026, 3:04:15 PM PDT (UTC-07:00)`. Active, automated-wait, and user-blocked intervals are Unknown; owner input is documented
  but its exact pause/resume boundary is not.
- **Model/reasoning and original forecast:** Execution model/reasoning not recorded. The later M28–M35 umbrella estimate
  was 3–6 focused days plus owner/station input; no original individual M28 forecast was retained.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 64.54 h; individual forecast variance Unknown.
- **Evidence/confidence:** `c6a9450`, `465cb4d`, `9abd69f`, [m23-ugc-safety-research.md](m23-ugc-safety-research.md),
  [m23-ugc-safety-validation.md](m23-ugc-safety-validation.md), and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** The multi-day wall-clock window includes unmeasured owner/manual gates. Future
  externally assisted milestones must log the user-blocked pause when the request is sent and resume when evidence arrives.

### M29 — Play Declarations, Privacy, and Data Safety

- **Objective:** Reconcile the exact artifact, reviewer access, audience/content/UGC classification, privacy/Data Safety,
  station retention/deletion/processor/IP facts, account boundaries, and media-playback evidence.
- **Timestamps and intervals:** Earliest Play-declaration evidence `July 15, 2026, 10:42:33 PM PDT (UTC-07:00)`*. Not completed. The
  milestone is waiting externally; exact historical active intervals and the start of the user-blocked pause are Unknown.
- **Model/reasoning and original forecast:** Original execution model/reasoning and forecast were not recorded at start.
  Current routing, added `July 19, 2026, 6:17:38 AM PDT (UTC-07:00)`, is Sol High with 2–5 active days after all named facts/candidate exist.
- **Time and variance:** Active, wait, blocked, counted, and total elapsed durations remain Unknown/ongoing; variance is
  not calculated before completion.
- **Evidence/confidence:** `4aa36d9`, `0fa5040`, `59d6096`, `849b5a0`, `93e8110`, `d3467cf`,
  [m23-play-declaration-packet.md](m23-play-declaration-packet.md), and [m23-data-safety.md](m23-data-safety.md).
  **Estimated** start proxy.
- **Variance explanation and lesson:** Do not backdate the July 19 forecast as original. On resume, record a real resume
  timestamp and retain the current external wait as Unknown rather than inventing a duration.

### M30 — Brand, Content, and Distribution Rights

- **Objective:** Obtain and privately retain written authorization for naming, branding, station assets/metadata,
  streams, screenshots, and Google Play testing/distribution.
- **Timestamps and intervals:** Earliest rights-response packet evidence `July 15, 2026, 10:59:35 PM PDT (UTC-07:00)`*. Not completed.
  The milestone is waiting externally; exact active intervals and the user-blocked pause start are Unknown.
- **Model/reasoning and original forecast:** Original model/reasoning and forecast were not recorded at start. Current
  routing, added July 19, is Sol High with 0.5–1 active day after written authorization arrives.
- **Time and variance:** Active, wait, blocked, counted, and total elapsed durations remain Unknown/ongoing; variance is
  not calculated before completion.
- **Evidence/confidence:** `c0e37ae`, [m23-owner-response-packet.md](m23-owner-response-packet.md), roadmap `e249071`,
  and forecast `f971a9e`. **Estimated** start proxy.
- **Variance explanation and lesson:** External response time must remain separate from reconciliation work. Record the
  actual resume timestamp when authorization arrives.

### M31 — Payments, External Links, and Account Lifecycle Compliance

- **Objective:** Establish the Contact-only Alpha boundary and remove unapproved membership purchase, registration,
  account-management, and direct privacy browser routes.
- **Timestamps and intervals:** Earliest implementation evidence `July 18, 2026, 5:30:30 PM PDT (UTC-07:00)`*; completed
  `July 18, 2026, 5:32:28 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Sol Extra High policy/architecture audit, Terra-bounded implementation, and
  Sol High acceptance are recorded; original hours forecast was not recorded.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 0.03 h; variance Unknown.
- **Evidence/confidence:** `9e340b9`, `91794bb`,
  [m31-payments-account-lifecycle-validation.md](m31-payments-account-lifecycle-validation.md), and the indexed Action.
  **Estimated**.
- **Variance explanation and lesson:** The two-minute commit window omits the audit/implementation before commit. Model
  routing is not time evidence; future audits must open intervals for review, implementation, and acceptance.

### M32 — Session, Controller, Network, and Supply-Chain Security

- **Objective:** Harden MediaSession authority, protected station sessions, redirects, canonical IDs, and build integrity,
  with adversarial physical-device evidence.
- **Timestamps and intervals:** Actual start Unknown; completed `July 18, 2026, 6:21:38 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Exact execution model/reasoning and original forecast not recorded.
- **Time and variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence/confidence:** `2bc45f7`, follow-up evidence `1c50ca0`/`cec5d5e`,
  [m32-security-validation.md](m32-security-validation.md), and associated Actions. **Unknown**.
- **Variance explanation and lesson:** A security milestone with one implementation checkpoint cannot be timed from Git.
  Separately time threat review, code, adversarial tests, and acceptance.

### M33 — Request Transaction Integrity

- **Objective:** Bind each one-shot request to fresh station, account, Queue, readiness, and track identity with bounded
  duplicate suppression.
- **Timestamps and intervals:** Actual start Unknown; completed `July 18, 2026, 7:43:32 PM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Sol acceptance is recorded; reasoning strength and original forecast are not.
- **Time and variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence/confidence:** `572d419`, documentation `b7e10b6`,
  [m33-request-transaction-validation.md](m33-request-transaction-validation.md), and associated Actions. **Unknown**.
- **Variance explanation and lesson:** The acceptance document proves outcome, not start. Record architecture review,
  implementation, Gradle, device, and acceptance intervals independently.

### M34 — Device and Accessibility Acceptance

- **Objective:** Accept adaptive window/foldable, physical hinge, human TalkBack, Voice Access, keyboard/pointer, large
  text, network recovery, and current-device evidence.
- **Timestamps and intervals:** Earliest relevant evidence `July 15, 2026, 12:09:53 PM PDT (UTC-07:00)`*; completed
  `July 19, 2026, 6:25:48 AM PDT (UTC-07:00)`. Exact active, automated-wait, and user-blocked intervals are Unknown; human/physical
  checkpoints are documented without full pause/resume timing.
- **Model/reasoning and original forecast:** Sol High acceptance is recorded; original individual forecast was not.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 90.27 h; variance Unknown.
- **Evidence/confidence:** `252e3a4` through `6ea021f`, PR #13, [m23-device-compatibility.md](m23-device-compatibility.md),
  and [m23-accessibility-validation.md](m23-accessibility-validation.md). **Estimated**.
- **Variance explanation and lesson:** This is the longest observable window because it aggregates many devices and
  human checkpoints, not because 90.27 active hours were measured. Log each matrix campaign and manual gate separately.

### M35 — Release Signing and Console Eligibility

- **Objective:** Prove current-head protected signing, exact upload identity, local clean/update lineage, package
  registration, eligible version code, and a repeatable signed build.
- **Timestamps and intervals:** Earliest focused audit evidence `July 15, 2026, 10:57:12 PM PDT (UTC-07:00)`*; completed
  `July 19, 2026, 5:41:32 AM PDT (UTC-07:00)`. Active, automated-wait, and user-blocked intervals are Unknown; owner confirmation and
  protected build checkpoints lack complete pause boundaries.
- **Model/reasoning and original forecast:** Sol acceptance is recorded; reasoning strength and original individual
  forecast were not.
- **Time and variance:** Counted time Unknown; minimum observable elapsed 78.74 h; variance Unknown.
- **Evidence/confidence:** `2e839cc`, `89795ef`, `3b775b7`, PR #12, `55413bd`, `97b732d`,
  [m23-release-candidate-audit.md](m23-release-candidate-audit.md), and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** Protected signing and owner/Console checks expanded calendar time, but their
  exact wait is unknown. Future signing milestones must log protected-build start/end and owner-blocked intervals.

### M36 — Notification Event-Source Authorization

- **Objective:** Accept an authorized station event source, webhook, or privacy-compatible relay contract covering
  ownership, payloads, authentication, retention/deletion, abuse, outage behavior, and station support.
- **Timestamps/intervals:** Not started; no start, completion, active, automated-wait, or user-blocked interval is open.
- **Model/reasoning and forecast:** Current planned route Sol High; 1–3 active Sol days after an accountable operator
  supplies the triggering proposal. The original forecast will be snapshotted when authorization starts execution.
- **Time/variance/evidence/confidence:** No actual time or variance yet. Roadmap `e249071` and forecast `f971a9e`; no
  PR, release, tag, or workflow. **Unknown** until start. Record the external trigger and actual execution start separately.

### M37 — Secure Closed-App Notification Delivery

- **Objective:** Implement minimal opt-in, station/token-isolated event delivery with duplicate, sign-out, reinstall,
  and protected-session safeguards and no perpetual polling.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra High, escalating unresolved delivery/privacy choices to Sol High; current
  forecast 5–10 active days after M36 and a test environment are accepted.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no related delivery
  commit/PR/release/workflow. **Unknown**. Snapshot this range as original only when authorized.

### M38 — Notification Lifecycle and Privacy Certification

- **Objective:** Certify foreground/background/closed/idle/reboot/network behavior, latency, battery/traffic, payload
  privacy, deep-link safety, and all five station boundaries.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra High execution followed by Sol High acceptance; current forecast 5–10 active
  days after M37 is stable.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no implementation
  evidence yet. **Unknown**. Time device campaigns and automated suites as separate intervals.

### M39 — Alpha Candidate and Documentation Freeze

- **Objective:** Freeze exact signed artifacts, version, signer, dependencies, permissions, listing, release notes,
  roadmap/testing, and all release-critical findings.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol High; current forecast 1–2 active days after M29, M30, and M36–M38 complete.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no candidate-finish
  commit/PR/release/workflow. **Unknown**. Record the exact freeze authorization as start, not prior planning.

### M40 — Play Alpha Delivery and Pre-launch Remediation

- **Objective:** Upload the M39 artifact, inspect Play splits, validate Play install/update, complete reviewer access,
  and resolve pre-review/pre-launch findings.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra High, with Sol High release decisions; current forecast 3–10 calendar days
  after M39, with extra time only for actual findings.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no Play workflow,
  tag, or release exists. **Unknown**. Separate Play processing as automated/external wait from remediation work.

### M41 — Explicit Alpha Publication

- **Objective:** Publish only after explicit owner authorization, then verify availability, instructions, support, and
  rollback records.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol High; current forecast 0.5–1 active day after M40 and explicit authorization.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no publication,
  release, tag, PR, or workflow. **Unknown**. Authorization wait must be user-blocked and excluded from counted time.

### M42 — Closed-Test Operations and Stabilization

- **Objective:** Maintain qualifying tester continuity, collect structured feedback, fix/retest findings, review Play
  health, and satisfy campaign exit criteria.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra Medium with Sol High checkpoints; current forecast 3–5 calendar weeks,
  including the mandatory continuity window and a feedback/fix buffer.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no campaign record.
  **Unknown**. Separate active remediation, Play/test automation, and tester/user-blocked calendar time.

### M43 — Production Access and Policy Approval

- **Objective:** Submit the production-access questionnaire with testing, feedback, change, value, policy, and readiness
  evidence and resolve the Play response.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol Extra High; current forecast 1–2 calendar weeks after M42 evidence is complete.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no application or
  workflow record. **Unknown**. Track Play review separately from active response work.

### M44 — Production Release and Staged Rollout

- **Objective:** Run an explicitly authorized staged production rollout with health thresholds, pause/rollback,
  release notes, and update validation.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol Extra High; current forecast 1–4 calendar weeks after production access and
  rollout approval.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no production release,
  tag, PR, or workflow. **Unknown**. Count monitoring work and waits in their actual categories.

### M45 — Operational Reliability and Recertification

- **Objective:** Maintain Play health/support, privacy handling, outage/contract-drift procedures, rights/moderation,
  target/API/dependencies, and signing-key recovery.
- **Timestamps/intervals:** Not started as a recurring program; no actual interval is open.
- **Model/reasoning and forecast:** Terra Medium for routine work and Sol High for material policy/security; current
  cadence is monthly light, quarterly recertification, and release-triggered review.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no recurring report.
  **Unknown**. Each review cycle should be a dated sub-entry so recurring time is not one endless elapsed interval.

### M46 — Architecture Sustainability

- **Objective:** Address measured architecture/performance hotspots in approved slices while preserving immutable state,
  repository contracts, and native boundaries.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra Medium, with Sol High for major refactors; current forecast 1–3 active weeks
  per approved slice after Alpha evidence identifies a hotspot.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no slice evidence.
  **Unknown**. Forecast and time every approved slice separately.

### M47 — Private Messages Server Repair and Protocol Certification

- **Objective:** Wait for site-owner delivery repair, then certify limits, station isolation, error/indeterminate behavior,
  and operator-confirmed production behavior.
- **Timestamps/intervals:** Not started; no repair-trigger or actual interval is recorded.
- **Model/reasoning and forecast:** Sol Extra High; no calendar forecast before external repair, then 1–3 active weeks.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no repair evidence.
  **Unknown**. Do not start elapsed time until the repaired production-verification trigger is accepted.

### M48 — Native Private Message Reading

- **Objective:** Add bounded protected Inbox/Sent models and immutable native UI with complete states and privacy tests.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra High; current forecast 1–2 active weeks after M47.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no implementation.
  **Unknown**. Snapshot forecast/model when M47 closes and M48 is authorized.

### M49 — Private Message Compose, Reply, and Send

- **Objective:** Add explicit preview/confirmation, one-shot mutation, limits, CSRF/session safety, and indeterminate
  delivery recovery.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra High; current forecast 1–2 active weeks after M48 and explicit mutation authority.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no mutation evidence.
  **Unknown**. User/owner authorization waits must be recorded separately.

### M50 — Private Message Five-Station and Notification Certification

- **Objective:** Certify independent station behavior and optional privacy-minimized delivery through the authorized
  notification architecture.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol Extra High; current forecast 2–4 active weeks after M49 and M38.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no certification.
  **Unknown**. Time each station/device campaign rather than relying on one final commit.

### M51 — Verified Forum Links

- **Objective:** Historically, verify Forum links and browser fallback; the project subsequently retired all Forum access
  from Player scope.
- **Timestamps/intervals:** Earliest focused blocker evidence `July 19, 2026, 7:37:21 AM PDT (UTC-07:00)`*; retirement decision recorded
  `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)`. Category intervals are Unknown.
- **Model/reasoning and original forecast:** Not recorded; no implementation forecast remains after retirement.
- **Time/variance/evidence/confidence:** Counted time Unknown; minimum observable research-to-decision window 1.30 h;
  forecast variance not applicable. `cb37b03`, `8b9980f`, [m51-forum-links-research.md](m51-forum-links-research.md),
  and the indexed Action. **Estimated**.
- **Variance explanation and lesson:** This is a retired decision window, not a completed objective. Timeboxed research
  that ends in no-go/retirement should record its start and decision acceptance like any other milestone.

### M52 — Native Forum Read-Only Foundation

- **Objective:** Former native Forum retrieval/rendering scope; permanently retired.
- **Timestamps/intervals:** Never started; retired by project decision `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)`; no actual intervals.
- **Model/reasoning and forecast:** None; no forecast after retirement.
- **Time/variance/evidence/confidence:** No counted or elapsed time and no variance can be assigned. Roadmap `e249071`
  and retirement `8b9980f`; no implementation/PR/release/workflow. **Unknown** rather than zero.

### M53 — Authenticated Forum Participation

- **Objective:** Former Forum sign-in/post/reply/moderation scope; permanently retired.
- **Timestamps/intervals:** Never started; retired by project decision `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)`; no actual intervals.
- **Model/reasoning and forecast:** None; no forecast after retirement.
- **Time/variance/evidence/confidence:** No counted or elapsed time and no variance can be assigned. Roadmap `e249071`
  and retirement `8b9980f`; no implementation/PR/release/workflow. **Unknown** rather than zero.

### M54 — Forum Five-Station and Notification Certification

- **Objective:** Former Forum certification/notification scope; permanently retired.
- **Timestamps/intervals:** Never started; retired by project decision `July 19, 2026, 8:55:35 AM PDT (UTC-07:00)`; no actual intervals.
- **Model/reasoning and forecast:** None; no forecast after retirement.
- **Time/variance/evidence/confidence:** No counted or elapsed time and no variance can be assigned. Roadmap `e249071`
  and retirement `8b9980f`; no implementation/PR/release/workflow. **Unknown** rather than zero.

### M55 — Google Cast Feasibility and Certification

- **Objective:** Decide go/no-go only after permitted stream use, receiver compatibility, lifecycle/route behavior,
  rights review, and five-station validation.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol High; current forecast 2–4 active weeks after permission/receiver triggers and
  may end in an explicit no-go.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no Cast evidence.
  **Unknown**. A no-go remains a valid timed outcome when the gate is authorized.

### M56 — Extended Station Capability Certification

- **Objective:** Independently verify non-SST request messages, activity/cooldown, membership, and capability differences.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol High; current forecast 1–3 active weeks after controlled evidence/test accounts.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no certification.
  **Unknown**. Time each station separately and preserve account/user-blocked waits.

### M57 — Account Registration, Recovery, and Management Access

- **Objective:** Verify permitted station routes and deletion/recovery behavior, then choose an authorized trusted-browser
  or native contract without sharing browser/app sessions.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol High; current forecast 1–3 active weeks after route authorization and account facts.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no route evidence.
  **Unknown**. Record authorization and test-account waits as user-blocked, not active.

### M58 — Membership Commerce Authorization and Billing Architecture

- **Objective:** Establish station/merchant authority, billing/activation architecture, products, tax/support/refund,
  privacy, and security obligations.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol Extra High; current forecast 2–4 active weeks for architecture after written
  authority and a viable partner; contract timing remains external.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no commerce evidence.
  **Unknown**. Keep contract waiting outside counted project time.

### M59 — Native VIP/RIP Purchase and Activation

- **Objective:** Implement approved native purchase, pending, acknowledgement, activation, restore, and failure flows
  with server-verified, station/account-isolated entitlement.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Terra High with Sol Extra High escalation for unresolved policy/security; current
  forecast 3–6 active weeks after M58's contract is accepted.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no implementation.
  **Unknown**. Record sandbox/provider waits separately from engineering work.

### M60 — Subscription Lifecycle and Five-Station Certification

- **Objective:** Certify renewal, cancellation, grace, refund, revocation, restore, reinstall, device/account switching,
  accessibility, support, and independent station product behavior.
- **Timestamps/intervals:** Not started; all actual intervals and totals are not applicable.
- **Model/reasoning and forecast:** Sol Extra High acceptance; current forecast 2–4 active weeks after M59 and five-station
  sandbox availability.
- **Time/variance/evidence/confidence:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no certification.
  **Unknown**. Time lifecycle matrices and external sandbox availability independently.

## Interval log template for active milestones

Add rows in chronological order. Close an active interval before opening a wait interval so time is not double counted.

| Category | Started | Ended/resumed | Duration | Reason/evidence | Counted? |
| --- | --- | --- | --- | --- | --- |
| Active execution | `Month D, YYYY, h:mm:ss AM/PM TZ (UTC±HH:MM)` | `Month D, YYYY, h:mm:ss AM/PM TZ (UTC±HH:MM)` | 0.00 h | Examination/implementation/test/docs/commit activity | Yes |
| Automated wait | `Month D, YYYY, h:mm:ss AM/PM TZ (UTC±HH:MM)` | `Month D, YYYY, h:mm:ss AM/PM TZ (UTC±HH:MM)` | 0.00 h | Build/test/install/deploy/workflow URL | Yes |
| User-blocked | `Month D, YYYY, h:mm:ss AM/PM TZ (UTC±HH:MM)` | `Month D, YYYY, h:mm:ss AM/PM TZ (UTC±HH:MM)` | 0.00 h | Approval/credential/access/decision requested and received | No |

## Required milestone completion report

Every milestone completion report must include this block with calculated values:

> Milestone [NUMBER] time:
>
> Forecast: [HOURS OR RANGE]
>
> Counted project time: [HOURS]
>
> Total elapsed time: [HOURS]
>
> User-blocked time excluded: [HOURS]
>
> Forecast variance: [AMOUNT AND PERCENTAGE]
>
> Cumulative counted project time through Milestone [NUMBER]: [HOURS]

If an actual value remains unsupported, use `Unknown` and explain why; never replace missing evidence with `0.00`.
