const testerPopoutMessages = document.querySelector("[data-tester-popout-messages]");

document.querySelector("[data-tester-popout-form]").addEventListener("submit", (event) => {
  event.preventDefault();
  const input = event.currentTarget.querySelector("input");
  const text = input.value.trim();
  if (!text) return;
  const message = document.createElement("article");
  message.className = "message";
  message.innerHTML = "<strong>You</strong><div></div><small>Just now · local only</small>";
  message.querySelector("div").textContent = text;
  testerPopoutMessages.append(message);
  input.value = "";
  testerPopoutMessages.scrollTop = testerPopoutMessages.scrollHeight;
});
