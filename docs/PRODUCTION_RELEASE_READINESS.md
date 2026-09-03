# Production release readiness

Status: In progress for M44. Production access is granted; no production bundle has been signed, uploaded, or
published by this work.

## Candidate lineage

- Google Play accepted `0.1.0-alpha08`, version code 17, from candidate commit `55a6ea1` for closed testing.
- The readiness branch starts from remote `main` commit `9b60beb`, which descends from Alpha08 and adds the merged
  Release A onboarding/accessibility work, Release B consented feedback/diagnostics work, and their accepted follow-ups.
- The proposed first-production identity is `1.0.0`, version code 18. It is a new artifact and must receive its own
  exact-source, signing, Play-delivery, update, and device evidence; Alpha08 evidence does not automatically transfer.

## Production-access evidence

On September 2, 2026, the owner supplied a Google Play notice confirming production access for
`com.codeframe78.twentyfourseven.player`. The exact provider-side grant timestamp and the submitted questionnaire are
not retained in the repository. The notice proves that production publishing is available; it does not prove that a
production release has been created or published.

## Current readiness evidence

The clean `9b60beb` baseline passed:

- `:app:compileDebugKotlin`;
- 202/202 debug unit tests;
- `:app:lintDebug` with no errors and 48 warnings;
- `:app:compileDebugAndroidTestKotlin`;
- `:app:bundleRelease`, including release lint-vital and R8; and
- the protected Linux signing environment preflight.

The resulting local AAB was intentionally unsigned because no signing secret was requested or supplied. These checks
establish a buildable baseline, not a releasable artifact.

## Release-critical Cast correction

The Alpha08 lineage exposed a Cast sender configured for a custom receiver, but the active public
`https://player.jamesjennison.net/cast/` path returned HTTP 404. Read-only server inspection found the matching receiver
artifact preserved in the rollback created by the August 22 Player-site replacement. That replacement artifact had
omitted the `cast/` directory because the site build did not compose the receiver source.

The owner authorized restoration on September 2, 2026. Site commit `618e41d` adds an allowlisted receiver-composition
step plus build/CI validation. The exact 65-file artifact was staged as a sibling directory, matched by relative-path
SHA-256 manifest, and swapped into the live vhost in a measured 4 ms upper bound. The prior live directory remains the
rollback point. Public HTTPS verification passed for the receiver, all six public Cast assets, CSP and MIME headers,
and representative unchanged Player, privacy, developer, tester-workspace, and tester-portal routes. The Android
candidate now enables Cast; physical sender/receiver acceptance remains a release gate.

## Gates before a production rollout can be authorized

| Gate | Acceptance evidence | State |
| --- | --- | --- |
| Candidate scope | Owner-authorized Cast restoration and no other unresolved release-critical feature | Cast retained; physical certification pending |
| Candidate identity | Clean committed `1.0.0` / code 18 source with release notes and exact revision provenance | In progress |
| Local validation | Compile, 202-unit-test baseline, lint, Android-test compilation, R8 bundle, release manifest, dependency, permission, and 16 KB checks | In progress |
| Physical mobile acceptance | Exact committed debug candidate on the connected Razr; clean install and update; five-station playback; navigation, onboarding, feedback, account, notification, and background-media smoke tests | Open |
| Android Auto acceptance | Exact candidate browses and plays the five approved stations through the existing Media3 service | Open |
| Signing | Owner enters the existing external JKS password in the approved one-prompt Linux flow; signer and artifact hashes are verified without retaining a secret | Owner interaction required |
| Play candidate | Code 18 accepted on a test track; Play-generated splits, update from Alpha08, clean install, pre-launch report, app-content declarations, and reviewer access reconciled | External action not authorized yet |
| Review control | Claude, Gemini, and DeepSeek findings dispositioned with no accepted unresolved BLOCKER/HIGH risk | In progress |
| First-launch geography | Owner names the initial production countries/regions | Owner decision required |
| Publication | Owner explicitly authorizes the exact artifact, initial geography, and launch action | Not authorized |

## Controlled first-launch plan

Google Play does not offer a rollout percentage for an app's first production release. Starting that release makes it
available to all eligible users in the selected countries/regions. The first-launch control is therefore geography,
not a nominal percentage.

1. Promote only the exact Play-tested code-18 bundle into a production draft with managed publishing retained.
2. Select only the owner-approved initial countries/regions and recheck device availability, store listing, pricing,
   declarations, reviewer access, and the release summary.
3. Start production only under separate explicit authorization.
4. Hold geographic expansion while reviewing Play pre-launch output, Android vitals, support reports, stream health,
   account/UGC reports, and the Play-delivered clean-install/update smoke tests.
5. Expand deliberately after 24 hours, 72 hours, and 7 days only when there is no unresolved release-blocking issue.

Android vitals are a release signal, not a substitute for testing. Pause expansion on any reproducible release crash or
ANR, broken playback across an approved station, authentication/session isolation defect, policy/declaration mismatch,
or severe accessibility regression. Google Play's current bad-behavior thresholds (1.09% user-perceived crash rate and
0.47% user-perceived ANR rate overall) are outer limits, not acceptable targets. Insufficient vitals volume must remain
`INSUFFICIENT_DATA`, not be treated as a pass.

## Recovery boundary

Before launch, keep the tested Alpha08 source/artifact record and a prepared higher-version hotfix path. During the
first release, stop geographic expansion immediately on a release-blocking finding. A first production launch cannot
be percentage-halted like a staged update; recovery must use the applicable Play control and, when code correction is
required, a separately tested and authorized higher-version replacement. Never reuse version code 18 for a replacement.

## Current owner-controlled decision

Name the initial production countries/regions. A first production release cannot be percentage-staged.
