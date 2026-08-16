#!/usr/bin/env node

import { spawn } from "node:child_process";
import { constants } from "node:fs";
import { access } from "node:fs/promises";
import { createServer } from "node:net";
import { setTimeout as delay } from "node:timers/promises";

const baseUrl = (process.argv[2] ?? "").replace(/\/$/, "");
if (!baseUrl) {
  process.stderr.write("usage: node scripts/test-project-site-firefox.mjs <site-url>\n");
  process.exit(2);
}

async function executable(candidates, description) {
  for (const candidate of candidates.filter(Boolean)) {
    try {
      await access(candidate, constants.X_OK);
      return candidate;
    } catch {
      // Try the next candidate.
    }
  }
  throw new Error(`${description} is required for the Firefox audit`);
}

async function freePort() {
  const server = createServer();
  await new Promise((resolve, reject) =>
    server.once("error", reject).listen(0, "127.0.0.1", resolve),
  );
  const port = server.address().port;
  await new Promise((resolve) => server.close(resolve));
  return port;
}

const geckodriver = await executable(
  [process.env.PROJECT_SITE_GECKODRIVER, "/snap/bin/geckodriver", "/usr/bin/geckodriver"],
  "geckodriver",
);
const port = await freePort();
const driver = spawn(geckodriver, ["--host", "127.0.0.1", "--port", String(port)], {
  stdio: ["ignore", "ignore", "pipe"],
});
let driverError = "";
driver.stderr.on("data", (chunk) => {
  driverError = `${driverError}${chunk}`.slice(-5000);
});

async function request(path, options = {}) {
  const response = await fetch(`http://127.0.0.1:${port}${path}`, {
    ...options,
    headers: { "content-type": "application/json", ...options.headers },
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || body.value?.error) {
    throw new Error(body.value?.message || `WebDriver request failed (${response.status})`);
  }
  return body.value;
}

async function waitForDriver() {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    try {
      await request("/status");
      return;
    } catch {
      await delay(100);
    }
  }
  throw new Error(`geckodriver did not start: ${driverError}`);
}

let sessionId;
try {
  await waitForDriver();
  const firefoxOptions = { args: ["-headless"] };
  if (process.env.PROJECT_SITE_FIREFOX) firefoxOptions.binary = process.env.PROJECT_SITE_FIREFOX;
  const session = await request("/session", {
    method: "POST",
    body: JSON.stringify({
      capabilities: {
        alwaysMatch: {
          browserName: "firefox",
          "moz:firefoxOptions": firefoxOptions,
          pageLoadStrategy: "normal",
        },
      },
    }),
  });
  sessionId = session.sessionId;

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
  const results = [];
  const failures = [];

  for (const [width, height] of [
    [500, 800],
    [768, 1024],
    [1024, 768],
    [1440, 1000],
  ]) {
    await request(`/session/${sessionId}/window/rect`, {
      method: "POST",
      body: JSON.stringify({ width, height, x: 0, y: 0 }),
    });
    const testedRoutes = width === 500 ? routes : ["/", "/product-testing/", "/privacy/"];
    for (const route of testedRoutes) {
      await request(`/session/${sessionId}/url`, {
        method: "POST",
        body: JSON.stringify({ url: `${baseUrl}${route}` }),
      });
      await delay(250);
      await request(`/session/${sessionId}/execute/async`, {
        method: "POST",
        body: JSON.stringify({
          script: `
            const done = arguments[arguments.length - 1];
            let y = 0;
            const step = Math.max(200, Math.floor(window.innerHeight / 2));
            const timer = setInterval(() => {
              window.scrollTo(0, y);
              y += step;
              if (y > document.documentElement.scrollHeight + step) {
                clearInterval(timer);
                done();
              }
            }, 25);`,
          args: [],
        }),
      });
      await delay(250);
      const result = await request(`/session/${sessionId}/execute/sync`, {
        method: "POST",
        body: JSON.stringify({
          script: `
            const visible = (node) => {
              const rect = node.getBoundingClientRect();
              const style = getComputedStyle(node);
              return rect.width > 0 && rect.height > 0 && style.visibility !== "hidden" && style.display !== "none";
            };
            const controls = [...document.querySelectorAll("button,input,summary,a.button,#project-navigation a")].filter(visible);
            return {
              title: document.title,
              h1: document.querySelectorAll("h1").length,
              navigation: document.querySelectorAll("#project-navigation a").length,
              clientWidth: document.documentElement.clientWidth,
              scrollWidth: document.documentElement.scrollWidth,
              missingImages: [...document.images].filter((image) => image.getAttribute("src") && (!image.complete || image.naturalWidth === 0)).map((image) => image.src),
              undersized: controls.filter((node) => getComputedStyle(node).display !== "inline" && (node.getBoundingClientRect().width < 24 || node.getBoundingClientRect().height < 24)).map((node) => (node.textContent || node.getAttribute("aria-label") || node.tagName).trim().slice(0, 50)),
              userAgent: navigator.userAgent,
            };`,
          args: [],
        }),
      });
      results.push({ width, height, route, ...result });
      if (!result.title || result.h1 !== 1 || result.navigation !== 8) {
        failures.push(`${width}px ${route} is missing core content`);
      }
      if (result.scrollWidth > result.clientWidth) {
        failures.push(`${width}px ${route} overflows by ${result.scrollWidth - result.clientWidth}px`);
      }
      if (result.missingImages.length) {
        failures.push(`${width}px ${route} has missing images: ${result.missingImages.join(", ")}`);
      }
      if (result.undersized.length) {
        failures.push(`${width}px ${route} has undersized controls: ${result.undersized.join(", ")}`);
      }
    }
  }

  const screenshot = await request(`/session/${sessionId}/screenshot`);
  const screenshotBytes = Math.floor((screenshot.length * 3) / 4);
  if (screenshotBytes < 10_000) {
    failures.push(`Firefox screenshot was unexpectedly small (${screenshotBytes} bytes)`);
  }

  process.stdout.write(
    `${JSON.stringify({ browser: "firefox", baseUrl, results, screenshotBytes }, null, 2)}\n`,
  );
  if (failures.length) {
    failures.forEach((failure) => process.stderr.write(`firefox audit: ${failure}\n`));
    process.exitCode = 1;
  } else {
    process.stdout.write("firefox audit: pass\n");
  }
} finally {
  if (sessionId) {
    await request(`/session/${sessionId}`, { method: "DELETE", body: "{}" }).catch(() => {});
  }
  driver.kill("SIGTERM");
  await Promise.race([new Promise((resolve) => driver.once("exit", resolve)), delay(2000)]);
}
