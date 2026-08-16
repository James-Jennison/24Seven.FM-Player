#!/usr/bin/env bash
set -euo pipefail

# Run this only from an owner-authenticated terminal on the isolated onboarding
# staging host. It never accepts credentials through arguments or environment
# variables, and it writes no password or hash to stdout, stderr, or logs.

readonly EXPECTED_ADMIN_USERNAME='jjennison'
readonly STAGING_CONFIRMATION='onboarding-staging.player.jamesjennison.net'
readonly EXPECTED_QUEUE_HANDLER_SHA256='384104ec7f264872313f87ce8e67fc3ec3d357648dd0cdebe71d3f03f6ab0e6c'
readonly STAGING_ACCOUNT_ROOT='/home/jamesjen'

if [[ ! -t 0 || ! -t 1 ]]; then
  printf '%s\n' 'An interactive owner terminal is required; no change was made.' >&2
  exit 1
fi

if [[ $# -ne 0 ]]; then
  printf '%s\n' 'Usage: reset-onboarding-staging-admin-credential.sh' >&2
  exit 1
fi

read -r -p "Type ${STAGING_CONFIRMATION} to confirm the staging-only credential reset: " confirmation
if [[ "${confirmation}" != "${STAGING_CONFIRMATION}" ]]; then
  printf '%s\n' 'Confirmation did not match; no change was made.' >&2
  exit 1
fi
unset confirmation

read -r -s -p 'New staging administrator password: ' password
printf '\n'
read -r -s -p 'Confirm staging administrator password: ' password_confirmation
printf '\n'
trap 'unset password password_confirmation' EXIT

if [[ ${#password} -lt 8 ]]; then
  printf '%s\n' 'The staging administrator password must be at least 8 characters; no change was made.' >&2
  exit 1
fi
if [[ "${password}" != "${password_confirmation}" ]]; then
  printf '%s\n' 'Passwords did not match; no change was made.' >&2
  exit 1
fi

vhost_file=""
for vhost_directory in /usr/local/apps/apache2/etc/conf.d /etc/apache2 /etc/httpd; do
  [[ -d "${vhost_directory}" ]] || continue
  candidate="$(grep -rlE --include='*.conf' --include='*.vhost' "^[[:space:]]*ServerName[[:space:]]+${STAGING_CONFIRMATION}([[:space:]]|$)" "${vhost_directory}" 2>/dev/null | head -n 1 || true)"
  if [[ -n "${candidate}" ]]; then
    vhost_file="${candidate}"
    break
  fi
done

if [[ -z "${vhost_file}" ]]; then
  printf '%s\n' 'The staging virtual-host mapping is unavailable; no change was made.' >&2
  exit 1
fi

queue_handler=''
configuration_path=''

# The staging vhost uses an Alias-backed release layout. Select the sole
# deployed handler matching the reviewed source hash and an adjacent writable
# private store; fail closed if the release mapping becomes ambiguous.
while IFS= read -r handler_candidate; do
  candidate_hash="$(sha256sum "${handler_candidate}" | awk '{print $1}')"
  [[ "${candidate_hash}" == "${EXPECTED_QUEUE_HANDLER_SHA256}" ]] || continue
  candidate_store="$(dirname "$(dirname "${handler_candidate}")")/.private-tester-queue-config.php"
  [[ -r "${candidate_store}" && -w "${candidate_store}" ]] || continue
  if [[ -n "${queue_handler}" ]]; then
    printf '%s\n' 'The staging queue handler mapping is ambiguous; no change was made.' >&2
    exit 1
  fi
  queue_handler="${handler_candidate}"
  configuration_path="${candidate_store}"
done < <(find "${STAGING_ACCOUNT_ROOT}" -xdev -type f -name private-tester-queue.php 2>/dev/null)

if [[ -z "${queue_handler}" ]]; then
  printf '%s\n' 'The deployed staging queue handler is unavailable; no change was made.' >&2
  exit 1
fi


# The password travels to PHP only on stdin, never through a process argument
# or environment variable. PHP atomically replaces the existing private config,
# clears only the sole coordinator account's staging login-limit table, and
# verifies the stored replacement without printing secret material.
printf '%s\0' "${password}" | php -d display_errors=0 -d log_errors=0 -r '
$configurationPath = $argv[1];
$expectedUsername = $argv[2];
$password = stream_get_contents(STDIN);
if (!is_string($password) || !str_ends_with($password, "\0")) {
    fwrite(STDERR, "Credential input was unavailable; no change was made.\n");
    exit(1);
}
$password = substr($password, 0, -1);
if (strlen($password) < 8) {
    fwrite(STDERR, "The staging administrator password must be at least 8 characters; no change was made.\n");
    exit(1);
}

$config = require $configurationPath;
if (!is_array($config)
    || !isset($config["admin_username"], $config["admin_password_hash"], $config["database_path"])
    || !is_string($config["admin_username"])
    || !is_string($config["admin_password_hash"])
    || !is_string($config["database_path"])
    || !hash_equals($expectedUsername, $config["admin_username"])) {
    fwrite(STDERR, "The existing staging administrator account does not match the expected account; no change was made.\n");
    exit(1);
}

$newHash = password_hash($password, PASSWORD_DEFAULT);
if (!is_string($newHash) || !password_get_info($newHash)["algo"]) {
    fwrite(STDERR, "A staging password hash could not be created; no change was made.\n");
    exit(1);
}

$config["admin_password_hash"] = $newHash;
$temporaryPath = tempnam(dirname($configurationPath), ".private-tester-queue-config.");
if ($temporaryPath === false) {
    fwrite(STDERR, "The staging credential store could not be prepared; no change was made.\n");
    exit(1);
}

try {
    $payload = "<?php\ndeclare(strict_types=1);\n\nreturn " . var_export($config, true) . ";\n";
    if (file_put_contents($temporaryPath, $payload, LOCK_EX) === false) {
        throw new RuntimeException();
    }
    $existingPermissions = fileperms($configurationPath);
    if ($existingPermissions === false || !chmod($temporaryPath, $existingPermissions & 0777)) {
        throw new RuntimeException();
    }
    if (!rename($temporaryPath, $configurationPath)) {
        throw new RuntimeException();
    }
    $temporaryPath = "";

    $database = new PDO("sqlite:" . $config["database_path"], null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
    $database->beginTransaction();
    // The current schema contains one coordinator account. Every row in this
    // table is its staging login-rate-limit state, keyed by client address.
    $database->exec("DELETE FROM administrator_login_limits");
    $remainingLimits = (int) $database->query("SELECT COUNT(*) FROM administrator_login_limits")->fetchColumn();
    if ($remainingLimits !== 0) {
        throw new RuntimeException();
    }
    $database->commit();

    $updated = require $configurationPath;
    if (!is_array($updated)
        || !isset($updated["admin_username"], $updated["admin_password_hash"])
        || !is_string($updated["admin_username"])
        || !is_string($updated["admin_password_hash"])
        || !hash_equals($expectedUsername, $updated["admin_username"])
        || !password_get_info($updated["admin_password_hash"])["algo"]
        || !password_verify($password, $updated["admin_password_hash"])) {
        throw new RuntimeException();
    }
} catch (Throwable $error) {
    if (isset($database) && $database instanceof PDO && $database->inTransaction()) {
        $database->rollBack();
    }
    if ($temporaryPath !== "" && is_file($temporaryPath)) {
        @unlink($temporaryPath);
    }
    fwrite(STDERR, "The staging credential reset did not complete; no secret details were emitted.\n");
    exit(1);
}

echo "The jjennison staging administrator credential was updated and its staging login rate limit was cleared.\n";
' "${configuration_path}" "${EXPECTED_ADMIN_USERNAME}"

unset password password_confirmation
trap - EXIT
