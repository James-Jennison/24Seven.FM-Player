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
    "/features/",
    "/development/",
    "/testing/",
    "/product-testing/",
    "/roadmap/",
    "/resources/",
    "/privacy/",
    "/404.html",
  ];

  for (const viewport of [
    { width: 390, height: 844, label: "narrow mobile" },
    { width: 768, height: 1024, label: "tablet" },
    { width: 1440, height: 1000, label: "laptop" },
    { width: 1920, height: 1080, label: "large desktop" },
  ]) {
    await send("Emulation.setDeviceMetricsOverride", {
      width: viewport.width,
      height: viewport.height,
      deviceScaleFactor: 1,
      mobile: viewport.width < 600,
    });
    const testedRoutes = viewport.width === 390 ? routes : ["/", "/product-testing/", "/privacy/"];
    for (const route of testedRoutes) {
      await navigate(route);
      await evaluate("window.scrollTo(0, document.documentElement.scrollHeight)");
      await delay(120);
      const state = await evaluate(`(() => ({
        title: document.title,
        h1: document.querySelectorAll('h1').length,
        overflow: document.documentElement.scrollWidth > window.innerWidth + 1,
        missingImages: [...document.images].filter((image) => image.getAttribute('src') && (!image.complete || image.naturalWidth === 0)).map((image) => image.src),
        canonical: document.querySelector('link[rel="canonical"]')?.href ?? '',
        navigation: document.querySelectorAll('#project-navigation a').length
      }))()`);
      assert(state.title, `${viewport.label} ${route} has no title`);
      assert(state.h1 === 1, `${viewport.label} ${route} has ${state.h1} h1 elements`);
      assert(!state.overflow, `${viewport.label} ${route} has horizontal overflow`);
      assert(state.missingImages.length === 0, `${viewport.label} ${route} has missing images: ${state.missingImages.join(", ")}`);
      assert(state.canonical.startsWith("https://player.jamesjennison.net/"), `${route} has the wrong canonical URL`);
      assert(state.navigation === 8, `${route} does not expose all eight primary destinations`);
    }
  }

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

  await navigate("/product-testing/");
  const testingState = await evaluate(`(() => {
    const cases = document.querySelectorAll('.test-session-grid article[id^="pt-"]');
    const first = document.querySelector('.test-check-button');
    first.click();
    return { cases: cases.length, checked: first.getAttribute('aria-pressed'), stored: localStorage.getItem('project-test-checklist-v1') };
  })()`);
  assert(testingState.cases === 35, `Expected 35 test cases; found ${testingState.cases}`);
  assert(testingState.checked === "true", "Tester progress control did not update");
  assert(testingState.stored, "Tester progress was not stored locally");

  await navigate("/resources/");
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
      emailPublished: document.body.textContent.includes('24sevenplayer@')
    };
  })()`);
  assert(privacyState.visible > 0, "Privacy search returned no session results");
  assert(!privacyState.emailPublished, "The unapproved public email remains visible");

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

  await send("Emulation.setScriptExecutionDisabled", { value: true });
  await navigate("/");
  const noScript = await evaluate(`(() => ({
    h1: document.querySelectorAll('h1').length,
    navigation: document.querySelectorAll('#project-navigation a').length,
    projectCards: document.querySelectorAll('.portal-grid a').length
  }))()`);
  assert(noScript.h1 === 1 && noScript.navigation === 8 && noScript.projectCards === 6, "No-JavaScript fallback lost essential content");

  assert(browserErrors.length === 0, `Browser errors: ${browserErrors.join(" | ")}`);
  console.log("Validated four responsive viewports, nine routes, keyboard and pointer interactions, local-only state, reduced motion, forced colors, and no-JavaScript fallback.");
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
