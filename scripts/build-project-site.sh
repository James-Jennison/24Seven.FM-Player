#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
jekyll_image="${PROJECT_SITE_JEKYLL_IMAGE:-ghcr.io/actions/jekyll-build-pages@sha256:6791ebfd912185ed59bfb5fb102664fa872496b79f87ff8b9cfba292a7345041}"
destination="${repository_root}/_site"
docker_user="${PROJECT_SITE_DOCKER_USER:-$(id -u):$(id -g)}"

"${script_dir}/prepare-project-site.sh"

if [[ -d "${destination}" ]]; then
  rm -rf -- "${destination}"
fi
# A rootless Docker host can map the host checkout owner to a different
# container identity. The public build destination is deliberately made
# writable for the isolated build container; source files remain read-only.
install -d -m 0777 "${destination}"

docker run --rm \
  --user "${docker_user}" \
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

# Jekyll intentionally excludes executable server files. Copy them in a second
# container invocation so a rootless Docker host keeps the generated assets and
# PHP hand-off writable by the same mapped container identity.
docker run --rm \
  --user "${docker_user}" \
  -v "${repository_root}:/workspace" \
  --entrypoint /bin/sh \
  "${jekyll_image}" \
  -ec '
    install -m 0644 /workspace/privacy-site/alpha-tester-interest.php /workspace/_site/alpha-tester-interest.php
    install -m 0644 /workspace/privacy-site/tester-onboarding-storage.php /workspace/_site/tester-onboarding-storage.php
    install -m 0644 /workspace/privacy-site/private-tester-queue.php /workspace/_site/private-tester-queue.php
    install -m 0644 /workspace/privacy-site/tester-portal.php /workspace/_site/tester-portal.php
    install -m 0644 /workspace/privacy-site/turnstile-test.php /workspace/_site/turnstile-test.php
    install -m 0644 /workspace/privacy-site/_data/tester_tasks.json /workspace/_site/assets/tester-tasks.json
  '

printf 'Built the project site at %s\n' "${destination}"
