const chat = document.querySelector("[data-portal-chat]");
const messages = document.querySelector("[data-portal-messages]");

document.addEventListener("click", (event) => {
  const action = event.target.closest("[data-portal-action]")?.dataset.portalAction;
  if (action === "open-chat") {
    chat.hidden = false;
    chat.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }
  if (action === "close-chat") chat.hidden = true;
  if (action === "detach-chat") {
    const popup = window.open("./tester-chat-popout.html", "twentyfourseven_tester_live_chat", "popup=yes,width=520,height=720,resizable=yes,scrollbars=yes");
    if (popup) popup.focus();
  }
});

document.querySelector("[data-portal-form]").addEventListener("submit", (event) => {
  event.preventDefault();
  const input = event.currentTarget.querySelector("input");
  const text = input.value.trim();
  if (!text) return;
  const message = document.createElement("article");
  message.className = "message";
  message.innerHTML = `<strong>You</strong><div></div><small>Just now</small>`;
  message.querySelector("div").textContent = text;
  messages.append(message);
  input.value = "";
  messages.scrollTop = messages.scrollHeight;
});
