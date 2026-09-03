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

The first committed code-18 candidate at `8d76d13` subsequently passed the same debug compile, 202-unit-test, lint,
Android-test compilation, and release bundle/R8 path. Its complete connected suite passed 73/73 on the Android 16
Razr. Release inspection confirmed package/version/SDK identity, the embedded source revision, the expected permission
set, four-ABI `libandroidx.graphics.path.so` packaging, 16 KB ZIP alignment, and `0x4000` ELF `LOAD` alignment. Those
results preceded the disclosure corrections below and therefore remain component evidence rather than final-tip
artifact evidence; the final committed source must repeat the affected and packaging checks.

Exact-candidate dependency inspection also found that Google Cast Android Sender SDK 22.3.1 automatically sends
Google encrypted, anonymous interaction diagnostics. Google's current disclosure describes generic discovery/session
events, mobile-device information, and client Cast-app information used in aggregate to improve Cast. The previous
privacy and Data Safety wording incorrectly said that no analytics SDK was present. The native notice, public policy,
Data Safety worksheet, and Play declaration packet now distinguish this Cast collection from the absence of ads,
tracking, crash reporting, developer-operated analytics, and a developer-operated backend. The declaration audit also
now includes the existing optional `specialUse` foreground Chat-mention monitor instead of describing only media
playback.

## Release-critical Cast correction

The Alpha08 lineage exposed a Cast sender configured for a custom receiver, but the active public
`https://player.jamesjennison.net/cast/` path returned HTTP 404. Read-only server inspection found the matching receiver
artifact preserved in the rollback created by the August 22 Player-site replacement. That replacement artifact had
omitted the `cast/` directory because the site build did not compose the receiver source.

The owner authorized restoration on September 2, 2026. Site commit `618e41d` adds an allowlisted receiver-composition
step plus build/CI validation. The exact 65-file artifact was staged as a sibling directory, matched by relative-path
SHA-256 manifest, and swapped into the live vhost in a measured 4 ms upper bound. The prior live directory remains the
rollback point. Public HTTPS verification passed for the receiver, all six public Cast assets, CSP and MIME headers,
and representative unchanged Player, privacy, developer, tester-workspace, and tester-portal routes.

The code-18 sender then connected to `TellyCast`, reported `remote media load result code=0`, displayed the active Cast
route, and the owner confirmed that the audio was casting. This closes the functional Cast-audio restoration gate.
The Google TV Streamer was only the passive Cast target; no Google TV app or project was changed. Receiver-screen
branding/artwork was not established by that observation and remains outside M44 in deferred expanded milestone M55.

## Bundled station-selector artwork

Physical Razr acceptance found that Death.FM's selector card could remain on the generic app-logo fallback even though
the configured public logo URL was healthy. The owner confirmed that station-selector identity artwork must not depend
on network loading. All five verified 200×200 station logos are now packaged in `drawable-nodpi`, and the compact,
cover-display, and landscape selectors resolve those bundled resources by canonical station ID. Dynamic now-playing
album artwork and metadata remain network-provided and are unchanged.

## Gates before a production rollout can be authorized

| Gate | Acceptance evidence | State |
| --- | --- | --- |
| Candidate scope | Owner-authorized repair of the existing Cast path; no separate Google TV work or broader M55 scope | Functional Cast audio passed; expanded M55 remains deferred |
| Candidate identity | Clean committed `1.0.0` / code 18 source with release notes and exact revision provenance | In progress |
| Local validation | Compile, unit tests, lint, Android-test compilation, R8 bundle, release manifest, dependency, permission, and 16 KB checks | Passed through `8173947`; final-tip repeat pending after bundled-selector correction |
| Physical mobile acceptance | Exact committed debug candidate on the connected Razr; clean install and update; five-station playback; navigation, onboarding, feedback, account, notification, and background-media smoke tests | 73/73 connected tests, five-station playback, background service, onboarding, and functional Cast audio passed before the bundled-selector correction; final-tip visual/runtime repeat remains open |
| Android Auto acceptance | Exact candidate browses and plays the five approved stations through the existing Media3 service | Open |
| Signing | Owner enters the existing external JKS password in the approved one-prompt Linux flow; signer and artifact hashes are verified without retaining a secret | Owner interaction required |
| Play candidate | Code 18 accepted on a test track; Play-generated splits, update from Alpha08, clean install, pre-launch report, app-content declarations, and reviewer access reconciled | External action not authorized yet |
| Review control | Claude, Gemini, and DeepSeek findings dispositioned with no accepted unresolved BLOCKER/HIGH risk | Cast-site review complete; convergence and production-candidate review in progress |
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
