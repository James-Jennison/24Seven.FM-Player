#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
site_root="${repository_root}/privacy-site"
privacy_directory="${site_root}/privacy"
privacy_page="${privacy_directory}/index.md"
temporary_privacy=""

cleanup() {
  if [[ -n "${temporary_privacy}" && -f "${temporary_privacy}" ]]; then
    rm -f -- "${temporary_privacy}"
  fi
}

trap cleanup EXIT
umask 022

mkdir -p -- "${privacy_directory}" "${site_root}/assets/project/screenshots" "${site_root}/assets/project/marketing"
temporary_privacy="$(mktemp "${privacy_directory}/.index.XXXXXX")"

{
  printf '%s\n' \
    '---' \
    'layout: default' \
    'title: Privacy notice · 24Seven.FM Player' \
    'description: Understand what the 24Seven.FM Player handles, stores locally, sends to a station, and deliberately does not collect.' \
    'permalink: /privacy/' \
    'section: privacy' \
    '---'
  # The layout supplies the page h1; omit only the source document's duplicate
  # first-line heading while preserving the canonical notice body verbatim.
  sed '1d' "${repository_root}/PRIVACY.md"
} > "${temporary_privacy}"

mv -- "${temporary_privacy}" "${privacy_page}"
temporary_privacy=""

cp -- "${repository_root}/docs/play-store-assets/app-icon-512.png" "${site_root}/assets/project/app-icon.png"
cp -- "${repository_root}/docs/play-store-assets/feature-graphic-1024x500.png" "${site_root}/assets/project/feature-graphic.png"
cp -- "${repository_root}/docs/play-store-assets/screenshots/phone-player-live-playing.png" "${site_root}/assets/project/screenshots/player.png"
cp -- "${repository_root}/docs/play-store-assets/screenshots/phone-queue-live.png" "${site_root}/assets/project/screenshots/queue.png"
cp -- "${repository_root}/docs/play-store-assets/screenshots/phone-stations.png" "${site_root}/assets/project/screenshots/stations.png"
cp -- "${repository_root}/docs/play-store-assets/screenshots/phone-more.png" "${site_root}/assets/project/screenshots/more.png"
cp -- "${repository_root}/docs/play-store-assets/marketing/chat-synthetic.png" "${site_root}/assets/project/marketing/chat-synthetic.png"
cp -- "${repository_root}/docs/play-store-assets/marketing/favorites-live.png" "${site_root}/assets/project/marketing/favorites-live.png"
cp -- "${repository_root}/docs/play-store-assets/marketing/now-playing-live.png" "${site_root}/assets/project/marketing/now-playing-live.png"
cp -- "${repository_root}/docs/play-store-assets/marketing/queue-live.png" "${site_root}/assets/project/marketing/queue-live.png"

printf 'Prepared the project site source at %s\n' "${site_root}"
