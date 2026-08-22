#!/usr/bin/env php
<?php
declare(strict_types=1);

if (!extension_loaded('pdo_sqlite')) {
    fwrite(STDOUT, "Portal activity timeline smoke: skipped (pdo_sqlite unavailable).\n");
    exit(0);
}

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function expectActivityTimeline(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

$path = tempnam(sys_get_temp_dir(), 'portal-activity-timeline-');
if ($path === false) throw new RuntimeException('Unable to create isolated activity-timeline storage.');

try {
    $database = database(['database_path' => $path]);
    $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, device, android_version, interests_json, status, imported_at, primary_station, device_form_factor, network_capabilities_json, audio_capabilities_json, accessibility_capabilities_json, testing_comfort, controlled_actions_json, testing_availability) VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        ->execute(['activity-timeline-local', '2026-08-22T17:00:00Z', 'Timeline Tester', 'timeline@example.test', 'Timeline device', 'Android 16', '["playback"]', '2026-08-22T17:00:00Z', 'sst', 'phone', '["wifi"]', '["device_speaker"]', '["general_accessibility"]', 'readonly', '["none"]', '1_2h']);

    markApplicationReviewed($database, 1);
    synchronizeOnboardingProfile($database, 1);
    recordTesterPlayOptIn($database, 1);
    recordTesterInitialSmokeTest($database, 1);
    $tester = $database->query('SELECT testers.*, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at FROM testers JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = 1')->fetch();
    expectActivityTimeline(is_array($tester), 'The isolated tester record must be available to the activity timeline.');

    $tasks = taskRegistry();
    $task = $tasks['TT-01'] ?? null;
    expectActivityTimeline(is_array($task), 'The validated task registry must include TT-01 for the activity timeline smoke.');
    $now = '2026-08-22T17:10:00Z';
    $database->prepare("INSERT INTO tester_task_assignments(tester_id, task_id, task_status, station_scope, configuration_scope, coordinator_note, mutation_authorized, submitted_for_review_at, created_at, updated_at) VALUES (1, ?, 'complete', 'StreamingSoundtracks.com', 'Timeline device', 'Private coordinator note', 0, ?, ?, ?)")
        ->execute([$task['id'], $now, $now, $now]);
    $assignmentId = (int) $database->lastInsertId();
    $case = (string) ($task['ptIds'][0] ?? 'PT-01');
    $database->prepare("INSERT INTO tester_feedback(tester_id, assignment_id, subject, details, outcome, category, pt_case, created_at) VALUES (1, ?, 'Private report subject', 'Private report details', 'pass', 'playback', ?, ?)")
        ->execute([$assignmentId, $case, $now]);
    $database->prepare("INSERT INTO tester_mail_archive(tester_id, message_type, subject, body, body_html, handoff_status, prepared_at, attempted_at) VALUES (1, 'assignment', 'Private assignment subject', 'Private message body', '<p>Private message body</p>', 'accepted', ?, ?)")
        ->execute([$now, $now]);

    $assignments = $database->query('SELECT * FROM tester_task_assignments WHERE tester_id = 1')->fetchAll();
    $html = testerActivityTimeline($database, $tester, $assignments, $tasks, 'tester');
    expectActivityTimeline(str_contains($html, 'Application received') && str_contains($html, 'Google Play opt-in self-confirmed') && str_contains($html, 'Initial smoke test self-confirmed'), 'The timeline must include the recorded onboarding evidence.');
    expectActivityTimeline(str_contains($html, e($case) . ' result recorded') && str_contains($html, 'Task submitted for Coordinator review') && str_contains($html, 'Coordinator recorded final task status'), 'The timeline must include assignment, report, and Coordinator decision evidence.');
    expectActivityTimeline(str_contains($html, 'Task-assignment email handoff') && str_contains($html, 'does not prove inbox delivery or reading'), 'The timeline must distinguish transport acceptance from inbox delivery.');
    expectActivityTimeline(!str_contains($html, 'Private coordinator note') && !str_contains($html, 'Private report subject') && !str_contains($html, 'Private report details') && !str_contains($html, 'Private assignment subject') && !str_contains($html, 'Private message body'), 'The timeline must not expose private notes, report contents, or email contents.');

    fwrite(STDOUT, "Portal activity timeline smoke: valid.\n");
} finally {
    unset($database);
    @unlink($path);
}
