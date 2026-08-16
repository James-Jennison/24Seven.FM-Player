<?php
declare(strict_types=1);

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require dirname(__DIR__) . '/privacy-site/private-tester-queue.php';

function adminLoginAssert(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$databasePath = tempnam(sys_get_temp_dir(), 'player-admin-login-');
if ($databasePath === false) {
    throw new RuntimeException('Unable to create the administrator-login test database.');
}

try {
    $_SERVER['REMOTE_ADDR'] = '203.0.113.54';
    $config = [
        'database_path' => $databasePath,
        'admin_username' => 'network.admin',
        'admin_password_hash' => password_hash('test-only-password', PASSWORD_DEFAULT),
        'admin_totp_secret' => 'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ',
    ];
    $database = database($config);
    $key = administratorLoginClientKey($config);
    adminLoginAssert(strlen($key) === 64 && !str_contains($key, '203.0.113.54'), 'Login throttling must store only a keyed client identifier.');
    adminLoginAssert(!administratorLoginIsLimited($database, $config), 'A fresh administrator client must not be rate limited.');
    adminLoginAssert(administratorLoginNameAccepted($config, 'network.admin'), 'The configured administrator login name must be accepted.');
    adminLoginAssert(!administratorLoginNameAccepted($config, 'Network.Admin'), 'Administrator login names must be exact and case-sensitive.');
    $totpSecret = administratorTotpSecret($config);
    adminLoginAssert(administratorTotpCode($totpSecret, 1) === '287082', 'Authenticator-code generation must match the standard TOTP test vector.');
    $totpStep = administratorAcceptedTotpStep('287082', $totpSecret, 59);
    adminLoginAssert($totpStep === 1, 'The current authenticator code must be accepted.');
    adminLoginAssert(consumeAdministratorTotpStep($database, $totpStep), 'A valid authenticator code must be consumable once.');
    adminLoginAssert(!consumeAdministratorTotpStep($database, $totpStep), 'A consumed authenticator code must not be replayable.');

    for ($attempt = 0; $attempt <= ADMIN_LOGIN_FREE_FAILURES; $attempt++) {
        recordAdministratorLoginFailure($database, $config);
    }
    adminLoginAssert(administratorLoginIsLimited($database, $config), 'Repeated failed administrator sign-ins must be rate limited.');

    clearAdministratorLoginFailures($database, $config);
    adminLoginAssert(!administratorLoginIsLimited($database, $config), 'A successful administrator sign-in must clear its rate limit.');

    $_SERVER['HTTP_HOST'] = 'player.jamesjennison.net';
    $_SERVER['HTTP_ORIGIN'] = ADMIN_LOGIN_ORIGIN;
    adminLoginAssert(administratorMfaRequired(), 'The live coordinator host must require MFA.');
    adminLoginAssert(administratorLoginRequestIsSameOrigin(), 'The canonical administrator origin must be accepted.');
    $_SERVER['HTTP_ORIGIN'] = 'https://attacker.example';
    adminLoginAssert(!administratorLoginRequestIsSameOrigin(), 'A cross-origin administrator sign-in must be rejected.');

    $_SERVER['HTTP_HOST'] = STAGING_ADMIN_BYPASS_HOST;
    $_SERVER['HTTP_ORIGIN'] = STAGING_ADMIN_LOGIN_ORIGIN;
    adminLoginAssert(!administratorMfaRequired(), 'The isolated staging coordinator host must not require MFA.');
    adminLoginAssert(administratorLoginRequestIsSameOrigin(), 'The staging administrator origin must be accepted.');
    $_SERVER['HTTP_ORIGIN'] = ADMIN_LOGIN_ORIGIN;
    adminLoginAssert(!administratorLoginRequestIsSameOrigin(), 'The live administrator origin must not be accepted on staging.');
    $authenticationSource = file_get_contents(dirname(__DIR__) . '/privacy-site/private-tester-queue.php');
    adminLoginAssert(is_string($authenticationSource) && !str_contains($authenticationSource, 'if (stagingAdministratorBypassIsAuthorized($config))'), 'Staging access must still require the administrator password.');
    $_SERVER['REMOTE_ADDR'] = '203.0.113.54';
    $stagingBypass = ['staging_admin_bypass' => ['expires_at' => 10_001, 'allowed_addresses' => ['203.0.113.54']]];
    adminLoginAssert(stagingAdministratorBypassIsAuthorized($stagingBypass, 10_000), 'An exact staging host, source address, and short expiry must permit staging-only access.');
    $_SERVER['REMOTE_ADDR'] = '203.0.113.55';
    adminLoginAssert(!stagingAdministratorBypassIsAuthorized($stagingBypass, 10_000), 'A staging bypass must reject a different source address.');
    $_SERVER['REMOTE_ADDR'] = '203.0.113.54';
    $_SERVER['HTTP_HOST'] = 'player.jamesjennison.net';
    adminLoginAssert(!stagingAdministratorBypassIsAuthorized($stagingBypass, 10_000), 'A staging bypass must never authorize the live host.');
    $_SERVER['HTTP_HOST'] = STAGING_ADMIN_BYPASS_HOST;
    adminLoginAssert(!stagingAdministratorBypassIsAuthorized($stagingBypass, 10_001), 'A staging bypass must expire without a cleanup job.');
} finally {
    unset($database);
    @unlink($databasePath);
}

echo "Administrator login security contract: valid.\n";
