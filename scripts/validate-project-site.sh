#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"

"${script_dir}/build-project-site.sh"
python3 "${script_dir}/validate-tester-tasks.py"
node --check "${repository_root}/privacy-site/assets/project.js"
node --check "${repository_root}/privacy-site/assets/theme-init.js"
node --check "${repository_root}/privacy-site/assets/private-tester-queue.js"
if command -v php >/dev/null 2>&1; then
  php -l "${repository_root}/privacy-site/alpha-tester-interest.php"
  php -l "${repository_root}/privacy-site/private-tester-queue.php"
  php -l "${repository_root}/scripts/import-alpha-tester-mailbox.php"
  php -l "${repository_root}/scripts/reroute-private-tester-batch.php"
else
  printf '%s\n' 'PHP CLI is unavailable locally; the exact staged artifact must be linted by the Player PHP runtime before promotion.'
fi
node --check "${repository_root}/scripts/test-project-site-browser.mjs"
node --check "${repository_root}/scripts/test-project-site-firefox.mjs"
node "${repository_root}/scripts/test-private-tester-queue.mjs"
python3 "${script_dir}/validate-project-site.py" "${repository_root}/_site"

"${script_dir}/prepare-pages-transition.sh" "_pages-transition"
python3 "${script_dir}/validate-pages-transition.py" "${repository_root}/_pages-transition"
