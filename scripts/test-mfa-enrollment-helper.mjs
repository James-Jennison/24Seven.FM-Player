#!/usr/bin/env node

import { readFile } from "node:fs/promises";

const helper = await readFile(new URL("configure-private-tester-queue.sh", import.meta.url), "utf8");

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

assert(helper.includes("command -v qrencode"), "MFA enrollment must require a local QR renderer.");
assert(helper.includes("otpauth://totp/24Seven.FM%20Player:"), "MFA enrollment must build a standard authenticator URI.");
assert(helper.includes("qrencode -t UTF8 -m 2 -s 1"), "MFA enrollment must render a terminal QR code locally.");
assert(!helper.includes("printf 'Secret: %s"), "MFA enrollment must not print the raw seed when QR enrollment is available.");
assert(helper.includes("Enter the current six-digit code to confirm enrollment"), "MFA enrollment must verify the authenticator before saving configuration.");

console.log("MFA enrollment helper contract: valid.");
