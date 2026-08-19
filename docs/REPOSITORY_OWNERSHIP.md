# Repository ownership

The repository split is a source-control boundary, not a deployment or public
website change.

- `James-Jennison/24Seven.FM-Player` owns the native Android application and
  publishes the reviewed tester-task contract.
- `James-Jennison/24Seven.FM-Website` owns all public website pages, assets,
  and browser-side Tester Hub behavior.
- `James-Jennison/24Seven.FM-Onboarding` owns the protected PHP onboarding
  runtime, coordinator workspace, tester portal, mail/archive workflow, and
  onboarding data lifecycle.

The legacy `privacy-site/` tree and associated website/onboarding scripts in
this repository are retained only as a recovery reference while the current
deployed artifact remains unchanged. Do not use them for new feature work or
future website/onboarding deployments. Future releases must use immutable
Website and Onboarding revisions and their composed-release contract.

Removing the recovery reference is an explicit later migration, after a
separately approved staging cutover and rollback verification. It is not part
of this repository split.
