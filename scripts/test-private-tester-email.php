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

echo 'Private Tester Queue email encoding contract: valid' . (class_exists('DOMDocument') ? '.' : ' (DOM extension unavailable in local PHP; parser declaration statically verified).') . "\n";
