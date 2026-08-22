#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"

# This is the one release-quality gate for the protected onboarding portal.
# It intentionally builds the same _site artifact that an approved Webuzo
# promotion will stage and compare; it does not deploy or contact production.
git -C "${repository_root}" diff --check
"${script_dir}/validate-project-site.sh"

printf 'Onboarding portal release gate passed for %s\n' \
  "$(git -C "${repository_root}" rev-parse HEAD)"
