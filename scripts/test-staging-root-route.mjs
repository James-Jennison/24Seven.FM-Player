#!/usr/bin/env node

import { readFile } from "node:fs/promises";

const htaccess = await readFile(new URL("../privacy-site/.htaccess", import.meta.url), "utf8");

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

assert(
  htaccess.includes("RewriteCond %{HTTP_HOST} ^onboarding-staging\\.player\\.jamesjennison\\.net(?::443)?$ [NC]"),
  "The staging-root route must be restricted to the staging hostname.",
);
assert(
  htaccess.includes("RewriteRule ^$ /private-tester-queue.php [R=302,L]"),
  "The staging root must redirect to the coordinator login route.",
);

console.log("Staging root coordinator-route contract: valid.");
