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
