#!/usr/bin/env php
<?php
declare(strict_types=1);

if (!extension_loaded('pdo_sqlite')) {
    fwrite(STDOUT, "Alpha tester auto-onboarding contract: skipped (pdo_sqlite unavailable).\n");
    exit(0);
}

require dirname(__DIR__) . '/privacy-site/tester-onboarding-storage.php';

function expectAutoOnboarding(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$directory = sys_get_temp_dir() . '/alpha-tester-auto-onboarding-' . bin2hex(random_bytes(8));
mkdir($directory, 0700);
$path = $directory . '/testers.sqlite';
register_shutdown_function(static function () use ($directory): void {
    foreach (glob($directory . '/*') ?: [] as $file) {
        unlink($file);
    }
    rmdir($directory);
});

$application = [
    'name' => 'Example Tester',
    'email' => 'tester@example.test',
    'country' => 'United States',
    'device' => 'Pixel Test',
    'android_version' => 'Android 16',
    'interests' => ['playback', 'network_recovery'],
    'experience' => 'Interested in accessibility testing.',
    'primary_station' => 'sst',
    'other_stations' => ['1980s'],
    'station_accounts' => ['none'],
    'device_form_factor' => 'phone',
    'other_devices' => '',
    'network_capabilities' => ['wifi'],
    'audio_capabilities' => ['device_speaker'],
    'accessibility_capabilities' => ['talkback'],
    'testing_comfort' => 'readonly',
    'controlled_actions' => ['none'],
    'testing_availability' => '1_2h',
    'recruitment_source' => 'testers_community',
];

$database = testerOnboardingDatabase($path);
$first = storeTesterOnboardingApplication($database, $application);
expectAutoOnboarding($first['created'], 'A validated public application must create a roster record.');
$record = $database->query('SELECT testers.source_message_uid, testers.status, testers.recruitment_source, onboarding.onboarding_status, onboarding.orientation_email_status FROM testers JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id')->fetch();
expectAutoOnboarding(is_array($record), 'A newly stored application must be visible to the private queue.');
expectAutoOnboarding(str_starts_with($record['source_message_uid'], 'web:'), 'A public application must receive a non-mailbox source identifier.');
expectAutoOnboarding($record['status'] === 'active' && $record['recruitment_source'] === 'testers_community', 'Roster status and approved recruitment attribution were not stored.');
expectAutoOnboarding($record['onboarding_status'] === 'profile_complete' && $record['orientation_email_status'] === 'not_sent', 'Storage must not infer an invitation, orientation, opt-in, or installation.');

$second = storeTesterOnboardingApplication($database, $application);
expectAutoOnboarding(!$second['created'] && $second['id'] === $first['id'], 'A browser retry must not create a duplicate tester.');
expectAutoOnboarding((int) $database->query('SELECT COUNT(*) FROM testers')->fetchColumn() === 1, 'Duplicate email submissions must remain idempotent.');

$application['testing_availability'] = null;
$application['email'] = 'pending@example.test';
$pending = storeTesterOnboardingApplication($database, $application);
$statement = $database->prepare('SELECT onboarding_status FROM tester_onboarding WHERE tester_id = ?');
$statement->execute([$pending['id']]);
expectAutoOnboarding($statement->fetchColumn() === 'profile_pending', 'Incomplete profile data must remain pending instead of being auto-advanced.');

fwrite(STDOUT, "Alpha tester auto-onboarding contract: valid.\n");
