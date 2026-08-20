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
revision=$(git rev-parse HEAD)
build_root=${TWENTYFOURSEVEN_ANDROID_BUILD_DIR:-"$project_root/app/build"}
apk="$build_root/outputs/apk/debug/app-debug.apk"

TWENTYFOURSEVEN_SOURCE_REVISION="$revision" \
  /bin/bash "$project_root/gradlew" :app:assembleDebug

[[ -f "$apk" ]] || fail 'Gradle completed without producing the expected debug APK'
"$project_root/scripts/verify-android-artifact.sh" \
  --baseline "$revision" \
  --apk "$apk"

printf 'debug-candidate=passed\n'
