#!/usr/bin/env php
<?php
declare(strict_types=1);

if (!extension_loaded('pdo_sqlite')) {
    fwrite(STDOUT, "Onboarding Phase 2 local staging smoke: skipped (pdo_sqlite unavailable).\n");
    exit(0);
}

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function expectPhaseTwo(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

$path = tempnam(sys_get_temp_dir(), 'onboarding-phase2-local-');
if ($path === false) throw new RuntimeException('Unable to create isolated local staging storage.');

try {
    $database = database(['database_path' => $path]);
    $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, device, android_version, interests_json, status, imported_at, primary_station, device_form_factor, network_capabilities_json, audio_capabilities_json, accessibility_capabilities_json, testing_comfort, controlled_actions_json, testing_availability) VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        ->execute(['phase-two-local', '2026-08-15T00:00:00Z', 'Local Stage Tester', 'local-stage@example.test', 'Local stage device', 'Android 16', '["playback"]', '2026-08-15T00:00:00Z', 'sst', 'phone', '["wifi"]', '["device_speaker"]', '["general_accessibility"]', 'readonly', '["none"]', '1_2h']);

    markApplicationReviewed($database, 1);
    $tester = $database->query('SELECT testers.*, onboarding.onboarding_status, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at, onboarding.reviewed_at FROM testers JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = 1')->fetch();
    expectPhaseTwo(is_array($tester) && applicationStage($tester) === 'accepted', 'A reviewed local tester must remain pre-assignment until their evidence is complete.');
    expectPhaseTwo(!testerReadyForAssignment($tester), 'Task assignment must be blocked before the two tester self-confirmations.');

    synchronizeOnboardingProfile($database, 1);
    expectPhaseTwo(recordTesterPlayOptIn($database, 1) === false, 'Play Opt-In alone must not unlock local task assignment.');
    recordTesterInitialSmokeTest($database, 1);
    $tester = $database->query('SELECT testers.*, onboarding.onboarding_status, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at, onboarding.reviewed_at FROM testers JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = 1')->fetch();
    expectPhaseTwo(is_array($tester) && testerReadyForAssignment($tester) && applicationStage($tester) === 'active', 'Both self-confirmations must unlock focused assignment in the isolated staging database.');

    $_POST = ['task_id' => 'TT-01', 'station_scope' => 'StreamingSoundtracks.com', 'configuration_scope' => 'Local stage device', 'coordinator_note' => 'Local-only smoke assignment'];
    [$task, $station, $configuration, $note, $mutationAuthorized] = assignmentInput(taskRegistry(), $tester);
    $now = gmdate('c');
    $database->prepare("INSERT INTO tester_task_assignments(tester_id, task_id, task_status, station_scope, configuration_scope, coordinator_note, mutation_authorized, created_at, updated_at) VALUES (1, ?, 'assigned', ?, ?, ?, ?, ?, ?)")
        ->execute([$task['id'], $station, $configuration, $note, $mutationAuthorized, $now, $now]);
    $assignmentId = (int) $database->lastInsertId();
    expectPhaseTwo($assignmentId > 0, 'A ready local tester must receive a focused task assignment.');

    $html = mergeCoordinatorMailHtml('<p>Hello {{tester_name}}, {{onboarding_status}}.</p>', $tester);
    $archiveId = prepareMailArchive($database, 1, 'assignment', assignmentEmailSubject($task), assignmentMessage($task, ['station_scope' => $station, 'configuration_scope' => $configuration, 'coordinator_note' => $note, 'mutation_authorized' => $mutationAuthorized]), $html, null, $assignmentId);
    expectPhaseTwo($archiveId > 0 && (string) $database->query('SELECT handoff_status FROM tester_mail_archive WHERE id = ' . $archiveId)->fetchColumn() === 'prepared', 'The local composer smoke must preserve a reviewed, unsent archive record.');

    $messageId = chatPostMessage($database, 1, 'coordinator', 'Local staging chat smoke.');
    expectPhaseTwo(count(chatMessages($database, 1, 'tester')) === 1, 'The local tester view must receive only its own staged conversation.');
    $database->prepare("UPDATE chat_messages SET created_at = '2000-01-01T00:00:00Z' WHERE id = ?")->execute([$messageId]);
    $database->prepare("UPDATE chat_threads SET last_message_at = '2000-01-01T00:00:00Z' WHERE tester_id = 1")->execute();
    chatPurgeExpired($database);
    expectPhaseTwo((int) $database->query('SELECT COUNT(*) FROM chat_messages')->fetchColumn() === 0 && (int) $database->query('SELECT COUNT(*) FROM chat_threads')->fetchColumn() === 0, 'The local retention job must purge expired staged messages and their empty thread.');

    fwrite(STDOUT, "Onboarding Phase 2 local staging smoke: valid.\n");
} finally {
    $_POST = [];
    unset($database);
    @unlink($path);
}
