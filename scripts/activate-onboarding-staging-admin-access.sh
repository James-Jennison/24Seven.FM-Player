#!/usr/bin/env bash
set -euo pipefail

# Run this only in an owner-authenticated SSH terminal on the isolated
# onboarding staging host. It never reads, changes, or displays the live
# administrator credentials or MFA seed.

if [[ ! -t 0 || ! -t 1 ]]; then
  printf '%s\n' 'An interactive owner SSH terminal is required; no change was made.' >&2
  exit 1
fi

if [[ "${1:-}" == "--disable" ]]; then
  action='disable'
elif [[ $# -eq 0 ]]; then
  action='enable'
else
  printf '%s\n' 'Usage: activate-onboarding-staging-admin-access.sh [--disable]' >&2
  exit 1
fi

client_address="${SSH_CONNECTION%% *}"
if ! php -r 'exit(filter_var($argv[1], FILTER_VALIDATE_IP) === false ? 1 : 0);' "${client_address}"; then
  printf '%s\n' 'The owner SSH source address is unavailable; no change was made.' >&2
  exit 1
fi

script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
configuration_path="${script_directory}/.onboarding-staging-bypass.php"

if [[ "${action}" == 'disable' ]]; then
  read -r -p 'Type DISABLE to remove temporary staging administrator access: ' confirmation
  if [[ "${confirmation}" != 'DISABLE' ]]; then
    printf '%s\n' 'Confirmation did not match; no change was made.' >&2
    exit 1
  fi
  rm -f -- "${configuration_path}"
  printf '%s\n' 'Temporary staging administrator access has been removed.'
  exit 0
fi

printf '%s\n' 'This enables administrator access only for this authenticated SSH client address.'
printf '%s\n' 'It is restricted to the staging hostname and expires automatically in four hours.'
read -r -p 'Type ENABLE to activate temporary staging administrator access: ' confirmation
if [[ "${confirmation}" != 'ENABLE' ]]; then
  printf '%s\n' 'Confirmation did not match; no change was made.' >&2
  exit 1
fi

expires_at="$(( $(date +%s) + 14400 ))"
umask 077
temporary_configuration="$(mktemp "${script_directory}/.onboarding-staging-bypass.XXXXXX")"
{
  printf '%s\n' '<?php' 'declare(strict_types=1);' '' 'return ['
  printf "    'expires_at' => %s,\n" "${expires_at}"
  printf "    'allowed_addresses' => ['%s'],\n" "${client_address}"
  printf '%s\n' '];'
} > "${temporary_configuration}"
chmod 0600 "${temporary_configuration}"
mv -f -- "${temporary_configuration}" "${configuration_path}"

printf '%s\n' 'Temporary staging administrator access is active for this SSH client address.'
