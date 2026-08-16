const testers = [
  { id: "TST-101", stage: "Applied", coverage: "Android 15 · audio", source: "Community", age: "2h", name: "Sample tester A", completion: 20, devices: 5, accessibility: 2, tasks: 0 },
  { id: "TST-102", stage: "Profile & Device", coverage: "Android 14 · tablet", source: "Direct", age: "4h", name: "Sample tester B", completion: 40, devices: 3, accessibility: 1, tasks: 0 },
  { id: "TST-103", stage: "Play Opt-In", coverage: "Android 13 · auto", source: "Partner", age: "1d", name: "Sample tester C", completion: 60, devices: 4, accessibility: 3, tasks: 0 },
  { id: "TST-104", stage: "First-Use Smoke Test", coverage: "Android 12 · Bluetooth", source: "Website", age: "1d", name: "Sample tester D", completion: 80, devices: 2, accessibility: 1, tasks: 0 },
  { id: "TST-105", stage: "Active Assignment", coverage: "Android 15 · foldable", source: "Community", age: "2d", name: "Sample tester E", completion: 100, devices: 6, accessibility: 2, tasks: 2 },
  { id: "TST-106", stage: "Profile & Device", coverage: "Android 14 · audio", source: "Campaign", age: "2d", name: "Sample tester F", completion: 40, devices: 3, accessibility: 1, tasks: 0 },
];

let selectedTester = testers[0];
let activeFilter = "all";
let messages = [
  { author: "Sample tester A", text: "Hello — I wanted to confirm the next step for my application.", time: "10:16 AM", role: "applicant" },
  { author: "Coordinator", text: "Thanks for reaching out. Your profile and device information are being reviewed.", time: "10:20 AM", role: "admin" },
  { author: "Applicant", text: "Great, thank you. Please let me know if you need anything else from me.", time: "10:21 AM", role: "applicant" },
];

const queue = document.querySelector("[data-queue]");
const record = document.querySelector("[data-record]");
const board = document.querySelector("[data-board]");
const messageList = document.querySelector("[data-messages]");
const rowTemplate = document.querySelector("#queue-row-template");

function stageClass(stage) { return stage.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, ""); }

function renderQueue() {
  const search = document.querySelector("[data-search]").value.trim().toLowerCase();
  queue.replaceChildren();
  testers
    .filter((tester) => activeFilter === "all" || tester.stage.toLowerCase() === activeFilter)
    .filter((tester) => `${tester.id} ${tester.coverage} ${tester.source}`.toLowerCase().includes(search))
    .forEach((tester) => {
      const item = rowTemplate.content.firstElementChild.cloneNode(true);
      item.querySelector('[data-field="id"]').textContent = tester.id;
      const stage = item.querySelector('[data-field="stage"]');
      stage.textContent = tester.stage;
      stage.className = `stage ${stageClass(tester.stage)}`;
      item.querySelector('[data-field="coverage"]').textContent = tester.coverage;
      item.querySelector('[data-field="source"]').textContent = tester.source;
      item.querySelector('[data-field="age"]').textContent = tester.age;
      item.classList.toggle("selected", tester.id === selectedTester.id);
      item.addEventListener("click", () => { selectedTester = tester; renderQueue(); renderRecord(); });
      queue.append(item);
    });
}

function renderRecord() {
  record.innerHTML = `
    <div class="record-top">
      <div><p class="eyebrow">${selectedTester.id}</p><h3 class="record-title">${selectedTester.name}</h3><p class="record-meta">Local sample profile · no production data</p></div>
      <div class="record-stage"><span>Current stage</span><b class="stage ${stageClass(selectedTester.stage)}">${selectedTester.stage}</b></div>
    </div>
    <section class="completion"><div class="completion-ring">${selectedTester.completion}%</div><div><strong>Five-stage evidence</strong><ul class="checklist"><li>Applied</li><li>Profile &amp; Device</li><li>Play Opt-In</li><li>First-Use Smoke Test</li><li class="pending">Active Assignment</li></ul></div></section>
    <section class="record-grid"><div class="capability"><span>Qualified devices</span><strong>${selectedTester.devices}</strong></div><div class="capability"><span>Accessibility paths</span><strong>${selectedTester.accessibility}</strong></div><div class="capability"><span>Focused tasks</span><strong>${selectedTester.tasks}</strong></div></section>
    <div class="record-footer"><span class="record-boundary">Tester portal is a separate authenticated experience.</span><button class="button primary" type="button" data-action="open-live-chat">Open live chat</button></div>`;
}

function renderBoard() {
  const columns = [
    ["Applied", "cyan", ["Consent record", "Application review"]],
    ["Profile & Device", "violet", ["Coverage details", "Device verification"]],
    ["Play Opt-In", "amber", ["Opt-in self-confirmation"]],
    ["First-Use Smoke Test", "lime", ["Playback smoke check"]],
    ["Active Assignment", "cyan", ["Audio route study", "Accessibility review"]],
  ];
  board.innerHTML = columns.map(([title, color, tasks], index) => `<section class="board-column"><div class="column-head"><span style="background:var(--${color})">${tasks.length}</span>${title}<small>${index === 4 ? "Current work" : "Evidence"}</small></div>${tasks.map((task, taskIndex) => `<article class="task-card" style="border-left-color:var(--${color})"><strong>${index === 4 ? "TT" : "Step"}-${String(index * 4 + taskIndex + 1).padStart(2, "0")}</strong><span>${task}</span><small>${index === 4 ? "Focused testing work" : "Recorded when confirmed"}</small></article>`).join("")}</section>`).join("");
}

function renderMessages() {
  messageList.replaceChildren();
  messages.forEach((message) => {
    const item = document.createElement("article");
    item.className = `message ${message.role === "admin" ? "admin" : ""}`;
    item.innerHTML = `<strong>${message.author}</strong><div>${message.text}</div><small>${message.time} ${message.role === "admin" ? "· delivered" : ""}</small>`;
    messageList.append(item);
  });
  messageList.scrollTop = messageList.scrollHeight;
}

function renderChatContext() {
  document.querySelectorAll("[data-chat-tester]").forEach((element) => { element.textContent = selectedTester.name; });
  document.querySelectorAll("[data-chat-context]").forEach((element) => {
    element.textContent = `${selectedTester.stage} · private tester-program conversation`;
  });
  document.querySelectorAll("[data-chat-target]").forEach((element) => {
    element.classList.toggle("active", element.dataset.chatTarget === selectedTester.id);
  });
}

function activateView(view) {
  document.querySelectorAll("[data-view-panel]").forEach((panel) => panel.classList.toggle("active", panel.dataset.viewPanel === view));
  document.querySelectorAll("[data-view]").forEach((button) => { const active = button.dataset.view === view; button.classList.toggle("active", active); button.setAttribute("aria-pressed", String(active)); });
}

function openLiveChat() { renderChatContext(); activateView("chat"); }

function openEmail() {
  activateView("email");
  document.querySelector("[data-email-subject]").focus();
}

document.querySelectorAll("[data-view]").forEach((button) => button.addEventListener("click", () => activateView(button.dataset.view)));
document.querySelectorAll("[data-filter]").forEach((button) => button.addEventListener("click", () => { activeFilter = button.dataset.filter; document.querySelectorAll("[data-filter]").forEach((item) => item.classList.toggle("active", item === button)); renderQueue(); }));
document.querySelectorAll("[data-chat-target]").forEach((button) => button.addEventListener("click", () => {
  const tester = testers.find((candidate) => candidate.id === button.dataset.chatTarget);
  if (!tester) return;
  selectedTester = tester;
  renderQueue();
  renderRecord();
  renderChatContext();
}));
document.querySelector("[data-search]").addEventListener("input", renderQueue);
document.addEventListener("click", (event) => {
  const action = event.target.closest("[data-action]")?.dataset.action;
  if (!action) return;
  if (action === "open-live-chat") openLiveChat();
  if (action === "filter-active") { activateView("operations"); activeFilter = "active-assignment"; document.querySelectorAll("[data-filter]").forEach((item) => item.classList.toggle("active", item.dataset.filter === "active-assignment")); renderQueue(); }
  if (action === "open-email") openEmail();
  if (action === "detach-chat") {
    window.sessionStorage.setItem("onboarding-live-chat-tester", selectedTester.name);
    const popup = window.open("./chat-popout.html", "twentyfourseven_live_chat", "popup=yes,width=520,height=720,resizable=yes,scrollbars=yes");
    if (popup) popup.focus();
  }
  if (action === "new-intake") alert("Local prototype: the real intake flow remains in the existing portal.");
  if (action === "save-draft") document.querySelector("[data-email-note]").textContent = "Draft saved locally for this browser session.";
  if (action === "send-email") {
    const subject = document.querySelector("[data-email-subject]").value || "Untitled coordinator email";
    const body = document.querySelector("[data-email-body]").value || "Write a message before reviewing it.";
    document.querySelector("[data-preview-subject]").textContent = subject;
    document.querySelector("[data-preview-body]").textContent = body
      .replaceAll("{{tester_name}}", selectedTester.name)
      .replaceAll("{{onboarding_status}}", selectedTester.stage)
      .replaceAll("{{tester_portal_url}}", "tester-portal.html")
      .replaceAll("{{program_name}}", "24Seven.FM Player Closed Alpha");
    document.querySelector("[data-email-preview]").hidden = false;
    document.querySelector("[data-email-note]").textContent = "Local review preview only — no email has been sent.";
  }
});

document.querySelector("[data-chat-form]").addEventListener("submit", (event) => {
  event.preventDefault();
  const input = event.currentTarget.querySelector("input");
  const text = input.value.trim();
  if (!text) return;
  messages.push({ author: "Coordinator", text, time: "Just now", role: "admin" });
  input.value = "";
  renderMessages();
  document.querySelector("[data-unread-count]").textContent = "0";
});

document.querySelector("[data-template]").addEventListener("change", (event) => {
  const templates = {
    orientation: ["Your 24Seven.FM Player next steps", "Hello {{tester_name}},\n\nYour profile is ready for the next step. Use {{tester_portal_url}} to record your Play opt-in and first-use smoke test when complete.\n\nThanks,"],
    review: ["Profile & device update", "Hello {{tester_name}},\n\nYour current onboarding status is {{onboarding_status}}. Please review the remaining coverage details in {{tester_portal_url}}.\n\nThanks,"],
  };
  const value = templates[event.target.value];
  if (!value) return;
  document.querySelector("[data-email-subject]").value = value[0];
  document.querySelector("[data-email-body]").value = value[1];
});

document.querySelectorAll("[data-mail-variable]").forEach((button) => button.addEventListener("click", () => {
  const field = document.querySelector("[data-email-body]");
  const start = field.selectionStart;
  const end = field.selectionEnd;
  const value = button.dataset.mailVariable || "";
  field.value = `${field.value.slice(0, start)}${value}${field.value.slice(end)}`;
  field.focus();
  field.selectionStart = field.selectionEnd = start + value.length;
}));

renderQueue();
renderRecord();
renderBoard();
renderMessages();
renderChatContext();
