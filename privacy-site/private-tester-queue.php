<?php
declare(strict_types=1);

/**
 * Private Alpha-tester coordinator queue.
 *
 * This endpoint intentionally has no public discovery link. It will not start
 * without a private configuration file and its SQLite database lives outside
 * the public document root. Configure it only on the production host with
 * .private-tester-queue-config.php one level above this deployed artifact.
 */

const QUEUE_CONFIG_FILE = '.private-tester-queue-config.php';
const SESSION_NAME = 'player_tester_queue';
const ADMIN_SESSION_IDLE_SECONDS = 1_800;
const ADMIN_SESSION_MAX_SECONDS = 28_800;
const ADMIN_LOGIN_MAX_PASSWORD_BYTES = 4_096;
const ADMIN_LOGIN_NAME_MAX_LENGTH = 64;
const ADMIN_LOGIN_FREE_FAILURES = 4;
const ADMIN_LOGIN_MAX_LOCK_SECONDS = 3_600;
const ADMIN_LOGIN_ORIGIN = 'https://player.jamesjennison.net';
const STAGING_ADMIN_LOGIN_HOST = 'onboarding-staging.player.jamesjennison.net';
const STAGING_ADMIN_LOGIN_ORIGIN = 'https://onboarding-staging.player.jamesjennison.net';
const ADMIN_TOTP_STEP_SECONDS = 30;
const ADMIN_TOTP_DIGITS = 6;
const ADMIN_TOTP_ALLOWED_DRIFT_STEPS = 1;
const MAX_SUBJECT_LENGTH = 180;
const MAX_BODY_LENGTH = 12_000;
const MAX_HTML_BODY_LENGTH = 24_000;
const MAIL_SUBMISSION_HOST = 'mail.jamesjennison.net';
const TESTER_PORTAL_PUBLIC_URL = 'https://player.jamesjennison.net/tester-portal.php';
const TASK_REGISTRY_FILE = 'assets/tester-tasks.json';
const MAX_ASSIGNMENT_CONFIGURATION_LENGTH = 300;
const MAX_ASSIGNMENT_NOTE_LENGTH = 1_000;
const MAX_ONBOARDING_NOTE_LENGTH = 1_000;
const CHAT_MAX_MESSAGE_LENGTH = 2_000;
const CHAT_SUBMISSION_WINDOW_SECONDS = 60;
const CHAT_SUBMISSION_MAXIMUM = 12;
const CHAT_RETENTION_DAYS = 90;
const ASSIGNMENT_STATUSES = ['assigned', 'in_progress', 'complete', 'blocked'];
const ONBOARDING_STATUSES = ['profile_pending', 'profile_complete', 'ready', 'paused'];
const APPLICATION_STAGES = ['pending_review', 'accepted', 'active'];
const RECRUITMENT_SOURCE_LABELS = [
    'direct' => 'Direct / project site',
    'testers_community' => 'Testers Community',
    'betabound' => 'Betabound',
    'betafamily' => 'BetaFamily',
    'other' => 'Other approved source',
];
const FEEDBACK_CATEGORIES = [
    'playback' => 'Playback',
    'connection_recovery' => 'Connection / reconnection',
    'station_switching' => 'Station switching',
    'metadata_artwork' => 'Now Playing / metadata / artwork',
    'favorites' => 'Favorites',
    'media_controls' => 'Notification / media controls',
    'audio_accessories' => 'Bluetooth / headphones / audio route',
    'layout_accessibility' => 'UI, layout, or accessibility',
    'performance_battery' => 'Performance or battery',
    'crash_freeze' => 'Crash or freeze',
    'other' => 'Other',
];
const STATION_SCOPES = [
    'Network-wide / not station-specific',
    'StreamingSoundtracks.com',
    '1980s.FM',
    'Adagio.FM',
    'Death.FM',
    'Entranced.FM',
];

ini_set('display_errors', '0');

function fail(int $status, string $message): never
{
    http_response_code($status);
    header('Content-Type: text/plain; charset=UTF-8');
    privateResponseHeaders();
    header('Cache-Control: no-store, private');
    header('Pragma: no-cache');
    header('X-Robots-Tag: noindex, nofollow, noarchive');
    echo $message;
    exit;
}

function config(): array
{
    $path = dirname(__DIR__) . '/' . QUEUE_CONFIG_FILE;
    if (!is_file($path)) {
        fail(503, 'The private tester queue is not configured.');
    }
    $config = require $path;
    foreach (['admin_password_hash', 'database_path', 'from_email'] as $key) {
        if (!isset($config[$key]) || !is_string($config[$key]) || $config[$key] === '') {
            fail(503, 'The private tester queue configuration is incomplete.');
        }
    }
    if (!password_get_info($config['admin_password_hash'])['algo']) {
        fail(503, 'The private tester queue password configuration is invalid.');
    }
    if (!filter_var($config['from_email'], FILTER_VALIDATE_EMAIL)) {
        fail(503, 'The private tester queue sender configuration is invalid.');
    }
    return $config;
}

function coordinatorDisplayName(array $config): string
{
    $name = trim((string) ($config['from_name'] ?? '24Seven.FM Coordinator'));
    if ($name === '' || !preg_match('//u', $name)) return '24Seven.FM Coordinator';
    return function_exists('mb_substr') ? mb_substr($name, 0, 100) : substr($name, 0, 100);
}

function database(array $config): PDO
{
    if (!extension_loaded('pdo_sqlite')) {
        fail(503, 'The private tester queue database driver is unavailable.');
    }
    $directory = dirname($config['database_path']);
    if (!is_dir($directory) || !is_writable($directory)) {
        fail(503, 'The private tester queue storage is unavailable.');
    }

    $database = new PDO('sqlite:' . $config['database_path'], null, null, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    $database->exec('PRAGMA foreign_keys = ON');
    $database->exec(
        'CREATE TABLE IF NOT EXISTS testers (
            id INTEGER PRIMARY KEY,
            source_message_uid TEXT NOT NULL UNIQUE,
            received_at TEXT NOT NULL,
            display_name TEXT NOT NULL,
            email TEXT NOT NULL UNIQUE COLLATE NOCASE,
            country TEXT,
            device TEXT NOT NULL,
            android_version TEXT NOT NULL,
            interests_json TEXT NOT NULL,
            experience TEXT,
            status TEXT NOT NULL CHECK(status IN (\'active\', \'inactive\')),
            imported_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS email_batches (
            id INTEGER PRIMARY KEY,
            subject TEXT NOT NULL,
            body TEXT NOT NULL,
            body_html TEXT,
            created_at TEXT NOT NULL,
            dispatched_at TEXT,
            status TEXT NOT NULL CHECK(status IN (\'prepared\', \'dispatched\', \'partial\', \'failed\'))
        );
        CREATE TABLE IF NOT EXISTS email_batch_recipients (
            batch_id INTEGER NOT NULL REFERENCES email_batches(id) ON DELETE CASCADE,
            tester_id INTEGER NOT NULL REFERENCES testers(id),
            delivery_status TEXT NOT NULL CHECK(delivery_status IN (\'pending\', \'accepted\', \'failed\')),
            accepted_at TEXT,
            PRIMARY KEY (batch_id, tester_id)
        );
        CREATE TABLE IF NOT EXISTS tester_mail_archive (
            id INTEGER PRIMARY KEY,
            tester_id INTEGER NOT NULL REFERENCES testers(id) ON DELETE CASCADE,
            batch_id INTEGER REFERENCES email_batches(id) ON DELETE CASCADE,
            assignment_id INTEGER REFERENCES tester_task_assignments(id) ON DELETE SET NULL,
            message_type TEXT NOT NULL CHECK(message_type IN (\'batch\', \'orientation\', \'assignment\')),
            subject TEXT NOT NULL,
            body TEXT NOT NULL,
            body_html TEXT NOT NULL,
            handoff_status TEXT NOT NULL CHECK(handoff_status IN (\'prepared\', \'accepted\', \'failed\')),
            prepared_at TEXT NOT NULL,
            attempted_at TEXT,
            UNIQUE(batch_id, tester_id)
        );
        CREATE INDEX IF NOT EXISTS tester_mail_archive_recent ON tester_mail_archive(prepared_at DESC, id DESC);
        CREATE INDEX IF NOT EXISTS tester_mail_archive_tester ON tester_mail_archive(tester_id, prepared_at DESC);
        CREATE TABLE IF NOT EXISTS tester_task_assignments (
            id INTEGER PRIMARY KEY,
            tester_id INTEGER NOT NULL REFERENCES testers(id) ON DELETE CASCADE,
            task_id TEXT NOT NULL,
            task_status TEXT NOT NULL DEFAULT \'assigned\' CHECK(task_status IN (\'assigned\', \'in_progress\', \'complete\', \'blocked\')),
            station_scope TEXT NOT NULL DEFAULT \'\',
            configuration_scope TEXT NOT NULL DEFAULT \'\',
            coordinator_note TEXT NOT NULL DEFAULT \'\',
            mutation_authorized INTEGER NOT NULL DEFAULT 0 CHECK(mutation_authorized IN (0, 1)),
            assignment_email_status TEXT NOT NULL DEFAULT \'not_sent\' CHECK(assignment_email_status IN (\'not_sent\', \'accepted\', \'failed\')),
            assignment_email_attempted_at TEXT,
            assignment_email_attempts INTEGER NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS tester_onboarding (
            tester_id INTEGER PRIMARY KEY REFERENCES testers(id) ON DELETE CASCADE,
            onboarding_status TEXT NOT NULL DEFAULT \'profile_pending\' CHECK(onboarding_status IN (\'profile_pending\', \'profile_complete\', \'ready\', \'paused\')),
            coordinator_note TEXT NOT NULL DEFAULT \'\',
            orientation_email_status TEXT NOT NULL DEFAULT \'not_sent\' CHECK(orientation_email_status IN (\'not_sent\', \'accepted\', \'failed\')),
            orientation_email_attempted_at TEXT,
            orientation_email_attempts INTEGER NOT NULL DEFAULT 0,
            initial_smoke_test_confirmed_at TEXT,
            withdrawal_requested_at TEXT,
            deletion_requested_at TEXT,
            reviewed_at TEXT,
            rejected_at TEXT,
            updated_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS tester_portal_tokens (
            id INTEGER PRIMARY KEY,
            tester_id INTEGER NOT NULL REFERENCES testers(id) ON DELETE CASCADE,
            token_hash TEXT NOT NULL UNIQUE,
            requested_at TEXT NOT NULL,
            expires_at TEXT NOT NULL,
            consumed_at TEXT
        );
        CREATE INDEX IF NOT EXISTS tester_portal_tokens_lookup ON tester_portal_tokens(token_hash, expires_at);
        CREATE TABLE IF NOT EXISTS tester_feedback (
            id INTEGER PRIMARY KEY,
            tester_id INTEGER NOT NULL REFERENCES testers(id) ON DELETE CASCADE,
            assignment_id INTEGER REFERENCES tester_task_assignments(id) ON DELETE SET NULL,
            subject TEXT NOT NULL,
            details TEXT NOT NULL,
            outcome TEXT NOT NULL CHECK(outcome IN (\'pass\', \'issue\', \'blocked\', \'note\')),
            category TEXT NOT NULL DEFAULT \'other\' CHECK(category IN (\'playback\', \'connection_recovery\', \'station_switching\', \'metadata_artwork\', \'favorites\', \'media_controls\', \'audio_accessories\', \'layout_accessibility\', \'performance_battery\', \'crash_freeze\', \'other\')),
            created_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS chat_threads (
            id INTEGER PRIMARY KEY,
            tester_id INTEGER NOT NULL UNIQUE REFERENCES testers(id) ON DELETE CASCADE,
            tester_deleted_at TEXT,
            coordinator_deleted_at TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL,
            last_message_at TEXT
        );
        CREATE TABLE IF NOT EXISTS chat_messages (
            id INTEGER PRIMARY KEY,
            thread_id INTEGER NOT NULL REFERENCES chat_threads(id) ON DELETE CASCADE,
            sender_role TEXT NOT NULL CHECK(sender_role IN (\'tester\', \'coordinator\')),
            recipient_role TEXT NOT NULL CHECK(recipient_role IN (\'tester\', \'coordinator\')),
            body TEXT NOT NULL,
            created_at TEXT NOT NULL,
            read_at TEXT,
            tester_deleted_at TEXT,
            coordinator_deleted_at TEXT
        );
        CREATE INDEX IF NOT EXISTS chat_messages_thread_recent ON chat_messages(thread_id, id DESC);
        CREATE TABLE IF NOT EXISTS chat_submission_limits (
            thread_id INTEGER NOT NULL REFERENCES chat_threads(id) ON DELETE CASCADE,
            sender_role TEXT NOT NULL CHECK(sender_role IN (\'tester\', \'coordinator\')),
            window_started_at INTEGER NOT NULL,
            submitted_count INTEGER NOT NULL,
            PRIMARY KEY(thread_id, sender_role)
        );
        CREATE TABLE IF NOT EXISTS administrator_login_limits (
            client_key TEXT PRIMARY KEY,
            failed_attempts INTEGER NOT NULL,
            locked_until INTEGER NOT NULL DEFAULT 0,
            updated_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS administrator_totp_uses (
            time_step INTEGER PRIMARY KEY,
            consumed_at TEXT NOT NULL
        );'
    );
    $emailBatchColumns = array_column($database->query('PRAGMA table_info(email_batches)')->fetchAll(), 'name');
    if (!in_array('body_html', $emailBatchColumns, true)) {
        $database->exec('ALTER TABLE email_batches ADD COLUMN body_html TEXT');
    }
    $assignmentColumns = array_column($database->query('PRAGMA table_info(tester_task_assignments)')->fetchAll(), 'name');
    if (!in_array('assignment_email_status', $assignmentColumns, true)) {
        $database->exec("ALTER TABLE tester_task_assignments ADD COLUMN assignment_email_status TEXT NOT NULL DEFAULT 'not_sent' CHECK(assignment_email_status IN ('not_sent', 'accepted', 'failed'))");
    }
    if (!in_array('assignment_email_attempted_at', $assignmentColumns, true)) {
        $database->exec('ALTER TABLE tester_task_assignments ADD COLUMN assignment_email_attempted_at TEXT');
    }
    if (!in_array('assignment_email_attempts', $assignmentColumns, true)) {
        $database->exec('ALTER TABLE tester_task_assignments ADD COLUMN assignment_email_attempts INTEGER NOT NULL DEFAULT 0');
    }
    $onboardingColumns = array_column($database->query('PRAGMA table_info(tester_onboarding)')->fetchAll(), 'name');
    if (!in_array('play_opt_in_confirmed_at', $onboardingColumns, true)) {
        $database->exec('ALTER TABLE tester_onboarding ADD COLUMN play_opt_in_confirmed_at TEXT');
    }
    if (!in_array('initial_smoke_test_confirmed_at', $onboardingColumns, true)) {
        $database->exec('ALTER TABLE tester_onboarding ADD COLUMN initial_smoke_test_confirmed_at TEXT');
    }
    if (!in_array('withdrawal_requested_at', $onboardingColumns, true)) {
        $database->exec('ALTER TABLE tester_onboarding ADD COLUMN withdrawal_requested_at TEXT');
    }
    if (!in_array('deletion_requested_at', $onboardingColumns, true)) {
        $database->exec('ALTER TABLE tester_onboarding ADD COLUMN deletion_requested_at TEXT');
    }
    if (!in_array('reviewed_at', $onboardingColumns, true)) {
        $database->exec('ALTER TABLE tester_onboarding ADD COLUMN reviewed_at TEXT');
    }
    if (!in_array('rejected_at', $onboardingColumns, true)) {
        $database->exec('ALTER TABLE tester_onboarding ADD COLUMN rejected_at TEXT');
    }
    $chatMessageColumns = array_column($database->query('PRAGMA table_info(chat_messages)')->fetchAll(), 'name');
    if (!in_array('recipient_role', $chatMessageColumns, true)) {
        $database->exec("ALTER TABLE chat_messages ADD COLUMN recipient_role TEXT NOT NULL DEFAULT 'tester' CHECK(recipient_role IN ('tester', 'coordinator'))");
        $database->exec("UPDATE chat_messages SET recipient_role = CASE sender_role WHEN 'tester' THEN 'coordinator' ELSE 'tester' END");
    }
    // Invitation and orientation are mail/archive events, not lifecycle gates.
    // Older records used status values for those events; normalize them while
    // preserving the underlying self-confirmation evidence.
    $database->exec("UPDATE tester_onboarding SET onboarding_status = CASE
        WHEN onboarding_status IN ('invited', 'orientation_sent')
             AND play_opt_in_confirmed_at IS NOT NULL AND play_opt_in_confirmed_at <> ''
             AND initial_smoke_test_confirmed_at IS NOT NULL AND initial_smoke_test_confirmed_at <> '' THEN 'ready'
        WHEN onboarding_status IN ('invited', 'orientation_sent') THEN 'profile_complete'
        ELSE onboarding_status END");
    $feedbackColumns = array_column($database->query('PRAGMA table_info(tester_feedback)')->fetchAll(), 'name');
    if (!in_array('category', $feedbackColumns, true)) {
        $database->exec("ALTER TABLE tester_feedback ADD COLUMN category TEXT NOT NULL DEFAULT 'other' CHECK(category IN ('playback', 'connection_recovery', 'station_switching', 'metadata_artwork', 'favorites', 'media_controls', 'audio_accessories', 'layout_accessibility', 'performance_battery', 'crash_freeze', 'other'))");
    }
    $testerColumns = array_column($database->query('PRAGMA table_info(testers)')->fetchAll(), 'name');
    $testerMigrations = [
        'primary_station' => 'TEXT',
        'other_stations_json' => "TEXT NOT NULL DEFAULT '[]'",
        'station_accounts_json' => "TEXT NOT NULL DEFAULT '[]'",
        'device_form_factor' => 'TEXT',
        'other_devices' => 'TEXT',
        'network_capabilities_json' => "TEXT NOT NULL DEFAULT '[]'",
        'audio_capabilities_json' => "TEXT NOT NULL DEFAULT '[]'",
        'accessibility_capabilities_json' => "TEXT NOT NULL DEFAULT '[]'",
        'testing_comfort' => 'TEXT',
        'controlled_actions_json' => "TEXT NOT NULL DEFAULT '[]'",
        'testing_availability' => 'TEXT',
        'recruitment_source' => "TEXT NOT NULL DEFAULT 'direct'",
    ];
    foreach ($testerMigrations as $column => $definition) {
        if (!in_array($column, $testerColumns, true)) {
            $database->exec("ALTER TABLE testers ADD COLUMN {$column} {$definition}");
        }
    }
    // Preserve the exact content and recipient outcomes of already-dispatched
    // coordinator batches before new mail types start using the common archive.
    $database->exec("INSERT OR IGNORE INTO tester_mail_archive (tester_id, batch_id, message_type, subject, body, body_html, handoff_status, prepared_at, attempted_at)
        SELECT recipients.tester_id, recipients.batch_id, 'batch', batches.subject, batches.body, COALESCE(batches.body_html, ''), recipients.delivery_status, batches.created_at, COALESCE(recipients.accepted_at, batches.dispatched_at, batches.created_at)
        FROM email_batch_recipients AS recipients
        JOIN email_batches AS batches ON batches.id = recipients.batch_id
        WHERE recipients.delivery_status IN ('accepted', 'failed')");
    return $database;
}

function startSession(): void
{
    ini_set('session.use_strict_mode', '1');
    ini_set('session.use_only_cookies', '1');
    ini_set('session.cookie_httponly', '1');
    ini_set('session.cookie_secure', '1');
    session_name(SESSION_NAME);
    session_set_cookie_params([
        'httponly' => true,
        'secure' => true,
        'samesite' => 'Strict',
        'path' => '/',
    ]);
    session_start();
}

function endSession(): void
{
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $cookie = session_get_cookie_params();
        setcookie(session_name(), '', [
            'expires' => time() - 42_000,
            'path' => $cookie['path'] ?? '/',
            'domain' => $cookie['domain'] ?? '',
            'secure' => true,
            'httponly' => true,
            'samesite' => 'Strict',
        ]);
    }
    session_destroy();
}

function administratorLoginClientKey(array $config): string
{
    $address = $_SERVER['REMOTE_ADDR'] ?? '';
    if (!is_string($address) || strlen($address) > 128) {
        $address = '';
    }
    // Keep the rate-limit key unlinkable to a raw address in the private DB.
    return hash_hmac('sha256', 'admin-login:' . $address, $config['admin_password_hash']);
}

function administratorLoginName(array $config): string
{
    $name = $config['admin_username'] ?? null;
    if (!is_string($name) || !preg_match('/^[A-Za-z0-9][A-Za-z0-9._@-]{2,63}$/', $name)) {
        fail(503, 'Administrator sign-in is not configured.');
    }
    return $name;
}

function administratorLoginNameAccepted(array $config, string $submitted): bool
{
    return strlen($submitted) <= ADMIN_LOGIN_NAME_MAX_LENGTH
        && hash_equals(administratorLoginName($config), $submitted);
}

function administratorTotpSecret(array $config): string
{
    $encoded = $config['admin_totp_secret'] ?? null;
    if (!is_string($encoded)) {
        fail(503, 'Administrator multi-factor authentication is not configured.');
    }
    $normalized = strtoupper(str_replace([' ', '-'], '', trim($encoded)));
    $normalized = rtrim($normalized, '=');
    if ($normalized === '' || !preg_match('/^[A-Z2-7]+$/', $normalized)) {
        fail(503, 'Administrator multi-factor authentication is not configured.');
    }
    $alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    $buffer = 0;
    $bits = 0;
    $secret = '';
    foreach (str_split($normalized) as $character) {
        $value = strpos($alphabet, $character);
        if ($value === false) {
            fail(503, 'Administrator multi-factor authentication is not configured.');
        }
        $buffer = ($buffer << 5) | $value;
        $bits += 5;
        while ($bits >= 8) {
            $bits -= 8;
            $secret .= chr(($buffer >> $bits) & 0xff);
        }
        $buffer = $bits === 0 ? 0 : $buffer & ((1 << $bits) - 1);
    }
    if (strlen($secret) < 16) {
        fail(503, 'Administrator multi-factor authentication is not configured.');
    }
    return $secret;
}

function administratorTotpCode(string $secret, int $timeStep): string
{
    $counter = pack('N2', intdiv($timeStep, 4_294_967_296), $timeStep % 4_294_967_296);
    $hash = hash_hmac('sha1', $counter, $secret, true);
    $offset = ord($hash[19]) & 0x0f;
    $binary = ((ord($hash[$offset]) & 0x7f) << 24)
        | (ord($hash[$offset + 1]) << 16)
        | (ord($hash[$offset + 2]) << 8)
        | ord($hash[$offset + 3]);
    return str_pad((string) ($binary % (10 ** ADMIN_TOTP_DIGITS)), ADMIN_TOTP_DIGITS, '0', STR_PAD_LEFT);
}

function administratorAcceptedTotpStep(string $code, string $secret, ?int $now = null): ?int
{
    if (!preg_match('/^[0-9]{6}$/', $code)) {
        return null;
    }
    $currentStep = intdiv($now ?? time(), ADMIN_TOTP_STEP_SECONDS);
    $accepted = null;
    for ($offset = -ADMIN_TOTP_ALLOWED_DRIFT_STEPS; $offset <= ADMIN_TOTP_ALLOWED_DRIFT_STEPS; $offset++) {
        $step = $currentStep + $offset;
        if (hash_equals(administratorTotpCode($secret, $step), $code)) {
            $accepted = $step;
        }
    }
    return $accepted;
}

function consumeAdministratorTotpStep(PDO $database, int $timeStep): bool
{
    $database->prepare('DELETE FROM administrator_totp_uses WHERE time_step < ?')->execute([$timeStep - 3]);
    $insert = $database->prepare('INSERT OR IGNORE INTO administrator_totp_uses(time_step, consumed_at) VALUES (?, ?)');
    $insert->execute([$timeStep, gmdate('c')]);
    return $insert->rowCount() === 1;
}

function administratorLoginIsLimited(PDO $database, array $config): bool
{
    $query = $database->prepare('SELECT locked_until FROM administrator_login_limits WHERE client_key = ?');
    $query->execute([administratorLoginClientKey($config)]);
    $lockedUntil = $query->fetchColumn();
    return $lockedUntil !== false && (int) $lockedUntil > time();
}

function recordAdministratorLoginFailure(PDO $database, array $config): void
{
    $key = administratorLoginClientKey($config);
    $query = $database->prepare('SELECT failed_attempts FROM administrator_login_limits WHERE client_key = ?');
    $query->execute([$key]);
    $attempts = ((int) ($query->fetchColumn() ?: 0)) + 1;
    $lockSeconds = $attempts <= ADMIN_LOGIN_FREE_FAILURES
        ? 0
        : min(ADMIN_LOGIN_MAX_LOCK_SECONDS, 60 * (2 ** min(6, $attempts - ADMIN_LOGIN_FREE_FAILURES - 1)));
    $database->prepare('INSERT INTO administrator_login_limits(client_key, failed_attempts, locked_until, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(client_key) DO UPDATE SET failed_attempts = excluded.failed_attempts, locked_until = excluded.locked_until, updated_at = excluded.updated_at')
        ->execute([$key, $attempts, time() + $lockSeconds, gmdate('c')]);
}

function clearAdministratorLoginFailures(PDO $database, array $config): void
{
    $database->prepare('DELETE FROM administrator_login_limits WHERE client_key = ?')->execute([administratorLoginClientKey($config)]);
}

function administratorRequestHost(): string
{
    return strtolower(trim((string) ($_SERVER['HTTP_HOST'] ?? '')));
}

function administratorMfaRequired(): bool
{
    return !hash_equals(STAGING_ADMIN_LOGIN_HOST, administratorRequestHost());
}

function administratorLoginOrigin(): string
{
    return administratorMfaRequired() ? ADMIN_LOGIN_ORIGIN : STAGING_ADMIN_LOGIN_ORIGIN;
}

function administratorLoginRequestIsSameOrigin(): bool
{
    $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
    if (is_string($origin) && $origin !== '') {
        return hash_equals(administratorLoginOrigin(), $origin);
    }
    $referer = $_SERVER['HTTP_REFERER'] ?? '';
    return !is_string($referer) || $referer === '' || str_starts_with($referer, administratorLoginOrigin() . '/');
}

function csrf(): string
{
    if (!isset($_SESSION['csrf']) || !is_string($_SESSION['csrf'])) {
        $_SESSION['csrf'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf'];
}

function validCsrf(): bool
{
    return isset($_POST['csrf'], $_SESSION['csrf'])
        && is_string($_POST['csrf'])
        && is_string($_SESSION['csrf'])
        && hash_equals($_SESSION['csrf'], $_POST['csrf']);
}

function privateResponseHeaders(): void
{
    header("Content-Security-Policy: default-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'; img-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");
    header('Permissions-Policy: camera=(), geolocation=(), microphone=(), payment=(), usb=()');
    header('Referrer-Policy: no-referrer');
    header('X-Content-Type-Options: nosniff');
}

function e(?string $value): string
{
    return htmlspecialchars($value ?? '', ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function redirect(string $location = ''): never
{
    header('Location: /private-tester-queue.php' . $location, true, 303);
    exit;
}

function requireAuthentication(array $config): void
{
    if (($_SESSION['authenticated'] ?? false) !== true) {
        renderLogin();
        exit;
    }
    $now = time();
    $authenticatedAt = $_SESSION['authenticated_at'] ?? null;
    $lastSeenAt = $_SESSION['last_seen_at'] ?? null;
    if (!is_int($authenticatedAt) || !is_int($lastSeenAt)
        || $authenticatedAt > $now || $lastSeenAt > $now
        || $now - $authenticatedAt > ADMIN_SESSION_MAX_SECONDS
        || $now - $lastSeenAt > ADMIN_SESSION_IDLE_SECONDS) {
        endSession();
        renderLogin('Your session has expired. Please sign in again.');
    }
    $_SESSION['last_seen_at'] = $now;
    if (password_needs_rehash($config['admin_password_hash'], PASSWORD_DEFAULT)) {
        // The hash remains private; surface no detail to a browser.
        error_log('private-tester-queue password hash should be refreshed');
    }
}

function renderPage(string $title, string $content): never
{
    header('Content-Type: text/html; charset=UTF-8');
    privateResponseHeaders();
    header('Cache-Control: no-store, private');
    header('Pragma: no-cache');
    header('X-Robots-Tag: noindex, nofollow, noarchive');
    header('X-Frame-Options: DENY');
    $activeWorkspace = str_starts_with($title, 'Live Chat') ? 'chat' : (str_contains($title, 'email') ? 'email' : 'operations');
    $operationsClass = $activeWorkspace === 'operations' ? ' active' : '';
    $chatClass = $activeWorkspace === 'chat' ? ' active' : '';
    $emailClass = $activeWorkspace === 'email' ? ' active' : '';
    echo '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>'
        . e($title) . '</title><style>'
        . 'body{margin:0;background:#090c15;color:#f7f4ec;font:16px/1.6 system-ui,-apple-system,"Segoe UI",sans-serif;-webkit-font-smoothing:antialiased}
.shell{max-width:72rem;margin:2.5rem auto;padding:0 1.25rem}
h1,h2{line-height:1.2;letter-spacing:-.01em}
h1{font-size:1.7rem;margin:.2rem 0 .4rem}
h2{font-size:1.15rem;margin:0 0 .6rem}
section{margin:1.25rem 0;padding:1.4rem 1.5rem;border:1px solid #232c40;border-radius:1rem;background:#111624;box-shadow:0 1px 2px rgba(0,0,0,.25)}
.muted{color:#b7bdca}
.notice{padding:.8rem 1rem;border-radius:.6rem;background:#173631;border:1px solid #235349}
.error{background:#4a2229;border-color:#7a3441}
.warning{padding:.65rem .85rem;border-radius:.4rem;border-left:3px solid #ffcb6b;background:#302719}
.visually-hidden{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
label{display:block;margin:.85rem 0 .35rem;font-weight:700;font-size:.92rem}
input,textarea,select,.editor{box-sizing:border-box;width:100%;padding:.65rem .75rem;border:1px solid #59647a;border-radius:.5rem;background:#fff;color:#182033;font:inherit}
input:focus,textarea:focus,select:focus{outline:2px solid #67e6d1;outline-offset:1px}
input[type="checkbox"]{width:auto;padding:0;accent-color:#67e6d1}
textarea{min-height:12rem}
.toolbar{display:flex;flex-wrap:wrap;gap:.4rem;margin:.35rem 0}
.toolbar button{margin:0;padding:.35rem .6rem;background:#e8edf5;color:#182033;font-size:.86rem}
.editor{min-height:14rem;overflow:auto}
.editor:focus{outline:3px solid #67e6d1;outline-offset:3px}
.editor:empty:before{color:#58647a;content:attr(data-placeholder);pointer-events:none}
.editor p:first-child{margin-top:0}
.editor p:last-child{margin-bottom:0}
a{color:#d29cff;text-decoration:none;font-weight:600}
a:hover{text-decoration:underline}
a:focus-visible{outline:2px solid #67e6d1;outline-offset:2px;border-radius:.2rem}
.link-button{display:inline-block;margin:.2rem .5rem .2rem 0;padding:.4rem .75rem;border:1px solid #3b4964;border-radius:.5rem;background:#161c2c;font-size:.84rem;font-weight:600}
.link-button:last-child{margin-right:0}
.link-button:hover{background:#1d2438;text-decoration:none}
button{margin-top:1rem;padding:.65rem 1.1rem;border:0;border-radius:.55rem;background:#67e6d1;color:#071411;font:700 .96rem/1 system-ui;cursor:pointer;transition:filter .12s ease}
button:hover{filter:brightness(1.07)}
button:focus-visible{outline:2px solid #f7f4ec;outline-offset:2px}
button.secondary{margin-left:.5rem;background:#d29cff;color:#22102f}
button.danger{background:#ff6b81;color:#31060d}
table{width:100%;border-collapse:collapse}
th,td{padding:.65rem .5rem;text-align:left;border-bottom:1px solid #232c40;vertical-align:top}
thead th{color:#9aa3b8;font-size:.78rem;text-transform:uppercase;letter-spacing:.05em;font-weight:700}
th:first-child,td:first-child{width:2.4rem}
.table-scroll{overflow-x:auto}
small{color:#b7bdca}
.actions{display:flex;gap:.6rem;flex-wrap:wrap}
.actions form{margin:0}
.dashboard-header{display:flex;justify-content:space-between;align-items:flex-start;gap:1rem;flex-wrap:wrap;margin-bottom:1.75rem}
.dashboard-header form{margin:0}
.dashboard-header h1{margin:.1rem 0 .4rem}
.eyebrow,.product-mark{margin:0 0 .35rem;color:#9fe6d8;text-transform:uppercase;font-size:.76rem;font-weight:700;letter-spacing:.09em}
.login{max-width:26rem;margin:3rem auto}
.onboarding-overview{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.75rem}
.onboarding-metric{padding:1rem;border:1px solid #2b3448;border-radius:.7rem;background:#0c101c}
.onboarding-metric strong{display:block;font-size:1.8rem;line-height:1.15;color:#67e6d1}
.onboarding-metric span{color:#b7bdca;font-size:.86rem}
.onboarding-metric.stage-pending_review strong{color:#ffcb6b}
.onboarding-metric.stage-accepted strong{color:#7fb2ff}
.onboarding-metric.stage-invited strong{color:#d29cff}
.onboarding-metric.stage-active strong{color:#67e6d1}
.applications-card{border-color:#3d3320;background:#161307}
.application-rows{display:flex;flex-direction:column;gap:.75rem;margin-top:.75rem}
.application-row{display:flex;justify-content:space-between;align-items:flex-start;gap:1rem;flex-wrap:wrap;padding:.9rem 1rem;border:1px solid #3d3d2c;border-radius:.65rem;background:#0f0d06}
.application-summary strong{font-size:1rem}
.application-actions{display:flex;flex-wrap:wrap;align-items:flex-start;gap:.5rem}
.application-actions form{margin:0}
.application-actions button{margin-top:0}
.reject-disclosure{margin:0}
.reject-disclosure summary{cursor:pointer;padding:.65rem 1.1rem;border-radius:.55rem;background:#2c1620;color:#ffb3c0;font-weight:700;font-size:.94rem;list-style:none}
.reject-disclosure summary::-webkit-details-marker{display:none}
.reject-disclosure[open] summary{border-radius:.55rem .55rem 0 0}
.reject-disclosure form{padding:.85rem;border:1px solid #3d2029;border-top:0;border-radius:0 0 .55rem .55rem;background:#150c10}
.reject-disclosure button{margin-top:.5rem}
.compose-card{margin:1.25rem 0;padding:1.1rem 1.4rem 1.4rem;border:1px solid #232c40;border-radius:1rem;background:#111624}
.compose-card summary{cursor:pointer;font-size:1.05rem;padding:.4rem 0;list-style:none}
.compose-card summary::-webkit-details-marker{display:none}
.compose-card summary:before{content:"▸ ";color:#67e6d1}
.compose-card[open] summary:before{content:"▾ "}
.compose-card section{border:0;padding:0;margin:.75rem 0 0;background:transparent;box-shadow:none}
.tester-task-panels{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}
.tester-task-panel,.assignment-existing,.onboarding-card{min-width:0;padding:1rem;border:1px solid #232c40;border-radius:.65rem}
.assignment-existing,.onboarding-card{margin:1rem 0;background:#0c101c}
.assignment-existing h4,.tester-task-panel h3,.onboarding-card h4{margin:.1rem 0}
.assignment-existing textarea,.tester-task-panel textarea,.onboarding-card textarea{min-height:5rem}
.inline-form{margin-top:.5rem}
.onboarding-status,.stage-badge{display:inline-block;padding:.2rem .6rem;border-radius:99px;font-size:.78rem;font-weight:700;letter-spacing:.01em}
.status-profile_pending{background:rgba(255,203,107,.16);color:#ffcb6b}
.status-profile_complete{background:rgba(127,178,255,.16);color:#7fb2ff}
.status-invited{background:rgba(210,156,255,.16);color:#d29cff}
.status-orientation_sent{background:rgba(79,209,197,.16);color:#4fd1c5}
.status-ready{background:rgba(103,230,209,.16);color:#67e6d1}
.status-paused{background:rgba(139,147,167,.2);color:#c3c9d6}
.status-unknown{background:rgba(183,189,202,.16);color:#b7bdca}
.stage-badge.stage-pending_review{background:rgba(255,203,107,.16);color:#ffcb6b}
.stage-badge.stage-accepted{background:rgba(127,178,255,.16);color:#7fb2ff}
.stage-badge.stage-invited{background:rgba(210,156,255,.16);color:#d29cff}
.stage-badge.stage-active{background:rgba(103,230,209,.16);color:#67e6d1}
.stage-badge.stage-unknown{background:rgba(183,189,202,.16);color:#b7bdca}
.profile-missing{margin:.65rem 0;padding:.65rem;border-left:3px solid #ffcb6b;background:#302719}
.profile-complete{margin:.65rem 0;padding:.65rem;border-left:3px solid #67e6d1;background:#173631}
.mutation-authorization{padding:.65rem;border-left:3px solid #ffcb6b;background:#302719}
.task-preview{min-height:3rem;padding:.75rem;border:1px solid #232c40;border-radius:.5rem;color:#b7bdca}
.task-preview strong{color:#f7f4ec}
.copy-assignment{margin-left:.5rem}
@media(max-width:44rem){table{font-size:.86rem}.optional{display:none}.onboarding-overview,.tester-task-panels{grid-template-columns:1fr}.shell{margin:1rem auto}.copy-assignment{margin-left:0}.dashboard-header{flex-direction:column}.application-row{flex-direction:column}.application-actions{width:100%}}
</style><link rel="stylesheet" href="/assets/onboarding-portal.css"></head><body><a class="skip-link" href="#workspace">Skip to workspace</a><div class="app-shell"><aside class="global-rail" aria-label="Coordinator workspace navigation"><a class="brand-mark" href="/" aria-label="24Seven.FM Player home"><img src="/assets/project/app-icon.png" alt=""></a><nav><a class="rail-button' . $operationsClass . '" href="/private-tester-queue.php" aria-label="Operations workspace">▦<span>Operations</span></a><a class="rail-button' . $chatClass . '" href="/private-tester-queue.php?live_chat=1" aria-label="Live Chat workspace">◌<span>Live Chat</span></a><a class="rail-button' . $emailClass . '" href="/private-tester-queue.php?email=1" aria-label="Email workspace">✉<span>Email</span></a></nav></aside><main id="workspace" class="desktop">'
        . $content . '</main></div></body></html>';
    exit;
}

function renderLogin(string $error = ''): never
{
    $message = $error === '' ? '' : '<p class="notice error">' . e($error) . '</p>';
    $mfaFields = administratorMfaRequired()
        ? '<label for="totp_code">Authenticator code</label><input id="totp_code" name="totp_code" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" required>'
        : '';
    $mfaNotice = administratorMfaRequired()
        ? 'Use the current six-digit code from your authenticator app. Sessions expire automatically; repeated unsuccessful attempts are temporarily limited.'
        : 'Staging coordinator sign-in uses the administrator password only. Sessions expire automatically; repeated unsuccessful attempts are temporarily limited.';
    renderPage('Administrator sign in · 24Seven.FM Player', '<section class="login"><p class="product-mark">24Seven.FM Player</p><h1>Administrator sign in</h1><p class="muted">Private coordinator access only. Use your administrator password on a trusted device.</p>' . $message
        . '<form method="post" action="/private-tester-queue.php"><input type="hidden" name="action" value="login"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label for="username">Administrator login name</label><input id="username" name="username" type="text" autocomplete="username" autocapitalize="none" spellcheck="false" maxlength="' . ADMIN_LOGIN_NAME_MAX_LENGTH . '" required autofocus><label for="password">Administrator password</label><input id="password" name="password" type="password" autocomplete="current-password" maxlength="' . ADMIN_LOGIN_MAX_PASSWORD_BYTES . '" required>' . $mfaFields . '<button type="submit">Sign in securely</button></form><p class="muted small">' . e($mfaNotice) . '</p></section>');
}

function field(string $name, int $maximum, bool $required = true, bool $singleLine = false): string
{
    $value = trim((string) ($_POST[$name] ?? ''));
    if (($required && $value === '') || textLength($value) > $maximum || str_contains($value, "\0") || ($singleLine && (str_contains($value, "\r") || str_contains($value, "\n")))) {
        throw new InvalidArgumentException('Please check the email subject and body.');
    }
    return $value;
}

function textLength(string $value): int
{
    return function_exists('mb_strlen') ? mb_strlen($value) : strlen($value);
}

function taskRegistry(): array
{
    static $tasks = null;
    if (is_array($tasks)) {
        return $tasks;
    }
    $path = __DIR__ . '/' . TASK_REGISTRY_FILE;
    // The deploy artifact receives the registry under assets/. Local source
    // validation intentionally reads the same canonical data file directly.
    if (!is_file($path)) {
        $path = __DIR__ . '/_data/tester_tasks.json';
    }
    $decoded = is_file($path) ? json_decode((string) file_get_contents($path), true) : null;
    if (!is_array($decoded) || !isset($decoded['tasks']) || !is_array($decoded['tasks'])) {
        throw new RuntimeException('The Tester Task registry is unavailable.');
    }
    $tasks = [];
    foreach ($decoded['tasks'] as $task) {
        if (!is_array($task) || !isset($task['id'], $task['title'], $task['ptIds'], $task['state'], $task['prerequisites'], $task['mutation'], $task['safetyWarning'])
            || !is_string($task['id']) || !is_array($task['ptIds']) || !is_array($task['mutation'])) {
            throw new RuntimeException('The Tester Task registry is invalid.');
        }
        $tasks[$task['id']] = $task;
    }
    if (count($tasks) !== 23) {
        throw new RuntimeException('The Tester Task registry is incomplete.');
    }
    return $tasks;
}

function activeTester(PDO $database, int $testerId): void
{
    $statement = $database->prepare("SELECT id FROM testers WHERE id = ? AND status = 'active'");
    $statement->execute([$testerId]);
    if ($statement->fetch() === false) {
        throw new InvalidArgumentException('The tester is no longer active. Refresh the queue and try again.');
    }
}

/**
 * Portal chat is intentionally separate from station and Player messaging.
 * Every thread belongs to exactly one active tester; coordinators select that
 * tester through their protected workspace and never supply an arbitrary
 * recipient from the browser.
 */
function chatThreadForTester(PDO $database, int $testerId): int
{
    activeTester($database, $testerId);
    $statement = $database->prepare('SELECT id FROM chat_threads WHERE tester_id = ?');
    $statement->execute([$testerId]);
    $id = $statement->fetchColumn();
    if ($id !== false) return (int) $id;
    $now = gmdate('c');
    $database->prepare('INSERT INTO chat_threads(tester_id, created_at, updated_at) VALUES (?, ?, ?) ON CONFLICT(tester_id) DO NOTHING')
        ->execute([$testerId, $now, $now]);
    $statement->execute([$testerId]);
    $id = $statement->fetchColumn();
    if ($id === false) throw new RuntimeException('The chat thread is unavailable.');
    return (int) $id;
}

function chatMessageBody(string $value): string
{
    $value = trim(str_replace(["\r\n", "\r"], "\n", $value));
    $length = function_exists('mb_strlen') ? mb_strlen($value) : strlen($value);
    if ($value === '' || $length > CHAT_MAX_MESSAGE_LENGTH || str_contains($value, "\0") || !preg_match('//u', $value)) {
        throw new InvalidArgumentException('Write a private message of no more than 2,000 characters.');
    }
    return $value;
}

function chatPurgeExpired(PDO $database): void
{
    $cutoff = gmdate('c', time() - CHAT_RETENTION_DAYS * 86_400);
    $database->prepare('DELETE FROM chat_messages WHERE created_at < ? OR (tester_deleted_at IS NOT NULL AND coordinator_deleted_at IS NOT NULL)')->execute([$cutoff]);
    $database->prepare('DELETE FROM chat_threads WHERE last_message_at IS NOT NULL AND last_message_at < ? AND NOT EXISTS (SELECT 1 FROM chat_messages WHERE chat_messages.thread_id = chat_threads.id)')->execute([$cutoff]);
}

function chatConsumeSubmissionLimit(PDO $database, int $threadId, string $senderRole): void
{
    if (!in_array($senderRole, ['tester', 'coordinator'], true)) throw new InvalidArgumentException('The chat sender is invalid.');
    $now = time();
    $database->beginTransaction();
    try {
        $statement = $database->prepare('SELECT window_started_at, submitted_count FROM chat_submission_limits WHERE thread_id = ? AND sender_role = ?');
        $statement->execute([$threadId, $senderRole]);
        $limit = $statement->fetch();
        if (!is_array($limit) || $now - (int) $limit['window_started_at'] >= CHAT_SUBMISSION_WINDOW_SECONDS) {
            $database->prepare('INSERT INTO chat_submission_limits(thread_id, sender_role, window_started_at, submitted_count) VALUES (?, ?, ?, 1) ON CONFLICT(thread_id, sender_role) DO UPDATE SET window_started_at = excluded.window_started_at, submitted_count = excluded.submitted_count')
                ->execute([$threadId, $senderRole, $now]);
        } elseif ((int) $limit['submitted_count'] >= CHAT_SUBMISSION_MAXIMUM) {
            throw new InvalidArgumentException('Please wait a moment before sending another chat message.');
        } else {
            $database->prepare('UPDATE chat_submission_limits SET submitted_count = submitted_count + 1 WHERE thread_id = ? AND sender_role = ?')->execute([$threadId, $senderRole]);
        }
        $database->commit();
    } catch (Throwable $exception) {
        if ($database->inTransaction()) $database->rollBack();
        throw $exception;
    }
}

function chatPostMessage(PDO $database, int $testerId, string $senderRole, string $body): int
{
    if (!in_array($senderRole, ['tester', 'coordinator'], true)) {
        throw new InvalidArgumentException('The chat sender is invalid.');
    }
    $body = chatMessageBody($body);
    chatPurgeExpired($database);
    $threadId = chatThreadForTester($database, $testerId);
    chatConsumeSubmissionLimit($database, $threadId, $senderRole);
    $now = gmdate('c');
    $recipientRole = $senderRole === 'tester' ? 'coordinator' : 'tester';
    $database->prepare('INSERT INTO chat_messages(thread_id, sender_role, recipient_role, body, created_at) VALUES (?, ?, ?, ?, ?)')->execute([$threadId, $senderRole, $recipientRole, $body, $now]);
    $database->prepare('UPDATE chat_threads SET updated_at = ?, last_message_at = ?, tester_deleted_at = NULL, coordinator_deleted_at = NULL WHERE id = ?')->execute([$now, $now, $threadId]);
    return (int) $database->lastInsertId();
}

function chatMessages(PDO $database, int $testerId, string $viewerRole, int $afterId = 0): array
{
    if (!in_array($viewerRole, ['tester', 'coordinator'], true)) throw new InvalidArgumentException('The chat viewer is invalid.');
    chatPurgeExpired($database);
    $threadId = chatThreadForTester($database, $testerId);
    $deletedColumn = $viewerRole === 'tester' ? 'tester_deleted_at' : 'coordinator_deleted_at';
    $statement = $database->prepare("SELECT id, sender_role, recipient_role, body, created_at, read_at FROM chat_messages WHERE thread_id = ? AND id > ? AND {$deletedColumn} IS NULL ORDER BY id ASC LIMIT 100");
    $statement->execute([$threadId, max(0, $afterId)]);
    $messages = $statement->fetchAll();
    $database->prepare('UPDATE chat_messages SET read_at = ? WHERE thread_id = ? AND recipient_role = ? AND read_at IS NULL')->execute([gmdate('c'), $threadId, $viewerRole]);
    return $messages;
}

function chatSoftDeleteMessage(PDO $database, int $testerId, string $viewerRole, int $messageId): void
{
    if (!in_array($viewerRole, ['tester', 'coordinator'], true) || $messageId < 1) throw new InvalidArgumentException('The chat message is invalid.');
    $threadId = chatThreadForTester($database, $testerId);
    $column = $viewerRole === 'tester' ? 'tester_deleted_at' : 'coordinator_deleted_at';
    $database->prepare("UPDATE chat_messages SET {$column} = COALESCE({$column}, ?) WHERE id = ? AND thread_id = ?")->execute([gmdate('c'), $messageId, $threadId]);
    chatPurgeExpired($database);
}

function listFromJson(?string $value): array
{
    $decoded = json_decode((string) $value, true);
    if (!is_array($decoded)) {
        return [];
    }
    return array_values(array_filter($decoded, static fn (mixed $item): bool => is_string($item) && $item !== ''));
}

function profileSummary(array $tester): array
{
    $required = [
        'primary_station' => 'primary station',
        'device_form_factor' => 'Android device type',
        'network_capabilities_json' => 'network-testing preferences',
        'audio_capabilities_json' => 'audio/accessory preferences',
        'accessibility_capabilities_json' => 'accessibility or alternative-input preferences',
        'testing_comfort' => 'testing comfort',
        'controlled_actions_json' => 'controlled-test preferences',
        'testing_availability' => 'two-week availability',
    ];
    $missing = [];
    foreach ($required as $column => $label) {
        $value = $tester[$column] ?? null;
        $provided = str_ends_with($column, '_json')
            ? listFromJson(is_string($value) ? $value : null) !== []
            : is_string($value) && trim($value) !== '';
        if (!$provided) {
            $missing[] = $label;
        }
    }
    return ['complete' => $missing === [], 'missing' => $missing];
}

function isGuestTester(array $tester): bool
{
    $accounts = listFromJson(is_string($tester['station_accounts_json'] ?? null) ? $tester['station_accounts_json'] : null);
    return $accounts === [] || $accounts === ['none'];
}

function taskAllowsGuestTester(array $task): bool
{
    return ($task['guestEligible'] ?? false) === true;
}

function onboardingStatus(array $tester): string
{
    $profile = profileSummary($tester);
    if (!$profile['complete']) {
        return 'profile_pending';
    }
    $optedIn = is_string($tester['play_opt_in_confirmed_at'] ?? null) && $tester['play_opt_in_confirmed_at'] !== '';
    $smokeTested = is_string($tester['initial_smoke_test_confirmed_at'] ?? null) && $tester['initial_smoke_test_confirmed_at'] !== '';
    $status = $tester['onboarding_status'] ?? 'profile_complete';
    if ($status === 'paused') return 'paused';
    if ($optedIn && $smokeTested) return 'ready';
    return $status === 'profile_pending' ? 'profile_pending' : 'profile_complete';
}

function testerReadyForAssignment(array $tester): bool
{
    return onboardingStatus($tester) === 'ready';
}

/**
 * Keep the persisted coordinator lifecycle aligned with the tester's current
 * profile evidence. A completed profile advances only the profile stage; it
 * never infers Play opt-in, smoke-test completion, or an active assignment.
 */
function synchronizeOnboardingProfile(PDO $database, int $testerId): string
{
    $statement = $database->prepare("SELECT testers.*, onboarding.onboarding_status, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
    $statement->execute([$testerId]);
    $tester = $statement->fetch();
    if ($tester === false) {
        throw new InvalidArgumentException('The tester is no longer active.');
    }
    $now = gmdate('c');
    $database->prepare('INSERT OR IGNORE INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempted_at, orientation_email_attempts, updated_at) VALUES (?, \'profile_pending\', \'\', \'not_sent\', NULL, 0, ?)')
        ->execute([$testerId, $now]);
    $profileComplete = profileSummary($tester)['complete'];
    if ($profileComplete) {
        $target = testerReadyForAssignment($tester) ? 'ready' : 'profile_complete';
        $database->prepare("UPDATE tester_onboarding SET onboarding_status = CASE WHEN onboarding_status = 'paused' THEN 'paused' ELSE ? END, updated_at = ? WHERE tester_id = ?")
            ->execute([$target, $now, $testerId]);
        $status = $database->prepare('SELECT onboarding_status FROM tester_onboarding WHERE tester_id = ?');
        $status->execute([$testerId]);
        return (string) $status->fetchColumn();
    }
    $database->prepare("UPDATE tester_onboarding SET onboarding_status = CASE WHEN onboarding_status = 'paused' THEN 'paused' ELSE 'profile_pending' END, updated_at = ? WHERE tester_id = ?")
        ->execute([$now, $testerId]);
    return 'profile_pending';
}

function recordTesterPlayOptIn(PDO $database, int $testerId): bool
{
    $statement = $database->prepare("SELECT testers.*, onboarding.initial_smoke_test_confirmed_at FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
    $statement->execute([$testerId]);
    $tester = $statement->fetch();
    if ($tester === false) {
        throw new InvalidArgumentException('The tester is no longer active.');
    }
    if (!profileSummary($tester)['complete']) {
        throw new InvalidArgumentException('Save your complete profile and device details before confirming opt-in.');
    }
    $now = gmdate('c');
    $ready = is_string($tester['initial_smoke_test_confirmed_at'] ?? null) && $tester['initial_smoke_test_confirmed_at'] !== '';
    $database->prepare("INSERT INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempts, updated_at, play_opt_in_confirmed_at) VALUES (?, ?, '', 'not_sent', 0, ?, ?) ON CONFLICT(tester_id) DO UPDATE SET onboarding_status = excluded.onboarding_status, play_opt_in_confirmed_at = excluded.play_opt_in_confirmed_at, updated_at = excluded.updated_at")
        ->execute([$testerId, $ready ? 'ready' : 'profile_complete', $now, $now]);
    return $ready;
}

function recordTesterInitialSmokeTest(PDO $database, int $testerId): void
{
    $statement = $database->prepare("SELECT testers.*, onboarding.play_opt_in_confirmed_at FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
    $statement->execute([$testerId]);
    $tester = $statement->fetch();
    if ($tester === false) {
        throw new InvalidArgumentException('The tester is no longer active.');
    }
    if (!profileSummary($tester)['complete']) {
        throw new InvalidArgumentException('Save your complete profile and device details before confirming the initial smoke test.');
    }
    if (!is_string($tester['play_opt_in_confirmed_at'] ?? null) || $tester['play_opt_in_confirmed_at'] === '') {
        throw new InvalidArgumentException('Record your Google Play opt-in before confirming the initial smoke test.');
    }
    $now = gmdate('c');
    $database->prepare("INSERT INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempts, updated_at, play_opt_in_confirmed_at, initial_smoke_test_confirmed_at) VALUES (?, 'ready', '', 'not_sent', 0, ?, ?, ?) ON CONFLICT(tester_id) DO UPDATE SET onboarding_status = 'ready', initial_smoke_test_confirmed_at = excluded.initial_smoke_test_confirmed_at, updated_at = excluded.updated_at")
        ->execute([$testerId, $now, $tester['play_opt_in_confirmed_at'], $now]);
}

function onboardingStatusLabel(string $status): string
{
    return match ($status) {
        'profile_pending' => 'Profile update needed',
        'profile_complete' => 'Profile complete',
        'ready' => 'Ready for assignment',
        'paused' => 'Paused',
        default => 'Unknown',
    };
}

function recruitmentSourceLabel(?string $source): string
{
    return RECRUITMENT_SOURCE_LABELS[$source ?? 'direct'] ?? RECRUITMENT_SOURCE_LABELS['other'];
}

/**
 * The four coordinator-facing application stages. These are computed, not
 * stored: they combine the coordinator's review action (reviewed_at) with
 * the existing onboarding_status lifecycle so a newly submitted or imported
 * application is never mistaken for one a coordinator has already actioned.
 *
 * pending_review  — not yet accepted, sent a details request, or rejected.
 * accepted        — reviewed and collecting the Profile & Device / tester
 *                    self-confirmation evidence. Mail activity is reported
 *                    separately and does not advance this stage.
 * active          — self-confirmed and ready for focused assignment.
 */
function applicationStage(array $tester): string
{
    if (($tester['reviewed_at'] ?? null) === null || $tester['reviewed_at'] === '') {
        return 'pending_review';
    }
    $status = onboardingStatus($tester);
    if ($status === 'ready') {
        return 'active';
    }
    return 'accepted';
}

function applicationStageLabel(string $stage): string
{
    return match ($stage) {
        'pending_review' => 'Pending review',
        'accepted' => 'Accepted, needs details',
        'active' => 'Active',
        default => 'Unknown',
    };
}

function applicationStageBadgeClass(string $stage): string
{
    return 'stage-badge stage-' . (in_array($stage, APPLICATION_STAGES, true) ? $stage : 'unknown');
}

function onboardingStatusBadgeClass(string $status): string
{
    return 'onboarding-status status-' . (in_array($status, ONBOARDING_STATUSES, true) ? $status : 'unknown');
}

/**
 * Mark an application as actioned by a coordinator (accept, request details,
 * or reject all pass through here). Idempotent: an already-reviewed record
 * keeps its original review timestamp.
 */
function markApplicationReviewed(PDO $database, int $testerId): void
{
    $now = gmdate('c');
    $database->prepare('INSERT OR IGNORE INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempted_at, orientation_email_attempts, updated_at) VALUES (?, \'profile_pending\', \'\', \'not_sent\', NULL, 0, ?)')
        ->execute([$testerId, $now]);
    $database->prepare('UPDATE tester_onboarding SET reviewed_at = COALESCE(reviewed_at, ?) WHERE tester_id = ?')
        ->execute([$now, $testerId]);
}

function onboardingInput(array $tester): array
{
    $status = (string) ($_POST['onboarding_status'] ?? '');
    if (!in_array($status, ONBOARDING_STATUSES, true)) {
        throw new InvalidArgumentException('Choose a valid onboarding status.');
    }
    $profile = profileSummary($tester);
    if (!$profile['complete'] && $status !== 'profile_pending') {
        throw new InvalidArgumentException('This tester needs their profile update before advancing onboarding.');
    }
    if ($status === 'ready' && (!is_string($tester['play_opt_in_confirmed_at'] ?? null) || $tester['play_opt_in_confirmed_at'] === '' || !is_string($tester['initial_smoke_test_confirmed_at'] ?? null) || $tester['initial_smoke_test_confirmed_at'] === '')) {
        throw new InvalidArgumentException('Ready is recorded automatically after both the Play opt-in and first-use smoke-test confirmations.');
    }
    return [$status, field('onboarding_note', MAX_ONBOARDING_NOTE_LENGTH, false)];
}

function saveOnboarding(PDO $database, int $testerId, string $status, string $note, ?string $emailStatus = null): void
{
    $existing = $database->prepare('SELECT orientation_email_status, orientation_email_attempted_at, orientation_email_attempts FROM tester_onboarding WHERE tester_id = ?');
    $existing->execute([$testerId]);
    $record = $existing->fetch();
    $orientationStatus = $emailStatus ?? (is_array($record) ? $record['orientation_email_status'] : 'not_sent');
    $attemptedAt = is_array($record) ? $record['orientation_email_attempted_at'] : null;
    $attempts = is_array($record) ? (int) $record['orientation_email_attempts'] : 0;
    $statement = $database->prepare('INSERT INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempted_at, orientation_email_attempts, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(tester_id) DO UPDATE SET onboarding_status = excluded.onboarding_status, coordinator_note = excluded.coordinator_note, orientation_email_status = excluded.orientation_email_status, orientation_email_attempted_at = excluded.orientation_email_attempted_at, orientation_email_attempts = excluded.orientation_email_attempts, updated_at = excluded.updated_at');
    $statement->execute([$testerId, $status, $note, $orientationStatus, $attemptedAt, $attempts, gmdate('c')]);
}

function onboardingMessage(array $tester): string
{
    return 'Hi ' . $tester['display_name'] . ",\n\n"
        . "Welcome to the 24Seven.FM Player Google Play Closed Test. The Player is an independently developed, unofficial player for the 24Seven.FM network of internet radio stations. Your tester profile is complete, and you are ready for the next step.\n\n"
        . "1. Join the Android test from the project’s Google Play testing link when your Play access is granted.\n"
        . "2. Open your tester portal at https://player.jamesjennison.net/tester-portal.php to keep your device profile current and submit focused task reports.\n"
        . "3. Install or update 24Seven.FM Player, then complete the first-run checklist at https://player.jamesjennison.net/product-testing/.\n"
        . "4. Complete the short first-use smoke check in your tester portal, then use only the focused Tester Tasks you are assigned. Each task has its own safety boundary and needs one result per PT case.\n"
        . "5. Send results through the portal or linked feedback form, including the app version, device, Android version, station, steps, and outcome.\n\n"
        . "You need your own Google account only to opt in to Google Play. Guest testing does not require a 24Seven.FM station account; do not create, use, or share one for this program.\n\n"
        . "Please do not send passwords, usernames, security answers, CAPTCHA answers, session information, or screenshots containing private information.\n\n"
        . "Thank you,\n24Seven.FM Player Testing Team";
}

function sendOnboardingEmail(PDO $database, array $config, int $testerId): bool
{
    $statement = $database->prepare("SELECT testers.*, onboarding.onboarding_status, onboarding.coordinator_note FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
    $statement->execute([$testerId]);
    $tester = $statement->fetch();
    if ($tester === false) {
        throw new InvalidArgumentException('The tester is no longer active.');
    }
    if (!profileSummary($tester)['complete']) {
        throw new InvalidArgumentException('This tester needs their profile update before an orientation email can be sent.');
    }
    $message = onboardingMessage($tester);
    $subject = 'Welcome to 24Seven.FM Player Closed Alpha';
    $html = plainTextToHtml($message);
    $archiveId = prepareMailArchive($database, (int) $tester['id'], 'orientation', $subject, $message, $html);
    $accepted = sendIndividualMail($config, $tester['email'], $subject, $message, $html);
    completeMailArchive($database, $archiveId, $accepted);
    $status = onboardingStatus($tester);
    $record = $database->prepare('SELECT coordinator_note, orientation_email_attempts FROM tester_onboarding WHERE tester_id = ?');
    $record->execute([$testerId]);
    $existing = $record->fetch();
    $note = is_array($existing) ? (string) $existing['coordinator_note'] : '';
    $attempts = (is_array($existing) ? (int) $existing['orientation_email_attempts'] : 0) + 1;
    $upsert = $database->prepare('INSERT INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempted_at, orientation_email_attempts, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(tester_id) DO UPDATE SET onboarding_status = excluded.onboarding_status, orientation_email_status = excluded.orientation_email_status, orientation_email_attempted_at = excluded.orientation_email_attempted_at, orientation_email_attempts = excluded.orientation_email_attempts, updated_at = excluded.updated_at');
    $upsert->execute([$testerId, $status, $note, $accepted ? 'accepted' : 'failed', gmdate('c'), $attempts, gmdate('c')]);
    return $accepted;
}

function assignmentId(): int
{
    $id = filter_var($_POST['assignment_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($id === false) {
        throw new InvalidArgumentException('The Tester Task assignment is invalid.');
    }
    return $id;
}

function testerId(): int
{
    $id = filter_var($_POST['tester_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($id === false) {
        throw new InvalidArgumentException('The tester is invalid.');
    }
    return $id;
}

function assignmentInput(array $tasks, array $tester): array
{
    $taskId = strtoupper(trim((string) ($_POST['task_id'] ?? '')));
    $task = $tasks[$taskId] ?? null;
    if (!is_array($task)) {
        throw new InvalidArgumentException('Choose a valid Tester Task.');
    }
    if (($task['state'] ?? '') !== 'current') {
        throw new InvalidArgumentException('Future / Blocked Tester Tasks cannot be assigned.');
    }
    if (isGuestTester($tester) && !taskAllowsGuestTester($task)) {
        throw new InvalidArgumentException('Guest testers can receive only the account-free Guest testing tasks.');
    }
    $station = trim((string) ($_POST['station_scope'] ?? ''));
    if (!in_array($station, STATION_SCOPES, true)) {
        throw new InvalidArgumentException('Choose a valid station scope.');
    }
    $configuration = field('configuration_scope', MAX_ASSIGNMENT_CONFIGURATION_LENGTH, false, true);
    $note = field('coordinator_note', MAX_ASSIGNMENT_NOTE_LENGTH, false);
    if (($task['requiresConfiguration'] ?? false) === true && $configuration === '') {
        throw new InvalidArgumentException('This specialist task requires the supplied harness or controlled configuration to be recorded.');
    }
    $mutationAuthorized = ($_POST['mutation_authorized'] ?? '') === '1';
    $mode = (string) ($task['mutation']['mode'] ?? 'none');
    if ($mode === 'required' && !$mutationAuthorized) {
        throw new InvalidArgumentException('This controlled task requires explicit coordinator authorization before assignment.');
    }
    if (!in_array($mode, ['required', 'optional'], true)) {
        $mutationAuthorized = false;
    }
    return [$task, $station, $configuration, $note, $mutationAuthorized ? 1 : 0];
}

function assignmentMessage(array $task, array $assignment): string
{
    $lines = [
        '24Seven.FM Player testing assignment',
        $task['id'] . ' — ' . $task['title'],
        'PT cases: ' . implode(', ', $task['ptIds']),
        'Scope: ' . ($assignment['station_scope'] !== '' ? $assignment['station_scope'] : 'Network-wide / not station-specific'),
    ];
    if ($assignment['configuration_scope'] !== '') {
        $lines[] = 'Configuration: ' . $assignment['configuration_scope'];
    }
    $lines[] = 'Prerequisites: ' . implode('; ', $task['prerequisites']);
    $lines[] = 'Authorization: ' . $task['mutation']['label'];
    if ((int) $assignment['mutation_authorized'] === 1) {
        $lines[] = 'Coordinator authorization: granted for the controlled subcase described above.';
    }
    $lines[] = 'Safety: ' . $task['safetyWarning'];
    if ($assignment['coordinator_note'] !== '') {
        $lines[] = 'Coordinator note: ' . $assignment['coordinator_note'];
    }
    $lines[] = 'Task cases: https://player.jamesjennison.net/product-testing/?task=' . rawurlencode($task['id']);
    $lines[] = 'Submit one result for each PT case.';
    return implode("\n", $lines);
}

function assignmentEmailSubject(array $task): string
{
    return '24Seven.FM Player: ' . $task['id'] . ' assignment';
}

function sendAssignmentEmail(PDO $database, array $config, int $assignmentId): bool
{
    $statement = $database->prepare('SELECT assignments.id, assignments.tester_id, assignments.task_id, assignments.station_scope, assignments.configuration_scope, assignments.coordinator_note, assignments.mutation_authorized, testers.display_name, testers.email FROM tester_task_assignments AS assignments JOIN testers ON testers.id = assignments.tester_id WHERE assignments.id = ? AND testers.status = \'active\'');
    $statement->execute([$assignmentId]);
    $assignment = $statement->fetch();
    if ($assignment === false) {
        throw new InvalidArgumentException('The assignment recipient is no longer active.');
    }
    $tasks = taskRegistry();
    $task = $tasks[$assignment['task_id']] ?? null;
    if (!is_array($task)) {
        throw new RuntimeException('The assigned Tester Task is unavailable.');
    }
    $message = 'Hi ' . $assignment['display_name'] . ",\n\n" . assignmentMessage($task, $assignment);
    $subject = assignmentEmailSubject($task);
    $html = plainTextToHtml($message);
    $archiveId = prepareMailArchive($database, (int) $assignment['tester_id'], 'assignment', $subject, $message, $html, null, $assignmentId);
    $accepted = sendIndividualMail($config, $assignment['email'], $subject, $message, $html);
    completeMailArchive($database, $archiveId, $accepted);
    $database->prepare('UPDATE tester_task_assignments SET assignment_email_status = ?, assignment_email_attempted_at = ?, assignment_email_attempts = assignment_email_attempts + 1, updated_at = ? WHERE id = ?')
        ->execute([$accepted ? 'accepted' : 'failed', gmdate('c'), gmdate('c'), $assignmentId]);
    return $accepted;
}

function appendSanitizedNodes(DOMDocument $output, DOMNode $source, DOMNode $destination): void
{
    $allowed = [
        'p' => 'p', 'div' => 'p', 'br' => 'br', 'strong' => 'strong', 'b' => 'strong',
        'em' => 'em', 'i' => 'em', 'u' => 'u', 's' => 's', 'strike' => 's',
        'h2' => 'h2', 'h3' => 'h3', 'h4' => 'h4',
        'ul' => 'ul', 'ol' => 'ol', 'li' => 'li', 'a' => 'a',
    ];
    foreach ($source->childNodes as $child) {
        if ($child->nodeType === XML_TEXT_NODE) {
            $destination->appendChild($output->createTextNode($child->nodeValue ?? ''));
            continue;
        }
        if ($child->nodeType !== XML_ELEMENT_NODE) {
            continue;
        }
        $tag = strtolower($child->nodeName);
        if (!isset($allowed[$tag])) {
            appendSanitizedNodes($output, $child, $destination);
            continue;
        }
        $element = $output->createElement($allowed[$tag]);
        if ($allowed[$tag] === 'a' && $child instanceof DOMElement && $child->hasAttribute('href')) {
            $href = trim($child->getAttribute('href'));
            $scheme = strtolower((string) parse_url($href, PHP_URL_SCHEME));
            if (in_array($scheme, ['http', 'https', 'mailto'], true)) {
                $element->setAttribute('href', $href);
                $element->setAttribute('rel', 'noopener noreferrer');
            }
        }
        $destination->appendChild($element);
        if ($allowed[$tag] !== 'br') {
            appendSanitizedNodes($output, $child, $element);
        }
    }
}

function sanitizeHtmlBody(string $html): string
{
    if (textLength($html) > MAX_HTML_BODY_LENGTH || str_contains($html, "\0")) {
        throw new InvalidArgumentException('Please keep the email message within the allowed length.');
    }
    if (!class_exists('DOMDocument')) {
        throw new RuntimeException('The rich-text email editor is unavailable.');
    }
    $source = new DOMDocument('1.0', 'UTF-8');
    $prior = libxml_use_internal_errors(true);
    // DOMDocument defaults HTML input to a legacy encoding unless the document
    // declares UTF-8. Without this declaration, curly punctuation and bullets
    // become mojibake before the message reaches the MIME encoder.
    $loaded = $source->loadHTML('<!doctype html><html><head><meta charset="UTF-8"></head><body><div id="queue-editor">' . $html . '</div></body></html>', LIBXML_HTML_NOIMPLIED | LIBXML_HTML_NODEFDTD);
    libxml_clear_errors();
    libxml_use_internal_errors($prior);
    if (!$loaded) {
        throw new InvalidArgumentException('The formatted message could not be read.');
    }
    $root = $source->getElementById('queue-editor');
    if (!$root instanceof DOMElement) {
        throw new InvalidArgumentException('The formatted message could not be read.');
    }
    $output = new DOMDocument('1.0', 'UTF-8');
    $container = $output->createElement('div');
    $output->appendChild($container);
    appendSanitizedNodes($output, $root, $container);
    $body = '';
    foreach ($container->childNodes as $child) {
        $body .= $output->saveHTML($child);
    }
    if (trim(strip_tags($body)) === '') {
        throw new InvalidArgumentException('Write an email message before reviewing recipients.');
    }
    return $body;
}

function plainTextFromHtml(string $html): string
{
    $withBreaks = preg_replace('/<\\/(p|li|h[1-6])>|<br\\s*\\/?\\s*>/i', "\n", $html) ?? $html;
    $text = html_entity_decode(strip_tags($withBreaks), ENT_QUOTES | ENT_HTML5, 'UTF-8');
    $text = preg_replace("/[^\\S\r\n]+/u", ' ', $text) ?? $text;
    $text = preg_replace("/\n{3,}/", "\n\n", $text) ?? $text;
    return trim($text);
}

function plainTextToHtml(string $text): string
{
    $paragraphs = preg_split("/\r?\n{2,}/", trim($text)) ?: [];
    $html = '';
    foreach ($paragraphs as $paragraph) {
        $html .= '<p>' . nl2br(e($paragraph), false) . '</p>';
    }
    return $html;
}

/**
 * Resolve the small, explicit set of coordinator-mail variables for one
 * recipient. Values are escaped because HTML templates can place a variable
 * in either a text node or a link attribute. The stored batch remains the
 * editable draft; every archive record receives the resolved exact message.
 */
function mergeCoordinatorMailHtml(string $html, array $tester): string
{
    $status = onboardingStatus($tester);
    return strtr($html, [
        '{{tester_name}}' => e((string) ($tester['display_name'] ?? 'Tester')),
        '{{onboarding_status}}' => e(onboardingStatusLabel($status)),
        '{{tester_portal_url}}' => e(TESTER_PORTAL_PUBLIC_URL),
        '{{program_name}}' => '24Seven.FM Player Closed Alpha',
    ]);
}

function selectedTesterIds(): array
{
    $raw = $_POST['tester_ids'] ?? [];
    if (!is_array($raw) || $raw === []) {
        throw new InvalidArgumentException('Select at least one active tester.');
    }
    $ids = [];
    foreach ($raw as $id) {
        if (!is_scalar($id) || !ctype_digit((string) $id) || (int) $id < 1) {
            throw new InvalidArgumentException('The selected recipients are invalid.');
        }
        $ids[(int) $id] = (int) $id;
    }
    return array_values($ids);
}

function selectedActiveTesters(PDO $database, array $ids): array
{
    $placeholders = implode(',', array_fill(0, count($ids), '?'));
    // Reviewed testers only: a pending application is handled with Accept /
    // Request details / Reject, not folded into a bulk coordinator send.
    $statement = $database->prepare("SELECT testers.id, testers.display_name, testers.email, onboarding.reviewed_at, onboarding.onboarding_status FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.status = 'active' AND testers.id IN ($placeholders) ORDER BY testers.id");
    $statement->execute($ids);
    $testers = $statement->fetchAll();
    if (count($testers) !== count($ids)) {
        throw new InvalidArgumentException('One or more selected testers are no longer active. Refresh the queue and try again.');
    }
    foreach ($testers as $tester) {
        if (applicationStage($tester) === 'pending_review') {
            throw new InvalidArgumentException('Applications awaiting review cannot receive a coordinator email yet. Accept, request details, or reject them first.');
        }
    }
    return $testers;
}

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

function base64MimePart(string $content): string
{
    return rtrim(chunk_split(base64_encode($content), 76, "\r\n"), "\r\n");
}

function encodedMimeHeader(string $value): string
{
    return '=?UTF-8?B?' . base64_encode($value) . '?=';
}

function multipartAlternativeMessage(string $fromName, string $from, string $recipient, string $subject, string $plainText, string $html): array
{
    $boundary = '=_24Seven_' . bin2hex(random_bytes(16));
    $headers = [
        'From: ' . encodedMimeHeader($fromName) . ' <' . $from . '>',
        'To: ' . $recipient,
        'Reply-To: ' . $from,
        'Subject: ' . encodedMimeHeader($subject),
        'MIME-Version: 1.0',
        'Content-Type: multipart/alternative; boundary="' . $boundary . '"',
    ];
    $message = implode("\r\n", [
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
        '',
    ]);
    $message = preg_replace('/(?m)^\./', '..', $message) ?? $message;
    return [$headers, $message];
}

function sendIndividualMail(array $config, string $recipient, string $subject, string $plainText, string $html): bool
{
    $senderName = trim((string) ($config['from_name'] ?? '24Seven.FM Player'));
    $from = $config['from_email'];
    [$headers, $message] = multipartAlternativeMessage($senderName, $from, $recipient, $subject, $plainText, $html);
    $socket = null;
    $stage = 'connect';
    try {
        $context = stream_context_create(['ssl' => [
            'verify_peer' => true,
            'verify_peer_name' => true,
            'allow_self_signed' => false,
            'peer_name' => MAIL_SUBMISSION_HOST,
        ]]);
        $socket = stream_socket_client(
            'tcp://' . MAIL_SUBMISSION_HOST . ':25',
            $errorNumber,
            $errorMessage,
            15,
            STREAM_CLIENT_CONNECT,
            $context,
        );
        if ($socket === false) {
            throw new RuntimeException('SMTP connection failed.');
        }
        stream_set_timeout($socket, 20);
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
        smtpCommand($socket, 'MAIL FROM:<' . $from . '>', [250]);
        $stage = 'recipient';
        smtpCommand($socket, 'RCPT TO:<' . $recipient . '>', [250, 251]);
        $stage = 'data';
        smtpCommand($socket, 'DATA', [354]);
        $stage = 'message';
        smtpWrite($socket, implode("\r\n", $headers) . "\r\n\r\n" . $message . "\r\n.\r\n");
        [$accepted] = smtpResponse($socket);
        if ($accepted !== 250) {
            throw new RuntimeException('SMTP message rejected.');
        }
        smtpCommand($socket, 'QUIT', [221]);
        fclose($socket);
        return true;
    } catch (Throwable $exception) {
        error_log('private-tester-queue mail transport failure stage=' . $stage);
        if (is_resource($socket)) {
            fclose($socket);
        }
        return false;
    }
}

function prepareMailArchive(PDO $database, int $testerId, string $messageType, string $subject, string $body, string $html, ?int $batchId = null, ?int $assignmentId = null): int
{
    if (!in_array($messageType, ['batch', 'orientation', 'assignment'], true)) {
        throw new InvalidArgumentException('The mail archive type is invalid.');
    }
    $statement = $database->prepare('INSERT INTO tester_mail_archive(tester_id, batch_id, assignment_id, message_type, subject, body, body_html, handoff_status, prepared_at) VALUES (?, ?, ?, ?, ?, ?, ?, \'prepared\', ?)');
    $statement->execute([$testerId, $batchId, $assignmentId, $messageType, $subject, $body, $html, gmdate('c')]);
    return (int) $database->lastInsertId();
}

function completeMailArchive(PDO $database, int $archiveId, bool $accepted): void
{
    $database->prepare('UPDATE tester_mail_archive SET handoff_status = ?, attempted_at = ? WHERE id = ? AND handoff_status = \'prepared\'')
        ->execute([$accepted ? 'accepted' : 'failed', gmdate('c'), $archiveId]);
}

function mailArchiveStatusLabel(string $status): string
{
    return match ($status) {
        'accepted' => 'Accepted by mail transport',
        'failed' => 'Mail transport failed',
        default => 'Prepared; no send outcome recorded',
    };
}

function mailArchiveTypeLabel(string $type): string
{
    return match ($type) {
        'orientation' => 'Orientation',
        'assignment' => 'Tester Task assignment',
        default => 'Coordinator batch',
    };
}

function onboardingBadge(string $status): string
{
    return '<span class="' . e(onboardingStatusBadgeClass($status)) . '">' . e(onboardingStatusLabel($status)) . '</span>';
}

function applicationStageBadge(string $stage): string
{
    return '<span class="' . e(applicationStageBadgeClass($stage)) . '">' . e(applicationStageLabel($stage)) . '</span>';
}

function renderOperationsDashboard(PDO $database, string $notice = '', string $error = '', bool $emailOnly = false): never
{
    $tasks = taskRegistry();
    $testers = $database->query("SELECT testers.*, onboarding.onboarding_status, onboarding.reviewed_at, onboarding.orientation_email_status, onboarding.orientation_email_attempted_at, onboarding.orientation_email_attempts FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.status = 'active' ORDER BY testers.received_at, testers.id")->fetchAll();
    $assignments = $database->query("SELECT tester_id, task_status FROM tester_task_assignments ORDER BY id")->fetchAll();
    $assignmentCounts = [];
    foreach ($assignments as $assignment) {
        $id = (int) $assignment['tester_id'];
        $assignmentCounts[$id] = ($assignmentCounts[$id] ?? 0) + 1;
    }

    // A pending application is not a roster tester: it has not been looked at
    // by a coordinator yet, so it is never mixed into onboarding/assignment
    // workflow below. See applicationStage() for the stage computation.
    $pendingApplications = [];
    $rosterTesters = [];
    $stageCounts = array_fill_keys(APPLICATION_STAGES, 0);
    $sourceCounts = array_fill_keys(array_keys(RECRUITMENT_SOURCE_LABELS), 0);
    foreach ($testers as $tester) {
        $stage = applicationStage($tester);
        $stageCounts[$stage]++;
        if ($stage === 'pending_review') {
            $pendingApplications[] = $tester;
        } else {
            $rosterTesters[] = $tester;
        }
        $source = $tester['recruitment_source'] ?? 'direct';
        if (isset($sourceCounts[$source])) {
            $sourceCounts[$source]++;
        } else {
            $sourceCounts['other']++;
        }
    }

    // ?compose_for=<id> pre-selects a just-accepted applicant in the compose
    // form below (see request_profile_details); only honored for a tester who
    // has actually cleared review, so a stale or tampered link cannot reopen
    // the pending-application actions through the compose path.
    $rosterIds = array_map(static fn (array $tester): int => (int) $tester['id'], $rosterTesters);
    $composeForId = filter_var($_GET['compose_for'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    $composeForId = $composeForId !== false && in_array($composeForId, $rosterIds, true) ? $composeForId : null;

    $applicationRows = '';
    foreach ($pendingApplications as $tester) {
        $id = (int) $tester['id'];
        $source = $tester['recruitment_source'] ?? 'direct';
        $applicationRows .= '<article class="application-row">'
            . '<div class="application-summary"><strong>' . e($tester['display_name']) . '</strong><br><small>' . e($tester['email']) . '</small><br><small>' . e($tester['device']) . ' · ' . e($tester['android_version']) . '</small><br><small>' . e(recruitmentSourceLabel(is_string($source) ? $source : null)) . ' · Received ' . e($tester['received_at']) . '</small></div>'
            . '<div class="application-actions">'
            . '<form method="post"><input type="hidden" name="action" value="accept_application"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $id . '"><button type="submit">Accept</button></form>'
            . '<form method="post"><input type="hidden" name="action" value="request_profile_details"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $id . '"><button class="secondary" type="submit">Request details</button></form>'
            . '<details class="reject-disclosure"><summary>Reject</summary><form method="post"><input type="hidden" name="action" value="reject_application"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $id . '"><label>Rejection reason<textarea name="rejection_reason" maxlength="' . MAX_ONBOARDING_NOTE_LENGTH . '" required placeholder="Kept in the private coordinator record only."></textarea></label><button class="danger" type="submit">Confirm reject</button></form></details>'
            . '</div></article>';
    }

    $rows = '';
    foreach ($rosterTesters as $tester) {
        $id = (int) $tester['id'];
        $status = onboardingStatus($tester);
        $stage = applicationStage($tester);
        $source = $tester['recruitment_source'] ?? 'direct';
        $profile = profileSummary($tester);
        $profileText = $profile['complete'] ? 'Complete' : count($profile['missing']) . ' update' . (count($profile['missing']) === 1 ? '' : 's') . ' needed';
        $taskText = ($assignmentCounts[$id] ?? 0) === 0 ? 'No focused task' : ($assignmentCounts[$id] . ' focused task' . ($assignmentCounts[$id] === 1 ? '' : 's'));
        $rows .= '<tr><td><strong>' . e($tester['display_name']) . '</strong><br><small>' . e($tester['email']) . '</small></td><td>' . e($tester['device']) . '<br><small>' . e($tester['android_version']) . '</small></td><td><small>' . e(recruitmentSourceLabel(is_string($source) ? $source : null)) . '</small></td><td>' . applicationStageBadge($stage) . '<br>' . onboardingBadge($status) . '<br><small>' . e($profileText) . '</small></td><td><small>' . e($taskText) . '</small></td><td><a class="link-button" href="/private-tester-queue.php?tester=' . $id . '">Open workspace</a><a class="link-button" href="/tester-portal.php?preview_tester=' . $id . '">Preview tester view</a></td></tr>';
    }

    $message = $notice === '' ? '' : '<p class="notice">' . e($notice) . '</p>';
    $message .= $error === '' ? '' : '<p class="notice error">' . e($error) . '</p>';
    $editor = '<label for="email-template">Email template</label><select id="email-template"><option value="">Custom email</option><option value="profile-update">Profile update request</option><option value="welcome">Welcome to the 24Seven.FM Player Google Play Closed Test</option><option value="feedback">Testing feedback request</option><option value="new-build">New build available</option><option value="reminder">Reminder to test</option><option value="known-issue">Known issue / workaround</option><option value="test-session">Test session request</option><option value="test-complete">Thank you / test complete</option><option value="tester-status">Tester status update</option><option value="release-signoff">Release candidate sign-off</option></select><p class="muted">Choose a template or write a custom message. The queue always sends one individual, multipart email per selected tester.</p><label for="body-editor">Message</label><div class="toolbar" role="toolbar" aria-label="Email text formatting"><button type="button" data-format="bold"><strong>B</strong><span class="visually-hidden">Bold</span></button><button type="button" data-format="italic"><em>I</em><span class="visually-hidden">Italic</span></button><button type="button" data-format="underline"><u>U</u><span class="visually-hidden">Underline</span></button><button type="button" data-format="insertUnorderedList">Bullets</button><button type="button" data-format="insertOrderedList">Numbered list</button><button type="button" data-format="createLink">Link</button><button type="button" data-format="removeFormat">Clear format</button></div><div class="toolbar" role="toolbar" aria-label="Email variables"><button type="button" data-insert-variable="{{tester_name}}">Tester name</button><button type="button" data-insert-variable="{{onboarding_status}}">Onboarding status</button><button type="button" data-insert-variable="{{tester_portal_url}}">Tester portal URL</button><button type="button" data-insert-variable="{{program_name}}">Program name</button></div><p class="muted small">Variables resolve separately for each recipient and the exact sent version is retained in the protected archive.</p><div id="body-editor" class="editor" contenteditable="true" role="textbox" aria-multiline="true" data-placeholder="Write a message for the selected testers."></div><input type="hidden" name="body_html" id="body-html"><noscript><label for="body">Message</label><textarea id="body" name="body" maxlength="' . MAX_BODY_LENGTH . '" required></textarea></noscript>';
    $archiveCount = (int) $database->query('SELECT COUNT(*) FROM tester_mail_archive')->fetchColumn();

    $recipientRows = implode('', array_map(static function (array $tester) use ($composeForId): string {
        $checked = $composeForId !== null && (int) $tester['id'] === $composeForId ? ' checked' : '';
        return '<tr><td><input aria-label="Select ' . e($tester['display_name']) . '" type="checkbox" name="tester_ids[]" value="' . (int) $tester['id'] . '"' . $checked . '></td><td>' . e($tester['display_name']) . '<br><small>' . e($tester['email']) . '</small></td><td>' . onboardingBadge(onboardingStatus($tester)) . '</td></tr>';
    }, $rosterTesters));
    // CSP is script-src 'self' — no inline <script> logic is allowed, so the
    // compose_for pre-selection is handed to the external JS file as a JSON
    // data island, matching the existing #tester-task-registry pattern.
    $composeHint = $composeForId === null ? '' : '<script id="compose-for-tester" type="application/json">' . json_encode(['testerId' => $composeForId, 'template' => 'profile-update'], JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_UNESCAPED_SLASHES) . '</script>';

    if ($emailOnly) {
        $content = '<header class="dashboard-header"><div><p class="eyebrow">24Seven.FM Player · Closed alpha</p><h1>Coordinator email</h1><p class="muted">Compose individual tester-program email, review the resolved draft, and inspect protected handoff records.</p><p><a class="link-button" href="/private-tester-queue.php">Operations</a><a class="link-button" href="/private-tester-queue.php?live_chat=1">Live Chat</a><a class="link-button" href="/private-tester-queue.php?mail_archive=1">Sent archive (' . $archiveCount . ')</a></p></div><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="secondary" type="submit">Sign out</button></form></header>' . $message
            . '<section class="compose-card"><h2>Compose a coordinator email</h2><form method="post" id="email-form"><input type="hidden" name="action" value="prepare"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><p class="muted">Only reviewed testers can receive a coordinator email; applications awaiting review use the Accept / Request details / Reject actions in Operations instead.</p><p><label><input type="checkbox" id="select-all"> Select all reviewed testers</label></p><div class="table-scroll"><table><thead><tr><th scope="col">Select</th><th scope="col">Tester</th><th scope="col">Onboarding</th></tr></thead><tbody>' . ($recipientRows === '' ? '<tr><td colspan="3" class="muted">No reviewed testers yet.</td></tr>' : $recipientRows) . '</tbody></table></div><label for="subject">Subject</label><input id="subject" name="subject" maxlength="' . MAX_SUBJECT_LENGTH . '" required>' . $editor . '<button type="submit">Review selected recipients</button></form></section>'
            . $composeHint
            . '<script src="/assets/private-tester-queue.js?v=onboarding-2" defer></script>';
        renderPage('Coordinator email', $content);
    }

    $content = '<header class="dashboard-header"><div><p class="eyebrow">24Seven.FM Player · Closed alpha</p><h1>Tester operations</h1><p class="muted">A private workspace for moving a new application through review to a focused, safe testing assignment.</p><p><a class="link-button" href="/private-tester-queue.php?live_chat=1">Live Chat</a><a class="link-button" href="/private-tester-queue.php?email=1">Email (' . $archiveCount . ')</a><a class="link-button" href="/private-tester-queue.php?mail_archive=1">Sent archive</a></p></div><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="secondary" type="submit">Sign out</button></form></header>' . $message
        . '<section><h2>Onboarding pipeline</h2><div class="onboarding-overview"><div class="onboarding-metric stage-pending_review"><strong>' . $stageCounts['pending_review'] . '</strong><span>Pending review</span></div><div class="onboarding-metric stage-accepted"><strong>' . $stageCounts['accepted'] . '</strong><span>Evidence in progress</span></div><div class="onboarding-metric stage-active"><strong>' . $stageCounts['active'] . '</strong><span>Active assignments</span></div></div><p class="muted">Workflow: review a new application → complete Profile &amp; Device → tester self-confirms Play opt-in and the first-use smoke test → assign focused work. Orientation and invitation messages remain archived mail events; they never advance the lifecycle or prove opt-in, installation, or activity.</p><p class="muted">Recruitment: ' . e('Direct ' . $sourceCounts['direct'] . ' · Testers Community ' . $sourceCounts['testers_community'] . ' · Betabound ' . $sourceCounts['betabound'] . ' · BetaFamily ' . $sourceCounts['betafamily'] . ' · Other ' . $sourceCounts['other']) . '</p></section>'
        . '<section class="applications-card"><h2>Applications awaiting review (' . count($pendingApplications) . ')</h2>' . ($applicationRows === '' ? '<p class="muted">No new applications right now.</p>' : '<p class="muted">Newly submitted or imported applications stay here, separate from the accepted roster, until a coordinator accepts, requests details, or rejects them.</p><div class="application-rows">' . $applicationRows . '</div>') . '</section>'
        . '<section><h2>Tester roster</h2><p class="muted">Open one workspace to review coverage, record onboarding progress, send an individual orientation email, and manage focused tasks.</p><div class="table-scroll"><table><thead><tr><th scope="col">Tester</th><th scope="col">Device</th><th scope="col">Recruitment</th><th scope="col">Stage &amp; onboarding</th><th scope="col">Assignments</th><th scope="col"><span class="visually-hidden">Open</span></th></tr></thead><tbody>' . ($rows === '' ? '<tr><td colspan="6" class="muted">No accepted testers yet.</td></tr>' : $rows) . '</tbody></table></div></section>'
        . '<details class="compose-card"' . ($composeForId !== null ? ' open' : '') . '><summary><strong>Compose a coordinator email</strong></summary><form method="post" id="email-form"><input type="hidden" name="action" value="prepare"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><section><h2>Recipients and message</h2><p class="muted">Only reviewed testers can receive a coordinator email; applications awaiting review use the Accept / Request details / Reject actions above instead.</p><p><label><input type="checkbox" id="select-all"> Select all reviewed testers</label></p><div class="table-scroll"><table><thead><tr><th scope="col">Select</th><th scope="col">Tester</th><th scope="col">Onboarding</th></tr></thead><tbody>' . ($recipientRows === '' ? '<tr><td colspan="3" class="muted">No reviewed testers yet.</td></tr>' : $recipientRows) . '</tbody></table></div><label for="subject">Subject</label><input id="subject" name="subject" maxlength="' . MAX_SUBJECT_LENGTH . '" required>' . $editor . '<button type="submit">Review selected recipients</button></section></form></details>'
        . $composeHint
        . '<script src="/assets/private-tester-queue.js?v=onboarding-2" defer></script>';
    renderPage('Tester operations', $content);
}

function renderTesterWorkspace(PDO $database, int $testerId, string $notice = '', string $error = ''): never
{
    $testerQuery = $database->prepare("SELECT testers.*, onboarding.onboarding_status, onboarding.coordinator_note AS onboarding_note, onboarding.orientation_email_status, onboarding.orientation_email_attempted_at, onboarding.orientation_email_attempts, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at, onboarding.withdrawal_requested_at, onboarding.deletion_requested_at FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
    $testerQuery->execute([$testerId]);
    $tester = $testerQuery->fetch();
    if ($tester === false) {
        redirect('?error=' . rawurlencode('The tester is no longer active.'));
    }
    $tasks = taskRegistry();
    $assignmentsQuery = $database->prepare('SELECT id, task_id, task_status, station_scope, configuration_scope, coordinator_note, mutation_authorized, assignment_email_status, assignment_email_attempted_at, assignment_email_attempts FROM tester_task_assignments WHERE tester_id = ? ORDER BY created_at, id');
    $assignmentsQuery->execute([$testerId]);
    $assignments = $assignmentsQuery->fetchAll();
    $feedbackQuery = $database->prepare('SELECT feedback.subject, feedback.details, feedback.outcome, feedback.category, feedback.created_at, assignments.task_id FROM tester_feedback AS feedback LEFT JOIN tester_task_assignments AS assignments ON assignments.id = feedback.assignment_id WHERE feedback.tester_id = ? ORDER BY feedback.created_at DESC, feedback.id DESC');
    $feedbackQuery->execute([$testerId]);
    $feedbackItems = '';
    foreach ($feedbackQuery->fetchAll() as $feedback) {
        $feedbackItems .= '<article class="assignment-existing"><h4>' . e(($feedback['task_id'] ?: 'General') . ' · ' . strtoupper((string) $feedback['outcome'])) . '</h4><p><strong>' . e(FEEDBACK_CATEGORIES[$feedback['category']] ?? FEEDBACK_CATEGORIES['other']) . ' · ' . e($feedback['subject']) . '</strong><br><small>' . e($feedback['created_at']) . '</small></p><p>' . nl2br(e($feedback['details']), false) . '</p></article>';
    }
    $profile = profileSummary($tester);
    $status = onboardingStatus($tester);
    $statusOptions = '';
    foreach (ONBOARDING_STATUSES as $candidate) {
        $disabled = !$profile['complete'] && $candidate !== 'profile_pending' ? ' disabled' : '';
        $statusOptions .= '<option value="' . e($candidate) . '"' . ($status === $candidate ? ' selected' : '') . $disabled . '>' . e(onboardingStatusLabel($candidate)) . '</option>';
    }
    $profileBlock = $profile['complete']
        ? '<p class="profile-complete"><strong>Profile complete.</strong> This tester can progress through invitation, orientation, and assignment.</p>'
        : '<p class="profile-missing"><strong>Profile update needed:</strong> ' . e(implode(', ', $profile['missing'])) . '. Keep onboarding at this step until the tester replies with the missing information.</p>';
    $orientation = ($tester['orientation_email_status'] ?? 'not_sent') === 'accepted'
        ? 'Accepted by the mail transport'
        : (($tester['orientation_email_status'] ?? 'not_sent') === 'failed' ? 'Mail transport failed' : 'Not sent');
    $guestTester = isGuestTester($tester);
    $taskOptions = '<option value="">Choose a current Tester Task</option>';
    foreach ($tasks as $task) {
        $notEligibleForGuest = $guestTester && !taskAllowsGuestTester($task);
        $disabled = (($task['state'] ?? '') === 'future' || $notEligibleForGuest) ? ' disabled' : '';
        $suffix = ($task['state'] ?? '') === 'future'
            ? ' (Future / Blocked)'
            : ($notEligibleForGuest ? ' (Requires station account or controlled setup)' : '');
        $taskOptions .= '<option value="' . e($task['id']) . '"' . $disabled . '>' . e($task['id'] . ' — ' . $task['title'] . $suffix) . '</option>';
    }
    $stationOptions = '';
    foreach (STATION_SCOPES as $station) {
        $stationOptions .= '<option value="' . e($station) . '">' . e($station) . '</option>';
    }
    $assignmentCards = '';
    foreach ($assignments as $assignment) {
        $task = $tasks[$assignment['task_id']] ?? null;
        if (!is_array($task)) {
            continue;
        }
        $currentStatusOptions = '';
        foreach (ASSIGNMENT_STATUSES as $candidate) {
            $currentStatusOptions .= '<option value="' . e($candidate) . '"' . ($assignment['task_status'] === $candidate ? ' selected' : '') . '>' . e(ucwords(str_replace('_', ' ', $candidate))) . '</option>';
        }
        $currentStationOptions = '';
        foreach (STATION_SCOPES as $station) {
            $currentStationOptions .= '<option value="' . e($station) . '"' . ($assignment['station_scope'] === $station ? ' selected' : '') . '>' . e($station) . '</option>';
        }
        $assignmentCards .= '<article class="assignment-existing"><h4>' . e($task['id'] . ' — ' . $task['title']) . '</h4><p><strong>PT cases:</strong> ' . e(implode(', ', $task['ptIds'])) . ' · <a href="/product-testing/?task=' . rawurlencode($task['id']) . '">Open cases</a></p><p><strong>Assignment email:</strong> ' . e($assignment['assignment_email_status'] === 'accepted' ? 'Accepted by the mail transport' : ($assignment['assignment_email_status'] === 'failed' ? 'Mail transport failed' : 'Not sent')) . '</p><form method="post"><input type="hidden" name="action" value="update_task"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="assignment_id" value="' . (int) $assignment['id'] . '"><label>Status<select name="task_status">' . $currentStatusOptions . '</select></label><label>Station scope<select name="station_scope">' . $currentStationOptions . '</select></label><label>Device / accessory scope<input name="configuration_scope" maxlength="' . MAX_ASSIGNMENT_CONFIGURATION_LENGTH . '" value="' . e($assignment['configuration_scope']) . '"></label><label>Coordinator note<textarea name="coordinator_note" maxlength="' . MAX_ASSIGNMENT_NOTE_LENGTH . '">' . e($assignment['coordinator_note']) . '</textarea></label><button type="submit">Save assignment</button></form></article>';
    }
    $message = $notice === '' ? '' : '<p class="notice">' . e($notice) . '</p>';
    $message .= $error === '' ? '' : '<p class="notice error">' . e($error) . '</p>';
    $guestTaskNotice = $guestTester
        ? '<p class="notice"><strong>Guest tester.</strong> Guest testers can receive only the account-free Guest testing tasks. Do not ask them to create, use, or share a station account.</p>'
        : '';
    $playOptIn = ($tester['play_opt_in_confirmed_at'] ?? '') !== '' ? (string) $tester['play_opt_in_confirmed_at'] : 'Not self-confirmed';
    $smokeTest = ($tester['initial_smoke_test_confirmed_at'] ?? '') !== '' ? (string) $tester['initial_smoke_test_confirmed_at'] : 'Not self-confirmed';
    $withdrawalRequest = ($tester['withdrawal_requested_at'] ?? '') !== '' ? (string) $tester['withdrawal_requested_at'] : 'None';
    $deletionRequest = ($tester['deletion_requested_at'] ?? '') !== '' ? (string) $tester['deletion_requested_at'] : 'None';
    $content = '<p><a href="/private-tester-queue.php">← Tester operations</a> · <a href="/tester-portal.php?preview_tester=' . $testerId . '">Preview tester view</a></p><div><p class="muted">Tester workspace</p><h1>' . e($tester['display_name']) . '</h1><p class="muted">' . e($tester['device']) . ' · ' . e($tester['android_version']) . ' · ' . e($tester['country'] ?: 'Country not provided') . ' · ' . e(recruitmentSourceLabel($tester['recruitment_source'] ?? null)) . '</p></div>' . $message
        . '<section><h2>Onboarding</h2>' . onboardingBadge($status) . $profileBlock . '<form method="post"><input type="hidden" name="action" value="update_onboarding"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $testerId . '"><label>Onboarding status<select name="onboarding_status">' . $statusOptions . '</select></label><label>Coordinator note<textarea name="onboarding_note" maxlength="' . MAX_ONBOARDING_NOTE_LENGTH . '" placeholder="Private operational note; never add credentials or secrets.">' . e($tester['onboarding_note'] ?? '') . '</textarea></label><button type="submit">Save onboarding progress</button></form><p><strong>Google Play opt-in:</strong> ' . e($playOptIn) . '<br><strong>Initial smoke test:</strong> ' . e($smokeTest) . '<br><small>These are tester self-confirmations, not inferred installation or usage telemetry.</small></p><p><strong>Withdrawal request:</strong> ' . e($withdrawalRequest) . '<br><strong>Record-deletion request:</strong> ' . e($deletionRequest) . '<br><small>Verify the request, stop program access promptly, and delete or anonymize the private tester record within the adopted 90-day policy period. A request is not evidence that deletion is already complete.</small></p>' . ($withdrawalRequest === 'None' ? '' : '<form method="post"><input type="hidden" name="action" value="deactivate_withdrawn_tester"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $testerId . '"><label><input style="width:auto" type="checkbox" name="confirm_deactivate" value="withdraw" required> I verified this withdrawal request and will stop this tester’s program access.</label><button class="danger" type="submit">Deactivate withdrawn tester</button></form>') . '<p><strong>Orientation email:</strong> ' . e($orientation) . ' <small>(' . (int) ($tester['orientation_email_attempts'] ?? 0) . ' handoff attempt' . ((int) ($tester['orientation_email_attempts'] ?? 0) === 1 ? '' : 's') . ')</small></p>' . ($profile['complete'] ? '<form method="post"><input type="hidden" name="action" value="send_onboarding_email"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $testerId . '"><button class="secondary" type="submit">Send orientation email</button></form>' : '') . '</section>'
        . '<section><h2>Focused Tester Tasks</h2><p class="muted">Assign only a focused bundle that matches this tester’s coverage. Each PT case still receives its own result.</p>' . $guestTaskNotice . ($assignmentCards === '' ? '<p class="muted">No task assigned yet.</p>' : $assignmentCards) . '<article class="onboarding-card"><h3>Assign a focused Tester Task</h3><form method="post" data-task-assignment-form><input type="hidden" name="action" value="assign_task"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $testerId . '"><label>Tester Task<select name="task_id" data-task-select required>' . $taskOptions . '</select></label><div class="task-preview" data-task-preview aria-live="polite">Choose a current task to see its PT cases, prerequisites, and safety boundary.</div><label>Station scope<select name="station_scope">' . $stationOptions . '</select></label><label>Device / accessory / form factor scope<input name="configuration_scope" maxlength="' . MAX_ASSIGNMENT_CONFIGURATION_LENGTH . '" placeholder="For example, Pixel 9, Bluetooth headset, folded"></label><label>Coordinator note<textarea name="coordinator_note" maxlength="' . MAX_ASSIGNMENT_NOTE_LENGTH . '"></textarea></label><label class="mutation-authorization" data-mutation-authorization hidden><input type="checkbox" name="mutation_authorized" value="1"> I explicitly authorize the controlled subcase described above.</label><button type="submit">Assign Tester Task</button></form></article></section><section><h2>Tester task reports</h2><p class="muted">Private reports submitted through the tester portal.</p>' . ($feedbackItems === '' ? '<p class="muted">No task reports submitted yet.</p>' : $feedbackItems) . '</section><script id="tester-task-registry" type="application/json">' . json_encode(array_values($tasks), JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_UNESCAPED_SLASHES) . '</script><script src="/assets/private-tester-queue.js?v=onboarding-2" defer></script>';
    renderPage('Tester workspace', $content);
}

function renderDashboard(PDO $database, string $notice = '', string $error = ''): never
{
    renderOperationsDashboard($database, $notice, $error);
}

function renderConfirmation(PDO $database, int $batchId): never
{
    $batch = $database->prepare('SELECT id, subject, body, body_html FROM email_batches WHERE id = ? AND status = \'prepared\'');
    $batch->execute([$batchId]);
    $batch = $batch->fetch();
    if ($batch === false) {
        redirect('?error=' . rawurlencode('The prepared email is no longer available.'));
    }
    $recipients = $database->prepare('SELECT testers.display_name, testers.email, onboarding.onboarding_status FROM email_batch_recipients JOIN testers ON testers.id = tester_id LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE batch_id = ? ORDER BY testers.id');
    $recipients->execute([$batchId]);
    $list = '';
    $recipientRecords = $recipients->fetchAll();
    foreach ($recipientRecords as $recipient) {
        $list .= '<li>' . e($recipient['display_name']) . ' <small>&lt;' . e($recipient['email']) . '&gt;</small></li>';
    }
    $previewRecipient = $recipientRecords[0] ?? null;
    if (!is_array($previewRecipient)) {
        redirect('?error=' . rawurlencode('This prepared email has no active recipients.'));
    }
    $template = is_string($batch['body_html'] ?? null) && $batch['body_html'] !== '' ? $batch['body_html'] : plainTextToHtml($batch['body']);
    $html = mergeCoordinatorMailHtml($template, $previewRecipient);
    $body = plainTextFromHtml($html);
    $content = '<h1>Review delivery</h1><section><h2>Recipients</h2><p class="muted">Each recipient receives an individual email. No recipient is placed in To, Cc, or Bcc with another tester.</p><ol>' . $list . '</ol></section><section><p class="eyebrow">Draft preview</p><h2>' . e($batch['subject']) . '</h2><p class="muted">This preview resolves variables for the first selected recipient. Each recipient receives their own resolved message, and that exact version is preserved in the sent archive.</p><div style="background:#fff;color:#182033;padding:1rem;border-radius:.4rem">' . $html . '</div><h3>Plain-text alternative</h3><pre style="white-space:pre-wrap;font:inherit">' . e($body) . '</pre></section><form method="post"><input type="hidden" name="action" value="send"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="batch_id" value="' . (int) $batch['id'] . '"><button class="danger" type="submit">Send to these recipients</button></form><p><a href="/private-tester-queue.php">Cancel and return to queue</a></p>';
    renderPage('Review tester email', $content);
}

function renderMailArchive(PDO $database): never
{
    $records = $database->query('SELECT archive.id, archive.message_type, archive.subject, archive.handoff_status, archive.prepared_at, archive.attempted_at, testers.display_name, testers.email FROM tester_mail_archive AS archive JOIN testers ON testers.id = archive.tester_id ORDER BY archive.prepared_at DESC, archive.id DESC LIMIT 250')->fetchAll();
    $rows = '';
    foreach ($records as $record) {
        $rows .= '<tr><td>' . e((string) ($record['attempted_at'] ?: $record['prepared_at'])) . '</td><td><strong>' . e($record['display_name']) . '</strong><br><small>' . e($record['email']) . '</small></td><td>' . e(mailArchiveTypeLabel((string) $record['message_type'])) . '</td><td>' . e(mailArchiveStatusLabel((string) $record['handoff_status'])) . '</td><td>' . e($record['subject']) . '</td><td><a href="/private-tester-queue.php?mail_record=' . (int) $record['id'] . '">View message</a></td></tr>';
    }
    $content = '<p><a href="/private-tester-queue.php">← Tester operations</a></p><h1>Sent mail archive</h1><p class="muted">This protected record preserves the recipient, exact multipart content, and mail-transport handoff outcome for coordinator batches, orientation emails, and Tester Task assignments. “Accepted” means the transport accepted the message; it does not prove inbox delivery or reading.</p>'
        . '<section><table><thead><tr><th>Attempt</th><th>Recipient</th><th>Type</th><th>Handoff</th><th>Subject</th><th><span class="visually-hidden">Open</span></th></tr></thead><tbody>' . ($rows === '' ? '<tr><td colspan="6" class="muted">No coordinator email handoffs are archived yet.</td></tr>' : $rows) . '</tbody></table></section>';
    renderPage('Sent mail archive', $content);
}

function coordinatorChatPayload(PDO $database, array $tester, int $afterId = 0): array
{
    $messages = chatMessages($database, (int) $tester['id'], 'coordinator', $afterId);
    return array_map(static fn (array $message): array => [
        'id' => (int) $message['id'],
        'sender' => $message['sender_role'] === 'tester' ? (string) $tester['display_name'] : 'Coordinator',
        'role' => $message['sender_role'],
        'body' => (string) $message['body'],
        'createdAt' => (string) $message['created_at'],
    ], $messages);
}

function coordinatorChatPoll(PDO $database, array $tester, int $afterId, bool $stream = false): never
{
    session_write_close();
    if ($stream) {
        header('Content-Type: text/event-stream; charset=UTF-8');
        header('Cache-Control: no-store, private');
        header('X-Accel-Buffering: no');
        for ($attempt = 0; $attempt < 20 && !connection_aborted(); $attempt++) {
            $messages = coordinatorChatPayload($database, $tester, $afterId);
            if ($messages !== []) {
                $afterId = (int) end($messages)['id'];
                echo "event: messages\ndata: " . json_encode($messages, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES) . "\n\n";
            } else {
                echo ": keepalive\n\n";
            }
            @ob_flush();
            flush();
            sleep(1);
        }
        exit;
    }
    for ($attempt = 0; $attempt < 12; $attempt++) {
        $messages = coordinatorChatPayload($database, $tester, $afterId);
        if ($messages !== []) break;
        sleep(1);
    }
    header('Content-Type: application/json; charset=UTF-8');
    header('Cache-Control: no-store, private');
    echo json_encode(['messages' => $messages ?? []], JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES);
    exit;
}

function renderLiveChatWorkspace(PDO $database, int $selectedTesterId = 0): never
{
    $testers = $database->query("SELECT id, display_name, device, android_version FROM testers WHERE status = 'active' ORDER BY received_at, id")->fetchAll();
    if ($testers === []) {
        renderPage('Live Chat · 24Seven.FM Player', '<p><a href="/private-tester-queue.php">← Tester operations</a></p><h1>Live Chat</h1><section><p class="muted">There are no active tester conversations yet.</p></section>');
    }
    $selected = null;
    foreach ($testers as $candidate) if ((int) $candidate['id'] === $selectedTesterId) $selected = $candidate;
    $selected ??= $testers[0];
    $messages = coordinatorChatPayload($database, $selected);
    $rows = '';
    foreach ($testers as $tester) {
        $active = (int) $tester['id'] === (int) $selected['id'] ? ' style="border-color:#67e6d1;background:#183f3c"' : '';
        $rows .= '<a class="link-button"' . $active . ' href="/private-tester-queue.php?live_chat=1&amp;chat_tester=' . (int) $tester['id'] . '">' . e($tester['display_name']) . '<br><small>' . e($tester['device'] . ' · ' . $tester['android_version']) . '</small></a>';
    }
    $items = '';
    foreach ($messages as $message) {
        $class = $message['role'] === 'coordinator' ? ' style="margin-left:auto;border-color:#67e6d177;background:#183f3c"' : '';
        $items .= '<article data-chat-message-id="' . (int) $message['id'] . '" style="max-width:80%;padding:.7rem .8rem;border:1px solid #33405a;border-radius:.7rem;margin:.65rem 0' . $class . '"><strong>' . e($message['sender']) . '</strong><div>' . nl2br(e($message['body'])) . '</div><small>' . e($message['createdAt']) . '</small><form method="post"><input type="hidden" name="action" value="delete_chat_message"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . (int) $selected['id'] . '"><input type="hidden" name="message_id" value="' . (int) $message['id'] . '"><button class="secondary" type="submit">Delete from my view</button></form></article>';
    }
    if ($items === '') $items = '<p class="muted" data-chat-empty>No messages yet. Use this channel only for tester-program support; never request credentials or secrets.</p>';
    $id = (int) $selected['id'];
    $content = '<p><a href="/private-tester-queue.php">← Tester operations</a> · <a href="/private-tester-queue.php?mail_archive=1">Email archive</a></p><div class="dashboard-header"><div><p class="eyebrow">Private coordinator workspace</p><h1>Live Chat — ' . e($selected['display_name']) . '</h1><p class="muted">Per-tester private conversation. Tester portal access never exposes this coordinator workspace.</p></div><button type="button" class="secondary" data-chat-detach>Detach ↗</button></div><div style="display:grid;grid-template-columns:minmax(12rem,.42fr) minmax(0,1fr);gap:1rem"><section style="margin:0"><h2>Conversations</h2>' . $rows . '</section><section style="margin:0" data-live-chat data-chat-poll="/private-tester-queue.php?chat_poll=1&amp;chat_tester=' . $id . '" data-chat-stream="/private-tester-queue.php?chat_stream=1&amp;chat_tester=' . $id . '"><h2>Live Chat</h2><div data-chat-messages aria-live="polite">' . $items . '</div><form method="post" data-chat-form><input type="hidden" name="action" value="send_chat"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $id . '"><label for="chat_body">Reply to ' . e($selected['display_name']) . '</label><textarea id="chat_body" name="chat_body" maxlength="' . CHAT_MAX_MESSAGE_LENGTH . '" required placeholder="Write a private message…"></textarea><button type="submit">Send</button></form></section></div><script src="/assets/onboarding-live-chat.js" defer></script>';
    renderPage('Live Chat — ' . (string) $selected['display_name'], $content);
}

function renderMailRecord(PDO $database, int $archiveId): never
{
    $statement = $database->prepare('SELECT archive.*, testers.display_name, testers.email FROM tester_mail_archive AS archive JOIN testers ON testers.id = archive.tester_id WHERE archive.id = ?');
    $statement->execute([$archiveId]);
    $record = $statement->fetch();
    if ($record === false) {
        redirect('?mail_archive=1&error=' . rawurlencode('The requested mail record is unavailable.'));
    }
    $html = (string) $record['body_html'];
    if ($html === '') {
        $html = plainTextToHtml((string) $record['body']);
    }
    $content = '<p><a href="/private-tester-queue.php?mail_archive=1">← Sent mail archive</a></p><h1>Archived coordinator email</h1><section><dl><dt>Recipient</dt><dd>' . e($record['display_name']) . ' &lt;' . e($record['email']) . '&gt;</dd><dt>Type</dt><dd>' . e(mailArchiveTypeLabel((string) $record['message_type'])) . '</dd><dt>Prepared</dt><dd>' . e($record['prepared_at']) . '</dd><dt>Handoff</dt><dd>' . e(mailArchiveStatusLabel((string) $record['handoff_status'])) . ($record['attempted_at'] ? ' · ' . e($record['attempted_at']) : '') . '</dd><dt>Subject</dt><dd>' . e($record['subject']) . '</dd></dl></section><section><h2>Rendered HTML</h2><div style="background:#fff;color:#182033;padding:1rem;border-radius:.4rem">' . $html . '</div><h2>Plain-text alternative</h2><pre style="white-space:pre-wrap;font:inherit">' . e($record['body']) . '</pre></section>';
    renderPage('Archived coordinator email', $content);
}

if (defined('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY')) {
    return;
}

try {
    $config = config();
    startSession();
    $database = database($config);
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST' && ($_POST['action'] ?? '') === 'login') {
        $username = (string) ($_POST['username'] ?? '');
        $password = (string) ($_POST['password'] ?? '');
        $loginAllowed = validCsrf()
            && administratorLoginRequestIsSameOrigin()
            && !administratorLoginIsLimited($database, $config)
            && strlen($password) <= ADMIN_LOGIN_MAX_PASSWORD_BYTES;
        $nameAccepted = administratorLoginNameAccepted($config, $username);
        $passwordAccepted = password_verify($password, $config['admin_password_hash']);
        $totpAccepted = true;
        $totpStep = null;
        if (administratorMfaRequired()) {
            $totpCode = (string) ($_POST['totp_code'] ?? '');
            $totpStep = administratorAcceptedTotpStep($totpCode, administratorTotpSecret($config));
            $totpAccepted = $totpStep !== null;
        }
        if ($loginAllowed && $nameAccepted && $passwordAccepted && $totpAccepted && ($totpStep === null || consumeAdministratorTotpStep($database, $totpStep))) {
            session_regenerate_id(true);
            $_SESSION = [
                'authenticated' => true,
                'authenticated_at' => time(),
                'last_seen_at' => time(),
            ];
            clearAdministratorLoginFailures($database, $config);
            csrf();
            redirect();
        }
        if (validCsrf() && administratorLoginRequestIsSameOrigin() && !administratorLoginIsLimited($database, $config)) {
            recordAdministratorLoginFailure($database, $config);
        }
        usleep(250_000);
        renderLogin('We could not sign you in. Check your details and try again later.');
    }
    requireAuthentication($config);

    $chatTesterId = filter_var($_GET['chat_tester'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'GET' && (isset($_GET['chat_stream']) || isset($_GET['chat_poll']))) {
        if ($chatTesterId === false) fail(400, 'The requested chat is invalid.');
        $testerStatement = $database->prepare("SELECT id, display_name FROM testers WHERE id = ? AND status = 'active'");
        $testerStatement->execute([$chatTesterId]);
        $chatTester = $testerStatement->fetch();
        if ($chatTester === false) fail(404, 'The requested chat is unavailable.');
        coordinatorChatPoll($database, $chatTester, max(0, (int) ($_GET['after'] ?? 0)), isset($_GET['chat_stream']));
    }

    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST') {
        if (!validCsrf()) {
            fail(403, 'The request could not be verified.');
        }
        $action = $_POST['action'] ?? '';
        if ($action === 'logout') {
            endSession();
            redirect();
        }
        if ($action === 'send_chat') {
            $testerId = testerId();
            chatPostMessage($database, $testerId, 'coordinator', (string) ($_POST['chat_body'] ?? ''));
            redirect('?live_chat=1&chat_tester=' . $testerId . '&notice=' . rawurlencode('Live Chat message sent.'));
        }
        if ($action === 'delete_chat_message') {
            $testerId = testerId();
            $messageId = filter_var($_POST['message_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
            if ($messageId === false) throw new InvalidArgumentException('The requested chat message is invalid.');
            chatSoftDeleteMessage($database, $testerId, 'coordinator', $messageId);
            redirect('?live_chat=1&chat_tester=' . $testerId . '&notice=' . rawurlencode('Chat message removed from your view.'));
        }
        if ($action === 'accept_application') {
            $testerId = testerId();
            activeTester($database, $testerId);
            markApplicationReviewed($database, $testerId);
            redirect('?notice=' . rawurlencode('Application accepted.'));
        }
        if ($action === 'request_profile_details') {
            $testerId = testerId();
            activeTester($database, $testerId);
            markApplicationReviewed($database, $testerId);
            redirect('?compose_for=' . $testerId . '&notice=' . rawurlencode('Application accepted. Compose the profile-update request below.'));
        }
        if ($action === 'reject_application') {
            $testerId = testerId();
            activeTester($database, $testerId);
            $reason = trim((string) ($_POST['rejection_reason'] ?? ''));
            if ($reason === '' || textLength($reason) > MAX_ONBOARDING_NOTE_LENGTH || str_contains($reason, "\0")) {
                throw new InvalidArgumentException('A rejection reason is required to reject an application.');
            }
            markApplicationReviewed($database, $testerId);
            $database->prepare("UPDATE testers SET status = 'inactive' WHERE id = ? AND status = 'active'")->execute([$testerId]);
            $database->prepare('UPDATE tester_onboarding SET rejected_at = COALESCE(rejected_at, ?), coordinator_note = ? WHERE tester_id = ?')
                ->execute([gmdate('c'), $reason, $testerId]);
            redirect('?notice=' . rawurlencode('Application rejected.'));
        }
        if ($action === 'update_onboarding') {
            $testerId = testerId();
            $tester = $database->prepare('SELECT testers.*, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = \'active\'');
            $tester->execute([$testerId]);
            $tester = $tester->fetch();
            if ($tester === false) {
                throw new InvalidArgumentException('The tester is no longer active.');
            }
            [$status, $note] = onboardingInput($tester);
            saveOnboarding($database, $testerId, $status, $note);
            redirect('?tester=' . $testerId . '&notice=' . rawurlencode('Onboarding progress saved.'));
        }
        if ($action === 'send_onboarding_email') {
            $testerId = testerId();
            $accepted = sendOnboardingEmail($database, $config, $testerId);
            redirect('?tester=' . $testerId . '&notice=' . rawurlencode('Orientation email handoff ' . ($accepted ? 'was accepted by the mail transport.' : 'failed.')));
        }
        if ($action === 'deactivate_withdrawn_tester') {
            $testerId = testerId();
            if (($_POST['confirm_deactivate'] ?? '') !== 'withdraw') {
                throw new InvalidArgumentException('Confirm the verified withdrawal before deactivating this tester.');
            }
            $request = $database->prepare("SELECT onboarding.withdrawal_requested_at FROM testers JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
            $request->execute([$testerId]);
            $request = $request->fetch();
            if ($request === false || !is_string($request['withdrawal_requested_at']) || $request['withdrawal_requested_at'] === '') {
                throw new InvalidArgumentException('A recorded withdrawal request is required before deactivating this tester.');
            }
            $database->prepare("UPDATE testers SET status = 'inactive' WHERE id = ? AND status = 'active'")->execute([$testerId]);
            redirect('?notice=' . rawurlencode('Tester access was deactivated after the recorded withdrawal request.'));
        }
        if ($action === 'assign_task') {
            $testerId = testerId();
            $testerQuery = $database->prepare("SELECT testers.*, onboarding.onboarding_status, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
            $testerQuery->execute([$testerId]);
            $tester = $testerQuery->fetch();
            if ($tester === false) {
                throw new InvalidArgumentException('The tester is no longer active. Refresh the queue and try again.');
            }
            if (!testerReadyForAssignment($tester)) {
                throw new InvalidArgumentException('Focused work can be assigned only after the tester completes Profile & Device, Play Opt-In, and the First-Use Smoke Test.');
            }
            [$task, $station, $configuration, $note, $mutationAuthorized] = assignmentInput(taskRegistry(), $tester);
            $insert = $database->prepare('INSERT INTO tester_task_assignments(tester_id, task_id, task_status, station_scope, configuration_scope, coordinator_note, mutation_authorized, created_at, updated_at) VALUES (?, ?, \'assigned\', ?, ?, ?, ?, ?, ?)');
            $now = gmdate('c');
            $insert->execute([$testerId, $task['id'], $station, $configuration, $note, $mutationAuthorized, $now, $now]);
            $assignmentId = (int) $database->lastInsertId();
            $accepted = sendAssignmentEmail($database, $config, $assignmentId);
            redirect('?tester=' . $testerId . '&notice=' . rawurlencode($task['id'] . ' was assigned; assignment email handoff ' . ($accepted ? 'was accepted by the mail transport.' : 'failed.')));
        }
        if ($action === 'update_task') {
            $id = assignmentId();
            $assignment = $database->prepare('SELECT tester_id, task_id FROM tester_task_assignments WHERE id = ?');
            $assignment->execute([$id]);
            $assignment = $assignment->fetch();
            if ($assignment === false) {
                throw new InvalidArgumentException('The Tester Task assignment is no longer available.');
            }
            activeTester($database, (int) $assignment['tester_id']);
            $status = (string) ($_POST['task_status'] ?? '');
            if (!in_array($status, ASSIGNMENT_STATUSES, true)) {
                throw new InvalidArgumentException('Choose a valid task status.');
            }
            $station = trim((string) ($_POST['station_scope'] ?? ''));
            if (!in_array($station, STATION_SCOPES, true)) {
                throw new InvalidArgumentException('Choose a valid station scope.');
            }
            $configuration = field('configuration_scope', MAX_ASSIGNMENT_CONFIGURATION_LENGTH, false, true);
            $note = field('coordinator_note', MAX_ASSIGNMENT_NOTE_LENGTH, false);
            $update = $database->prepare('UPDATE tester_task_assignments SET task_status = ?, station_scope = ?, configuration_scope = ?, coordinator_note = ?, updated_at = ? WHERE id = ?');
            $update->execute([$status, $station, $configuration, $note, gmdate('c'), $id]);
            redirect('?tester=' . (int) $assignment['tester_id'] . '&notice=' . rawurlencode('Tester Task assignment updated.'));
        }
        if ($action === 'remove_task') {
            $id = assignmentId();
            $assignment = $database->prepare('SELECT tester_id FROM tester_task_assignments WHERE id = ?');
            $assignment->execute([$id]);
            $assignment = $assignment->fetch();
            if ($assignment === false) {
                throw new InvalidArgumentException('The Tester Task assignment is no longer available.');
            }
            activeTester($database, (int) $assignment['tester_id']);
            $database->prepare('DELETE FROM tester_task_assignments WHERE id = ?')->execute([$id]);
            redirect('?tester=' . (int) $assignment['tester_id'] . '&notice=' . rawurlencode('Tester Task assignment removed.'));
        }
        if ($action === 'resend_assignment_email') {
            $id = assignmentId();
            $accepted = sendAssignmentEmail($database, $config, $id);
            $owner = $database->prepare('SELECT tester_id FROM tester_task_assignments WHERE id = ?');
            $owner->execute([$id]);
            $owner = $owner->fetch();
            redirect('?tester=' . (int) ($owner['tester_id'] ?? 0) . '&notice=' . rawurlencode('Assignment email handoff ' . ($accepted ? 'was accepted by the mail transport.' : 'failed.')));
        }
        if ($action === 'prepare') {
            $ids = selectedTesterIds();
            $subject = field('subject', MAX_SUBJECT_LENGTH, true, true);
            $fallbackBody = field('body', MAX_BODY_LENGTH, false);
            $htmlInput = (string) ($_POST['body_html'] ?? '');
            $html = $htmlInput !== '' ? sanitizeHtmlBody($htmlInput) : plainTextToHtml($fallbackBody);
            $body = plainTextFromHtml($html);
            if ($body === '') {
                throw new InvalidArgumentException('Write an email message before reviewing recipients.');
            }
            $testers = selectedActiveTesters($database, $ids);
            $database->beginTransaction();
            $insertBatch = $database->prepare("INSERT INTO email_batches(subject, body, body_html, created_at, status) VALUES (?, ?, ?, ?, 'prepared')");
            $insertBatch->execute([$subject, $body, $html, gmdate('c')]);
            $batchId = (int) $database->lastInsertId();
            $insertRecipient = $database->prepare("INSERT INTO email_batch_recipients(batch_id, tester_id, delivery_status) VALUES (?, ?, 'pending')");
            foreach ($testers as $tester) {
                $insertRecipient->execute([$batchId, $tester['id']]);
            }
            $database->commit();
            redirect('?batch=' . $batchId);
        }
        if ($action === 'send') {
            $batchId = filter_var($_POST['batch_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
            if ($batchId === false) {
                throw new InvalidArgumentException('The prepared email is invalid.');
            }
            $batch = $database->prepare('SELECT subject, body, body_html FROM email_batches WHERE id = ? AND status = \'prepared\'');
            $batch->execute([$batchId]);
            $batch = $batch->fetch();
            if ($batch === false) {
                throw new InvalidArgumentException('This email batch was already sent or is unavailable.');
            }
            $recipients = $database->prepare("SELECT testers.id, testers.email, testers.display_name, onboarding.onboarding_status FROM email_batch_recipients JOIN testers ON testers.id = tester_id LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE batch_id = ? AND delivery_status = 'pending' ORDER BY testers.id");
            $recipients->execute([$batchId]);
            $update = $database->prepare('UPDATE email_batch_recipients SET delivery_status = ?, accepted_at = ? WHERE batch_id = ? AND tester_id = ?');
            $accepted = 0;
            $failed = 0;
            foreach ($recipients->fetchAll() as $recipient) {
                $template = is_string($batch['body_html'] ?? null) && $batch['body_html'] !== '' ? $batch['body_html'] : plainTextToHtml($batch['body']);
                $html = mergeCoordinatorMailHtml($template, $recipient);
                $body = plainTextFromHtml($html);
                $archiveId = prepareMailArchive($database, (int) $recipient['id'], 'batch', $batch['subject'], $body, $html, $batchId);
                $delivered = sendIndividualMail($config, $recipient['email'], $batch['subject'], $body, $html);
                completeMailArchive($database, $archiveId, $delivered);
                $update->execute([$delivered ? 'accepted' : 'failed', gmdate('c'), $batchId, $recipient['id']]);
                $delivered ? $accepted++ : $failed++;
            }
            $database->prepare('UPDATE email_batches SET status = ?, dispatched_at = ? WHERE id = ?')->execute([$failed === 0 ? 'dispatched' : ($accepted === 0 ? 'failed' : 'partial'), gmdate('c'), $batchId]);
            redirect('?notice=' . rawurlencode("Mail handoff recorded: {$accepted} accepted by the server transport, {$failed} failed."));
        }
        throw new InvalidArgumentException('The requested action is invalid.');
    }
    if (isset($_GET['batch']) && ctype_digit((string) $_GET['batch'])) {
        renderConfirmation($database, (int) $_GET['batch']);
    }
    if (isset($_GET['mail_record']) && ctype_digit((string) $_GET['mail_record'])) {
        renderMailRecord($database, (int) $_GET['mail_record']);
    }
    if (isset($_GET['mail_archive'])) {
        renderMailArchive($database);
    }
    if (isset($_GET['email'])) {
        renderOperationsDashboard($database, (string) ($_GET['notice'] ?? ''), (string) ($_GET['error'] ?? ''), true);
    }
    if (isset($_GET['live_chat'])) {
        renderLiveChatWorkspace($database, $chatTesterId === false ? 0 : (int) $chatTesterId);
    }
    if (isset($_GET['tester']) && ctype_digit((string) $_GET['tester'])) {
        renderTesterWorkspace($database, (int) $_GET['tester'], (string) ($_GET['notice'] ?? ''), (string) ($_GET['error'] ?? ''));
    }
    renderDashboard($database, (string) ($_GET['notice'] ?? ''), (string) ($_GET['error'] ?? ''));
} catch (InvalidArgumentException $exception) {
    if (isset($database) && $database instanceof PDO) {
        renderDashboard($database, '', $exception->getMessage());
    }
    fail(400, 'The request is invalid.');
} catch (Throwable $exception) {
    error_log('private-tester-queue request failed: ' . get_class($exception));
    fail(503, 'The private tester queue is temporarily unavailable.');
}
