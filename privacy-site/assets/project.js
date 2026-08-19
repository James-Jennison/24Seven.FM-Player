(function () {
  'use strict';

  const root = document.documentElement;
  root.classList.add('js');

  const header = document.querySelector('[data-site-header]');
  const navigation = document.querySelector('#project-navigation');
  const navToggle = document.querySelector('.nav-toggle');
  const themeToggle = document.querySelector('.theme-toggle');
  const themeLabel = document.querySelector('.theme-toggle-label');
  const themeColor = document.querySelector('meta[name="theme-color"]');
  const scrollProgress = document.querySelector('.scroll-progress span');
  const backToTop = document.querySelector('.back-to-top');
  const toast = document.querySelector('.site-toast');
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)').matches;

  function slugify(value) {
    return value
      .toLocaleLowerCase()
      .normalize('NFKD')
      .replace(/[\u0300-\u036f]/g, '')
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

  function isEditable(target) {
    return Boolean(target && (target.matches('input, textarea, select') || target.isContentEditable));
  }

  function updateHeaderHeight() {
    if (header) root.style.setProperty('--header-height', header.offsetHeight + 'px');
  }

  function setTheme(theme, persist) {
    root.dataset.theme = theme;
    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    if (themeLabel) themeLabel.textContent = theme === 'dark' ? 'Dark' : 'Light';
    if (themeToggle) themeToggle.setAttribute('aria-label', 'Switch to ' + nextTheme + ' theme');
    if (themeColor) themeColor.setAttribute('content', theme === 'dark' ? '#090c15' : '#f5f7fb');
    if (persist) {
      try {
        localStorage.setItem('project-theme', theme);
      } catch (error) {
        // Theme persistence is optional.
      }
    }
  }

  setTheme(root.dataset.theme === 'light' ? 'light' : 'dark', false);
  if (themeToggle) {
    themeToggle.addEventListener('click', function () {
      setTheme(root.dataset.theme === 'dark' ? 'light' : 'dark', true);
    });
  }

  function closeNavigation() {
    if (!header || !navToggle) return;
    header.dataset.navOpen = 'false';
    navToggle.setAttribute('aria-expanded', 'false');
  }

  if (navToggle) {
    navToggle.addEventListener('click', function () {
      if (!header) return;
      const isOpen = header.dataset.navOpen === 'true';
      header.dataset.navOpen = String(!isOpen);
      navToggle.setAttribute('aria-expanded', String(!isOpen));
      updateHeaderHeight();
    });
  }
  if (navigation) {
    navigation.addEventListener('click', function (event) {
      if (event.target.closest('a')) closeNavigation();
    });
  }
  document.addEventListener('click', function (event) {
    if (header && header.dataset.navOpen === 'true' && !header.contains(event.target)) closeNavigation();
  });

  let scrollFrame = null;
  function updateScrollState() {
    scrollFrame = null;
    const scrollable = document.documentElement.scrollHeight - window.innerHeight;
    const ratio = scrollable > 0 ? Math.min(1, window.scrollY / scrollable) : 0;
    if (scrollProgress) scrollProgress.style.transform = 'scaleX(' + ratio + ')';
    if (backToTop) backToTop.dataset.visible = String(window.scrollY > 720);
    if (header) header.dataset.scrolled = String(window.scrollY > 12);
  }

  window.addEventListener('scroll', function () {
    if (scrollFrame === null) scrollFrame = window.requestAnimationFrame(updateScrollState);
  }, { passive: true });
  if (backToTop) {
    backToTop.addEventListener('click', function () {
      window.scrollTo({ top: 0, behavior: reducedMotion ? 'auto' : 'smooth' });
    });
  }

  if (finePointer && !reducedMotion) {
    let pointerFrame = null;
    let pointerX = window.innerWidth * 0.5;
    let pointerY = window.innerHeight * 0.25;
    document.addEventListener('pointermove', function (event) {
      pointerX = event.clientX;
      pointerY = event.clientY;
      if (pointerFrame !== null) return;
      pointerFrame = window.requestAnimationFrame(function () {
        root.style.setProperty('--pointer-x', pointerX + 'px');
        root.style.setProperty('--pointer-y', pointerY + 'px');
        pointerFrame = null;
      });
    }, { passive: true });
  }

  function announce(message) {
    if (!toast) return;
    toast.textContent = message;
    toast.dataset.visible = 'true';
    window.clearTimeout(announce.timeout);
    announce.timeout = window.setTimeout(function () {
      toast.dataset.visible = 'false';
    }, 2000);
  }

  function copyText(value) {
    if (navigator.clipboard && window.isSecureContext) return navigator.clipboard.writeText(value);
    const field = document.createElement('textarea');
    field.value = value;
    field.setAttribute('readonly', '');
    field.style.position = 'fixed';
    field.style.opacity = '0';
    document.body.appendChild(field);
    field.select();
    const copied = document.execCommand('copy');
    field.remove();
    return copied ? Promise.resolve() : Promise.reject(new Error('Copy was not available'));
  }

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

  document.querySelectorAll('main section').forEach(function (section) {
    if (section.id) return;
    const heading = section.querySelector(':scope > h1, :scope > h2, :scope > div:first-child h1, :scope > div:first-child h2, :scope > .section-heading h2, :scope > .content-intro h2');
    if (heading) section.id = uniqueId((heading.id || heading.textContent) + '-section');
  });

  function observeSectionNavigation(nav) {
    if (!nav || !('IntersectionObserver' in window)) return;
    const links = Array.from(nav.querySelectorAll('a[href^="#"]'));
    const sections = links
      .map(function (link) {
        return document.getElementById(decodeURIComponent(link.getAttribute('href').slice(1)));
      })
      .filter(Boolean);
    if (!sections.length) return;

    const observer = new IntersectionObserver(function (entries) {
      const visible = entries
        .filter(function (entry) { return entry.isIntersecting; })
        .sort(function (left, right) { return right.intersectionRatio - left.intersectionRatio; })[0];
      if (!visible) return;
      links.forEach(function (link) {
        if (link.getAttribute('href') === '#' + visible.target.id) link.setAttribute('aria-current', 'location');
        else link.removeAttribute('aria-current');
      });
    }, { rootMargin: '-28% 0px -62% 0px', threshold: [0, 0.2, 0.6] });
    sections.forEach(function (section) { observer.observe(section); });
  }

  document.querySelectorAll('.section-jump, .privacy-toc').forEach(observeSectionNavigation);

  document.querySelectorAll('[data-filter-controls]').forEach(function (controls) {
    const target = document.querySelector(controls.dataset.target);
    if (!target) return;
    const items = Array.from(target.querySelectorAll('[data-filter-item]'));
    const queryField = controls.querySelector('[data-filter-query]');
    const stateButtons = Array.from(controls.querySelectorAll('[data-filter-state]'));
    const count = controls.querySelector('[data-filter-count]');
    const empty = target.querySelector('[data-filter-empty]');
    let selectedState = 'all';

    function applyFilter() {
      const query = (queryField ? queryField.value : '').trim().toLocaleLowerCase();
      let visibleCount = 0;
      items.forEach(function (item) {
        const matchesQuery = !query || item.textContent.toLocaleLowerCase().includes(query);
        const matchesState = selectedState === 'all' || item.dataset.state === selectedState;
        item.hidden = !(matchesQuery && matchesState);
        if (!item.hidden) visibleCount += 1;
      });
      if (count) count.textContent = String(visibleCount);
      if (empty) empty.hidden = visibleCount !== 0;
    }

    if (queryField) queryField.addEventListener('input', applyFilter);
    stateButtons.forEach(function (button) {
      button.addEventListener('click', function () {
        selectedState = button.dataset.filterState;
        stateButtons.forEach(function (candidate) {
          candidate.setAttribute('aria-pressed', String(candidate === button));
        });
        applyFilter();
      });
    });
    applyFilter();
  });

  function enhanceResourceCatalog() {
    const catalog = document.querySelector('[data-resource-catalog]');
    if (!catalog) return;
    const queryField = catalog.querySelector('[data-resource-search]');
    const buttons = Array.from(catalog.querySelectorAll('[data-resource-filter]'));
    const count = catalog.querySelector('[data-resource-visible-count]');
    const empty = document.querySelector('[data-resource-empty-state]');
    const sections = Array.from(document.querySelectorAll('[data-resource-category]'));
    let selectedCategory = 'all';

    function applyResourceFilter() {
      const query = (queryField ? queryField.value : '').trim().toLocaleLowerCase();
      let visible = 0;
      sections.forEach(function (section) {
        const categoryMatches = selectedCategory === 'all' || section.dataset.resourceCategory === selectedCategory;
        let sectionVisible = 0;
        section.querySelectorAll('.resource-grid a, .document-list a').forEach(function (link) {
          const matches = categoryMatches && (!query || link.textContent.toLocaleLowerCase().includes(query));
          link.hidden = !matches;
          if (matches) {
            visible += 1;
            sectionVisible += 1;
          }
        });
        section.hidden = sectionVisible === 0;
      });
      if (count) count.textContent = visible + (visible === 1 ? ' resource visible' : ' resources visible');
      if (empty) empty.hidden = visible !== 0;
      if (document.activeElement && document.activeElement.hidden && queryField) queryField.focus();
    }

    if (queryField) queryField.addEventListener('input', applyResourceFilter);
    buttons.forEach(function (button) {
      button.addEventListener('click', function () {
        selectedCategory = button.dataset.resourceFilter;
        buttons.forEach(function (candidate) {
          candidate.setAttribute('aria-pressed', String(candidate === button));
        });
        applyResourceFilter();
      });
    });
    catalog.dataset.enhanced = 'true';
    applyResourceFilter();
  }

  enhanceResourceCatalog();

  function enhanceTestCatalog() {
    const catalog = document.querySelector('[data-test-catalog]');
    if (!catalog) return;
    const queryField = catalog.querySelector('[data-test-search]');
    const filterButtons = Array.from(catalog.querySelectorAll('[data-test-filter]'));
    const progressText = catalog.querySelector('[data-test-progress-text]');
    const progress = catalog.querySelector('[data-test-progress]');
    const visibleCount = catalog.querySelector('[data-test-visible-count]');
    const taskNote = catalog.querySelector('[data-test-task-note]');
    const clearButton = catalog.querySelector('[data-test-clear-progress]');
    const empty = document.querySelector('[data-test-empty-state]');
    const cases = Array.from(document.querySelectorAll('.test-session-grid article[id^="pt-"]'));
    const caseSections = Array.from(document.querySelectorAll('.content-section')).filter(function (section) {
      return Boolean(section.querySelector('.test-session-grid article[id^="pt-"]'));
    });
    const requestedTaskId = (new URLSearchParams(window.location.search).get('task') || '').toUpperCase();
    const taskCard = requestedTaskId ? document.querySelector('[data-task-card="' + requestedTaskId + '"]') : null;
    const taskCaseIds = taskCard
      ? (taskCard.dataset.taskPtIds || '').split(',').map(function (id) { return id.trim().toUpperCase(); }).filter(Boolean)
      : [];
    const storageKey = 'project-test-checklist-v1';
    let selectedFilter = 'all';
    let checked = new Set();

    try {
      const saved = JSON.parse(localStorage.getItem(storageKey) || '[]');
      checked = new Set(saved.filter(function (id) {
        return cases.some(function (testCase) { return testCase.id === id; });
      }));
    } catch (error) {
      checked = new Set();
    }

    function persistProgress() {
      try {
        localStorage.setItem(storageKey, JSON.stringify(Array.from(checked).sort()));
      } catch (error) {
        // The checklist remains usable for this page view when storage is unavailable.
      }
    }

    function updateCaseButton(testCase) {
      const button = testCase.querySelector('[data-test-check-button]');
      const isChecked = checked.has(testCase.id);
      testCase.dataset.testChecked = String(isChecked);
      if (button) {
        const label = button.dataset.testCaseLabel || testCase.id.toUpperCase();
        button.setAttribute('aria-pressed', String(isChecked));
        button.setAttribute(
          'aria-label',
          isChecked ? 'Return ' + label + ' to remaining' : 'Mark ' + label + ' checked locally'
        );
        button.textContent = isChecked ? '✓ Checked locally' : 'Mark checked locally';
      }
    }

    cases.forEach(function (testCase) {
      const label = (testCase.querySelector('h3') || testCase).textContent.trim();
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'test-check-button';
      button.dataset.testCheckButton = '';
      button.dataset.testCaseLabel = testCase.id.toUpperCase() + ' ' + label;
      button.addEventListener('click', function () {
        if (checked.has(testCase.id)) checked.delete(testCase.id);
        else checked.add(testCase.id);
        persistProgress();
        updateCaseButton(testCase);
        applyTestFilter();
        announce(checked.has(testCase.id) ? testCase.id.toUpperCase() + ' checked locally' : testCase.id.toUpperCase() + ' returned to remaining');
      });
      const marker = testCase.firstElementChild;
      if (marker && marker.nextSibling) testCase.insertBefore(button, marker.nextSibling);
      else testCase.appendChild(button);
      updateCaseButton(testCase);
    });

    function applyTestFilter() {
      const query = (queryField ? queryField.value : '').trim().toLocaleLowerCase();
      let visible = 0;
      cases.forEach(function (testCase) {
        const isChecked = checked.has(testCase.id);
        const isFuture = testCase.classList.contains('future-test-case');
        const matchesQuery = !query || testCase.textContent.toLocaleLowerCase().includes(query);
        const matchesTask = !taskCaseIds.length || taskCaseIds.includes(testCase.id.toUpperCase());
        const matchesFilter =
          selectedFilter === 'all' ||
          selectedFilter === 'checked' && isChecked ||
          selectedFilter === 'remaining' && !isChecked ||
          selectedFilter === 'future' && isFuture;
        testCase.hidden = !(matchesQuery && matchesFilter && matchesTask);
        if (!testCase.hidden) visible += 1;
      });
      caseSections.forEach(function (section) {
        section.hidden = !section.querySelector('.test-session-grid article:not([hidden])');
      });
      if (progressText) progressText.textContent = checked.size + ' of ' + cases.length + ' checked locally';
      if (progress) {
        progress.max = cases.length;
        progress.value = checked.size;
        progress.textContent = checked.size + ' of ' + cases.length;
      }
      if (visibleCount) visibleCount.textContent = visible + (visible === 1 ? ' test case visible' : ' test cases visible');
      if (clearButton) clearButton.disabled = checked.size === 0;
      if (empty) empty.hidden = visible !== 0;
    }

    if (queryField) queryField.addEventListener('input', applyTestFilter);
    filterButtons.forEach(function (button) {
      button.addEventListener('click', function () {
        selectedFilter = button.dataset.testFilter;
        filterButtons.forEach(function (candidate) {
          candidate.setAttribute('aria-pressed', String(candidate === button));
        });
        applyTestFilter();
      });
    });
    if (clearButton) {
      clearButton.addEventListener('click', function () {
        if (!checked.size || !window.confirm('Clear this browser-local test checklist?')) return;
        checked.clear();
        persistProgress();
        cases.forEach(updateCaseButton);
        applyTestFilter();
        announce('Browser-local checklist cleared');
      });
    }
    if (taskCard) {
      taskCard.dataset.taskFocused = 'true';
      if (taskNote) {
        taskNote.hidden = false;
        taskNote.textContent = requestedTaskId + ' is selected. Submit one result for each included PT case.';
      }
    }
    catalog.dataset.enhanced = 'true';
    applyTestFilter();
  }

  enhanceTestCatalog();

  function enhanceCompactPortal() {
    const section = document.body.dataset.pageSection;
    const layouts = {
      features: [
        {
          selectors: ['#community', '#station-matrix', '#boundaries'],
          eyebrow: 'Product reference',
          title: 'Community, station support, and boundaries',
          description: 'Open the detailed capability map and supported-use boundaries.'
        }
      ],
      development: [
        {
          selectors: ['#source-map', '#toolchain', '#contribute'],
          eyebrow: 'Engineering reference',
          title: 'Source map, toolchain, and contribution guide',
          description: 'Open the repository-level implementation reference.'
        }
      ],
      testing: [
        {
          selectors: ['#specialized', '#ci', '#evidence', '#remaining'],
          eyebrow: 'Evidence library',
          title: 'Specialized checks, CI, and remaining gates',
          description: 'Open the detailed validation record and the work that remains.'
        }
      ],
      roadmap: [
        {
          selectors: ['#history', '#notifications', '#production', '#future'],
          eyebrow: 'Roadmap reference',
          title: 'Historical milestones and later delivery gates',
          description: 'Open the full chronology, future work, and non-current programs.'
        }
      ],
      resources: [
        {
          selectors: ['#station-certification', '#readiness'],
          eyebrow: 'Reference library',
          title: 'Station certification and release-readiness records',
          description: 'Open the detailed source and validation library.'
        }
      ],
      'product-testing': [
        {
          selectors: ['#alpha-tester-interest'],
          eyebrow: 'New tester',
          title: 'Apply for Closed Alpha access',
          description: 'Open the private application when you are ready to provide your device and testing coverage.'
        },
        {
          selectors: ['#testing-tasks'],
          eyebrow: 'Task library',
          title: 'Browse coordinator-assigned testing tasks',
          description: 'Open the available task bundles, their prerequisites, and their safety boundaries.'
        },
        {
          selectors: ['#catalog-controls', '[data-test-empty-state]', '#start', '#listening-tests', '#member-tests', '#device-tests', '#future-tests', '#coverage', '#report', '#safety'],
          eyebrow: 'Testing reference',
          title: 'Open the complete test catalog and reporting guide',
          description: 'Search cases, review detailed instructions, and consult coverage and safety guidance only when your assignment needs it.'
        }
      ]
    };
    const groups = layouts[section];
    if (!groups) return;

    groups.forEach(function (group) {
      const items = group.selectors.map(function (selector) {
        return document.querySelector(selector);
      }).filter(Boolean);
      if (!items.length) return;

      const first = items[0];
      const disclosure = document.createElement('details');
      disclosure.className = 'reference-disclosure';
      disclosure.dataset.referenceDisclosure = '';
      const summary = document.createElement('summary');
      summary.innerHTML = '<span><small></small><strong></strong><em></em></span><span class="reference-disclosure-action">Open <b aria-hidden="true">+</b></span>';
      summary.querySelector('small').textContent = group.eyebrow;
      summary.querySelector('strong').textContent = group.title;
      summary.querySelector('em').textContent = group.description;
      const body = document.createElement('div');
      body.className = 'reference-disclosure-body';

      first.parentNode.insertBefore(disclosure, first);
      disclosure.appendChild(summary);
      disclosure.appendChild(body);
      items.forEach(function (item) { body.appendChild(item); });
    });

    function openDisclosureForHash() {
      if (!window.location.hash) return;
      let target;
      try {
        target = document.querySelector(window.location.hash);
      } catch (error) {
        return;
      }
      const disclosure = target && target.closest('details.reference-disclosure');
      if (disclosure) disclosure.open = true;
    }

    window.addEventListener('hashchange', openDisclosureForHash);
    openDisclosureForHash();
  }

  enhanceCompactPortal();

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
    if (applicationResult === 'sent') setStatus(requestedSource === 'testers-community' ? 'Your Testers Community profile was sent.' : 'Your tester profile was sent.', 'success');
    if (applicationResult === 'error') setStatus('The application could not be delivered. Please try again later.', 'error');

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
    const actions = document.createElement('div');
    actions.className = 'tester-application-actions';
    actions.dataset.applicationWizardActions = '';
    wizard.append(progress, progressText);
    form.prepend(wizard);
    form.append(actions);

    let currentStep = applicationResult === 'sent' ? applicationSteps.length - 1 : 0;

    const firstInvalidControl = function (step) {
      return Array.from(step.querySelectorAll('input, select, textarea')).find(function (control) {
        return !control.disabled && control.willValidate && !control.validity.valid;
      });
    };

    const showStep = function (nextStep) {
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
        button.addEventListener('click', function () { showStep(index); });
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
        back.addEventListener('click', function () { showStep(currentStep - 1); });
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
          showStep(currentStep + 1);
        });
        actions.append(next);
      }
      form.scrollIntoView({ block: 'start', behavior: 'smooth' });
    };

    showStep(currentStep);

    form.addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!form.reportValidity()) return;
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

  function enhanceArchitecture() {
    const panel = document.querySelector('[data-architecture-detail-panel]');
    const nodes = Array.from(document.querySelectorAll('[data-architecture-detail]'));
    if (!panel || !nodes.length) return;
    panel.id = panel.id || 'architecture-detail';
    nodes.forEach(function (node) {
      node.tabIndex = 0;
      node.setAttribute('role', 'button');
      node.setAttribute('aria-controls', panel.id);
      node.setAttribute('aria-pressed', 'false');
      function selectNode() {
        nodes.forEach(function (candidate) {
          candidate.setAttribute('aria-pressed', String(candidate === node));
        });
        const title = (node.querySelector('strong') || node).textContent.trim();
        panel.innerHTML = '<strong></strong><span></span>';
        panel.querySelector('strong').textContent = title;
        panel.querySelector('span').textContent = node.dataset.architectureDetail;
      }
      node.addEventListener('click', selectNode);
      node.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          selectNode();
        }
      });
    });
    panel.dataset.enhanced = 'true';
  }

  enhanceArchitecture();

  document.querySelectorAll('[data-command-library] code').forEach(function (code) {
    const wrapper = document.createElement('div');
    wrapper.className = 'command-copy-row';
    code.parentNode.insertBefore(wrapper, code);
    wrapper.appendChild(code);
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'command-copy';
    button.textContent = 'Copy';
    button.setAttribute('aria-label', 'Copy command: ' + code.textContent.trim());
    button.addEventListener('click', function () {
      copyText(code.textContent.trim())
        .then(function () {
          button.textContent = 'Copied';
          announce('Command copied');
          window.setTimeout(function () { button.textContent = 'Copy'; }, 1600);
        })
        .catch(function () { announce('Unable to copy command'); });
    });
    wrapper.appendChild(button);
  });

  document.querySelectorAll('[data-interactive-table] tbody tr').forEach(function (row) {
    row.tabIndex = 0;
    row.classList.add('interactive-table-row');
    row.title = 'Select to highlight this row';
    function toggleRow() {
      const table = row.closest('[data-interactive-table]');
      table.querySelectorAll('tbody tr').forEach(function (candidate) {
        if (candidate !== row) candidate.removeAttribute('data-selected');
      });
      if (row.dataset.selected === 'true') row.removeAttribute('data-selected');
      else row.dataset.selected = 'true';
    }
    row.addEventListener('click', toggleRow);
    row.addEventListener('keydown', function (event) {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        toggleRow();
      }
    });
  });

  const mediaDialog = document.querySelector('.media-dialog');
  const mediaImage = mediaDialog ? mediaDialog.querySelector('img') : null;
  const mediaCaption = mediaDialog ? mediaDialog.querySelector('p') : null;
  const mediaClose = mediaDialog ? mediaDialog.querySelector('.media-dialog-close') : null;
  let mediaOpener = null;
  document.querySelectorAll('[data-lightbox]').forEach(function (trigger) {
    trigger.addEventListener('click', function (event) {
      if (!mediaDialog || !mediaImage || !mediaCaption || typeof mediaDialog.showModal !== 'function') return;
      event.preventDefault();
      mediaOpener = trigger;
      mediaImage.src = trigger.dataset.lightbox;
      mediaImage.alt = trigger.dataset.lightboxAlt || '';
      mediaCaption.textContent = trigger.dataset.lightboxCaption || '';
      mediaDialog.showModal();
    });
  });
  if (mediaClose) mediaClose.addEventListener('click', function () { mediaDialog.close(); });
  if (mediaDialog) {
    mediaDialog.addEventListener('click', function (event) {
      if (event.target === mediaDialog) mediaDialog.close();
    });
    mediaDialog.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && mediaDialog.open) {
        event.preventDefault();
        mediaDialog.close();
      }
    });
    mediaDialog.addEventListener('close', function () {
      if (mediaOpener && typeof mediaOpener.focus === 'function') mediaOpener.focus();
    });
  }

  function enhanceSiteExplorer() {
    const explorer = document.querySelector('.site-explorer');
    const toggle = document.querySelector('.site-explorer-toggle');
    if (!explorer || !toggle || typeof explorer.showModal !== 'function') return;
    const close = explorer.querySelector('.site-explorer-close');
    const queryField = explorer.querySelector('[data-explorer-query]');
    const sectionList = explorer.querySelector('[data-explorer-sections]');
    const empty = explorer.querySelector('[data-explorer-empty]');
    let opener = null;

    if (sectionList) {
      sectionList.innerHTML = '';
      document.querySelectorAll('main section[id]').forEach(function (section) {
        const heading = section.querySelector(':scope > h1, :scope > h2, :scope > div:first-child h1, :scope > div:first-child h2, :scope > .section-heading h2, :scope > .content-intro h2');
        if (!heading) return;
        const link = document.createElement('a');
        link.href = '#' + section.id;
        link.dataset.explorerItem = '';
        link.innerHTML = '<strong></strong><span>Jump to this section</span>';
        link.querySelector('strong').textContent = heading.textContent.replace('#', '').trim();
        sectionList.appendChild(link);
      });
      if (!sectionList.children.length) sectionList.innerHTML = '<p>No additional sections on this page.</p>';
    }

    function items() {
      return Array.from(explorer.querySelectorAll('[data-explorer-item]'));
    }

    function applyExplorerSearch() {
      const query = (queryField ? queryField.value : '').trim().toLocaleLowerCase();
      let visible = 0;
      items().forEach(function (item) {
        const matches = !query || item.textContent.toLocaleLowerCase().includes(query);
        item.hidden = !matches;
        if (matches) visible += 1;
      });
      if (empty) empty.hidden = visible !== 0;
    }

    function openExplorer() {
      opener = document.activeElement;
      explorer.showModal();
      if (queryField) {
        queryField.value = '';
        applyExplorerSearch();
        window.requestAnimationFrame(function () { queryField.focus(); });
      }
    }

    function closeExplorer() {
      if (explorer.open) explorer.close();
    }

    toggle.addEventListener('click', openExplorer);
    if (close) close.addEventListener('click', closeExplorer);
    if (queryField) queryField.addEventListener('input', applyExplorerSearch);
    sectionList.addEventListener('click', closeExplorer);
    explorer.addEventListener('click', function (event) {
      if (event.target === explorer) closeExplorer();
    });
    explorer.addEventListener('close', function () {
      if (opener && typeof opener.focus === 'function') opener.focus();
    });
    explorer.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeExplorer();
        return;
      }
      if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
      const visibleItems = items().filter(function (item) { return !item.hidden; });
      if (!visibleItems.length) return;
      const currentIndex = visibleItems.indexOf(document.activeElement);
      const direction = event.key === 'ArrowDown' ? 1 : -1;
      const nextIndex = currentIndex < 0 ? 0 : (currentIndex + direction + visibleItems.length) % visibleItems.length;
      event.preventDefault();
      visibleItems[nextIndex].focus();
    });
    document.addEventListener('keydown', function (event) {
      const shortcut = (event.key === '/' && !isEditable(event.target)) || ((event.ctrlKey || event.metaKey) && event.key.toLocaleLowerCase() === 'k');
      if (!shortcut) return;
      event.preventDefault();
      if (explorer.open) closeExplorer();
      else openExplorer();
    });
    explorer.dataset.enhanced = 'true';
    toggle.dataset.enhanced = 'true';
  }

  enhanceSiteExplorer();

  if (!reducedMotion && 'IntersectionObserver' in window) {
    const countTargets = Array.from(document.querySelectorAll('.metrics dt, .page-summary dd'))
      .filter(function (item) { return /^\d+$/.test(item.textContent.trim()); });
    const countObserver = new IntersectionObserver(function (entries, observer) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        const item = entry.target;
        const target = Number(item.textContent.trim());
        const started = performance.now();
        function draw(now) {
          const progress = Math.min(1, (now - started) / 650);
          const eased = 1 - Math.pow(1 - progress, 3);
          item.textContent = String(Math.round(target * eased));
          if (progress < 1) window.requestAnimationFrame(draw);
        }
        window.requestAnimationFrame(draw);
        observer.unobserve(item);
      });
    }, { threshold: 0.7 });
    countTargets.forEach(function (item) { countObserver.observe(item); });

    const revealTargets = document.querySelectorAll(
      '.feature-grid article, .portal-grid a, .roadmap-card, .detail-grid article, .principle-grid article, ' +
      '.source-map article, .validation-layers article, .evidence-grid article, .boundary-grid article, ' +
      '.test-session-grid article, .milestone-history li, .resource-grid a, .document-list a, .process-timeline li, figure'
    );
    root.classList.add('reveal-enabled');
    const revealObserver = new IntersectionObserver(function (entries, observer) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.dataset.revealed = 'true';
        observer.unobserve(entry.target);
      });
    }, { rootMargin: '0px 0px -7% 0px', threshold: 0.08 });
    revealTargets.forEach(function (item) { revealObserver.observe(item); });
  }

  if (finePointer && !reducedMotion) {
    document.querySelectorAll(
      '.feature-grid article, .portal-grid a, .roadmap-card, .detail-grid article, .principle-grid article, ' +
      '.source-map article, .validation-layers article, .evidence-grid article, .boundary-grid article, ' +
      '.test-session-grid article, .resource-grid a'
    ).forEach(function (card) {
      card.classList.add('pointer-reactive');
      card.addEventListener('pointermove', function (event) {
        const bounds = card.getBoundingClientRect();
        card.style.setProperty('--card-x', event.clientX - bounds.left + 'px');
        card.style.setProperty('--card-y', event.clientY - bounds.top + 'px');
      }, { passive: true });
    });
  }

  window.addEventListener('resize', updateHeaderHeight, { passive: true });
  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') closeNavigation();
  });
  updateHeaderHeight();
  updateScrollState();
}());
