#!/usr/bin/env node

import { readFile } from "node:fs/promises";

const [portal, queue, build] = await Promise.all([
  readFile(new URL("../privacy-site/tester-portal.php", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/private-tester-queue.php", import.meta.url), "utf8"),
  readFile(new URL("validate-project-site.sh", import.meta.url), "utf8"),
]);

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

assert(portal.includes("define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true)"), "Tester portal must reuse the private queue library without invoking coordinator routing.");
assert(portal.includes("TESTER_PORTAL_SESSION_NAME"), "Tester portal must use a session separate from the coordinator session.");
assert(portal.includes("random_bytes(32)"), "Tester portal sign-in links must use cryptographically random tokens.");
assert(portal.includes("portalTokenHash($raw)"), "Tester portal must persist only the sign-in token hash.");
assert(portal.includes("PORTAL_TOKEN_TTL_SECONDS"), "Tester portal sign-in links must expire.");
assert(portal.includes("const PORTAL_REQUEST_COOLDOWN_SECONDS = 60;"), "Tester portal repeat-link cooldown must remain at one minute.");
assert(portal.includes("consumed_at"), "Tester portal sign-in links must be single use.");
assert(portal.includes("portalRenderLinkConfirmation"), "Sign-in links must require a confirmation step so mail scanners do not consume them.");
assert(portal.includes("action\" value=\"consume_link"), "Sign-in link consumption must be a form POST.");
assert(portal.includes("($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'GET' && isset($_GET['token'])"), "The confirmation screen must only render for a GET so its POST can consume the link.");
assert(portal.includes("tester_portal_tokens"), "Tester portal token storage is missing.");
assert(portal.includes("tester_feedback"), "Tester portal feedback persistence is missing.");
assert(portal.includes("WHERE id = ? AND tester_id = ?"), "Feedback must be bound to the signed-in tester's own assignment.");
assert(portal.includes("play_opt_in_confirmed_at"), "Tester opt-in confirmation state is missing.");
assert(portal.includes("onboarding_status = 'ready'"), "A completed opt-in must advance a complete tester to ready for assignment.");
assert(portal.includes("PORTAL_TESTING_INTERESTS"), "Tester profile must include the original testing-interest intake options.");
assert(portal.includes("name=\"display_name\""), "Tester profile must allow a tester to update their name.");
assert(portal.includes("name=\"country\""), "Tester profile must allow a tester to update their country or region.");
assert(portal.includes("portalCheckboxes('other_stations'"), "Tester profile must allow a tester to update other familiar stations.");
assert(portal.includes("portalCheckboxes('testing_interests'"), "Tester profile must allow a tester to update testing interests.");
assert(portal.includes("name=\"experience\""), "Tester profile must allow a tester to update assignment notes or experience.");
assert(portal.includes("Email changes are reviewed by the coordinator"), "Tester email must remain a reviewed identity change.");
assert(portal.includes("TESTER_PORTAL_TURNSTILE_ACTION"), "Magic-link requests must be protected by Turnstile.");
assert(portal.includes("portalVerifyTurnstile($turnstileToken"), "Turnstile must be verified before a magic link is sent.");
assert(portal.includes("We could not send a sign-in link. Please try again later."), "Portal link requests must report a failed transport instead of claiming delivery.");
assert(portal.includes("DELETE FROM tester_portal_tokens WHERE id = ?"), "A failed portal-link delivery must remove its unusable token.");
assert(portal.includes("function portalAdminPreviewTesterId"), "The coordinator must have an authenticated tester-view preview path.");
assert(portal.includes("($_SESSION['authenticated'] ?? false) === true"), "Tester previews must require the coordinator session.");
assert(portal.includes("Tester previews are read-only."), "Tester previews must reject state-changing requests.");
assert(portal.includes("Read-only coordinator preview."), "Tester previews must be visibly identified as read-only.");
assert(!portal.includes("password_verify("), "Tester portal must not collect or verify tester passwords.");
assert(queue.includes("CREATE TABLE IF NOT EXISTS tester_portal_tokens"), "Queue database migration must create portal token storage.");
assert(queue.includes("CREATE TABLE IF NOT EXISTS tester_feedback"), "Queue database migration must create feedback storage.");
assert(queue.includes("play_opt_in_confirmed_at"), "Queue database migration must retain tester opt-in confirmation.");
assert(queue.includes("Tester task reports"), "Coordinator workspaces must display submitted tester reports.");
assert(queue.includes("Preview tester view"), "Coordinator roster and workspaces must link to the tester-view preview.");
assert(build.includes("test-tester-portal.mjs"), "Site validator must run the tester portal contract.");

console.log("Tester portal security and data-routing contract: valid.");
