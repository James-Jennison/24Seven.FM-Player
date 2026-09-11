#!/usr/bin/env bash
# Focused fail-closed contracts for the local promotion channel. These tests do
# not connect to Webuzo or contact the public site.
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
promotion_script="${script_dir}/promote-tester-portal-release.sh"

bash -n "$promotion_script"

set +e
dry_run_output="$(cd "$repository_root" && PLAYER_PORTAL_EXPECTED_COMMIT="$(git rev-parse HEAD)" "$promotion_script" --dry-run 2>&1)"
dry_run_status=$?
set -e

test "$dry_run_status" -eq 66
if test -n "$(git -C "$repository_root" status --porcelain)"; then
  test "$dry_run_output" = 'working-tree-not-clean'
else
  test "$dry_run_output" = 'production-branch-required'
fi

python3 - "$promotion_script" <<'PY'
from pathlib import Path
import sys

source = Path(sys.argv[1]).read_text(encoding="utf-8")

def require(fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(f"promotion contract missing: {fragment}")

require('test -z "$(git status --porcelain)"')
require('test "$(git branch --show-current)" = "$PRODUCTION_BRANCH"')
require('test "$head_commit" = "$expected_commit"')
require('test "$head_commit" = "$tracked_commit"')
require('test "${#vhost_configs[@]}" -eq 1')
require('test "${#roots[@]}" -eq 1')
require('test "$http_root" = "$https_root"')
require('test -s "$local_manifest"')
require('test -s "$remote_manifest"')
require('cmp --silent "$local_manifest" "$remote_manifest"')
require("mkdir '$lock_dir'")
require('cmp --silent "$previous_manifest" "$restored_manifest"')

dry_run_stop = source.index("if test \"$mode\" = '--dry-run'; then")
first_mutation = source.index('mkdir \'$lock_dir\'')
if dry_run_stop >= first_mutation:
    raise SystemExit("dry-run must stop before a remote mutation")

print("Tester portal promotion-channel contracts: valid.")
PY
