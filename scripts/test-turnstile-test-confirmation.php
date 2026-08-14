#!/usr/bin/env php
<?php
declare(strict_types=1);

putenv('TURNSTILE_TEST_LIBRARY=1');
require dirname(__DIR__) . '/privacy-site/turnstile-test.php';

function expectTurnstileTest(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$confirmation = testConfirmationEmail();
expectTurnstileTest($confirmation['subject'] === '24Seven.FM Player Turnstile test succeeded', 'Turnstile test subject is incorrect.');
expectTurnstileTest(str_contains($confirmation['plainText'], 'player.jamesjennison.net'), 'Turnstile test confirmation must identify the tested site.');
expectTurnstileTest(str_contains($confirmation['plainText'], 'No application or account data was stored.'), 'Turnstile test confirmation must preserve its no-intake guarantee.');

$source = file_get_contents(dirname(__DIR__) . '/privacy-site/turnstile-test.php');
expectTurnstileTest(is_string($source), 'Unable to inspect the Turnstile test endpoint.');
expectTurnstileTest(str_contains($source, "require __DIR__ . '/alpha-tester-interest.php';"), 'Turnstile test must load the SMTP helper from the deployed document root.');

fwrite(STDOUT, "Turnstile test confirmation contract: valid.\n");
