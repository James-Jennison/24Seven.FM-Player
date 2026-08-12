const EXPECTED_ORIGIN = "https://player.jamesjennison.net";
const EXPECTED_HOSTNAME = "player.jamesjennison.net";
const EXPECTED_ACTION = "alpha_tester_interest";
const MAX_REQUEST_BYTES = 16 * 1024;

const FIELD_LIMITS = {
  name: 100,
  email: 254,
  country: 80,
  device: 160,
  androidVersion: 48,
  experience: 1200,
};

const responseHeaders = {
  "Cache-Control": "no-store",
  "Content-Security-Policy": "default-src 'none'; frame-ancestors 'none'; base-uri 'none'",
  "Content-Type": "application/json; charset=UTF-8",
  "Referrer-Policy": "no-referrer",
  "X-Content-Type-Options": "nosniff",
};

function json(body, status) {
  return new Response(JSON.stringify(body), { status, headers: responseHeaders });
}

function clean(value, limit) {
  if (typeof value !== "string") return "";
  return value.replace(/[\u0000-\u001F\u007F]/g, " ").replace(/\s+/g, " ").trim().slice(0, limit);
}

function validEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) && value.length <= FIELD_LIMITS.email;
}

function selectedValues(form, name, allowed) {
  return form.getAll(name)
    .filter((value) => typeof value === "string" && allowed.has(value))
    .slice(0, allowed.size);
}

function textEmail(application) {
  const lines = [
    "24Seven.FM Player Alpha tester-interest application",
    "",
    `Name: ${application.name}`,
    `Google Play account email: ${application.email}`,
    `Country or region: ${application.country || "Not provided"}`,
    `Device: ${application.device}`,
    `Android version: ${application.androidVersion}`,
    `Testing interests: ${application.interests.join(", ") || "General testing"}`,
    `Prior testing experience: ${application.experience || "Not provided"}`,
    "",
    "The applicant agreed to be contacted about 24Seven.FM Player internal testing.",
    "Do not reply with station credentials or other authentication secrets.",
  ];
  return lines.join("\n");
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== "/api/alpha-tester-interest") return json({ ok: false, message: "Not found." }, 404);
    if (request.method !== "POST") return json({ ok: false, message: "Method not allowed." }, 405);
    if (request.headers.get("Origin") !== EXPECTED_ORIGIN) return json({ ok: false, message: "Request could not be verified." }, 403);

    const contentLength = Number(request.headers.get("Content-Length") || 0);
    if (!Number.isFinite(contentLength) || contentLength > MAX_REQUEST_BYTES) {
      return json({ ok: false, message: "The application is too large." }, 413);
    }

    let form;
    try {
      form = await request.formData();
    } catch {
      return json({ ok: false, message: "Submit the form again." }, 400);
    }

    if (clean(form.get("company"), 100)) return json({ ok: true, message: "Thanks for your interest." }, 200);

    const token = clean(form.get("cf-turnstile-response"), 2048);
    if (!token) return json({ ok: false, message: "Complete the verification check first." }, 400);

    let verification;
    try {
      const result = await fetch("https://challenges.cloudflare.com/turnstile/v0/siteverify", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          secret: env.TURNSTILE_SECRET,
          response: token,
          remoteip: request.headers.get("CF-Connecting-IP") || "",
        }),
      });
      verification = result.ok ? await result.json() : null;
    } catch {
      verification = null;
    }
    if (!verification || verification.success !== true || verification.action !== EXPECTED_ACTION || verification.hostname !== EXPECTED_HOSTNAME) {
      return json({ ok: false, message: "Verification expired or failed. Please try again." }, 403);
    }

    const application = {
      name: clean(form.get("name"), FIELD_LIMITS.name),
      email: clean(form.get("email"), FIELD_LIMITS.email),
      country: clean(form.get("country"), FIELD_LIMITS.country),
      device: clean(form.get("device"), FIELD_LIMITS.device),
      androidVersion: clean(form.get("androidVersion"), FIELD_LIMITS.androidVersion),
      interests: selectedValues(form, "interests", new Set(["Playback", "Foldable and tablet", "Accessibility", "Account and community", "General testing"])),
      experience: clean(form.get("experience"), FIELD_LIMITS.experience),
    };
    if (!application.name || !validEmail(application.email) || !application.device || !application.androidVersion || form.get("consent") !== "yes") {
      return json({ ok: false, message: "Complete the required fields and consent checkbox." }, 400);
    }

    try {
      await env.EMAIL.send({
        to: env.RECIPIENT_EMAIL,
        from: "alpha-testing@jamesjennison.net",
        replyTo: application.email,
        subject: `Alpha tester interest: ${application.name}`,
        text: textEmail(application),
      });
    } catch {
      return json({ ok: false, message: "The application could not be delivered. Please try again later." }, 503);
    }

    return json({ ok: true, message: "Thanks — your tester-interest application was sent to the project coordinator." }, 200);
  },
};
