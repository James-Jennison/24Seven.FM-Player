<?php
declare(strict_types=1);

/**
 * Tester self-service portal for the closed Alpha program.
 *
 * This endpoint shares the private queue database but never exposes the
 * coordinator session, roster, private notes, or another tester's data.
 * Registered testers authenticate with an expiring, single-use sign-in link.
 */

define('PRIVATE_TESTER_QUEUE_LIBRARY_ONLY', true);
require __DIR__ . '/private-tester-queue.php';

const TESTER_PORTAL_SESSION_NAME = 'player_tester_portal';
const TESTER_PORTAL_URL = 'https://player.jamesjennison.net/tester-portal.php';
const TESTER_PORTAL_ORIGIN = 'https://player.jamesjennison.net';
const TESTER_PORTAL_TURNSTILE_ACTION = 'tester-portal-link';
const TESTER_PORTAL_TURNSTILE_HOSTNAME = 'player.jamesjennison.net';
const TESTER_PORTAL_TURNSTILE_CONFIG_FILE = '.turnstile-test-config.php';
const TESTER_PORTAL_TURNSTILE_SITEKEY = '0x4AAAAAAEPR2A0JwM5Qhrvt';
const PORTAL_TOKEN_TTL_SECONDS = 1_800;
const PORTAL_REQUEST_COOLDOWN_SECONDS = 60;
const PORTAL_MAX_FEEDBACK_SUBJECT = 180;
const PORTAL_MAX_FEEDBACK_DETAILS = 8_000;

const PORTAL_PRIMARY_STATIONS = [
    'sst' => 'StreamingSoundtracks.com', '1980s' => '1980s.FM', 'afm' => 'Adagio.FM',
    'dfm' => 'Death.FM', 'efm' => 'Entranced.FM', 'multiple' => 'More than one station', 'none' => 'No primary station',
];
const PORTAL_DEVICE_TYPES = ['phone' => 'Standard phone', 'foldable' => 'Foldable / flip phone', 'tablet' => 'Android tablet', 'chromebook' => 'Chromebook with Android app support', 'other' => 'Other Android device'];
const PORTAL_TESTING_INTERESTS = ['playback' => 'Playback and media controls', 'queue_history_data' => 'Queue, History, and station data', 'accounts_favorites' => 'Accounts and Favorites', 'request_safety' => 'Song request browsing and safety', 'chat_community' => 'Chat and community features', 'network_recovery' => 'Network loss and recovery', 'audio_accessories' => 'Audio devices and accessories', 'adaptive_layouts' => 'Foldable, tablet, and adaptive layouts', 'accessibility' => 'Accessibility and alternative input', 'general' => 'General testing / anything needed'];
const PORTAL_NETWORK = ['wifi' => 'Wi-Fi', 'mobile_data' => 'Mobile/cellular data', 'network_handoff' => 'Wi-Fi/mobile handoff', 'network_disconnect' => 'Network recovery testing'];
const PORTAL_AUDIO = ['device_speaker' => 'Device speaker', 'bluetooth_headphones' => 'Bluetooth headphones or earbuds', 'bluetooth_speaker' => 'Bluetooth speaker', 'wired_headphones' => 'Wired headphones/headset', 'usb_audio' => 'USB audio device', 'android_auto' => 'Android Auto', 'hearing_aid' => 'Hearing aid / assistive audio', 'hdmi_audio' => 'HDMI / external display audio', 'external_input' => 'External keyboard or mouse/trackpad', 'none' => 'None beyond the device'];
const PORTAL_ACCESSIBILITY = ['talkback' => 'TalkBack', 'large_text' => 'Large text / enlarged display', 'voice_access' => 'Voice Access', 'switch_access' => 'Switch Access', 'accessibility_scanner' => 'Accessibility Scanner / touch-target review', 'external_keyboard' => 'External keyboard', 'mouse_trackpad' => 'Mouse / trackpad', 'general_accessibility' => 'General accessibility testing', 'none' => 'None'];
const PORTAL_TESTING_COMFORT = ['readonly' => 'Read-only and general testing', 'account' => 'Account testing', 'controlled' => 'Controlled live testing', 'any' => 'Any appropriate testing'];
const PORTAL_CONTROLLED_ACTIONS = ['song_request' => 'One authorized song request', 'chat_message' => 'One harmless authorized Chat message', 'chat_mention' => 'Two-account Chat mention testing', 'session_testing' => 'Sign-in / sign-out / session testing', 'report_block' => 'Report/block/unblock without sending a moderation email', 'account_testing' => 'General account-based testing', 'none' => 'None'];
const PORTAL_AVAILABILITY = ['under_30m' => 'Less than 30 minutes', '30_60m' => 'About 30–60 minutes', '1_2h' => 'About 1–2 hours', '2_4h' => 'About 2–4 hours', 'over_4h' => 'More than 4 hours', 'varies' => 'It varies'];
const PORTAL_STATIONS = ['sst' => 'StreamingSoundtracks.com', '1980s' => '1980s.FM', 'afm' => 'Adagio.FM', 'dfm' => 'Death.FM', 'efm' => 'Entranced.FM'];
const PORTAL_REQUIRED_PROFILE_FIELDS = [
    'display_name' => 'Name',
    'device' => 'Current device',
    'android_version' => 'Android version',
    'primary_station' => 'Primary station',
    'device_form_factor' => 'Device form factor',
    'network_capabilities' => 'Network capabilities',
    'audio_capabilities' => 'Audio/accessory capabilities',
    'accessibility_capabilities' => 'Accessibility and alternative input',
    'testing_comfort' => 'Testing comfort',
    'controlled_actions' => 'Controlled-test preferences',
    'testing_availability' => 'Typical two-week availability',
];
const PORTAL_WORKSPACE_VIEWS = ['dashboard', 'onboarding', 'profile', 'tasks', 'reports', 'activity', 'support'];

function portalStartSession(): void
{
    session_name(TESTER_PORTAL_SESSION_NAME);
    session_set_cookie_params(['httponly' => true, 'secure' => true, 'samesite' => 'Strict', 'path' => '/']);
    session_start();
}

function portalAdminPreviewTesterId(): ?int
{
    $value = $_GET['preview_tester'] ?? null;
    if ($value === null) return null;
    if (!is_string($value) || !ctype_digit($value) || (int) $value < 1) {
        throw new InvalidArgumentException('The requested tester preview is invalid.');
    }
    session_name(SESSION_NAME);
    session_set_cookie_params(['httponly' => true, 'secure' => true, 'samesite' => 'Strict', 'path' => '/']);
    session_start();
    $authenticated = ($_SESSION['authenticated'] ?? false) === true;
    session_write_close();
    if (!$authenticated) fail(403, 'Coordinator authentication is required for this preview.');
    return (int) $value;
}

function portalRedirect(string $query = ''): never
{
    header('Location: /tester-portal.php' . $query, true, 303);
    exit;
}

function portalWorkspaceView(): string
{
    $view = $_GET['view'] ?? 'dashboard';
    return is_string($view) && in_array($view, PORTAL_WORKSPACE_VIEWS, true) ? $view : 'dashboard';
}

function portalWorkspaceUrl(string $view, ?int $previewTesterId = null, array $extraParameters = []): string
{
    if (!in_array($view, PORTAL_WORKSPACE_VIEWS, true)) {
        throw new InvalidArgumentException('The requested tester workspace is invalid.');
    }
    $parameters = ['view' => $view];
    if ($previewTesterId !== null) $parameters = ['preview_tester' => (string) $previewTesterId] + $parameters;
    foreach ($extraParameters as $name => $value) {
        if (is_string($name) && is_scalar($value)) $parameters[$name] = (string) $value;
    }
    return '/tester-portal.php?' . http_build_query($parameters, '', '&', PHP_QUERY_RFC3986);
}

function portalWorkspaceNavigation(string $activeView, ?int $previewTesterId = null): string
{
    $items = [
        'dashboard' => ['Dashboard', '▦'],
        'onboarding' => ['Onboarding', '✓'],
        'profile' => ['Profile & Device', '◈'],
        'tasks' => ['My Tasks', '☷'],
        'reports' => ['Report results', '≡'],
        'activity' => ['Activity', '◷'],
        'support' => ['Support', '◌'],
    ];
    $links = '';
    foreach ($items as $view => [$label, $icon]) {
        $active = $view === $activeView ? ' active' : '';
        $links .= '<a class="rail-button' . $active . '" href="' . e(portalWorkspaceUrl($view, $previewTesterId)) . '" aria-label="' . e($label) . '"' . ($view === $activeView ? ' aria-current="page"' : '') . '><b aria-hidden="true">' . $icon . '</b><span>' . e($label) . '</span></a>';
    }
    return '<aside class="global-rail" aria-label="Tester workspace navigation"><a class="brand-mark" href="/" aria-label="24Seven.FM Player home"><img src="/assets/project/app-icon.png" alt=""></a><nav>' . $links . '</nav></aside>';
}

function portalPage(string $title, string $content, string $workspaceRail = ''): never
{
    header('Content-Type: text/html; charset=UTF-8');
    header('Cache-Control: no-store, private');
    header('Pragma: no-cache');
    header('X-Robots-Tag: noindex, nofollow, noarchive');
    header('X-Frame-Options: DENY');
    $page = '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>' . e($title) . '</title><style>'
        . ':root{color-scheme:dark;--bg:#0b0e14;--card:#151b26;--line:rgba(255,255,255,.1);--muted:#aab4c7;--text:#f7f8fc;--purple:#ad7cff;--teal:#6de5d1;--amber:#ffd27a;--red:#ff9ca6}*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 85% -15%,#5c2c873d,transparent 34%),var(--bg);color:var(--text);font:16px/1.5 Inter,Roboto,system-ui,sans-serif}.shell{max-width:1120px;margin:auto;padding:30px 20px 64px}.top{display:flex;justify-content:space-between;gap:18px;align-items:center;margin-bottom:30px}.brand{display:flex;gap:11px;align-items:center;font-weight:850}.brand-icon{width:34px;height:34px;border-radius:10px;object-fit:cover}.eyebrow{margin:0;color:#c3a6ff;font-size:11px;font-weight:850;letter-spacing:.12em;text-transform:uppercase}h1{margin:2px 0;font-size:32px;letter-spacing:-.04em}h2{margin:0;font-size:20px}h3{margin:0;font-size:15px}.muted{color:var(--muted)}.card{margin:18px 0;padding:23px;border:1px solid var(--line);border-radius:16px;background:linear-gradient(145deg,#171e2b,#111722);box-shadow:0 14px 34px #00000025}.hero{padding:32px}.hero p{max-width:650px}.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.metric,.task{padding:16px;border:1px solid var(--line);border-radius:13px;background:#0e131d}.metric b{display:block;margin-top:7px;font-size:14px}.dot{display:inline-grid;place-items:center;width:24px;height:24px;border:1px solid #66728a;border-radius:50%;color:#7d8a9f}.done .dot{border-color:var(--teal);background:#174d47;color:var(--teal)}.step{display:flex;gap:11px;align-items:start;padding:11px 0;border-bottom:1px solid var(--line)}.step:last-child{border:0}.step b{display:block}.step small{color:var(--muted)}.two{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(280px,.8fr);gap:18px}fieldset{border:0;margin:0;padding:0}label{display:block;margin:14px 0 6px;font-size:12px;font-weight:800;color:#cbd3e2}input,select,textarea{width:100%;padding:10px 11px;border:1px solid var(--line);border-radius:9px;background:#0d121b;color:var(--text);font:inherit}textarea{min-height:120px}input:focus,select:focus,textarea:focus{outline:3px solid #986bff30;border-color:var(--purple)}.choices{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;padding:4px 0}.choices label{display:flex;gap:8px;align-items:start;margin:0;padding:9px;border:1px solid var(--line);border-radius:9px;background:#0d121b;font-size:12px;font-weight:600}.choices input{width:auto;margin-top:2px}.button{display:inline-block;margin-top:18px;padding:10px 14px;border:0;border-radius:10px;background:linear-gradient(135deg,#b582ff,#8054f8);color:#fff;font:800 13px system-ui;cursor:pointer;text-decoration:none}.secondary{background:#273148}.pill{display:inline-flex;gap:5px;align-items:center;padding:5px 8px;border-radius:999px;background:#193e39;color:#9cf5e4;font-size:11px;font-weight:850}.pill.pending{background:#493b22;color:#ffdb8d}.pill.blocked{background:#472c35;color:#ffb4bc}.notice{padding:11px 13px;border-radius:10px;background:#183d37;color:#bffcf1}.error{background:#4a2730;color:#ffdce1}.admin-preview{border:1px solid #b685ff66;background:#332551;color:#eadfff}.task p{margin:6px 0;color:var(--muted);font-size:13px}.task ul{padding-left:19px;color:#d6deed;font-size:13px}.right{float:right}.login{max-width:530px;margin:9vh auto}.login .card{padding:30px}.small{font-size:12px}@media(max-width:760px){.grid,.two{grid-template-columns:1fr}.choices{grid-template-columns:1fr}.shell{padding:20px 14px}.hero{padding:22px}.top{align-items:start}h1{font-size:27px}}</style></head><body>__PORTAL_SHELL_START__' . $content . '__PORTAL_SHELL_END__</body></html>';
    $portalStyleAsset = __DIR__ . '/assets/onboarding-portal.css';
    $portalStyleVersion = is_file($portalStyleAsset) ? substr((string) hash_file('sha256', $portalStyleAsset), 0, 12) : 'portal';
    $checklistStyleAsset = __DIR__ . '/assets/tester-portal.css';
    $checklistStyleVersion = is_file($checklistStyleAsset) ? substr((string) hash_file('sha256', $checklistStyleAsset), 0, 12) : 'checklist-style';
    $wizardStyleAsset = __DIR__ . '/assets/onboarding-wizard.css';
    $wizardStyleVersion = is_file($wizardStyleAsset) ? substr((string) hash_file('sha256', $wizardStyleAsset), 0, 12) : 'wizard-style';
    $shellStart = '<a class="skip-link" href="#tester-portal">Skip to tester portal</a>';
    $shellEnd = '';
    if ($workspaceRail === '') {
        $shellStart .= '<main id="tester-portal" class="portal-preview-shell">';
        $shellEnd = '</main>';
    } else {
        $shellStart .= '<div class="app-shell">' . $workspaceRail . '<main id="tester-portal" class="desktop tester-desktop">';
        $shellEnd = '</main></div>';
    }
    $page = str_replace('</style></head>', '</style><style>' . activityTimelineResilientStyle() . '</style><link rel="stylesheet" href="/assets/onboarding-portal.css?v=' . e($portalStyleVersion) . '"><link rel="stylesheet" href="/assets/tester-portal.css?v=' . e($checklistStyleVersion) . '"><link rel="stylesheet" href="/assets/onboarding-wizard.css?v=' . e($wizardStyleVersion) . '"></head>', $page);
    $page = str_replace('__PORTAL_SHELL_START__', $shellStart, $page);
    $page = str_replace('__PORTAL_SHELL_END__', '<script>(function(){const form=document.querySelector("form input[name=action][value=save_profile]")?.form;if(!form)return;const required=["display_name","device","android_version","primary_station","device_form_factor","network_capabilities","audio_capabilities","accessibility_capabilities","testing_comfort","controlled_actions","testing_availability"];const missing=new URLSearchParams(location.search).get("profile_missing")?.split(",")||[];for(const name of required){const group=form.querySelector(`[name="${name}[]"]`)?.closest(".choices");const control=group||form.querySelector(`[name="${name}"]`);if(!control)continue;const label=group?group.previousElementSibling:control.previousElementSibling;const id=`profile-${name}`;control.id=id;if(label?.tagName==="LABEL"){label.id=`${id}-label`;if(!label.querySelector(".required-mark")){label.insertAdjacentHTML("beforeend",` <span class="required-mark" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span>`);}if(group){group.setAttribute("role","group");group.setAttribute("aria-labelledby",label.id);group.setAttribute("aria-required","true");}}if(missing.includes(name)){control.classList.add("profile-field-missing");}}const first=missing.map(name=>document.getElementById(`profile-${name}`)).find(Boolean);if(first){first.scrollIntoView({block:"center"});first.focus({preventScroll:true});}})();</script><script src="/assets/onboarding-profile-form.js" defer></script>' . $shellEnd, $page);
    $checklistAsset = __DIR__ . '/assets/tester-portal.js';
    $checklistVersion = is_file($checklistAsset) ? substr((string) hash_file('sha256', $checklistAsset), 0, 12) : 'checklist';
    $wizardAsset = __DIR__ . '/assets/onboarding-wizard.js';
    $wizardVersion = is_file($wizardAsset) ? substr((string) hash_file('sha256', $wizardAsset), 0, 12) : 'wizard';
    $activityAsset = __DIR__ . '/assets/activity-timeline.js';
    $activityVersion = is_file($activityAsset) ? substr((string) hash_file('sha256', $activityAsset), 0, 12) : 'activity';
    $page = str_replace('</body></html>', '<script src="/assets/tester-portal.js?v=' . e($checklistVersion) . '" defer></script><script src="/assets/onboarding-wizard.js?v=' . e($wizardVersion) . '" defer></script><script src="/assets/activity-timeline.js?v=' . e($activityVersion) . '" defer></script></body></html>', $page);
    echo $page;
    exit;
}

function portalNotice(string $name): string
{
    $value = $_GET[$name] ?? '';
    $notice = is_string($value) && $value !== '' ? '<p class="notice' . ($name === 'error' ? ' error' : '') . '">' . e($value) . '</p>' : '';
    return $notice . ($name === 'error' ? portalProfileMissingNotice() : '');
}

function portalText(string $name, int $maximum, bool $required = false): string
{
    $value = trim((string) ($_POST[$name] ?? ''));
    if (($required && $value === '') || strlen($value) > $maximum || str_contains($value, "\0") || !preg_match('//u', $value)) {
        throw new InvalidArgumentException('Please check the highlighted profile information.');
    }
    return $value;
}

function portalChoice(string $name, array $options, bool $required = false): ?string
{
    $value = portalText($name, 80, $required);
    if ($value === '') return null;
    if (!array_key_exists($value, $options)) throw new InvalidArgumentException('Please choose a valid profile option.');
    return $value;
}

function portalChoices(string $name, array $options, bool $required = false, bool $noneExclusive = false): array
{
    $values = $_POST[$name] ?? [];
    if (!is_array($values) || ($required && $values === [])) throw new InvalidArgumentException('Please complete the required profile options.');
    $result = [];
    foreach ($values as $value) {
        if (!is_string($value) || !array_key_exists($value, $options)) throw new InvalidArgumentException('Please choose valid profile options.');
        $result[$value] = true;
    }
    return $noneExclusive && isset($result['none']) ? ['none'] : array_keys($result);
}

function portalRequiredProfileFields(): array
{
    $missing = [];
    foreach (PORTAL_REQUIRED_PROFILE_FIELDS as $field => $_label) {
        $value = $_POST[$field] ?? null;
        $provided = str_ends_with($field, 'capabilities') || $field === 'controlled_actions'
            ? is_array($value) && $value !== []
            : is_string($value) && trim($value) !== '';
        if (!$provided) $missing[] = $field;
    }
    return $missing;
}

function portalProfileMissingNotice(): string
{
    $raw = $_GET['profile_missing'] ?? '';
    if (!is_string($raw) || $raw === '') return '';
    $fields = array_values(array_filter(explode(',', $raw), static fn (string $field): bool => array_key_exists($field, PORTAL_REQUIRED_PROFILE_FIELDS)));
    if ($fields === []) return '';
    $links = array_map(static fn (string $field): string => '<a href="#profile-' . e($field) . '">' . e(PORTAL_REQUIRED_PROFILE_FIELDS[$field]) . '</a>', $fields);
    return '<div class="notice error" role="alert"><strong>Profile not saved.</strong> Complete the required fields marked with *: ' . implode(', ', $links) . '.</div>';
}

function portalTokenHash(string $token): string { return hash('sha256', $token); }

function portalTurnstileAccepted(array $verification): bool
{
    return ($verification['success'] ?? false) === true
        && ($verification['action'] ?? '') === TESTER_PORTAL_TURNSTILE_ACTION
        && ($verification['hostname'] ?? '') === TESTER_PORTAL_TURNSTILE_HOSTNAME;
}

function portalVerifyTurnstile(string $token, string $secret): bool
{
    if ($token === '' || strlen($token) > 2048 || trim($secret) === '') return false;
    $payload = http_build_query([
        'secret' => $secret,
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
    return is_array($verification) && portalTurnstileAccepted($verification);
}

function portalTester(PDO $database): array
{
    $id = $_SESSION['tester_portal_id'] ?? null;
    if (!is_int($id) && !ctype_digit((string) $id)) portalRedirect();
    try {
        return portalTesterById($database, (int) $id);
    } catch (InvalidArgumentException) {
        $_SESSION = [];
        session_destroy();
        portalRedirect('?error=' . rawurlencode('This tester access is no longer active.'));
    }
}

function portalTesterById(PDO $database, int $id): array
{
    $query = $database->prepare("SELECT testers.*, onboarding.play_opt_in_confirmed_at, onboarding.initial_smoke_test_confirmed_at, onboarding.withdrawal_requested_at, onboarding.deletion_requested_at, onboarding.onboarding_status FROM testers LEFT JOIN tester_onboarding AS onboarding ON onboarding.tester_id = testers.id WHERE testers.id = ? AND testers.status = 'active'");
    $query->execute([$id]);
    $tester = $query->fetch();
    if ($tester === false) {
        throw new InvalidArgumentException('This tester access is no longer active.');
    }
    return $tester;
}

function portalCheckboxes(string $name, array $options, array $selected): string
{
    $html = '<div class="choices">';
    foreach ($options as $key => $label) {
        $html .= '<label><input type="checkbox" name="' . e($name) . '[]" value="' . e($key) . '"' . (in_array($key, $selected, true) ? ' checked' : '') . '> ' . e($label) . '</label>';
    }
    return $html . '</div>';
}

function portalSelect(string $name, array $options, ?string $selected, string $placeholder): string
{
    $html = '<select name="' . e($name) . '" required><option value="">' . e($placeholder) . '</option>';
    foreach ($options as $key => $label) $html .= '<option value="' . e($key) . '"' . ($key === $selected ? ' selected' : '') . '>' . e($label) . '</option>';
    return $html . '</select>';
}

function portalAssignments(PDO $database, int $testerId): array
{
    $query = $database->prepare('SELECT assignments.id, assignments.task_id, assignments.task_status, assignments.station_scope, assignments.configuration_scope, assignments.coordinator_note, assignments.submitted_for_review_at, assignments.created_at, assignments.updated_at, handoffs.pt_case AS retest_pt_case, handoffs.source_assignment_id AS retest_source_assignment_id, handoffs.tester_instruction AS retest_instruction FROM tester_task_assignments AS assignments LEFT JOIN tester_retest_handoffs AS handoffs ON handoffs.retest_assignment_id = assignments.id WHERE assignments.tester_id = ? ORDER BY assignments.created_at DESC, assignments.id DESC');
    $query->execute([$testerId]);
    return $query->fetchAll();
}

function portalHumanTimestamp(?string $value): string
{
    return humanTimestamp($value);
}

function portalChatPayload(PDO $database, array $tester, string $coordinatorName, int $afterId = 0): array
{
    $messages = chatMessages($database, (int) $tester['id'], 'tester', $afterId);
    return array_map(static fn (array $message): array => [
        'id' => (int) $message['id'],
        'sender' => $message['sender_role'] === 'tester' ? 'You' : $coordinatorName,
        'role' => $message['sender_role'],
        'body' => (string) $message['body'],
        'createdAt' => portalHumanTimestamp((string) $message['created_at']),
    ], $messages);
}

function portalChatPoll(PDO $database, array $tester, string $coordinatorName, int $afterId, bool $stream = false): never
{
    session_write_close();
    if ($stream) {
        header('Content-Type: text/event-stream; charset=UTF-8');
        header('Cache-Control: no-store, private');
        header('X-Accel-Buffering: no');
        for ($attempt = 0; $attempt < 20 && !connection_aborted(); $attempt++) {
            $messages = portalChatPayload($database, $tester, $coordinatorName, $afterId);
            if ($messages !== []) {
                $afterId = (int) end($messages)['id'];
                echo "event: messages\ndata: " . json_encode($messages, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES) . "\n\n";
            } else {
                echo ": keepalive\n\n";
            }
            @ob_flush();
            flush();
            sleep(1);
        }
        exit;
    }
    for ($attempt = 0; $attempt < 12; $attempt++) {
        $messages = portalChatPayload($database, $tester, $coordinatorName, $afterId);
        if ($messages !== []) break;
        sleep(1);
    }
    header('Content-Type: application/json; charset=UTF-8');
    header('Cache-Control: no-store, private');
    echo json_encode(['messages' => $messages ?? []], JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES);
    exit;
}

function portalChatPanel(PDO $database, array $tester, string $coordinatorName): string
{
    $messages = portalChatPayload($database, $tester, $coordinatorName);
    $items = '';
    foreach ($messages as $message) {
        $items .= '<article class="chat-message ' . ($message['role'] === 'tester' ? 'mine' : 'coordinator') . '" data-chat-message-id="' . (int) $message['id'] . '"><strong>' . e($message['sender']) . '</strong><div>' . nl2br(e($message['body'])) . '</div><small>' . e($message['createdAt']) . '</small><form method="post"><input type="hidden" name="action" value="delete_chat_message"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="message_id" value="' . (int) $message['id'] . '"><button class="button secondary" type="submit">Delete from my view</button></form></article>';
    }
    if ($items === '') $items = '<p class="muted" data-chat-empty>No messages yet. Use Live Chat for testing-program support; do not send credentials or account secrets.</p>';
    $items = '<style>.live-chat .card-title{display:flex;justify-content:space-between;gap:1rem;align-items:start}.chat-messages{display:flex;min-height:14rem;max-height:30rem;overflow:auto;flex-direction:column;gap:.75rem;margin:1rem 0;padding:.2rem}.chat-message{max-width:82%;padding:.8rem .9rem;border:1px solid var(--line);border-radius:13px;background:#0e131d}.chat-message.mine{align-self:end;border-color:#3e988a;background:#173d38}.chat-message strong,.chat-message small{display:block}.chat-message small{margin-top:.4rem}.chat-message form{margin:0}.chat-message .button{margin-top:.55rem;padding:.45rem .65rem;font-size:11px}.chat-composer{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:.7rem;align-items:end}.chat-composer textarea{min-height:3.7rem;margin:0}.chat-composer .button{margin:0}@media(max-width:560px){.live-chat .card-title,.chat-composer{display:block}.chat-composer .button{margin-top:.7rem}.chat-message{max-width:94%}}</style>' . $items;
    return '<section class="card live-chat" data-live-chat data-chat-poll="/tester-portal.php?chat_poll=1" data-chat-stream="/tester-portal.php?chat_stream=1"><div class="card-title"><div><p class="eyebrow">Private support</p><h2>Live Chat with ' . e($coordinatorName) . '</h2><p class="muted small">Only you and ' . e($coordinatorName) . ' can view this tester-program conversation.</p></div><button class="button secondary" type="button" data-chat-detach>Detach ↗</button></div><div class="chat-messages" data-chat-messages aria-live="polite">' . $items . '</div><form method="post" class="chat-composer" data-chat-form><input type="hidden" name="action" value="send_chat"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label class="visually-hidden" for="chat_body">Write a private chat message</label><textarea id="chat_body" name="chat_body" maxlength="' . CHAT_MAX_MESSAGE_LENGTH . '" required placeholder="Write a private message…"></textarea><button type="submit">Send</button></form></section><script src="/assets/onboarding-live-chat.js" defer></script>';
}

function portalRenderChatPopout(PDO $database, array $tester, string $coordinatorName): never
{
    portalPage('Live Chat — ' . $coordinatorName, '<div class="top"><div class="brand"><img class="brand-icon" src="/assets/project/app-icon.png" alt=""><span>24Seven.FM Player<br><small class="muted">Tester Live Chat with ' . e($coordinatorName) . '</small></span></div></div>' . portalChatPanel($database, $tester, $coordinatorName));
}

function portalRequestLink(PDO $database, array $config): void
{
    $email = strtolower(portalText('email', 254, true));
    $generic = 'If that address belongs to an active tester, a sign-in link is on its way.';
    $turnstileConfig = require dirname(__DIR__) . '/' . TESTER_PORTAL_TURNSTILE_CONFIG_FILE;
    $turnstileToken = $_POST['cf-turnstile-response'] ?? '';
    if (!is_array($turnstileConfig)
        || !isset($turnstileConfig['secret'])
        || !is_string($turnstileConfig['secret'])
        || !is_string($turnstileToken)
        || !portalVerifyTurnstile($turnstileToken, $turnstileConfig['secret'])) {
        throw new InvalidArgumentException('Please complete the verification and try again.');
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) portalRedirect('?notice=' . rawurlencode($generic));
    $query = $database->prepare("SELECT id, display_name, email FROM testers WHERE email = ? AND status = 'active'");
    $query->execute([$email]);
    $tester = $query->fetch();
    if ($tester === false) portalRedirect('?notice=' . rawurlencode($generic));
    $recent = $database->prepare('SELECT requested_at FROM tester_portal_tokens WHERE tester_id = ? ORDER BY id DESC LIMIT 1');
    $recent->execute([(int) $tester['id']]);
    $last = $recent->fetchColumn();
    if (is_string($last) && strtotime($last) + PORTAL_REQUEST_COOLDOWN_SECONDS > time()) portalRedirect('?notice=' . rawurlencode($generic));
    $raw = bin2hex(random_bytes(32));
    $now = gmdate('c');
    $expires = gmdate('c', time() + PORTAL_TOKEN_TTL_SECONDS);
    $database->prepare('DELETE FROM tester_portal_tokens WHERE expires_at < ? OR consumed_at IS NOT NULL')->execute([$now]);
    $database->prepare('INSERT INTO tester_portal_tokens(tester_id, token_hash, requested_at, expires_at) VALUES (?, ?, ?, ?)')->execute([(int) $tester['id'], portalTokenHash($raw), $now, $expires]);
    $tokenId = (int) $database->lastInsertId();
    $link = TESTER_PORTAL_URL . '?token=' . rawurlencode($raw);
    $plain = "Hi {$tester['display_name']},\n\nUse this one-time link to open your 24Seven.FM Player tester portal:\n{$link}\n\nIt expires in 30 minutes. If you did not request it, you can ignore this message.\n\n24Seven.FM Player Testing Team";
    $html = '<p>Hi ' . e($tester['display_name']) . ',</p><p>Use this one-time link to open your <strong>24Seven.FM Player</strong> tester portal:</p><p><a href="' . e($link) . '">Open my tester portal</a></p><p>This link expires in 30 minutes. If you did not request it, you can ignore this message.</p><p>24Seven.FM Player Testing Team</p>';
    if (!sendIndividualMail($config, $tester['email'], 'Your 24Seven.FM Player tester portal sign-in link', $plain, $html)) {
        $database->prepare('DELETE FROM tester_portal_tokens WHERE id = ?')->execute([$tokenId]);
        throw new InvalidArgumentException('We could not send a sign-in link. Please try again later.');
    }
    portalRedirect('?notice=' . rawurlencode($generic));
}

function portalPendingToken(PDO $database, string $token): array
{
    if (!preg_match('/^[a-f0-9]{64}$/', $token)) portalRedirect('?error=' . rawurlencode('That sign-in link is invalid or has expired.'));
    $query = $database->prepare("SELECT id, tester_id FROM tester_portal_tokens WHERE token_hash = ? AND expires_at >= ? AND consumed_at IS NULL");
    $query->execute([portalTokenHash($token), gmdate('c')]);
    $record = $query->fetch();
    if ($record === false) portalRedirect('?error=' . rawurlencode('That sign-in link is invalid or has expired.'));
    return $record;
}

function portalConsumeLink(PDO $database, string $token): void
{
    $record = portalPendingToken($database, $token);
    $database->beginTransaction();
    $consume = $database->prepare('UPDATE tester_portal_tokens SET consumed_at = ? WHERE id = ? AND consumed_at IS NULL');
    $consume->execute([gmdate('c'), (int) $record['id']]);
    if ($consume->rowCount() !== 1) {
        $database->rollBack();
        portalRedirect('?error=' . rawurlencode('That sign-in link is invalid or has expired.'));
    }
    $database->prepare('DELETE FROM tester_portal_tokens WHERE tester_id = ? AND id <> ?')->execute([(int) $record['tester_id'], (int) $record['id']]);
    $database->commit();
    session_regenerate_id(true);
    $_SESSION['tester_portal_id'] = (int) $record['tester_id'];
    csrf();
    portalRedirect('?notice=' . rawurlencode('You are signed in to your tester portal.'));
}

function portalSaveProfile(PDO $database, array $tester, array $config): void
{
    $wasComplete = profileSummary($tester)['complete'];
    $missing = portalRequiredProfileFields();
    if ($missing !== []) {
        portalRedirect('?error=' . rawurlencode('Please complete the required profile fields.') . '&profile_missing=' . rawurlencode(implode(',', $missing)) . '#intake-profile');
    }
    $displayName = portalText('display_name', 100, true);
    $country = portalText('country', 80);
    $primary = portalChoice('primary_station', PORTAL_PRIMARY_STATIONS, true);
    $otherStations = portalChoices('other_stations', PORTAL_STATIONS);
    if (array_key_exists((string) $primary, PORTAL_STATIONS)) {
        $otherStations = array_values(array_diff($otherStations, [(string) $primary]));
    }
    $accounts = portalChoices('station_accounts', PORTAL_STATIONS + ['none' => 'None']);
    $deviceType = portalChoice('device_form_factor', PORTAL_DEVICE_TYPES, true);
    $network = portalChoices('network_capabilities', PORTAL_NETWORK, true);
    $audio = portalChoices('audio_capabilities', PORTAL_AUDIO, true, true);
    $accessibility = portalChoices('accessibility_capabilities', PORTAL_ACCESSIBILITY, true, true);
    $comfort = portalChoice('testing_comfort', PORTAL_TESTING_COMFORT, true);
    $controlled = portalChoices('controlled_actions', PORTAL_CONTROLLED_ACTIONS, true, true);
    $availability = portalChoice('testing_availability', PORTAL_AVAILABILITY, true);
    $interests = portalChoices('testing_interests', PORTAL_TESTING_INTERESTS);
    $experience = portalText('experience', 1200);
    $update = $database->prepare('UPDATE testers SET display_name = ?, country = ?, device = ?, android_version = ?, primary_station = ?, other_stations_json = ?, station_accounts_json = ?, device_form_factor = ?, other_devices = ?, interests_json = ?, network_capabilities_json = ?, audio_capabilities_json = ?, accessibility_capabilities_json = ?, testing_comfort = ?, controlled_actions_json = ?, testing_availability = ?, experience = ? WHERE id = ?');
    $update->execute([$displayName, $country, portalText('device', 160, true), portalText('android_version', 48, true), $primary, json_encode($otherStations, JSON_THROW_ON_ERROR), json_encode($accounts, JSON_THROW_ON_ERROR), $deviceType, portalText('other_devices', 500), json_encode($interests, JSON_THROW_ON_ERROR), json_encode($network, JSON_THROW_ON_ERROR), json_encode($audio, JSON_THROW_ON_ERROR), json_encode($accessibility, JSON_THROW_ON_ERROR), $comfort, json_encode($controlled, JSON_THROW_ON_ERROR), $availability, $experience, (int) $tester['id']]);
    $onboardingStatus = synchronizeOnboardingProfile($database, (int) $tester['id']);
    if (!$wasComplete && $onboardingStatus !== 'profile_pending') {
        $updatedTester = portalTesterById($database, (int) $tester['id']);
        sendProfileCompletionNotification($database, $config, $updatedTester);
    }
    portalRedirect('?notice=' . rawurlencode('Your Profile & Device details are complete. Next, install the Player through Google Play and confirm your opt-in.'));
}

function portalConfirmOptIn(PDO $database, array $tester): void
{
    if (($_POST['confirm_opt_in'] ?? '') !== 'yes') throw new InvalidArgumentException('Confirm your completed opt-in before continuing.');
    $ready = recordTesterPlayOptIn($database, (int) $tester['id']);
    portalRedirect('?notice=' . rawurlencode($ready ? 'Your Google Play opt-in confirmation is recorded. You are ready for a focused assignment.' : 'Your Google Play opt-in confirmation is recorded. Complete the short first-use smoke test next.'));
}

function portalConfirmInitialSmokeTest(PDO $database, array $tester): void
{
    if (($_POST['confirm_initial_smoke_test'] ?? '') !== 'yes') throw new InvalidArgumentException('Confirm the completed initial smoke test before continuing.');
    recordTesterInitialSmokeTest($database, (int) $tester['id']);
    portalRedirect('?notice=' . rawurlencode('Your initial smoke-test confirmation is recorded. You are ready for a focused assignment.'));
}

function portalRequestPrivacyAction(PDO $database, array $tester): void
{
    $request = (string) ($_POST['privacy_request'] ?? '');
    $column = match ($request) {
        'withdrawal' => 'withdrawal_requested_at',
        'deletion' => 'deletion_requested_at',
        default => throw new InvalidArgumentException('Choose a valid privacy request.'),
    };
    if (($_POST['confirm_privacy_request'] ?? '') !== 'yes') {
        throw new InvalidArgumentException('Confirm the privacy request before continuing.');
    }
    $now = gmdate('c');
    $database->prepare("INSERT INTO tester_onboarding(tester_id, onboarding_status, coordinator_note, orientation_email_status, orientation_email_attempts, updated_at, {$column}) VALUES (?, 'profile_pending', '', 'not_sent', 0, ?, ?) ON CONFLICT(tester_id) DO UPDATE SET {$column} = excluded.{$column}, updated_at = excluded.updated_at")
        ->execute([(int) $tester['id'], $now, $now]);
    $message = $request === 'withdrawal'
        ? 'Your withdrawal request is recorded. The coordinator will end tester access and follow up through the registered contact route.'
        : 'Your deletion request is recorded. The coordinator will verify and process it under the tester-data retention policy.';
    portalRedirect('?notice=' . rawurlencode($message));
}

function portalSubmitFeedback(PDO $database, array $tester): void
{
    $assignmentId = filter_var($_POST['assignment_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($assignmentId === false) throw new InvalidArgumentException('Choose one of your assigned tasks.');
    $assignment = $database->prepare('SELECT id, task_id, task_status, submitted_for_review_at FROM tester_task_assignments WHERE id = ? AND tester_id = ?');
    $assignment->execute([$assignmentId, (int) $tester['id']]);
    $assignment = $assignment->fetch();
    if ($assignment === false || $assignment['task_status'] === 'complete') throw new InvalidArgumentException('That task is no longer available for a tester report.');
    $latestReview = latestAssignmentReviewEvent($database, $assignmentId);
    if (($latestReview['decision'] ?? '') === 'returned') throw new InvalidArgumentException('Submit the requested clarification before adding another PT-case report.');
    if (($assignment['submitted_for_review_at'] ?? '') !== '') throw new InvalidArgumentException('This task is already with the Coordinator for review.');
    $task = taskRegistry()[$assignment['task_id']] ?? null;
    if (!is_array($task)) throw new InvalidArgumentException('That assigned task is no longer available.');
    $ptCase = portalText('pt_case', 20, true);
    if (!in_array($ptCase, assignmentRequiredPtCases($database, $assignmentId, $task), true)) throw new InvalidArgumentException('Choose one of the PT cases assigned with this task.');
    $outcome = (string) ($_POST['outcome'] ?? '');
    if (!in_array($outcome, ['pass', 'issue', 'blocked', 'note'], true)) throw new InvalidArgumentException('Choose a valid report outcome.');
    $category = (string) ($_POST['category'] ?? '');
    if (!array_key_exists($category, FEEDBACK_CATEGORIES)) throw new InvalidArgumentException('Choose a valid feedback category.');
    try {
        $database->prepare('INSERT INTO tester_feedback(tester_id, assignment_id, subject, details, outcome, category, pt_case, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')->execute([(int) $tester['id'], $assignmentId, portalText('subject', PORTAL_MAX_FEEDBACK_SUBJECT, true), portalText('details', PORTAL_MAX_FEEDBACK_DETAILS, true), $outcome, $category, $ptCase, gmdate('c')]);
    } catch (PDOException $error) {
        if (str_contains($error->getMessage(), 'tester_feedback.assignment_id, tester_feedback.pt_case')) {
            throw new InvalidArgumentException('A result for this PT case is already recorded. Ask the Coordinator to return the task for correction if needed.');
        }
        throw $error;
    }
    $database->prepare("UPDATE tester_task_assignments SET task_status = CASE WHEN task_status = 'assigned' THEN 'in_progress' ELSE task_status END, updated_at = ? WHERE id = ?")
        ->execute([gmdate('c'), $assignmentId]);
    portalRedirect('?notice=' . rawurlencode('Your PT-case result has been saved. Submit the task for Coordinator review after every assigned case is reported.'));
}

function portalSubmitTaskForReview(PDO $database, array $tester): void
{
    $assignmentId = filter_var($_POST['assignment_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($assignmentId === false) throw new InvalidArgumentException('Choose a valid assigned task.');
    $statement = $database->prepare('SELECT id, task_id, task_status, submitted_for_review_at FROM tester_task_assignments WHERE id = ? AND tester_id = ?');
    $statement->execute([$assignmentId, (int) $tester['id']]);
    $assignment = $statement->fetch();
    if ($assignment === false || $assignment['task_status'] === 'complete') throw new InvalidArgumentException('That task is no longer available for review.');
    $latestReview = latestAssignmentReviewEvent($database, $assignmentId);
    if (($latestReview['decision'] ?? '') === 'returned') throw new InvalidArgumentException('Submit the requested clarification before returning this task to Coordinator review.');
    if (($assignment['submitted_for_review_at'] ?? '') !== '') throw new InvalidArgumentException('This task is already with the Coordinator for review.');
    $task = taskRegistry()[$assignment['task_id']] ?? null;
    if (!is_array($task)) throw new InvalidArgumentException('That assigned task is no longer available.');
    $progress = assignmentReportProgress($database, (int) $assignment['id'], $task);
    if (!$progress['complete']) {
        throw new InvalidArgumentException('Report every assigned PT case before submitting this task for Coordinator review. Missing: ' . implode(', ', $progress['missing']) . '.');
    }
    $database->prepare("UPDATE tester_task_assignments SET task_status = CASE WHEN task_status = 'assigned' THEN 'in_progress' ELSE task_status END, submitted_for_review_at = ?, updated_at = ? WHERE id = ?")
        ->execute([gmdate('c'), gmdate('c'), $assignmentId]);
    portalRedirect('?notice=' . rawurlencode('Your task is submitted for Coordinator review. The Coordinator records the final completion or blocked status.'));
}

function portalSubmitReviewClarification(PDO $database, array $tester): void
{
    $assignmentId = filter_var($_POST['assignment_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($assignmentId === false) throw new InvalidArgumentException('Choose a valid returned task.');
    $assignmentQuery = $database->prepare('SELECT id, task_status FROM tester_task_assignments WHERE id = ? AND tester_id = ?');
    $assignmentQuery->execute([$assignmentId, (int) $tester['id']]);
    $assignment = $assignmentQuery->fetch();
    if ($assignment === false || in_array((string) $assignment['task_status'], ['complete', 'blocked'], true)) throw new InvalidArgumentException('That task is no longer available for clarification.');
    $review = latestAssignmentReviewEvent($database, $assignmentId);
    if ($review === null || ($review['decision'] ?? '') !== 'returned') throw new InvalidArgumentException('This task has not been returned for clarification.');
    $response = portalText('clarification_response', PORTAL_MAX_FEEDBACK_DETAILS, true);
    $now = gmdate('c');
    $database->beginTransaction();
    try {
        $database->prepare('INSERT INTO tester_task_review_responses(assignment_id, tester_id, review_event_id, response, created_at) VALUES (?, ?, ?, ?, ?)')
            ->execute([$assignmentId, (int) $tester['id'], (int) $review['id'], $response, $now]);
        $database->prepare("UPDATE tester_task_assignments SET task_status = 'in_progress', submitted_for_review_at = ?, updated_at = ? WHERE id = ?")
            ->execute([$now, $now, $assignmentId]);
        $database->commit();
    } catch (Throwable $error) {
        if ($database->inTransaction()) $database->rollBack();
        throw $error;
    }
    portalRedirect('?notice=' . rawurlencode('Your clarification was submitted for Coordinator review.'));
}

function portalAssignmentSteps(PDO $database, array $task, array $assignment): array
{
    $scope = trim((string) ($assignment['station_scope'] ?? '')) ?: (string) $task['stationScope'];
    $configuration = trim((string) ($assignment['configuration_scope'] ?? '')) ?: 'your registered device configuration';
    $taskSteps = $task['testerSteps'] ?? [];
    if (!is_array($taskSteps) || $taskSteps === []) {
        $taskSteps = ['Open ' . implode(', ', assignmentRequiredPtCases($database, (int) $assignment['id'], $task)) . ' and complete each listed check in order. ' . $task['purpose']];
    }
    $retest = trim((string) ($assignment['retest_pt_case'] ?? ''));
    return [
        'Set up the assigned Player build for ' . $scope . ' on ' . $configuration . '.',
        ...($retest === '' ? [] : ['This is a separate re-test for ' . $retest . ' only. ' . trim((string) ($assignment['retest_instruction'] ?? '')) . ' Do not re-test or report other PT cases with this assignment.']),
        ...array_values(array_filter($taskSteps, 'is_string')),
        'Expected result: ' . (is_string($task['expectedResult'] ?? null) ? $task['expectedResult'] : 'the behavior remains usable and matches the detailed PT-case acceptance criteria.'),
        'Stop at this boundary: ' . $task['safetyWarning'],
        'Report each PT case separately: open Report a result, choose this assignment, select Pass, Issue, Blocked, or Usability note, then state the device/setup, steps taken, expected result, and observed result.',
    ];
}

function portalAssignedChecklistMarkup(array $task, array $requiredPtCases): string
{
    static $caseMarkup = null;
    if ($caseMarkup === null) {
        $caseMarkup = [];
        $sourcePath = __DIR__ . '/dev/tester-workspace/index.html';
        $source = is_file($sourcePath) ? (string) file_get_contents($sourcePath) : '';
        if ($source !== '' && preg_match_all('~<article\\b[^>]*\\bid="(pt-[0-9]+)"[^>]*>(.*?)</article>~is', $source, $matches, PREG_SET_ORDER)) {
            foreach ($matches as $match) {
                $caseMarkup[strtolower($match[1])] = trim($match[2]);
            }
        }
    }

    $checklists = '';
    foreach ($requiredPtCases as $ptCase) {
        if (!is_string($ptCase)) continue;
        $checklist = $caseMarkup[strtolower($ptCase)] ?? '';
        if ($checklist === '') {
            $steps = $task['testerSteps'] ?? testerTaskInstructions($task);
            $stepMarkup = is_array($steps) ? implode('', array_map(static fn (mixed $step): string => is_string($step) ? '<li>' . e($step) . '</li>' : '', $steps)) : '';
            $checklist = '<span>' . e($ptCase . ' · Assigned checklist') . '</span><h3>' . e((string) $task['title']) . '</h3><ol>' . $stepMarkup . '</ol><p class="test-pass"><strong>Expected result:</strong> ' . e((string) ($task['expectedResult'] ?? 'Complete the assigned checklist within the stated safety boundary.')) . '</p>';
        }
        $checklists .= '<section class="pt-checklist-case">' . $checklist . '</section>';
    }
    return $checklists;
}

function portalRenderLogin(): never
{
    $content = '<div class="login"><div class="top"><div class="brand"><img class="brand-icon" src="/assets/project/app-icon.png" alt=""><span>24Seven.FM Player<br><small class="muted">Closed Alpha tester portal</small></span></div></div>' . portalNotice('notice') . portalNotice('error') . '<section class="card"><p class="eyebrow">Tester self-service</p><h1>Open your tester portal</h1><p class="muted">Enter the address registered for the Closed Alpha. We will send a single-use sign-in link if it matches an active tester.</p><form method="post"><input type="hidden" name="action" value="request_link"><label for="email">Registered email</label><input id="email" name="email" type="email" autocomplete="email" required><div class="cf-turnstile" data-sitekey="' . e(TESTER_PORTAL_TURNSTILE_SITEKEY) . '" data-action="' . e(TESTER_PORTAL_TURNSTILE_ACTION) . '"></div><button class="button" type="submit">Send sign-in link</button></form><p class="muted small">Never enter a password, account credential, or verification code here.</p></section></div><script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>';
    portalPage('Tester portal sign in', $content);
}

function portalRenderLinkConfirmation(PDO $database, string $token): never
{
    portalPendingToken($database, $token);
    $content = '<div class="login"><div class="top"><div class="brand"><img class="brand-icon" src="/assets/project/app-icon.png" alt=""><span>24Seven.FM Player<br><small class="muted">Closed Alpha tester portal</small></span></div></div><section class="card"><p class="eyebrow">Secure sign in</p><h1>Continue to your tester portal</h1><p class="muted">Use this device to continue. This confirmation helps prevent email security scanners from consuming your one-time link.</p><form method="post"><input type="hidden" name="action" value="consume_link"><input type="hidden" name="token" value="' . e($token) . '"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="button" type="submit">Continue securely</button></form></section></div>';
    portalPage('Continue to tester portal', $content);
}

function portalRenderDashboard(PDO $database, array $tester, bool $adminPreview = false, string $coordinatorName = '24Seven.FM Coordinator'): never
{
    $profile = profileSummary($tester);
    $assignments = portalAssignments($database, (int) $tester['id']);
    $tasks = taskRegistry();
    $feedback = $database->prepare('SELECT assignment_id, pt_case, subject, outcome, category, created_at FROM tester_feedback WHERE tester_id = ? ORDER BY created_at DESC LIMIT 5');
    $feedback->execute([(int) $tester['id']]);
    $reports = $feedback->fetchAll();
    $optedIn = is_string($tester['play_opt_in_confirmed_at'] ?? null) && $tester['play_opt_in_confirmed_at'] !== '';
    $smokeTested = is_string($tester['initial_smoke_test_confirmed_at'] ?? null) && $tester['initial_smoke_test_confirmed_at'] !== '';
    $assigned = array_filter($assignments, static fn (array $assignment): bool => in_array($assignment['task_status'], ['assigned', 'in_progress', 'blocked'], true));
    $steps = [[true, 'Applied', 'Your protected application and participation consent are recorded.'], [$profile['complete'], 'Profile & Device', $profile['complete'] ? 'Your assignment coverage is complete.' : 'Finish the guided Profile & Device setup so the coordinator can match work to your actual coverage.'], [$optedIn, 'Install Player / Play Opt-In', $optedIn ? 'Your self-confirmation is recorded.' : 'Open Google Play, opt in, then install or update the Player before confirming here.'], [$smokeTested, 'First-Use Smoke Test', $smokeTested ? 'Your self-confirmation is recorded.' : 'Launch, play one station, switch stations, briefly background playback, and return to the Player.'], [count($assigned) > 0, 'Active Assignment', count($assigned) > 0 ? 'Your current focused work is available below.' : 'A coordinator will add focused work when coverage is needed.']];
    $optInAction = !$optedIn
        ? '<section class="task" style="margin-top:22px"><p class="eyebrow">Step 3 of 5 · Install Player</p><h3>Install the Player from Google Play</h3><p>After finishing Profile &amp; Device, open the Google Play test page. Complete opt-in if Google Play asks, then choose <strong>Install</strong> or <strong>Update</strong>. Return here only after that is complete.</p><p><a class="button secondary" href="https://play.google.com/apps/testing/com.codeframe78.twentyfourseven.player">Open Google Play to install Player</a></p><form method="post"><input type="hidden" name="action" value="confirm_opt_in"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label><input style="width:auto" type="checkbox" name="confirm_opt_in" value="yes" required> I completed the Google Play opt-in with my registered account.</label><button class="button" type="submit">Record my opt-in confirmation</button></form></section>'
        : '';
    $smokeTestAction = $optedIn && !$smokeTested
        ? '<section class="task" style="margin-top:22px"><p class="eyebrow">Short first-use check</p><h3>Initial smoke test</h3><p>On your Play-installed build: launch the Player, play one station for a few minutes, switch stations, briefly background playback and use Android media controls, then return to the Player. Report anything that failed or felt confusing.</p><form method="post"><input type="hidden" name="action" value="confirm_initial_smoke_test"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label><input style="width:auto" type="checkbox" name="confirm_initial_smoke_test" value="yes" required> I completed this initial smoke test on my Play-installed build. This is my own confirmation, not automated installation or activity evidence.</label><button class="button" type="submit">Record smoke-test confirmation</button></form></section>'
        : '';
    $withdrawalRequestedAt = is_string($tester['withdrawal_requested_at'] ?? null) ? $tester['withdrawal_requested_at'] : '';
    $deletionRequestedAt = is_string($tester['deletion_requested_at'] ?? null) ? $tester['deletion_requested_at'] : '';
    $privacyAction = '<section class="card"><p class="eyebrow">Leaving the program</p><h2>Withdrawal and tester-record requests</h2><p class="muted">A withdrawal stops your participation. A deletion request asks the coordinator to remove or anonymize your private tester record under the program retention policy. Neither request affects a separate 24Seven.FM station account.</p>'
        . ($withdrawalRequestedAt === ''
            ? '<form method="post"><input type="hidden" name="action" value="request_privacy_action"><input type="hidden" name="privacy_request" value="withdrawal"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label><input style="width:auto" type="checkbox" name="confirm_privacy_request" value="yes" required> I want to withdraw from this closed test.</label><button class="button secondary" type="submit">Request withdrawal</button></form>'
            : '<p class="notice"><strong>Withdrawal requested:</strong> ' . e(portalHumanTimestamp($withdrawalRequestedAt)) . '. The coordinator will stop program access while handling this request.</p>')
        . ($deletionRequestedAt === ''
            ? '<form method="post" style="margin-top:16px"><input type="hidden" name="action" value="request_privacy_action"><input type="hidden" name="privacy_request" value="deletion"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label><input style="width:auto" type="checkbox" name="confirm_privacy_request" value="yes" required> I request deletion or anonymization of my private tester record.</label><button class="button secondary" type="submit">Request record deletion</button></form>'
            : '<p class="notice"><strong>Record-deletion request:</strong> ' . e(portalHumanTimestamp($deletionRequestedAt)) . '. The coordinator will verify and process it under the retention policy.</p>')
        . '<p class="muted small">Do not include passwords, station credentials, CAPTCHA answers, session data, or authentication secrets in a request.</p></section>';
    $assignmentCards = '';
    $reportAssignmentOptions = '';
    $reportCaseOptions = '';
    $previewTesterId = $adminPreview ? (int) $tester['id'] : null;
    $focusedAssignmentId = filter_var($_GET['assignment'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    if ($focusedAssignmentId === false) $focusedAssignmentId = null;
    $activeAssignmentCount = 0;
    $reportedCaseCount = 0;
    $requiredCaseCount = 0;
    foreach ($assignments as $assignment) {
        $task = $tasks[$assignment['task_id']] ?? null;
        if (!is_array($task)) continue;
        $state = $assignment['task_status'] === 'blocked' ? 'blocked' : ($assignment['task_status'] === 'complete' ? '' : 'pending');
        $progress = assignmentReportProgress($database, (int) $assignment['id'], $task);
        $plan = implode('', array_map(static fn (string $step): string => '<li>' . e($step) . '</li>', portalAssignmentSteps($database, $task, $assignment)));
        $latestReview = latestAssignmentReviewEvent($database, (int) $assignment['id']);
        $reviewSubmitted = is_string($assignment['submitted_for_review_at'] ?? null) && $assignment['submitted_for_review_at'] !== '';
        $reviewReturned = ($latestReview['decision'] ?? '') === 'returned' && !$reviewSubmitted;
        $progressText = count($progress['reported']) . ' of ' . count($progress['required']) . ' PT-case results submitted';
        $reportedCaseCount += count($progress['reported']);
        $requiredCaseCount += count($progress['required']);
        if (in_array($assignment['task_status'], ['assigned', 'in_progress', 'blocked'], true)) {
            $activeAssignmentCount++;
        }
        $progressMeter = '<div class="work-record-progress"><span><strong>Reporting progress</strong> ' . e($progressText) . '</span><progress value="' . count($progress['reported']) . '" max="' . max(1, count($progress['required'])) . '">' . e($progressText) . '</progress></div>';
        $dialogId = 'pt-checklist-' . (int) $assignment['id'];
        $assignedScope = trim((string) ($assignment['station_scope'] ?? '')) ?: (string) $task['stationScope'];
        $retestLabel = trim((string) ($assignment['retest_pt_case'] ?? '')) === '' ? '' : 'Separate re-test · ';
        $checklistDialog = '<dialog id="' . e($dialogId) . '" class="pt-checklist-dialog" aria-labelledby="' . e($dialogId . '-title') . '"><div class="pt-checklist-dialog-content"><header><p class="eyebrow">' . e($retestLabel) . 'Assigned PT checklist</p><h2 id="' . e($dialogId . '-title') . '">' . e($task['id'] . ' — ' . $task['title']) . '</h2><p class="muted">Only the PT case' . (count($progress['required']) === 1 ? '' : 's') . ' assigned with this focused task are shown. Scope: ' . e($assignedScope) . '.</p></header>' . portalAssignedChecklistMarkup($task, $progress['required']) . '<div class="pt-checklist-dialog-actions"><button type="button" class="button secondary" data-pt-checklist-close>Close checklist</button></div></div></dialog>';
        $reviewAction = '';
        if ($reviewReturned) {
            $reviewAction = '<section class="notice"><strong>Coordinator clarification requested:</strong> ' . nl2br(e((string) ($latestReview['tester_note'] ?? '')), false) . '<form method="post"><input type="hidden" name="action" value="submit_review_clarification"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="assignment_id" value="' . (int) $assignment['id'] . '"><label>Clarification for the Coordinator <textarea name="clarification_response" maxlength="' . PORTAL_MAX_FEEDBACK_DETAILS . '" required placeholder="Respond to the Coordinator’s requested clarification. Do not include credentials, session information, or private screenshots."></textarea></label><button class="button" type="submit">Submit clarification for review</button></form></section>';
        } elseif ($reviewSubmitted) {
            $reviewAction = '<p class="notice"><strong>Submitted for Coordinator review:</strong> ' . e(portalHumanTimestamp((string) $assignment['submitted_for_review_at'])) . '. The Coordinator will record the final status.</p>';
        } elseif ($progress['complete'] && $assignment['task_status'] !== 'complete') {
            $reviewAction = '<form method="post"><input type="hidden" name="action" value="submit_task_for_review"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><input type="hidden" name="assignment_id" value="' . (int) $assignment['id'] . '"><button class="button" type="submit">Submit task for Coordinator review</button></form>';
        } elseif ($assignment['task_status'] !== 'complete') {
            $reviewAction = '<p class="muted small">Report every assigned PT case before this task can be submitted for Coordinator review. Missing: ' . e(implode(', ', $progress['missing'])) . '.</p>';
        }
        if (!$reviewSubmitted && !$reviewReturned && $assignment['task_status'] !== 'complete') {
            $selected = $focusedAssignmentId === (int) $assignment['id'] ? ' selected' : '';
            $reportAssignmentOptions .= '<option value="' . (int) $assignment['id'] . '"' . $selected . '>' . e($task['id'] . ' — ' . $progressText) . '</option>';
            $reportCaseOptions .= '<optgroup label="' . e($task['id'] . ' — ' . $task['title']) . '">';
            foreach ($progress['missing'] as $ptCase) {
                $reportCaseOptions .= '<option value="' . e($ptCase) . '" data-assignment-id="' . (int) $assignment['id'] . '">' . e($ptCase) . '</option>';
            }
            $reportCaseOptions .= '</optgroup>';
        }
        $reportLink = !$reviewSubmitted && !$reviewReturned && $assignment['task_status'] !== 'complete'
            ? '<a class="button secondary" href="' . e(portalWorkspaceUrl('reports', $previewTesterId, ['assignment' => (int) $assignment['id']])) . '">Report a PT case</a>'
            : '';
        $assignmentCards .= '<article class="task work-record"><div class="work-record-heading"><div><p class="eyebrow">' . ($retestLabel === '' ? 'Focused task' : 'Focused re-test') . '</p><h3>' . e($task['id'] . ' — ' . $task['title']) . '</h3></div><span class="pill ' . $state . '">' . e(ucwords(str_replace('_', ' ', $assignment['task_status']))) . '</span></div><div class="work-record-context"><span><strong>PT cases</strong> ' . e(implode(', ', $progress['required'])) . '</span><span><strong>Scope</strong> ' . e((string) ($assignment['station_scope'] ?: $task['stationScope'])) . '</span></div>' . assignmentLifecycleMarkup($assignment, $progress, 'tester', $latestReview) . $progressMeter . '<div class="work-record-instructions"><p class="task-plan-label">Complete and report</p><ol class="task-plan">' . $plan . '</ol></div><div class="work-record-actions"><button type="button" class="button secondary" data-pt-checklist-open="' . e($dialogId) . '" aria-haspopup="dialog">Open the detailed PT checklist</button>' . $reportLink . '</div>' . $reviewAction . '</article>' . $checklistDialog;
    }
    if ($assignmentCards === '') $assignmentCards = '<article class="task"><h3>No focused task yet</h3><p>Your coordinator will match an assignment to your coverage and available setup.</p></article>';
    $reportForm = $reportAssignmentOptions === ''
        ? '<p class="muted">No PT-case report is waiting. A submitted task stays with the Coordinator until it is reviewed.</p>'
        : '<form method="post"><input type="hidden" name="action" value="submit_feedback"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label>Assigned task</label><select name="assignment_id" required><option value="">Choose an assigned task</option>' . $reportAssignmentOptions . '</select><label>PT case</label><select name="pt_case" required><option value="">Choose the case you completed</option>' . $reportCaseOptions . '</select><label>Feedback category</label><select name="category" required>' . implode('', array_map(static fn (string $key, string $label): string => '<option value="' . e($key) . '">' . e($label) . '</option>', array_keys(FEEDBACK_CATEGORIES), FEEDBACK_CATEGORIES)) . '</select><label>Outcome</label><select name="outcome" required><option value="pass">Pass / worked as expected</option><option value="issue">Issue found</option><option value="blocked">Blocked</option><option value="note">Usability note</option></select><label>Short title</label><input name="subject" maxlength="' . PORTAL_MAX_FEEDBACK_SUBJECT . '" required><label>Steps and result</label><textarea name="details" maxlength="' . PORTAL_MAX_FEEDBACK_DETAILS . '" required></textarea><button class="button" type="submit">Submit PT-case report</button></form>';
    $requestedView = portalWorkspaceView();
    $needsOnboarding = !$profile['complete'] || !$optedIn || !$smokeTested;
    $view = $needsOnboarding ? 'onboarding' : $requestedView;
    $progressChecklist = '<section class="card"><p class="eyebrow">Your progress</p><h2>Onboarding checklist</h2><div>' . implode('', array_map(static fn (array $step): string => '<div class="step ' . ($step[0] ? 'done' : '') . '"><span class="dot">' . ($step[0] ? '✓' : '○') . '</span><span><b>' . e($step[1]) . '</b><small>' . e($step[2]) . '</small></span></div>', $steps)) . '</div></section>';
    $profileForm = '<form method="post"><input type="hidden" name="action" value="save_profile"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label>Name</label><input name="display_name" maxlength="100" value="' . e($tester['display_name']) . '" required><label>Country or region</label><input name="country" maxlength="80" value="' . e($tester['country']) . '"><p class="muted small"><strong>Registered email:</strong> ' . e($tester['email']) . '<br>Email changes are reviewed by the coordinator because this address is your sign-in and Google Play eligibility identity.</p><label>Current device</label><input name="device" value="' . e($tester['device']) . '" required><label>Android version</label><input name="android_version" value="' . e($tester['android_version']) . '" required><label>Primary station</label>' . portalSelect('primary_station', PORTAL_PRIMARY_STATIONS, $tester['primary_station'], 'Choose a primary station') . '<label>Other familiar stations</label>' . portalCheckboxes('other_stations', PORTAL_STATIONS, listFromJson($tester['other_stations_json'])) . '<label>Device form factor</label>' . portalSelect('device_form_factor', PORTAL_DEVICE_TYPES, $tester['device_form_factor'], 'Choose a device type') . '<label>Other devices or configuration notes</label><textarea name="other_devices">' . e($tester['other_devices']) . '</textarea><label>Testing interests</label>' . portalCheckboxes('testing_interests', PORTAL_TESTING_INTERESTS, listFromJson($tester['interests_json'])) . '<label>Existing station access (optional; station names only)</label><p class="muted small">Guest testing does not require a 24Seven.FM account. Leave this clear or choose None; never create an account or enter credentials for this program.</p>' . portalCheckboxes('station_accounts', PORTAL_STATIONS + ['none' => 'None'], listFromJson($tester['station_accounts_json'])) . '<label>Network capabilities</label>' . portalCheckboxes('network_capabilities', PORTAL_NETWORK, listFromJson($tester['network_capabilities_json'])) . '<label>Audio/accessory capabilities</label>' . portalCheckboxes('audio_capabilities', PORTAL_AUDIO, listFromJson($tester['audio_capabilities_json'])) . '<label>Accessibility and alternative input</label>' . portalCheckboxes('accessibility_capabilities', PORTAL_ACCESSIBILITY, listFromJson($tester['accessibility_capabilities_json'])) . '<label>Testing comfort</label>' . portalSelect('testing_comfort', PORTAL_TESTING_COMFORT, $tester['testing_comfort'], 'Choose a testing comfort level') . '<label>Controlled-test preferences</label>' . portalCheckboxes('controlled_actions', PORTAL_CONTROLLED_ACTIONS, listFromJson($tester['controlled_actions_json'])) . '<label>Typical two-week availability</label>' . portalSelect('testing_availability', PORTAL_AVAILABILITY, $tester['testing_availability'], 'Choose availability') . '<label>Assignment notes or prior testing experience</label><textarea name="experience" maxlength="1200">' . e($tester['experience']) . '</textarea><button class="button" type="submit">Save Profile &amp; Device details</button></form>';
    $profilePanel = '<section id="intake-profile" class="card"><p class="eyebrow">Profile &amp; Device</p><h2>Maintain your testing coverage</h2><p class="muted">Keep your device, setup, and testing coverage current so the coordinator can assign work that matches your actual configuration.</p>' . $profileForm . '</section>';
    $onboardingPanel = '<div class="two" data-portal-onboarding><section id="intake-profile" class="card"><p class="eyebrow">Profile &amp; Device · Steps 1–2 of 5</p><h2>Finish your tester profile</h2><p class="muted">Complete the guided profile and device coverage screens, then choose <strong>Finish Profile &amp; Continue to Install</strong>. That lets the coordinator match focused work to your setup.</p>' . str_replace('Save Profile &amp; Device details', 'Finish Profile &amp; Continue to Install', $profileForm) . $optInAction . $smokeTestAction . '</section><section class="card"><p class="eyebrow">Guided setup</p><h2>What happens next</h2><p class="muted">The five-step guide keeps your onboarding focused. Confirm only actions you personally completed; it does not infer installation or app activity.</p>' . $progressChecklist . '</section></div>';
    $completedOnboardingPanel = '<section class="card"><p class="eyebrow">Onboarding complete</p><h2>Your self-reported setup is recorded</h2><p class="muted">Your Profile &amp; Device details, Google Play opt-in, and first-use smoke test are each retained as separate self-confirmations. A coordinator can now assign focused work that matches your coverage.</p><p class="notice"><strong>Google Play opt-in:</strong> ' . e(portalHumanTimestamp((string) $tester['play_opt_in_confirmed_at'])) . '<br><strong>Initial smoke test:</strong> ' . e(portalHumanTimestamp((string) $tester['initial_smoke_test_confirmed_at'])) . '</p></section>' . $progressChecklist;
    $tasksPanel = '<section class="workspace-primary-card"><div><p class="eyebrow">My work queue</p><h2>' . e($activeAssignmentCount > 0 ? (string) $activeAssignmentCount . ' focused assignment' . ($activeAssignmentCount === 1 ? '' : 's') . ' in progress' : 'No focused task yet') . '</h2><p class="muted">' . e($activeAssignmentCount > 0 ? (string) $reportedCaseCount . ' of ' . (string) $requiredCaseCount . ' assigned PT-case results are recorded. Open each work record for its exact scope, safety boundary, and reporting path.' : 'Your Coordinator will add focused work when your completed device coverage is needed. Your onboarding record remains available in the meantime.') . '</p></div>' . ($activeAssignmentCount > 0 ? '<a class="button" href="' . e(portalWorkspaceUrl('reports', $previewTesterId)) . '">Report results</a>' : '<a class="button secondary" href="' . e(portalWorkspaceUrl('onboarding', $previewTesterId)) . '">View onboarding status</a>') . '</section><section class="card"><p class="eyebrow">Focused assignments</p><h2>Your task records</h2><div class="grid" style="grid-template-columns:1fr">' . $assignmentCards . '</div></section>';
    $activityPanel = testerActivityTimeline($database, $tester, $assignments, $tasks, 'tester');
    $reportsPanel = '<section class="workspace-primary-card"><div><p class="eyebrow">Evidence queue</p><h2>Report each PT case separately</h2><p class="muted">A task moves to Coordinator review only after every assigned case has a structured tester report. The Coordinator, not the portal, records its final status.</p></div><a class="button secondary" href="' . e(portalWorkspaceUrl('tasks', $previewTesterId)) . '">Review my task records</a></section><section class="card"><p class="eyebrow">Task report</p><h2>Submit feedback for a focused task</h2><p class="muted">Submit one structured result for each assigned PT case, including Blocked when you cannot proceed. Do not include passwords, credentials, private messages, session information, or private screenshots.</p>' . $reportForm . ($reports === [] ? '' : '<h3 style="margin-top:24px">Recent reports</h3>' . implode('', array_map(static fn (array $report): string => '<p class="small"><span class="pill">' . e($report['outcome']) . '</span> ' . e($report['pt_case'] ?? 'PT case') . ' · ' . e(FEEDBACK_CATEGORIES[$report['category']] ?? FEEDBACK_CATEGORIES['other']) . ' · ' . e($report['subject']) . ' <span class="muted">' . e(portalHumanTimestamp((string) $report['created_at'])) . '</span></p>', $reports))) . '</section>';
    $nextView = $needsOnboarding ? 'onboarding' : (count($assigned) > 0 ? 'tasks' : 'dashboard');
    $nextLabel = $needsOnboarding ? 'Continue onboarding' : (count($assigned) > 0 ? 'Open my tasks' : 'View onboarding status');
    $dashboardPanel = '<section class="workspace-primary-card"><div><p class="eyebrow">Your next action</p><h2>' . e($nextLabel) . '</h2><p class="muted">' . ($needsOnboarding ? 'Finish the evidence only you can provide, then your Coordinator can safely match focused work to your setup.' : (count($assigned) > 0 ? 'Your assignment has an exact PT-case checklist, safety boundary, and reporting path ready for you.' : 'Your onboarding evidence is recorded. Your Coordinator will assign focused work when your coverage is needed.')) . '</p></div><a class="button" href="' . e(portalWorkspaceUrl($nextView, $previewTesterId)) . '">' . e($nextLabel) . '</a></section>'
        . '<section class="workspace-attention-grid" aria-label="Tester workspace overview"><a class="attention-card" href="' . e(portalWorkspaceUrl('onboarding', $previewTesterId)) . '"><span>Onboarding</span><strong>' . e($needsOnboarding ? 'Action needed' : 'Complete') . '</strong><small>' . e($needsOnboarding ? 'Continue the protected setup journey.' : 'Profile, opt-in, and smoke-test evidence are recorded.') . '</small></a><a class="attention-card" href="' . e(portalWorkspaceUrl('tasks', $previewTesterId)) . '"><span>My work</span><strong>' . e(count($assigned) > 0 ? (string) count($assigned) . ' assignment' . (count($assigned) === 1 ? '' : 's') : 'No assignment') . '</strong><small>Open the exact checklist and safety boundary for your focused work.</small></a><a class="attention-card" href="' . e(portalWorkspaceUrl('support', $previewTesterId)) . '"><span>Coordinator</span><strong>Private support</strong><small>Ask a question without placing program information in public channels.</small></a></section>'
        . $progressChecklist
        . '<section class="card"><p class="eyebrow">Workspace</p><h2>Open a record</h2><div class="workspace-action-grid"><a href="' . e(portalWorkspaceUrl('profile', $previewTesterId)) . '"><strong>Profile &amp; Device</strong><span>Update your coverage and registered setup.</span></a><a href="' . e(portalWorkspaceUrl('tasks', $previewTesterId)) . '"><strong>My Tasks</strong><span>Read your assigned instructions and safety boundary.</span></a><a href="' . e(portalWorkspaceUrl('reports', $previewTesterId)) . '"><strong>Report results</strong><span>Record one outcome for each assigned PT case.</span></a><a href="' . e(portalWorkspaceUrl('activity', $previewTesterId)) . '"><strong>Activity</strong><span>See the evidence already recorded for your journey.</span></a><a href="' . e(portalWorkspaceUrl('support', $previewTesterId)) . '"><strong>Support</strong><span>Open your private Coordinator conversation.</span></a></div></section>';
    $pageContent = match ($view) {
        'onboarding' => $needsOnboarding ? $onboardingPanel : $completedOnboardingPanel,
        'profile' => $profilePanel . $privacyAction,
        'tasks' => $tasksPanel,
        'reports' => $reportsPanel,
        'activity' => $activityPanel,
        'support' => portalChatPanel($database, $tester, $coordinatorName),
        default => $dashboardPanel,
    };
    $workspaceTitle = match ($view) {
        'onboarding' => 'Onboarding',
        'profile' => 'Profile & Device',
        'tasks' => 'My Tasks',
        'reports' => 'Report results',
        'activity' => 'Activity',
        'support' => 'Support',
        default => 'My workspace',
    };
    $workspaceDescription = match ($view) {
        'onboarding' => 'Complete each self-confirmed step so your Coordinator can safely assign focused testing work.',
        'profile' => 'Maintain the device and coverage record used to match work to your actual setup.',
        'tasks' => 'Follow the exact assigned scope, safety boundary, and PT-case reporting plan.',
        'reports' => 'Submit one structured outcome for each assigned PT case.',
        'activity' => 'Review the protected onboarding and task evidence already recorded for you.',
        'support' => 'Keep program questions and Coordinator communication in this private workspace.',
        default => 'Your protected place for onboarding evidence, focused work, results, and support. Guest testing does not require a 24Seven.FM station account.',
    };
    $content = '<header class="workspace-page-header"><div class="workspace-page-heading"><p class="eyebrow">24Seven.FM Player · Closed Alpha</p><h1>' . e($workspaceTitle) . '</h1><p class="muted">' . e($workspaceDescription) . '</p></div><div class="workspace-page-actions"><span class="workspace-role">Tester workspace</span><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="button secondary" type="submit">Sign out</button></form></div></header>' . portalNotice('notice') . portalNotice('error') . '<div data-portal-view="' . e($view) . '">' . $pageContent . '</div>';
    if ($adminPreview) {
        $content = '<p class="notice admin-preview"><strong>Read-only coordinator preview.</strong> This is what the tester sees. Profile, opt-in, and report controls are disabled here; no tester session or data is changed. <a href="/private-tester-queue.php?tester=' . (int) $tester['id'] . '">Return to the coordinator workspace</a>.</p>' . str_replace(['<form method="post">', '</form>'], ['<form method="post"><fieldset disabled>', '</fieldset></form>'], $content);
    }
    $title = $adminPreview ? 'Tester portal preview' : match ($view) {
        'onboarding' => 'Tester onboarding',
        'profile' => 'Profile & Device',
        'tasks' => 'My tester tasks',
        'reports' => 'Tester task reports',
        'activity' => 'Tester activity',
        'support' => 'Tester support',
        default => 'My tester portal',
    };
    portalPage($title, $content, portalWorkspaceNavigation($view, $previewTesterId));
}

try {
    $config = config();
    $adminPreviewTesterId = portalAdminPreviewTesterId();
    portalStartSession();
    $database = database($config);
    if ($adminPreviewTesterId !== null) {
        if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') fail(405, 'Tester previews are read-only.');
        portalRenderDashboard($database, portalTesterById($database, $adminPreviewTesterId), true, coordinatorDisplayName($config));
    }
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST' && ($_POST['action'] ?? '') === 'request_link') {
        if (!hash_equals(TESTER_PORTAL_ORIGIN, $_SERVER['HTTP_ORIGIN'] ?? '')) fail(403, 'This request is not allowed.');
        portalRequestLink($database, $config);
    }
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST' && ($_POST['action'] ?? '') === 'consume_link') {
        if (!validCsrf()) fail(403, 'The request could not be verified.');
        portalConsumeLink($database, (string) ($_POST['token'] ?? ''));
    }
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'GET' && isset($_GET['token']) && is_string($_GET['token'])) {
        portalRenderLinkConfirmation($database, $_GET['token']);
    }
    if (!isset($_SESSION['tester_portal_id'])) portalRenderLogin();
    $tester = portalTester($database);
    $coordinatorName = coordinatorDisplayName($config);
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'GET' && isset($_GET['chat_stream'])) portalChatPoll($database, $tester, $coordinatorName, max(0, (int) ($_GET['after'] ?? 0)), true);
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'GET' && isset($_GET['chat_poll'])) portalChatPoll($database, $tester, $coordinatorName, max(0, (int) ($_GET['after'] ?? 0)));
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'GET' && isset($_GET['chat_popout'])) portalRenderChatPopout($database, $tester, $coordinatorName);
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST') {
        if (!validCsrf()) fail(403, 'The request could not be verified.');
        $action = (string) ($_POST['action'] ?? '');
        if ($action === 'logout') { $_SESSION = []; session_destroy(); portalRedirect('?notice=' . rawurlencode('You are signed out.')); }
        if ($action === 'save_profile') portalSaveProfile($database, $tester, $config);
        if ($action === 'confirm_opt_in') portalConfirmOptIn($database, $tester);
        if ($action === 'confirm_initial_smoke_test') portalConfirmInitialSmokeTest($database, $tester);
        if ($action === 'request_privacy_action') portalRequestPrivacyAction($database, $tester);
        if ($action === 'submit_feedback') portalSubmitFeedback($database, $tester);
        if ($action === 'submit_task_for_review') portalSubmitTaskForReview($database, $tester);
        if ($action === 'submit_review_clarification') portalSubmitReviewClarification($database, $tester);
        if ($action === 'send_chat') {
            chatPostMessage($database, (int) $tester['id'], 'tester', (string) ($_POST['chat_body'] ?? ''));
            portalRedirect('?notice=' . rawurlencode('Your Live Chat message was sent.'));
        }
        if ($action === 'delete_chat_message') {
            $messageId = filter_var($_POST['message_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
            if ($messageId === false) throw new InvalidArgumentException('The requested chat message is invalid.');
            chatSoftDeleteMessage($database, (int) $tester['id'], 'tester', $messageId);
            portalRedirect('?notice=' . rawurlencode('Chat message removed from your view.'));
        }
        throw new InvalidArgumentException('The requested portal action is invalid.');
    }
    portalRenderDashboard($database, $tester, false, $coordinatorName);
} catch (InvalidArgumentException $exception) {
    portalRedirect('?error=' . rawurlencode($exception->getMessage()));
} catch (Throwable $exception) {
    error_log('tester portal request failed: ' . get_class($exception));
    fail(503, 'The tester portal is temporarily unavailable.');
}
