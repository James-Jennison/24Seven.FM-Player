(function () {
  'use strict';

  const root = document.documentElement;
  root.classList.add('js');

  const header = document.querySelector('[data-site-header]');
  const navigation = document.querySelector('#site-navigation');
  const navToggle = document.querySelector('.nav-toggle');
  const themeToggle = document.querySelector('.theme-toggle');
  const themeLabel = document.querySelector('.theme-toggle-label');
  const themeColor = document.querySelector('meta[name="theme-color"]');
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const lightScheme = window.matchMedia('(prefers-color-scheme: light)');
  const themeKey = 'project-theme';

  function slugify(value) {
    return value
      .toLocaleLowerCase()
      .normalize('NFKD')
      .replace(/[̀-ͯ]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '') || 'section';
  }

  function uniqueId(value) {
    const base = slugify(value);
    let candidate = base;
    let suffix = 2;
    while (document.getElementById(candidate)) {
      candidate = base + '-' + suffix;
      suffix += 1;
    }
    return candidate;
  }

  // Theme: the visitor's device setting decides until they use the toggle.
  function savedTheme() {
    try {
      const value = localStorage.getItem(themeKey);
      return value === 'light' || value === 'dark' ? value : null;
    } catch (error) {
      return null;
    }
  }

  function setTheme(theme, persist) {
    root.dataset.theme = theme;
    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    if (themeLabel) themeLabel.textContent = nextTheme === 'light' ? 'Light' : 'Dark';
    if (themeToggle) themeToggle.setAttribute('aria-label', 'Switch to ' + nextTheme + ' mode');
    if (themeColor) themeColor.setAttribute('content', theme === 'dark' ? '#09070f' : '#f4efe7');
    if (persist) {
      try {
        localStorage.setItem(themeKey, theme);
      } catch (error) {
        // Theme persistence is optional.
      }
    }
  }

  setTheme(root.dataset.theme === 'light' ? 'light' : 'dark', false);
  if (themeToggle) {
    themeToggle.hidden = false;
    themeToggle.addEventListener('click', function () {
      setTheme(root.dataset.theme === 'dark' ? 'light' : 'dark', true);
    });
  }
  lightScheme.addEventListener('change', function (event) {
    if (!savedTheme()) setTheme(event.matches ? 'light' : 'dark', false);
  });

  // Menu on narrow screens.
  function closeNavigation() {
    if (!header || !navToggle) return;
    delete header.dataset.navOpen;
    navToggle.setAttribute('aria-expanded', 'false');
    navToggle.setAttribute('aria-label', 'Open menu');
  }

  if (header && navToggle && navigation) {
    navToggle.hidden = false;
    navToggle.addEventListener('click', function () {
      if (header.dataset.navOpen !== undefined) {
        closeNavigation();
        return;
      }
      header.dataset.navOpen = '';
      navToggle.setAttribute('aria-expanded', 'true');
      navToggle.setAttribute('aria-label', 'Close menu');
    });
    navigation.addEventListener('click', function (event) {
      if (event.target.closest('a')) closeNavigation();
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && header.dataset.navOpen !== undefined) {
        closeNavigation();
        navToggle.focus();
      }
    });
  }

  // Station doors: an accordion from 1240 pixels wide, plain rows below that.
  // project.css uses the same width.
  function enhanceDoors() {
    const container = document.querySelector('[data-doors]');
    if (!container) return;
    const doors = Array.from(container.querySelectorAll('[data-door]'));
    const wide = window.matchMedia('(min-width: 1240px)');

    function openDoor(door, moveFocus) {
      doors.forEach(function (item) {
        const isOpen = item === door;
        item.classList.toggle('is-open', isOpen);
        const button = item.querySelector('.door-open');
        if (button) button.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
      });
      if (moveFocus) {
        const link = door.querySelector('.door-link');
        if (link) link.focus({ preventScroll: true });
      }
    }

    function enable() {
      container.classList.add('is-accordion');
      doors.forEach(function (door) {
        if (door.querySelector('.door-open')) return;
        const name = door.querySelector('.door-name');
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'door-open';
        button.setAttribute('aria-label', 'Show ' + (name ? name.textContent.trim() : 'station'));
        button.setAttribute('aria-expanded', 'false');
        button.addEventListener('click', function () { openDoor(door, true); });
        door.prepend(button);
      });
      openDoor(doors.find(function (door) { return door.classList.contains('is-open'); }) || doors[0], false);
    }

    function disable() {
      container.classList.remove('is-accordion');
      doors.forEach(function (door) {
        const button = door.querySelector('.door-open');
        if (button) button.remove();
      });
    }

    function apply() {
      if (wide.matches) enable();
      else disable();
    }

    wide.addEventListener('change', apply);
    apply();
  }

  enhanceDoors();

  function enhancePrivacyDocument() {
    const documentBody = document.querySelector('[data-privacy-document]');
    const queryField = document.querySelector('[data-privacy-query]');
    const count = document.querySelector('[data-privacy-count]');
    const toc = document.querySelector('[data-privacy-toc]');
    const reset = document.querySelector('[data-privacy-reset]');
    const empty = document.querySelector('[data-privacy-empty]');
    if (!documentBody || !queryField || !toc) return;

    const sections = [];
    let currentSection = null;
    Array.from(documentBody.children).forEach(function (node) {
      if (node === empty) return;
      if (node.tagName === 'H2') {
        currentSection = document.createElement('section');
        currentSection.className = 'policy-section';
        currentSection.id = uniqueId(node.textContent);
        documentBody.insertBefore(currentSection, node);
        currentSection.appendChild(node);
        sections.push(currentSection);
      } else if (currentSection) {
        currentSection.appendChild(node);
      }
    });

    sections.forEach(function (section, index) {
      const heading = section.querySelector('h2');
      const link = document.createElement('a');
      link.href = '#' + section.id;
      link.innerHTML = '<span aria-hidden="true">' + String(index + 1).padStart(2, '0') + '</span><strong></strong>';
      link.querySelector('strong').textContent = heading.textContent;
      link.dataset.privacyTocLink = section.id;
      toc.appendChild(link);
    });

    function applyPrivacySearch() {
      const query = queryField.value.trim().toLocaleLowerCase();
      let visible = 0;
      sections.forEach(function (section) {
        const matches = !query || section.textContent.toLocaleLowerCase().includes(query);
        section.hidden = !matches;
        const link = toc.querySelector('[data-privacy-toc-link="' + section.id + '"]');
        if (link) link.hidden = !matches;
        if (matches) visible += 1;
      });
      if (count) count.textContent = String(visible);
      if (empty) empty.hidden = visible !== 0;
      if (reset) reset.hidden = !query;
    }

    queryField.addEventListener('input', applyPrivacySearch);
    if (reset) {
      reset.addEventListener('click', function () {
        queryField.value = '';
        applyPrivacySearch();
        queryField.focus();
      });
    }
    applyPrivacySearch();
  }

  enhancePrivacyDocument();

  function enhanceAlphaTesterApplication() {
    const form = document.querySelector('[data-alpha-tester-form]');
    if (!form) return;
    const status = form.querySelector('[data-alpha-tester-status]');
    const submit = form.querySelector('[data-alpha-tester-submit]');
    const source = form.querySelector('[data-recruitment-source]');
    const sources = {
      direct: 'direct',
      'testers-community': 'testers_community',
      betabound: 'betabound',
      betafamily: 'betafamily',
      other: 'other'
    };
    const requestedSource = new URLSearchParams(window.location.search).get('source');
    if (source && requestedSource && sources[requestedSource]) source.value = sources[requestedSource];

    if (requestedSource === 'testers-community') {
      const replaceText = function (selector, value) {
        const element = document.querySelector(selector);
        if (element) element.textContent = value;
      };
      replaceText('[data-alpha-tester-eyebrow]', 'Testers Community Pack');
      replaceText('[data-alpha-tester-title]', 'Opt in, then register your profile');
      replaceText('[data-alpha-tester-description]', 'First use the Google Play closed-test opt-in link supplied in the Testers Community Pack instructions, signed in with the same Google account enrolled through Testers Community. Then register your device coverage and testing preferences here. This profile does not grant access and is not proof of Google Play opt-in, installation, or activity. Guest testing does not require a 24Seven.FM station account.');
      replaceText('[data-alpha-tester-submit]', 'Register Testers Community profile');
      replaceText('[data-alpha-tester-next-title]', 'Opt in, then a focused assignment');
      const steps = document.querySelector('[data-alpha-tester-next-steps]');
      if (steps) {
        steps.replaceChildren();
        [
          'Open the Google Play closed-test opt-in link from the Testers Community Pack instructions.',
          'Opt in with the same Google account that Testers Community enrolled for the Pack.',
          'Register your coverage here, then use the Tester Hub for focused assignments and private feedback.'
        ].forEach(function (step) {
          const item = document.createElement('li');
          item.textContent = step;
          steps.appendChild(item);
        });
      }
    }

    function setStatus(message, state) {
      if (!status) return;
      status.textContent = message;
      status.dataset.state = state || '';
    }

    const applicationResult = new URLSearchParams(window.location.search).get('application');
    const draftKey = '24seven-player-alpha-application-draft-v1';
    const saveDraft = function () {
      const fields = {};
      form.querySelectorAll('input, select, textarea').forEach(function (control) {
        if (!control.name || control.name === 'cf-turnstile-response' || control.name === 'company') return;
        if ((control.type === 'checkbox' || control.type === 'radio') && !control.checked) return;
        (fields[control.name] ||= []).push(control.value);
      });
      try { sessionStorage.setItem(draftKey, JSON.stringify(fields)); } catch (_) {}
    };
    const restoreDraft = function () {
      if (applicationResult !== 'error') return false;
      try {
        const fields = JSON.parse(sessionStorage.getItem(draftKey) || '{}');
        Object.keys(fields).forEach(function (name) {
          const values = new Set(fields[name]);
          form.querySelectorAll('[name="' + CSS.escape(name) + '"]').forEach(function (control) {
            if (control.type === 'checkbox' || control.type === 'radio') control.checked = values.has(control.value);
            else if (values.size) control.value = Array.from(values)[0];
          });
        });
        return Object.keys(fields).length > 0;
      } catch (_) { return false; }
    };
    const restoredDraft = restoreDraft();
    if (applicationResult === 'sent') setStatus(requestedSource === 'testers-community' ? 'Your Testers Community profile was sent.' : 'Your tester profile was sent.', 'success');
    if (applicationResult === 'error') setStatus(restoredDraft ? 'We kept your entries in this browser session. Verify them and try again.' : 'The application could not be delivered. Please try again later.', 'error');

    const applicationSteps = Array.from(form.querySelectorAll(':scope > [data-application-step]'));
    if (applicationSteps.length === 0) return;

    const wizard = document.createElement('div');
    wizard.className = 'tester-application-wizard';
    const progress = document.createElement('nav');
    progress.className = 'tester-application-progress';
    progress.setAttribute('aria-label', 'Application progress');
    const progressText = document.createElement('p');
    progressText.className = 'tester-application-progress-text';
    progressText.setAttribute('aria-live', 'polite');
    progressText.tabIndex = -1;
    const actions = document.createElement('div');
    actions.className = 'tester-application-actions';
    actions.dataset.applicationWizardActions = '';
    wizard.append(progress, progressText);
    form.prepend(wizard);
    form.append(actions);

    let currentStep = applicationResult === 'sent' || restoredDraft ? applicationSteps.length - 1 : 0;

    const firstInvalidControl = function (step) {
      return Array.from(step.querySelectorAll('input, select, textarea')).find(function (control) {
        return !control.disabled && control.willValidate && !control.validity.valid;
      });
    };

    const showStep = function (nextStep, options) {
      const shouldMoveFocus = options && options.moveFocus;
      const shouldScroll = options && options.scroll;
      currentStep = Math.max(0, Math.min(nextStep, applicationSteps.length - 1));
      applicationSteps.forEach(function (step, index) {
        step.hidden = index !== currentStep;
      });
      progress.replaceChildren();
      applicationSteps.forEach(function (step, index) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'tester-application-progress-step';
        button.textContent = String(index + 1);
        button.setAttribute('aria-label', 'Step ' + String(index + 1) + ': ' + (step.dataset.applicationStepTitle || 'Application step'));
        button.setAttribute('aria-current', index === currentStep ? 'step' : 'false');
        button.disabled = index > currentStep;
        button.classList.toggle('is-current', index === currentStep);
        button.classList.toggle('is-complete', index < currentStep);
        button.addEventListener('click', function () { showStep(index, { moveFocus: true, scroll: true }); });
        progress.append(button);
      });
      const title = applicationSteps[currentStep].dataset.applicationStepTitle || 'Application';
      progressText.textContent = 'Step ' + String(currentStep + 1) + ' of ' + String(applicationSteps.length) + ': ' + title;

      actions.replaceChildren();
      if (currentStep > 0) {
        const back = document.createElement('button');
        back.type = 'button';
        back.className = 'button secondary';
        back.textContent = 'Back';
        back.addEventListener('click', function () { showStep(currentStep - 1, { moveFocus: true, scroll: true }); });
        actions.append(back);
      }
      if (currentStep < applicationSteps.length - 1) {
        const next = document.createElement('button');
        next.type = 'button';
        next.className = 'button primary';
        next.textContent = 'Continue';
        next.addEventListener('click', function () {
          const invalid = firstInvalidControl(applicationSteps[currentStep]);
          if (invalid) {
            invalid.reportValidity();
            invalid.focus();
            return;
          }
          showStep(currentStep + 1, { moveFocus: true, scroll: true });
        });
        actions.append(next);
      }
      if (shouldScroll) form.scrollIntoView({ block: 'start', behavior: reducedMotion ? 'auto' : 'smooth' });
      if (shouldMoveFocus) window.requestAnimationFrame(function () { progressText.focus({ preventScroll: true }); });
    };

    showStep(currentStep);
    form.addEventListener('input', saveDraft);
    form.addEventListener('change', saveDraft);

    form.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!form.reportValidity()) return;
      saveDraft();
      submit.disabled = true;
      setStatus('Sending your application…', 'pending');
      try {
        const response = await fetch(form.action, {
          method: 'POST',
          headers: { Accept: 'application/json' },
          body: new FormData(form),
        });
        const result = await response.json();
        if (!response.ok || result.ok !== true) throw new Error(result.message || 'The application could not be sent.');
        try { sessionStorage.removeItem(draftKey); } catch (_) {}
        form.reset();
        setStatus(result.message, 'success');
        showStep(applicationSteps.length - 1);
      } catch (error) {
        setStatus(error && error.message ? error.message : 'The application could not be sent. Please try again.', 'error');
      } finally {
        submit.disabled = false;
      }
    });
  }

  enhanceAlphaTesterApplication();
}());
