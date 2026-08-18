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

function portalPage(string $title, string $content): never
{
    header('Content-Type: text/html; charset=UTF-8');
    header('Cache-Control: no-store, private');
    header('Pragma: no-cache');
    header('X-Robots-Tag: noindex, nofollow, noarchive');
    header('X-Frame-Options: DENY');
    $page = '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>' . e($title) . '</title><style>'
        . ':root{color-scheme:dark;--bg:#0b0e14;--card:#151b26;--line:rgba(255,255,255,.1);--muted:#aab4c7;--text:#f7f8fc;--purple:#ad7cff;--teal:#6de5d1;--amber:#ffd27a;--red:#ff9ca6}*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 85% -15%,#5c2c873d,transparent 34%),var(--bg);color:var(--text);font:16px/1.5 Inter,Roboto,system-ui,sans-serif}.shell{max-width:1120px;margin:auto;padding:30px 20px 64px}.top{display:flex;justify-content:space-between;gap:18px;align-items:center;margin-bottom:30px}.brand{display:flex;gap:11px;align-items:center;font-weight:850}.brand-icon{width:34px;height:34px;border-radius:10px;object-fit:cover}.eyebrow{margin:0;color:#c3a6ff;font-size:11px;font-weight:850;letter-spacing:.12em;text-transform:uppercase}h1{margin:2px 0;font-size:32px;letter-spacing:-.04em}h2{margin:0;font-size:20px}h3{margin:0;font-size:15px}.muted{color:var(--muted)}.card{margin:18px 0;padding:23px;border:1px solid var(--line);border-radius:16px;background:linear-gradient(145deg,#171e2b,#111722);box-shadow:0 14px 34px #00000025}.hero{padding:32px}.hero p{max-width:650px}.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.metric,.task{padding:16px;border:1px solid var(--line);border-radius:13px;background:#0e131d}.metric b{display:block;margin-top:7px;font-size:14px}.dot{display:inline-grid;place-items:center;width:24px;height:24px;border:1px solid #66728a;border-radius:50%;color:#7d8a9f}.done .dot{border-color:var(--teal);background:#174d47;color:var(--teal)}.step{display:flex;gap:11px;align-items:start;padding:11px 0;border-bottom:1px solid var(--line)}.step:last-child{border:0}.step b{display:block}.step small{color:var(--muted)}.two{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(280px,.8fr);gap:18px}fieldset{border:0;margin:0;padding:0}label{display:block;margin:14px 0 6px;font-size:12px;font-weight:800;color:#cbd3e2}input,select,textarea{width:100%;padding:10px 11px;border:1px solid var(--line);border-radius:9px;background:#0d121b;color:var(--text);font:inherit}textarea{min-height:120px}input:focus,select:focus,textarea:focus{outline:3px solid #986bff30;border-color:var(--purple)}.choices{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;padding:4px 0}.choices label{display:flex;gap:8px;align-items:start;margin:0;padding:9px;border:1px solid var(--line);border-radius:9px;background:#0d121b;font-size:12px;font-weight:600}.choices input{width:auto;margin-top:2px}.button{display:inline-block;margin-top:18px;padding:10px 14px;border:0;border-radius:10px;background:linear-gradient(135deg,#b582ff,#8054f8);color:#fff;font:800 13px system-ui;cursor:pointer;text-decoration:none}.secondary{background:#273148}.pill{display:inline-flex;gap:5px;align-items:center;padding:5px 8px;border-radius:999px;background:#193e39;color:#9cf5e4;font-size:11px;font-weight:850}.pill.pending{background:#493b22;color:#ffdb8d}.pill.blocked{background:#472c35;color:#ffb4bc}.notice{padding:11px 13px;border-radius:10px;background:#183d37;color:#bffcf1}.error{background:#4a2730;color:#ffdce1}.admin-preview{border:1px solid #b685ff66;background:#332551;color:#eadfff}.task p{margin:6px 0;color:var(--muted);font-size:13px}.task ul{padding-left:19px;color:#d6deed;font-size:13px}.right{float:right}.login{max-width:530px;margin:9vh auto}.login .card{padding:30px}.small{font-size:12px}@media(max-width:760px){.grid,.two{grid-template-columns:1fr}.choices{grid-template-columns:1fr}.shell{padding:20px 14px}.hero{padding:22px}.top{align-items:start}h1{font-size:27px}}</style></head><body><main class="shell">' . $content . '</main></body></html>';
    $page = str_replace('</style></head><body><main class="shell">', '</style><link rel="stylesheet" href="/assets/onboarding-portal.css"><link rel="stylesheet" href="/assets/onboarding-wizard.css?v=wizard-centered-1"></head><body><a class="skip-link" href="#tester-portal">Skip to tester portal</a><main id="tester-portal" class="portal-preview-shell">', $page);
    $page = str_replace('</main></body></html>', '<script>(function(){const form=document.querySelector("form input[name=action][value=save_profile]")?.form;if(!form)return;const required=["display_name","device","android_version","primary_station","device_form_factor","network_capabilities","audio_capabilities","accessibility_capabilities","testing_comfort","controlled_actions","testing_availability"];const missing=new URLSearchParams(location.search).get("profile_missing")?.split(",")||[];for(const name of required){const group=form.querySelector(`[name="${name}[]"]`)?.closest(".choices");const control=group||form.querySelector(`[name="${name}"]`);if(!control)continue;const label=group?group.previousElementSibling:control.previousElementSibling;const id=`profile-${name}`;control.id=id;if(label?.tagName==="LABEL"){label.id=`${id}-label`;if(!label.querySelector(".required-mark")){label.insertAdjacentHTML("beforeend",` <span class="required-mark" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span>`);}if(group){group.setAttribute("role","group");group.setAttribute("aria-labelledby",label.id);group.setAttribute("aria-required","true");}}if(missing.includes(name)){control.classList.add("profile-field-missing");}}const first=missing.map(name=>document.getElementById(`profile-${name}`)).find(Boolean);if(first){first.scrollIntoView({block:"center"});first.focus({preventScroll:true});}})();</script></main></body></html>', $page);
    $page = str_replace('</main></body></html>', '<script src="/assets/onboarding-profile-form.js" defer></script></main></body></html>', $page);
    $page = str_replace('</main></body></html>', '<script src="/assets/onboarding-wizard.js?v=wizard-card-split-1" defer></script></main></body></html>', $page);
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
    $query = $database->prepare('SELECT id, task_id, task_status, station_scope, configuration_scope, created_at, updated_at FROM tester_task_assignments WHERE tester_id = ? ORDER BY created_at DESC, id DESC');
    $query->execute([$testerId]);
    return $query->fetchAll();
}

function portalChatPayload(PDO $database, array $tester, string $coordinatorName, int $afterId = 0): array
{
    $messages = chatMessages($database, (int) $tester['id'], 'tester', $afterId);
    return array_map(static fn (array $message): array => [
        'id' => (int) $message['id'],
        'sender' => $message['sender_role'] === 'tester' ? 'You' : $coordinatorName,
        'role' => $message['sender_role'],
        'body' => (string) $message['body'],
        'createdAt' => (string) $message['created_at'],
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
    $assignment = $database->prepare('SELECT id FROM tester_task_assignments WHERE id = ? AND tester_id = ?');
    $assignment->execute([$assignmentId, (int) $tester['id']]);
    if ($assignment->fetch() === false) throw new InvalidArgumentException('That task is no longer available.');
    $outcome = (string) ($_POST['outcome'] ?? '');
    if (!in_array($outcome, ['pass', 'issue', 'blocked', 'note'], true)) throw new InvalidArgumentException('Choose a valid report outcome.');
    $category = (string) ($_POST['category'] ?? '');
    if (!array_key_exists($category, FEEDBACK_CATEGORIES)) throw new InvalidArgumentException('Choose a valid feedback category.');
    $database->prepare('INSERT INTO tester_feedback(tester_id, assignment_id, subject, details, outcome, category, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)')->execute([(int) $tester['id'], $assignmentId, portalText('subject', PORTAL_MAX_FEEDBACK_SUBJECT, true), portalText('details', PORTAL_MAX_FEEDBACK_DETAILS, true), $outcome, $category, gmdate('c')]);
    portalRedirect('?notice=' . rawurlencode('Your task report has been saved for the coordinator.'));
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
    $feedback = $database->prepare('SELECT subject, outcome, category, created_at FROM tester_feedback WHERE tester_id = ? ORDER BY created_at DESC LIMIT 5');
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
            : '<p class="notice"><strong>Withdrawal requested:</strong> ' . e($withdrawalRequestedAt) . '. The coordinator will stop program access while handling this request.</p>')
        . ($deletionRequestedAt === ''
            ? '<form method="post" style="margin-top:16px"><input type="hidden" name="action" value="request_privacy_action"><input type="hidden" name="privacy_request" value="deletion"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label><input style="width:auto" type="checkbox" name="confirm_privacy_request" value="yes" required> I request deletion or anonymization of my private tester record.</label><button class="button secondary" type="submit">Request record deletion</button></form>'
            : '<p class="notice"><strong>Record-deletion request:</strong> ' . e($deletionRequestedAt) . '. The coordinator will verify and process it under the retention policy.</p>')
        . '<p class="muted small">Do not include passwords, station credentials, CAPTCHA answers, session data, or authentication secrets in a request.</p></section>';
    $assignmentCards = '';
    foreach ($assignments as $assignment) {
        $task = $tasks[$assignment['task_id']] ?? null;
        if (!is_array($task)) continue;
        $state = $assignment['task_status'] === 'blocked' ? 'blocked' : ($assignment['task_status'] === 'complete' ? '' : 'pending');
        $assignmentCards .= '<article class="task"><span class="pill ' . $state . '">' . e(ucwords(str_replace('_', ' ', $assignment['task_status']))) . '</span><h3 style="margin-top:10px">' . e($task['id'] . ' — ' . $task['title']) . '</h3><p>' . e($task['purpose']) . '</p><p><strong>Scope:</strong> ' . e($assignment['station_scope'] ?: $task['stationScope']) . '<br><strong>Configuration:</strong> ' . e($assignment['configuration_scope'] ?: 'Use your registered device configuration.') . '</p><ul><li>PT cases: ' . e(implode(', ', $task['ptIds'])) . '</li><li>Safety: ' . e($task['safetyWarning']) . '</li></ul><a class="button secondary" href="/product-testing/?task=' . rawurlencode($task['id']) . '">Open task cases</a></article>';
    }
    if ($assignmentCards === '') $assignmentCards = '<article class="task"><h3>No focused task yet</h3><p>Your coordinator will match an assignment to your coverage and available setup.</p></article>';
    $content = '<div class="top"><div class="brand"><img class="brand-icon" src="/assets/project/app-icon.png" alt=""><span>24Seven.FM Player<br><small class="muted">Closed Alpha tester portal</small></span></div><form method="post"><input type="hidden" name="action" value="logout"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><button class="button secondary" type="submit">Sign out</button></form></div>' . portalNotice('notice') . portalNotice('error')
        . '<section class="card hero"><p class="eyebrow">Tester dashboard</p><h1>Welcome, ' . e($tester['display_name']) . '</h1><p class="muted">To start: finish Profile &amp; Device, install the Player through Google Play, then complete the short first-use check. This independently developed, unofficial Player is tested through Google Play Closed Testing. Confirm only actions you completed, and submit focused testing results without exposing your data to other testers. Guest testing does not require a 24Seven.FM station account.</p></section>'
        . '<section class="card"><h2>Onboarding checklist</h2><div>' . implode('', array_map(static fn (array $step): string => '<div class="step ' . ($step[0] ? 'done' : '') . '"><span class="dot">' . ($step[0] ? '✓' : '○') . '</span><span><b>' . e($step[1]) . '</b><small>' . e($step[2]) . '</small></span></div>', $steps)) . '</div></section>'
        . '<div class="two"><section id="intake-profile" class="card"><p class="eyebrow">Profile &amp; Device · Steps 1–2 of 5</p><h2>Finish your tester profile</h2><p class="muted">Complete the guided profile and device coverage screens, then choose <strong>Finish Profile &amp; Continue to Install</strong>. That lets the coordinator match focused work to your setup.</p><form method="post"><input type="hidden" name="action" value="save_profile"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label>Name</label><input name="display_name" maxlength="100" value="' . e($tester['display_name']) . '" required><label>Country or region</label><input name="country" maxlength="80" value="' . e($tester['country']) . '"><p class="muted small"><strong>Registered email:</strong> ' . e($tester['email']) . '<br>Email changes are reviewed by the coordinator because this address is your sign-in and Google Play eligibility identity.</p><label>Current device</label><input name="device" value="' . e($tester['device']) . '" required><label>Android version</label><input name="android_version" value="' . e($tester['android_version']) . '" required><label>Primary station</label>' . portalSelect('primary_station', PORTAL_PRIMARY_STATIONS, $tester['primary_station'], 'Choose a primary station') . '<label>Other familiar stations</label>' . portalCheckboxes('other_stations', PORTAL_STATIONS, listFromJson($tester['other_stations_json'])) . '<label>Device form factor</label>' . portalSelect('device_form_factor', PORTAL_DEVICE_TYPES, $tester['device_form_factor'], 'Choose a device type') . '<label>Other devices or configuration notes</label><textarea name="other_devices">' . e($tester['other_devices']) . '</textarea><label>Testing interests</label>' . portalCheckboxes('testing_interests', PORTAL_TESTING_INTERESTS, listFromJson($tester['interests_json'])) . '<label>Existing station access (optional; station names only)</label><p class="muted small">Guest testing does not require a 24Seven.FM account. Leave this clear or choose None; never create an account or enter credentials for this program.</p>' . portalCheckboxes('station_accounts', PORTAL_STATIONS + ['none' => 'None'], listFromJson($tester['station_accounts_json'])) . '<label>Network capabilities</label>' . portalCheckboxes('network_capabilities', PORTAL_NETWORK, listFromJson($tester['network_capabilities_json'])) . '<label>Audio/accessory capabilities</label>' . portalCheckboxes('audio_capabilities', PORTAL_AUDIO, listFromJson($tester['audio_capabilities_json'])) . '<label>Accessibility and alternative input</label>' . portalCheckboxes('accessibility_capabilities', PORTAL_ACCESSIBILITY, listFromJson($tester['accessibility_capabilities_json'])) . '<label>Testing comfort</label>' . portalSelect('testing_comfort', PORTAL_TESTING_COMFORT, $tester['testing_comfort'], 'Choose a testing comfort level') . '<label>Controlled-test preferences</label>' . portalCheckboxes('controlled_actions', PORTAL_CONTROLLED_ACTIONS, listFromJson($tester['controlled_actions_json'])) . '<label>Typical two-week availability</label>' . portalSelect('testing_availability', PORTAL_AVAILABILITY, $tester['testing_availability'], 'Choose availability') . '<label>Assignment notes or prior testing experience</label><textarea name="experience" maxlength="1200">' . e($tester['experience']) . '</textarea><button class="button" type="submit">Finish Profile &amp; Continue to Install</button></form>' . $optInAction . $smokeTestAction . '</section>'
        . '<section class="card"><p class="eyebrow">Active assignments</p><h2>Your focused testing work</h2><div class="grid" style="grid-template-columns:1fr">' . $assignmentCards . '</div></section></div>'
        . '<section class="card"><p class="eyebrow">Task report</p><h2>Submit feedback for a focused task</h2><p class="muted">Include what you tried, expected, and observed. Do not include passwords, credentials, private messages, session information, or private screenshots.</p><form method="post"><input type="hidden" name="action" value="submit_feedback"><input type="hidden" name="csrf" value="' . e(csrf()) . '"><label>Assigned task</label><select name="assignment_id" required><option value="">Choose an assigned task</option>' . implode('', array_map(static fn (array $assignment): string => '<option value="' . (int) $assignment['id'] . '">' . e($assignment['task_id'] . ' — ' . ucwords(str_replace('_', ' ', $assignment['task_status']))) . '</option>', $assignments)) . '</select><label>Feedback category</label><select name="category" required>' . implode('', array_map(static fn (string $key, string $label): string => '<option value="' . e($key) . '">' . e($label) . '</option>', array_keys(FEEDBACK_CATEGORIES), FEEDBACK_CATEGORIES)) . '</select><label>Outcome</label><select name="outcome" required><option value="pass">Pass / worked as expected</option><option value="issue">Issue found</option><option value="blocked">Blocked</option><option value="note">Usability note</option></select><label>Short title</label><input name="subject" maxlength="' . PORTAL_MAX_FEEDBACK_SUBJECT . '" required><label>Steps and result</label><textarea name="details" maxlength="' . PORTAL_MAX_FEEDBACK_DETAILS . '" required></textarea><button class="button" type="submit">Submit task report</button></form>' . ($reports === [] ? '' : '<h3 style="margin-top:24px">Recent reports</h3>' . implode('', array_map(static fn (array $report): string => '<p class="small"><span class="pill">' . e($report['outcome']) . '</span> ' . e(FEEDBACK_CATEGORIES[$report['category']] ?? FEEDBACK_CATEGORIES['other']) . ' · ' . e($report['subject']) . ' <span class="muted">' . e($report['created_at']) . '</span></p>', $reports))) . '</section>' . portalChatPanel($database, $tester, $coordinatorName) . $privacyAction;
    if ($adminPreview) {
        $content = '<p class="notice admin-preview"><strong>Read-only coordinator preview.</strong> This is what the tester sees. Profile, opt-in, and report controls are disabled here; no tester session or data is changed. <a href="/private-tester-queue.php?tester=' . (int) $tester['id'] . '">Return to the coordinator workspace</a>.</p>' . str_replace(['<form method="post">', '</form>'], ['<form method="post"><fieldset disabled>', '</fieldset></form>'], $content);
    }
    portalPage($adminPreview ? 'Tester portal preview' : 'My tester portal', $content);
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
