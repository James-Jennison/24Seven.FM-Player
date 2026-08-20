# Android artifact release gate

Use this gate before installing a debug APK on a physical device or describing an APK as a test-release build.

## 1. Select the baseline explicitly

Treat the accepted test-release commit as the source baseline. Do not substitute `origin/main` merely because it is easier to branch from. If the two histories diverge, record the exact baseline commit and reconcile the histories separately before a GitHub release.

Create an isolated worktree from that baseline, then keep it clean through validation:

```bash
git worktree add -b codex/<feature>-test /tmp/<feature>-test <accepted-test-release-commit>
```

## 2. Build a traceable debug APK

Build only from a clean, committed worktree. The candidate builder embeds the exact `HEAD` revision and rejects every other baseline; a build without that provenance is intentionally rejected.

```bash
TWENTYFOURSEVEN_ANDROID_BUILD_DIR=/tmp/24seven-android-build \
scripts/build-debug-candidate.sh
```

## 3. Verify before installation

The verifier requires a clean source tree, a named baseline exactly equal to `HEAD`, the expected debug package, matching version code, and the embedded source revision.

```bash
scripts/verify-android-artifact.sh \
  --baseline <accepted-test-release-commit> \
  --apk /tmp/24seven-android-build/outputs/apk/debug/app-debug.apk
```

Only after that passes may the APK be installed. Run the same command with `--device <adb-serial>` after installation to confirm the device’s installed version code matches the verified artifact.

## 4. Accept on the device

Open the debug package and confirm the visible UI matches the accepted test-release baseline before testing a feature. The in-app diagnostics report includes the source revision for a final provenance check.

## 5. Reconcile GitHub separately

When the test-release branch and `origin/main` have both advanced, do not treat an older remote snapshot as a substitute baseline. Reconcile and validate history independently; only then publish the tested feature branch or merge it into GitHub `main`. If a proven-bad `main` history must be replaced, first preserve it under an explicit archive ref and use a lease-protected force update only with explicit authorization.
