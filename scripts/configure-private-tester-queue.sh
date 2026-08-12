#!/usr/bin/env bash
set -euo pipefail

# Run only on the Player Webuzo host as the site owner. It prompts on the
# terminal, never accepts a password on the command line, and persists only a
# password hash outside the public document root.

configuration_path="/home/jamesjen/.private-tester-queue-config.php"
database_path="/home/jamesjen/.private-tester-queue/testers.sqlite"
sender_email="24sevenfmplayertest@jamesjennison.net"
sender_name="24Seven.FM Player"

if [[ ! -f "${database_path}" ]]; then
  printf '%s\n' 'The private tester database is unavailable.' >&2
  exit 1
fi

read -r -s -p 'Set queue administrator password: ' password
printf '\n'
read -r -s -p 'Confirm queue administrator password: ' confirmation
printf '\n'

if [[ -z "${password}" || "${password}" != "${confirmation}" ]]; then
  unset password confirmation
  printf '%s\n' 'Passwords did not match or were empty; no change was made.' >&2
  exit 1
fi

password_hash="$(printf '%s' "${password}" | php -r 'echo password_hash(stream_get_contents(STDIN), PASSWORD_DEFAULT);')"
unset password confirmation
if [[ -z "${password_hash}" ]]; then
  printf '%s\n' 'A password hash could not be created.' >&2
  exit 1
fi

umask 077
temporary_configuration="$(mktemp "$(dirname "${configuration_path}")/.private-tester-queue-config.XXXXXX")"

{
  printf '%s\n' '<?php' 'declare(strict_types=1);' '' 'return ['
  printf "    'admin_password_hash' => %s,\n" "$(php -r 'var_export($argv[1]);' "${password_hash}")"
  printf "    'database_path' => %s,\n" "$(php -r 'var_export($argv[1]);' "${database_path}")"
  printf "    'from_email' => %s,\n" "$(php -r 'var_export($argv[1]);' "${sender_email}")"
  printf "    'from_name' => %s,\n" "$(php -r 'var_export($argv[1]);' "${sender_name}")"
  printf '%s\n' '];'
} > "${temporary_configuration}"

mv -f -- "${temporary_configuration}" "${configuration_path}"
temporary_configuration=""
chmod 0600 "${configuration_path}"
printf '%s\n' 'Private tester queue administrator access is configured.'
