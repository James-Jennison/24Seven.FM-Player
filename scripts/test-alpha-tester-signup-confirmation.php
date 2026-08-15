#!/usr/bin/env php
<?php
declare(strict_types=1);

putenv('ALPHA_TESTER_INTEREST_TEST_LIBRARY=1');
require dirname(__DIR__) . '/privacy-site/alpha-tester-interest.php';

function expect(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

function decodedMimePart(string $message, string $contentType): string
{
    $pattern = '#Content-Type: ' . preg_quote($contentType, '#') . '; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n([A-Za-z0-9+/=\r\n]+)#';
    expect(preg_match($pattern, $message, $match) === 1, "Missing {$contentType} MIME part.");
    $decoded = base64_decode(str_replace(["\r", "\n"], '', $match[1]), true);
    expect(is_string($decoded), "Unable to decode {$contentType} MIME part.");
    return $decoded;
}

$confirmation = signupConfirmationEmail();
$again = signupConfirmationEmail();
$testersCommunityConfirmation = signupConfirmationEmail('testers_community');

expect($confirmation === $again, 'Signup confirmation template must be deterministic.');
expect($confirmation['subject'] === 'Thanks for signing up to test the 24Seven.FM Player', 'Signup confirmation subject is incorrect.');
expect(str_contains($confirmation['html'], '<strong>24Seven.FM Player</strong>'), 'HTML Player emphasis is missing.');
expect(str_contains($confirmation['html'], "<strong>there's nothing else you need to do yet</strong>"), 'HTML no-action emphasis is missing.');
expect(str_contains($confirmation['html'], '<strong>welcome email</strong>'), 'HTML welcome-email emphasis is missing.');
expect(substr_count($confirmation['html'], '<li>') === 6 && substr_count($confirmation['html'], '</li>') === 6, 'HTML must have the six-item list.');
expect(str_contains($confirmation['html'], '<p><strong>James —</strong> <strong>24Seven.FM Player Testing Team</strong></p>'), 'HTML signature formatting is incorrect.');
expect(str_contains($confirmation['plainText'], "At this stage, there's nothing else you need to do yet."), 'Plain-text no-action statement is missing.');
expect(substr_count($confirmation['plainText'], "\n- ") === 6, 'Plain-text must have the six-item list.');
expect(str_contains($confirmation['plainText'], 'Once you\'ve been added to the testing program, you\'ll receive a separate welcome email'), 'Plain text must keep signup and acceptance separate.');
expect(str_contains($confirmation['plainText'], 'James — 24Seven.FM Player Testing Team'), 'Plain-text signature is incorrect.');
expect(str_contains($confirmation['plainText'], 'alpha-testing@jamesjennison.net is not a monitored inbox. Please do not reply to this confirmation email.'), 'Plain-text non-monitored-address disclaimer is missing.');
expect(str_contains($confirmation['html'], '<strong>Please note:</strong> alpha-testing@jamesjennison.net is not a monitored inbox. Please do not reply to this confirmation email.'), 'HTML non-monitored-address disclaimer is missing.');
expect(str_contains($confirmation['html'], '—') && str_contains($confirmation['plainText'], '—'), 'Unicode em dash is missing from the template.');
expect(!str_contains($confirmation['plainText'], '<p>') && !str_contains($confirmation['plainText'], '<li>'), 'HTML markup leaked into the plain-text alternative.');
expect(!str_contains($confirmation['html'], 'https://play.google.com') && !str_contains($confirmation['plainText'], 'https://play.google.com'), 'Confirmation must not provide installation links.');
expect(!str_contains(strtolower($confirmation['plainText']), 'you are already a tester'), 'Confirmation must not imply acceptance.');
expect(!str_contains($confirmation['html'], 'Tester Queue') && !str_contains($confirmation['plainText'], 'Tester Queue'), 'Private Queue data leaked into confirmation.');
expect($testersCommunityConfirmation === signupConfirmationEmail('testers_community'), 'Testers Community confirmation template must be deterministic.');
expect(str_contains($testersCommunityConfirmation['plainText'], 'Testers Community Pack access uses the dedicated Google Group'), 'Testers Community confirmation must explain the Group-based access path.');
expect(str_contains($testersCommunityConfirmation['plainText'], 'does not itself grant Play access'), 'Testers Community confirmation must not imply automatic access.');
expect(!str_contains($testersCommunityConfirmation['plainText'], 'https://play.google.com'), 'Testers Community confirmation must not expose an installation link.');
expect(!str_contains(strtolower($testersCommunityConfirmation['plainText']), 'password:'), 'Testers Community confirmation must not expose credentials.');

$message = smtpMessage('tester@example.test', 'alpha@jamesjennison.net', 'alpha@jamesjennison.net', $confirmation['subject'], $confirmation['plainText'], $confirmation['html']);
expect(str_contains($message, 'Content-Type: multipart/alternative; boundary="=_24Seven_'), 'Confirmation must use multipart alternative delivery.');
expect(decodedMimePart($message, 'text/plain') === $confirmation['plainText'], 'MIME plain-text alternative changed during encoding.');
expect(decodedMimePart($message, 'text/html') === '<!doctype html><html><body>' . $confirmation['html'] . '</body></html>', 'MIME HTML representation changed during encoding.');
expect(str_contains(decodedMimePart($message, 'text/plain'), '—'), 'Unicode em dash did not survive plain-text MIME transport.');
expect(str_contains(decodedMimePart($message, 'text/html'), '—'), 'Unicode em dash did not survive HTML MIME transport.');

$source = file_get_contents(dirname(__DIR__) . '/privacy-site/alpha-tester-interest.php');
expect(is_string($source), 'Unable to inspect signup handler.');
$intakePosition = strpos($source, '$sent = smtpDeliver(');
$confirmationPosition = strpos($source, '$confirmation = signupConfirmationEmail($application[\'recruitment_source\']);');
expect($intakePosition !== false && $confirmationPosition !== false && $confirmationPosition > $intakePosition, 'Confirmation must occur only after coordinator intake succeeds.');
expect(substr_count($source, '$confirmation = signupConfirmationEmail($application[\'recruitment_source\']);') === 1, 'A successful signup must have one source-aware confirmation attempt.');
expect(str_contains($source, "smtpDiagnostic('signup_confirmation');"), 'Bounded non-sensitive confirmation failure handling is missing.');
expect(!str_contains($source, 'tester_task_assignments'), 'Signup confirmation must not create Tester Task assignments.');

fwrite(STDOUT, "Alpha tester signup confirmation contract: valid.\n");
