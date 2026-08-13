<?php
declare(strict_types=1);

/**
 * Same-origin Alpha tester-interest receiver.
 *
 * The recipient and rate-limit key are deliberately supplied from a private
 * file one level above the public document root. Do not place them in this
 * artifact or replace them with browser-visible configuration.
 */

const FORM_ORIGIN = 'https://player.jamesjennison.net';
const FALLBACK_LOCATION = '/product-testing/';
const DELIVERY_DOMAIN = 'jamesjennison.net';
const MAX_REQUEST_BYTES = 16_384;
const RATE_WINDOW_SECONDS = 1_800;
const RATE_LIMIT = 3;
const SIGNUP_CONFIRMATION_SUBJECT = 'Thanks for signing up to test the 24Seven.FM Player';

ini_set('display_errors', '0');

$wantsJson = str_contains($_SERVER['HTTP_ACCEPT'] ?? '', 'application/json');

function respond(int $status, string $message, string $fallback, bool $wantsJson): never
{
    if ($wantsJson) {
        http_response_code($status);
        header('Content-Type: application/json; charset=UTF-8');
        header('Cache-Control: no-store');
        echo json_encode(['ok' => $status < 400, 'message' => $message], JSON_UNESCAPED_SLASHES);
        exit;
    }

    header('Location: ' . FALLBACK_LOCATION . '?application=' . rawurlencode($fallback) . '#alpha-tester-interest', true, 303);
    exit;
}

function requestField(string $name, int $maximum, bool $required = false): string
{
    $value = $_POST[$name] ?? '';
    if (is_array($value)) {
        throw new InvalidArgumentException('Invalid field.');
    }

    $value = trim((string) $value);
    if (($required && $value === '') || strlen($value) > $maximum || !preg_match('//u', $value)) {
        throw new InvalidArgumentException('Invalid field.');
    }
    return $value;
}

function consumeRateLimit(array $config): bool
{
    $ip = $_SERVER['REMOTE_ADDR'] ?? '';
    if ($ip === '' || !isset($config['rate_limit_key']) || !is_string($config['rate_limit_key'])) {
        return false;
    }

    $directory = dirname(__DIR__) . '/.cache/alpha-tester-interest';
    if (!is_dir($directory) && !mkdir($directory, 0700, true) && !is_dir($directory)) {
        return false;
    }

    $now = time();
    foreach (new FilesystemIterator($directory, FilesystemIterator::SKIP_DOTS) as $entry) {
        if ($entry->isFile() && $entry->getMTime() + RATE_WINDOW_SECONDS < $now) {
            unlink($entry->getPathname());
        }
    }

    $identifier = hash_hmac('sha256', $ip, $config['rate_limit_key']);
    $handle = fopen($directory . '/' . $identifier, 'c+');
    if ($handle === false || !flock($handle, LOCK_EX)) {
        return false;
    }

    try {
        $record = stream_get_contents($handle);
        [$windowEnds, $count] = array_pad(array_map('intval', explode(':', (string) $record, 2)), 2, 0);
        if ($windowEnds <= $now) {
            $windowEnds = $now + RATE_WINDOW_SECONDS;
            $count = 0;
        }
        if ($count >= RATE_LIMIT) {
            return false;
        }

        rewind($handle);
        ftruncate($handle, 0);
        fwrite($handle, $windowEnds . ':' . ($count + 1));
        fflush($handle);
        return true;
    } finally {
        flock($handle, LOCK_UN);
        fclose($handle);
    }
}

function smtpWrite($socket, string $value): void
{
    $offset = 0;
    $length = strlen($value);
    while ($offset < $length) {
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
        if (!preg_match('/^(\d{3})([ -])/', $line, $matches)) {
            throw new RuntimeException('Invalid SMTP response.');
        }
        $code ??= (int) $matches[1];
        if ($code !== (int) $matches[1]) {
            throw new RuntimeException('Inconsistent SMTP response.');
        }
        $lines[] = $line;
        if ($matches[2] === ' ') {
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

function smtpDiagnostic(string $stage): void
{
    // Keep delivery diagnostics useful without recording application content
    // or any personally identifying request data in the server error log.
    error_log('alpha-tester-interest delivery failure stage=' . $stage);
}

function signupConfirmationEmail(): array
{
    return [
        'subject' => SIGNUP_CONFIRMATION_SUBJECT,
        'plainText' => <<<'TEXT'
Thanks for signing up to help test the 24Seven.FM Player!

We've received your interest in joining the internal testing program. We really appreciate your willingness to help us test the app, find bugs, and improve the experience before a wider release.

At this stage, there's nothing else you need to do yet.

Once you've been added to the testing program, you'll receive a separate welcome email with everything you need to get started, including:

- Instructions for downloading and installing the 24Seven.FM Player
- Information about the current testing build
- How testing assignments work
- How to find your assigned testing tasks
- How to report passes, bugs, and other feedback
- Important testing and privacy guidelines

Testing will be divided into focused assignments, so you won't be expected to test every feature or complete the entire testing catalog yourself.

Some tests may require a particular station account, Android version, device type, accessory, or other setup. If that's the case, your assignment will tell you exactly what you need.

Please keep an eye on your inbox for the welcome email once your tester access has been activated.

Thanks again for volunteering to help make the 24Seven.FM Player better.

James — 24Seven.FM Player Testing Team
TEXT,
        'html' => <<<'HTML'
<p>Thanks for signing up to help test the <strong>24Seven.FM Player</strong>!</p>

<p>We've received your interest in joining the internal testing program. We really appreciate your willingness to help us test the app, find bugs, and improve the experience before a wider release.</p>

<p>At this stage, <strong>there's nothing else you need to do yet</strong>.</p>

<p>Once you've been added to the testing program, you'll receive a separate <strong>welcome email</strong> with everything you need to get started, including:</p>

<ul>
  <li>Instructions for downloading and installing the 24Seven.FM Player</li>
  <li>Information about the current testing build</li>
  <li>How testing assignments work</li>
  <li>How to find your assigned testing tasks</li>
  <li>How to report passes, bugs, and other feedback</li>
  <li>Important testing and privacy guidelines</li>
</ul>

<p>Testing will be divided into focused assignments, so you won't be expected to test every feature or complete the entire testing catalog yourself.</p>

<p>Some tests may require a particular station account, Android version, device type, accessory, or other setup. If that's the case, your assignment will tell you exactly what you need.</p>

<p>Please keep an eye on your inbox for the welcome email once your tester access has been activated.</p>

<p>Thanks again for volunteering to help make the <strong>24Seven.FM Player</strong> better.</p>

<p><strong>James —</strong> <strong>24Seven.FM Player Testing Team</strong></p>
HTML,
    ];
}

function base64MimePart(string $content): string
{
    return rtrim(chunk_split(base64_encode($content), 76, "\r\n"), "\r\n");
}

function smtpMessage(string $recipient, string $sender, string $replyTo, string $subject, string $plainText, ?string $html = null): string
{
    if ($html === null) {
        return implode("\r\n", [
            'From: ' . $sender,
            'To: ' . $recipient,
            'Reply-To: ' . $replyTo,
            'Subject: ' . $subject,
            'MIME-Version: 1.0',
            'Content-Type: text/plain; charset=UTF-8',
            'Content-Transfer-Encoding: base64',
            '',
            base64MimePart($plainText),
        ]);
    }
    $boundary = '=_24Seven_' . bin2hex(random_bytes(16));
    return implode("\r\n", [
        'From: ' . $sender,
        'To: ' . $recipient,
        'Reply-To: ' . $replyTo,
        'Subject: ' . $subject,
        'MIME-Version: 1.0',
        'Content-Type: multipart/alternative; boundary="' . $boundary . '"',
        '',
        '--' . $boundary,
        'Content-Type: text/plain; charset=UTF-8',
        'Content-Transfer-Encoding: base64',
        '',
        base64MimePart($plainText),
        '--' . $boundary,
        'Content-Type: text/html; charset=UTF-8',
        'Content-Transfer-Encoding: base64',
        '',
        base64MimePart('<!doctype html><html><body>' . $html . '</body></html>'),
        '--' . $boundary . '--',
    ]);
}

function smtpDeliver(string $recipient, string $sender, string $replyTo, string $subject, string $plainText, ?string $html = null): bool
{
    if (!getmxrr(DELIVERY_DOMAIN, $hosts, $priorities) || $hosts === []) {
        smtpDiagnostic('mx_lookup');
        return false;
    }
    array_multisort($priorities, SORT_ASC, SORT_NUMERIC, $hosts);

    foreach ($hosts as $host) {
        $host = rtrim($host, '.');
        if ($host === '') {
            continue;
        }

        $socket = null;
        $stage = 'connect';
        try {
            $context = stream_context_create(['ssl' => [
                'verify_peer' => true,
                'verify_peer_name' => true,
                'allow_self_signed' => false,
                'peer_name' => $host,
            ]]);
            $socket = stream_socket_client(
                'tcp://' . $host . ':25',
                $errorNumber,
                $errorMessage,
                10,
                STREAM_CLIENT_CONNECT,
                $context,
            );
            if ($socket === false) {
                throw new RuntimeException('SMTP connection failed.');
            }
            stream_set_timeout($socket, 15);
            $stage = 'greeting';
            [$greeting] = smtpResponse($socket);
            if ($greeting !== 220) {
                throw new RuntimeException('SMTP greeting rejected.');
            }
            $stage = 'ehlo';
            [, $capabilities] = smtpCommand($socket, 'EHLO player.jamesjennison.net', [250]);
            if (!array_filter($capabilities, static fn (string $line): bool => str_contains(strtoupper($line), 'STARTTLS'))) {
                throw new RuntimeException('SMTP STARTTLS is unavailable.');
            }
            $stage = 'starttls';
            smtpCommand($socket, 'STARTTLS', [220]);
            if (!stream_socket_enable_crypto($socket, true, STREAM_CRYPTO_METHOD_TLS_CLIENT)) {
                throw new RuntimeException('SMTP TLS negotiation failed.');
            }
            $stage = 'tls_ehlo';
            smtpCommand($socket, 'EHLO player.jamesjennison.net', [250]);
            $stage = 'mail_from';
            smtpCommand($socket, 'MAIL FROM:<' . $sender . '>', [250]);
            $stage = 'recipient';
            smtpCommand($socket, 'RCPT TO:<' . $recipient . '>', [250, 251]);
            $stage = 'data';
            smtpCommand($socket, 'DATA', [354]);

            $data = smtpMessage($recipient, $sender, $replyTo, $subject, $plainText, $html);
            $data = str_replace(["\r\n", "\r"], "\n", $data);
            $data = preg_replace('/(?m)^\./', '..', $data) ?? $data;
            $stage = 'message';
            smtpWrite($socket, str_replace("\n", "\r\n", $data) . "\r\n.\r\n");
            [$code] = smtpResponse($socket);
            if ($code !== 250) {
                throw new RuntimeException('SMTP message rejected.');
            }
            smtpCommand($socket, 'QUIT', [221]);
            fclose($socket);
            return true;
        } catch (Throwable $exception) {
            smtpDiagnostic($stage);
            if (is_resource($socket)) {
                fclose($socket);
            }
        }
    }
    return false;
}

if (getenv('ALPHA_TESTER_INTEREST_TEST_LIBRARY') === '1') {
    return;
}

try {
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        respond(405, 'This form accepts submissions only.', 'error', $wantsJson);
    }
    if ((int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > MAX_REQUEST_BYTES) {
        respond(413, 'The application is too large to send.', 'error', $wantsJson);
    }
    if (!hash_equals(FORM_ORIGIN, $_SERVER['HTTP_ORIGIN'] ?? '')) {
        respond(403, 'This application request is not allowed.', 'error', $wantsJson);
    }

    $config = require dirname(__DIR__) . '/.alpha-tester-interest-config.php';
    if (!is_array($config) || !isset($config['recipient'], $config['sender'], $config['rate_limit_key'])) {
        throw new RuntimeException('Missing private form configuration.');
    }

    $name = requestField('name', 100, true);
    $email = requestField('email', 254, true);
    $country = requestField('country', 80);
    $device = requestField('device', 160, true);
    $androidVersion = requestField('androidVersion', 48, true);
    $experience = requestField('experience', 1200);
    $company = requestField('company', 100);
    $consent = requestField('consent', 3, true);
    if (!filter_var($email, FILTER_VALIDATE_EMAIL) || $consent !== 'yes') {
        throw new InvalidArgumentException('Invalid application.');
    }

    $allowedInterests = ['Playback', 'Foldable and tablet', 'Accessibility', 'Account and community', 'General testing'];
    $interests = $_POST['interests'] ?? [];
    if (!is_array($interests) || count($interests) > count($allowedInterests)) {
        throw new InvalidArgumentException('Invalid interests.');
    }
    foreach ($interests as $interest) {
        if (!is_string($interest)) {
            throw new InvalidArgumentException('Invalid interests.');
        }
    }
    $interests = array_values(array_unique(array_map(static fn ($interest): string => trim((string) $interest), $interests)));
    if (array_diff($interests, $allowedInterests)) {
        throw new InvalidArgumentException('Invalid interests.');
    }

    // Honeypot submissions appear successful but are discarded without mail.
    if ($company !== '') {
        respond(200, 'Your tester-interest application was sent.', 'sent', $wantsJson);
    }
    if (!consumeRateLimit($config)) {
        respond(429, 'Please wait before sending another application.', 'error', $wantsJson);
    }

    $message = implode("\n", [
        'Alpha tester-interest application',
        '',
        'Display name: ' . $name,
        'Google Play account email: ' . $email,
        'Country or region: ' . ($country !== '' ? $country : 'Not provided'),
        'Android device: ' . $device,
        'Android version: ' . $androidVersion,
        'Interests: ' . ($interests ? implode(', ', $interests) : 'Not provided'),
        'Prior testing experience: ' . ($experience !== '' ? $experience : 'Not provided'),
        'Consent: Confirmed',
    ]);
    $sent = smtpDeliver(
        $config['recipient'],
        $config['sender'],
        $email,
        '24Seven.FM Player Alpha tester-interest application',
        $message,
    );
    if (!$sent) {
        throw new RuntimeException('Mail transport rejected the submission.');
    }

    $confirmation = signupConfirmationEmail();
    if (!smtpDeliver(
        $email,
        $config['sender'],
        $config['sender'],
        $confirmation['subject'],
        $confirmation['plainText'],
        $confirmation['html'],
    )) {
        // Coordinator intake was already accepted. Do not re-submit it or
        // expose transport detail to the applicant if this bounded follow-up fails.
        smtpDiagnostic('signup_confirmation');
    }

    respond(200, 'Your tester-interest application was sent.', 'sent', $wantsJson);
} catch (InvalidArgumentException $exception) {
    respond(400, 'Please check the required fields and try again.', 'error', $wantsJson);
} catch (Throwable $exception) {
    respond(503, 'The application could not be delivered. Please try again later.', 'error', $wantsJson);
}
