#!/usr/bin/env php
<?php
declare(strict_types=1);

putenv('ALPHA_TESTER_INTEREST_TEST_LIBRARY=1');
require dirname(__DIR__) . '/privacy-site/alpha-tester-interest.php';

function expectIntake(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

function expectInvalid(callable $callback, string $message): void
{
    try {
        $callback();
    } catch (InvalidArgumentException) {
        return;
    }
    throw new RuntimeException($message);
}

function validApplication(): array
{
    return [
        'name' => 'Example Tester',
        'email' => 'tester@example.test',
        'country' => 'United States',
        'primaryStation' => 'sst',
        'otherStations' => ['sst', '1980s', '1980s'],
        'stationAccounts' => ['sst', 'none'],
        'device' => 'Pixel Test',
        'androidVersion' => 'Android 16',
        'deviceFormFactor' => 'phone',
        'otherDevices' => 'Tablet test device',
        'interests' => ['playback', 'network_recovery', 'playback'],
        'networkCapabilities' => ['wifi', 'network_handoff'],
        'audioCapabilities' => ['device_speaker', 'none'],
        'accessibilityCapabilities' => ['talkback', 'none'],
        'testingComfort' => 'controlled',
        'controlledActions' => ['song_request', 'none'],
        'testingAvailability' => '1_2h',
        'experience' => 'Interested in accessibility testing.',
        'company' => '',
        'consent' => 'yes',
    ];
}

$_POST = validApplication();
$application = applicationFromRequest();
expectIntake($application['primary_station'] === 'sst', 'Primary station must retain its canonical ID.');
expectIntake($application['other_stations'] === ['1980s'], 'Primary-station and duplicate other-station values must normalize safely.');
expectIntake($application['station_accounts'] === ['none'], 'None must be exclusive for station accounts.');
expectIntake($application['audio_capabilities'] === ['none'], 'None must be exclusive for audio capabilities.');
expectIntake($application['accessibility_capabilities'] === ['none'], 'None must be exclusive for accessibility capabilities.');
expectIntake($application['controlled_actions'] === ['none'], 'None must be exclusive for controlled actions.');
expectIntake($application['interests'] === ['playback', 'network_recovery'], 'Duplicate checkbox values must normalize.');

expectIntake(turnstileVerificationAccepted(['success' => true, 'action' => TURNSTILE_ACTION, 'hostname' => TURNSTILE_HOSTNAME]), 'A matching Turnstile verification must be accepted.');
expectIntake(!turnstileVerificationAccepted(['success' => true, 'action' => 'turnstile-test', 'hostname' => TURNSTILE_HOSTNAME]), 'A Turnstile action mismatch must be rejected.');
expectIntake(!turnstileVerificationAccepted(['success' => true, 'action' => TURNSTILE_ACTION, 'hostname' => 'example.test']), 'A Turnstile hostname mismatch must be rejected.');

foreach (array_keys(PRIMARY_STATIONS) as $station) {
    $_POST = validApplication();
    $_POST['primaryStation'] = $station;
    expectIntake(applicationFromRequest()['primary_station'] === $station, "Primary station {$station} was rejected.");
}

foreach ([
    ['primaryStation', 'unknown'], ['deviceFormFactor', 'watch'], ['testingComfort', 'unsafe'], ['testingAvailability', 'always'],
] as [$field, $value]) {
    $_POST = validApplication();
    $_POST[$field] = $value;
    expectInvalid(static fn (): array => applicationFromRequest(), "{$field} must be allowlisted.");
}
foreach ([['otherStations', ['unknown']], ['stationAccounts', ['unknown']], ['networkCapabilities', ['unknown']], ['audioCapabilities', ['unknown']], ['accessibilityCapabilities', ['unknown']], ['interests', ['unknown']], ['controlledActions', ['unknown']]] as [$field, $value]) {
    $_POST = validApplication();
    $_POST[$field] = $value;
    expectInvalid(static fn (): array => applicationFromRequest(), "{$field} must reject unknown values.");
}
$_POST = validApplication();
unset($_POST['primaryStation']);
expectInvalid(static fn (): array => applicationFromRequest(), 'Primary station must be required.');
$_POST = validApplication();
unset($_POST['deviceFormFactor']);
expectInvalid(static fn (): array => applicationFromRequest(), 'Device form factor must be required.');
$_POST = validApplication();
unset($_POST['testingComfort']);
expectInvalid(static fn (): array => applicationFromRequest(), 'Testing comfort must be required.');
$_POST = validApplication();
$_POST['otherDevices'] = str_repeat('a', 501);
expectInvalid(static fn (): array => applicationFromRequest(), 'Other-device text must be bounded.');
$_POST = validApplication();
$_POST['experience'] = str_repeat('é', 601);
expectInvalid(static fn (): array => applicationFromRequest(), 'Assignment notes must be byte-bounded UTF-8.');

$_POST = validApplication();
$message = coordinatorIntakeMessage(applicationFromRequest());
foreach (['Primary station:', 'Other familiar stations:', 'Existing station access (optional):', 'Device form factor:', 'Other Android devices:', 'Network capabilities:', 'Audio/accessory capabilities:', 'Accessibility/alternative-input capabilities:', 'Testing comfort level:', 'Controlled-action preferences:', 'Two-week availability:', 'Assignment notes:'] as $label) {
    expectIntake(str_contains($message, $label), "Coordinator intake email is missing {$label}");
}
expectIntake(str_contains($message, 'Recruitment source: Direct / project site'), 'Coordinator intake email is missing validated recruitment attribution.');
expectIntake(!str_contains($message, 'password') && !str_contains($message, 'CAPTCHA answer'), 'Coordinator message must not create secret fields.');

$maximum = validApplication();
$maximum['name'] = str_repeat('a', 100);
$maximum['email'] = str_repeat('a', 240) . '@example.test';
$maximum['country'] = str_repeat('a', 80);
$maximum['device'] = str_repeat('a', 160);
$maximum['androidVersion'] = str_repeat('a', 48);
$maximum['otherDevices'] = str_repeat('a', 500);
$maximum['experience'] = str_repeat('a', 1200);
$maximum['otherStations'] = array_keys(STATIONS);
$maximum['stationAccounts'] = array_keys(STATIONS);
$maximum['interests'] = array_keys(TESTING_INTERESTS);
$maximum['networkCapabilities'] = array_keys(NETWORK_CAPABILITIES);
$maximum['audioCapabilities'] = array_keys(AUDIO_CAPABILITIES);
$maximum['accessibilityCapabilities'] = array_keys(ACCESSIBILITY_CAPABILITIES);
$maximum['controlledActions'] = array_keys(CONTROLLED_ACTIONS);
expectIntake(strlen(http_build_query($maximum)) < MAX_REQUEST_BYTES, 'Maximum legitimate form payload exceeds request-size limit.');

$form = file_get_contents(dirname(__DIR__) . '/privacy-site/product-testing/index.html');
expectIntake(is_string($form), 'Unable to inspect the tester-interest form.');
expectIntake(FORM_ORIGIN . FALLBACK_LOCATION . '#alpha-tester-interest' === 'https://player.jamesjennison.net/product-testing/#alpha-tester-interest', 'The alpha-tester application endpoint must remain anchored to the canonical Player route.');
expectIntake(substr_count($form, 'href="{{ site.alpha_tester_application_url }}"') === 2, 'The Tester Hub application links must use the canonical Player endpoint.');
foreach (['<fieldset data-application-step="identity"', 'What is your primary 24Seven.FM station?', 'Guest testing does not require a 24Seven.FM station account.', 'Existing 24Seven.FM station access', 'name="primaryStation" required', 'name="deviceFormFactor" required', 'name="testingComfort" value="readonly" required', 'do not create an account or enter your username, password, security answer, CAPTCHA answer, or any other login information.', 'name="company"', 'name="consent" value="yes" required'] as $needle) {
    expectIntake(str_contains($form, $needle), "Form contract is missing {$needle}");
}
expectIntake(str_contains($form, 'name="recruitmentSource" value="direct" data-recruitment-source'), 'Form must retain a safe direct-recruitment fallback.');
foreach (['data-application-step="identity"', 'data-application-step="listening"', 'data-application-step="device"', 'data-application-step="coverage"', 'data-application-step="preferences"', 'data-application-step="review"'] as $step) {
    expectIntake(str_contains($form, $step), "Application wizard step is missing {$step}");
}
$projectScript = file_get_contents(dirname(__DIR__) . '/privacy-site/assets/project.js');
expectIntake(is_string($projectScript) && str_contains($projectScript, "'testers-community': 'testers_community'") && str_contains($projectScript, "betafamily: 'betafamily'"), 'Tagged recruitment sources must be explicitly allowlisted in the public form enhancement.');
expectIntake(is_string($projectScript) && str_contains($projectScript, "requestedSource === 'testers-community'") && str_contains($projectScript, 'Opt in, then register your profile'), 'Testers Community must receive the explicit opt-in-to-profile sequence.');
expectIntake(is_string($projectScript) && str_contains($projectScript, 'tester-application-progress') && str_contains($projectScript, 'firstInvalidControl'), 'Application wizard must gate each step without changing the receiver contract.');
expectIntake(str_contains($form, 'class="cf-turnstile" data-sitekey="0x4AAAAAAEPR2A0JwM5Qhrvt" data-action="alpha-tester-interest"'), 'The Alpha signup form must embed its dedicated Turnstile action.');
expectIntake(!str_contains($form, 'name="username"') && !str_contains($form, 'name="password"'), 'The form must not collect credentials.');

$source = file_get_contents(dirname(__DIR__) . '/privacy-site/alpha-tester-interest.php');
expectIntake(is_string($source), 'Unable to inspect the tester-interest handler.');
expectIntake(strpos($source, 'verifyTurnstile($turnstileToken, $turnstileConfig[\'secret\'])') < strpos($source, '$application = applicationFromRequest();'), 'Turnstile must be verified before application processing.');
expectIntake(strpos($source, 'storeAcceptedApplication($config, $application);') < strpos($source, '$message = coordinatorIntakeMessage($application);'), 'A validated application must enter the private Tester Hub before mail handoff.');
expectIntake(str_contains($source, 'The protected Tester Hub record is the acceptance boundary') && str_contains($source, 'please do not submit it again'), 'A coordinator mail-handoff failure must not make an already stored application appear to fail.');
expectIntake(is_string($projectScript) && str_contains($projectScript, "const draftKey = '24seven-player-alpha-application-draft-v1'") && str_contains($projectScript, 'We kept your entries in this browser session'), 'A failed fallback submission must restore the browser-session application draft without preserving the Turnstile response.');
expectIntake(str_contains($source, "TESTER_ONBOARDING_STORAGE_FILE = 'tester-onboarding-storage.php'"), 'The public endpoint must use the narrow onboarding storage boundary.');

expectIntake(!is_file(dirname(__DIR__) . '/workers/alpha-tester-interest/worker.mjs') && !is_file(dirname(__DIR__) . '/workers/alpha-tester-interest/wrangler.toml'), 'The unused Cloudflare Worker must not remain in the repository workflow.');

fwrite(STDOUT, "Alpha tester intake contract: valid.\n");
