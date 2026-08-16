#!/usr/bin/env bash
set -euo pipefail

script_path="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/reset-onboarding-staging-admin-credential.sh"
runner_path="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/run-onboarding-staging-admin-credential-reset.sh"

[[ -x "${script_path}" ]] || {
  printf '%s\n' 'The staging credential-reset command must be executable.' >&2
  exit 1
}

bash -n "${script_path}"
[[ -x "${runner_path}" ]] || {
  printf '%s\n' 'The staging credential-reset runner must be executable.' >&2
  exit 1
}
bash -n "${runner_path}"

for required_fragment in \
  "EXPECTED_ADMIN_USERNAME='jjennison'" \
  'read -r -s -p' \
  '[[ ${#password} -lt 8 ]]' \
  'strlen($password) < 8' \
  'readlink -f "${staging_document_root}/private-tester-queue.php"' \
  'dirname "$(dirname "${queue_handler}")"' \
  'password_hash($password, PASSWORD_DEFAULT)' \
  'DELETE FROM administrator_login_limits' \
  'password_verify($password, $updated["admin_password_hash"])' \
  'onboarding-staging.player.jamesjennison.net'; do
  grep -Fq -- "${required_fragment}" "${script_path}" || {
    printf 'Missing required staging credential-reset control: %s\n' "${required_fragment}" >&2
    exit 1
  }
done

for required_runner_fragment in \
  'ssh -tt -F /home/jjennison/.ssh/config' \
  'website-vm-admin' \
  'reset-onboarding-staging-admin-credential.sh'; do
  grep -Fq -- "${required_runner_fragment}" "${runner_path}" || {
    printf 'Missing required staging credential-reset runner control: %s\n' "${required_runner_fragment}" >&2
    exit 1
  }
done

if grep -Eq -- '(^|[^[:alnum:]_])echo[[:space:]].*\$\{?password' "${script_path}"; then
  printf '%s\n' 'The staging credential-reset command must not echo password values.' >&2
  exit 1
fi

printf '%s\n' 'Staging credential-reset command checks passed.'
