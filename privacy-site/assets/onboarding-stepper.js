(() => {
  const profileForm = document.querySelector('form input[name="action"][value="save_profile"]')?.form;
  if (!profileForm) return;

  const profileCard = profileForm.closest('.card');
  const dashboardCard = profileCard?.parentElement?.querySelector(':scope > .card:last-child');
  const optInCard = [...profileCard.querySelectorAll(':scope > .task')].find((card) => card.textContent.includes('Google Play opt-in'));
  const smokeCard = [...profileCard.querySelectorAll(':scope > .task')].find((card) => card.textContent.includes('Initial smoke test'));
  if (!profileCard) return;

  const intake = document.createElement('section');
  const device = document.createElement('section');
  intake.className = 'portal-card';
  device.className = 'portal-card';
  intake.innerHTML = '<p class="eyebrow">Step 1</p><h2>Intake profile</h2><p class="muted">Tell us how you would like to test. Fields marked * are required.</p>';
  device.innerHTML = '<p class="eyebrow">Step 2</p><h2>Device and coverage</h2><p class="muted">Choose the setup and testing coverage you can realistically confirm.</p>';

  const intakeLabels = new Set(['Name', 'Country or region', 'Primary station', 'Other familiar stations', 'Testing interests', 'Existing station access (optional; station names only)', 'Assignment notes or prior testing experience']);
  const deviceLabels = new Set(['Current device', 'Android version', 'Device form factor', 'Other devices or configuration notes', 'Network capabilities', 'Audio/accessory capabilities', 'Accessibility and alternative input', 'Testing comfort', 'Controlled-test preferences', 'Typical two-week availability']);
  const saveButton = profileForm.querySelector('button[type="submit"]');
  let destination = intake;
  for (const child of [...profileForm.children]) {
    if (child === saveButton) continue;
    if (child.tagName === 'LABEL') {
      const label = child.textContent.replace(/\s*\*\s*(required)?\s*$/i, '').trim();
      if (intakeLabels.has(label)) destination = intake;
      if (deviceLabels.has(label)) destination = device;
    }
    destination.append(child);
  }
  if (saveButton) device.append(saveButton);
  profileForm.append(intake, device);

  const stages = [
    { id: 'intake', label: 'Intake profile', panel: intake },
    { id: 'device', label: 'Device', panel: device },
    { id: 'optin', label: 'Play Opt-In', panel: optInCard },
    { id: 'smoke', label: 'First Use Smoke Test', panel: smokeCard },
    { id: 'assignment', label: 'Dashboard / Active assignment', panel: dashboardCard },
  ].filter((stage) => stage.panel);
  if (stages.length === 0) return;

  const rail = document.createElement('nav');
  rail.className = 'onboarding-pills';
  rail.setAttribute('aria-label', 'Tester onboarding stages');
  const title = document.createElement('p');
  title.className = 'eyebrow';
  title.textContent = 'Your onboarding path';
  profileCard.parentElement.insertBefore(title, profileCard);
  profileCard.parentElement.insertBefore(rail, profileCard);

  const select = (stage) => {
    stages.forEach((candidate) => {
      const selected = candidate === stage;
      candidate.panel.hidden = !selected;
      candidate.button.classList.toggle('secondary', !selected);
      candidate.button.setAttribute('aria-current', selected ? 'step' : 'false');
    });
    stage.panel.scrollIntoView({ block: 'start', behavior: 'smooth' });
  };
  stages.forEach((stage) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'button secondary';
    button.textContent = stage.label;
    button.addEventListener('click', () => select(stage));
    stage.button = button;
    rail.append(button);
  });

  const savedProfile = new URLSearchParams(window.location.search).get('notice')?.includes('saved');
  const initial = savedProfile ? stages.find((stage) => stage.id === 'optin') : stages.find((stage) => stage.id === 'intake');
  select(initial ?? stages[0]);
})();
