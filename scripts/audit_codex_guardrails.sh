#!/usr/bin/env bash
# Safe local audit: no Gmail, browser, production, network, credential, or mutation access.
set -uo pipefail

repo_root="$(git rev-parse --show-toplevel)"
codex_home="${CODEX_HOME:-$HOME/.codex}"
global_agents="$codex_home/AGENTS.md"
global_override="$codex_home/AGENTS.override.md"
global_rules="$codex_home/rules/default.rules"
project_agents="$repo_root/AGENTS.md"
project_config="$repo_root/.codex/config.toml"
project_rules="$repo_root/.codex/rules/player-hard-rules.rules"
pass_count=0
warn_count=0
fail_count=0

pass() { printf 'PASS: %s\n' "$1"; pass_count=$((pass_count + 1)); }
warn() { printf 'WARN: %s\n' "$1"; warn_count=$((warn_count + 1)); }
fail() { printf 'FAIL: %s\n' "$1"; fail_count=$((fail_count + 1)); }
expect_file() { if [ -f "$1" ]; then pass "$2"; else fail "$2 missing: $1"; fi; }
expect_text() { if rg -q -- "$2" "$1"; then pass "$3"; else fail "$3"; fi; }

printf 'Codex guardrail audit\n'
printf 'Repository root: %s\n' "$repo_root"
printf 'CODEX_HOME: %s\n' "$codex_home"
printf 'Codex version: '
codex --version

expect_file "$global_agents" "global guidance file exists"
if [ -e "$global_override" ]; then warn "global AGENTS.override.md exists; inspect its precedence before relying on this audit"; else pass "no global AGENTS.override.md"; fi
expect_file "$global_rules" "system-wide rule file exists"
expect_file "$project_agents" "Player guidance file exists"
expect_file "$project_config" "trusted Player config exists"
expect_file "$project_rules" "Player rule file exists"

if rg -q '^project_doc_max_bytes' "$codex_home/config.toml"; then
  warn "custom project_doc_max_bytes is configured; compare it with active guidance size"
else
  guidance_bytes="$(( $(wc -c < "$global_agents") + $(wc -c < "$project_agents") ))"
  if [ "$guidance_bytes" -lt 32768 ]; then pass "active global and Player guidance fit the documented 32 KiB default"; else fail "active guidance may exceed the documented 32 KiB default"; fi
fi

project_override_count="$(find "$repo_root" -path "$repo_root/.git" -prune -o -name AGENTS.override.md -print | wc -l)"
if [ "$project_override_count" -eq 0 ]; then pass "no project AGENTS.override.md"; else warn "project AGENTS.override.md detected; inspect it for a policy conflict"; fi
nested_agent_count="$(find "$repo_root" -path "$repo_root/.git" -prune -o -name AGENTS.md -print | wc -l)"
if [ "$nested_agent_count" -eq 1 ]; then pass "only root Player AGENTS.md is active"; else warn "nested Player AGENTS.md files detected; confirm they do not weaken root policy"; fi

expect_text "$global_agents" 'Completion pressure NEVER constitutes authorization\.' "global hard-rule contract is present"
expect_text "$project_agents" 'HARD_RULE_BLOCKER: Gmail access is prohibited for this project\.' "Player Gmail blocker is present"
expect_text "$project_config" '^enabled = false$' "Player config contains disabled restricted integration"
expect_file "$codex_home/CAPABILITY_BASELINE.md" "system-wide capability baseline exists"
if ! rg -q '^in_app_browser = false$|^\[mcp_servers\.node_repl\]$' "$project_config"; then
  pass "Player config does not retain the retired native-browser workaround"
else
  fail "Player config still disables the native-browser route"
fi
managed_requirements="/etc/codex/requirements.toml"

if \
  rg -q '^\[apps\.gmail\]$' "$managed_requirements" &&
  awk '
    /^\[apps\.gmail\]$/ {
      in_gmail=1
      found_section=1
      next
    }

    /^\[/ && in_gmail {
      in_gmail=0
    }

    in_gmail && /^[[:space:]]*enabled[[:space:]]*=[[:space:]]*false[[:space:]]*$/ {
      disabled=1
    }

    END {
      exit !(found_section && disabled)
    }
  ' "$managed_requirements"
then
  pass "managed system-wide Gmail app restriction is active"
else
  fail "managed system-wide Gmail app restriction is missing or not disabled"
fi

if rg -q '^approval_policy = "on-request"$' "$codex_home/config.toml"; then pass "user approval policy is on-request"; else fail "user approval policy is not on-request"; fi

if python3 - "$managed_requirements" "$codex_home/config.toml" "$project_config" <<'PY'
import sys
import tomllib
from pathlib import Path

managed, user_config, project_config = (Path(path) for path in sys.argv[1:])
with managed.open('rb') as handle:
    requirements = tomllib.load(handle)
profiles = requirements.get('allowed_permission_profiles', {})
allowed = lambda name: isinstance(profiles, dict) and profiles.get(name) is True
if not (allowed(':workspace') and allowed(':danger-full-access') and 'on-request' in requirements.get('allowed_approval_policies', [])):
    raise SystemExit(1)
if requirements.get('default_permissions') == 'owner_local_development' or allowed('owner_local_development') or 'owner_local_development' in requirements.get('permissions', {}):
    raise SystemExit(1)
for path in (user_config, project_config):
    with path.open('rb') as handle:
        config = tomllib.load(handle)
    if 'owner_local_development' in config.get('permissions', {}):
        raise SystemExit(1)
PY
then
  pass "healthy managed permission baseline permits workspace, deliberate full access, and on-request without owner_local_development"
else
  fail "managed permission baseline is stale or malformed"
fi

check_rule() {
  local expected="$1"
  shift
  local result
  if ! result="$(codex execpolicy check --rules "$global_rules" --rules "$project_rules" -- "$@")"; then
    fail "execpolicy check failed for: $*"
    return
  fi
  if [ "$expected" = "allow" ] && printf '%s' "$result" | rg -q '^\{"matchedRules":\[\]\}$'; then
    pass "rule allow (no restrictive match): $*"
  elif printf '%s' "$result" | rg -q "\"decision\":\"$expected\""; then
    pass "rule $expected: $*"
  else
    fail "rule expected $expected: $*"
  fi
}

check_global_rule() {
  local expected="$1"
  shift
  local result
  if ! result="$(codex execpolicy check --rules "$global_rules" -- "$@")"; then
    fail "global execpolicy check failed for: $*"
    return
  fi
  if printf '%s' "$result" | rg -q "\"decision\":\"$expected\""; then
    pass "global rule $expected: $*"
  else
    fail "global rule expected $expected: $*"
  fi
}

check_rule forbidden git reset --hard
check_rule forbidden git clean -fd
check_rule prompt git clean -fdx
check_rule prompt git push origin main
check_global_rule prompt ssh website-vm-admin
check_global_rule prompt ssh mail-vm-admin
check_global_rule prompt ssh webuzo-production-admin
check_rule prompt ssh website-vm-admin
check_rule forbidden ssh mail-vm-admin
check_rule forbidden ssh webuzo-production-admin
check_rule forbidden ./gradlew clean
check_rule forbidden git restore AGENTS.md
check_rule allow ./gradlew :app:compileDebugKotlin
check_rule allow git status
check_rule allow git diff

if feature_state="$(codex features list | awk '$1 == "in_app_browser" { print $3 }')" && [ "$feature_state" = "true" ]; then pass "native Player in_app_browser feature is available"; else fail "native Player in_app_browser feature is not available"; fi
if browser_use_state="$(codex features list | awk '$1 == "browser_use" { print $3 }')" && [ "$browser_use_state" = "true" ]; then pass "native Browser use feature remains enabled"; else fail "native Browser use feature is not enabled"; fi
if computer_use_state="$(codex features list | awk '$1 == "computer_use" { print $3 }')" && [ "$computer_use_state" = "true" ]; then pass "Computer Use feature remains enabled"; else fail "Computer Use feature is not enabled"; fi
mcp_inventory="$(codex mcp list)"
if printf '%s\n' "$mcp_inventory" | rg -q 'node_repl.*enabled'; then pass "configured node_repl browser bridge remains enabled"; else fail "configured node_repl browser bridge is not enabled"; fi
if printf '%s\n' "$mcp_inventory" | rg -q 'openaiDeveloperDocs.*enabled'; then pass "official OpenAI documentation MCP remains enabled"; else fail "official OpenAI documentation MCP is not enabled"; fi
plugin_inventory="$(codex plugin list)"
if printf '%s\n' "$plugin_inventory" | rg -q '^browser@openai-bundled[[:space:]]+installed, enabled'; then pass "native Browser plugin remains enabled"; else fail "native Browser plugin is not enabled"; fi

if [ -f "$managed_requirements" ]; then pass "managed requirements are present at /etc/codex/requirements.toml (active source; inspect separately under owner control)"; else warn "managed requirements are not active; docs/codex-security/requirements.toml.proposed is OWNER ACTION REQUIRED"; fi

printf 'SUMMARY: PASS=%s WARN=%s FAIL=%s\n' "$pass_count" "$warn_count" "$fail_count"
if [ "$fail_count" -ne 0 ]; then exit 1; fi
