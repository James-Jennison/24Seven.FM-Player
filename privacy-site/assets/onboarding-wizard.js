(() => {
  const reportForm = document.querySelector('form input[name="action"][value="submit_feedback"]')?.form;
  const assignmentSelect = reportForm?.querySelector('select[name="assignment_id"]');
  const ptCaseSelect = reportForm?.querySelector('select[name="pt_case"]');
  const syncReportCases = () => {
    if (!assignmentSelect || !ptCaseSelect) return;
    const assignmentId = assignmentSelect.value;
    let selectedIsVisible = false;
    for (const option of ptCaseSelect.options) {
      const visible = option.value === '' || option.dataset.assignmentId === assignmentId;
      option.hidden = !visible;
      option.disabled = !visible;
      if (visible && option.selected) selectedIsVisible = true;
    }
    if (!selectedIsVisible) ptCaseSelect.value = '';
  };
  assignmentSelect?.addEventListener('change', syncReportCases);
  syncReportCases();

  const form = document.querySelector('form input[name="action"][value="save_profile"]')?.form;
  if (!form) return;
  const workspaceView = document.querySelector('[data-portal-view]')?.dataset.portalView;
  if (workspaceView !== 'onboarding') return;

  const profileCard = form.closest('.card');
  const twoColumn = profileCard?.parentElement;
  if (!profileCard || !twoColumn) return;
  const portalRoot = document.querySelector('#tester-portal');

  const taskCards = [...profileCard.querySelectorAll(':scope > .task')];
  const optInCard = taskCards.find((card) => card.textContent.includes('Google Play opt-in'));
  const smokeCard = taskCards.find((card) => card.textContent.includes('Initial smoke test'));
  const saveButton = form.querySelector('button[type="submit"]');
  const requiredNames = [
    'display_name', 'device', 'android_version', 'primary_station', 'device_form_factor',
    'network_capabilities', 'audio_capabilities', 'accessibility_capabilities',
    'testing_comfort', 'controlled_actions', 'testing_availability',
  ];
  const profileComplete = requiredNames.every((name) => {
    const controls = [...form.querySelectorAll(`[name="${name}"], [name="${name}[]"]`)];
    if (controls.length === 0) return false;
    if (controls[0] instanceof HTMLInputElement && controls[0].type === 'checkbox') {
      return controls.some((control) => control.checked);
    }
    return controls[0].value.trim() !== '';
  });
  if (profileComplete && !optInCard && !smokeCard) return;

  const overlay = document.createElement('div');
  overlay.className = 'onboarding-wizard-overlay';
  overlay.setAttribute('role', 'dialog');
  overlay.setAttribute('aria-modal', 'true');
  overlay.setAttribute('aria-label', 'Complete your tester onboarding');
  overlay.tabIndex = -1;

  const wizard = document.createElement('section');
  wizard.className = 'onboarding-wizard';
  const heading = document.createElement('header');
  heading.className = 'onboarding-wizard-header';
  heading.innerHTML = '<p class="eyebrow">24Seven.FM Player · Closed Alpha</p><h2>Finish Profile & Device to start testing</h2><p class="muted">Complete these two screens, then we will take you directly to Google Play to install the Player and confirm the short first-use check.</p>';
  const rail = document.createElement('nav');
  rail.className = 'onboarding-wizard-rail';
  rail.setAttribute('aria-label', 'Onboarding progress');
  const body = document.createElement('div');
  body.className = 'onboarding-wizard-body';
  wizard.append(heading, rail, body);
  overlay.append(wizard);
  document.body.append(overlay);

  const draft = document.createElement('section');
  const coverage = document.createElement('section');
  const formContent = form.querySelector(':scope > fieldset') ?? form;
  const draftFields = document.createElement('fieldset');
  const coverageFields = document.createElement('fieldset');
  const readOnlyPreview = formContent instanceof HTMLFieldSetElement && formContent.disabled;
  if (readOnlyPreview) {
    draftFields.disabled = true;
    coverageFields.disabled = true;
  }
  draft.className = 'onboarding-wizard-panel';
  coverage.className = 'onboarding-wizard-panel';
  draft.innerHTML = '<p class="eyebrow">Step 1 of 5</p><h3>About you</h3><p class="muted">Tell us how you would like to test. Fields marked * are required.</p>';
  coverage.innerHTML = '<p class="eyebrow">Step 2 of 5</p><h3>Your device and coverage</h3><p class="muted">Choose the setup and testing coverage you can realistically confirm.</p>';

  const intakeLabels = new Set([
    'Name', 'Country or region', 'Primary station', 'Other familiar stations',
    'Testing interests', 'Existing station access (optional; station names only)',
    'Assignment notes or prior testing experience',
  ]);
  const saveControls = new Set([saveButton]);
  let destination = draftFields;
  for (const child of [...formContent.children]) {
    if (saveControls.has(child)) continue;
    if (child.tagName === 'LABEL') {
      const label = child.textContent.replace(/\s*\*\s*(?:\(?required\)?)?\s*$/i, '').trim();
      destination = intakeLabels.has(label) ? draftFields : coverageFields;
    }
    destination.append(child);
  }
  if (saveButton) {
    saveButton.textContent = 'Finish Profile & Continue to Install →';
    coverageFields.append(saveButton);
  }
  if (formContent !== form) formContent.remove();
  draft.append(draftFields);
  coverage.append(coverageFields);
  form.append(draft, coverage);

  const stagePanel = (id, label, element, complete = false) => ({ id, label, element, complete });
  const stages = [
    stagePanel('intake', 'Intake profile', form),
    stagePanel('device', 'Device', form),
    stagePanel('optin', 'Play Opt-In', optInCard),
    stagePanel('smoke', 'First-Use Smoke Test', smokeCard),
    stagePanel('dashboard', 'Dashboard', null, profileComplete && !optInCard && !smokeCard),
  ];
  let profilePage = 0;

  const currentStage = () => {
    if (!profileComplete) return profilePage === 0 ? 'intake' : 'device';
    if (optInCard) return 'optin';
    if (smokeCard) return 'smoke';
    return 'dashboard';
  };

  const render = () => {
    const active = currentStage();
    rail.replaceChildren();
    stages.forEach((stage, index) => {
      const pill = document.createElement('span');
      pill.className = 'onboarding-wizard-pill';
      pill.textContent = `${index + 1}. ${stage.label}`;
      if (stage.id === active) pill.classList.add('active');
      if (stage.complete || (active === 'smoke' && stage.id === 'optin')) pill.classList.add('complete');
      rail.append(pill);
    });
    body.replaceChildren();
    if (active === 'dashboard') {
      overlay.remove();
      return;
    }
    if (active === 'intake' || active === 'device') {
      draft.hidden = profilePage !== 0;
      coverage.hidden = profilePage !== 1;
      body.append(form);
      const controls = document.createElement('div');
      controls.className = 'onboarding-wizard-actions';
      if (profilePage > 0) {
        const back = document.createElement('button');
        back.type = 'button';
        back.className = 'button secondary';
        back.textContent = '← Back';
        back.addEventListener('click', () => { profilePage = 0; render(); });
        controls.append(back);
      }
      if (profilePage === 0) {
        const next = document.createElement('button');
        next.type = 'button';
        next.className = 'button';
        next.textContent = 'Next →';
        next.addEventListener('click', () => { profilePage = 1; render(); });
        controls.append(next);
      }
      body.append(controls);
      return;
    }
    const card = active === 'optin' ? optInCard : smokeCard;
    if (!card) return;
    body.append(card);
    const submit = card.querySelector('button[type="submit"]');
    if (submit) submit.textContent = active === 'optin' ? 'Record opt-in and continue →' : 'Complete onboarding →';
  };

  profileCard.hidden = true;
  twoColumn.classList.add('onboarding-dashboard-locked');
  if (portalRoot) portalRoot.inert = true;
  render();
  if (overlay.isConnected) overlay.focus();
})();
