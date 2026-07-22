#!/usr/bin/env python3
"""Validate the deployable 24Seven.FM Player static-site artifact."""

from __future__ import annotations

import json
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlparse


PRODUCTION_ORIGIN = "https://player.jamesjennison.net"
EXPECTED_PAGES = {
    "/": "index.html",
    "/features/": "features/index.html",
    "/development/": "development/index.html",
    "/testing/": "testing/index.html",
    "/product-testing/": "product-testing/index.html",
    "/roadmap/": "roadmap/index.html",
    "/resources/": "resources/index.html",
    "/privacy/": "privacy/index.html",
    "/404.html": "404.html",
}
REQUIRED_FILES = {
    ".htaccess",
    "assets/privacy.css",
    "assets/project.css",
    "assets/project.js",
    "assets/theme-init.js",
    "assets/project/app-icon.png",
    "assets/project/feature-graphic.png",
    "assets/project/screenshots/more.png",
    "assets/project/screenshots/player.png",
    "assets/project/screenshots/queue.png",
    "assets/project/screenshots/stations.png",
    "assets/project/screenshots/tablet.png",
    "robots.txt",
    "sitemap.xml",
    "site.webmanifest",
}
FORBIDDEN_TEXT = {
    "codeframe78.github.io",
    "github.com/codeframe78/24Seven.FM-Player",
    "github.com/users/codeframe78",
    "24sevenplayer@jamesjennison.net",
    "mailto:",
    "Sol High",
    "Terra High",
}
FORBIDDEN_PARTS = {".git", ".env", ".jekyll-cache", ".sass-cache", "node_modules"}
FORBIDDEN_SUFFIXES = {".aab", ".apk", ".env", ".jks", ".keystore", ".kt", ".log", ".map", ".md", ".pem", ".properties", ".yaml", ".yml"}
SECRET_MARKERS = {
    "-----BEGIN PRIVATE KEY-----",
    "-----BEGIN OPENSSH PRIVATE KEY-----",
    "github_pat_",
    "ghp_",
    "sk-proj-",
}


class DocumentAudit(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: set[str] = set()
        self.hrefs: list[str] = []
        self.sources: list[str] = []
        self.meta: dict[str, str] = {}
        self.canonical: str | None = None
        self.manifest: str | None = None
        self.lang: str | None = None
        self.title_depth = 0
        self.title_text: list[str] = []
        self.h1_count = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        identifier = values.get("id")
        if identifier:
            self.ids.add(identifier)
        if tag == "html":
            self.lang = values.get("lang")
        if tag == "a" and values.get("href") is not None:
            self.hrefs.append(values["href"] or "")
        if tag in {"img", "script"} and values.get("src"):
            self.sources.append(values["src"] or "")
        if tag == "link" and values.get("href"):
            relationships = (values.get("rel") or "").split()
            if "canonical" in relationships:
                self.canonical = values["href"]
            if "manifest" in relationships:
                self.manifest = values["href"]
        if tag == "meta" and values.get("content") is not None:
            key = values.get("name") or values.get("property")
            if key:
                self.meta[key] = values["content"] or ""
        if tag == "title":
            self.title_depth += 1
        if tag == "h1":
            self.h1_count += 1

    def handle_endtag(self, tag: str) -> None:
        if tag == "title" and self.title_depth:
            self.title_depth -= 1

    def handle_data(self, data: str) -> None:
        if self.title_depth:
            self.title_text.append(data)

    @property
    def title(self) -> str:
        return "".join(self.title_text).strip()


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def output_path_for_url(root: Path, path: str) -> Path:
    decoded = unquote(path)
    if decoded == "/":
        return root / "index.html"
    if decoded.endswith("/"):
        return root / decoded.lstrip("/") / "index.html"
    return root / decoded.lstrip("/")


def audit() -> int:
    repository_root = Path(__file__).resolve().parent.parent
    artifact_root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else repository_root / "_site"
    failures: list[str] = []

    if not artifact_root.is_dir():
        print(f"Artifact does not exist: {artifact_root}", file=sys.stderr)
        return 1

    files = sorted(path for path in artifact_root.rglob("*") if path.is_file())
    relative_files = {path.relative_to(artifact_root).as_posix() for path in files}
    expected_html_files = set(EXPECTED_PAGES.values())
    actual_html_files = {path for path in relative_files if path.endswith(".html")}
    unexpected_html_files = sorted(actual_html_files - expected_html_files)
    if unexpected_html_files:
        fail(f"Unexpected HTML files: {', '.join(unexpected_html_files)}", failures)

    for path in artifact_root.rglob("*"):
        relative = path.relative_to(artifact_root)
        if path.is_symlink():
            fail(f"Symlink is not allowed in the artifact: {relative}", failures)
        if any(part in FORBIDDEN_PARTS or part.startswith(".env") for part in relative.parts):
            fail(f"Forbidden artifact path: {relative}", failures)
        if path.is_file() and path.suffix.lower() in FORBIDDEN_SUFFIXES:
            fail(f"Forbidden artifact file type: {relative}", failures)

    expected_files = expected_html_files | REQUIRED_FILES
    missing_files = sorted(expected_files - relative_files)
    if missing_files:
        fail(f"Missing required files: {', '.join(missing_files)}", failures)

    html_documents: dict[Path, DocumentAudit] = {}
    for path in files:
        if path.stat().st_size > 8 * 1024 * 1024:
            fail(f"Artifact file exceeds 8 MiB: {path.relative_to(artifact_root)}", failures)
        if path.suffix.lower() not in {".html", ".css", ".js", ".xml", ".txt", ".json", ".webmanifest"}:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for marker in FORBIDDEN_TEXT:
            if marker.lower() in text.lower():
                fail(f"Forbidden public marker {marker!r} in {path.relative_to(artifact_root)}", failures)
        for marker in SECRET_MARKERS:
            if marker in text:
                fail(f"Secret marker {marker!r} in {path.relative_to(artifact_root)}", failures)
        if "http://" in text.lower() and 'xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"' not in text:
            fail(f"Insecure HTTP URL in {path.relative_to(artifact_root)}", failures)
        if path.suffix.lower() == ".html":
            parser = DocumentAudit()
            parser.feed(text)
            html_documents[path] = parser

    for route, relative_path in EXPECTED_PAGES.items():
        path = artifact_root / relative_path
        document = html_documents.get(path)
        if document is None:
            continue
        expected_canonical = f"{PRODUCTION_ORIGIN}{route}"
        if document.lang != "en":
            fail(f"Missing lang=en on {route}", failures)
        if not document.title:
            fail(f"Missing document title on {route}", failures)
        if document.h1_count != 1:
            fail(f"Expected one h1 on {route}; found {document.h1_count}", failures)
        if document.canonical != expected_canonical:
            fail(f"Canonical mismatch on {route}: {document.canonical!r}", failures)
        if document.meta.get("og:url") != expected_canonical:
            fail(f"Open Graph URL mismatch on {route}", failures)
        for key in ("description", "robots", "og:title", "og:description", "og:image", "twitter:card"):
            if not document.meta.get(key):
                fail(f"Missing {key} metadata on {route}", failures)
        if document.manifest != "/site.webmanifest":
            fail(f"Manifest link mismatch on {route}: {document.manifest!r}", failures)
        if route == "/404.html" and "noindex" not in document.meta.get("robots", ""):
            fail("The 404 page must be noindex", failures)

    for source_path, document in html_documents.items():
        source_route = "/" + source_path.relative_to(artifact_root).as_posix()
        for raw_reference in document.hrefs + document.sources:
            if not raw_reference or raw_reference.startswith(("data:", "javascript:")):
                continue
            parsed = urlparse(raw_reference)
            if parsed.scheme in {"http", "https"} and parsed.netloc != "player.jamesjennison.net":
                continue
            if parsed.scheme and parsed.scheme not in {"http", "https"}:
                fail(f"Unsupported URL scheme in {source_route}: {raw_reference}", failures)
                continue
            target_path = parsed.path
            if not target_path:
                target_file = source_path
            elif target_path.startswith("/"):
                target_file = output_path_for_url(artifact_root, target_path)
            else:
                target_file = (source_path.parent / target_path).resolve()
            try:
                target_file.relative_to(artifact_root)
            except ValueError:
                fail(f"Reference escapes artifact in {source_route}: {raw_reference}", failures)
                continue
            if not target_file.exists():
                fail(f"Broken internal reference in {source_route}: {raw_reference}", failures)
                continue
            if parsed.fragment and target_file.suffix.lower() == ".html":
                target_document = html_documents.get(target_file)
                if target_document is None or unquote(parsed.fragment) not in target_document.ids:
                    fail(f"Broken fragment in {source_route}: {raw_reference}", failures)

    robots = (artifact_root / "robots.txt").read_text(encoding="utf-8") if (artifact_root / "robots.txt").is_file() else ""
    if f"Sitemap: {PRODUCTION_ORIGIN}/sitemap.xml" not in robots:
        fail("robots.txt does not identify the production sitemap", failures)

    sitemap = (artifact_root / "sitemap.xml").read_text(encoding="utf-8") if (artifact_root / "sitemap.xml").is_file() else ""
    for route in EXPECTED_PAGES:
        if route == "/404.html":
            continue
        if f"<loc>{PRODUCTION_ORIGIN}{route}</loc>" not in sitemap:
            fail(f"Sitemap is missing {route}", failures)

    manifest_path = artifact_root / "site.webmanifest"
    if manifest_path.is_file():
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as error:
            fail(f"Invalid site.webmanifest: {error}", failures)
        else:
            if manifest.get("start_url") != "/" or manifest.get("scope") != "/":
                fail("Manifest start_url and scope must use the dedicated-domain root", failures)

    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        print(f"Validation failed with {len(failures)} error(s).", file=sys.stderr)
        return 1

    total_bytes = sum(path.stat().st_size for path in files)
    print(
        f"Validated {len(files)} deployment files ({total_bytes / 1024:.1f} KiB), "
        f"{len(html_documents)} HTML documents, metadata, routes, fragments, and public-content boundaries."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(audit())
