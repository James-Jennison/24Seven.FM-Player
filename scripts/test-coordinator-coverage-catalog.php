#!/usr/bin/env php
<?php
declare(strict_types=1);

if (!extension_loaded('pdo_sqlite')) {
    fwrite(STDOUT, "Coordinator Coverage Catalog contract: skipped (pdo_sqlite unavailable).\n");
    exit(0);
}

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function expectCoverageCatalog(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

$path = tempnam(sys_get_temp_dir(), 'coverage-catalog-');
if ($path === false) throw new RuntimeException('Unable to create isolated coverage-catalog storage.');

try {
    $database = database(['database_path' => $path]);
    $insert = $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, device, android_version, interests_json, status, imported_at, primary_station, device_form_factor, network_capabilities_json, audio_capabilities_json, accessibility_capabilities_json, testing_comfort, controlled_actions_json, testing_availability) VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    $insert->execute(['coverage-ready', '2026-08-23T00:00:00Z', 'Coverage Ready', 'coverage-ready@example.test', 'Catalog Phone', 'Android 16', '["playback"]', '2026-08-23T00:00:00Z', 'sst', 'foldable', '["wifi","network_handoff"]', '["bluetooth_headphones"]', '["talkback"]', 'readonly', '["none"]', '1_2h']);
    $insert->execute(['coverage-pending', '2026-08-23T00:01:00Z', 'Coverage Pending', 'coverage-pending@example.test', 'Catalog Tablet', 'Android 15', '[]', '2026-08-23T00:01:00Z', '', '', '[]', '[]', '[]', '', '[]', '']);

    markApplicationReviewed($database, 1);
    synchronizeOnboardingProfile($database, 1);
    recordTesterPlayOptIn($database, 1);
    recordTesterInitialSmokeTest($database, 1);

    $assignmentCountBefore = (int) $database->query('SELECT COUNT(*) FROM tester_task_assignments')->fetchColumn();
    $catalog = coordinatorCoverageCatalog($database);
    $assignmentCountAfter = (int) $database->query('SELECT COUNT(*) FROM tester_task_assignments')->fetchColumn();
    expectCoverageCatalog($assignmentCountBefore === 0 && $assignmentCountAfter === 0, 'The Coverage Catalog must not create assignments.');
    expectCoverageCatalog($catalog['metrics']['active'] === 2, 'The Coverage Catalog must count only active Testers.');
    expectCoverageCatalog($catalog['metrics']['profile_complete'] === 1 && $catalog['metrics']['ready'] === 1 && $catalog['metrics']['available'] === 1, 'Catalog readiness and capacity must derive from existing onboarding evidence.');
    expectCoverageCatalog(($catalog['dimensions']['android_version']['Android 16']['count'] ?? 0) === 1, 'Android-version coverage must aggregate the recorded device profile.');
    expectCoverageCatalog(($catalog['dimensions']['device_form_factor']['foldable']['label'] ?? '') === 'Foldable / flip phone', 'Form-factor coverage must remain human readable.');
    expectCoverageCatalog(($catalog['dimensions']['primary_station']['sst']['label'] ?? '') === 'StreamingSoundtracks.com', 'Station coverage must remain human readable.');
    expectCoverageCatalog(($catalog['dimensions']['network_capabilities_json']['network_handoff']['count'] ?? 0) === 1, 'Network coverage must preserve the selected capability.');
    expectCoverageCatalog(($catalog['dimensions']['audio_capabilities_json']['bluetooth_headphones']['count'] ?? 0) === 1, 'Audio coverage must preserve the selected capability.');
    expectCoverageCatalog(($catalog['dimensions']['accessibility_capabilities_json']['talkback']['count'] ?? 0) === 1, 'Accessibility coverage must preserve the selected capability.');
    expectCoverageCatalog(($catalog['dimensions']['android_version']['Android 16']['testers'][0]['available'] ?? false) === true, 'Catalog entries must retain only safe existing-record routing state.');

    fwrite(STDOUT, "Coordinator Coverage Catalog contract: valid.\n");
} finally {
    unset($database);
    @unlink($path);
}
