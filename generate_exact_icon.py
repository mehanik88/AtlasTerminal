#!/usr/bin/env python3
"""Render the AtlasTerminal launcher icon in the shared Atlas icon format.

The supplied reference is a flattened 1254x1254 image. Atlas applications keep
the background in the adaptive-icon resource and store only a 432x432 RGBA
foreground filled with the shared #7893A0 accent. This script extracts the
blue-gray symbol, scales it to the same visual bounds as the other Atlas apps,
and writes both the Android foreground and a flattened documentation preview.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parent
DEFAULT_SOURCE = ROOT / "docs/images/terminal-icon-reference.png"
DEFAULT_FOREGROUND = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_foreground.png"
DEFAULT_PREVIEW = ROOT / "docs/images/app-icon.png"

CANVAS_SIZE = 432
SYMBOL_SIZE = 194
ACCENT = (120, 147, 160)  # #7893A0, shared by the Atlas applications
BACKGROUND = (23, 23, 23)  # #171717, shared adaptive-icon background


def extract_symbol_mask(source: Image.Image) -> Image.Image:
    """Return an antialiased alpha mask for the blue-gray symbol."""

    source = source.convert("RGB")
    mask = Image.new("L", source.size, 0)
    source_pixels = source.load()
    mask_pixels = mask.load()

    # The reference has a nearly neutral graphite background and a blue-gray
    # symbol. Chroma is therefore a more reliable separator than brightness,
    # especially around the rounded black corners of the flattened image.
    for y in range(source.height):
        for x in range(source.width):
            red, green, blue = source_pixels[x, y]
            green_delta = max(0, green - red)
            blue_delta = max(0, blue - red)
            alpha = max(green_delta / 18.0, blue_delta / 34.0)
            if alpha < 0.12:
                alpha = 0.0
            mask_pixels[x, y] = round(min(1.0, alpha) * 255)

    bbox = mask.getbbox()
    if bbox is None:
        raise RuntimeError("The reference image does not contain a blue-gray symbol")

    return mask.crop(bbox).resize((SYMBOL_SIZE, SYMBOL_SIZE), Image.Resampling.LANCZOS)


def render(source_path: Path, foreground_path: Path, preview_path: Path) -> None:
    symbol_mask = extract_symbol_mask(Image.open(source_path))
    foreground = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), ACCENT + (0,))
    symbol = Image.new("RGBA", symbol_mask.size, ACCENT + (255,))
    symbol.putalpha(symbol_mask)
    offset = ((CANVAS_SIZE - SYMBOL_SIZE) // 2,) * 2
    foreground.alpha_composite(symbol, dest=offset)

    foreground_path.parent.mkdir(parents=True, exist_ok=True)
    preview_path.parent.mkdir(parents=True, exist_ok=True)
    foreground.save(foreground_path)

    background = Image.new("RGBA", foreground.size, BACKGROUND + (255,))
    Image.alpha_composite(background, foreground).convert("RGB").save(preview_path)

    print(f"Rendered foreground: {foreground_path}")
    print(f"Rendered preview: {preview_path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--foreground", type=Path, default=DEFAULT_FOREGROUND)
    parser.add_argument("--preview", type=Path, default=DEFAULT_PREVIEW)
    args = parser.parse_args()
    render(args.source, args.foreground, args.preview)


if __name__ == "__main__":
    main()
