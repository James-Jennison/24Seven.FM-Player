#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'signed-play-bundle=failed: %s\n' "$*" >&2
  exit 1
}

project_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail 'run from a Git worktree'
git -C "$project_root" diff --quiet || fail 'source worktree has unstaged changes'
git -C "$project_root" diff --cached --quiet || fail 'source worktree has staged changes'

keystore=${TWENTYFOURSEVEN_UPLOAD_STORE_FILE:-}
[[ -n "$keystore" ]] || fail 'set TWENTYFOURSEVEN_UPLOAD_STORE_FILE to the authorized external JKS file'
[[ -f "$keystore" ]] || fail 'the configured JKS file does not exist'

resolved_keystore=$(readlink -f "$keystore")
[[ "$resolved_keystore" != "$project_root"/* ]] || fail 'the JKS file must remain outside the repository'

for required_command in keytool jarsigner openssl sha256sum; do
  command -v "$required_command" >/dev/null || fail "required command '$required_command' is unavailable"
done

IFS= read -r -s -p 'Play upload keystore password: ' signing_password
printf '\n'
[[ -n "$signing_password" ]] || fail 'a password is required'

cleanup() {
  unset signing_password KEYTOOL_STORE_PASSWORD TWENTYFOURSEVEN_UPLOAD_STORE_PASSWORD TWENTYFOURSEVEN_UPLOAD_KEY_PASSWORD
}
trap cleanup EXIT

export KEYTOOL_STORE_PASSWORD="$signing_password"
private_key_aliases=$(keytool -list -v \
  -keystore "$resolved_keystore" \
  -storepass:env KEYTOOL_STORE_PASSWORD \
  | awk '
      /^Alias name: / { alias = substr($0, 13) }
      /^Entry type: PrivateKeyEntry$/ { print alias }
    ')
alias_count=$(printf '%s\n' "$private_key_aliases" | sed '/^$/d' | wc -l | tr -d ' ')
[[ "$alias_count" == '1' ]] || fail 'the JKS file must contain exactly one private-key entry'
key_alias=$(printf '%s\n' "$private_key_aliases" | sed '/^$/d')

build_root=${TWENTYFOURSEVEN_ANDROID_BUILD_DIR:-"$project_root/app/build"}
bundle="$build_root/outputs/bundle/release/app-release.aab"
source_revision=$(git -C "$project_root" rev-parse HEAD)

TWENTYFOURSEVEN_SOURCE_REVISION="$source_revision" \
TWENTYFOURSEVEN_UPLOAD_STORE_FILE="$resolved_keystore" \
TWENTYFOURSEVEN_UPLOAD_STORE_PASSWORD="$signing_password" \
TWENTYFOURSEVEN_UPLOAD_KEY_ALIAS="$key_alias" \
TWENTYFOURSEVEN_UPLOAD_KEY_PASSWORD="$signing_password" \
  /bin/bash "$project_root/gradlew" :app:bundleRelease --no-daemon --console=plain

[[ -f "$bundle" ]] || fail 'Gradle completed without producing the expected release bundle'
jarsigner -verify -verbose -certs "$bundle" | grep -q '^jar verified\.$' \
  || fail 'the release bundle does not contain a verified JAR signature'

keystore_certificate=$(keytool -exportcert \
  -keystore "$resolved_keystore" \
  -alias "$key_alias" \
  -storepass:env KEYTOOL_STORE_PASSWORD \
  | sha256sum \
  | awk '{ print $1 }')
bundle_certificate=$(keytool -printcert -rfc -jarfile "$bundle" \
  | openssl x509 -inform PEM -outform DER \
  | sha256sum \
  | awk '{ print $1 }')
[[ "$keystore_certificate" == "$bundle_certificate" ]] \
  || fail 'the bundle signer does not match the configured JKS certificate'

bundle_hash=$(sha256sum "$bundle" | awk '{ print $1 }')
printf 'signed-play-bundle=passed\n'
printf 'source-revision=%s\n' "$source_revision"
printf 'bundle=%s\n' "$bundle"
printf 'bundle-sha256=%s\n' "$bundle_hash"
printf 'upload-certificate-sha256=%s\n' "$bundle_certificate"
