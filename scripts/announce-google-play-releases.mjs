#!/usr/bin/env node

import { readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const PUBLISHED = "RELEASE_LIFECYCLE_STATE_PUBLISHED";

export function releaseKey(release) {
  const versionCodes = (release.activeArtifacts ?? [])
    .map(({ versionCode }) => String(versionCode))
    .sort((left, right) => Number(left) - Number(right));
  return [release.track, release.releaseName || "unnamed", versionCodes.join(",")].join(":");
}

export function publishedReleases(releases, knownReleaseKeys) {
  return releases
    .filter((release) => release.releaseLifecycleState === PUBLISHED)
    .filter((release) => !knownReleaseKeys.has(releaseKey(release)));
}

export function announcementsFor(newlyPublishedReleases, bootstrap) {
  return bootstrap ? [] : newlyPublishedReleases;
}

export function releaseNotesFromMarkdown(markdown) {
  const section = markdown.match(/^## What's new\s*$([\s\S]*?)(?=^##\s|$(?![\s\S]))/im);
  if (!section) return [];

  return [...section[1].matchAll(/^\s*-\s+(.+)$/gm)]
    .map((match) => match[1].trim())
    .filter(Boolean);
}

async function releaseNotesFor(release) {
  const releaseName = release.releaseName ?? "";
  const documentName = releaseName.match(/\(([^()]+)\)$/)?.[1].trim() || releaseName;
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(documentName)) {
    throw new Error(`Release ${JSON.stringify(releaseName)} has no safe release-notes document name.`);
  }

  let releaseDocument;
  try {
    releaseDocument = await readFile(resolve("docs/releases", `${documentName}.md`), "utf8");
  } catch (error) {
    if (error?.code === "ENOENT") {
      throw new Error(`Missing authoritative release notes for ${documentName}.`);
    }
    throw error;
  }

  const notes = releaseNotesFromMarkdown(releaseDocument);
  if (notes.length === 0) {
    throw new Error(`Authoritative release notes for ${documentName} have no What's new entries.`);
  }
  return notes;
}

export function messageFor(release, releaseNotes = []) {
  const versionCodes = (release.activeArtifacts ?? [])
    .map(({ versionCode }) => `\`${versionCode}\``)
    .join(", ");
  const releaseName = release.releaseName ? `Release: \`${release.releaseName}\`` : "";

  return [
    "🚀 **24Seven.FM Player update published**",
    `Track: **${release.track === "alpha" ? "Closed testing – Alpha" : release.track}**`,
    `Version code${(release.activeArtifacts ?? []).length === 1 ? "" : "s"}: ${versionCodes}`,
    releaseName,
    ...(releaseNotes.length === 0 ? [] : ["**What's new**", ...releaseNotes.map((note) => `- ${note}`)]),
    "Available to testers on Google Play.",
  ].filter(Boolean).join("\n");
}

export function updatedState(previousState, releases, now) {
  const known = { ...(previousState.announced ?? {}) };
  for (const release of releases) {
    known[releaseKey(release)] = {
      announcedAt: now,
      track: release.track,
      releaseName: release.releaseName || null,
      versionCodes: (release.activeArtifacts ?? []).map(({ versionCode }) => String(versionCode)),
    };
  }
  return { schemaVersion: 1, announced: known };
}

async function getTrackReleases({ accessToken, packageName, track }) {
  const url = new URL(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/tracks/${encodeURIComponent(track)}/releases`,
  );
  const response = await fetch(url, {
    headers: { authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) {
    throw new Error(`Google Play release query for track ${track} failed with HTTP ${response.status}.`);
  }
  const body = await response.json();
  return body.releases ?? [];
}

async function main(environment) {
  const packageName = environment.GOOGLE_PLAY_PACKAGE_NAME;
  const tracks = (environment.GOOGLE_PLAY_TRACKS || "alpha")
    .split(",")
    .map((track) => track.trim())
    .filter(Boolean);
  const statePath = resolve(environment.PLAY_RELEASE_STATE_FILE || ".github/play-release-announcer-state.json");
  const bootstrap = environment.PLAY_RELEASE_BOOTSTRAP === "true";

  if (!packageName) throw new Error("GOOGLE_PLAY_PACKAGE_NAME is required.");
  if (!environment.GOOGLE_PLAY_ACCESS_TOKEN) throw new Error("GOOGLE_PLAY_ACCESS_TOKEN is required.");
  if (tracks.length === 0) throw new Error("At least one Google Play track is required.");

  const previousState = JSON.parse(await readFile(statePath, "utf8"));
  const knownReleaseKeys = new Set(Object.keys(previousState.announced ?? {}));
  const releases = (await Promise.all(tracks.map((track) => getTrackReleases({
    accessToken: environment.GOOGLE_PLAY_ACCESS_TOKEN,
    packageName,
    track,
  })))).flat();
  const newlyPublishedReleases = publishedReleases(releases, knownReleaseKeys);
  const announcements = announcementsFor(newlyPublishedReleases, bootstrap);
  const nextState = updatedState(previousState, newlyPublishedReleases, new Date().toISOString());

  await writeFile(statePath, `${JSON.stringify(nextState, null, 2)}\n`);

  if (environment.GITHUB_OUTPUT) {
    const messages = (await Promise.all(announcements.map(async (release) => (
      messageFor(release, await releaseNotesFor(release))
    )))).join("\n\n");
    await writeFile(environment.GITHUB_OUTPUT, [
      `count=${announcements.length}`,
      `state_changed=${newlyPublishedReleases.length !== 0}`,
      "message<<PLAY_RELEASE_ANNOUNCEMENT",
      messages,
      "PLAY_RELEASE_ANNOUNCEMENT",
    ].join("\n") + "\n", { flag: "a" });
  } else {
    process.stdout.write(`${JSON.stringify({
      bootstrap,
      count: announcements.length,
      stateChanged: newlyPublishedReleases.length !== 0,
      messages: announcements.map(messageFor),
    }, null, 2)}\n`);
  }
}

if (process.argv[1] === new URL(import.meta.url).pathname) {
  main(process.env).catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
