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
const TURNSTILE_CONFIG_FILE = '.turnstile-test-config.php';
const TURNSTILE_ACTION = 'alpha-tester-interest';
const TURNSTILE_HOSTNAME = 'player.jamesjennison.net';
const MAX_REQUEST_BYTES = 16_384;
const RATE_WINDOW_SECONDS = 1_800;
const RATE_LIMIT = 3;
const SIGNUP_CONFIRMATION_SUBJECT = 'Thanks for signing up to test the 24Seven.FM Player';
const STATIONS = [
    'sst' => 'StreamingSoundtracks.com',
    '1980s' => '1980s.FM',
    'afm' => 'Adagio.FM',
    'dfm' => 'Death.FM',
    'efm' => 'Entranced.FM',
];
const PRIMARY_STATIONS = STATIONS + [
    'multiple' => 'I regularly listen to more than one',
    'none' => "I don't have a primary station",
];
const DEVICE_FORM_FACTORS = [
    'phone' => 'Standard phone',
    'foldable' => 'Foldable / flip phone',
    'tablet' => 'Android tablet',
    'chromebook' => 'Chromebook with Android app support',
    'other' => 'Other Android device',
];
const TESTING_INTERESTS = [
    'playback' => 'Playback and media controls',
    'queue_history_data' => 'Queue, History, and station data',
    'accounts_favorites' => 'Accounts and Favorites',
    'request_safety' => 'Song request browsing and safety',
    'chat_community' => 'Chat and community features',
    'network_recovery' => 'Network loss and recovery',
    'audio_accessories' => 'Audio devices and accessories',
    'adaptive_layouts' => 'Foldable, tablet, and adaptive layouts',
    'accessibility' => 'Accessibility and alternative input',
    'general' => 'General testing / anything needed',
];
const NETWORK_CAPABILITIES = [
    'wifi' => 'Wi-Fi',
    'mobile_data' => 'Mobile/cellular data',
    'network_handoff' => 'Switching between Wi-Fi and mobile data',
    'network_disconnect' => 'Temporarily disconnecting network access to test recovery',
];
const AUDIO_CAPABILITIES = [
    'device_speaker' => 'Device speaker',
    'bluetooth_headphones' => 'Bluetooth headphones or earbuds',
    'bluetooth_speaker' => 'Bluetooth speaker',
    'wired_headphones' => 'Wired headphones/headset',
    'usb_audio' => 'USB audio device',
    'android_auto' => 'Android Auto / automotive media',
    'hearing_aid' => 'Hearing aid / assistive audio device',
    'hdmi_audio' => 'HDMI / external display audio',
    'external_input' => 'External keyboard or mouse/trackpad',
    'none' => 'None beyond the device itself',
];
const ACCESSIBILITY_CAPABILITIES = [
    'talkback' => 'TalkBack',
    'large_text' => 'Large text / enlarged display',
    'voice_access' => 'Voice Access',
    'switch_access' => 'Switch Access',
    'accessibility_scanner' => 'Accessibility Scanner / touch-target review',
    'external_keyboard' => 'External keyboard',
    'mouse_trackpad' => 'Mouse / trackpad',
    'general_accessibility' => 'General accessibility testing',
    'none' => 'None',
];
const TESTING_COMFORT = [
    'readonly' => 'Read-only and general testing',
    'account' => 'Account testing',
    'controlled' => 'Controlled live testing',
    'any' => 'Any appropriate testing',
];
const CONTROLLED_ACTIONS = [
    'song_request' => 'One authorized song request',
    'chat_message' => 'One harmless authorized Chat message',
    'chat_mention' => 'Two-account Chat mention testing',
    'session_testing' => 'Sign-in / sign-out / session testing',
    'report_block' => 'Report/block/unblock workflow without sending a moderation email',
    'account_testing' => 'General account-based testing',
    'none' => 'None',
];
const TESTING_AVAILABILITY = [
    'under_30m' => 'Less than 30 minutes',
    '30_60m' => 'About 30–60 minutes',
    '1_2h' => 'About 1–2 hours',
    '2_4h' => 'About 2–4 hours',
    'over_4h' => 'More than 4 hours',
    'varies' => 'It varies',
];
const RECRUITMENT_SOURCES = [
    'direct' => 'Direct / project site',
    'testers_community' => 'Testers Community',
    'betabound' => 'Betabound',
    'betafamily' => 'BetaFamily',
    'other' => 'Other approved source',
];

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

function requestChoice(string $name, array $allowed, bool $required = false): ?string
{
    $value = requestField($name, 80, $required);
    if ($value === '') {
        return null;
    }
    if (!array_key_exists($value, $allowed)) {
        throw new InvalidArgumentException('Invalid field.');
    }
    return $value;
}

function requestChoices(string $name, array $allowed, int $maximum, bool $noneExclusive = false): array
{
    $values = $_POST[$name] ?? [];
    if (!is_array($values) || count($values) > $maximum) {
        throw new InvalidArgumentException('Invalid field.');
    }
    $normalized = [];
    foreach ($values as $value) {
        if (!is_string($value) || strlen($value) > 80 || !preg_match('//u', $value) || !array_key_exists($value, $allowed)) {
            throw new InvalidArgumentException('Invalid field.');
        }
        $normalized[$value] = true;
    }
    $result = array_keys($normalized);
    if ($noneExclusive && in_array('none', $result, true)) {
        return ['none'];
    }
    return $result;
}

function turnstileVerificationAccepted(array $verification): bool
{
    return ($verification['success'] ?? false) === true
        && ($verification['action'] ?? '') === TURNSTILE_ACTION
        && ($verification['hostname'] ?? '') === TURNSTILE_HOSTNAME;
}

function verifyTurnstile(string $token, string $secret): bool
{
    if ($token === '' || strlen($token) > 2048 || trim($secret) === '') {
        return false;
    }

    $payload = http_build_query([
        'secret' => $secret,
        'response' => $token,
        'remoteip' => $_SERVER['REMOTE_ADDR'] ?? '',
    ], '', '&', PHP_QUERY_RFC3986);
    $context = stream_context_create(['http' => [
        'method' => 'POST',
        'header' => "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: " . strlen($payload) . "\r\n",
        'content' => $payload,
        'timeout' => 10,
        'ignore_errors' => true,
    ]]);
    $response = @file_get_contents('https://challenges.cloudflare.com/turnstile/v0/siteverify', false, $context);
    $verification = is_string($response) ? json_decode($response, true) : null;

    return is_array($verification) && turnstileVerificationAccepted($verification);
}

function labels(array $values, array $allowed, string $fallback = 'Not provided'): string
{
    if ($values === []) {
        return $fallback;
    }
    return implode(', ', array_map(static fn (string $value): string => $allowed[$value], $values));
}

function applicationFromRequest(): array
{
    $application = [
        'name' => requestField('name', 100, true),
        'email' => requestField('email', 254, true),
        'country' => requestField('country', 80),
        'primary_station' => requestChoice('primaryStation', PRIMARY_STATIONS, true),
        'other_stations' => requestChoices('otherStations', STATIONS, count(STATIONS)),
        'station_accounts' => requestChoices('stationAccounts', STATIONS + ['none' => 'None'], count(STATIONS) + 1, true),
        'device' => requestField('device', 160, true),
        'android_version' => requestField('androidVersion', 48, true),
        'device_form_factor' => requestChoice('deviceFormFactor', DEVICE_FORM_FACTORS, true),
        'other_devices' => requestField('otherDevices', 500),
        'interests' => requestChoices('interests', TESTING_INTERESTS, count(TESTING_INTERESTS)),
        'network_capabilities' => requestChoices('networkCapabilities', NETWORK_CAPABILITIES, count(NETWORK_CAPABILITIES)),
        'audio_capabilities' => requestChoices('audioCapabilities', AUDIO_CAPABILITIES, count(AUDIO_CAPABILITIES), true),
        'accessibility_capabilities' => requestChoices('accessibilityCapabilities', ACCESSIBILITY_CAPABILITIES, count(ACCESSIBILITY_CAPABILITIES), true),
        'testing_comfort' => requestChoice('testingComfort', TESTING_COMFORT, true),
        'controlled_actions' => requestChoices('controlledActions', CONTROLLED_ACTIONS, count(CONTROLLED_ACTIONS), true),
        'testing_availability' => requestChoice('testingAvailability', TESTING_AVAILABILITY),
        'experience' => requestField('experience', 1200),
        'recruitment_source' => requestChoice('recruitmentSource', RECRUITMENT_SOURCES) ?? 'direct',
        'company' => requestField('company', 100),
        'consent' => requestField('consent', 3, true),
    ];
    if (!filter_var($application['email'], FILTER_VALIDATE_EMAIL) || $application['consent'] !== 'yes') {
        throw new InvalidArgumentException('Invalid application.');
    }
    if (in_array($application['primary_station'], array_keys(STATIONS), true)) {
        $application['other_stations'] = array_values(array_diff($application['other_stations'], [$application['primary_station']]));
    }
    return $application;
}

function coordinatorIntakeMessage(array $application): string
{
    return implode("\n", [
        'Alpha tester-interest application',
        '',
        'Display name: ' . $application['name'],
        'Google Play account email: ' . $application['email'],
        'Country or region: ' . ($application['country'] !== '' ? $application['country'] : 'Not provided'),
        'Primary station: ' . PRIMARY_STATIONS[$application['primary_station']],
        'Other familiar stations: ' . labels($application['other_stations'], STATIONS),
        'Existing station access (optional): ' . labels($application['station_accounts'], STATIONS + ['none' => 'None']),
        'Android device: ' . $application['device'],
        'Android version: ' . $application['android_version'],
        'Device form factor: ' . DEVICE_FORM_FACTORS[$application['device_form_factor']],
        'Other Android devices: ' . ($application['other_devices'] !== '' ? $application['other_devices'] : 'Not provided'),
        'Testing interests: ' . labels($application['interests'], TESTING_INTERESTS),
        'Network capabilities: ' . labels($application['network_capabilities'], NETWORK_CAPABILITIES),
        'Audio/accessory capabilities: ' . labels($application['audio_capabilities'], AUDIO_CAPABILITIES),
        'Accessibility/alternative-input capabilities: ' . labels($application['accessibility_capabilities'], ACCESSIBILITY_CAPABILITIES),
        'Testing comfort level: ' . TESTING_COMFORT[$application['testing_comfort']],
        'Controlled-action preferences: ' . labels($application['controlled_actions'], CONTROLLED_ACTIONS),
        'Two-week availability: ' . ($application['testing_availability'] === null ? 'Not provided' : TESTING_AVAILABILITY[$application['testing_availability']]),
        'Assignment notes: ' . ($application['experience'] !== '' ? $application['experience'] : 'Not provided'),
        'Recruitment source: ' . RECRUITMENT_SOURCES[$application['recruitment_source']],
        'Consent: Confirmed',
    ]);
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

function signupConfirmationEmail(string $recruitmentSource = 'direct'): array
{
    if ($recruitmentSource === 'testers_community') {
        return [
            'subject' => SIGNUP_CONFIRMATION_SUBJECT,
            'plainText' => <<<'TEXT'
Thanks for registering your Testers Community profile for the independently developed, unofficial 24Seven.FM Player!

Testers Community Pack access uses the Google Play Closed Test opt-in instructions supplied in the Pack. Opt in through Google Play with the same Google account that Testers Community enrolled for the Pack and that you registered here.

This profile helps the coordinator match device coverage and focused assignments. It does not itself grant Play access, prove a Play opt-in, or prove installation or activity. Guest testing does not require a 24Seven.FM station account.

After your profile is reviewed for coverage, you will receive a separate Tester Hub sign-in link for focused assignments and private feedback. Please do not reply to this confirmation address, and never provide passwords, station credentials, CAPTCHA answers, or verification codes.

James — 24Seven.FM Player Testing Team
TEXT,
            'html' => <<<'HTML'
<p>Thanks for registering your Testers Community profile for the independently developed, unofficial <strong>24Seven.FM Player</strong>!</p>

<p>Testers Community Pack access uses the Google Play Closed Test opt-in instructions supplied in the Pack. Opt in through Google Play with the same Google account that Testers Community enrolled for the Pack and that you registered here.</p>

<p>This profile helps the coordinator match device coverage and focused assignments. It does not itself grant Play access, prove a Play opt-in, or prove installation or activity. Guest testing does not require a 24Seven.FM station account.</p>

<p>After your profile is reviewed for coverage, you will receive a separate Tester Hub sign-in link for focused assignments and private feedback. Please do not reply to this confirmation address, and never provide passwords, station credentials, CAPTCHA answers, or verification codes.</p>

<p><strong>James —</strong> <strong>24Seven.FM Player Testing Team</strong></p>
HTML,
        ];
    }

    return [
        'subject' => SIGNUP_CONFIRMATION_SUBJECT,
        'plainText' => <<<'TEXT'
Thanks for signing up to help test the independently developed, unofficial 24Seven.FM Player!

We've received your interest in joining the Google Play Closed Testing program. The Player is an independently developed, unofficial player for the 24Seven.FM network of internet radio stations. We appreciate your willingness to help test the app, find bugs, and improve the experience before a wider release.

At this stage, there's nothing else you need to do yet.

Once you've been added to the testing program, you'll receive a separate welcome email with everything you need to get started, including:

- Instructions for downloading and installing the 24Seven.FM Player
- Information about the current testing build
- How testing assignments work
- How to find your assigned testing tasks
- How to report passes, bugs, and other feedback
- Important testing and privacy guidelines

Testing will be divided into focused assignments, so you won't be expected to test every feature or complete the entire testing catalog yourself. You need your own Google account only to opt in to Google Play; guest testing does not require a 24Seven.FM station account.

Some tests may require a particular station account, Android version, device type, accessory, or other setup. If that's the case, your assignment will tell you exactly what you need.

Please keep an eye on your inbox for the welcome email once your tester access has been activated.

Please note: alpha-testing@jamesjennison.net is not a monitored inbox. Please do not reply to this confirmation email.

Thanks again for volunteering to help make the 24Seven.FM Player better.

James — 24Seven.FM Player Testing Team
TEXT,
        'html' => <<<'HTML'
<p>Thanks for signing up to help test the independently developed, unofficial <strong>24Seven.FM Player</strong>!</p>

<p>We've received your interest in joining the Google Play Closed Testing program. The Player is an independently developed, unofficial player for the 24Seven.FM network of internet radio stations. We appreciate your willingness to help test the app, find bugs, and improve the experience before a wider release.</p>

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

<p><strong>Please note:</strong> alpha-testing@jamesjennison.net is not a monitored inbox. Please do not reply to this confirmation email.</p>

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

    $turnstileConfig = require dirname(__DIR__) . '/' . TURNSTILE_CONFIG_FILE;
    if (!is_array($turnstileConfig) || !isset($turnstileConfig['secret']) || !is_string($turnstileConfig['secret'])) {
        throw new RuntimeException('Missing Turnstile configuration.');
    }
    $turnstileToken = $_POST['cf-turnstile-response'] ?? '';
    if (!is_string($turnstileToken) || !verifyTurnstile($turnstileToken, $turnstileConfig['secret'])) {
        respond(403, 'Please complete the verification and try again.', 'error', $wantsJson);
    }

    $config = require dirname(__DIR__) . '/.alpha-tester-interest-config.php';
    if (!is_array($config) || !isset($config['recipient'], $config['sender'], $config['rate_limit_key'])) {
        throw new RuntimeException('Missing private form configuration.');
    }

    $application = applicationFromRequest();

    // Honeypot submissions appear successful but are discarded without mail.
    if ($application['company'] !== '') {
        respond(200, 'Your tester-interest application was sent.', 'sent', $wantsJson);
    }
    if (!consumeRateLimit($config)) {
        respond(429, 'Please wait before sending another application.', 'error', $wantsJson);
    }

    $message = coordinatorIntakeMessage($application);
    $sent = smtpDeliver(
        $config['recipient'],
        $config['sender'],
        $application['email'],
        '24Seven.FM Player Alpha tester-interest application',
        $message,
    );
    if (!$sent) {
        throw new RuntimeException('Mail transport rejected the submission.');
    }

    $confirmation = signupConfirmationEmail($application['recruitment_source']);
    if (!smtpDeliver(
        $application['email'],
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
