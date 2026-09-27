#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'debug-candidate=failed: %s\n' "$*" >&2
  exit 1
}

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail 'run from a Git worktree'
git diff --quiet || fail 'source worktree has unstaged changes; commit or discard them before building'
git diff --cached --quiet || fail 'source worktree has staged changes; commit them before building'

project_root=$(git rev-parse --show-toplevel)

is_protected_build() {
  [[ ${HEAVY_BUILD_ACTIVE:-0} == 1 ]] \
    || grep -q '/build\.slice/' /proc/self/cgroup
}

# Preserve the GitHub Actions path while automatically protecting workstation
# candidate builds when the machine-global runner is installed.
if [[ ${CI:-} != true ]] && ! is_protected_build; then
  if heavy_build_runner=$(command -v heavy-build 2>/dev/null); then
    cd "$project_root"
    exec "$heavy_build_runner" -- bash "$project_root/scripts/build-debug-candidate.sh" "$@"
  fi
  printf '%s\n' 'heavy-build is required for local Android candidate builds on this workstation.' >&2
  exit 69
fi

revision=$(git rev-parse HEAD)
build_root=${TWENTYFOURSEVEN_ANDROID_BUILD_DIR:-"$project_root/app/build"}
apk="$build_root/outputs/apk/debug/app-debug.apk"

gradle_resource_args=()
if is_protected_build; then
  gradle_resource_args=(--no-daemon "--max-workers=${HEAVY_BUILD_JOBS:-6}")
fi

TWENTYFOURSEVEN_SOURCE_REVISION="$revision" \
  /bin/bash "$project_root/gradlew" "${gradle_resource_args[@]}" :app:assembleDebug

[[ -f "$apk" ]] || fail 'Gradle completed without producing the expected debug APK'
/bin/bash "$project_root/scripts/verify-android-artifact.sh" \
  --baseline "$revision" \
  --apk "$apk"

printf 'debug-candidate=passed\n'
