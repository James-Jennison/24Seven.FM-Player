# Deployment — official-app wording, October 13, 2026

- **Live since:** October 13, 2026 at 18:11:22 UTC (atomic exchange).
- **Source:** `codex/site-official-app` at `e19d5e2364b2635d170b769d4d91a71308e8090b`; the site lineage
  `codex/site-c2-redesign` carries the same site content as the squash merge `24bf777da4d1e3ac200e3abfab7d82c5ae02d8e0` (pull request #127).
- **Why:** 24seven.FM made the Player its official Android app on October 13, 2026. The footer, site
  description, tester-program copy and the privacy notice now carry the owner's chosen statement; the privacy
  notice is pinned at `24bf777` by `docs/privacy-reviews/native-privacy-24bf777.md`.
- **Build:** `scripts/validate-project-site.sh` on gthost-build01 in `~/build/claude-site-official`; 58 files;
  artifact manifest SHA-256 `3ff0de72168257ad83e45b01156bd9b90faad2ed14a718c976a4f9618ccf3e85`.
- **Verification before the swap:** both `24sevenfmplayer.com` virtual hosts (80 and 443) map to
  `/home/jamesjen/public_html/24sevenfmplayer.com`; the live tree hashed to the October 9 artifact
  (`3141b5b0…`); the copy in the unserved home staging area and the sibling staging directory both hashed to the
  new manifest; ownership `jamesjen:nobody`, directories 755, files 644.
- **Swap:** `renameat2(RENAME_EXCHANGE)` between the live root and the staging directory.
- **Verification after the swap:** live tree hashes to the new manifest; public HTTPS returns 200 for `/`,
  `/stations/`, `/features/`, `/platforms/`, `/privacy/`, `/privacy/tv/`, `/product-testing/`, `/cast/` and 404
  for `/dev/`; eight served pages are byte-identical to the deployed files; the home and privacy pages carry the
  official statement, the privacy page says "October 13, 2026", and no stale wording or `mailto:` link is served.
- **Rollback:** `/home/jamesjen/site-rollbacks/24sevenfmplayer.com/24sevenfmplayer.com.rollback-privacy-1.0.2-4888510-20261013T181122Z`
  (manifest `3141b5b0…`); its former sibling address returns 404.
