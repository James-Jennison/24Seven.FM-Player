# Release A — onboarding and accessibility validation

Status: **PASS WITH CONNECTED ACCESSIBILITY FOLLOW-UP**

Release A addresses the closed-test onboarding recommendation without reopening the completed M34 device/accessibility milestone or broadening the Player's privacy, network, account, or station-authority boundaries.

## External feedback addressed

The Testers Community closed-test report recommended a dynamic new-user walkthrough covering station switching, queues, Chat, tooltips where useful, and an explicit skip path. It also recommended continued accessibility review. Release A implements the walkthrough and extends accessibility coverage around the new surface while preserving the existing M34 baseline.

No Release A work treats the external report as evidence of a previously unidentified crash or functional defect; the report stated that no critical crashes or functional discrepancies were found in its tested configurations.

## Existing accessibility baseline

`docs/m23-accessibility-validation.md` remains the authoritative historical M34 evidence. That completed work already covers:

- TalkBack traversal and actionable-node labeling;
- font scale 2.0 and enlarged-display reflow;
- compact phone, Fold, and Tablet layouts;
- physical Voice Access validation; and
- Bluetooth keyboard/pointer acceptance.

Release A does not rewrite or reopen those results. Its responsibility is to avoid regressing them and to make the new guide compatible with the same accessibility expectations.

## Implementation

Release A adds a fully native Compose app guide with six concise steps:

1. Welcome to 24Seven.FM Player.
2. Choose and switch stations.
3. Player and Now Playing.
4. Queue and requests.
5. Community and personal features.
6. Ready/start listening.

The guide is station-capability-aware. Queue/history, song requests, Chat, Favorites, station accounts, and request activity are described from the selected station's existing capability flags rather than assumed network-wide.

The guide performs no network request, sign-in, song request, Chat post, or station mutation merely by being displayed. It does not add analytics or telemetry.

## First-run and existing-install behavior

`AppGuideRepository` stores a local, versioned completion value. The initial release uses `CURRENT_APP_GUIDE_VERSION = 1`.

- A genuine fresh installation is eligible to see the guide automatically.
- Skip and normal completion both mark the current guide version complete so the app does not nag on every launch.
- Existing installations that predate the guide are not forced through a newly introduced blocking tutorial. They are initialized as having completed the current version.
- The guide can be reopened manually from the More surface using the `App guide` control.
- Manual reopening does not reset or otherwise corrupt the persisted completion state.
- A future guide can be intentionally re-presented by deliberately advancing the version contract rather than by clearing unrelated preferences.

The preference contains only local guide-version state and introduces no user identity or remote synchronization.

## Accessibility design of the new surface

The onboarding dialog uses a full-height scrollable Compose surface so instructional text and actions remain reachable when vertical space is constrained or text is enlarged. The current step title exposes heading semantics. All primary controls use visible text labels (`Skip`, `Back`, `Next`, and `Start listening`) rather than icon-only actions or color-only state.

The progress indicator is textual (`Step N of 6`), so the current position does not depend on color. The guide uses the existing Material theme rather than introducing a separate color system.

The manual `App guide` control is a standard Material button with visible text and Android-managed touch semantics.

## Automated coverage added

### Local-state tests

`AppGuideRepositoryTest` covers:

- automatic presentation on a fresh preference state;
- safe migration for an existing installation with no prior guide preference; and
- current-version completion in the in-memory test repository.

### Compose instrumentation tests

`AppGuideTest` covers:

- first-step skip behavior;
- forward navigation;
- backward navigation; and
- final completion.

These tests are committed under `app/src/androidTest`, but the repository's current pull-request workflow does not start an emulator or execute `connectedDebugAndroidTest`. Their presence must therefore not be represented as an executed connected-device result.

## CI validation

The first Release A CI attempt correctly failed on two Release A compile defects: an incorrect Material 3 floating-action-button overload and an incorrect `rememberSaveable` import. Both were corrected on the Release A branch.

The corrected code head `daf42311f926d3e23396f96de54ae10f01678ab1` passed GitHub Actions run `32841714273` (`Android build, test, and lint`). The workflow executed:

```text
bash ./gradlew test lint --no-daemon --console=plain
bash scripts/build-debug-candidate.sh
```

Observed successful gates from that run:

- debug Kotlin compilation: passed;
- release Kotlin compilation: passed;
- debug unit tests: passed;
- release unit tests: passed;
- app lint: passed;
- security-harness lint: passed;
- `test lint`: `BUILD SUCCESSFUL`;
- `:app:assembleDebug`: `BUILD SUCCESSFUL`;
- artifact verification: `passed`;
- debug version code: `17`;
- debug candidate: `passed`.

The workflow reported 109 actionable tasks for the test/lint build and 38 actionable tasks for the debug assembly build.

## Connected accessibility follow-up

Release A is not recorded as an unconditional accessibility PASS yet because the PR workflow does not execute the newly added connected Compose tests or a real TalkBack traversal of the new guide.

Before treating the Release A accessibility extension as fully closed, perform a focused connected-device/emulator pass covering only the new surface:

- open/skip/complete/reopen the guide;
- font scale 2.0 with all instructional text and actions reachable;
- TalkBack traversal through title, progress, and navigation controls with no unlabeled actionable node;
- compact portrait and constrained-height/landscape layout;
- one medium/expanded layout; and
- keyboard/D-pad navigation where the existing test environment supports it.

This is a focused Release A follow-up, not a rerun of all historical M34 physical-device evidence.

## Scope and safety confirmation

Release A made no intended change to:

- playback or Media3 ownership;
- station endpoints or stream configuration;
- authentication/session boundaries;
- request transaction validation;
- Chat or community safety policy;
- privacy/data collection;
- analytics or telemetry;
- backend or server services;
- the tester onboarding portal;
- Google Play publication; or
- production deployment.

## Roadmap and time-ledger treatment

Release A is tracked as a named post-closed-test feedback initiative rather than being assigned a new canonical `Mxx` identifier. `docs/ROADMAP.md` and `docs/MILESTONE_TIME_LEDGER.md` currently define the canonical M01–M60 program; inventing or renumbering a canonical milestone here would rewrite that governance boundary. This validation record therefore preserves Release A separately while leaving the M01–M60 historical ledger unchanged.

If the project later adopts a formal post-M60 canonical sequence, Release A can be mapped into that sequence without rewriting this evidence.
