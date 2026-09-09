#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd -- "${script_dir}/.." && pwd)"
relative_destination="${1:-_pages-transition}"

if [[ "${relative_destination}" == /* || "${relative_destination}" == "." || "${relative_destination}" == ".." || "${relative_destination}" == *"/../"* ]]; then
  printf 'Unsafe transition destination: %s\n' "${relative_destination}" >&2
  exit 1
fi

destination="${repository_root}/${relative_destination}"
case "${destination}" in
  "${repository_root}/"*) ;;
  *) printf 'Transition destination escaped the repository.\n' >&2; exit 1 ;;
esac

if [[ -e "${destination}" ]]; then
  rm -rf -- "${destination}"
fi
mkdir -p -- "${destination}"

write_transition() {
  local output_path="$1"
  local target_url="$2"
  local target_label="$3"
  local output_directory
  output_directory="$(dirname -- "${output_path}")"
  mkdir -p -- "${output_directory}"

  printf '%s\n' \
    '<!doctype html>' \
    '<html lang="en">' \
    '<head>' \
    '  <meta charset="utf-8">' \
    '  <meta name="viewport" content="width=device-width, initial-scale=1">' \
    '  <meta name="robots" content="noindex, follow">' \
    "  <meta http-equiv=\"refresh\" content=\"5; url=${target_url}\">" \
    "  <link rel=\"canonical\" href=\"${target_url}\">" \
    '  <title>24Seven.FM Player has moved</title>' \
    '  <style>body{margin:0;background:#090c15;color:#f7f7fb;font:1rem/1.6 system-ui,sans-serif}main{width:min(42rem,calc(100% - 2rem));margin:12vh auto;padding:clamp(1.5rem,5vw,3rem);border:1px solid #343a55;border-radius:1rem;background:#111522}p{color:#bdc3d6}a{color:#71e2cf;text-underline-offset:.2em}a:focus-visible{outline:3px solid #8eb8ff;outline-offset:.3rem}</style>' \
    '</head>' \
    '<body>' \
    '<main>' \
    '  <p>24Seven.FM Player</p>' \
    '  <h1>This project site has moved.</h1>' \
    "  <p>${target_label} now lives on the dedicated JamesJennison.net project domain.</p>" \
    "  <p><a href=\"${target_url}\" data-transition-target>Continue to ${target_url}</a></p>" \
    '  <p>This transition page does not collect analytics or visitor data.</p>' \
    '</main>' \
    '<script>' \
    "  const target = new URL('${target_url}');" \
    '  target.search = window.location.search;' \
    '  target.hash = window.location.hash;' \
    '  const link = document.querySelector("[data-transition-target]");' \
    '  link.href = target.href;' \
    '  window.location.replace(target.href);' \
    '</script>' \
    '</body>' \
    '</html>' > "${output_path}"
}

write_transition "${destination}/index.html" "https://24sevenfmplayer.com/privacy/" "The privacy notice"
write_transition "${destination}/project/index.html" "https://24sevenfmplayer.com/" "The project overview"
write_transition "${destination}/project/features/index.html" "https://24sevenfmplayer.com/features/" "Product features"
write_transition "${destination}/project/development/index.html" "https://24sevenfmplayer.com/development/" "Development documentation"
write_transition "${destination}/project/testing/index.html" "https://24sevenfmplayer.com/testing/" "Testing evidence"
write_transition "${destination}/project/product-testing/index.html" "https://24sevenfmplayer.com/product-testing/" "The product-testing workspace"
write_transition "${destination}/project/roadmap/index.html" "https://24sevenfmplayer.com/roadmap/" "The project roadmap"
write_transition "${destination}/project/resources/index.html" "https://24sevenfmplayer.com/resources/" "Project resources"

printf '%s\n' 'User-agent: *' 'Disallow: /' > "${destination}/robots.txt"
touch "${destination}/.nojekyll"
printf 'Prepared the GitHub Pages transition artifact at %s\n' "${destination}"
