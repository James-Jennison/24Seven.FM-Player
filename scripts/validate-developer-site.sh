#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
artifact_root="${1:-${repository_root}/_dev_site}"

required_files=(
  .htaccess
  404.html
  index.html
  development/index.html
  testing/index.html
  tester-workspace/index.html
  roadmap/index.html
  resources/index.html
  assets/project.css
  assets/project.js
  assets/theme-init.js
  assets/project/app-icon.png
)

for file in "${required_files[@]}"; do
  [[ -f "${artifact_root}/${file}" ]] || { printf 'Missing developer-site file: %s\n' "${file}" >&2; exit 1; }
done

if rg -n 'https://player\.jamesjennison\.net/dev|href="/dev' "${artifact_root}" --glob '*.html'; then
  printf 'Developer-site artifact retains Player /dev links.\n' >&2
  exit 1
fi

for document in "${artifact_root}"/index.html "${artifact_root}"/*/index.html; do
  rg -q 'https://dev\.jamesjennison\.net' "${document}" || { printf 'Missing developer origin: %s\n' "${document}" >&2; exit 1; }
done

printf 'Developer-site artifact: 6 workspace routes and shared assets — valid.\n'
