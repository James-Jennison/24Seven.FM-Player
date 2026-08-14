#!/usr/bin/env bash
set -euo pipefail

# Run only on the Player Webuzo host as the site owner. It prompts on the
# terminal, never accepts a password or MFA seed on the command line, and
# persists only private authentication configuration outside the public root.

configuration_path="/home/jamesjen/.private-tester-queue-config.php"
database_path="/home/jamesjen/.private-tester-queue/testers.sqlite"
sender_email="24sevenfmplayertest@jamesjennison.net"
sender_name="24Seven.FM Player"

if [[ ! -f "${database_path}" ]]; then
  printf '%s\n' 'The private tester database is unavailable.' >&2
  exit 1
fi

read -r -p 'Set queue administrator login name: ' administrator_username
if [[ ! "${administrator_username}" =~ ^[A-Za-z0-9][A-Za-z0-9._@-]{2,63}$ ]]; then
  unset administrator_username
  printf '%s\n' 'Administrator login names must be 3–64 characters using letters, digits, period, underscore, at-sign, or hyphen; no change was made.' >&2
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

if ! command -v openssl >/dev/null 2>&1; then
  unset administrator_username password password_hash
  printf '%s\n' 'OpenSSL is required to generate the authenticator-app enrollment seed.' >&2
  exit 1
fi

totp_secret="$(openssl rand 20 | php -r '
  $bytes = stream_get_contents(STDIN);
  if ($bytes === false || strlen($bytes) !== 20) { exit(1); }
  $alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  $buffer = 0;
  $bits = 0;
  $encoded = "";
  foreach (str_split($bytes) as $character) {
    $buffer = ($buffer << 8) | ord($character);
    $bits += 8;
    while ($bits >= 5) {
      $bits -= 5;
      $encoded .= $alphabet[($buffer >> $bits) & 31];
    }
    $buffer = $bits === 0 ? 0 : $buffer & ((1 << $bits) - 1);
  }
  if ($bits > 0) { $encoded .= $alphabet[($buffer << (5 - $bits)) & 31]; }
  echo $encoded;
')"
if [[ ! "${totp_secret}" =~ ^[A-Z2-7]{32}$ ]]; then
  unset administrator_username password password_hash totp_secret
  printf '%s\n' 'An authenticator-app enrollment seed could not be generated; no change was made.' >&2
  exit 1
fi

if ! command -v qrencode >/dev/null 2>&1; then
  unset administrator_username password password_hash totp_secret
  printf '%s\n' 'The local QR renderer is unavailable; no change was made.' >&2
  exit 1
fi

totp_uri="otpauth://totp/24Seven.FM%20Player:${administrator_username}?secret=${totp_secret}&issuer=24Seven.FM%20Player&algorithm=SHA1&digits=6&period=30"
printf '\n%s\n' 'Scan this QR code with your authenticator app to add 24Seven.FM Player administrator:'
if ! printf '%s' "${totp_uri}" | qrencode -t UTF8 -m 2 -s 1; then
  unset administrator_username password password_hash totp_secret totp_uri
  printf '%s\n' 'The local QR code could not be rendered; no change was made.' >&2
  exit 1
fi
unset totp_uri
read -r -s -p 'Enter the current six-digit code to confirm enrollment: ' totp_code
printf '\n'
if ! printf '%s\n%s\n' "${totp_secret}" "${totp_code}" | php -r '
  $secret = trim((string) fgets(STDIN));
  $code = trim((string) fgets(STDIN));
  if (!preg_match("/^[A-Z2-7]{32}$/", $secret) || !preg_match("/^[0-9]{6}$/", $code)) { exit(1); }
  $alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  $buffer = 0; $bits = 0; $key = "";
  foreach (str_split($secret) as $character) {
    $buffer = ($buffer << 5) | strpos($alphabet, $character); $bits += 5;
    while ($bits >= 8) { $bits -= 8; $key .= chr(($buffer >> $bits) & 255); }
    $buffer = $bits === 0 ? 0 : $buffer & ((1 << $bits) - 1);
  }
  $step = intdiv(time(), 30);
  for ($offset = -1; $offset <= 1; $offset++) {
    $counter = pack("N2", intdiv($step + $offset, 4294967296), ($step + $offset) % 4294967296);
    $hash = hash_hmac("sha1", $counter, $key, true); $index = ord($hash[19]) & 15;
    $value = ((ord($hash[$index]) & 127) << 24) | (ord($hash[$index + 1]) << 16) | (ord($hash[$index + 2]) << 8) | ord($hash[$index + 3]);
    if (hash_equals(str_pad((string) ($value % 1000000), 6, "0", STR_PAD_LEFT), $code)) { exit(0); }
  }
  exit(1);
'; then
  unset administrator_username password password_hash totp_secret totp_code
  printf '%s\n' 'The authenticator code was not accepted; no change was made.' >&2
  exit 1
fi
unset totp_code

umask 077
temporary_configuration="$(mktemp "$(dirname "${configuration_path}")/.private-tester-queue-config.XXXXXX")"

{
  printf '%s\n' '<?php' 'declare(strict_types=1);' '' 'return ['
  printf "    'admin_username' => %s,\n" "$(printf '%s' "${administrator_username}" | php -r 'var_export(stream_get_contents(STDIN));')"
  printf "    'admin_password_hash' => %s,\n" "$(printf '%s' "${password_hash}" | php -r 'var_export(stream_get_contents(STDIN));')"
  printf "    'admin_totp_secret' => %s,\n" "$(printf '%s' "${totp_secret}" | php -r 'var_export(stream_get_contents(STDIN));')"
  printf "    'database_path' => %s,\n" "$(php -r 'var_export($argv[1]);' "${database_path}")"
  printf "    'from_email' => %s,\n" "$(php -r 'var_export($argv[1]);' "${sender_email}")"
  printf "    'from_name' => %s,\n" "$(php -r 'var_export($argv[1]);' "${sender_name}")"
  printf '%s\n' '];'
} > "${temporary_configuration}"

mv -f -- "${temporary_configuration}" "${configuration_path}"
temporary_configuration=""
unset administrator_username
unset password_hash totp_secret
chmod 0600 "${configuration_path}"
printf '%s\n' 'Private tester queue administrator access is configured.'
