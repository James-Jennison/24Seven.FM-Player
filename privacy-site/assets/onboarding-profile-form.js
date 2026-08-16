(() => {
  const form = document.querySelector('form input[name="action"][value="save_profile"]')?.form;
  if (!form) return;

  const storageKey = 'twentyfourseven-tester-profile-draft-v1';
  const requiredFields = [
    'display_name', 'device', 'android_version', 'primary_station', 'device_form_factor',
    'network_capabilities', 'audio_capabilities', 'accessibility_capabilities',
    'testing_comfort', 'controlled_actions', 'testing_availability',
  ];
  const missingFields = new URLSearchParams(window.location.search).get('profile_missing')?.split(',') ?? [];

  const readDraft = () => {
    try {
      const value = window.sessionStorage.getItem(storageKey);
      return value ? JSON.parse(value) : {};
    } catch {
      return {};
    }
  };

  const writeDraft = () => {
    const values = {};
    for (const control of form.elements) {
      if (!(control instanceof HTMLElement) || !control.getAttribute('name')) continue;
      const name = control.getAttribute('name');
      if (name === 'csrf' || name === 'action') continue;
      if (control instanceof HTMLInputElement && control.type === 'checkbox') {
        values[name] ??= [];
        if (control.checked) values[name].push(control.value);
      } else if (control instanceof HTMLInputElement || control instanceof HTMLSelectElement || control instanceof HTMLTextAreaElement) {
        values[name] = control.value;
      }
    }
    window.sessionStorage.setItem(storageKey, JSON.stringify(values));
  };

  const restoreDraft = () => {
    const draft = readDraft();
    for (const [name, value] of Object.entries(draft)) {
      const controls = form.querySelectorAll(`[name="${CSS.escape(name)}"]`);
      controls.forEach((control) => {
        if (control instanceof HTMLInputElement && control.type === 'checkbox') {
          control.checked = (Array.isArray(value) ? value : [value]).includes(control.value);
        } else if (typeof value === 'string') {
          control.value = value;
        }
      });
    }
  };

  const markRequiredFields = () => {
    const intro = document.createElement('p');
    intro.className = 'muted small';
    intro.innerHTML = '<strong><span aria-hidden="true">*</span> Required.</strong> All other profile fields are optional.';
    form.insertBefore(intro, form.firstChild.nextSibling);

    for (const name of requiredFields) {
      const group = form.querySelector(`[name="${name}[]"]`)?.closest('.choices');
      const control = group ?? form.querySelector(`[name="${name}"]`);
      if (!control) continue;
      const label = group ? group.previousElementSibling : control.previousElementSibling;
      const id = `profile-${name}`;
      control.id = id;
      if (label?.tagName === 'LABEL') {
        label.id = `${id}-label`;
        label.insertAdjacentHTML('beforeend', ' <strong aria-hidden="true">*</strong><span class="visually-hidden"> (required)</span>');
      }
      if (group) {
        group.setAttribute('role', 'group');
        group.setAttribute('aria-labelledby', `${id}-label`);
        group.setAttribute('aria-required', 'true');
      }
      if (missingFields.includes(name)) {
        control.style.borderColor = '#ffcb6b';
        control.style.boxShadow = '0 0 0 2px #ffcb6b33';
      }
    }
  };

  if (new URLSearchParams(window.location.search).get('notice')?.includes('saved')) {
    window.sessionStorage.removeItem(storageKey);
  } else {
    restoreDraft();
  }
  markRequiredFields();
  form.addEventListener('submit', writeDraft);

  const firstMissing = missingFields.map((name) => document.getElementById(`profile-${name}`)).find(Boolean);
  if (firstMissing) {
    firstMissing.scrollIntoView({ block: 'center' });
    firstMissing.focus({ preventScroll: true });
  }
})();
