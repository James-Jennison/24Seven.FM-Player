import assert from "node:assert/strict";
import test from "node:test";
import {
  announcementsFor,
  messageFor,
  publishedReleases,
  releaseKey,
  updatedState,
} from "../announce-google-play-releases.mjs";

const publishedAlpha = {
  releaseName: "0.1.0-alpha04",
  track: "alpha",
  activeArtifacts: [{ versionCode: 7 }],
  releaseLifecycleState: "RELEASE_LIFECYCLE_STATE_PUBLISHED",
};

test("selects only new releases that are available to users", () => {
  const draft = { ...publishedAlpha, releaseLifecycleState: "RELEASE_LIFECYCLE_STATE_DRAFT" };
  assert.deepEqual(publishedReleases([publishedAlpha, draft], new Set()), [publishedAlpha]);
  assert.deepEqual(publishedReleases([publishedAlpha], new Set([releaseKey(publishedAlpha)])), []);
});

test("formats the closed Alpha announcement without mentions", () => {
  assert.equal(
    messageFor(publishedAlpha),
    "🚀 **24Seven.FM Player update published**\nTrack: **Closed testing – Alpha**\nVersion code: `7`\nRelease: `0.1.0-alpha04`\nAvailable to testers on Google Play.",
  );
});

test("bootstrap records an existing release without announcing it", () => {
  assert.deepEqual(announcementsFor([publishedAlpha], true), []);
  assert.deepEqual(announcementsFor([publishedAlpha], false), [publishedAlpha]);
});

test("adds release identity to the durable ledger", () => {
  const state = updatedState({ schemaVersion: 1, announced: {} }, [publishedAlpha], "2026-08-19T20:00:00.000Z");
  assert.deepEqual(state.announced[releaseKey(publishedAlpha)], {
    announcedAt: "2026-08-19T20:00:00.000Z",
    track: "alpha",
    releaseName: "0.1.0-alpha04",
    versionCodes: ["7"],
  });
});
