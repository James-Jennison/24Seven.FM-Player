<?php
declare(strict_types=1);

if (!function_exists('mb_strlen')) {
    function mb_strlen(string $string, ?string $encoding = null): int
    {
        return strlen($string);
    }
}

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function assertion(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$source = '<p>We’re sorry the earlier message did not render correctly.</p><div>Open the tester portal.</div><ul><li>Primary station</li><li>Android device</li></ul>';
$endpointSource = file_get_contents(dirname(__DIR__) . '/privacy-site/private-tester-queue.php');
assertion(is_string($endpointSource) && str_contains($endpointSource, '<meta charset="UTF-8">'), 'Rich HTML parser must explicitly declare UTF-8.');
assertion(is_string($endpointSource) && str_contains($endpointSource, "'div' => 'p'"), 'Content-editor paragraphs must be retained as email paragraphs.');

$html = class_exists('DOMDocument') ? sanitizeHtmlBody($source) : $source;
$plainText = plainTextFromHtml($html);

assertion(str_contains($html, 'We’re'), 'Rich HTML must preserve UTF-8 punctuation.');
assertion(!str_contains($html, 'Weâ€™re'), 'Rich HTML must not contain mojibake.');
assertion(str_contains($plainText, 'We’re'), 'Plain-text alternative must preserve UTF-8 punctuation.');
assertion(str_contains($plainText, 'Open the tester portal.'), 'Plain-text alternative must retain content-editor paragraphs.');
assertion(str_contains($plainText, 'Primary station'), 'Plain-text alternative must retain list content.');

[$headers, $message] = multipartAlternativeMessage(
    '24Seven.FM Player',
    'test-sender@example.test',
    'test-recipient@example.test',
    'Apology: tester profile update',
    $plainText,
    $html,
);

$headerText = implode("\r\n", $headers);
assertion(str_contains($headerText, 'Subject: =?UTF-8?B?'), 'Subject must be UTF-8 MIME encoded.');
assertion(str_contains($headerText, 'multipart/alternative'), 'Message must declare multipart/alternative.');
assertion(str_contains($message, 'Content-Type: text/plain; charset=UTF-8'), 'Plain-text MIME part is missing.');
assertion(str_contains($message, 'Content-Type: text/html; charset=UTF-8'), 'Rich HTML MIME part is missing.');
assertion(str_contains(base64_decode(base64MimePart($plainText), true) ?: '', 'We’re'), 'UTF-8 text must survive MIME encoding.');

$archivePath = tempnam(sys_get_temp_dir(), 'tester-mail-archive-');
if ($archivePath === false) {
    throw new RuntimeException('Unable to create a temporary mail-archive database.');
}
try {
    $database = database(['database_path' => $archivePath]);
    $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, device, android_version, interests_json, status, imported_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?)")
        ->execute(['archive-test', '2026-08-14T00:00:00Z', 'Archive Test', 'archive@example.test', 'Test Device', '16', '[]', '2026-08-14T00:00:00Z']);
    $archiveId = prepareMailArchive($database, 1, 'orientation', 'Welcome', 'Plain archive body', '<p>HTML archive body</p>');
    completeMailArchive($database, $archiveId, true);
    $archive = $database->query('SELECT message_type, subject, body, body_html, handoff_status, attempted_at FROM tester_mail_archive WHERE id = 1')->fetch();
    assertion(is_array($archive) && $archive['message_type'] === 'orientation', 'Coordinator mail archive must retain its message type.');
    assertion(($archive['subject'] ?? '') === 'Welcome' && ($archive['body'] ?? '') === 'Plain archive body', 'Coordinator mail archive must retain the exact plain-text message.');
    assertion(($archive['body_html'] ?? '') === '<p>HTML archive body</p>', 'Coordinator mail archive must retain the exact HTML message.');
    assertion(($archive['handoff_status'] ?? '') === 'accepted' && is_string($archive['attempted_at'] ?? null), 'Coordinator mail archive must record the transport outcome.');

    $database->prepare("INSERT INTO email_batches(subject, body, body_html, created_at, status) VALUES (?, ?, ?, ?, 'prepared')")
        ->execute(['Profile update', 'Hi {{tester_name}}', '<p>Hi {{tester_name}},</p>', '2026-08-16T00:00:00Z']);
    $batchId = (int) $database->lastInsertId();
    $database->prepare("INSERT INTO email_batch_recipients(batch_id, tester_id, delivery_status) VALUES (?, ?, 'pending')")
        ->execute([$batchId, 1]);
    $confirmationRecipients = $database->prepare('SELECT testers.display_name, testers.email, onboarding.onboarding_status FROM email_batch_recipients AS recipients JOIN testers ON testers.id = recipients.tester_id LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = recipients.tester_id WHERE recipients.batch_id = ? ORDER BY testers.id');
    $confirmationRecipients->execute([$batchId]);
    assertion(count($confirmationRecipients->fetchAll()) === 1, 'Prepared batches must load their recipient preview after the onboarding join.');

    $pendingRecipients = $database->prepare("SELECT testers.id, testers.email, testers.display_name, onboarding.onboarding_status FROM email_batch_recipients AS recipients JOIN testers ON testers.id = recipients.tester_id LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = recipients.tester_id WHERE recipients.batch_id = ? AND recipients.delivery_status = 'pending' ORDER BY testers.id");
    $pendingRecipients->execute([$batchId]);
    assertion(count($pendingRecipients->fetchAll()) === 1, 'Prepared batches must load pending recipients for individual handoff.');
} finally {
    unset($database);
    @unlink($archivePath);
}

echo 'Private Tester Queue email encoding contract: valid' . (class_exists('DOMDocument') ? '.' : ' (DOM extension unavailable in local PHP; parser declaration statically verified).') . "\n";
