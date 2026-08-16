#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"

"${script_dir}/build-project-site.sh"
python3 "${script_dir}/validate-tester-tasks.py"
node --check "${repository_root}/privacy-site/assets/project.js"
node --check "${repository_root}/privacy-site/assets/theme-init.js"
node --check "${repository_root}/privacy-site/assets/private-tester-queue.js"
node --check "${repository_root}/privacy-site/assets/onboarding-live-chat.js"
if command -v php >/dev/null 2>&1; then
  php -l "${repository_root}/privacy-site/alpha-tester-interest.php"
  php -l "${repository_root}/privacy-site/tester-onboarding-storage.php"
  php -l "${repository_root}/privacy-site/private-tester-queue.php"
  php "${repository_root}/scripts/test-administrator-login-security.php"
  php -l "${repository_root}/privacy-site/tester-portal.php"
  php "${repository_root}/scripts/test-private-tester-email.php"
  php "${repository_root}/scripts/test-coordinator-email-composer.php"
  php -l "${repository_root}/privacy-site/turnstile-test.php"
  php "${repository_root}/scripts/test-turnstile-test-confirmation.php"
  php -l "${repository_root}/scripts/import-alpha-tester-mailbox.php"
  php -l "${repository_root}/scripts/reroute-private-tester-batch.php"
  php "${repository_root}/scripts/test-alpha-tester-signup-confirmation.php"
  php "${repository_root}/scripts/test-alpha-tester-intake.php"
  php "${repository_root}/scripts/test-alpha-tester-auto-onboarding.php"
  php "${repository_root}/scripts/test-alpha-tester-import.php"
  php "${repository_root}/scripts/test-onboarding-live-chat.php"
  php "${repository_root}/scripts/test-onboarding-phase2-local.php"
else
  printf '%s\n' 'PHP CLI is unavailable locally; the exact staged artifact must be linted by the Player PHP runtime before promotion.'
fi
node --check "${repository_root}/scripts/test-project-site-browser.mjs"
node --check "${repository_root}/scripts/test-project-site-firefox.mjs"
node "${repository_root}/scripts/test-private-tester-queue.mjs"
node "${repository_root}/scripts/test-mfa-enrollment-helper.mjs"
node "${repository_root}/scripts/test-tester-portal.mjs"
node "${repository_root}/scripts/test-onboarding-phase2-ui.mjs"
python3 "${script_dir}/validate-project-site.py" "${repository_root}/_site"

"${script_dir}/prepare-pages-transition.sh" "_pages-transition"
python3 "${script_dir}/validate-pages-transition.py" "${repository_root}/_pages-transition"
