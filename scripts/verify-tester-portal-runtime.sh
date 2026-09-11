#!/usr/bin/env bash
# Verify the protected tester-portal configuration layout and its safe public
# failure mode without reading configuration values, issuing a tester token, or
# sending mail. Intended for a post-deploy operational check.
set -euo pipefail

portal_url="${TESTER_PORTAL_URL:-https://24sevenfmplayer.com/tester-portal.php}"
ssh_alias="${WEBSITE_SSH_ALIAS:-website-vm-admin}"

ssh -o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes "$ssh_alias" 'set -euo pipefail
httpd_pid=$(pgrep -o -f "[h]ttpd")
httpd_executable=$(readlink -f "/proc/${httpd_pid}/exe")
vhost_config=$("$httpd_executable" -t -D DUMP_VHOSTS 2>&1 | sed -n "s/.*port 443 namevhost 24sevenfmplayer\.com (\([^:]*\):[0-9][0-9]*).*/\1/p" | head -n 1)
test -n "$vhost_config"

document_root=$(awk "
  BEGIN { inside=0; matched=0 }
  /<VirtualHost[^>]*:443>/ { inside=1; matched=0 }
  inside && /ServerName[[:space:]]+24sevenfmplayer\\.com/ { matched=1 }
  inside && matched && /DocumentRoot[[:space:]]+/ { gsub(/\"/, \"\", \$2); print \$2; exit }
  /<\\/VirtualHost>/ { inside=0; matched=0 }
" "$vhost_config")
test -n "$document_root"

expected_config="$(dirname "$document_root")/.turnstile-test-config.php"
shared_config="$(dirname "$(dirname "$document_root")")/.turnstile-test-config.php"
config_owner=$(stat -c %U "$shared_config")

test -L "$expected_config"
test "$(readlink "$expected_config")" = "../.turnstile-test-config.php"
test "$(readlink -f "$expected_config")" = "$shared_config"
runuser -u "$config_owner" -- test -r "$expected_config"
printf "protected-runtime-layout=pass\n"'

get_status=$(curl --silent --show-error --max-time 20 --output /dev/null --write-out '%{http_code}' "$portal_url")
post_status=$(curl --silent --show-error --max-time 20 --output /dev/null --dump-header - --request POST \
  --header 'Origin: https://24sevenfmplayer.com' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data 'action=request_link&email=probe%40invalid.test&cf-turnstile-response=' \
  "$portal_url" | awk 'toupper($1) ~ /^HTTP\// { status=$2 } END { print status }')

test "$get_status" = '200'
test "$post_status" = '303'
printf 'public-get=200\nempty-turnstile-post=303\n'
