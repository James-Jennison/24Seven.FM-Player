#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
jekyll_image="${PROJECT_SITE_JEKYLL_IMAGE:-ghcr.io/actions/jekyll-build-pages@sha256:6791ebfd912185ed59bfb5fb102664fa872496b79f87ff8b9cfba292a7345041}"
destination="${repository_root}/_site"

"${script_dir}/prepare-project-site.sh"

if [[ -d "${destination}" ]]; then
  rm -rf -- "${destination}"
fi

docker run --rm \
  --user "$(id -u):$(id -g)" \
  -e GITHUB_WORKSPACE=/workspace \
  -e INPUT_SOURCE=privacy-site \
  -e INPUT_DESTINATION=_site \
  -e INPUT_TOKEN= \
  -e GITHUB_REPOSITORY=James-Jennison/24Seven.FM-Player \
  -e INPUT_BUILD_REVISION="$(git -C "${repository_root}" rev-parse HEAD)" \
  -e GITHUB_API_URL=https://api.github.com \
  -e INPUT_VERBOSE=false \
  -e INPUT_FUTURE=false \
  -v "${repository_root}:/workspace" \
  "${jekyll_image}"

# Jekyll intentionally excludes executable server files. The endpoint is
# reviewed with the static artifact but runs only through the existing
# Webuzo PHP handler after an approved deployment.
install -m 0644 "${repository_root}/privacy-site/alpha-tester-interest.php" "${destination}/alpha-tester-interest.php"
install -m 0644 "${repository_root}/privacy-site/private-tester-queue.php" "${destination}/private-tester-queue.php"
install -m 0644 "${repository_root}/privacy-site/turnstile-test.php" "${destination}/turnstile-test.php"
# The public task metadata is rendered into the workspace by Jekyll and read by
# the protected PHP queue from this derived artifact. It contains no tester data.
install -m 0644 "${repository_root}/privacy-site/_data/tester_tasks.json" "${destination}/assets/tester-tasks.json"

printf 'Built the project site at %s\n' "${destination}"
