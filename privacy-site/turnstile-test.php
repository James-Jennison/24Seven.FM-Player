<?php
declare(strict_types=1);

/**
 * Isolated Turnstile verification endpoint. It performs one test-only
 * confirmation delivery after a successful verification and does not record
 * application or account data.
 */

const TEST_ORIGIN = 'https://player.jamesjennison.net';
const TEST_ACTION = 'turnstile-test';
const TEST_HOSTNAME = 'player.jamesjennison.net';
const TEST_CONFIG_FILE = '.turnstile-test-config.php';
const TEST_MAIL_CONFIG_FILE = '.turnstile-test-mail-config.php';
const ALPHA_TESTER_CONFIG_FILE = '.alpha-tester-interest-config.php';

function result(int $status, string $message): never
{
    http_response_code($status);
    header('Content-Type: text/html; charset=UTF-8');
    header('Cache-Control: no-store');
    header('X-Robots-Tag: noindex, nofollow, noarchive');
    echo '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="robots" content="noindex, nofollow, noarchive"><title>Turnstile test</title></head><body><p>'
        . htmlspecialchars($message, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')
        . '</p><p><a href="/turnstile-test/">Return to the isolated Turnstile test</a></p></body></html>';
    exit;
}

function testConfirmationEmail(): array
{
    return [
        'subject' => '24Seven.FM Player Turnstile test succeeded',
        'plainText' => "The isolated Turnstile test at player.jamesjennison.net was verified successfully.\n\nNo application or account data was stored. This message confirms only the test email delivery path.\n",
    ];
}

if (getenv('TURNSTILE_TEST_LIBRARY') === '1') {
    return;
}

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST' || !hash_equals(TEST_ORIGIN, $_SERVER['HTTP_ORIGIN'] ?? '')) {
    result(403, 'This verification request is not allowed.');
}

$token = $_POST['cf-turnstile-response'] ?? '';
if (!is_string($token) || $token === '' || strlen($token) > 2048) {
    result(403, 'Turnstile verification was not completed.');
}

$config = require dirname(__DIR__) . '/' . TEST_CONFIG_FILE;
if (!is_array($config) || !isset($config['secret']) || !is_string($config['secret']) || trim($config['secret']) === '') {
    result(503, 'The isolated Turnstile test is not configured.');
}

$mailConfig = require dirname(__DIR__) . '/' . TEST_MAIL_CONFIG_FILE;
$deliveryConfig = require dirname(__DIR__) . '/' . ALPHA_TESTER_CONFIG_FILE;
if (!is_array($mailConfig)
    || !isset($mailConfig['recipient'])
    || !is_string($mailConfig['recipient'])
    || !filter_var($mailConfig['recipient'], FILTER_VALIDATE_EMAIL)
    || !is_array($deliveryConfig)
    || !isset($deliveryConfig['sender'])
    || !is_string($deliveryConfig['sender'])
    || !filter_var($deliveryConfig['sender'], FILTER_VALIDATE_EMAIL)) {
    result(503, 'The isolated Turnstile test confirmation is not configured.');
}

$payload = http_build_query([
    'secret' => $config['secret'],
    'response' => $token,
    'remoteip' => $_SERVER['REMOTE_ADDR'] ?? '',
], '', '&', PHP_QUERY_RFC3986);

$context = stream_context_create(['http' => [
    'method' => 'POST',
    'header' => "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: " . strlen($payload) . "\r\n",
    'content' => $payload,
    'timeout' => 10,
    'ignore_errors' => true,
]]);
$response = @file_get_contents('https://challenges.cloudflare.com/turnstile/v0/siteverify', false, $context);
$verification = is_string($response) ? json_decode($response, true) : null;

if (!is_array($verification)
    || ($verification['success'] ?? false) !== true
    || ($verification['action'] ?? '') !== TEST_ACTION
    || ($verification['hostname'] ?? '') !== TEST_HOSTNAME) {
    result(403, 'Turnstile verification was rejected. Refresh the test page and try again.');
}

putenv('ALPHA_TESTER_INTEREST_TEST_LIBRARY=1');
require dirname(__DIR__) . '/alpha-tester-interest.php';
putenv('ALPHA_TESTER_INTEREST_TEST_LIBRARY');

$confirmation = testConfirmationEmail();
if (!smtpDeliver(
    $mailConfig['recipient'],
    $deliveryConfig['sender'],
    $deliveryConfig['sender'],
    $confirmation['subject'],
    $confirmation['plainText'],
)) {
    result(503, 'Turnstile verification succeeded, but the test confirmation email could not be delivered.');
}

result(200, 'Turnstile verification succeeded. A test confirmation email was sent.');
