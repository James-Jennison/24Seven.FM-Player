#!/usr/bin/env php
<?php
declare(strict_types=1);

/**
 * One-time/private importer for Alpha tester-interest messages.
 *
 * Invoke this only on a trusted host with individual RFC 822 messages written
 * outside the repository. Nothing in this script supplies mailbox credentials
 * or an email address. A message UID is retained solely for idempotency.
 *
 * php scripts/import-alpha-tester-mailbox.php --database /secure/queue.sqlite \
 *   --uid 12 --received-at 2026-08-12T16:19:03Z --message /secure/message-12.eml
 *
 * Use --message - only for a single message streamed through standard input.
 */

function usage(): never
{
    fwrite(STDERR, "Usage: import-alpha-tester-mailbox.php --database PATH --uid UID --received-at ISO-8601 --message RFC822_FILE [--message ...]\n");
    exit(64);
}

function value(array $options, string $name): string
{
    $value = $options[$name] ?? null;
    if (!is_string($value) || $value === '') {
        usage();
    }
    return $value;
}

function database(string $path): PDO
{
    $database = new PDO('sqlite:' . $path, null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
    $database->exec('PRAGMA foreign_keys = ON');
    $database->exec("CREATE TABLE IF NOT EXISTS testers (
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
        status TEXT NOT NULL CHECK(status IN ('active', 'inactive')),
        imported_at TEXT NOT NULL
    )");
    return $database;
}

function messageBody(string $raw): string
{
    $parts = preg_split("/\r?\n\r?\n/", $raw, 2);
    return $parts[1] ?? $raw;
}

function parseFields(string $raw): array
{
    $body = messageBody($raw);
    $labels = [
        'name' => 'Display name',
        'email' => 'Google Play account email',
        'country' => 'Country or region',
        'device' => 'Android device',
        'android_version' => 'Android version',
        'interests' => 'Interests',
        'experience' => 'Prior testing experience',
    ];
    $fields = [];
    foreach ($labels as $key => $label) {
        if (!preg_match('/^' . preg_quote($label, '/') . ':\\s*(.*)$/mi', $body, $match)) {
            throw new RuntimeException("Message does not contain {$label}.");
        }
        $fields[$key] = trim($match[1]);
    }
    if (!filter_var($fields['email'], FILTER_VALIDATE_EMAIL) || $fields['name'] === '' || $fields['device'] === '' || $fields['android_version'] === '') {
        throw new RuntimeException('Message fields are invalid.');
    }
    $fields['country'] = $fields['country'] === 'Not provided' ? null : $fields['country'];
    $fields['experience'] = $fields['experience'] === 'Not provided' ? null : $fields['experience'];
    $fields['interests'] = $fields['interests'] === 'Not provided' ? [] : array_values(array_filter(array_map('trim', explode(',', $fields['interests']))));
    return $fields;
}

$options = getopt('', ['database:', 'uid:', 'received-at:', 'message:']);
$databasePath = value($options, 'database');
$uid = value($options, 'uid');
$receivedAt = value($options, 'received-at');
$messages = $options['message'] ?? [];
if (!is_array($messages)) {
    $messages = [$messages];
}
if ($messages === [] || !preg_match('/^[0-9A-Za-z._:-]+$/', $uid) || strtotime($receivedAt) === false) {
    usage();
}

$database = database($databasePath);
$insert = $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, country, device, android_version, interests_json, experience, status, imported_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?) ON CONFLICT(source_message_uid) DO NOTHING");
$imported = 0;
foreach ($messages as $offset => $messagePath) {
    if (!is_string($messagePath)) {
        throw new RuntimeException('A message file is unavailable.');
    }
    if ($messagePath === '-') {
        if (count($messages) !== 1) {
            throw new RuntimeException('Standard input can contain only one message.');
        }
        $raw = stream_get_contents(STDIN);
    } elseif (is_file($messagePath)) {
        $raw = file_get_contents($messagePath);
    } else {
        throw new RuntimeException('A message file is unavailable.');
    }
    if (!is_string($raw)) {
        throw new RuntimeException('The message could not be read.');
    }
    $fields = parseFields($raw);
    $sourceId = count($messages) === 1 ? $uid : $uid . '-' . ($offset + 1);
    $insert->execute([$sourceId, $receivedAt, $fields['name'], $fields['email'], $fields['country'], $fields['device'], $fields['android_version'], json_encode($fields['interests'], JSON_THROW_ON_ERROR), $fields['experience'], gmdate('c')]);
    $imported += $insert->rowCount();
}
fwrite(STDOUT, "Imported {$imported} tester application(s).\n");
