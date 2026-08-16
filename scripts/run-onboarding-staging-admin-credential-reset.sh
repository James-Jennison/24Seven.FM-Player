#!/usr/bin/env bash
set -euo pipefail

# Starts the deployed staging-only reset command in an interactive SSH terminal.
# Password input remains exclusively between the operator's terminal and the
# staging host; this wrapper never accepts, reads, or forwards a password.

ssh -tt -F /home/jjennison/.ssh/config \
  -o BatchMode=yes \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=yes \
  -o ConnectTimeout=15 \
  website-vm-admin '
set -euo pipefail
vhost_file=""
for vhost_directory in /usr/local/apps/apache2/etc/conf.d /etc/apache2 /etc/httpd; do
  [ -d "${vhost_directory}" ] || continue
  candidate=$(grep -rlE --include="*.conf" --include="*.vhost" "^[[:space:]]*ServerName[[:space:]]+onboarding-staging\.player\.jamesjennison\.net([[:space:]]|$)" "${vhost_directory}" 2>/dev/null | head -n 1 || true)
  if [ -n "${candidate}" ]; then
    vhost_file="${candidate}"
    break
  fi
done
[ -n "${vhost_file}" ] || { printf "%s\\n" "staging-vhost-not-found" >&2; exit 1; }
staging_document_root=$(awk "tolower(\$1) == \"documentroot\" { print \$2; exit }" "${vhost_file}")
[ -n "${staging_document_root}" ] || { printf "%s\\n" "staging-document-root-not-found" >&2; exit 1; }
exec bash "$(dirname "${staging_document_root}")/reset-onboarding-staging-admin-credential.sh"
'
