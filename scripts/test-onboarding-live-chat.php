#!/usr/bin/env php
<?php
declare(strict_types=1);

if (!extension_loaded('pdo_sqlite')) {
    fwrite(STDOUT, "Onboarding Live Chat contract: skipped (pdo_sqlite unavailable).\n");
    exit(0);
}

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function expectChat(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

$legacyPath = tempnam(sys_get_temp_dir(), 'onboarding-live-chat-legacy-');
if ($legacyPath === false) throw new RuntimeException('Unable to create legacy chat storage.');
try {
    $legacy = new PDO('sqlite:' . $legacyPath, null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
    $legacy->exec("CREATE TABLE testers (id INTEGER PRIMARY KEY, source_message_uid TEXT NOT NULL UNIQUE, received_at TEXT NOT NULL, display_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, country TEXT, device TEXT NOT NULL, android_version TEXT NOT NULL, interests_json TEXT NOT NULL, experience TEXT, status TEXT NOT NULL, imported_at TEXT NOT NULL);
        CREATE TABLE tester_onboarding (tester_id INTEGER PRIMARY KEY, onboarding_status TEXT NOT NULL CHECK(onboarding_status IN ('profile_pending', 'profile_complete', 'invited', 'orientation_sent', 'ready', 'paused')), coordinator_note TEXT NOT NULL DEFAULT '', orientation_email_status TEXT NOT NULL DEFAULT 'not_sent', orientation_email_attempted_at TEXT, orientation_email_attempts INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL);
        INSERT INTO testers(id, source_message_uid, received_at, display_name, email, device, android_version, interests_json, status, imported_at) VALUES (1, 'legacy-tester', '2026-08-15T00:00:00Z', 'Legacy Tester', 'legacy@example.test', 'Test device', 'Android 16', '[]', 'active', '2026-08-15T00:00:00Z');
        INSERT INTO tester_onboarding(tester_id, onboarding_status, updated_at) VALUES (1, 'orientation_sent', '2026-08-15T00:00:00Z');
        CREATE TABLE chat_threads (id INTEGER PRIMARY KEY, tester_id INTEGER NOT NULL UNIQUE, tester_deleted_at TEXT, coordinator_deleted_at TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, last_message_at TEXT);
        CREATE TABLE chat_messages (id INTEGER PRIMARY KEY, thread_id INTEGER NOT NULL, sender_role TEXT NOT NULL, body TEXT NOT NULL, created_at TEXT NOT NULL, read_at TEXT, tester_deleted_at TEXT, coordinator_deleted_at TEXT);
        INSERT INTO chat_threads(id, tester_id, created_at, updated_at) VALUES (1, 1, '2026-08-15T00:00:00Z', '2026-08-15T00:00:00Z');
        INSERT INTO chat_messages(id, thread_id, sender_role, body, created_at) VALUES (1, 1, 'tester', 'legacy message', '2026-08-15T00:00:00Z');");
    unset($legacy);
    $migrated = database(['database_path' => $legacyPath]);
    expectChat((string) $migrated->query('SELECT recipient_role FROM chat_messages WHERE id = 1')->fetchColumn() === 'coordinator', 'Existing chat records must receive the correct recipient role during migration.');
    expectChat((string) $migrated->query('SELECT onboarding_status FROM tester_onboarding WHERE tester_id = 1')->fetchColumn() === 'profile_complete', 'Legacy orientation status must migrate to a non-blocking mail event while preserving the evidence lifecycle.');
    unset($migrated);
} finally {
    @unlink($legacyPath);
}

$path = tempnam(sys_get_temp_dir(), 'onboarding-live-chat-');
if ($path === false) throw new RuntimeException('Unable to create temporary chat storage.');
try {
    $database = database(['database_path' => $path]);
    $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, device, android_version, interests_json, status, imported_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?)")
        ->execute(['chat-contract', '2026-08-15T00:00:00Z', 'Example Tester', 'tester@example.test', 'Test device', 'Android 16', '[]', '2026-08-15T00:00:00Z']);
    $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, device, android_version, interests_json, status, imported_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?)")
        ->execute(['chat-contract-second', '2026-08-15T00:00:01Z', 'Second Tester', 'second@example.test', 'Second test device', 'Android 16', '[]', '2026-08-15T00:00:01Z']);

    expectChat(synchronizeOnboardingProfile($database, 1) === 'profile_pending', 'An incomplete profile must remain at the Profile & Device stage.');
    try {
        recordTesterPlayOptIn($database, 1);
        throw new RuntimeException('An incomplete profile must not record Play Opt-In.');
    } catch (InvalidArgumentException) {
        // Expected: the second lifecycle stage is required before opt-in evidence.
    }
    $database->prepare("UPDATE testers SET primary_station = 'sst', device_form_factor = 'phone', network_capabilities_json = '[\"wifi\"]', audio_capabilities_json = '[\"device_speaker\"]', accessibility_capabilities_json = '[\"general_accessibility\"]', testing_comfort = 'readonly', controlled_actions_json = '[\"none\"]', testing_availability = '1_2h' WHERE id = 1")->execute();
    expectChat(synchronizeOnboardingProfile($database, 1) === 'profile_complete', 'A completed tester profile must advance the persisted Profile & Device stage.');
    expectChat($database->query('SELECT onboarding_status FROM tester_onboarding WHERE tester_id = 1')->fetchColumn() === 'profile_complete', 'The profile-complete state must persist independently of invitation or readiness.');
    $database->prepare("UPDATE testers SET testing_availability = NULL WHERE id = 1")->execute();
    expectChat(synchronizeOnboardingProfile($database, 1) === 'profile_pending', 'Removing required coverage must return the lifecycle to Profile & Device pending.');
    $database->prepare("UPDATE testers SET testing_availability = '1_2h' WHERE id = 1")->execute();
    synchronizeOnboardingProfile($database, 1);
    try {
        recordTesterInitialSmokeTest($database, 1);
        throw new RuntimeException('A smoke-test record requires an earlier Play Opt-In.');
    } catch (InvalidArgumentException) {
        // Expected: the self-confirmations must occur in their lifecycle order.
    }
    expectChat(recordTesterPlayOptIn($database, 1) === false, 'Play opt-in alone must not mark a tester ready.');
    expectChat($database->query('SELECT onboarding_status FROM tester_onboarding WHERE tester_id = 1')->fetchColumn() === 'profile_complete', 'Play opt-in must preserve a distinct pre-smoke onboarding state.');
    recordTesterInitialSmokeTest($database, 1);
    $readiness = $database->query('SELECT onboarding_status, play_opt_in_confirmed_at, initial_smoke_test_confirmed_at FROM tester_onboarding WHERE tester_id = 1')->fetch();
    expectChat($readiness['onboarding_status'] === 'ready' && $readiness['play_opt_in_confirmed_at'] !== '' && $readiness['initial_smoke_test_confirmed_at'] !== '', 'Only both self-confirmations may automatically set Ready.');
    expectChat(synchronizeOnboardingProfile($database, 1) === 'ready', 'Refreshing a complete profile must preserve the Ready lifecycle state.');
    $database->prepare('UPDATE testers SET testing_availability = NULL WHERE id = 1')->execute();
    expectChat(synchronizeOnboardingProfile($database, 1) === 'profile_pending', 'Removing required profile evidence must revoke Ready until the profile is restored.');
    $database->prepare("UPDATE testers SET testing_availability = '1_2h' WHERE id = 1")->execute();
    expectChat(synchronizeOnboardingProfile($database, 1) === 'ready', 'Restoring required profile evidence must restore Ready when both recorded self-confirmations remain valid.');

    $testerMessage = chatPostMessage($database, 1, 'tester', 'I completed the first-use check.');
    $coordinatorMessage = chatPostMessage($database, 1, 'coordinator', 'Thanks. Your focused assignment is available.');
    expectChat($testerMessage > 0 && $coordinatorMessage > $testerMessage, 'Messages must receive ordered identifiers.');

    $testerView = chatMessages($database, 1, 'tester');
    $coordinatorView = chatMessages($database, 1, 'coordinator');
    expectChat(count($testerView) === 2 && count($coordinatorView) === 2, 'Both authorized roles must see the single tester thread.');
    expectChat($testerView[1]['sender_role'] === 'coordinator', 'Tester view must preserve the coordinator sender role.');
    expectChat($testerView[0]['recipient_role'] === 'coordinator' && $testerView[1]['recipient_role'] === 'tester', 'Each message must persist the intended recipient role.');
    expectChat(chatMessages($database, 2, 'tester') === [], 'A tester thread must never expose another tester’s messages.');
    expectChat((string) $database->query('SELECT read_at FROM chat_messages WHERE id = ' . $coordinatorMessage)->fetchColumn() !== '', 'Opening a tester thread must mark coordinator messages read.');

    try {
        chatPostMessage($database, 1, 'untrusted', 'Invalid sender role.');
        throw new RuntimeException('Chat sender roles must be constrained.');
    } catch (InvalidArgumentException) {
        // Expected: only the portal-tested roles may submit messages.
    }

    chatSoftDeleteMessage($database, 1, 'tester', $testerMessage);
    expectChat(count(chatMessages($database, 1, 'tester')) === 1, 'Tester soft deletion must hide only the tester view.');
    expectChat(count(chatMessages($database, 1, 'coordinator')) === 2, 'Tester soft deletion must not hide the coordinator view.');

    for ($index = 0; $index < CHAT_SUBMISSION_MAXIMUM - 1; $index++) chatPostMessage($database, 1, 'coordinator', 'Rate-limit contract ' . $index);
    try {
        chatPostMessage($database, 1, 'coordinator', 'This exceeds the bounded coordinator rate.');
        throw new RuntimeException('Chat submissions must be rate limited.');
    } catch (InvalidArgumentException) {
        // Expected: the shared thread limit is role-specific and bounded.
    }

    $database->prepare("UPDATE chat_messages SET created_at = '2000-01-01T00:00:00Z' WHERE id = ?")->execute([$coordinatorMessage]);
    chatPurgeExpired($database);
    expectChat((int) $database->query('SELECT COUNT(*) FROM chat_messages WHERE id = ' . $coordinatorMessage)->fetchColumn() === 0, 'Retention purge must remove expired chat records.');
    fwrite(STDOUT, "Onboarding Live Chat contract: valid.\n");
} finally {
    unset($database);
    @unlink($path);
}
