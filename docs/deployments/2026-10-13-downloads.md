# Deployment — published test build under /downloads/, October 13, 2026

- **Live since:** October 13, 2026 at 21:26:49 UTC (atomic exchange).
- **Source:** `codex/site-downloads` at `d851bb75a3fdfe64c8849ccdc0b7e2f2cf9f553b` (pull request #132).
- **Why:** the owner asked for the current debug APK to be published on the Player website. The validator
  forbids APKs inside the reviewed artifact and the atomic swap replaces the whole document root, so published
  builds live in `/home/jamesjen/site-downloads/24sevenfmplayer.com/downloads/` (APK, `.sha256`, `index.html`)
  and are copied into the staging directory as `downloads/` before every swap. The `.htaccess` serves `.apk`
  as `application/vnd.android.package-archive` attachments with `Cache-Control: no-store` and
  `X-Robots-Tag: noindex, nofollow`; the directory index carries `noindex` too.
- **Published build:** `24seven-player-debug-plain-sst-logo-063e125.apk`, 30,025,257 bytes, SHA-256
  `113f365e93437e548516e2d20888cae667a587de5431461cfd3b4838c130c0f1`, debug-signed from app commit `063e125`
  (official wording, launch screen, plain StreamingSoundtracks.com logo). It installs beside the Play release as
  the separate debug package.
- **Build:** `scripts/validate-project-site.sh` on gthost-build01 in `~/build/claude-site-official`; 58 files;
  artifact manifest SHA-256 `e8b682c9b6ac04e0a40d5cdd6c67b324fee66e96310818bff86871f797f1ad2a`.
- **Verification:** staging directory (`jamesjen:nobody`, 755/644) with `downloads/` excluded hashed to the
  manifest; no symlinks; `renameat2(RENAME_EXCHANGE)`. After the swap the APK URL returned 200 with the type,
  disposition, no-store and noindex headers (`cf-cache-status: BYPASS`) and bytes matching the SHA-256; the
  home, stations, features, platforms, product-testing, privacy, privacy/tv, cast, 404, robots and sitemap files
  are byte-identical to the artifact; `/dev/` and a missing APK name return 404.
- **Rollback:** `/home/jamesjen/site-rollbacks/24sevenfmplayer.com/24sevenfmplayer.com.rollback-sst-logo-a5e4051-20261013T212649Z`
  (previous live tree, manifest `6da5ad52…`); its former sibling address returns 404.
