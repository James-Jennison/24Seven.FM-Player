#!/usr/bin/env node

import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const baseUrl = (process.argv[2] ?? "http://127.0.0.1:4173").replace(/\/$/, "");
const chromeBinary = process.env.PROJECT_SITE_CHROME ?? "/usr/bin/google-chrome";
const hostResolverRules = process.env.PROJECT_SITE_CHROME_HOST_RESOLVER_RULES;
const profile = await mkdtemp(join(tmpdir(), "player-site-chrome-"));
const chrome = spawn(
  chromeBinary,
  [
    "--headless=new",
    "--no-sandbox",
    "--disable-gpu",
    "--disable-background-networking",
    "--disable-component-update",
    "--disable-default-apps",
    "--disable-extensions",
    "--no-first-run",
    ...(hostResolverRules ? [`--host-resolver-rules=${hostResolverRules}`] : []),
    "--remote-debugging-port=0",
    `--user-data-dir=${profile}`,
    "about:blank",
  ],
  { stdio: "ignore" },
);
const chromeExited = new Promise((resolve) => chrome.once("exit", resolve));

const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

async function readDebuggerPort() {
  const path = join(profile, "DevToolsActivePort");
  for (let attempt = 0; attempt < 100; attempt += 1) {
    try {
      const [port] = (await readFile(path, "utf8")).trim().split("\n");
      return port;
    } catch {
      await delay(50);
    }
  }
  throw new Error("Chrome did not expose a debugging port");
}

let socket;
let commandId = 0;
const pending = new Map();
const eventWaiters = new Map();
const browserErrors = [];
const stationLayoutAssertions = { intro: [], mobile: [], tablet: [], desktop: [] };

function waitForEvent(method, timeout = 10_000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`Timed out waiting for ${method}`)), timeout);
    const listeners = eventWaiters.get(method) ?? [];
    listeners.push((params) => {
      clearTimeout(timer);
      resolve(params);
    });
    eventWaiters.set(method, listeners);
  });
}

function send(method, params = {}) {
  commandId += 1;
  const id = commandId;
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    socket.send(JSON.stringify({ id, method, params }));
  });
}

async function evaluate(expression) {
  const response = await send("Runtime.evaluate", {
    expression,
    returnByValue: true,
    awaitPromise: true,
  });
  if (response.exceptionDetails) {
    throw new Error(response.exceptionDetails.text ?? "Browser evaluation failed");
  }
  return response.result.value;
}

async function navigate(route) {
  const loaded = waitForEvent("Page.loadEventFired");
  await send("Page.navigate", { url: `${baseUrl}${route}` });
  await loaded;
  await delay(120);
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

try {
  const port = await readDebuggerPort();
  const target = await fetch(`http://127.0.0.1:${port}/json/new?${encodeURIComponent(`${baseUrl}/`)}`, {
    method: "PUT",
  }).then((response) => response.json());

  socket = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener("open", resolve, { once: true });
    socket.addEventListener("error", reject, { once: true });
  });
  socket.addEventListener("message", (event) => {
    const message = JSON.parse(event.data);
    if (message.id) {
      const handler = pending.get(message.id);
      if (!handler) return;
      pending.delete(message.id);
      if (message.error) handler.reject(new Error(message.error.message));
      else handler.resolve(message.result ?? {});
      return;
    }
    if (message.method === "Runtime.exceptionThrown") {
      browserErrors.push(message.params.exceptionDetails.text ?? "Uncaught browser exception");
    }
    if (message.method === "Log.entryAdded" && message.params.entry.level === "error") {
      browserErrors.push(message.params.entry.text);
    }
    const listeners = eventWaiters.get(message.method) ?? [];
    const listener = listeners.shift();
    if (listener) listener(message.params);
  });

  await send("Page.enable");
  await send("Runtime.enable");
  await send("Log.enable");

  const routes = [
    "/",
    "/stations/",
    "/features/",
    "/platforms/",
    "/development/",
    "/testing/",
    "/product-testing/",
    "/roadmap/",
    "/resources/",
    "/dev/",
    "/dev/development/",
    "/dev/testing/",
    "/dev/tester-workspace/",
    "/dev/roadmap/",
    "/dev/resources/",
    "/privacy/",
    "/privacy/tv/",
    "/404.html",
  ];
  const expectedPublicNavigation = ["/", "/stations/", "/features/", "/platforms/", "/privacy/"];
  const expectedDeveloperNavigation = ["/dev/development/", "/dev/testing/", "/dev/tester-workspace/", "/dev/roadmap/", "/dev/resources/"];

  for (const viewport of [
    { width: 320, height: 568, label: "narrow mobile" },
    { width: 390, height: 844, label: "standard mobile" },
    { width: 560, height: 900, label: "mobile grid boundary" },
    { width: 561, height: 900, label: "mobile grid boundary plus one" },
    { width: 768, height: 1024, label: "tablet" },
    { width: 800, height: 1024, label: "tablet grid boundary" },
    { width: 801, height: 1024, label: "tablet grid boundary plus one" },
    { width: 1024, height: 1000, label: "compact laptop" },
    { width: 1366, height: 768, label: "standard laptop" },
    { width: 1440, height: 1000, label: "laptop" },
    { width: 1920, height: 864, label: "27-inch desktop browser" },
    { width: 1920, height: 1080, label: "large desktop" },
  ]) {
    await send("Emulation.setDeviceMetricsOverride", {
      width: viewport.width,
      height: viewport.height,
      deviceScaleFactor: 1,
      mobile: viewport.width < 600,
    });
    const testedRoutes = viewport.width <= 390
      ? routes
      : [560, 561, 800, 801].includes(viewport.width)
        ? ["/stations/"]
        : ["/", "/stations/", "/platforms/", "/product-testing/", "/privacy/", "/privacy/tv/", "/dev/", "/dev/tester-workspace/"];
    for (const route of testedRoutes) {
      await navigate(route);
      await evaluate("window.scrollTo(0, document.documentElement.scrollHeight)");
      await delay(120);
      const state = await evaluate(`(() => ({
        title: document.title,
        h1: document.querySelectorAll('h1').length,
        overflow: document.documentElement.scrollWidth > window.innerWidth + 1,
        overflowingElements: [...document.querySelectorAll('body *')]
          .map((element) => {
            const rect = element.getBoundingClientRect();
            return { selector: element.className || element.id || element.tagName, left: Math.round(rect.left), right: Math.round(rect.right) };
          })
          .filter((element) => element.left < -1 || element.right > window.innerWidth + 1)
          .slice(0, 8),
        missingImages: [...document.images].filter((image) => image.getAttribute('src') && (!image.complete || image.naturalWidth === 0)).map((image) => image.src),
        canonical: document.querySelector('link[rel="canonical"]')?.href ?? '',
        navigation: document.querySelectorAll('#project-navigation a').length,
        navigationPaths: [...document.querySelectorAll('#project-navigation a')].map((link) => new URL(link.href).pathname),
        developerWorkspace: document.body.classList.contains('developer-workspace'),
        expectedNavigation: 5
      }))()`);
      assert(state.title, `${viewport.label} ${route} has no title`);
      assert(state.h1 === 1, `${viewport.label} ${route} has ${state.h1} h1 elements`);
      assert(!state.overflow, `${viewport.label} ${route} has horizontal overflow: ${JSON.stringify(state.overflowingElements)}`);
      assert(state.missingImages.length === 0, `${viewport.label} ${route} has missing images: ${state.missingImages.join(", ")}`);
      assert(state.canonical.startsWith("https://24sevenfmplayer.com/"), `${route} has the wrong canonical URL`);
      assert(state.navigation === state.expectedNavigation, `${route} exposes the wrong workspace navigation`);
      const expectedNavigation = state.developerWorkspace ? expectedDeveloperNavigation : expectedPublicNavigation;
      assert(JSON.stringify(state.navigationPaths) === JSON.stringify(expectedNavigation), `${route} exposes the wrong primary navigation destinations: ${state.navigationPaths.join(", ")}`);

      if (route === "/" && viewport.width >= 1024) {
        const homeViewport = await evaluate(`(() => {
          const destinationGrid = document.querySelector('.home-destination-grid');
          const decisionSurface = document.querySelector('.home-decision-surface');
          const heroGallery = document.querySelector('.landing-hero-gallery');
          const heroCopy = [document.querySelector('#hero-title'), document.querySelector('.landing-home .hero-lede')];
          const scrollY = window.scrollY;
          const primaryElements = [
            document.querySelector('#hero-title'),
            ...document.querySelectorAll('.landing-home .actions a'),
            ...document.querySelectorAll('.landing-hero-device'),
            ...destinationGrid.querySelectorAll('.home-destination')
          ];
          return {
            destinationsDocumentBottom: destinationGrid.getBoundingClientRect().bottom + scrollY,
            decisionSurfaceDocumentBottom: decisionSurface.getBoundingClientRect().bottom + scrollY,
            viewportHeight: window.innerHeight,
            viewportWidth: window.innerWidth,
            destinationCount: destinationGrid.querySelectorAll('.home-destination').length,
            heroCaptureCount: document.querySelectorAll('.landing-hero-device').length,
            heroGallery: (() => {
              const rect = heroGallery.getBoundingClientRect();
              return { top: rect.top + scrollY, bottom: rect.bottom + scrollY, left: rect.left, right: rect.right };
            })(),
            heroGallerySemantics: {
              role: heroGallery.getAttribute('role'),
              label: heroGallery.getAttribute('aria-label'),
              captionCount: heroGallery.querySelectorAll('figcaption').length
            },
            heroCopyBounds: heroCopy.map((element) => {
              const rect = element.getBoundingClientRect();
              return { top: rect.top + scrollY, bottom: rect.bottom + scrollY, left: rect.left, right: rect.right };
            }),
            heroFigureBounds: [...document.querySelectorAll('.landing-hero-device')].map((element) => {
              const rect = element.getBoundingClientRect();
              return { top: rect.top + scrollY, bottom: rect.bottom + scrollY, left: rect.left, right: rect.right };
            }),
            actionBounds: [...document.querySelectorAll('.landing-home .actions a')].map((element) => {
              const rect = element.getBoundingClientRect();
              return { top: rect.top + scrollY, bottom: rect.bottom + scrollY, left: rect.left, right: rect.right };
            }),
            destinationBounds: [...destinationGrid.querySelectorAll('.home-destination')].map((element) => {
              const rect = element.getBoundingClientRect();
              return { top: rect.top + scrollY, bottom: rect.bottom + scrollY, left: rect.left, right: rect.right };
            }),
            primaryBounds: primaryElements.map((element) => {
              const rect = element.getBoundingClientRect();
              return {
                label: element.id || element.textContent.trim().replace(/\s+/g, ' ').slice(0, 32),
                top: rect.top + scrollY,
                bottom: rect.bottom + scrollY,
                left: rect.left,
                right: rect.right
              };
            })
          };
        })()`);
        assert(homeViewport.destinationCount === 4, `${viewport.label} Home lost a primary destination`);
        assert(homeViewport.heroCaptureCount === 2, `${viewport.label} Home must present both primary mobile captures`);
        assert(homeViewport.heroGallerySemantics.role === 'group' && homeViewport.heroGallerySemantics.label === 'Captured Android mobile Player screens' && homeViewport.heroGallerySemantics.captionCount === 2, `${viewport.label} Home hero gallery lost its named two-capture semantics`);
        assert(homeViewport.destinationsDocumentBottom <= homeViewport.viewportHeight + 1, `${viewport.label} Home leaves its primary destinations below the initial viewport`);
        assert(homeViewport.decisionSurfaceDocumentBottom <= homeViewport.viewportHeight + 1, `${viewport.label} Home decision surface exceeds the initial viewport`);
        assert(homeViewport.primaryBounds.length === 9, `${viewport.label} Home primary-surface inventory changed unexpectedly`);
        assert(homeViewport.primaryBounds.every((element) => element.top >= -1 && element.bottom <= homeViewport.viewportHeight + 1 && element.left >= -1 && element.right <= homeViewport.viewportWidth + 1), `${viewport.label} Home leaves a primary control or capture outside the initial viewport: ${JSON.stringify(homeViewport.primaryBounds)}`);
        assert(homeViewport.heroCopyBounds.every((element) => element.top >= -1 && element.bottom <= homeViewport.viewportHeight + 1 && element.left >= -1 && element.right <= homeViewport.viewportWidth + 1), `${viewport.label} Home leaves hero copy outside the initial viewport: ${JSON.stringify(homeViewport.heroCopyBounds)}`);
        assert(homeViewport.heroFigureBounds.every((figure) => figure.top >= homeViewport.heroGallery.top - 1 && figure.bottom <= homeViewport.heroGallery.bottom + 1 && figure.left >= homeViewport.heroGallery.left - 1 && figure.right <= homeViewport.heroGallery.right + 1), `${viewport.label} Home hero media paints outside its reserved gallery: ${JSON.stringify(homeViewport)}`);
        const intersects = (left, right) => left.left < right.right && left.right > right.left && left.top < right.bottom && left.bottom > right.top;
        const nonFigureBounds = [...homeViewport.heroCopyBounds, ...homeViewport.actionBounds, ...homeViewport.destinationBounds];
        assert(homeViewport.heroFigureBounds.every((figure) => !nonFigureBounds.some((element) => intersects(figure, element))), `${viewport.label} Home hero media overlaps copy, a primary action, or a destination: ${JSON.stringify(homeViewport)}`);
        assert(!intersects(homeViewport.heroFigureBounds[0], homeViewport.heroFigureBounds[1]), `${viewport.label} Home hero captures overlap each other: ${JSON.stringify(homeViewport.heroFigureBounds)}`);
        assert(homeViewport.heroFigureBounds[0].right - homeViewport.heroFigureBounds[0].left >= 150 && homeViewport.heroFigureBounds[1].right - homeViewport.heroFigureBounds[1].left >= 120, `${viewport.label} Home hero captures are too small to serve as useful product previews: ${JSON.stringify(homeViewport.heroFigureBounds)}`);
        const measuredCaptureWidths = homeViewport.heroFigureBounds.map((figure) => Math.round(figure.right - figure.left));
        console.log(`Home hero geometry ${viewport.label}: captures=${measuredCaptureWidths.join("px,")}px; gallery=${Math.round(homeViewport.heroGallery.right - homeViewport.heroGallery.left)}px wide; decisionSurfaceBottom=${Math.round(homeViewport.decisionSurfaceDocumentBottom)}px of ${homeViewport.viewportHeight}px.`);
      }

      if (route === "/stations/") {
        const stationLinks = await evaluate(`(() => [...document.querySelectorAll('.landing-station a')].map((link) => ({
          href: link.href,
          target: link.target,
          rel: link.rel,
          text: link.textContent.replace(/\\s+/g, ' ').trim(),
          accessibleName: link.getAttribute('aria-label') ?? '',
          cardLinkCount: link.closest('.landing-station')?.querySelectorAll('a, button, input, select, textarea, [tabindex]:not([tabindex="-1"])').length ?? 0
        })))()`);
        assert(stationLinks.length === 5, "Station directory does not expose five official destinations");
        assert(stationLinks.every((link) => link.target === "_blank" && link.rel.includes("noopener") && link.rel.includes("noreferrer") && link.text.includes("Visit official site") && link.text.includes("↗")), "Station directory lost its visible new-tab disclosure or tab-nabbing protection");
        assert(stationLinks.every((link) => link.accessibleName.trim().length > 0 && link.cardLinkCount === 1), "Each station card must expose exactly one named keyboard destination");
        const stationActionNegative = await evaluate(`(() => {
          const card = document.querySelector('.landing-station');
          const probe = document.createElement('button');
          probe.type = 'button';
          probe.textContent = 'Regression probe';
          card.append(probe);
          const rejected = card.querySelectorAll('a, button, input, select, textarea, [tabindex]:not([tabindex="-1"])').length !== 1;
          probe.remove();
          return rejected;
        })()`);
        assert(stationActionNegative, "Station-directory action guard did not reject an added keyboard control");
        const stationLayout = await evaluate(`(() => {
          const intro = document.querySelector('.landing-section-heading');
          const grid = document.querySelector('.landing-station-grid');
          const cards = [...document.querySelectorAll('.landing-station')];
          const bounds = (element) => {
            const { left, top, width, height } = element.getBoundingClientRect();
            return { left, top, width, height, right: left + width, bottom: top + height, center: left + width / 2 };
          };
          return { intro: [...intro.children].map(bounds), grid: bounds(grid), cards: cards.map(bounds) };
        })()`);
        assert(stationLayout.cards.length === 5, `${viewport.label} station directory has the wrong station-card count`);
        const [introHeading, introCopy] = stationLayout.intro;
        if (viewport.width <= 900) {
          assert(introCopy.top >= introHeading.bottom - 1, `${viewport.label} station introduction did not stack`);
          stationLayoutAssertions.intro.push(viewport.width);
        }
        const [first, second, third, fourth, fifth] = stationLayout.cards;
        if (viewport.width > 900) {
          assert(Math.abs(first.top - second.top) < 2 && Math.abs(second.top - third.top) < 2, `${viewport.label} station cards did not form a three-card desktop opening row`);
          assert(Math.abs(fourth.top - fifth.top) < 2 && fourth.top > first.bottom, `${viewport.label} station cards did not form a compact 3+2 desktop directory`);
          stationLayoutAssertions.desktop.push(viewport.width);
        } else if (viewport.width > 580) {
          assert(Math.abs(first.top - second.top) < 2 && Math.abs(third.top - fourth.top) < 2 && third.top > first.bottom, `${viewport.label} station cards did not form a 2+2 tablet directory`);
          assert(fifth.top > third.bottom && Math.abs(fifth.center - stationLayout.grid.center) < 2, `${viewport.label} tablet remainder card is not centered`);
          stationLayoutAssertions.tablet.push(viewport.width);
        } else {
          assert(second.top > first.bottom && third.top > second.bottom && fourth.top > third.bottom && fifth.top > fourth.bottom, `${viewport.label} station cards did not form a one-column mobile stack`);
          assert(Math.abs(fifth.width - first.width) < 2, `${viewport.label} station cards lost equal sizing on mobile`);
          stationLayoutAssertions.mobile.push(viewport.width);
        }
      }

      if (route === "/platforms/") {
        const platformContract = await evaluate(`(() => ({
          text: document.body.innerText,
          googlePlayLinks: [...document.querySelectorAll('.platform-card#android-mobile a')].filter((link) => /play\\.google\\.com/i.test(link.href)).map((link) => link.href),
          publicInstallLabels: [...document.querySelectorAll('.platform-card#android-mobile a, .platform-card#android-mobile button')].map((control) => control.textContent.replace(/\\s+/g, ' ').trim()).filter((label) => /^(install|download|get it on google play)/i.test(label)),
          headingSizes: [...document.querySelectorAll('.platform-card h2')].map((heading) => Number.parseFloat(getComputedStyle(heading).fontSize)),
          imageRatios: [...document.querySelectorAll('.platform-card img')].map((image) => image.getBoundingClientRect().height / image.getBoundingClientRect().width)
        }))()`);
        assert(platformContract.text.includes("24Seven.FM: Internet Radio App is available to install from Google Play."), "Platforms lost the listing-backed Android mobile availability statement");
        assert(platformContract.text.includes("The Android TV Player is in early development and testing, with no public Android TV install path yet."), "Platforms lost the fact-bound Android TV availability statement");
        assert(platformContract.googlePlayLinks.length === 1 && platformContract.publicInstallLabels.length === 1 && platformContract.publicInstallLabels[0] === "Get it on Google Play ↗", "Platforms must expose the single listing-backed Android install path");
        assert(platformContract.headingSizes.length === 2 && platformContract.headingSizes.every((size) => size <= 60), "Platforms lost its bounded card-heading typography");
        assert(platformContract.imageRatios.length === 2 && platformContract.imageRatios.every((ratio) => ratio > 0 && ratio <= 2.3), "Platforms stretched a capture beyond its natural aspect ratio");
      }

      if (route === "/features/") {
        const featureContract = await evaluate(`(() => {
          const cards = [...document.querySelectorAll('.feature-directory-grid article')];
          return {
            text: document.body.innerText,
            actionsPerCard: cards.map((card) => [...card.querySelectorAll('a, button, input, select, textarea, [tabindex]:not([tabindex="-1"])')].map((control) => ({
              label: control.getAttribute('aria-label') || control.textContent.replace(/\\s+/g, ' ').trim()
            })))
          };
        })()`);
        assert(featureContract.text.includes("24Seven.FM: Internet Radio App is available to install from Google Play."), "Features lost the listing-backed Android availability statement");
        assert(featureContract.text.includes("The invited tester program remains separate from the public Android release."), "Features lost the separate tester-program statement");
        assert(featureContract.actionsPerCard.slice(0, 3).every((actions) => actions.length === 0), "Informational feature cards unexpectedly introduced keyboard destinations");
        assert(featureContract.actionsPerCard[3]?.length === 1 && featureContract.actionsPerCard[3][0].label === "Get it on Google Play ↗", "Android-availability feature card must expose one named install destination");
      }
    }
  }

  await send("Emulation.setDeviceMetricsOverride", {
    width: 390,
    height: 844,
    deviceScaleFactor: 1,
    mobile: true,
  });
  await navigate("/product-testing/");
  const wizardState = await evaluate(`(() => {
    const form = document.querySelector('[data-alpha-tester-form]');
    const steps = [...form.querySelectorAll(':scope > [data-application-step]')];
    const continueButton = () => form.querySelector('.tester-application-actions .button.primary');
    const visibleStep = () => steps.find((step) => !step.hidden)?.dataset.applicationStep;
    const initial = visibleStep();
    const progress = form.querySelectorAll('.tester-application-progress-step');
    form.querySelector('[name="name"]').value = 'Wizard tester';
    form.querySelector('[name="email"]').value = 'wizard@example.test';
    continueButton().click();
    return {
      initial,
      afterIdentity: visibleStep(),
      stepCount: steps.length,
      progressCount: progress.length,
      futureProgressDisabled: [...progress].slice(1).every((button) => button.disabled),
    };
  })()`);
  assert(wizardState.initial === "identity", "Tester application did not begin at the identity step");
  assert(wizardState.afterIdentity === "listening", "Tester application did not advance after valid identity details");
  assert(wizardState.stepCount === 6 && wizardState.progressCount === 6, "Tester application wizard does not expose six stages");
  assert(wizardState.futureProgressDisabled, "Tester application wizard permits skipping incomplete stages");

  await send("Emulation.setDeviceMetricsOverride", {
    width: 390,
    height: 844,
    deviceScaleFactor: 1,
    mobile: true,
  });
  await navigate("/");
  const homeInteractions = await evaluate(`(() => {
    const theme = document.querySelector('.theme-toggle');
    const before = document.documentElement.dataset.theme;
    theme.click();
    const after = document.documentElement.dataset.theme;
    const menu = document.querySelector('.nav-toggle');
    menu.click();
    const menuOpen = menu.getAttribute('aria-expanded');
    document.querySelector('.site-explorer-toggle').click();
    const explorerOpen = document.querySelector('#site-explorer').open;
    document.querySelector('.site-explorer-close').click();
    document.querySelector('[data-lightbox]').click();
    const lightboxOpen = document.querySelector('.media-dialog').open;
    return { before, after, menuOpen, explorerOpen, lightboxOpen };
  })()`);
  assert(homeInteractions.before !== homeInteractions.after, "Theme control did not change theme");
  assert(homeInteractions.menuOpen === "true", "Mobile navigation did not open");
  assert(homeInteractions.explorerOpen, "Site explorer did not open");
  assert(homeInteractions.lightboxOpen, "Screenshot dialog did not open");
  await send("Input.dispatchKeyEvent", { type: "keyDown", key: "Escape", code: "Escape" });
  await send("Input.dispatchKeyEvent", { type: "keyUp", key: "Escape", code: "Escape" });
  await delay(50);
  assert(!(await evaluate("document.querySelector('.media-dialog').open")), "Escape did not close the screenshot dialog");

  await send("Emulation.setDeviceMetricsOverride", {
    width: 1440,
    height: 1000,
    deviceScaleFactor: 1,
    mobile: false,
  });
  await navigate("/");
  await send("Input.dispatchKeyEvent", { type: "keyDown", key: "Tab", code: "Tab" });
  await send("Input.dispatchKeyEvent", { type: "keyUp", key: "Tab", code: "Tab" });
  const keyboardFocus = await evaluate(`(() => {
    const active = document.activeElement;
    const style = getComputedStyle(active);
    return {
      focusable: active?.matches('a, button, input, [tabindex]:not([tabindex="-1"])') ?? false,
      visible: style.outlineStyle !== 'none' && style.outlineWidth !== '0px'
    };
  })()`);
  assert(keyboardFocus.focusable, "Tab did not reach an interactive control");
  assert(keyboardFocus.visible, "Keyboard focus did not have a visible outline");
  await send("Input.dispatchKeyEvent", { type: "keyDown", key: "/", code: "Slash" });
  await send("Input.dispatchKeyEvent", { type: "keyUp", key: "/", code: "Slash" });
  await delay(50);
  const shortcutState = await evaluate(`(() => ({
    open: document.querySelector('#site-explorer').open,
    queryFocused: document.activeElement === document.querySelector('[data-explorer-query]')
  }))()`);
  assert(shortcutState.open && shortcutState.queryFocused, "Keyboard shortcut did not open and focus the site explorer");
  await send("Input.dispatchKeyEvent", { type: "keyDown", key: "Escape", code: "Escape" });
  await send("Input.dispatchKeyEvent", { type: "keyUp", key: "Escape", code: "Escape" });

  await navigate("/dev/tester-workspace/");
  const testingState = await evaluate(`(() => {
    const cases = document.querySelectorAll('.test-session-grid article[id^="pt-"]');
    const first = document.querySelector('.test-check-button');
    first.click();
    return { cases: cases.length, checked: first.getAttribute('aria-pressed'), stored: localStorage.getItem('project-test-checklist-v1') };
  })()`);
  assert(testingState.cases === 34, `Expected 34 test cases; found ${testingState.cases}`);
  assert(testingState.checked === "true", "Tester progress control did not update");
  assert(testingState.stored, "Tester progress was not stored locally");

  await navigate("/dev/tester-workspace/?task=TT-02");
  const taskFilterState = await evaluate(`(() => ({
    taskCards: document.querySelectorAll('[data-task-card]').length,
    summary: document.querySelector('.task-summary-grid')?.textContent ?? '',
    focused: document.querySelector('[data-task-card="TT-02"]')?.dataset.taskFocused,
    visibleCases: [...document.querySelectorAll('.test-session-grid article[id^="pt-"]')]
      .filter((item) => !item.hidden).map((item) => item.id),
    taskNote: document.querySelector('[data-test-task-note]')?.textContent ?? ''
  }))()`);
  assert(taskFilterState.taskCards === 23, `Expected 23 Tester Tasks; found ${taskFilterState.taskCards}`);
  assert(taskFilterState.summary.includes("34") && taskFilterState.summary.includes("19") && taskFilterState.summary.includes("4"), "Tester Task summary is incomplete");
  assert(taskFilterState.focused === "true", "TT-02 was not focused from the task URL");
  assert(taskFilterState.visibleCases.join(",") === "pt-02,pt-03", `TT-02 exposed the wrong PT cases: ${taskFilterState.visibleCases.join(",")}`);
  assert(taskFilterState.taskNote.includes("one result for each"), "Task filter did not explain per-case reporting");

  await navigate("/dev/resources/");
  const resourceState = await evaluate(`(() => {
    const field = document.querySelector('[data-resource-search]');
    field.value = 'architecture';
    field.dispatchEvent(new Event('input', { bubbles: true }));
    return {
      total: document.querySelectorAll('[data-resource-category] a').length,
      visible: [...document.querySelectorAll('[data-resource-category] a')].filter((item) => !item.hidden).length
    };
  })()`);
  assert(resourceState.total === 20, `Expected 20 curated resources; found ${resourceState.total}`);
  assert(resourceState.visible > 0 && resourceState.visible < resourceState.total, "Resource search did not filter");

  await navigate("/privacy/");
  const privacyState = await evaluate(`(() => {
    const field = document.querySelector('[data-privacy-query]');
    field.value = 'sessions';
    field.dispatchEvent(new Event('input', { bubbles: true }));
    return {
      visible: [...document.querySelectorAll('[data-privacy-document] > section')].filter((item) => !item.hidden).length,
      stationDeletionPath: document.body.textContent.includes('Contact/Feedback system to reach Network/Station Administration')
    };
  })()`);
  assert(privacyState.visible > 0, "Privacy search returned no session results");
  assert(privacyState.stationDeletionPath, "The source-backed station deletion path is missing");

  await send("Emulation.setEmulatedMedia", {
    media: "screen",
    features: [{ name: "prefers-reduced-motion", value: "reduce" }],
  });
  await navigate("/");
  const reducedMotion = await evaluate(`(() => ({
    scrollBehavior: getComputedStyle(document.documentElement).scrollBehavior,
    ambientAnimation: getComputedStyle(document.querySelector('.ambient-stage span')).animationName
  }))()`);
  assert(reducedMotion.scrollBehavior === "auto", "Reduced motion did not disable smooth scrolling");
  assert(reducedMotion.ambientAnimation === "none", "Reduced motion left ambient animation enabled");

  await send("Emulation.setEmulatedMedia", {
    media: "screen",
    features: [{ name: "forced-colors", value: "active" }],
  });
  await navigate("/");
  const forcedColors = await evaluate(`(() => ({
    ambientDisplay: getComputedStyle(document.querySelector('.ambient-stage')).display,
    buttonBorder: getComputedStyle(document.querySelector('.theme-toggle')).borderStyle
  }))()`);
  assert(forcedColors.ambientDisplay === "none", "Forced colors left decorative ambient layers visible");
  assert(forcedColors.buttonBorder !== "none", "Forced colors removed the theme control boundary");

  await navigate("/stations/");
  const forcedStationFocus = await evaluate(`(() => [...document.querySelectorAll('.landing-station a')].map((link) => {
    link.focus({ focusVisible: true });
    const style = getComputedStyle(link);
    return {
      named: Boolean(link.getAttribute('aria-label')?.trim()),
      visible: style.outlineStyle !== 'none' && style.outlineWidth !== '0px',
      inVisualAndDomOrder: [...document.querySelectorAll('.landing-station a')].indexOf(link) === [...document.querySelectorAll('.landing-station')].indexOf(link.closest('.landing-station'))
    };
  }))()`);
  assert(forcedStationFocus.length === 5 && forcedStationFocus.every((link) => link.named && link.visible && link.inVisualAndDomOrder), "Forced colors lost named, visible, ordered station-directory keyboard focus");
  const forcedStationFocusNegative = await evaluate(`(() => {
    const link = document.querySelector('.landing-station a');
    link.focus({ focusVisible: true });
    const priorValue = link.style.getPropertyValue('outline');
    const priorPriority = link.style.getPropertyPriority('outline');
    link.style.setProperty('outline', 'none', 'important');
    const rejected = getComputedStyle(link).outlineStyle === 'none' || getComputedStyle(link).outlineWidth === '0px';
    if (priorValue) link.style.setProperty('outline', priorValue, priorPriority);
    else link.style.removeProperty('outline');
    return rejected;
  })()`);
  assert(forcedStationFocusNegative, "Forced-colors focus guard did not reject a suppressed station-link outline");

  await send("Emulation.setScriptExecutionDisabled", { value: true });
  await navigate("/");
  const noScript = await evaluate(`(() => ({
    h1: document.querySelectorAll('h1').length,
    navigation: document.querySelectorAll('#project-navigation a').length,
    destinations: document.querySelectorAll('.home-destination').length,
    heroCaptures: document.querySelectorAll('.landing-hero-device').length,
    heroGalleryRole: document.querySelector('.landing-hero-gallery')?.getAttribute('role') ?? '',
    heroGalleryLabel: document.querySelector('.landing-hero-gallery')?.getAttribute('aria-label') ?? '',
    heroCaptions: document.querySelectorAll('.landing-hero-device figcaption').length
  }))()`);
  assert(noScript.h1 === 1 && noScript.navigation === 5 && noScript.destinations === 4 && noScript.heroCaptures === 2 && noScript.heroCaptions === 2 && noScript.heroGalleryRole === 'group' && noScript.heroGalleryLabel === 'Captured Android mobile Player screens', "No-JavaScript home fallback lost essential content");
  await navigate("/stations/");
  const noScriptStations = await evaluate(`(() => ({ h1: document.querySelectorAll('h1').length, stationPanels: document.querySelectorAll('.landing-station').length }))()`);
  assert(noScriptStations.h1 === 1 && noScriptStations.stationPanels === 5, "No-JavaScript station directory lost essential content");

  assert(browserErrors.length === 0, `Browser errors: ${browserErrors.join(" | ")}`);
  assert(Object.values(stationLayoutAssertions).every((viewports) => viewports.length > 0), "Station layout assertions did not cover every responsive branch");
  console.log(`Station layout assertion coverage: intro=${stationLayoutAssertions.intro.join(",")}; mobile=${stationLayoutAssertions.mobile.join(",")}; tablet=${stationLayoutAssertions.tablet.join(",")}; desktop=${stationLayoutAssertions.desktop.join(",")}.`);
  console.log("Validated responsive and breakpoint-boundary viewports, consumer and developer routes, keyboard and pointer interactions, local-only state, reduced motion, forced colors, and no-JavaScript fallback.");
} finally {
  if (socket?.readyState === WebSocket.OPEN) socket.close();
  if (chrome.exitCode === null) {
    chrome.kill("SIGTERM");
    await Promise.race([chromeExited, delay(2_000)]);
  }
  if (chrome.exitCode === null) {
    chrome.kill("SIGKILL");
    await chromeExited;
  }
  for (let attempt = 0; attempt < 5; attempt += 1) {
    try {
      await rm(profile, { recursive: true, force: true });
      break;
    } catch (error) {
      if (attempt === 4) throw error;
      await delay(100);
    }
  }
}
