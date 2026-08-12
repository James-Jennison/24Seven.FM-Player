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
assert(endpoint.includes("task_status TEXT NOT NULL DEFAULT \\'assigned\\'"), "Assignments must have a separate default status");
assert(endpoint.includes("task_status IN (\\'assigned\\', \\'in_progress\\', \\'complete\\', \\'blocked\\')"), "Assignment statuses are incomplete");
assert(endpoint.includes("if (($task['state'] ?? '') !== 'current')"), "Future task server-side block is missing");
assert(endpoint.includes("Future / Blocked Tester Tasks cannot be assigned."), "Future task rejection message is missing");
assert(endpoint.includes("INSERT INTO tester_task_assignments"), "Queue cannot create assignments");
assert(endpoint.includes("UPDATE tester_task_assignments SET"), "Queue cannot update assignments");
assert(endpoint.includes("DELETE FROM tester_task_assignments"), "Queue cannot remove assignments");
assert(endpoint.includes("STATION_SCOPES"), "Queue station scope persistence is missing");
assert(endpoint.includes("assignmentMessage(array $task, array $assignment)"), "Copy assignment server packet is missing");
assert(!/function assignmentMessage[\s\S]*?\$assignment\['email'\]/.test(endpoint), "Copy assignment must not include tester email");
assert(!/function assignmentMessage[\s\S]*?\$assignment\['display_name'\]/.test(endpoint), "Copy assignment must not include tester name");
assert(client.includes("data-assignment-copy"), "Copy assignment client action is missing");
assert(client.includes("data-task-assignment-form"), "Task preview client integration is missing");
assert(client.includes("checkbox.required = mode === 'required'"), "Required mutation authorization is not enforced in the UI");
assert(tasks.find((task) => task.id === "TT-09").safetyWarning.includes("ONE LIVE REQUEST MAXIMUM"), "TT-09 safety boundary is missing");
for (const id of ["TT-19", "TT-20"]) {
  const task = tasks.find((candidate) => candidate.id === id);
  assert(task.requiresConfiguration === true && task.mutation.mode === "required", `${id} must require a harness and explicit authorization`);
}
assert(endpoint.includes("requires the supplied harness or controlled configuration"), "Queue does not enforce the required security harness scope");
assert(tasks.find((task) => task.id === "TT-17").state === "future", "TT-17 must remain future / blocked");

console.log("Private Tester Queue task-assignment contract: valid.");
