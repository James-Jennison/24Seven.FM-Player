#!/usr/bin/env node

import { readFile } from "node:fs/promises";

const [registryText, endpoint, client] = await Promise.all([
  readFile(new URL("../privacy-site/_data/tester_tasks.json", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/private-tester-queue.php", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/assets/private-tester-queue.js", import.meta.url), "utf8"),
]);
const registry = JSON.parse(registryText);

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

const tasks = registry.tasks;
const current = tasks.filter((task) => task.state === "current");
const future = tasks.filter((task) => task.state === "future");
assert(current.length === 19 && future.length === 4, "Queue task availability must match the canonical registry");
assert(!endpoint.includes("email_batches (\n            id INTEGER PRIMARY KEY,\n            tester_id"), "Existing email persistence must remain separate from task assignments");
assert(endpoint.includes("CREATE TABLE IF NOT EXISTS tester_task_assignments"), "Queue must add the assignment table additively");
assert(endpoint.includes("CREATE TABLE IF NOT EXISTS tester_onboarding"), "Queue must persist onboarding separately from enrollment and assignments");
assert(endpoint.includes("initial_smoke_test_confirmed_at"), "Queue must retain the tester's initial smoke-test self-confirmation.");
assert(endpoint.includes("recruitment_source"), "Queue must retain validated recruitment-source attribution.");
assert(endpoint.includes("FEEDBACK_CATEGORIES"), "Queue must retain structured feedback categories.");
assert(endpoint.includes("CREATE TABLE IF NOT EXISTS tester_mail_archive"), "Queue must retain a protected coordinator mail archive");
assert(endpoint.includes("CREATE TABLE IF NOT EXISTS administrator_login_limits"), "Queue must persist private administrator-login rate limits.");
assert(endpoint.includes("function administratorLoginClientKey"), "Administrator login rate limiting must not persist a raw client address.");
assert(endpoint.includes("function administratorLoginNameAccepted"), "Administrator sign-in must require a configured login name.");
assert(endpoint.includes("function administratorAcceptedTotpStep"), "Administrator sign-in must require an authenticator-app code.");
assert(endpoint.includes("function consumeAdministratorTotpStep"), "Administrator authenticator codes must not be replayable.");
assert(endpoint.includes("hash_hmac('sha256', 'admin-login:'"), "Administrator login rate-limit identifiers must be keyed.");
assert(endpoint.includes("function administratorLoginRequestIsSameOrigin"), "Administrator login must validate its same-origin submission.");
assert(endpoint.includes("ADMIN_SESSION_IDLE_SECONDS"), "Administrator sessions must have an idle-expiry boundary.");
assert(endpoint.includes("ADMIN_SESSION_MAX_SECONDS"), "Administrator sessions must have an absolute-expiry boundary.");
assert(endpoint.includes("session.use_strict_mode"), "Administrator sessions must reject uninitialized session identifiers.");
assert(endpoint.includes("Content-Security-Policy"), "Private coordinator responses must set a content-security policy.");
assert(endpoint.includes("<h1>Administrator sign in</h1>"), "The private queue must render the hardened administrator sign-in page.");
assert(endpoint.includes('name="username"'), "The administrator sign-in page must collect a login name.");
assert(endpoint.includes('name="totp_code"'), "The administrator sign-in page must collect an authenticator-app code.");
assert(endpoint.includes("function prepareMailArchive(PDO $database"), "Queue must archive coordinator mail before handoff");
assert(endpoint.includes("function completeMailArchive(PDO $database"), "Queue must record the coordinator-mail handoff outcome");
assert(endpoint.includes("function renderMailArchive(PDO $database)"), "Queue must provide a coordinator mail-archive view");
assert(endpoint.includes("function renderMailRecord(PDO $database, int $archiveId)"), "Queue must provide an exact archived-message view");
assert(endpoint.includes("?mail_archive=1"), "Queue must link coordinators to the sent-mail archive");
assert(endpoint.includes("profile_pending', 'profile_complete', 'invited', 'orientation_sent', 'ready', 'paused"), "Onboarding lifecycle statuses are incomplete");
assert(endpoint.includes("function profileSummary(array $tester): array"), "Queue must evaluate assignment-relevant profile completeness");
assert(endpoint.includes("function isGuestTester(array $tester): bool"), "Queue must identify testers with no station account as guest testers.");
assert(endpoint.includes("function taskAllowsGuestTester(array $task): bool"), "Queue must use an explicit guest-task eligibility flag.");
assert(endpoint.includes("Guest testers can receive only the account-free Guest testing tasks."), "Queue must block account-dependent assignments for guest testers.");
assert(endpoint.includes("function renderOperationsDashboard"), "Queue must render an operations dashboard instead of every tester form at once");
assert(endpoint.includes("function renderTesterWorkspace"), "Queue must provide a focused per-tester workspace");
assert(endpoint.includes("send_onboarding_email"), "Queue must support an individual orientation-email action");
assert(endpoint.includes("This tester needs their profile update before an orientation email can be sent."), "Orientation must not bypass profile completion");
assert(endpoint.includes("orientation_email_attempts"), "Orientation-email handoff attempts must be auditable");
assert(endpoint.includes("task_status TEXT NOT NULL DEFAULT \\'assigned\\'"), "Assignments must have a separate default status");
assert(endpoint.includes("task_status IN (\\'assigned\\', \\'in_progress\\', \\'complete\\', \\'blocked\\')"), "Assignment statuses are incomplete");
assert(endpoint.includes("if (($task['state'] ?? '') !== 'current')"), "Future task server-side block is missing");
assert(endpoint.includes("Future / Blocked Tester Tasks cannot be assigned."), "Future task rejection message is missing");
assert(endpoint.includes("INSERT INTO tester_task_assignments"), "Queue cannot create assignments");
assert(endpoint.includes("UPDATE tester_task_assignments SET"), "Queue cannot update assignments");
assert(endpoint.includes("DELETE FROM tester_task_assignments"), "Queue cannot remove assignments");
assert(endpoint.includes("assignment_email_status"), "Assignment email handoff status is not persisted");
assert(endpoint.includes("assignment_email_attempts"), "Assignment email attempts are not persisted");
assert(endpoint.includes("sendAssignmentEmail(PDO $database, array $config, int $assignmentId)"), "Automatic assignment email is missing");
assert(endpoint.includes("$accepted = sendAssignmentEmail($database, $config, $assignmentId);"), "New task assignment does not send its email");
assert(endpoint.includes("resend_assignment_email"), "Explicit assignment-email resend is missing");
assert(endpoint.includes("MAIL_SUBMISSION_HOST"), "Assignment email must use the existing private SMTP transport");
assert(endpoint.includes("STATION_SCOPES"), "Queue station scope persistence is missing");
assert(endpoint.includes("assignmentMessage(array $task, array $assignment)"), "Copy assignment server packet is missing");
const assignmentMessageSource = endpoint.split("function assignmentMessage")[1].split("function assignmentEmailSubject")[0];
assert(!assignmentMessageSource.includes("$assignment['email']"), "Copy assignment must not include tester email");
assert(!assignmentMessageSource.includes("$assignment['display_name']"), "Copy assignment must not include tester name");
assert(client.includes("data-assignment-copy"), "Copy assignment client action is missing");
assert(client.includes("data-task-assignment-form"), "Task preview client integration is missing");
assert(client.includes("'profile-update'"), "Profile-update email template is missing");
assert(client.includes("checkbox.required = mode === 'required'"), "Required mutation authorization is not enforced in the UI");
assert(tasks.find((task) => task.id === "TT-09").safetyWarning.includes("ONE LIVE REQUEST MAXIMUM"), "TT-09 safety boundary is missing");
for (const id of ["TT-01", "TT-02", "TT-03", "TT-04", "TT-05", "TT-06", "TT-14", "TT-15", "TT-16"]) {
  assert(tasks.find((task) => task.id === id).guestEligible === true, `${id} must be explicitly available to guest testers`);
}
assert(tasks.find((task) => task.id === "TT-07").guestEligible !== true, "Account-dependent work must remain unavailable to guest testers");
for (const id of ["TT-19", "TT-20"]) {
  const task = tasks.find((candidate) => candidate.id === id);
  assert(task.requiresConfiguration === true && task.mutation.mode === "required", `${id} must require a harness and explicit authorization`);
}
assert(endpoint.includes("requires the supplied harness or controlled configuration"), "Queue does not enforce the required security harness scope");
assert(tasks.find((task) => task.id === "TT-17").state === "future", "TT-17 must remain future / blocked");

console.log("Private Tester Queue task-assignment contract: valid.");
