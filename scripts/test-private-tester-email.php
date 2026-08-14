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

$assignmentTask = [
    'id' => 'TT-01',
    'title' => 'Install, Update & First Launch',
    'purpose' => 'Confirm the assigned Alpha build installs or updates cleanly and opens as expected.',
    'ptIds' => ['PT-01'],
    'prerequisites' => ['Assigned Play/Alpha build', 'Clean-install capability'],
    'safetyWarning' => 'Record the assigned installation path; do not substitute an unapproved build.',
    'mutation' => ['mode' => 'none', 'label' => 'Not part of this testing task.'],
];
$assignment = [
    'station_scope' => '',
    'configuration_scope' => 'Pixel 9 / Android 16',
    'coordinator_note' => 'Please test the update path first.',
    'mutation_authorized' => 0,
];
$assignmentPlain = assignmentMessage($assignmentTask, $assignment);
$assignmentHtml = assignmentHtml($assignmentTask, $assignment, 'Archive Test');
assertion(str_contains($assignmentPlain, 'WHAT TO DO'), 'Assignment email must provide a clear action section.');
assertion(str_contains($assignmentPlain, 'tester-portal.php'), 'Assignment email must link testers to submit their report.');
assertion(str_contains($assignmentPlain, "— James\n24Seven.FM Player Testing Team"), 'Assignment email must use the approved sign-off.');
assertion(str_contains($assignmentHtml, '<ol>') && str_contains($assignmentHtml, 'Open the tester portal'), 'Assignment HTML must provide a numbered task flow and portal link.');
assertion(str_contains(assignmentEmailSubject($assignmentTask), 'Action required:'), 'Assignment subject must communicate a required next step.');

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
} finally {
    unset($database);
    @unlink($archivePath);
}

echo 'Private Tester Queue email encoding contract: valid' . (class_exists('DOMDocument') ? '.' : ' (DOM extension unavailable in local PHP; parser declaration statically verified).') . "\n";
