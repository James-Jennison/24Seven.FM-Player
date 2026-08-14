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
const MAX_SUBJECT_LENGTH = 180;
const MAX_BODY_LENGTH = 12_000;
const MAX_HTML_BODY_LENGTH = 24_000;
const MAIL_SUBMISSION_HOST = 'mail.jamesjennison.net';
const TASK_REGISTRY_FILE = 'assets/tester-tasks.json';
const MAX_ASSIGNMENT_CONFIGURATION_LENGTH = 300;
const MAX_ASSIGNMENT_NOTE_LENGTH = 1_000;
const MAX_ONBOARDING_NOTE_LENGTH = 1_000;
const ASSIGNMENT_STATUSES = ['assigned', 'in_progress', 'complete', 'blocked'];
const ONBOARDING_STATUSES = ['profile_pending', 'profile_complete', 'invited', 'orientation_sent', 'ready', 'paused'];
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
            onboarding_status TEXT NOT NULL DEFAULT \'profile_pending\' CHECK(onboarding_status IN (\'profile_pending\', \'profile_complete\', \'invited\', \'orientation_sent\', \'ready\', \'paused\')),
            coordinator_note TEXT NOT NULL DEFAULT \'\',
            orientation_email_status TEXT NOT NULL DEFAULT \'not_sent\' CHECK(orientation_email_status IN (\'not_sent\', \'accepted\', \'failed\')),
            orientation_email_attempted_at TEXT,
            orientation_email_attempts INTEGER NOT NULL DEFAULT 0,
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
            created_at TEXT NOT NULL
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
    ];
    foreach ($testerMigrations as $column => $definition) {
        if (!in_array($column, $testerColumns, true)) {
            $database->exec("ALTER TABLE testers ADD COLUMN {$column} {$definition}");
        }
    }
    return $database;
}

function startSession(): void
{
    session_name(SESSION_NAME);
    session_set_cookie_params([
        'httponly' => true,
        'secure' => true,
        'samesite' => 'Strict',
        'path' => '/',
    ]);
    session_start();
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
    if (password_needs_rehash($config['admin_password_hash'], PASSWORD_DEFAULT)) {
        // The hash remains private; surface no detail to a browser.
        error_log('private-tester-queue password hash should be refreshed');
    }
}

function renderPage(string $title, string $content): never
{
    header('Content-Type: text/html; charset=UTF-8');
    header('Cache-Control: no-store, private');
    header('Pragma: no-cache');
    header('X-Robots-Tag: noindex, nofollow, noarchive');
    header('X-Frame-Options: DENY');
    echo '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>'
        . e($title) . '</title><style>'
        . 'body{margin:0;background:#090c15;color:#f7f4ec;font:16px/1.5 system-ui,sans-serif}.shell{max-width:72rem;margin:3rem auto;padding:0 1rem}h1,h2{line-height:1.15}section{margin:1rem 0;padding:1.25rem;border:1px solid #30394d;border-radius:.8rem;background:#111624}.muted{color:#b7bdca}.notice{padding:.8rem;border-radius:.5rem;background:#173631}.error{background:#4a2229}.warning{padding:.65rem;border-left:3px solid #ffcb6b;background:#302719}.visually-hidden{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}label{display:block;margin:.75rem 0 .3rem;font-weight:700}input,textarea,select,.editor{box-sizing:border-box;width:100%;padding:.65rem;border:1px solid #59647a;border-radius:.4rem;background:#fff;color:#182033;font:inherit}input[type="checkbox"]{width:auto;padding:0}textarea{min-height:12rem}.toolbar{display:flex;flex-wrap:wrap;gap:.4rem;margin:.35rem 0}.toolbar button{margin:0;padding:.35rem .55rem;background:#e8edf5;color:#182033;font-size:.86rem}.editor{min-height:14rem;overflow:auto}.editor:focus{outline:3px solid #67e6d1;outline-offset:3px}.editor:empty:before{color:#58647a;content:attr(data-placeholder);pointer-events:none}.editor p:first-child{margin-top:0}.editor p:last-child{margin-bottom:0}button{margin-top:1rem;padding:.65rem 1rem;border:0;border-radius:.4rem;background:#67e6d1;color:#071411;font:700 1rem system-ui;cursor:pointer}button.secondary{margin-left:.5rem;background:#d29cff;color:#22102f}table{width:100%;border-collapse:collapse}th,td{padding:.6rem;text-align:left;border-bottom:1px solid #30394d;vertical-align:top}th:first-child,td:first-child{width:2.4rem}small{color:#b7bdca}.actions{display:flex;gap:.6rem;flex-wrap:wrap}.actions form{margin:0}.onboarding-overview{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.75rem}.onboarding-metric{padding:.8rem;border:1px solid #3b4964;border-radius:.55rem;background:#0c101c}.onboarding-metric strong{display:block;font-size:1.6rem;color:#67e6d1}.tester-task-panels{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}.tester-task-panel,.assignment-existing,.onboarding-card{min-width:0;padding:1rem;border:1px solid #30394d;border-radius:.6rem}.assignment-existing,.onboarding-card{margin:1rem 0;background:#0c101c}.assignment-existing h4,.tester-task-panel h3,.onboarding-card h4{margin:.1rem 0}.assignment-existing textarea,.tester-task-panel textarea,.onboarding-card textarea{min-height:5rem}.inline-form{margin-top:.5rem}.onboarding-status{display:inline-block;padding:.16rem .5rem;border-radius:99px;background:#233552;color:#dfeaff;font-size:.84rem;font-weight:700}.profile-missing{margin:.65rem 0;padding:.65rem;border-left:3px solid #ffcb6b;background:#302719}.profile-complete{margin:.65rem 0;padding:.65rem;border-left:3px solid #67e6d1;background:#173631}.mutation-authorization{padding:.65rem;border-left:3px solid #ffcb6b;background:#302719}.task-preview{min-height:3rem;padding:.75rem;border:1px solid #30394d;border-radius:.4rem;color:#b7bdca}.task-preview strong{color:#f7f4ec}.copy-assignment{margin-left:.5rem}@media(max-width:44rem){table{font-size:.86rem}.optional{display:none}.onboarding-overview,.tester-task-panels{grid-template-columns:1fr}.shell{margin:1rem auto}.copy-assignment{margin-left:0}}</style></head><body><main class="shell">'
        . $content . '</main></body></html>';
    exit;
}

function renderLogin(string $error = ''): never
{
    $message = $error === '' ? '' : '<p class="notice error">' . e($error) . '</p>';
    renderPage('Private tester queue', '<h1>Private tester queue</h1><p class="muted">Coordinator access only.</p>' . $message
        . '<form method="post" action="/private-tester-queue.php"><input type="hidden" name="action" value="login"><label for="password">Password</label><input id="password" name="password" type="password" autocomplete="current-password" required autofocus><button type="submit">Sign in</button></form>');
}

function field(string $name, int $maximum, bool $required = true, bool $singleLine = false): string
{
    $value = trim((string) ($_POST[$name] ?? ''));
    if (($required && $value === '') || mb_strlen($value) > $maximum || str_contains($value, "\0") || ($singleLine && (str_contains($value, "\r") || str_contains($value, "\n")))) {
        throw new InvalidArgumentException('Please check the email subject and body.');
    }
    return $value;
}

function taskRegistry(): array
{
    static $tasks = null;
    if (is_array($tasks)) {
        return $tasks;
    }
    $path = __DIR__ . '/' . TASK_REGISTRY_FILE;
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
        'station_accounts_json' => 'station account availability',
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

function onboardingStatus(array $tester): string
{
    $profile = profileSummary($tester);
    if (!$profile['complete']) {
        return 'profile_pending';
    }
    $status = $tester['onboarding_status'] ?? 'profile_complete';
    return is_string($status) && in_array($status, ONBOARDING_STATUSES, true) ? $status : 'profile_complete';
}

function onboardingStatusLabel(string $status): string
{
    return match ($status) {
        'profile_pending' => 'Profile update needed',
        'profile_complete' => 'Profile complete',
        'invited' => 'Invitation sent',
        'orientation_sent' => 'Orientation sent',
        'ready' => 'Ready for assignment',
        'paused' => 'Paused',
        default => 'Unknown',
    };
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
        . "Welcome to the 24Seven.FM Player Closed Alpha. Your tester profile is complete, and you are ready for the next step.\n\n"
        . "1. Join the Android test from the project’s Google Play testing link when you receive it.\n"
        . "2. Open your tester portal at https://player.jamesjennison.net/tester-portal.php to keep your device profile current and submit focused task reports.\n"
        . "3. Install or update 24Seven.FM Player, then complete the first-run checklist at https://player.jamesjennison.net/product-testing/.\n"
        . "4. Use only the focused Tester Tasks you are assigned. Each task has its own safety boundary and needs one result per PT case.\n"
        . "5. Send results through the linked feedback form, including the app version, device, Android version, station, steps, and outcome.\n\n"
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
    $accepted = sendIndividualMail($config, $tester['email'], 'Welcome to 24Seven.FM Player Closed Alpha', $message, plainTextToHtml($message));
    $status = onboardingStatus($tester);
    if ($accepted && $status !== 'ready') {
        $status = 'orientation_sent';
    }
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

function assignmentInput(array $tasks): array
{
    $taskId = strtoupper(trim((string) ($_POST['task_id'] ?? '')));
    $task = $tasks[$taskId] ?? null;
    if (!is_array($task)) {
        throw new InvalidArgumentException('Choose a valid Tester Task.');
    }
    if (($task['state'] ?? '') !== 'current') {
        throw new InvalidArgumentException('Future / Blocked Tester Tasks cannot be assigned.');
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
    $statement = $database->prepare('SELECT assignments.id, assignments.task_id, assignments.station_scope, assignments.configuration_scope, assignments.coordinator_note, assignments.mutation_authorized, testers.display_name, testers.email FROM tester_task_assignments AS assignments JOIN testers ON testers.id = assignments.tester_id WHERE assignments.id = ? AND testers.status = \'active\'');
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
    $accepted = sendIndividualMail($config, $assignment['email'], assignmentEmailSubject($task), $message, plainTextToHtml($message));
    $database->prepare('UPDATE tester_task_assignments SET assignment_email_status = ?, assignment_email_attempted_at = ?, assignment_email_attempts = assignment_email_attempts + 1, updated_at = ? WHERE id = ?')
        ->execute([$accepted ? 'accepted' : 'failed', gmdate('c'), gmdate('c'), $assignmentId]);
    return $accepted;
}

function appendSanitizedNodes(DOMDocument $output, DOMNode $source, DOMNode $destination): void
{
    $allowed = [
        'p' => 'p', 'br' => 'br', 'strong' => 'strong', 'b' => 'strong',
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
    if (mb_strlen($html) > MAX_HTML_BODY_LENGTH || str_contains($html, "\0")) {
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
    $statement = $database->prepare("SELECT id, display_name, email FROM testers WHERE status = 'active' AND id IN ($placeholders) ORDER BY id");
    $statement->execute($ids);
    $testers = $statement->fetchAll();
    if (count($testers) !== count($ids)) {
        throw new InvalidArgumentException('One or more selected testers are no longer active. Refresh the queue and try again.');
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

function onboardingBadge(string $status): string
{
    return '<span class="onboarding-status">' . e(onboardingStatusLabel($status)) . '</span>';
}

function renderOperationsDashboard(PDO $database, string $notice = '', string $error = ''): never
{
    $tasks = taskRegistry();
    $testers = $database->query("SELECT testers.*, onboarding.onboarding_status, onboarding.orientation_email_status, onboarding.orientation_email_attempted_at, onboarding.orientation_email_attempts FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.status = 'active' ORDER BY testers.received_at, testers.id")->fetchAll();
    $assignments = $database->query("SELECT tester_id, task_status FROM tester_task_assignments ORDER BY id")->fetchAll();
    $assignmentCounts = [];
    foreach ($assignments as $assignment) {
        $id = (int) $assignment['tester_id'];
        $assignmentCounts[$id] = ($assignmentCounts[$id] ?? 0) + 1;
    }
    $metrics = ['profile_pending' => 0, 'profile_complete' => 0, 'orientation_sent' => 0, 'ready' => 0];
    $rows = '';
    foreach ($testers as $tester) {
        $id = (int) $tester['id'];
        $status = onboardingStatus($tester);
        if (isset($metrics[$status])) {
            $metrics[$status]++;
        }
        $profile = profileSummary($tester);
        $profileText = $profile['complete'] ? 'Complete' : count($profile['missing']) . ' update' . (count($profile['missing']) === 1 ? '' : 's') . ' needed';
        $taskText = ($assignmentCounts[$id] ?? 0) === 0 ? 'No focused task' : ($assignmentCounts[$id] . ' focused task' . ($assignmentCounts[$id] === 1 ? '' : 's'));
        $rows .= '<tr><td><strong>' . e($tester['display_name']) . '</strong><br><small>' . e($tester['email']) . '</small></td><td>' . e($tester['device']) . '<br><small>' . e($tester['android_version']) . '</small></td><td>' . onboardingBadge($status) . '<br><small>' . e($profileText) . '</small></td><td><small>' . e($taskText) . '</small></td><td><a href="/private-tester-queue.php?tester=' . $id . '">Open workspace</a><br><a href="/tester-portal.php?preview_tester=' . $id . '">Preview tester view</a></td></tr>';
    }
    $message = $notice === '' ? '' : '<p class="notice">' . e($notice) . '</p>';
    $message .= $error === '' ? '' : '<p class="notice error">' . e($error) . '</p>';
    $editor = '<label for="email-template">Email template</label><select id="email-template"><option value="">Custom email</option><option value="profile-update">Profile update request</option><option value="welcome">Welcome to the 24Seven.FM Player Closed Alpha Test</option><option value="feedback">Testing feedback request</option><option value="new-build">New build available</option><option value="reminder">Reminder to test</option><option value="known-issue">Known issue / workaround</option><option value="test-session">Test session request</option><option value="test-complete">Thank you / test complete</option><option value="tester-status">Tester status update</option><option value="release-signoff">Release candidate sign-off</option></select><p class="muted">Choose a template or write a custom message. The queue always sends one individual, multipart email per selected tester.</p><label for="body-editor">Message</label><div class="toolbar" role="toolbar" aria-label="Email text formatting"><button type="button" data-format="bold"><strong>B</strong><span class="visually-hidden">Bold</span></button><button type="button" data-format="italic"><em>I</em><span class="visually-hidden">Italic</span></button><button type="button" data-format="underline"><u>U</u><span class="visually-hidden">Underline</span></button><button type="button" data-format="insertUnorderedList">Bullets</button><button type="button" data-format="insertOrderedList">Numbered list</button><button type="button" data-format="createLink">Link</button><button type="button" data-format="removeFormat">Clear format</button></div><div id="body-editor" class="editor" contenteditable="true" role="textbox" aria-multiline="true" data-placeholder="Write a message for the selected testers."></div><input type="hidden" name="body_html" id="body-html"><noscript><label for="body">Message</label><textarea id="body" name="body" maxlength="' . MAX_BODY_LENGTH . '" required></textarea></noscript>';
    $content = '<div class="actions"><div><p class="muted">24Seven.FM Player · closed alpha</p><h1>Tester operations</h1><p class="muted">A private workspace for moving accepted volunteers from a complete profile to a focused, safe testing assignment.</p></div><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="secondary" type="submit">Sign out</button></form></div>' . $message
        . '<section><h2>Onboarding pipeline</h2><div class="onboarding-overview"><div class="onboarding-metric"><strong>' . $metrics['profile_pending'] . '</strong>Profile updates needed</div><div class="onboarding-metric"><strong>' . $metrics['profile_complete'] . '</strong>Ready to invite</div><div class="onboarding-metric"><strong>' . $metrics['orientation_sent'] . '</strong>Orientation sent</div><div class="onboarding-metric"><strong>' . $metrics['ready'] . '</strong>Ready for assignment</div></div><p class="muted">Workflow: confirm coverage → invite and orient → mark ready → assign a focused Tester Task. Enrollment and onboarding stay separate.</p></section>'
        . '<section><h2>Tester roster</h2><p class="muted">Open one workspace to review coverage, record onboarding progress, send an individual orientation email, and manage focused tasks.</p><table><thead><tr><th scope="col">Tester</th><th scope="col">Device</th><th scope="col">Onboarding</th><th scope="col">Assignments</th><th scope="col"><span class="visually-hidden">Open</span></th></tr></thead><tbody>' . $rows . '</tbody></table></section>'
        . '<details><summary><strong>Compose a coordinator email</strong></summary><form method="post" id="email-form"><input type="hidden" name="action" value="prepare"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><section><h2>Recipients and message</h2><p><label><input type="checkbox" id="select-all"> Select all active testers</label></p><table><thead><tr><th scope="col">Select</th><th scope="col">Tester</th><th scope="col">Onboarding</th></tr></thead><tbody>' . implode('', array_map(static fn (array $tester): string => '<tr><td><input aria-label="Select ' . e($tester['display_name']) . '" type="checkbox" name="tester_ids[]" value="' . (int) $tester['id'] . '"></td><td>' . e($tester['display_name']) . '<br><small>' . e($tester['email']) . '</small></td><td>' . onboardingBadge(onboardingStatus($tester)) . '</td></tr>', $testers)) . '</tbody></table><label for="subject">Subject</label><input id="subject" name="subject" maxlength="' . MAX_SUBJECT_LENGTH . '" required>' . $editor . '<button type="submit">Review selected recipients</button></section></form></details><script src="/assets/private-tester-queue.js?v=onboarding-1" defer></script>';
    renderPage('Tester operations', $content);
}

function renderTesterWorkspace(PDO $database, int $testerId, string $notice = '', string $error = ''): never
{
    $testerQuery = $database->prepare("SELECT testers.*, onboarding.onboarding_status, onboarding.coordinator_note AS onboarding_note, onboarding.orientation_email_status, onboarding.orientation_email_attempted_at, onboarding.orientation_email_attempts FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
    $testerQuery->execute([$testerId]);
    $tester = $testerQuery->fetch();
    if ($tester === false) {
        redirect('?error=' . rawurlencode('The tester is no longer active.'));
    }
    $tasks = taskRegistry();
    $assignmentsQuery = $database->prepare('SELECT id, task_id, task_status, station_scope, configuration_scope, coordinator_note, mutation_authorized, assignment_email_status, assignment_email_attempted_at, assignment_email_attempts FROM tester_task_assignments WHERE tester_id = ? ORDER BY created_at, id');
    $assignmentsQuery->execute([$testerId]);
    $assignments = $assignmentsQuery->fetchAll();
    $feedbackQuery = $database->prepare('SELECT feedback.subject, feedback.details, feedback.outcome, feedback.created_at, assignments.task_id FROM tester_feedback AS feedback LEFT JOIN tester_task_assignments AS assignments ON assignments.id = feedback.assignment_id WHERE feedback.tester_id = ? ORDER BY feedback.created_at DESC, feedback.id DESC');
    $feedbackQuery->execute([$testerId]);
    $feedbackItems = '';
    foreach ($feedbackQuery->fetchAll() as $feedback) {
        $feedbackItems .= '<article class="assignment-existing"><h4>' . e(($feedback['task_id'] ?: 'General') . ' · ' . strtoupper((string) $feedback['outcome'])) . '</h4><p><strong>' . e($feedback['subject']) . '</strong><br><small>' . e($feedback['created_at']) . '</small></p><p>' . nl2br(e($feedback['details']), false) . '</p></article>';
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
    $taskOptions = '<option value="">Choose a current Tester Task</option>';
    foreach ($tasks as $task) {
        $taskOptions .= '<option value="' . e($task['id']) . '"' . (($task['state'] ?? '') === 'future' ? ' disabled' : '') . '>' . e($task['id'] . ' — ' . $task['title'] . (($task['state'] ?? '') === 'future' ? ' (Future / Blocked)' : '')) . '</option>';
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
    $content = '<p><a href="/private-tester-queue.php">← Tester operations</a> · <a href="/tester-portal.php?preview_tester=' . $testerId . '">Preview tester view</a></p><div><p class="muted">Tester workspace</p><h1>' . e($tester['display_name']) . '</h1><p class="muted">' . e($tester['device']) . ' · ' . e($tester['android_version']) . ' · ' . e($tester['country'] ?: 'Country not provided') . '</p></div>' . $message
        . '<section><h2>Onboarding</h2>' . onboardingBadge($status) . $profileBlock . '<form method="post"><input type="hidden" name="action" value="update_onboarding"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $testerId . '"><label>Onboarding status<select name="onboarding_status">' . $statusOptions . '</select></label><label>Coordinator note<textarea name="onboarding_note" maxlength="' . MAX_ONBOARDING_NOTE_LENGTH . '" placeholder="Private operational note; never add credentials or secrets.">' . e($tester['onboarding_note'] ?? '') . '</textarea></label><button type="submit">Save onboarding progress</button></form><p><strong>Orientation email:</strong> ' . e($orientation) . ' <small>(' . (int) ($tester['orientation_email_attempts'] ?? 0) . ' handoff attempt' . ((int) ($tester['orientation_email_attempts'] ?? 0) === 1 ? '' : 's') . ')</small></p>' . ($profile['complete'] ? '<form method="post"><input type="hidden" name="action" value="send_onboarding_email"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $testerId . '"><button class="secondary" type="submit">Send orientation email</button></form>' : '') . '</section>'
        . '<section><h2>Focused Tester Tasks</h2><p class="muted">Assign only a focused bundle that matches this tester’s coverage. Each PT case still receives its own result.</p>' . ($assignmentCards === '' ? '<p class="muted">No task assigned yet.</p>' : $assignmentCards) . '<article class="onboarding-card"><h3>Assign a focused Tester Task</h3><form method="post" data-task-assignment-form><input type="hidden" name="action" value="assign_task"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $testerId . '"><label>Tester Task<select name="task_id" data-task-select required>' . $taskOptions . '</select></label><div class="task-preview" data-task-preview aria-live="polite">Choose a current task to see its PT cases, prerequisites, and safety boundary.</div><label>Station scope<select name="station_scope">' . $stationOptions . '</select></label><label>Device / accessory / form factor scope<input name="configuration_scope" maxlength="' . MAX_ASSIGNMENT_CONFIGURATION_LENGTH . '" placeholder="For example, Pixel 9, Bluetooth headset, folded"></label><label>Coordinator note<textarea name="coordinator_note" maxlength="' . MAX_ASSIGNMENT_NOTE_LENGTH . '"></textarea></label><label class="mutation-authorization" data-mutation-authorization hidden><input type="checkbox" name="mutation_authorized" value="1"> I explicitly authorize the controlled subcase described above.</label><button type="submit">Assign Tester Task</button></form></article></section><section><h2>Tester task reports</h2><p class="muted">Private reports submitted through the tester portal.</p>' . ($feedbackItems === '' ? '<p class="muted">No task reports submitted yet.</p>' : $feedbackItems) . '</section><script id="tester-task-registry" type="application/json">' . json_encode(array_values($tasks), JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_UNESCAPED_SLASHES) . '</script><script src="/assets/private-tester-queue.js?v=onboarding-1" defer></script>';
    renderPage('Tester workspace', $content);
}

function renderDashboard(PDO $database, string $notice = '', string $error = ''): never
{
    renderOperationsDashboard($database, $notice, $error);
}

function renderDashboardLegacy(PDO $database, string $notice = '', string $error = ''): never
{
    $tasks = taskRegistry();
    $testers = $database->query("SELECT id, display_name, email, country, device, android_version, interests_json, experience, received_at FROM testers WHERE status = 'active' ORDER BY received_at, id")->fetchAll();
    $assignments = $database->query("SELECT id, tester_id, task_id, task_status, station_scope, configuration_scope, coordinator_note, mutation_authorized, assignment_email_status, assignment_email_attempted_at, assignment_email_attempts, created_at, updated_at FROM tester_task_assignments ORDER BY created_at, id")->fetchAll();
    $assignmentsByTester = [];
    foreach ($assignments as $assignment) {
        if (isset($tasks[$assignment['task_id']])) {
            $assignmentsByTester[(int) $assignment['tester_id']][] = $assignment;
        }
    }
    $rows = '';
    foreach ($testers as $tester) {
        $interests = json_decode($tester['interests_json'], true);
        $interests = is_array($interests) && $interests !== [] ? implode(', ', array_map('strval', $interests)) : 'Not provided';
        $assignmentLinks = '';
        foreach ($assignmentsByTester[(int) $tester['id']] ?? [] as $assignment) {
            $task = $tasks[$assignment['task_id']];
            $assignmentLinks .= '<a href="#tester-' . (int) $tester['id'] . '">' . e($task['id']) . '</a> <small>(' . e(str_replace('_', ' ', $assignment['task_status'])) . ')</small><br>';
        }
        if ($assignmentLinks === '') {
            $assignmentLinks = '<small>Unassigned</small>';
        }
        $rows .= '<tr><td><input aria-label="Select ' . e($tester['display_name']) . '" type="checkbox" name="tester_ids[]" value="' . (int) $tester['id'] . '"></td><td>'
            . e($tester['display_name']) . '<br><small>' . e($tester['email']) . '</small></td><td>' . e($tester['device']) . '<br><small>' . e($tester['android_version']) . '</small></td><td class="optional">' . e($tester['country'] ?: 'Not provided') . '<br><small>' . e($interests) . '</small></td><td class="optional"><small>' . e($tester['experience'] ?: 'Not provided') . '</small></td><td>' . $assignmentLinks . '</td></tr>';
    }
    $message = $notice === '' ? '' : '<p class="notice">' . e($notice) . '</p>';
    $message .= $error === '' ? '' : '<p class="notice error">' . e($error) . '</p>';
    $editor = '<label for="email-template">Email template</label><select id="email-template"><option value="">Custom email</option><option value="welcome">Welcome to the 24Seven.FM Player Closed Alpha Test</option><option value="feedback">Testing feedback request</option><option value="new-build">New build available</option><option value="reminder">Reminder to test</option><option value="known-issue">Known issue / workaround</option><option value="test-session">Test session request</option><option value="test-complete">Thank you / test complete</option><option value="tester-status">Tester status update</option><option value="release-signoff">Release candidate sign-off</option></select><p class="muted">Selecting a template replaces the subject and message; you can edit either before review.</p><label for="body-editor">Message</label><div class="toolbar" role="toolbar" aria-label="Email text formatting"><button type="button" data-format="bold"><strong>B</strong><span class="visually-hidden">Bold</span></button><button type="button" data-format="italic"><em>I</em><span class="visually-hidden">Italic</span></button><button type="button" data-format="underline"><u>U</u><span class="visually-hidden">Underline</span></button><button type="button" data-format="insertUnorderedList">Bullets</button><button type="button" data-format="insertOrderedList">Numbered list</button><button type="button" data-format="createLink">Link</button><button type="button" data-format="removeFormat">Clear format</button></div><div id="body-editor" class="editor" contenteditable="true" role="textbox" aria-multiline="true" data-placeholder="Write a message for the selected testers."></div><input type="hidden" name="body_html" id="body-html"><noscript><label for="body">Message</label><textarea id="body" name="body" maxlength="' . MAX_BODY_LENGTH . '" required></textarea></noscript>';
    $taskOptions = '<option value="">Choose a Tester Task</option>';
    foreach ($tasks as $task) {
        $taskOptions .= '<option value="' . e($task['id']) . '"' . (($task['state'] ?? '') === 'future' ? ' disabled' : '') . '>' . e($task['id'] . ' — ' . $task['title'] . (($task['state'] ?? '') === 'future' ? ' (Future / Blocked)' : '')) . '</option>';
    }
    $stationOptions = '';
    foreach (STATION_SCOPES as $station) {
        $stationOptions .= '<option value="' . e($station) . '">' . e($station) . '</option>';
    }
    $panels = '';
    foreach ($testers as $tester) {
        $id = (int) $tester['id'];
        $existing = '';
        foreach ($assignmentsByTester[$id] ?? [] as $assignment) {
            $task = $tasks[$assignment['task_id']];
            $copy = assignmentMessage($task, $assignment);
            $statusOptions = '';
            foreach (ASSIGNMENT_STATUSES as $status) {
                $statusOptions .= '<option value="' . e($status) . '"' . ($assignment['task_status'] === $status ? ' selected' : '') . '>' . e(ucwords(str_replace('_', ' ', $status))) . '</option>';
            }
            $stationUpdateOptions = '';
            foreach (STATION_SCOPES as $station) {
                $stationUpdateOptions .= '<option value="' . e($station) . '"' . ($assignment['station_scope'] === $station ? ' selected' : '') . '>' . e($station) . '</option>';
            }
            $handoff = $assignment['assignment_email_status'] === 'accepted'
                ? 'Accepted by the mail transport'
                : ($assignment['assignment_email_status'] === 'failed' ? 'Mail transport failed — resend only if appropriate.' : 'Not sent yet');
            $existing .= '<div class="assignment-existing"><h4>' . e($task['id'] . ' — ' . $task['title']) . '</h4><p><strong>PT cases:</strong> ' . e(implode(', ', $task['ptIds'])) . ' · <a href="/product-testing/?task=' . rawurlencode($task['id']) . '">Open cases</a></p><p><strong>Assignment email:</strong> ' . e($handoff) . ' <small>(' . (int) $assignment['assignment_email_attempts'] . ' handoff attempt' . ((int) $assignment['assignment_email_attempts'] === 1 ? '' : 's') . ')</small></p><p class="warning"><strong>Safety:</strong> ' . e($task['safetyWarning']) . '</p><form method="post" class="assignment-update"><input type="hidden" name="action" value="update_task"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="assignment_id" value="' . (int) $assignment['id'] . '"><label>Status<select name="task_status">' . $statusOptions . '</select></label><label>Station scope<select name="station_scope">' . $stationUpdateOptions . '</select></label><label>Device / accessory / form factor scope<input name="configuration_scope" maxlength="' . MAX_ASSIGNMENT_CONFIGURATION_LENGTH . '" value="' . e($assignment['configuration_scope']) . '"></label><label>Coordinator note<textarea name="coordinator_note" maxlength="' . MAX_ASSIGNMENT_NOTE_LENGTH . '">' . e($assignment['coordinator_note']) . '</textarea></label><button type="submit">Save assignment</button><button class="secondary copy-assignment" type="button" data-assignment-copy="' . e($copy) . '">Copy assignment</button></form><form method="post" class="inline-form"><input type="hidden" name="action" value="resend_assignment_email"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="assignment_id" value="' . (int) $assignment['id'] . '"><button class="secondary" type="submit">Send assignment email</button></form><form method="post" class="inline-form"><input type="hidden" name="action" value="remove_task"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="assignment_id" value="' . (int) $assignment['id'] . '"><button class="danger" type="submit">Remove assignment</button></form></div>';
        }
        $panels .= '<article class="tester-task-panel" id="tester-' . $id . '"><h3>' . e($tester['display_name']) . '</h3><p class="muted">' . e($tester['device']) . ' · ' . e($tester['android_version']) . '</p>' . ($existing === '' ? '<p class="muted">No Tester Task assigned yet.</p>' : $existing) . '<form method="post" class="assignment-create" data-task-assignment-form><input type="hidden" name="action" value="assign_task"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="tester_id" value="' . $id . '"><h4>Assign a focused Tester Task</h4><label>Tester Task<select name="task_id" data-task-select required>' . $taskOptions . '</select></label><div class="task-preview" data-task-preview aria-live="polite">Choose a current task to see its PT cases, prerequisites, and safety boundary.</div><label>Station scope<select name="station_scope">' . $stationOptions . '</select></label><label>Device / accessory / form factor scope<input name="configuration_scope" maxlength="' . MAX_ASSIGNMENT_CONFIGURATION_LENGTH . '" placeholder="For example, Pixel 9, Bluetooth headset, folded"></label><label>Coordinator note<textarea name="coordinator_note" maxlength="' . MAX_ASSIGNMENT_NOTE_LENGTH . '" placeholder="Only information the tester needs to carry out this assignment."></textarea></label><label class="mutation-authorization" data-mutation-authorization hidden><input type="checkbox" name="mutation_authorized" value="1"> I explicitly authorize the controlled subcase described above.</label><button type="submit">Assign Tester Task</button></form></article>';
    }
    $registryJson = json_encode(array_values($tasks), JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT | JSON_UNESCAPED_SLASHES);
    $content = '<div class="actions"><div><h1>Private tester queue</h1><p class="muted">' . count($testers) . ' active tester' . (count($testers) === 1 ? '' : 's') . '. Enrollment status and task status are separate. Each send is delivered individually; recipients are never exposed to one another.</p></div><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="secondary" type="submit">Sign out</button></form></div>' . $message
        . '<section><h2>Tester Task assignments</h2><p class="muted">Assign focused bundles, not the entire catalog. Each assignment can contain more than one PT case; testers submit one result for each PT case. Future / Blocked tasks remain visible for planning and cannot be assigned.</p><div class="tester-task-panels">' . $panels . '</div></section>'
        . '<form method="post" id="email-form"><input type="hidden" name="action" value="prepare"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><section><h2>Active testers</h2><p><label><input type="checkbox" id="select-all"> Select all active testers</label></p><table><thead><tr><th scope="col">Select</th><th scope="col">Tester</th><th scope="col">Device</th><th class="optional" scope="col">Coverage</th><th class="optional" scope="col">Experience</th><th scope="col">Tester Tasks</th></tr></thead><tbody>' . $rows . '</tbody></table></section><section><h2>Prepare HTML email</h2><p class="muted">Use the formatting bar, then review the selected recipients and rendered message before any delivery is attempted. A plain-text alternative is included for mail clients that cannot show HTML.</p><label for="subject">Subject</label><input id="subject" name="subject" maxlength="' . MAX_SUBJECT_LENGTH . '" required>' . $editor . '<button type="submit">Review selected recipients</button></section></form><script id="tester-task-registry" type="application/json">' . $registryJson . '</script><script src="/assets/private-tester-queue.js?v=tasks-1" defer></script>';
    renderPage('Private tester queue', $content);
}

function renderConfirmation(PDO $database, int $batchId): never
{
    $batch = $database->prepare('SELECT id, subject, body, body_html FROM email_batches WHERE id = ? AND status = \'prepared\'');
    $batch->execute([$batchId]);
    $batch = $batch->fetch();
    if ($batch === false) {
        redirect('?error=' . rawurlencode('The prepared email is no longer available.'));
    }
    $recipients = $database->prepare('SELECT display_name, email FROM email_batch_recipients JOIN testers ON testers.id = tester_id WHERE batch_id = ? ORDER BY testers.id');
    $recipients->execute([$batchId]);
    $list = '';
    foreach ($recipients->fetchAll() as $recipient) {
        $list .= '<li>' . e($recipient['display_name']) . ' <small>&lt;' . e($recipient['email']) . '&gt;</small></li>';
    }
    $html = is_string($batch['body_html'] ?? null) && $batch['body_html'] !== '' ? $batch['body_html'] : plainTextToHtml($batch['body']);
    $content = '<h1>Review delivery</h1><section><h2>Recipients</h2><p class="muted">Each recipient receives an individual email. No recipient is placed in To, Cc, or Bcc with another tester.</p><ol>' . $list . '</ol></section><section><h2>' . e($batch['subject']) . '</h2><div style="background:#fff;color:#182033;padding:1rem;border-radius:.4rem">' . $html . '</div><h3>Plain-text alternative</h3><pre style="white-space:pre-wrap;font:inherit">' . e($batch['body']) . '</pre></section><form method="post"><input type="hidden" name="action" value="send"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="batch_id" value="' . (int) $batch['id'] . '"><button class="danger" type="submit">Send to these recipients</button></form><p><a href="/private-tester-queue.php">Cancel and return to queue</a></p>';
    renderPage('Review tester email', $content);
}

if (defined('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY')) {
    return;
}

try {
    $config = config();
    startSession();
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST' && ($_POST['action'] ?? '') === 'login') {
        $password = (string) ($_POST['password'] ?? '');
        if (password_verify($password, $config['admin_password_hash'])) {
            session_regenerate_id(true);
            $_SESSION['authenticated'] = true;
            csrf();
            redirect();
        }
        renderLogin('The password was not accepted.');
    }
    requireAuthentication($config);
    $database = database($config);

    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST') {
        if (!validCsrf()) {
            fail(403, 'The request could not be verified.');
        }
        $action = $_POST['action'] ?? '';
        if ($action === 'logout') {
            $_SESSION = [];
            session_destroy();
            redirect();
        }
        if ($action === 'update_onboarding') {
            $testerId = testerId();
            $tester = $database->prepare('SELECT * FROM testers WHERE id = ? AND status = \'active\'');
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
        if ($action === 'assign_task') {
            $testerId = testerId();
            activeTester($database, $testerId);
            [$task, $station, $configuration, $note, $mutationAuthorized] = assignmentInput(taskRegistry());
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
            $recipients = $database->prepare("SELECT testers.id, testers.email FROM email_batch_recipients JOIN testers ON testers.id = tester_id WHERE batch_id = ? AND delivery_status = 'pending' ORDER BY testers.id");
            $recipients->execute([$batchId]);
            $update = $database->prepare('UPDATE email_batch_recipients SET delivery_status = ?, accepted_at = ? WHERE batch_id = ? AND tester_id = ?');
            $accepted = 0;
            $failed = 0;
            foreach ($recipients->fetchAll() as $recipient) {
                $html = is_string($batch['body_html'] ?? null) && $batch['body_html'] !== '' ? $batch['body_html'] : plainTextToHtml($batch['body']);
                $delivered = sendIndividualMail($config, $recipient['email'], $batch['subject'], $batch['body'], $html);
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
