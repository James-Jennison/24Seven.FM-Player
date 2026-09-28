#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
source_root="${repository_root}/cast-receiver"
destination_root="${repository_root}/_site/cast"

runtime_files=(
  .htaccess
  index.html
  receiver-artwork-v1.css
  receiver-artwork-v2.css
  receiver-landscape-v1.css
  receiver-landscape-v2.js
  assets/24seven-fm-mark.svg
)

for relative_path in "${runtime_files[@]}"; do
  [[ -f "${source_root}/${relative_path}" ]] || {
    printf 'Missing Cast receiver source: %s\n' "${relative_path}" >&2
    exit 1
  }
done

install -d -m 0755 "${destination_root}/assets"
for relative_path in "${runtime_files[@]}"; do
  install -m 0644 "${source_root}/${relative_path}" "${destination_root}/${relative_path}"
done

printf 'Staged the Cast receiver at %s\n' "${destination_root}"
