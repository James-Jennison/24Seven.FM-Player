#!/usr/bin/env node

import { readFile } from "node:fs/promises";

const [portal, queue, build, wizard] = await Promise.all([
  readFile(new URL("../privacy-site/tester-portal.php", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/private-tester-queue.php", import.meta.url), "utf8"),
  readFile(new URL("validate-project-site.sh", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/assets/onboarding-wizard.js", import.meta.url), "utf8"),
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
assert(portal.includes("Applied") && portal.includes("Profile & Device") && portal.includes("Play Opt-In") && portal.includes("First-Use Smoke Test") && portal.includes("Active Assignment"), "Tester portal must render the five-stage evidence lifecycle.");
assert(!portal.includes("Agreement</li>"), "Agreement must remain in the application consent record rather than become a portal stage.");
assert(portal.includes("portalChatPoll") && portal.includes("chat_stream") && portal.includes("chat_poll"), "Tester Live Chat must offer SSE delivery with a polling fallback.");
assert(portal.includes("chatPostMessage($database, (int) $tester['id'], 'tester'"), "Tester Live Chat must bind submissions to the signed-in tester.");
assert(portal.includes("portalPage('Live Chat — ' . $coordinatorName") && queue.includes("renderPage('Live Chat — ' . (string) $selected['display_name']"), "Detached Live Chat titles must identify the other participant.");
assert(portal.includes("WHERE id = ? AND tester_id = ?"), "Feedback must be bound to the signed-in tester's own assignment.");
assert(portal.includes("play_opt_in_confirmed_at"), "Tester opt-in confirmation state is missing.");
assert(portal.includes("confirm_initial_smoke_test"), "Tester portal must provide an initial smoke-test self-confirmation.");
assert(portal.includes("This is my own confirmation, not automated installation or activity evidence."), "Smoke-test evidence must not be overstated.");
assert(portal.includes("function portalRequestPrivacyAction"), "Tester portal must provide a bounded withdrawal/deletion-request action.");
assert(portal.includes("request_privacy_action"), "Tester portal must route authenticated privacy requests.");
assert(portal.includes("name=\"confirm_privacy_request\""), "Tester privacy requests must require an explicit confirmation.");
assert(portal.includes("withdrawal_requested_at") && portal.includes("deletion_requested_at"), "Tester privacy request timestamps must be retained separately.");
assert(portal.includes("The coordinator will verify and process it under the retention policy."), "Tester portal must not claim that a recorded request is already completed.");
assert(portal.includes("FEEDBACK_CATEGORIES"), "Tester portal must use the queue's structured feedback categories.");
assert(queue.includes("function recordTesterPlayOptIn(PDO $database, int $testerId): bool"), "The lifecycle must persist the Play Opt-In evidence separately.");
assert(queue.includes("function recordTesterInitialSmokeTest(PDO $database, int $testerId): void"), "The lifecycle must persist the First-Use Smoke Test evidence separately.");
assert(portal.includes("recordTesterPlayOptIn($database, (int) $tester['id'])") && portal.includes("recordTesterInitialSmokeTest($database, (int) $tester['id'])"), "The tester portal must record both self-confirmations before it presents Ready for assignment.");
assert(portal.includes("PORTAL_TESTING_INTERESTS"), "Tester profile must include the original testing-interest intake options.");
assert(portal.includes("name=\"display_name\""), "Tester profile must allow a tester to update their name.");
assert(portal.includes("name=\"country\""), "Tester profile must allow a tester to update their country or region.");
assert(portal.includes("portalCheckboxes('other_stations'"), "Tester profile must allow a tester to update other familiar stations.");
assert(portal.includes("portalCheckboxes('testing_interests'"), "Tester profile must allow a tester to update testing interests.");
assert(portal.includes("name=\"experience\""), "Tester profile must allow a tester to update assignment notes or experience.");
assert(portal.includes("Email changes are reviewed by the coordinator"), "Tester email must remain a reviewed identity change.");
assert(portal.includes("Guest testing does not require a 24Seven.FM station account."), "Tester portal must explain that a station account is optional.");
assert(portal.includes("Existing station access (optional; station names only)"), "Tester portal must label existing station access as optional.");
assert(portal.includes("const PORTAL_REQUIRED_PROFILE_FIELDS"), "Tester profile must declare the fields required to save the intake.");
assert(portal.includes("profile_missing") && portal.includes("Complete the required fields marked with *"), "A rejected profile save must name every missing required field.");
assert(portal.includes("portalChoices('station_accounts', PORTAL_STATIONS + ['none' => 'None'])"), "Optional station access must not block a tester from saving their profile.");
assert(portal.includes("required-mark") && portal.includes("first.focus({preventScroll:true})"), "The tester portal must mark required profile fields and focus the first missing field.");
assert(portal.includes('/assets/onboarding-profile-form.js'), "Tester profile requirements and draft recovery must use the CSP-safe external form asset.");
assert(portal.includes("hash_file('sha256', $wizardAsset)") && portal.includes('/assets/onboarding-wizard.js?v='), "Tester onboarding must derive its wizard asset version from the deployed content.");
assert(portal.includes('/assets/onboarding-wizard.css'), "Tester onboarding must load the wizard presentation asset.");
assert(wizard.includes("Next →") && wizard.includes("← Back"), "The onboarding wizard must provide forward and back controls.");
assert(!wizard.includes("disabled = preview"), "Coordinator previews must allow read-only navigation through onboarding pages.");
assert(wizard.includes("form.querySelector(':scope > fieldset') ?? form") && wizard.includes("draftFields") && wizard.includes("coverageFields"), "The onboarding wizard must split a preview-wrapped profile into separate intake and device/coverage cards.");
assert(wizard.includes("readOnlyPreview") && wizard.includes("draftFields.disabled = true") && wizard.includes("coverageFields.disabled = true"), "Splitting coordinator previews must preserve their read-only field controls.");
assert(wizard.indexOf("if (profileComplete && !optInCard && !smokeCard) {") < wizard.indexOf("document.body.append(overlay);"), "Completed testers must bypass the onboarding modal before it is created.");
assert(wizard.includes("collapseCard(profileCard, 'Profile & Device'") && wizard.includes("collapseCard(cardWithHeading('Submit feedback for a focused task')") && wizard.includes("collapseCard(document.querySelector('[data-live-chat]')?.closest('.card')"), "Completed testers must see a task-first workspace with profile, reporting, and support opened on demand.");
assert(portal.includes("hash_file('sha256', $portalStyleAsset)") && portal.includes('/assets/onboarding-portal.css?v='), "Tester portal styles must be fingerprinted so workspace changes cannot be hidden behind a stale cache.");
assert(portal.includes("TESTER_PORTAL_TURNSTILE_ACTION"), "Magic-link requests must be protected by Turnstile.");
assert(portal.includes("portalVerifyTurnstile($turnstileToken"), "Turnstile must be verified before a magic link is sent.");
assert(portal.includes("We could not send a sign-in link. Please try again later."), "Portal link requests must report a failed transport instead of claiming delivery.");
assert(portal.includes("DELETE FROM tester_portal_tokens WHERE id = ?"), "A failed portal-link delivery must remove its unusable token.");
assert(portal.includes("$optInAction"), "Google Play opt-in action must be rendered after the profile form.");
assert(portal.includes("After finishing Profile &amp; Device"), "Google Play installation must explain that Profile & Device comes first.");
assert(portal.includes('id="intake-profile"') && portal.includes('Finish your tester profile'), "Tester profile completion must have a clear, addressable destination.");
assert(portal.includes('Finish Profile &amp; Continue to Install') && wizard.includes('Finish Profile & Continue to Install'), "Profile completion must name the next installation step in both fallback and guided flows.");
assert(portal.includes('Open Google Play to install Player') && portal.includes('Install the Player from Google Play'), "The first install action must be explicit and reachable after Profile & Device.");
assert(portal.includes('function portalAssignmentSteps') && portal.includes('Complete and report') && portal.includes('Report each PT case separately'), "Every assigned task must provide ordered setup, execution, safety, and reporting instructions.");
assert(portal.includes("'testerSteps'") && portal.includes("'expectedResult'"), "Task-specific execution steps and expected results must be rendered when the canonical task registry provides them.");
assert(portal.includes("function portalAdminPreviewTesterId"), "The coordinator must have an authenticated tester-view preview path.");
assert(portal.includes("($_SESSION['authenticated'] ?? false) === true"), "Tester previews must require the coordinator session.");
assert(portal.includes("Tester previews are read-only."), "Tester previews must reject state-changing requests.");
assert(portal.includes("Read-only coordinator preview."), "Tester previews must be visibly identified as read-only.");
assert(portal.includes('class="brand-icon" src="/assets/project/app-icon.png"'), "Tester portal branding must use the established Player icon.");
assert(!portal.includes('class="mark">7</span>'), "Tester portal must not use an invented numeric brand mark.");
assert(!portal.includes("password_verify("), "Tester portal must not collect or verify tester passwords.");
assert(queue.includes("CREATE TABLE IF NOT EXISTS tester_portal_tokens"), "Queue database migration must create portal token storage.");
assert(queue.includes("CREATE TABLE IF NOT EXISTS tester_feedback"), "Queue database migration must create feedback storage.");
assert(queue.includes("CREATE TABLE IF NOT EXISTS chat_threads") && queue.includes("CREATE TABLE IF NOT EXISTS chat_messages"), "Queue database migration must create isolated Live Chat storage.");
assert(queue.includes("recipient_role TEXT NOT NULL"), "Live Chat messages must persist both sender and recipient roles.");
assert(queue.includes("chat_submission_limits") && queue.includes("CHAT_RETENTION_DAYS"), "Live Chat must retain rate-limit and purge controls.");
assert(queue.includes("coordinatorChatPoll") && queue.includes("chatPostMessage($database, $testerId, 'coordinator'"), "Coordinator Live Chat must use the same protected thread boundary.");
assert(queue.includes("play_opt_in_confirmed_at"), "Queue database migration must retain tester opt-in confirmation.");
assert(queue.includes("function synchronizeOnboardingProfile"), "Profile & Device completion must be persisted in the onboarding lifecycle.");
assert(portal.includes("synchronizeOnboardingProfile($database, (int) $tester['id'])"), "Saving a tester profile must synchronize the five-stage lifecycle.");
assert(queue.includes("CREATE TABLE IF NOT EXISTS tester_profile_completion_notifications"), "Profile completion notifications must have one auditable record per tester.");
assert(queue.includes("function sendProfileCompletionNotification") && portal.includes("sendProfileCompletionNotification($database, $config, $updatedTester)"), "A tester's first complete profile save must notify the coordinator without exposing profile fields in the email.");
assert(queue.includes("profileCompletionNotificationRecipient") && queue.includes("$config['coordinator_email'] ?? $config['from_email']"), "Profile-completion notices must use the configured coordinator recipient without embedding an address in source.");
assert(queue.includes("withdrawal_requested_at") && queue.includes("deletion_requested_at"), "Queue database migration must retain tester privacy request timestamps.");
assert(queue.includes("Record-deletion request:"), "Coordinator workspace must surface tester deletion requests.");
assert(queue.includes("Tester task reports"), "Coordinator workspaces must display submitted tester reports.");
assert(queue.includes("Preview tester view"), "Coordinator roster and workspaces must link to the tester-view preview.");
assert(build.includes("test-tester-portal.mjs"), "Site validator must run the tester portal contract.");

console.log("Tester portal security and data-routing contract: valid.");
