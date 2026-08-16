#!/usr/bin/env node

import { readFile } from "node:fs/promises";

const [queue, portal, chatClient, workspaceStyles] = await Promise.all([
  readFile(new URL("../privacy-site/private-tester-queue.php", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/tester-portal.php", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/assets/onboarding-live-chat.js", import.meta.url), "utf8"),
  readFile(new URL("../privacy-site/assets/onboarding-portal.css", import.meta.url), "utf8"),
]);

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

assert(queue.includes('?email=1') && queue.includes('?live_chat=1'), "Coordinator Operations must route Email and Live Chat as separate workspaces.");
assert(queue.includes('/assets/onboarding-portal.css') && queue.includes('global-rail'), "Coordinator routes must use the approved workspace rail and shared visual treatment.");
assert(queue.includes('operations-workspace') && queue.includes('Five-stage evidence path') && queue.includes('boardCounts'), "Coordinator Operations must preserve the approved compact five-stage workspace hierarchy.");
assert(queue.includes('?email=1&compose_for='), "Profile-detail requests must land in the dedicated Email workspace with the recipient preserved.");
assert(portal.includes('/assets/onboarding-portal.css') && portal.includes('portal-preview-shell'), "Tester routes must use the same approved workspace visual treatment.");
assert(workspaceStyles.includes('.global-rail') && workspaceStyles.includes('.window') && workspaceStyles.includes('@media(max-width:720px)'), "The promoted workspace styling must preserve its rail, window, and responsive behavior.");
assert(queue.includes('Draft preview') && queue.includes('exact version is preserved in the sent archive'), "Coordinator Email must provide a resolved review step and archived delivery evidence.");
assert(queue.includes("renderPage('Live Chat — ' . (string) $selected['display_name']"), "Coordinator detached chat must use the selected tester name in its document title.");
assert(portal.includes("portalPage('Live Chat — ' . $coordinatorName"), "Tester detached chat must use the assigned coordinator name in its document title.");
assert(queue.includes('Tester portal access never exposes this coordinator workspace.'), "Coordinator Live Chat must state the tester-portal isolation boundary.");
assert(queue.includes('class="conversation-item') && queue.includes('</strong><span>'), "Coordinator Live Chat must render tester names and device details as separate lines.");
assert(portal.includes('Only you and ') && portal.includes('can view this tester-program conversation.'), "Tester Live Chat must state its per-tester conversation boundary.");
assert(chatClient.includes('url.searchParams.set("chat_popout", "1")'), "Detached Live Chat must preserve the authenticated same-origin portal route.");
assert(chatClient.includes('window.open(url, "twentyfourseven_onboarding_chat"'), "Detached Live Chat must open in the dedicated pop-out window.");
assert(chatClient.includes('credentials: "same-origin"'), "Live Chat polling must retain the authenticated same-origin session boundary.");
assert(chatClient.includes('new EventSource(endpoint(root.dataset.chatStream))') && chatClient.includes('window.setInterval(poll, 6000)'), "Live Chat must retain SSE delivery with polling fallback.");
assert(queue.includes('function chatPurgeExpired(PDO $database): void'), "The Phase 2 retention smoke path must exercise the portal purge lifecycle.");

console.log("Onboarding Phase 2 workspace and pop-out contract: valid.");
