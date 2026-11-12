"""Resize the approved NiK artwork into Android launcher and in-app resources.

Requires Pillow. Run from any directory: python branding/generate_icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
ICON_BACKGROUND = "#000000"
art = Image.open(ROOT / "branding/nikgapps-nik.png").convert("RGBA")
art = art.crop(art.getchannel("A").getbbox())


def fit(size, extent, monochrome=False):
    scaled = art.copy()
    scaled.thumbnail((round(extent), round(extent)), Image.Resampling.LANCZOS)
    # Keep the existing visible size, but ignore near-transparent generation
    # artifacts when centering. Otherwise invisible pixels shift the logo.
    visible_bounds = scaled.getchannel("A").point(lambda alpha: 255 if alpha > 32 else 0).getbbox()
    if visible_bounds is None:
        raise ValueError("Icon artwork has no visible pixels")
    scaled = scaled.crop(visible_bounds)
    if monochrome:
        mask = scaled.getchannel("A")
        scaled = Image.new("RGBA", scaled.size, "white")
        scaled.putalpha(mask)
    canvas = Image.new("RGBA", (size, size))
    canvas.alpha_composite(scaled, ((size - scaled.width) // 2, (size - scaled.height) // 2))
    return canvas


for density, factor in [("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4)]:
    directory = RES / f"mipmap-{density}"
    directory.mkdir(parents=True, exist_ok=True)
    layer_size = round(108 * factor)
    fit(layer_size, 60 * factor).save(directory / "ic_launcher_foreground.png")
    fit(layer_size, 60 * factor, monochrome=True).save(directory / "ic_launcher_monochrome.png")
    Image.new("RGB", (layer_size, layer_size), ICON_BACKGROUND).save(directory / "ic_launcher_background.png")
    size = round(48 * factor)
    for name, circular in [("ic_launcher", False), ("ic_launcher_round", True)]:
        bg = Image.new("RGBA", (size, size))
        draw = ImageDraw.Draw(bg)
        if circular:
            draw.ellipse((0, 0, size - 1, size - 1), fill=ICON_BACKGROUND)
        else:
            draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=round(size * .22), fill=ICON_BACKGROUND)
        bg.alpha_composite(fit(size, size * .72))
        bg.save(directory / f"{name}.png")
    # Replace prior WebP resources without leaving duplicate Android names.
    for name in ["ic_launcher", "ic_launcher_round", "ic_launcher_foreground", "ic_launcher_background"]:
        (directory / f"{name}.webp").unlink(missing_ok=True)

drawable = RES / "drawable-nodpi"
drawable.mkdir(exist_ok=True)
fit(640, 600).save(drawable / "nikgapps_logo.png")
(drawable / "nikgapps_logo.webp").unlink(missing_ok=True)
fit(96, 88, monochrome=True).save(drawable / "ic_launcher_foreground.png")
(RES / "drawable/ic_launcher_foreground.xml").unlink(missing_ok=True)
store_icon = Image.new("RGBA", (512, 512), ICON_BACKGROUND)
store_icon.alpha_composite(fit(512, 350))
store_icon.convert("RGB").save(ROOT / "branding/play-store-icon.png")

# Preview round/squircle launcher masks and the themed icon at readable sizes.
preview = Image.new("RGB", (720, 280), "#eeeeee")
for index, (shape, mono) in enumerate([("round", False), ("squircle", False), ("round", True)]):
    tile = Image.new("RGBA", (200, 200))
    mask = Image.new("L", tile.size)
    draw = ImageDraw.Draw(mask)
    if shape == "round":
        draw.ellipse((0, 0, 199, 199), fill=255)
    else:
        draw.rounded_rectangle((0, 0, 199, 199), radius=52, fill=255)
    tile.paste("#364b67" if mono else ICON_BACKGROUND, (0, 0, 200, 200))
    foreground = fit(300, 167, monochrome=mono)
    tile.alpha_composite(foreground.crop((50, 50, 250, 250)))
    tile.putalpha(mask)
    preview.paste(tile, (20 + index * 240, 30), tile)
preview.save(ROOT / "branding/launcher-preview.png")
