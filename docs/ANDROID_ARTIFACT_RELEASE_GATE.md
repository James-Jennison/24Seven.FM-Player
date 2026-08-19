# Android artifact release gate

Use this gate before installing a debug APK on a physical device or describing an APK as a test-release build.

## 1. Select the baseline explicitly

Treat the accepted test-release commit as the source baseline. Do not substitute `origin/main` merely because it is easier to branch from. If the two histories diverge, record the exact baseline commit and reconcile the histories separately before a GitHub release.

Create an isolated worktree from that baseline, then keep it clean through validation:

```bash
git worktree add -b codex/<feature>-test /tmp/<feature>-test <accepted-test-release-commit>
```

## 2. Build a traceable debug APK

Embed the exact source revision at build time. A build without it is intentionally rejected by the verifier.

```bash
TWENTYFOURSEVEN_SOURCE_REVISION="$(git rev-parse HEAD)" \
TWENTYFOURSEVEN_ANDROID_BUILD_DIR=/tmp/24seven-android-build \
bash ./gradlew :app:assembleDebug
```

## 3. Verify before installation

The verifier requires a clean source tree, a named baseline that is an ancestor of `HEAD`, the expected debug package, matching version code, and the embedded source revision.

```bash
scripts/verify-android-artifact.sh \
  --baseline <accepted-test-release-commit> \
  --apk /tmp/24seven-android-build/outputs/apk/debug/app-debug.apk
```

Only after that passes may the APK be installed. Run the same command with `--device <adb-serial>` after installation to confirm the device’s installed version code matches the verified artifact.

## 4. Accept on the device

Open the debug package and confirm the visible UI matches the accepted test-release baseline before testing a feature. The in-app diagnostics report includes the source revision for a final provenance check.

## 5. Reconcile GitHub separately

When the test-release branch and `origin/main` have both advanced, do not force-push and do not treat an older remote snapshot as a substitute baseline. Plan and validate the history reconciliation independently; only then publish the tested feature branch or merge it into GitHub `main`.
