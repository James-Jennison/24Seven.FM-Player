#!/usr/bin/env php
<?php
declare(strict_types=1);

if (!extension_loaded('pdo_sqlite')) {
    fwrite(STDOUT, "Alpha tester importer contract: skipped (pdo_sqlite unavailable).\n");
    exit(0);
}

function expectImport(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$directory = sys_get_temp_dir() . '/alpha-tester-import-' . bin2hex(random_bytes(8));
mkdir($directory, 0700);
$database = $directory . '/queue.sqlite';
$message = $directory . '/message.eml';
register_shutdown_function(static function () use ($directory): void {
    foreach (glob($directory . '/*') ?: [] as $path) {
        unlink($path);
    }
    rmdir($directory);
});

file_put_contents($message, "From: alpha@example.test\r\n\r\nAlpha tester-interest application\n\nDisplay name: Example Tester\nGoogle Play account email: tester@example.test\nCountry or region: Not provided\nPrimary station: StreamingSoundtracks.com\nOther familiar stations: 1980s.FM, Entranced.FM\nStation accounts available: StreamingSoundtracks.com\nAndroid device: Pixel Test\nAndroid version: Android 16\nDevice form factor: Standard phone\nOther Android devices: Not provided\nTesting interests: Playback and media controls, Network loss and recovery\nNetwork capabilities: Wi-Fi\nAudio/accessory capabilities: Device speaker\nAccessibility/alternative-input capabilities: TalkBack\nTesting comfort level: Controlled live testing\nControlled-action preferences: One authorized song request\nTwo-week availability: About 1–2 hours\nAssignment notes: No formal QA experience\nConsent: Confirmed\n");

$command = escapeshellarg(PHP_BINARY) . ' ' . escapeshellarg(dirname(__DIR__) . '/scripts/import-alpha-tester-mailbox.php')
    . ' --database ' . escapeshellarg($database)
    . ' --uid 42 --received-at 2026-08-13T15:17:40Z --message ' . escapeshellarg($message);
exec($command, $output, $status);
expectImport($status === 0 && $output === ['Imported 1 tester application(s).'], 'New intake email did not import.');

$pdo = new PDO('sqlite:' . $database, null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$row = $pdo->query('SELECT primary_station, other_stations_json, station_accounts_json, device_form_factor, network_capabilities_json, testing_comfort, controlled_actions_json, testing_availability, recruitment_source FROM testers')->fetch(PDO::FETCH_ASSOC);
expectImport(is_array($row), 'Imported tester record is missing.');
expectImport($row['primary_station'] === 'sst' && $row['device_form_factor'] === 'phone' && $row['testing_comfort'] === 'controlled', 'Canonical scalar intake values were not preserved.');
expectImport($row['other_stations_json'] === '["1980s","efm"]' && $row['network_capabilities_json'] === '["wifi"]' && $row['controlled_actions_json'] === '["song_request"]', 'Canonical list intake values were not preserved.');
expectImport($row['testing_availability'] === '1_2h', 'Testing availability was not preserved.');
expectImport($row['recruitment_source'] === 'direct', 'Legacy direct recruitment attribution was not preserved.');
expectImport($pdo->query("SELECT onboarding_status FROM tester_onboarding WHERE tester_id = 1")->fetchColumn() === 'profile_pending', 'Imported applications must enter the private onboarding lifecycle conservatively.');

file_put_contents($message, "From: alpha@example.test\r\n\r\nAlpha tester-interest application\n\nDisplay name: Comma Tester\nGoogle Play account email: comma@example.test\nCountry or region: Not provided\nPrimary station: Death.FM\nOther familiar stations: Not provided\nStation accounts available: Death.FM\nAndroid device: Pixel Test\nAndroid version: Android 16\nDevice form factor: Standard phone\nOther Android devices: Not provided\nTesting interests: Playback and media controls, Queue, History, and station data, Accounts and Favorites, Song request browsing and safety, Chat and community features, Audio devices and accessories, Accessibility and alternative input\nNetwork capabilities: Wi-Fi, Mobile/cellular data, Switching between Wi-Fi and mobile data\nAudio/accessory capabilities: Bluetooth headphones or earbuds, Bluetooth speaker\nAccessibility/alternative-input capabilities: Not provided\nTesting comfort level: Read-only and general testing\nControlled-action preferences: None\nTwo-week availability: Less than 30 minutes\nAssignment notes: Not provided\nConsent: Confirmed\n");
$command = escapeshellarg(PHP_BINARY) . ' ' . escapeshellarg(dirname(__DIR__) . '/scripts/import-alpha-tester-mailbox.php')
    . ' --database ' . escapeshellarg($database)
    . ' --uid 44 --received-at 2026-08-15T14:56:00Z --message ' . escapeshellarg($message);
exec($command, $output, $status);
expectImport($status === 0 && end($output) === 'Imported 1 tester application(s).', 'Comma-containing intake labels did not import.');
$commaLists = $pdo->query("SELECT interests_json, network_capabilities_json, audio_capabilities_json FROM testers WHERE source_message_uid = '44'")->fetch(PDO::FETCH_ASSOC);
expectImport(is_array($commaLists) && $commaLists['interests_json'] === '["playback","queue_history_data","accounts_favorites","request_safety","chat_community","audio_accessories","accessibility"]' && $commaLists['network_capabilities_json'] === '["wifi","mobile_data","network_handoff"]' && $commaLists['audio_capabilities_json'] === '["bluetooth_headphones","bluetooth_speaker"]', 'Comma-containing list labels were not canonicalized correctly.');

file_put_contents($message, "From: alpha@example.test\r\n\r\nDisplay name: Legacy Tester\nGoogle Play account email: legacy@example.test\nCountry or region: Not provided\nAndroid device: Legacy Phone\nAndroid version: Android 15\nInterests: Playback\nPrior testing experience: Not provided\n");
$command = escapeshellarg(PHP_BINARY) . ' ' . escapeshellarg(dirname(__DIR__) . '/scripts/import-alpha-tester-mailbox.php')
    . ' --database ' . escapeshellarg($database)
    . ' --uid 43 --received-at 2026-08-13T15:17:40Z --message ' . escapeshellarg($message);
exec($command, $output, $status);
expectImport($status === 0 && end($output) === 'Imported 1 tester application(s).', 'Legacy intake email did not remain importable.');
$legacy = $pdo->query("SELECT primary_station, other_stations_json, testing_comfort FROM testers WHERE source_message_uid = '43'")->fetch(PDO::FETCH_ASSOC);
expectImport(is_array($legacy) && $legacy['primary_station'] === null && $legacy['other_stations_json'] === '[]' && $legacy['testing_comfort'] === null, 'Legacy tester defaults are not backward compatible.');

fwrite(STDOUT, "Alpha tester importer contract: valid.\n");
