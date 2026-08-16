import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const helper = readFileSync(new URL("./activate-onboarding-staging-admin-access.sh", import.meta.url), "utf8");
assert(helper.includes('[[ ! -t 0 || ! -t 1 ]]'), "Staging activation must require an interactive owner terminal.");
assert(helper.includes('SSH_CONNECTION'), "Staging activation must bind to the authenticated SSH client address.");
assert(helper.includes("Type ENABLE"), "Staging activation must require explicit owner confirmation.");
assert(helper.includes('14400'), "Staging activation must have a short fixed expiry.");
assert(helper.includes('chmod 0600'), "Staging activation config must remain owner-readable only.");
assert(helper.includes('"${1:-}" == "--disable"'), "Staging activation must support explicit removal.");
assert(!helper.includes('admin_totp_secret'), "Staging activation must not handle MFA secrets.");
assert(!helper.includes('admin_password_hash'), "Staging activation must not handle administrator password hashes.");
console.log("Staging administrator access helper contract: valid.");
