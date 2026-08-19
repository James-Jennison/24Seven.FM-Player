#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/verify-android-artifact.sh --baseline <git-ref> --apk <debug-apk> [--device <adb-serial>]

Checks the exact source baseline, a clean worktree, debug package identity, version code,
and the source revision embedded in the APK manifest. Supply --device only after installation
to verify that the same version code is installed on that device.
EOF
}

fail() {
  printf 'artifact-verification=failed: %s\n' "$*" >&2
  exit 1
}

baseline=''
apk=''
device=''
while (($#)); do
  case "$1" in
    --baseline) baseline=${2:-}; shift 2 ;;
    --apk) apk=${2:-}; shift 2 ;;
    --device) device=${2:-}; shift 2 ;;
    --help|-h) usage; exit 0 ;;
    *) fail "unknown argument: $1" ;;
  esac
done

[[ -n "$baseline" ]] || fail '--baseline is required'
[[ -n "$apk" && -f "$apk" ]] || fail '--apk must name an existing APK'
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail 'run from a Git worktree'
git diff --quiet || fail 'source worktree has unstaged changes'
git diff --cached --quiet || fail 'source worktree has staged changes'

baseline_commit=$(git rev-parse --verify "${baseline}^{commit}") || fail 'baseline does not resolve to a commit'
head_commit=$(git rev-parse HEAD)
git merge-base --is-ancestor "$baseline_commit" "$head_commit" || fail 'baseline is not an ancestor of HEAD'

source_version=$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\).*/\1/p' app/build.gradle.kts | head -n 1)
source_package=$(sed -n 's/^[[:space:]]*applicationId = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)
[[ -n "$source_version" && -n "$source_package" ]] || fail 'could not read application version/package from app/build.gradle.kts'

aapt_bin=$(command -v aapt || true)
if [[ -z "$aapt_bin" && -n "${ANDROID_HOME:-}" ]]; then
  aapt_bin=$(find "$ANDROID_HOME/build-tools" -type f -name aapt -print 2>/dev/null | sort -V | tail -n 1 || true)
fi
[[ -n "$aapt_bin" ]] || fail 'aapt is unavailable; set ANDROID_HOME or add aapt to PATH'

badging=$($aapt_bin dump badging "$apk")
apk_package=$(sed -n "s/package: name='\([^']*\)'.*/\1/p" <<<"$badging")
apk_version=$(sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" <<<"$badging" | head -n 1)
[[ "$apk_package" == "${source_package}.debug" ]] || fail 'APK is not the debug package for this source tree'
[[ "$apk_version" == "$source_version" ]] || fail 'APK version code does not match this source tree'

manifest_dump=$($aapt_bin dump xmltree "$apk" AndroidManifest.xml)
grep -Fq "$head_commit" <<<"$manifest_dump" || fail 'APK lacks this HEAD source revision; rebuild with TWENTYFOURSEVEN_SOURCE_REVISION set to HEAD'

if [[ -n "$device" ]]; then
  installed_version=$(adb -s "$device" shell dumpsys package "$apk_package" 2>/dev/null | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p' | head -n 1 | tr -d '\r')
  [[ "$installed_version" == "$apk_version" ]] || fail 'connected device does not have this APK version installed'
fi

printf 'artifact-verification=passed\n'
printf 'baseline=%s\n' "$baseline_commit"
printf 'head=%s\n' "$head_commit"
printf 'debug-version-code=%s\n' "$apk_version"
