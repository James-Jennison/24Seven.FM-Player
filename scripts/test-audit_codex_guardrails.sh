#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
audit_output="$($repo_root/scripts/audit_codex_guardrails.sh)"
printf '%s\n' "$audit_output"
printf '%s\n' "$audit_output" | rg -q '^SUMMARY: PASS=[0-9]+ WARN=[0-9]+ FAIL=0$'
