(() => {
  function closeChecklist(dialog) {
    if (typeof dialog.close === 'function') {
      dialog.close();
    } else {
      dialog.removeAttribute('open');
    }
  }

  document.querySelectorAll('[data-pt-checklist-open]').forEach((trigger) => {
    const dialogId = trigger.getAttribute('data-pt-checklist-open');
    const dialog = dialogId ? document.getElementById(dialogId) : null;
    if (!dialog) return;

    trigger.addEventListener('click', () => {
      if (dialog.open) return;
      if (typeof dialog.showModal === 'function') {
        dialog.showModal();
      } else {
        dialog.setAttribute('open', '');
      }
    });

    dialog.addEventListener('click', (event) => {
      if (event.target === dialog) closeChecklist(dialog);
    });

    dialog.querySelectorAll('[data-pt-checklist-close]').forEach((closeButton) => {
      closeButton.addEventListener('click', () => closeChecklist(dialog));
    });
  });
})();
