"""Genera los recursos de marca de CoinDen desde los SVG de design/brand/.

Uso (desde la raíz del repo):  python scripts/brand/export_brand.py
Requiere PyMuPDF (fitz) para dibujar los SVG y Pillow para PNG/WebP.

Los SVG no son transparentes: los contornos oscuros son el fondo #1C140F que se ve entre
las formas. Por eso el arte siempre se usa con ese fondo y el ícono adaptable lleva una capa
de fondo del mismo color, así no se ve ninguna costura.
"""

import io
import os
import re

import fitz
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BRAND = os.path.join(ROOT, "design", "brand")
RES = os.path.join(ROOT, "composeApp", "src", "androidMain", "res")
COMPOSE = os.path.join(ROOT, "composeApp", "src", "commonMain", "composeResources")

BACKGROUND = "#1C140F"
BACKGROUND_RGB = (0x1C, 0x14, 0x0F)

# Densidades de Android: factor sobre 1 dp.
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

# Ícono adaptable: lienzo de 108 dp; la zona segura es un círculo de 66 dp.
ADAPTIVE_DP = 108
ART_DP = 66
# Splash (API 31+): lienzo de 288 dp; el ícono se recorta en un círculo de 192 dp.
SPLASH_DP = 288
SPLASH_ART_DP = 176


def svg_with_background(name: str) -> str:
    """El SVG con su primer trazo (el rectángulo de fondo) pintado de #1C140F."""
    src = open(os.path.join(BRAND, name), encoding="utf-8").read()
    start = src.index("<path")
    end = src.index("/>", start) + 2
    first = re.sub(r'fill="#[0-9A-Fa-f]{6}"', f'fill="{BACKGROUND}"', src[start:end])
    return src[:start] + first + src[end:]


def render(svg: str, size: int) -> Image.Image:
    """Dibuja el SVG cuadrado a size x size píxeles."""
    doc = fitz.open(stream=svg.encode("utf-8"), filetype="svg")
    page = doc[0]
    scale = size / page.rect.width
    pix = page.get_pixmap(matrix=fitz.Matrix(scale, scale), alpha=False)
    return Image.open(io.BytesIO(pix.tobytes("png"))).convert("RGBA")


def centered(art: Image.Image, canvas: int) -> Image.Image:
    layer = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    offset = (canvas - art.width) // 2
    layer.alpha_composite(art, (offset, offset))
    return layer


def silhouette(art: Image.Image) -> Image.Image:
    """Monocromo: opaco donde el arte no es fondo (los contornos quedan como huecos)."""
    out = Image.new("RGBA", art.size, (0, 0, 0, 0))
    src = art.load()
    dst = out.load()
    for y in range(art.height):
        for x in range(art.width):
            r, g, b, _ = src[x, y]
            far = abs(r - BACKGROUND_RGB[0]) + abs(g - BACKGROUND_RGB[1]) + abs(b - BACKGROUND_RGB[2])
            if far > 90:
                dst[x, y] = (255, 255, 255, 255)
    return out


def save(image: Image.Image, path: str, fmt: str = "PNG", **options) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path, fmt, **options)
    print("  ", os.path.relpath(path, ROOT), image.size)


def main() -> None:
    icon_svg = svg_with_background("coinden-icono-app.svg")
    logo_svg = svg_with_background("coinden-logo-ilustracion.svg")

    print("Ícono adaptable, monocromo y splash:")
    for qualifier, factor in DENSITIES.items():
        canvas = round(ADAPTIVE_DP * factor)
        art = render(icon_svg, round(ART_DP * factor))
        save(centered(art, canvas), os.path.join(RES, f"mipmap-{qualifier}", "ic_launcher_foreground.png"))
        save(centered(silhouette(art), canvas), os.path.join(RES, f"mipmap-{qualifier}", "ic_launcher_monochrome.png"))
        splash = render(icon_svg, round(SPLASH_ART_DP * factor))
        save(centered(splash, round(SPLASH_DP * factor)), os.path.join(RES, f"drawable-{qualifier}", "splash_icon.png"))

    print("Logo e ícono para la app (WebP @2x y @3x):")
    for qualifier, factor in {"xhdpi": 2, "xxhdpi": 3}.items():
        folder = os.path.join(COMPOSE, f"drawable-{qualifier}")
        save(render(logo_svg, 200 * factor).convert("RGB"), os.path.join(folder, "coinden_logo.webp"), "WEBP", quality=90, method=6)
        save(render(icon_svg, 48 * factor).convert("RGB"), os.path.join(folder, "coinden_icon.webp"), "WEBP", quality=92, method=6)


if __name__ == "__main__":
    main()
