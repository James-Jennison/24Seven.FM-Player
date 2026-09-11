#!/usr/bin/env bash
# Promote an already-reviewed tester-portal artifact through the canonical
# Webuzo mapping. It is intentionally inert unless invoked with --promote.
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
contract_file="${repository_root}/deployment/player-portal.env"

usage() {
  cat <<'USAGE'
Usage: scripts/promote-tester-portal-release.sh --dry-run|--promote

--dry-run  Verify the exact commit, release branch, local artifact gate, and
            live Webuzo mapping with a read-only SSH check. It exits before
            sibling staging, upload, manifest comparison, or promotion.
--promote  Stage the reviewed artifact beside the live document root, compare
            relative file hashes, atomically promote it, retain one rollback
            release, then require the protected runtime guard to pass.

Both modes require PLAYER_PORTAL_EXPECTED_COMMIT to equal HEAD and the
configured tracked production reference. --promote is the only mutating mode.
USAGE
}

mode=""
case "${1:-}" in
  --dry-run|--promote) mode="$1" ;;
  --help|-h) usage; exit 0 ;;
  *) usage >&2; exit 64 ;;
esac

test -f "$contract_file"
# shellcheck disable=SC1090
source "$contract_file"

for required in SITE_ID PRODUCTION_BRANCH PRODUCTION_REMOTE_REF SITE_TYPE PRODUCTION_DOMAIN BUILD_COMMAND ARTIFACT_DIR POST_DEPLOY_CHECK; do
  test -n "${!required:-}" || { printf 'deployment-contract-missing=%s\n' "$required" >&2; exit 65; }
done
test "$SITE_TYPE" = 'static'

expected_commit="${PLAYER_PORTAL_EXPECTED_COMMIT:-}"
test -n "$expected_commit" || { printf 'expected-commit-required\n' >&2; exit 65; }

cd "$repository_root"
test -z "$(git status --porcelain)" || { printf 'working-tree-not-clean\n' >&2; exit 66; }
test "$(git branch --show-current)" = "$PRODUCTION_BRANCH" || { printf 'production-branch-required\n' >&2; exit 66; }

head_commit="$(git rev-parse HEAD)"
tracked_commit="$(git rev-parse "$PRODUCTION_REMOTE_REF")"
test "$head_commit" = "$expected_commit" || { printf 'expected-commit-mismatch\n' >&2; exit 66; }
test "$head_commit" = "$tracked_commit" || { printf 'tracked-production-reference-mismatch\n' >&2; exit 66; }

"$BUILD_COMMAND"
test -d "$ARTIFACT_DIR"

ssh_options=(-o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes)
ssh_alias="website-vm-admin"

discover_mapping() {
  ssh "${ssh_options[@]}" "$ssh_alias" 'bash -s' <<'REMOTE'
set -euo pipefail
httpd_pid=$(pgrep -o -f "[h]ttpd")
httpd_executable=$(readlink -f "/proc/${httpd_pid}/exe")
mapfile -t vhost_configs < <("$httpd_executable" -t -D DUMP_VHOSTS 2>&1 | sed -n "s/.*port 443 namevhost 24sevenfmplayer\.com (\([^:]*\):[0-9][0-9]*).*/\1/p")
test "${#vhost_configs[@]}" -eq 1
vhost_config="${vhost_configs[0]}"
root_for_port() {
  local port="$1"
  mapfile -t roots < <(awk -v port="$port" "
    BEGIN { inside=0; matched=0 }
    \$0 ~ \"<VirtualHost[^>]*:\" port \">\" { inside=1; matched=0 }
    inside && /ServerName[[:space:]]+24sevenfmplayer\\.com/ { matched=1; next }
    inside && matched && /DocumentRoot[[:space:]]+/ { gsub(/\"/, \"\", \$2); print \$2; matched=0 }
    /<\\/VirtualHost>/ { inside=0; matched=0 }
  " "$vhost_config")
  test "${#roots[@]}" -eq 1
  printf "%s\n" "${roots[0]}"
}
http_root=$(root_for_port 80)
https_root=$(root_for_port 443)
test -n "$http_root"
test "$http_root" = "$https_root"
test -d "$https_root"
printf "%s\n" "$https_root"
REMOTE
}

live_root="$(discover_mapping)"
test -n "$live_root"

if test "$mode" = '--dry-run'; then
  printf 'promotion-dry-run=pass\n'
  exit 0
fi

release_id="${head_commit:0:12}-$(date -u +%Y%m%dT%H%M%SZ)"
parent_dir="$(dirname "$live_root")"
stage_dir="${parent_dir}/.player-portal-stage-${release_id}"
rollback_dir="${parent_dir}/.player-portal-rollback-${release_id}"
lock_dir="${parent_dir}/.player-portal-promotion.lock"
local_workspace="$(mktemp -d "${TMPDIR:-/tmp}/player-portal-promote.XXXXXX")"
local_manifest="${local_workspace}/manifest.sha256"
remote_manifest="${local_workspace}/remote-manifest.sha256"
previous_manifest="${local_workspace}/previous-manifest.sha256"
restored_manifest="${local_workspace}/restored-manifest.sha256"
lock_held=false
cleanup() {
  if test "$lock_held" = true; then
    ssh "${ssh_options[@]}" "$ssh_alias" "rmdir '$lock_dir'" >/dev/null 2>&1 || true
  fi
  rm -rf "$local_workspace"
}
trap cleanup EXIT

(
  cd "$ARTIFACT_DIR"
  find . -type f -printf '%P\0' | sort -z | xargs -0 sha256sum
) > "$local_manifest"
test -s "$local_manifest"

ssh "${ssh_options[@]}" "$ssh_alias" "set -euo pipefail; mkdir '$lock_dir'; test ! -e '$stage_dir'; test ! -L '$stage_dir'; test ! -e '$rollback_dir'; test ! -L '$rollback_dir'; owner=\$(stat -c %U '$live_root'); group=\$(stat -c %G '$live_root'); install -d -m 0750 -o \"\$owner\" -g \"\$group\" '$stage_dir'"
lock_held=true
ssh "${ssh_options[@]}" "$ssh_alias" "set -euo pipefail; cd '$live_root'; find . -type f -printf '%P\\0' | sort -z | xargs -0 sha256sum" > "$previous_manifest"
test -s "$previous_manifest"
tar -C "$ARTIFACT_DIR" -cpf - . | ssh "${ssh_options[@]}" "$ssh_alias" "tar -C '$stage_dir' -xpf -"
ssh "${ssh_options[@]}" "$ssh_alias" "set -euo pipefail; find '$stage_dir' -type l -print -quit | grep -q . && exit 70 || true; find '$stage_dir' -type d -exec chmod 0755 {} +; find '$stage_dir' -type f -exec chmod 0644 {} +; owner=\$(stat -c %U '$live_root'); group=\$(stat -c %G '$live_root'); chown -R \"\$owner\":\"\$group\" '$stage_dir'; cd '$stage_dir'; find . -type f -printf '%P\\0' | sort -z | xargs -0 sha256sum" > "$remote_manifest"
test -s "$remote_manifest"
cmp --silent "$local_manifest" "$remote_manifest" || { printf 'staging-manifest-mismatch\n' >&2; exit 70; }

if ! ssh "${ssh_options[@]}" "$ssh_alias" "set -euo pipefail; mv '$live_root' '$rollback_dir'; if ! mv '$stage_dir' '$live_root'; then mv '$rollback_dir' '$live_root'; exit 71; fi"; then
  printf 'atomic-promotion-failed\n' >&2
  exit 71
fi

if ! "$POST_DEPLOY_CHECK"; then
  ssh "${ssh_options[@]}" "$ssh_alias" "set -euo pipefail; test -d '$rollback_dir'; test -d '$live_root'; failed='$parent_dir/.player-portal-failed-${release_id}'; mv '$live_root' \"\$failed\"; mv '$rollback_dir' '$live_root'"
  ssh "${ssh_options[@]}" "$ssh_alias" "set -euo pipefail; cd '$live_root'; find . -type f -printf '%P\\0' | sort -z | xargs -0 sha256sum" > "$restored_manifest"
  test -s "$restored_manifest"
  cmp --silent "$previous_manifest" "$restored_manifest" || { printf 'rollback-manifest-mismatch\n' >&2; exit 73; }
  if ! "$POST_DEPLOY_CHECK"; then
    printf 'rollback-runtime-guard-failed\n' >&2
    exit 74
  fi
  printf 'post-deploy-guard-failed-rollback-restored\n' >&2
  exit 72
fi

printf 'promotion=pass\nrollback-release=retained\n'
