#!/usr/bin/env python3
"""Generate the display-sized website images from the Play Store source assets.

The public pages show screenshots and station artwork far smaller than their
source files. This script writes display-sized copies into
docs/play-store-assets/web/, which prepare-project-site.sh then copies into the
site. Re-run it, and commit the result, whenever a source image changes.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "docs" / "play-store-assets"
OUTPUT = SOURCE / "web"

# (source, output, width in CSS pixels x2 for high-density screens, format, quality)
IMAGES = [
    ("app-icon-512.png", "app-icon-96.png", 96, "PNG", None),
    ("marketing/now-playing-live.png", "now-playing.webp", 560, "WEBP", 84),
    ("marketing/queue-live.png", "queue.webp", 480, "WEBP", 84),
    ("marketing/favorites-live.png", "favorites.webp", 480, "WEBP", 84),
    ("marketing/chat-synthetic.png", "chat-demonstration.webp", 480, "WEBP", 84),
    ("marketing/tv-now-playing-testing.png", "tv-testing.webp", 720, "WEBP", 82),
    ("marketing/stations/sst.png", "stations/sst.webp", 200, "WEBP", 90),
    ("marketing/stations/1980s.png", "stations/1980s.webp", 200, "WEBP", 90),
    ("marketing/stations/adagio.png", "stations/adagio.webp", 200, "WEBP", 90),
    ("marketing/stations/death.png", "stations/death.webp", 200, "WEBP", 90),
    ("marketing/stations/entranced.png", "stations/entranced.webp", 200, "WEBP", 90),
]


def main() -> int:
    for source_name, output_name, width, image_format, quality in IMAGES:
        source = SOURCE / source_name
        output = OUTPUT / output_name
        output.parent.mkdir(parents=True, exist_ok=True)
        with Image.open(source) as image:
            mode = "RGBA" if image_format == "PNG" else "RGB"
            image = image.convert(mode)
            if image.width > width:
                height = round(image.height * width / image.width)
                image = image.resize((width, height), Image.LANCZOS)
            options = {"optimize": True} if image_format == "PNG" else {"quality": quality, "method": 6}
            image.save(output, image_format, **options)
        print(f"{output.relative_to(ROOT)}  {image.width}x{image.height}  {output.stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
