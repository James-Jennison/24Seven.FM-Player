const popoutMessages = document.querySelector("[data-popout-messages]");
const testerName = window.sessionStorage.getItem("onboarding-live-chat-tester") || "Sample tester A";

document.title = `Live Chat — ${testerName}`;

document.querySelectorAll("[data-popout-tester]").forEach((element) => {
  element.textContent = testerName;
});

document.querySelector("[data-popout-form]").addEventListener("submit", (event) => {
  event.preventDefault();
  const input = event.currentTarget.querySelector("input");
  const text = input.value.trim();
  if (!text) return;
  const message = document.createElement("article");
  message.className = "message admin";
  message.innerHTML = "<strong>Coordinator</strong><div></div><small>Just now · local only</small>";
  message.querySelector("div").textContent = text;
  popoutMessages.append(message);
  input.value = "";
  popoutMessages.scrollTop = popoutMessages.scrollHeight;
});
