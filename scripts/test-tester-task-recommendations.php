#!/usr/bin/env php
<?php
declare(strict_types=1);

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function expectRecommendation(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

function recommendationTester(array $overrides = []): array
{
    return array_replace([
        'primary_station' => 'sst',
        'device' => 'Example Fold',
        'android_version' => 'Android 16',
        'device_form_factor' => 'phone',
        'interests_json' => '["general"]',
        'station_accounts_json' => '["sst"]',
        'network_capabilities_json' => '["wifi"]',
        'audio_capabilities_json' => '["device_speaker"]',
        'accessibility_capabilities_json' => '["none"]',
        'testing_comfort' => 'readonly',
        'controlled_actions_json' => '["none"]',
        'testing_availability' => '1_2h',
        'onboarding_status' => 'ready',
        'play_opt_in_confirmed_at' => '2026-08-17T17:21:13+00:00',
        'initial_smoke_test_confirmed_at' => '2026-08-17T17:24:30+00:00',
    ], $overrides);
}

$tasks = taskRegistry();
$specialist = recommendationTester([
    'device_form_factor' => 'foldable',
    'interests_json' => '["audio_accessories","network_recovery","adaptive_layouts","accessibility"]',
    'network_capabilities_json' => '["wifi","network_handoff","network_disconnect"]',
    'audio_capabilities_json' => '["bluetooth_headphones","usb_audio"]',
    'accessibility_capabilities_json' => '["talkback"]',
]);
$recommendations = testerTaskRecommendations($tasks, $specialist);
$ids = array_map(static fn (array $recommendation): string => (string) $recommendation['task']['id'], $recommendations);
expectRecommendation($ids === ['TT-04', 'TT-06', 'TT-15', 'TT-16'], 'Specialist coverage must rank matching current tasks first.');
expectRecommendation(str_contains(assignmentConfigurationPrefill($specialist), 'Example Fold (Android 16)') && str_contains(assignmentConfigurationPrefill($specialist), 'Form factor: Foldable / flip phone'), 'Assignment scope must prefill the registered device profile.');
expectRecommendation(assignmentStationScopePrefill($specialist) === 'StreamingSoundtracks.com', 'Assignment station scope must prefill the tester primary station.');

$guest = recommendationTester(['station_accounts_json' => '["none"]', 'interests_json' => '["audio_accessories","accounts_favorites"]', 'audio_capabilities_json' => '["bluetooth_headphones"]']);
$guestIds = array_map(static fn (array $recommendation): string => (string) $recommendation['task']['id'], testerTaskRecommendations($tasks, $guest));
expectRecommendation(in_array('TT-04', $guestIds, true) && !in_array('TT-07', $guestIds, true), 'Guest recommendations must remain account-free.');
expectRecommendation(testerTaskRecommendations($tasks, recommendationTester(['initial_smoke_test_confirmed_at' => ''])) === [], 'Recommendations must remain unavailable until the required tester evidence is complete.');

fwrite(STDOUT, "Tester Task recommendation contract: valid.\n");
