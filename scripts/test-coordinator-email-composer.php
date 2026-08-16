#!/usr/bin/env php
<?php
declare(strict_types=1);

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function expectComposer(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

$html = mergeCoordinatorMailHtml(
    '<p>Hello {{tester_name}},</p><p>{{program_name}} status: {{onboarding_status}}.</p><p><a href="{{tester_portal_url}}">Open your portal</a></p>',
    [
        'display_name' => '<Example Tester>',
        'onboarding_status' => 'ready',
        'primary_station' => 'sst',
        'device_form_factor' => 'phone',
        'network_capabilities_json' => '["wifi"]',
        'audio_capabilities_json' => '["device_speaker"]',
        'accessibility_capabilities_json' => '["general_accessibility"]',
        'testing_comfort' => 'readonly',
        'controlled_actions_json' => '["none"]',
        'testing_availability' => '1_2h',
    ]
);

expectComposer(!str_contains($html, '{{'), 'All supported mail variables must resolve before sending.');
expectComposer(str_contains($html, '&lt;Example Tester&gt;'), 'Recipient names must be HTML-escaped during mail merge.');
expectComposer(str_contains($html, 'Ready for assignment'), 'Onboarding status must resolve using the lifecycle label.');
expectComposer(str_contains($html, TESTER_PORTAL_PUBLIC_URL), 'Tester portal URLs must resolve to the dedicated tester portal.');
expectComposer(str_contains(plainTextFromHtml($html), '<Example Tester>'), 'The plain-text mail alternative must preserve the resolved recipient name.');

fwrite(STDOUT, "Coordinator email composer contract: valid.\n");
