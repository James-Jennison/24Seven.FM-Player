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
    const details = response.exceptionDetails;
    throw new Error(details.exception?.description ?? details.text ?? "Browser evaluation failed");
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

  const routes = ["/", "/stations/", "/features/", "/platforms/", "/product-testing/", "/privacy/", "/privacy/tv/", "/404.html"];
  const stationSites = ["https://streamingsoundtracks.com/", "https://1980s.fm/", "https://adagio.fm/", "https://death.fm/", "https://entranced.fm/"];
  const longestStationName = "StreamingSoundtracks.com";

  async function setViewport(width, height) {
    await send("Emulation.setDeviceMetricsOverride", { width, height, deviceScaleFactor: 1, mobile: width < 700 });
  }

  async function setMedia(features) {
    await send("Emulation.setEmulatedMedia", {
      features: Object.entries(features).map(([name, value]) => ({ name, value })),
    });
  }

  const pageAudit = `(() => {
    const visible = (node) => {
      const rect = node.getBoundingClientRect();
      const style = getComputedStyle(node);
      return rect.width > 0 && rect.height > 0 && style.visibility !== "hidden" && style.display !== "none";
    };
    const names = [...document.querySelectorAll("h2")].filter((node) => node.textContent.trim() === ${JSON.stringify(longestStationName)} && visible(node));
    const nameReport = names.map((node) => {
      const range = document.createRange();
      range.selectNodeContents(node);
      const lines = new Set([...range.getClientRects()].map((rect) => Math.round(rect.top))).size;
      const text = range.getBoundingClientRect();
      const box = node.closest(".door, .station-band").getBoundingClientRect();
      return { lines, inside: text.left >= box.left && text.right <= box.right };
    });
    const controls = [...document.querySelectorAll("button, input:not([type=checkbox]):not([type=radio]), a.button, .site-nav a")].filter(visible);
    return {
      title: document.title,
      headings: document.querySelectorAll("h1").length,
      navigation: document.querySelectorAll("#site-navigation a").length,
      overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      missingImages: [...document.images].filter((image) => !image.complete || image.naturalWidth === 0).map((image) => image.getAttribute("src")),
      retiredLinks: [...document.querySelectorAll("a[href]")].map((link) => link.getAttribute("href")).filter((href) => new RegExp("^/(dev|turnstile-test|roadmap|development|resources|testing)(/|$)").test(href)),
      undersized: controls.filter((node) => node.getBoundingClientRect().width < 24 || node.getBoundingClientRect().height < 24).map((node) => (node.textContent || node.getAttribute("aria-label") || node.tagName).trim().slice(0, 40)),
      nameReport,
    };
  })()`;

  // Every page at every width: core content, no sideways scrolling, and the
  // longest station name on one line inside its box.
  await setMedia({ "prefers-color-scheme": "dark" });
  const viewports = [
    { width: 360, height: 740, label: "narrow phone" },
    { width: 390, height: 844, label: "phone" },
    { width: 759, height: 1000, label: "below the phone boundary" },
    { width: 820, height: 1180, label: "tablet" },
    { width: 1239, height: 900, label: "widest rows layout" },
    { width: 1240, height: 900, label: "narrowest doors layout" },
    { width: 1399, height: 900, label: "below the wide doors layout" },
    { width: 1440, height: 900, label: "desktop" },
    { width: 1920, height: 1080, label: "wide desktop" },
  ];
  for (const viewport of viewports) {
    await setViewport(viewport.width, viewport.height);
    for (const route of routes) {
      await navigate(route);
      // Images below the first screen load only once they are scrolled to.
      await evaluate(`(async () => {
        const step = Math.max(200, Math.floor(window.innerHeight / 2));
        for (let y = 0; y <= document.documentElement.scrollHeight; y += step) {
          window.scrollTo(0, y);
          await new Promise((resolve) => setTimeout(resolve, 20));
        }
        const deadline = Date.now() + 3000;
        while (Date.now() < deadline && [...document.images].some((image) => !image.complete)) {
          await new Promise((resolve) => setTimeout(resolve, 50));
        }
        window.scrollTo(0, 0);
      })()`);
      const audit = await evaluate(pageAudit);
      const where = `${route} at ${viewport.width}px (${viewport.label})`;
      assert(audit.title, `Missing title on ${where}`);
      assert(audit.headings === 1, `Expected one h1 on ${where}; found ${audit.headings}`);
      assert(audit.navigation === 4, `Expected four primary links on ${where}; found ${audit.navigation}`);
      assert(audit.overflow <= 0, `${where} scrolls sideways by ${audit.overflow}px`);
      assert(audit.missingImages.length === 0, `${where} has missing images: ${audit.missingImages.join(", ")}`);
      assert(audit.retiredLinks.length === 0, `${where} links to a retired route: ${audit.retiredLinks.join(", ")}`);
      assert(audit.undersized.length === 0, `${where} has undersized controls: ${audit.undersized.join(", ")}`);
      for (const name of audit.nameReport) {
        assert(name.lines === 1, `${longestStationName} wraps onto ${name.lines} lines on ${where}`);
        assert(name.inside, `${longestStationName} leaves its box on ${where}`);
      }
      if (route === "/" || route === "/stations/") {
        assert(audit.nameReport.length === 1, `${longestStationName} is not shown on ${where}`);
      }
    }
  }

  // Home fits one desktop screen.
  for (const [width, height] of [[1440, 780], [1920, 950]]) {
    await setViewport(width, height);
    await navigate("/");
    const extra = await evaluate("document.documentElement.scrollHeight - window.innerHeight");
    assert(extra <= 0, `Home scrolls by ${extra}px in a ${width}x${height} window`);
  }

  // Theme follows the device until the visitor uses the toggle.
  await setViewport(1440, 900);
  await navigate("/");
  await evaluate("localStorage.removeItem('project-theme')");
  for (const scheme of ["dark", "light"]) {
    await setMedia({ "prefers-color-scheme": scheme });
    await navigate("/");
    const theme = await evaluate("document.documentElement.dataset.theme");
    assert(theme === scheme, `Home opened in ${theme} on a ${scheme} device`);
  }
  const liveChange = await evaluate(`(async () => {
    document.querySelector(".theme-toggle").click();
    return { theme: document.documentElement.dataset.theme, saved: localStorage.getItem("project-theme"), label: document.querySelector(".theme-toggle").getAttribute("aria-label") };
  })()`);
  assert(liveChange.theme === "dark" && liveChange.saved === "dark", "The theme toggle did not switch a light device to dark");
  assert(liveChange.label === "Switch to light mode", "The theme toggle is not labelled with its next action");
  await navigate("/stations/");
  assert((await evaluate("document.documentElement.dataset.theme")) === "dark", "The chosen theme did not carry to the next page");
  const background = await evaluate("getComputedStyle(document.body).backgroundColor");
  assert(background === "rgb(9, 7, 15)", `Dark mode background is ${background}`);
  await evaluate("localStorage.removeItem('project-theme')");
  await setMedia({ "prefers-color-scheme": "dark" });

  // Station doors on a desktop.
  await navigate("/");
  const doors = await evaluate(`(() => {
    const container = document.querySelector("[data-doors]");
    const items = [...container.querySelectorAll("[data-door]")];
    const state = () => items.map((door) => ({
      open: door.classList.contains("is-open"),
      expanded: door.querySelector(".door-open")?.getAttribute("aria-expanded"),
      linkVisible: door.querySelector(".door-link").getBoundingClientRect().width > 0,
      href: door.querySelector(".door-link").getAttribute("href"),
      width: Math.round(door.getBoundingClientRect().width),
    }));
    const before = state();
    items[3].querySelector(".door-open").click();
    const after = state();
    return { accordion: container.classList.contains("is-accordion"), before, after, focusInOpened: items[3].contains(document.activeElement) };
  })()`);
  assert(doors.accordion, "Station doors are not an accordion on a desktop");
  assert(doors.before.map((door) => door.open).join() === "true,false,false,false,false", "The first door is not the one open on arrival");
  assert(doors.after.map((door) => door.open).join() === "false,false,false,true,false", "Choosing a door did not open it and close the others");
  assert(doors.after[3].expanded === "true" && doors.after[0].expanded === "false", "Door buttons do not report their expanded state");
  assert(doors.after.every((door) => door.linkVisible === door.open), "Only the open door should show its station link");
  assert(doors.focusInOpened, "Focus did not move into the opened door");
  assert(doors.after.map((door) => door.href).join() === stationSites.join(), "Doors do not link to the five official station sites");

  // Station rows on a tablet and a phone.
  for (const [width, height] of [[820, 1180], [390, 844]]) {
    await setViewport(width, height);
    await navigate("/");
    const rows = await evaluate(`(() => {
      const container = document.querySelector("[data-doors]");
      return {
        accordion: container.classList.contains("is-accordion"),
        buttons: container.querySelectorAll(".door-open").length,
        links: [...container.querySelectorAll(".door-link")].map((link) => ({ href: link.getAttribute("href"), name: link.textContent.replace(/\\s+/g, " ").trim(), height: link.getBoundingClientRect().height })),
      };
    })()`);
    assert(!rows.accordion && rows.buttons === 0, `Station rows at ${width}px still carry door buttons`);
    assert(rows.links.map((link) => link.href).join() === stationSites.join(), `Station rows at ${width}px do not link to the official sites`);
    assert(rows.links.every((link) => link.height >= 44 && link.name.includes("official site")), `Station row links at ${width}px are too small or unnamed`);
  }

  // Menu on a phone.
  await setViewport(390, 844);
  await navigate("/features/");
  const menu = await evaluate(`(() => {
    const toggle = document.querySelector(".nav-toggle");
    const navigation = document.querySelector("#site-navigation");
    const shown = () => getComputedStyle(navigation).display !== "none";
    const closed = shown();
    toggle.click();
    const opened = shown();
    const expanded = toggle.getAttribute("aria-expanded");
    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", bubbles: true }));
    return { closed, opened, expanded, closedAgain: shown(), current: navigation.querySelector("[aria-current=page]")?.textContent.trim() };
  })()`);
  assert(!menu.closed && menu.opened && menu.expanded === "true" && !menu.closedAgain, "The phone menu does not open and close");
  assert(menu.current === "Features", "The current page is not marked in the menu");

  // Privacy notice search.
  await setViewport(1440, 900);
  await navigate("/privacy/");
  const privacy = await evaluate(`(() => {
    const field = document.querySelector("[data-privacy-query]");
    const count = () => Number(document.querySelector("[data-privacy-count]").textContent);
    const sections = () => [...document.querySelectorAll(".policy-section")];
    const all = count();
    field.value = "backup";
    field.dispatchEvent(new Event("input", { bubbles: true }));
    const filtered = count();
    const hidden = sections().filter((section) => section.hidden).length;
    document.querySelector("[data-privacy-reset]").click();
    return { all, filtered, hidden, restored: count(), links: document.querySelectorAll("[data-privacy-toc] a").length, updated: document.querySelector("[data-privacy-document]").textContent.includes("Last updated:") };
  })()`);
  assert(privacy.all === 5 && privacy.links === 5, `The privacy notice lists ${privacy.all} sections and ${privacy.links} links; expected five of each`);
  assert(privacy.filtered > 0 && privacy.filtered < privacy.all && privacy.hidden === privacy.all - privacy.filtered, "The privacy search did not narrow the notice");
  assert(privacy.restored === privacy.all, "Clearing the privacy search did not restore the notice");
  assert(privacy.updated, "The privacy notice is missing its update date");

  // Tester application steps.
  await navigate("/product-testing/");
  const application = await evaluate(`(() => {
    const form = document.querySelector("[data-alpha-tester-form]");
    const steps = [...form.querySelectorAll(":scope > [data-application-step]")];
    return { steps: steps.length, shown: steps.filter((step) => !step.hidden).length, progress: form.querySelectorAll(".tester-application-progress-step").length, action: form.getAttribute("action") };
  })()`);
  assert(application.steps === 6 && application.shown === 1 && application.progress === 6, "The tester application does not present six steps one at a time");
  assert(application.action === "/alpha-tester-interest.php", "The tester application posts to an unexpected address");

  // Reduced motion.
  await setMedia({ "prefers-color-scheme": "dark", "prefers-reduced-motion": "reduce" });
  await navigate("/");
  const motion = await evaluate("getComputedStyle(document.querySelector('[data-door]')).transitionDuration");
  assert(/^0s(, 0s)*$/.test(motion), `Doors still animate under reduced motion (${motion})`);
  await setMedia({ "prefers-color-scheme": "dark" });

  // Without JavaScript every page still shows its content and links.
  await send("Emulation.setScriptExecutionDisabled", { value: true });
  for (const [width, height] of [[390, 844], [1440, 900]]) {
    await setViewport(width, height);
    await navigate("/");
    await send("Emulation.setScriptExecutionDisabled", { value: false });
    const plain = await evaluate(`(() => ({
      scripted: document.documentElement.classList.contains("js"),
      links: [...document.querySelectorAll(".door-link")].filter((link) => link.getBoundingClientRect().width > 0).length,
      navigation: getComputedStyle(document.querySelector("#site-navigation")).display !== "none",
      toggles: [...document.querySelectorAll(".theme-toggle, .nav-toggle")].filter((node) => node.getBoundingClientRect().width > 0).length,
      overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    }))()`);
    await send("Emulation.setScriptExecutionDisabled", { value: true });
    assert(!plain.scripted, "The no-JavaScript check ran with scripts enabled");
    assert(plain.links === 5, `Without JavaScript only ${plain.links} station links show at ${width}px`);
    assert(plain.navigation, `Without JavaScript the navigation is hidden at ${width}px`);
    assert(plain.toggles === 0, `Without JavaScript a dead control shows at ${width}px`);
    assert(plain.overflow <= 0, `Without JavaScript Home scrolls sideways at ${width}px`);
  }
  await send("Emulation.setScriptExecutionDisabled", { value: false });

  assert(browserErrors.length === 0, `Browser errors: ${browserErrors.join(" | ")}`);
  console.log(`Validated ${routes.length} pages at ${viewports.length} widths, the one-screen Home, device-led theme, station doors and rows, the phone menu, privacy search, tester application steps, reduced motion, and the no-JavaScript fallback.`);
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
