# Google Play release announcer

This observer announces only releases that the Google Play Developer API reports as
`RELEASE_LIFECYCLE_STATE_PUBLISHED`—available to users on the selected track. It does not
announce uploads, drafts, or releases that are merely in review or approved for manual publication.

The initial configured track is `alpha`, the current **Closed testing – Alpha** track. Do not add
`production` to `GOOGLE_PLAY_RELEASE_TRACKS` until the first production release has been approved
and is ready for announcement.

## Activation

The workflow is intentionally inert until the owner creates the following repository configuration:

- Repository variable `GOOGLE_PLAY_RELEASE_ANNOUNCER_ENABLED` set to `true`.
- Repository variable `GOOGLE_PLAY_WIF_PROVIDER`: the narrowly scoped Google Cloud Workload
  Identity Provider for this repository.
- Repository variable `GOOGLE_PLAY_WIF_SERVICE_ACCOUNT`: service account with read-only Google
  Play release access for `com.codeframe78.twentyfourseven.player`.
- Existing `DISCORD_WEBHOOK_URL` secret and `DISCORD_THREAD_ID` variable must remain configured.

The workflow requests the `androidpublisher` OAuth scope and uses workload identity federation, so
no long-lived Google service-account key is stored in GitHub. The release ledger lives in
`.github/play-release-announcer-state.json` on the dedicated
`google-play-release-ledger` branch. It is committed only after Discord accepts the announcement,
preventing repeat posts on later scheduled runs without bypassing `main` branch protection.

Before enabling the hourly schedule, run **Announce Google Play releases** manually with
**bootstrap** selected. It records the releases already published to Alpha without posting them.
Only then set `GOOGLE_PLAY_RELEASE_ANNOUNCER_ENABLED` to `true`; later published Alpha releases
are announced once and recorded in the ledger branch.

## Historical backfills

For an explicitly approved historical announcement, dispatch the workflow with a non-empty
`backfill_message`. This is deliberately manual and does not alter the release ledger: it cannot
mistake an old release for a newly published one, and the operator supplies the evidence-backed
message to avoid inventing release notes.

Google documents `PUBLISHED` as meaning that a release is available to users on its track:
https://developers.google.com/android-publisher/api-ref/rest/v3/applications.tracks.releases
