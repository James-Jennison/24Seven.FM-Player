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
    $columns = array_column($database->query('PRAGMA table_info(testers)')->fetchAll(), 'name');
    $migrations = [
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
    foreach ($migrations as $column => $definition) {
        if (!in_array($column, $columns, true)) {
            $database->exec("ALTER TABLE testers ADD COLUMN {$column} {$definition}");
        }
    }
    $database->exec("CREATE TABLE IF NOT EXISTS tester_onboarding (
        tester_id INTEGER PRIMARY KEY REFERENCES testers(id) ON DELETE CASCADE,
        onboarding_status TEXT NOT NULL DEFAULT 'profile_pending' CHECK(onboarding_status IN ('profile_pending', 'profile_complete', 'invited', 'orientation_sent', 'ready', 'paused')),
        coordinator_note TEXT NOT NULL DEFAULT '',
        orientation_email_status TEXT NOT NULL DEFAULT 'not_sent' CHECK(orientation_email_status IN ('not_sent', 'accepted', 'failed')),
        orientation_email_attempted_at TEXT,
        orientation_email_attempts INTEGER NOT NULL DEFAULT 0,
        play_opt_in_confirmed_at TEXT,
        initial_smoke_test_confirmed_at TEXT,
        withdrawal_requested_at TEXT,
        deletion_requested_at TEXT,
        updated_at TEXT NOT NULL
    )");
    return $database;
}

function messageBody(string $raw): string
{
    $parts = preg_split("/\r?\n\r?\n/", $raw, 2);
    return $parts[1] ?? $raw;
}

const INTAKE_VALUE_MAPS = [
    'primary_station' => [
        'StreamingSoundtracks.com' => 'sst', '1980s.FM' => '1980s', 'Adagio.FM' => 'afm', 'Death.FM' => 'dfm', 'Entranced.FM' => 'efm',
        'I regularly listen to more than one' => 'multiple', "I don't have a primary station" => 'none',
    ],
    'other_stations' => ['StreamingSoundtracks.com' => 'sst', '1980s.FM' => '1980s', 'Adagio.FM' => 'afm', 'Death.FM' => 'dfm', 'Entranced.FM' => 'efm'],
    'station_accounts' => ['StreamingSoundtracks.com' => 'sst', '1980s.FM' => '1980s', 'Adagio.FM' => 'afm', 'Death.FM' => 'dfm', 'Entranced.FM' => 'efm', 'None' => 'none'],
    'device_form_factor' => ['Standard phone' => 'phone', 'Foldable / flip phone' => 'foldable', 'Android tablet' => 'tablet', 'Chromebook with Android app support' => 'chromebook', 'Other Android device' => 'other'],
    'interests' => ['Playback and media controls' => 'playback', 'Queue, History, and station data' => 'queue_history_data', 'Accounts and Favorites' => 'accounts_favorites', 'Song request browsing and safety' => 'request_safety', 'Chat and community features' => 'chat_community', 'Network loss and recovery' => 'network_recovery', 'Audio devices and accessories' => 'audio_accessories', 'Foldable, tablet, and adaptive layouts' => 'adaptive_layouts', 'Accessibility and alternative input' => 'accessibility', 'General testing / anything needed' => 'general', 'Playback' => 'playback', 'Foldable and tablet' => 'adaptive_layouts', 'Accessibility' => 'accessibility', 'Account and community' => 'accounts_favorites', 'General testing' => 'general'],
    'network_capabilities' => ['Wi-Fi' => 'wifi', 'Mobile/cellular data' => 'mobile_data', 'Switching between Wi-Fi and mobile data' => 'network_handoff', 'Temporarily disconnecting network access to test recovery' => 'network_disconnect'],
    'audio_capabilities' => ['Device speaker' => 'device_speaker', 'Bluetooth headphones or earbuds' => 'bluetooth_headphones', 'Bluetooth speaker' => 'bluetooth_speaker', 'Wired headphones/headset' => 'wired_headphones', 'USB audio device' => 'usb_audio', 'Android Auto / automotive media' => 'android_auto', 'Hearing aid / assistive audio device' => 'hearing_aid', 'HDMI / external display audio' => 'hdmi_audio', 'External keyboard or mouse/trackpad' => 'external_input', 'None beyond the device itself' => 'none'],
    'accessibility_capabilities' => ['TalkBack' => 'talkback', 'Large text / enlarged display' => 'large_text', 'Voice Access' => 'voice_access', 'Switch Access' => 'switch_access', 'Accessibility Scanner / touch-target review' => 'accessibility_scanner', 'External keyboard' => 'external_keyboard', 'Mouse / trackpad' => 'mouse_trackpad', 'General accessibility testing' => 'general_accessibility', 'None' => 'none'],
    'testing_comfort' => ['Read-only and general testing' => 'readonly', 'Account testing' => 'account', 'Controlled live testing' => 'controlled', 'Any appropriate testing' => 'any'],
    'controlled_actions' => ['One authorized song request' => 'song_request', 'One harmless authorized Chat message' => 'chat_message', 'Two-account Chat mention testing' => 'chat_mention', 'Sign-in / sign-out / session testing' => 'session_testing', 'Report/block/unblock workflow without sending a moderation email' => 'report_block', 'General account-based testing' => 'account_testing', 'None' => 'none'],
    'testing_availability' => ['Less than 30 minutes' => 'under_30m', 'About 30–60 minutes' => '30_60m', 'About 1–2 hours' => '1_2h', 'About 2–4 hours' => '2_4h', 'More than 4 hours' => 'over_4h', 'It varies' => 'varies'],
];

function parseFields(string $raw): array
{
    $body = messageBody($raw);
    $labels = [
        'name' => 'Display name',
        'email' => 'Google Play account email',
        'country' => 'Country or region',
        'device' => 'Android device',
        'android_version' => 'Android version',
        'interests' => 'Testing interests',
        'experience' => 'Assignment notes',
    ];
    $fields = [];
    foreach ($labels as $key => $label) {
        if (!preg_match('/^' . preg_quote($label, '/') . ':\\s*(.*)$/mi', $body, $match)) {
            if ($key === 'interests' && preg_match('/^Interests:\\s*(.*)$/mi', $body, $match)) {
                // Legacy application mail before assignment-preference intake.
            } elseif ($key === 'experience' && preg_match('/^Prior testing experience:\\s*(.*)$/mi', $body, $match)) {
                // Legacy application mail before assignment-preference intake.
            } else {
                throw new RuntimeException("Message does not contain {$label}.");
            }
        }
        $fields[$key] = trim($match[1]);
    }
    if (!filter_var($fields['email'], FILTER_VALIDATE_EMAIL) || $fields['name'] === '' || $fields['device'] === '' || $fields['android_version'] === '') {
        throw new RuntimeException('Message fields are invalid.');
    }
    $fields['country'] = $fields['country'] === 'Not provided' ? null : $fields['country'];
    $fields['experience'] = $fields['experience'] === 'Not provided' ? null : $fields['experience'];
    $fields['interests'] = $fields['interests'] === 'Not provided' ? [] : array_values(array_filter(array_map('trim', explode(',', $fields['interests']))));
    $optional = [
        'primary_station' => 'Primary station',
        'other_stations' => 'Other familiar stations',
        'station_accounts' => 'Station accounts available',
        'device_form_factor' => 'Device form factor',
        'other_devices' => 'Other Android devices',
        'network_capabilities' => 'Network capabilities',
        'audio_capabilities' => 'Audio/accessory capabilities',
        'accessibility_capabilities' => 'Accessibility/alternative-input capabilities',
        'testing_comfort' => 'Testing comfort level',
        'controlled_actions' => 'Controlled-action preferences',
        'testing_availability' => 'Two-week availability',
    ];
    foreach ($optional as $key => $label) {
        $fields[$key] = preg_match('/^' . preg_quote($label, '/') . ':\\s*(.*)$/mi', $body, $match) ? trim($match[1]) : 'Not provided';
    }
    $fields['recruitment_source'] = preg_match('/^Recruitment source:\s*(.*)$/mi', $body, $match) ? trim($match[1]) : 'Direct / project site';
    return $fields;
}

function canonicalRecruitmentSource(string $value): string
{
    return match ($value) {
        'Direct / project site' => 'direct',
        'Testers Community' => 'testers_community',
        'Betabound' => 'betabound',
        'BetaFamily' => 'betafamily',
        'Other approved source' => 'other',
        default => throw new RuntimeException('Unknown recruitment source.'),
    };
}

function canonicalValue(string $field, string $value): ?string
{
    if ($value === 'Not provided') {
        return null;
    }
    $canonical = INTAKE_VALUE_MAPS[$field][$value] ?? null;
    if ($canonical === null) {
        throw new RuntimeException("Unknown {$field} value.");
    }
    return $canonical;
}

function canonicalList(string $field, string $value): array
{
    if ($value === 'Not provided') {
        return [];
    }
    $values = array_values(array_filter(array_map('trim', explode(',', $value))));
    $canonical = [];
    foreach ($values as $item) {
        $candidate = canonicalValue($field, $item);
        if ($candidate !== null) {
            $canonical[$candidate] = true;
        }
    }
    if (isset($canonical['none'])) {
        return ['none'];
    }
    return array_keys($canonical);
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
$insert = $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, country, device, android_version, interests_json, experience, status, imported_at, primary_station, other_stations_json, station_accounts_json, device_form_factor, other_devices, network_capabilities_json, audio_capabilities_json, accessibility_capabilities_json, testing_comfort, controlled_actions_json, testing_availability, recruitment_source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(source_message_uid) DO NOTHING");
$onboarding = $database->prepare("INSERT OR IGNORE INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempted_at, orientation_email_attempts, updated_at) SELECT id, 'profile_pending', '', 'not_sent', NULL, 0, ? FROM testers WHERE source_message_uid = ?");
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
    $insert->execute([
        $sourceId,
        $receivedAt,
        $fields['name'],
        $fields['email'],
        $fields['country'],
        $fields['device'],
        $fields['android_version'],
        json_encode($fields['interests'] === [] ? [] : canonicalList('interests', implode(', ', $fields['interests'])), JSON_THROW_ON_ERROR),
        $fields['experience'],
        gmdate('c'),
        canonicalValue('primary_station', $fields['primary_station']),
        json_encode(canonicalList('other_stations', $fields['other_stations']), JSON_THROW_ON_ERROR),
        json_encode(canonicalList('station_accounts', $fields['station_accounts']), JSON_THROW_ON_ERROR),
        canonicalValue('device_form_factor', $fields['device_form_factor']),
        $fields['other_devices'] === 'Not provided' ? null : $fields['other_devices'],
        json_encode(canonicalList('network_capabilities', $fields['network_capabilities']), JSON_THROW_ON_ERROR),
        json_encode(canonicalList('audio_capabilities', $fields['audio_capabilities']), JSON_THROW_ON_ERROR),
        json_encode(canonicalList('accessibility_capabilities', $fields['accessibility_capabilities']), JSON_THROW_ON_ERROR),
        canonicalValue('testing_comfort', $fields['testing_comfort']),
        json_encode(canonicalList('controlled_actions', $fields['controlled_actions']), JSON_THROW_ON_ERROR),
        canonicalValue('testing_availability', $fields['testing_availability']),
        canonicalRecruitmentSource($fields['recruitment_source']),
    ]);
    $onboarding->execute([gmdate('c'), $sourceId]);
    $imported += $insert->rowCount();
}
fwrite(STDOUT, "Imported {$imported} tester application(s).\n");
