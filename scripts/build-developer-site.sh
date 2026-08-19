#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
source_site="${repository_root}/_site"
destination="${repository_root}/_dev_site"
developer_origin="https://dev.jamesjennison.net"
public_origin="https://player.jamesjennison.net"

"${script_dir}/build-project-site.sh"

rm -rf -- "${destination}"
install -d "${destination}"
cp -a "${source_site}/dev/." "${destination}/"
cp -a "${source_site}/assets" "${destination}/assets"
install -m 0644 "${source_site}/.htaccess" "${destination}/.htaccess"
install -m 0644 "${source_site}/404.html" "${destination}/404.html"
install -m 0644 "${source_site}/robots.txt" "${destination}/robots.txt"
install -m 0644 "${source_site}/site.webmanifest" "${destination}/site.webmanifest"

while IFS= read -r -d '' document; do
  perl -0pi -e '
    s{https://player\.jamesjennison\.net/dev(?=[/"])}{https://dev.jamesjennison.net}g;
    s{href="/dev(?=[/"])}{href="https://dev.jamesjennison.net}g;
    s{href="/(privacy|product-testing)/}{href="https://player.jamesjennison.net/$1/}g;
    s{href="/"}{href="https://player.jamesjennison.net/"}g;
  ' "${document}"
done < <(find "${destination}" -type f -name '*.html' -print0)

printf 'Built the developer site at %s\n' "${destination}"
