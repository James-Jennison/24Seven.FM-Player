<?php
declare(strict_types=1);

/**
 * Shared, private storage for a validated tester application.
 *
 * This file intentionally contains no authentication, mail, or browser
 * behavior. The public receiver supplies only its own private database path;
 * the coordinator queue has separate authentication and uses the same SQLite
 * database. Keeping this boundary narrow avoids exposing the queue's private
 * administrator configuration to the public form handler.
 */

function testerOnboardingDatabase(string $path): PDO
{
    if (!extension_loaded('pdo_sqlite')) {
        throw new RuntimeException('Tester onboarding storage is unavailable.');
    }
    $directory = dirname($path);
    if ($path === '' || !is_dir($directory) || !is_writable($directory)) {
        throw new RuntimeException('Tester onboarding storage is unavailable.');
    }

    $database = new PDO('sqlite:' . $path, null, null, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    $database->exec('PRAGMA foreign_keys = ON');
    testerOnboardingSchema($database);
    return $database;
}

function testerOnboardingSchema(PDO $database): void
{
    $database->exec(
        'CREATE TABLE IF NOT EXISTS testers (
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
            status TEXT NOT NULL CHECK(status IN (\'active\', \'inactive\')),
            imported_at TEXT NOT NULL,
            primary_station TEXT,
            other_stations_json TEXT NOT NULL DEFAULT \'[]\',
            station_accounts_json TEXT NOT NULL DEFAULT \'[]\',
            device_form_factor TEXT,
            other_devices TEXT,
            network_capabilities_json TEXT NOT NULL DEFAULT \'[]\',
            audio_capabilities_json TEXT NOT NULL DEFAULT \'[]\',
            accessibility_capabilities_json TEXT NOT NULL DEFAULT \'[]\',
            testing_comfort TEXT,
            controlled_actions_json TEXT NOT NULL DEFAULT \'[]\',
            testing_availability TEXT,
            recruitment_source TEXT NOT NULL DEFAULT \'direct\'
        );
        CREATE TABLE IF NOT EXISTS tester_onboarding (
            tester_id INTEGER PRIMARY KEY REFERENCES testers(id) ON DELETE CASCADE,
            onboarding_status TEXT NOT NULL DEFAULT \'profile_pending\' CHECK(onboarding_status IN (\'profile_pending\', \'profile_complete\', \'invited\', \'orientation_sent\', \'ready\', \'paused\')),
            coordinator_note TEXT NOT NULL DEFAULT \'\',
            orientation_email_status TEXT NOT NULL DEFAULT \'not_sent\' CHECK(orientation_email_status IN (\'not_sent\', \'accepted\', \'failed\')),
            orientation_email_attempted_at TEXT,
            orientation_email_attempts INTEGER NOT NULL DEFAULT 0,
            play_opt_in_confirmed_at TEXT,
            initial_smoke_test_confirmed_at TEXT,
            withdrawal_requested_at TEXT,
            deletion_requested_at TEXT,
            reviewed_at TEXT,
            rejected_at TEXT,
            updated_at TEXT NOT NULL
        );'
    );

    $testerColumns = array_column($database->query('PRAGMA table_info(testers)')->fetchAll(), 'name');
    foreach ([
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
    ] as $column => $definition) {
        if (!in_array($column, $testerColumns, true)) {
            $database->exec("ALTER TABLE testers ADD COLUMN {$column} {$definition}");
        }
    }

    $onboardingColumns = array_column($database->query('PRAGMA table_info(tester_onboarding)')->fetchAll(), 'name');
    foreach ([
        'play_opt_in_confirmed_at' => 'TEXT',
        'initial_smoke_test_confirmed_at' => 'TEXT',
        'withdrawal_requested_at' => 'TEXT',
        'deletion_requested_at' => 'TEXT',
        'reviewed_at' => 'TEXT',
        'rejected_at' => 'TEXT',
    ] as $column => $definition) {
        if (!in_array($column, $onboardingColumns, true)) {
            $database->exec("ALTER TABLE tester_onboarding ADD COLUMN {$column} {$definition}");
        }
    }
}

/**
 * Store the public application once, without treating it as Play access,
 * opt-in, installation, acceptance, or an assignment. Email uniqueness makes
 * a browser retry idempotent and never rewrites a coordinator-managed record.
 *
 * @return array{id: int, created: bool}
 */
function storeTesterOnboardingApplication(PDO $database, array $application): array
{
    $now = gmdate('c');
    $sourceId = 'web:' . bin2hex(random_bytes(16));
    $profileComplete = $application['testing_availability'] !== null;
    $database->beginTransaction();
    try {
        $insert = $database->prepare("INSERT INTO testers(source_message_uid, received_at, display_name, email, country, device, android_version, interests_json, experience, status, imported_at, primary_station, other_stations_json, station_accounts_json, device_form_factor, other_devices, network_capabilities_json, audio_capabilities_json, accessibility_capabilities_json, testing_comfort, controlled_actions_json, testing_availability, recruitment_source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(email) DO NOTHING");
        $insert->execute([
            $sourceId,
            $now,
            $application['name'],
            $application['email'],
            $application['country'] !== '' ? $application['country'] : null,
            $application['device'],
            $application['android_version'],
            json_encode($application['interests'], JSON_THROW_ON_ERROR),
            $application['experience'] !== '' ? $application['experience'] : null,
            $now,
            $application['primary_station'],
            json_encode($application['other_stations'], JSON_THROW_ON_ERROR),
            json_encode($application['station_accounts'], JSON_THROW_ON_ERROR),
            $application['device_form_factor'],
            $application['other_devices'] !== '' ? $application['other_devices'] : null,
            json_encode($application['network_capabilities'], JSON_THROW_ON_ERROR),
            json_encode($application['audio_capabilities'], JSON_THROW_ON_ERROR),
            json_encode($application['accessibility_capabilities'], JSON_THROW_ON_ERROR),
            $application['testing_comfort'],
            json_encode($application['controlled_actions'], JSON_THROW_ON_ERROR),
            $application['testing_availability'],
            $application['recruitment_source'],
        ]);
        $created = $insert->rowCount() === 1;

        $lookup = $database->prepare('SELECT id FROM testers WHERE email = ? COLLATE NOCASE');
        $lookup->execute([$application['email']]);
        $id = $lookup->fetchColumn();
        if ($id === false) {
            throw new RuntimeException('Tester onboarding storage failed.');
        }

        if ($created) {
            $onboarding = $database->prepare('INSERT INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempted_at, orientation_email_attempts, updated_at) VALUES (?, ?, \'\', \'not_sent\', NULL, 0, ?)');
            $onboarding->execute([(int) $id, $profileComplete ? 'profile_complete' : 'profile_pending', $now]);
        }
        $database->commit();
        return ['id' => (int) $id, 'created' => $created];
    } catch (Throwable $exception) {
        if ($database->inTransaction()) {
            $database->rollBack();
        }
        throw $exception;
    }
}
