(() => {
  const root = document.querySelector("[data-live-chat]");
  if (!root) return;
  const list = root.querySelector("[data-chat-messages]");
  const detach = root.querySelector("[data-chat-detach]");
  const csrf = root.querySelector("input[name=csrf]")?.value;
  const testerId = root.querySelector("input[name=tester_id]")?.value;
  let lastId = Math.max(0, ...[...list.querySelectorAll("[data-chat-message-id]")].map((item) => Number(item.dataset.chatMessageId) || 0));
  let fallbackTimer = null;

  const append = (message) => {
    lastId = Math.max(lastId, Number(message.id) || 0);
    list.querySelector("[data-chat-empty]")?.remove();
    const item = document.createElement("article");
    item.className = `chat-message ${message.role === "tester" ? "mine" : "coordinator"}`;
    item.dataset.chatMessageId = String(message.id);
    const sender = document.createElement("strong");
    sender.textContent = message.sender;
    const body = document.createElement("div");
    body.textContent = message.body;
    const timestamp = document.createElement("small");
    timestamp.textContent = message.createdAt;
    item.append(sender, body, timestamp);
    if (csrf) {
      const remove = document.createElement("form");
      remove.method = "post";
      [["action", "delete_chat_message"], ["csrf", csrf], ["message_id", String(message.id)], ...(testerId ? [["tester_id", testerId]] : [])].forEach(([name, value]) => {
        const field = document.createElement("input");
        field.type = "hidden";
        field.name = name;
        field.value = value;
        remove.append(field);
      });
      const button = document.createElement("button");
      button.type = "submit";
      button.className = "secondary";
      button.textContent = "Delete from my view";
      remove.append(button);
      item.append(remove);
    }
    list.append(item);
    list.scrollTop = list.scrollHeight;
  };

  const consume = (messages) => messages.filter((message) => Number(message.id) > lastId).forEach(append);
  const endpoint = (base) => `${base}${base.includes("?") ? "&" : "?"}after=${encodeURIComponent(lastId)}`;
  const poll = async () => {
    try {
      const response = await fetch(endpoint(root.dataset.chatPoll), { credentials: "same-origin", headers: { Accept: "application/json" } });
      if (!response.ok) throw new Error("poll failed");
      consume((await response.json()).messages || []);
    } catch {
      // A later interval retries without surfacing transport details to either participant.
    }
  };
  const startFallback = () => {
    if (fallbackTimer) return;
    poll();
    fallbackTimer = window.setInterval(poll, 6000);
  };

  if (window.EventSource && root.dataset.chatStream) {
    const stream = new EventSource(endpoint(root.dataset.chatStream));
    stream.addEventListener("messages", (event) => {
      try { consume(JSON.parse(event.data)); } catch { /* ignore malformed transient event data */ }
    });
    stream.onerror = () => {
      stream.close();
      startFallback();
    };
  } else {
    startFallback();
  }

  detach?.addEventListener("click", () => {
    const url = new URL(window.location.href);
    url.searchParams.set("chat_popout", "1");
    window.open(url, "twentyfourseven_onboarding_chat", "popup=yes,width=520,height=720,resizable=yes,scrollbars=yes");
  });
})();
