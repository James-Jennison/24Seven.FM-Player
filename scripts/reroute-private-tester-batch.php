#!/usr/bin/env php
<?php
declare(strict_types=1);

/**
 * Explicit one-time reroute for a prepared/dispatched tester email batch.
 *
 * Run only on the Player Webuzo host. It sends individual multipart messages
 * through mail.jamesjennison.net and reports aggregate outcomes only.
 *
 * php reroute-private-tester-batch.php --batch-id 1 --expected-recipients 8
 */

const MAIL_SUBMISSION_HOST = 'mail.jamesjennison.net';

function smtpWrite($socket, string $value): void
{
    $offset = 0;
    while ($offset < strlen($value)) {
        $written = fwrite($socket, substr($value, $offset));
        if ($written === false || $written === 0) {
            throw new RuntimeException('SMTP write failed.');
        }
        $offset += $written;
    }
}

function smtpResponse($socket): array
{
    $lines = [];
    $code = null;
    while (($line = fgets($socket, 2048)) !== false) {
        $line = rtrim($line, "\r\n");
        if (!preg_match('/^(\d{3})([ -])/', $line, $match)) {
            throw new RuntimeException('Invalid SMTP response.');
        }
        $code ??= (int) $match[1];
        if ($code !== (int) $match[1]) {
            throw new RuntimeException('Inconsistent SMTP response.');
        }
        $lines[] = $line;
        if ($match[2] === ' ') {
            return [$code, $lines];
        }
    }
    throw new RuntimeException('SMTP connection closed.');
}

function smtpCommand($socket, string $command, array $expected): array
{
    smtpWrite($socket, $command . "\r\n");
    [$code, $lines] = smtpResponse($socket);
    if (!in_array($code, $expected, true)) {
        throw new RuntimeException('SMTP command rejected.');
    }
    return [$code, $lines];
}

function plainTextToHtml(string $text): string
{
    $paragraphs = preg_split("/\r?\n{2,}/", trim($text)) ?: [];
    $html = '';
    foreach ($paragraphs as $paragraph) {
        $html .= '<p>' . nl2br(htmlspecialchars($paragraph, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8'), false) . '</p>';
    }
    return $html;
}

function submitMessage(array $config, string $recipient, string $subject, string $plainText, string $html, string $archivePath): bool
{
    $senderName = trim((string) ($config['from_name'] ?? '24Seven.FM Player'));
    $from = $config['from_email'];
    $boundary = '=_24Seven_' . bin2hex(random_bytes(16));
    $headers = [
        'From: =?UTF-8?B?' . base64_encode($senderName) . '?= <' . $from . '>',
        'To: ' . $recipient,
        'Reply-To: ' . $from,
        'MIME-Version: 1.0',
        'Content-Type: multipart/alternative; boundary="' . $boundary . '"',
    ];
    $message = implode("\r\n", [
        '--' . $boundary,
        'Content-Type: text/plain; charset=UTF-8',
        'Content-Transfer-Encoding: 8bit',
        '',
        $plainText,
        '--' . $boundary,
        'Content-Type: text/html; charset=UTF-8',
        'Content-Transfer-Encoding: 8bit',
        '',
        '<!doctype html><html><body>' . $html . '</body></html>',
        '--' . $boundary . '--',
        '',
    ]);
    $message = preg_replace('/(?m)^\./', '..', $message) ?? $message;
    $payload = implode("\r\n", $headers) . "\r\nSubject: " . $subject . "\r\n\r\n" . $message;
    if (file_put_contents($archivePath, $payload, LOCK_EX) === false || !chmod($archivePath, 0600)) {
        throw new RuntimeException('Unable to create the private sent-message archive.');
    }
    $socket = null;
    $acceptedByServer = false;
    try {
        $context = stream_context_create(['ssl' => [
            'verify_peer' => true,
            'verify_peer_name' => true,
            'allow_self_signed' => false,
            'peer_name' => MAIL_SUBMISSION_HOST,
        ]]);
        $socket = stream_socket_client('tcp://' . MAIL_SUBMISSION_HOST . ':25', $errorNumber, $errorMessage, 15, STREAM_CLIENT_CONNECT, $context);
        if ($socket === false) {
            throw new RuntimeException('SMTP connection failed.');
        }
        stream_set_timeout($socket, 20);
        [$greeting] = smtpResponse($socket);
        if ($greeting !== 220) {
            throw new RuntimeException('SMTP greeting rejected.');
        }
        [, $capabilities] = smtpCommand($socket, 'EHLO player.jamesjennison.net', [250]);
        if (!array_filter($capabilities, static fn (string $line): bool => str_contains(strtoupper($line), 'STARTTLS'))) {
            throw new RuntimeException('SMTP STARTTLS is unavailable.');
        }
        smtpCommand($socket, 'STARTTLS', [220]);
        if (!stream_socket_enable_crypto($socket, true, STREAM_CRYPTO_METHOD_TLS_CLIENT)) {
            throw new RuntimeException('SMTP TLS negotiation failed.');
        }
        smtpCommand($socket, 'EHLO player.jamesjennison.net', [250]);
        smtpCommand($socket, 'MAIL FROM:<' . $from . '>', [250]);
        smtpCommand($socket, 'RCPT TO:<' . $recipient . '>', [250, 251]);
        smtpCommand($socket, 'DATA', [354]);
        smtpWrite($socket, $payload . "\r\n.\r\n");
        [$accepted] = smtpResponse($socket);
        if ($accepted !== 250) {
            throw new RuntimeException('SMTP message rejected.');
        }
        $acceptedByServer = true;
        smtpCommand($socket, 'QUIT', [221]);
        fclose($socket);
        return true;
    } catch (Throwable $exception) {
        if (is_resource($socket)) {
            fclose($socket);
        }
        if ($acceptedByServer) {
            return true;
        }
        @unlink($archivePath);
        return false;
    }
}

$options = getopt('', ['batch-id:', 'expected-recipients:', 'archive-directory:']);
$batchId = filter_var($options['batch-id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
$expected = filter_var($options['expected-recipients'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
$archiveDirectory = $options['archive-directory'] ?? null;
if ($batchId === false || $expected === false || !is_string($archiveDirectory) || !is_dir($archiveDirectory) || !is_writable($archiveDirectory)) {
    fwrite(STDERR, "Usage: reroute-private-tester-batch.php --batch-id ID --expected-recipients COUNT --archive-directory DIRECTORY\n");
    exit(64);
}

$configurationPath = getenv('PRIVATE_TESTER_QUEUE_CONFIG');
if (!is_string($configurationPath) || $configurationPath === '') {
    throw new RuntimeException('Private tester queue configuration path is unavailable.');
}
$config = require $configurationPath;
if (!is_array($config) || !isset($config['database_path'], $config['from_email']) || !filter_var($config['from_email'], FILTER_VALIDATE_EMAIL)) {
    throw new RuntimeException('Private tester queue configuration is invalid.');
}
$database = new PDO('sqlite:' . $config['database_path'], null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]);
$batchStatement = $database->prepare('SELECT subject, body, body_html FROM email_batches WHERE id = ?');
$batchStatement->execute([$batchId]);
$batch = $batchStatement->fetch();
if ($batch === false) {
    throw new RuntimeException('Requested batch is unavailable.');
}
$recipientStatement = $database->prepare('SELECT testers.email FROM email_batch_recipients JOIN testers ON testers.id = tester_id WHERE batch_id = ? ORDER BY testers.id');
$recipientStatement->execute([$batchId]);
$recipients = $recipientStatement->fetchAll();
if (count($recipients) !== $expected) {
    throw new RuntimeException('Recipient count does not match the explicit reroute request.');
}
foreach (array_keys($recipients) as $index) {
    if (file_exists($archiveDirectory . '/message-' . ($index + 1) . '.eml')) {
        throw new RuntimeException('The private sent-message archive is not empty.');
    }
}
$html = is_string($batch['body_html'] ?? null) && $batch['body_html'] !== '' ? $batch['body_html'] : plainTextToHtml($batch['body']);
$accepted = 0;
foreach ($recipients as $index => $recipient) {
    if (submitMessage($config, $recipient['email'], $batch['subject'], $batch['body'], $html, $archiveDirectory . '/message-' . ($index + 1) . '.eml')) {
        $accepted++;
    }
}
printf("Mail-server submission accepted=%d expected=%d\n", $accepted, $expected);
exit($accepted === $expected ? 0 : 1);
