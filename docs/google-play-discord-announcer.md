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
`.github/play-release-announcer-state.json`; it is committed only after Discord accepts the
announcement, preventing repeat posts on later scheduled runs.

Before enabling the hourly schedule, run **Announce Google Play releases** manually with
**bootstrap** selected. It records the releases already published to Alpha without posting them.
Only then set `GOOGLE_PLAY_RELEASE_ANNOUNCER_ENABLED` to `true`; later published Alpha releases
are announced once and recorded in the ledger. GitHub Actions must be allowed to push the ledger
commit to `main`; if branch protection blocks that push, configure a narrowly scoped bypass for
`github-actions[bot]` before enabling the scheduled workflow.

Google documents `PUBLISHED` as meaning that a release is available to users on its track:
https://developers.google.com/android-publisher/api-ref/rest/v3/applications.tracks.releases
