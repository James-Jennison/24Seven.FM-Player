(() => {
  function closeChecklist(dialog) {
    if (typeof dialog.close === 'function') {
      dialog.close();
    } else {
      dialog.removeAttribute('open');
    }
  }

  function openChecklist(trigger) {
    const dialogId = trigger.getAttribute('data-pt-checklist-open');
    const dialog = dialogId ? document.getElementById(dialogId) : null;
    if (!dialog || dialog.open) return;

    if (typeof dialog.showModal === 'function') {
      dialog.showModal();
    } else {
      dialog.setAttribute('open', '');
    }
  }

  document.addEventListener('click', (event) => {
    const element = event.target instanceof Element ? event.target : null;
    const trigger = element?.closest('[data-pt-checklist-open]');
    if (trigger) {
      openChecklist(trigger);
      return;
    }

    const closeButton = element?.closest('[data-pt-checklist-close]');
    if (closeButton) {
      const dialog = closeButton.closest('dialog');
      if (dialog) closeChecklist(dialog);
      return;
    }

    if (element instanceof HTMLDialogElement) closeChecklist(element);
  });
})();
