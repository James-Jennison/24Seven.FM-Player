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
        );'
    );
    $columns = $database->query('PRAGMA table_info(email_batches)')->fetchAll();
    $hasHtmlBody = false;
    foreach ($columns as $column) {
        if (($column['name'] ?? '') === 'body_html') {
            $hasHtmlBody = true;
            break;
        }
    }
    if (!$hasHtmlBody) {
        $database->exec('ALTER TABLE email_batches ADD COLUMN body_html TEXT');
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
        . 'body{margin:0;background:#090c15;color:#f7f4ec;font:16px/1.5 system-ui,sans-serif}.shell{max-width:72rem;margin:3rem auto;padding:0 1rem}h1,h2{line-height:1.15}section{margin:1rem 0;padding:1.25rem;border:1px solid #30394d;border-radius:.8rem;background:#111624}.muted{color:#b7bdca}.notice{padding:.8rem;border-radius:.5rem;background:#173631}.error{background:#4a2229}.visually-hidden{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}label{display:block;margin:.75rem 0 .3rem;font-weight:700}input,textarea,select,.editor{box-sizing:border-box;width:100%;padding:.65rem;border:1px solid #59647a;border-radius:.4rem;background:#fff;color:#182033;font:inherit}textarea{min-height:12rem}.toolbar{display:flex;flex-wrap:wrap;gap:.4rem;margin:.35rem 0}.toolbar button{margin:0;padding:.35rem .55rem;background:#e8edf5;color:#182033;font-size:.86rem}.editor{min-height:14rem;overflow:auto}.editor:focus{outline:3px solid #67e6d1;outline-offset:3px}.editor:empty:before{color:#58647a;content:attr(data-placeholder);pointer-events:none}.editor p:first-child{margin-top:0}.editor p:last-child{margin-bottom:0}button{margin-top:1rem;padding:.65rem 1rem;border:0;border-radius:.4rem;background:#67e6d1;color:#071411;font:700 1rem system-ui;cursor:pointer}button.secondary{margin-left:.5rem;background:#d29cff;color:#22102f}table{width:100%;border-collapse:collapse}th,td{padding:.6rem;text-align:left;border-bottom:1px solid #30394d;vertical-align:top}th:first-child,td:first-child{width:2.4rem}small{color:#b7bdca}.actions{display:flex;gap:.6rem;flex-wrap:wrap}.actions form{margin:0}.danger{background:#ffcb6b;color:#2a1a00}@media(max-width:44rem){table{font-size:.86rem}.optional{display:none}}</style></head><body><main class="shell">'
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

function appendSanitizedNodes(DOMDocument $output, DOMNode $source, DOMNode $destination): void
{
    $allowed = [
        'p' => 'p', 'br' => 'br', 'strong' => 'strong', 'b' => 'strong',
        'em' => 'em', 'i' => 'em', 'u' => 'u', 's' => 's', 'strike' => 's',
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
    $loaded = $source->loadHTML('<!doctype html><html><body><div id="queue-editor">' . $html . '</div></body></html>', LIBXML_HTML_NOIMPLIED | LIBXML_HTML_NODEFDTD);
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

function sendIndividualMail(array $config, string $recipient, string $subject, string $plainText, string $html): bool
{
    $senderName = trim((string) ($config['from_name'] ?? '24Seven.FM Player'));
    $from = $config['from_email'];
    $encodedName = '=?UTF-8?B?' . base64_encode($senderName) . '?=';
    $boundary = '=_24Seven_' . bin2hex(random_bytes(16));
    $headers = [
        'From: ' . $encodedName . ' <' . $from . '>',
        'To: ' . $recipient,
        'Reply-To: ' . $from,
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
        smtpWrite($socket, implode("\r\n", $headers) . "\r\nSubject: " . $subject . "\r\n\r\n" . $message . "\r\n.\r\n");
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
    $editor = '<label for="email-template">Email template</label><select id="email-template"><option value="">Custom email</option><option value="welcome">Welcome to internal test</option><option value="feedback">Testing feedback request</option></select><p class="muted">Selecting a template replaces the subject and message; you can edit either before review.</p><label for="body-editor">Message</label><div class="toolbar" role="toolbar" aria-label="Email text formatting"><button type="button" data-format="bold"><strong>B</strong><span class="visually-hidden">Bold</span></button><button type="button" data-format="italic"><em>I</em><span class="visually-hidden">Italic</span></button><button type="button" data-format="underline"><u>U</u><span class="visually-hidden">Underline</span></button><button type="button" data-format="insertUnorderedList">Bullets</button><button type="button" data-format="insertOrderedList">Numbered list</button><button type="button" data-format="createLink">Link</button><button type="button" data-format="removeFormat">Clear format</button></div><div id="body-editor" class="editor" contenteditable="true" role="textbox" aria-multiline="true" data-placeholder="Write a message for the selected testers."></div><input type="hidden" name="body_html" id="body-html"><noscript><label for="body">Message</label><textarea id="body" name="body" maxlength="' . MAX_BODY_LENGTH . '" required></textarea></noscript>';
    $content = '<div class="actions"><div><h1>Private tester queue</h1><p class="muted">' . count($testers) . ' active tester' . (count($testers) === 1 ? '' : 's') . '. Each send is delivered individually; recipients are never exposed to one another.</p></div><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="secondary" type="submit">Sign out</button></form></div>' . $message
        . '<form method="post" id="email-form"><input type="hidden" name="action" value="prepare"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><section><h2>Active testers</h2><p><label><input type="checkbox" id="select-all"> Select all active testers</label></p><table><thead><tr><th scope="col">Select</th><th scope="col">Tester</th><th scope="col">Device</th><th class="optional" scope="col">Coverage</th><th class="optional" scope="col">Experience</th></tr></thead><tbody>' . $rows . '</tbody></table></section><section><h2>Prepare HTML email</h2><p class="muted">Use the formatting bar, then review the selected recipients and rendered message before any delivery is attempted. A plain-text alternative is included for mail clients that cannot show HTML.</p><label for="subject">Subject</label><input id="subject" name="subject" maxlength="' . MAX_SUBJECT_LENGTH . '" required>' . $editor . '<button type="submit">Review selected recipients</button></section></form><script src="/assets/private-tester-queue.js?v=templates-1" defer></script>';
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
