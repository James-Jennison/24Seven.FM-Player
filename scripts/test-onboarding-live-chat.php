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
    $legacy->exec("CREATE TABLE chat_threads (id INTEGER PRIMARY KEY, tester_id INTEGER NOT NULL UNIQUE, tester_deleted_at TEXT, coordinator_deleted_at TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, last_message_at TEXT);
        CREATE TABLE chat_messages (id INTEGER PRIMARY KEY, thread_id INTEGER NOT NULL, sender_role TEXT NOT NULL, body TEXT NOT NULL, created_at TEXT NOT NULL, read_at TEXT, tester_deleted_at TEXT, coordinator_deleted_at TEXT);
        INSERT INTO chat_threads(id, tester_id, created_at, updated_at) VALUES (1, 1, '2026-08-15T00:00:00Z', '2026-08-15T00:00:00Z');
        INSERT INTO chat_messages(id, thread_id, sender_role, body, created_at) VALUES (1, 1, 'tester', 'legacy message', '2026-08-15T00:00:00Z');");
    unset($legacy);
    $migrated = database(['database_path' => $legacyPath]);
    expectChat((string) $migrated->query('SELECT recipient_role FROM chat_messages WHERE id = 1')->fetchColumn() === 'coordinator', 'Existing chat records must receive the correct recipient role during migration.');
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
