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
        );'
    );
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

function e(string|null $value): string
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
        . 'body{margin:0;background:#090c15;color:#f7f4ec;font:16px/1.5 system-ui,sans-serif}.shell{max-width:72rem;margin:3rem auto;padding:0 1rem}h1,h2{line-height:1.15}section{margin:1rem 0;padding:1.25rem;border:1px solid #30394d;border-radius:.8rem;background:#111624}.muted{color:#b7bdca}.notice{padding:.8rem;border-radius:.5rem;background:#173631}.error{background:#4a2229}label{display:block;margin:.75rem 0 .3rem;font-weight:700}input,textarea{box-sizing:border-box;width:100%;padding:.65rem;border:1px solid #59647a;border-radius:.4rem;background:#fff;color:#182033;font:inherit}textarea{min-height:12rem}button{margin-top:1rem;padding:.65rem 1rem;border:0;border-radius:.4rem;background:#67e6d1;color:#071411;font:700 1rem system-ui;cursor:pointer}button.secondary{margin-left:.5rem;background:#d29cff;color:#22102f}table{width:100%;border-collapse:collapse}th,td{padding:.6rem;text-align:left;border-bottom:1px solid #30394d;vertical-align:top}th:first-child,td:first-child{width:2.4rem}small{color:#b7bdca}.actions{display:flex;gap:.6rem;flex-wrap:wrap}.actions form{margin:0}.danger{background:#ffcb6b;color:#2a1a00}@media(max-width:44rem){table{font-size:.86rem}.optional{display:none}}</style></head><body><main class="shell">'
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

function sendIndividualMail(array $config, string $recipient, string $subject, string $body): bool
{
    $senderName = trim((string) ($config['from_name'] ?? '24Seven.FM Player'));
    $from = $config['from_email'];
    $encodedName = '=?UTF-8?B?' . base64_encode($senderName) . '?=';
    $headers = [
        'From: ' . $encodedName . ' <' . $from . '>',
        'Reply-To: ' . $from,
        'MIME-Version: 1.0',
        'Content-Type: text/plain; charset=UTF-8',
        'Content-Transfer-Encoding: 8bit',
    ];
    return mail($recipient, $subject, $body, implode("\r\n", $headers), '-f' . $from);
}

function renderDashboard(PDO $database, string $notice = '', string $error = ''): never
{
    $testers = $database->query("SELECT id, display_name, email, country, device, android_version, interests_json, experience, received_at FROM testers WHERE status = 'active' ORDER BY received_at, id")->fetchAll();
    $rows = '';
    foreach ($testers as $tester) {
        $interests = json_decode($tester['interests_json'], true);
        $interests = is_array($interests) && $interests !== [] ? implode(', ', array_map('strval', $interests)) : 'Not provided';
        $rows .= '<tr><td><input aria-label="Select ' . e($tester['display_name']) . '" type="checkbox" name="tester_ids[]" value="' . (int) $tester['id'] . '"></td><td>'
            . e($tester['display_name']) . '<br><small>' . e($tester['email']) . '</small></td><td>' . e($tester['device']) . '<br><small>' . e($tester['android_version']) . '</small></td><td class="optional">' . e($tester['country'] ?: 'Not provided') . '<br><small>' . e($interests) . '</small></td><td class="optional"><small>' . e($tester['experience'] ?: 'Not provided') . '</small></td></tr>';
    }
    $message = $notice === '' ? '' : '<p class="notice">' . e($notice) . '</p>';
    $message .= $error === '' ? '' : '<p class="notice error">' . e($error) . '</p>';
    $content = '<div class="actions"><div><h1>Private tester queue</h1><p class="muted">' . count($testers) . ' active tester' . (count($testers) === 1 ? '' : 's') . '. Each send is delivered individually; recipients are never exposed to one another.</p></div><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="secondary" type="submit">Sign out</button></form></div>' . $message
        . '<form method="post"><input type="hidden" name="action" value="prepare"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><section><h2>Active testers</h2><p><label><input type="checkbox" id="select-all"> Select all active testers</label></p><table><thead><tr><th scope="col">Select</th><th scope="col">Tester</th><th scope="col">Device</th><th class="optional" scope="col">Coverage</th><th class="optional" scope="col">Experience</th></tr></thead><tbody>' . $rows . '</tbody></table></section><section><h2>Prepare email</h2><p class="muted">The next step shows the selected recipients and the final message before any delivery is attempted.</p><label for="subject">Subject</label><input id="subject" name="subject" maxlength="' . MAX_SUBJECT_LENGTH . '" required><label for="body">Message</label><textarea id="body" name="body" maxlength="' . MAX_BODY_LENGTH . '" required></textarea><button type="submit">Review selected recipients</button></section></form><script>document.getElementById("select-all").addEventListener("change",function(){document.querySelectorAll("input[name=\"tester_ids[]\"]").forEach(function(box){box.checked=event.target.checked;});});</script>';
    renderPage('Private tester queue', $content);
}

function renderConfirmation(PDO $database, int $batchId): never
{
    $batch = $database->prepare('SELECT id, subject, body FROM email_batches WHERE id = ? AND status = \'prepared\'');
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
    $content = '<h1>Review delivery</h1><section><h2>Recipients</h2><p class="muted">Each recipient receives an individual email. No recipient is placed in To, Cc, or Bcc with another tester.</p><ol>' . $list . '</ol></section><section><h2>' . e($batch['subject']) . '</h2><pre style="white-space:pre-wrap;font:inherit">' . e($batch['body']) . '</pre></section><form method="post"><input type="hidden" name="action" value="send"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="batch_id" value="' . (int) $batch['id'] . '"><button class="danger" type="submit">Send to these recipients</button></form><p><a href="/private-tester-queue.php">Cancel and return to queue</a></p>';
    renderPage('Review tester email', $content);
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
        if ($action === 'prepare') {
            $ids = selectedTesterIds();
            $subject = field('subject', MAX_SUBJECT_LENGTH, true, true);
            $body = field('body', MAX_BODY_LENGTH);
            $testers = selectedActiveTesters($database, $ids);
            $database->beginTransaction();
            $insertBatch = $database->prepare("INSERT INTO email_batches(subject, body, created_at, status) VALUES (?, ?, ?, 'prepared')");
            $insertBatch->execute([$subject, $body, gmdate('c')]);
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
            $batch = $database->prepare('SELECT subject, body FROM email_batches WHERE id = ? AND status = \'prepared\'');
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
                $delivered = sendIndividualMail($config, $recipient['email'], $batch['subject'], $batch['body']);
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
