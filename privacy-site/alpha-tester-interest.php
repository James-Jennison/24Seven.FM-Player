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
const MAX_REQUEST_BYTES = 16_384;
const RATE_WINDOW_SECONDS = 1_800;
const RATE_LIMIT = 3;

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
    $headers = [
        'From: ' . $config['sender'],
        'Reply-To: ' . $email,
        'MIME-Version: 1.0',
        'Content-Type: text/plain; charset=UTF-8',
    ];
    $sent = mail(
        $config['recipient'],
        '24Seven.FM Player Alpha tester-interest application',
        $message,
        implode("\r\n", $headers),
        '-f' . $config['sender'],
    );
    if (!$sent) {
        throw new RuntimeException('Mail transport rejected the submission.');
    }

    respond(200, 'Your tester-interest application was sent.', 'sent', $wantsJson);
} catch (InvalidArgumentException $exception) {
    respond(400, 'Please check the required fields and try again.', 'error', $wantsJson);
} catch (Throwable $exception) {
    respond(503, 'The application could not be delivered. Please try again later.', 'error', $wantsJson);
}
