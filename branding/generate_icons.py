from PIL import Image
from pathlib import Path

src = Path(r"C:\Users\김쥬\woohaeng-board\branding\woohaeng-logo.png")
logo = Image.open(src).convert("RGBA")
res = Path(r"C:\Users\김쥬\woohaeng-board\apps\android\app\src\main\res")
web = Path(r"C:\Users\김쥬\woohaeng-board\apps\web\public")
web.mkdir(parents=True, exist_ok=True)

legacy = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
adaptive = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def fit_square(img: Image.Image, size: int, pad_ratio: float = 0.08) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    inner = int(size * (1 - pad_ratio * 2))
    scaled = img.copy()
    scaled.thumbnail((inner, inner), Image.Resampling.LANCZOS)
    x = (size - scaled.width) // 2
    y = (size - scaled.height) // 2
    canvas.paste(scaled, (x, y), scaled)
    return canvas


def adaptive_fg(img: Image.Image, size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    inner = int(size * 0.66)
    plate = Image.new("RGBA", (inner, inner), (255, 255, 255, 255))
    scaled = img.copy()
    content = int(inner * 0.92)
    scaled.thumbnail((content, content), Image.Resampling.LANCZOS)
    px = (inner - scaled.width) // 2
    py = (inner - scaled.height) // 2
    plate.paste(scaled, (px, py), scaled)
    x = (size - inner) // 2
    y = (size - inner) // 2
    canvas.paste(plate, (x, y), plate)
    return canvas


for folder, size in legacy.items():
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    icon = fit_square(logo, size, pad_ratio=0.06)
    icon.save(d / "ic_launcher.png")
    icon.save(d / "ic_launcher_round.png")

for folder, size in adaptive.items():
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    bg = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    bg.save(d / "ic_launcher_background.png")
    adaptive_fg(logo, size).save(d / "ic_launcher_foreground.png")

fit_square(logo, 32, 0.04).save(web / "favicon-32.png")
fit_square(logo, 16, 0.04).save(web / "favicon-16.png")
fit_square(logo, 192, 0.05).save(web / "icon-192.png")
fit_square(logo, 512, 0.05).save(web / "icon-512.png")
fit_square(logo, 180, 0.05).save(web / "apple-touch-icon.png")
fit_square(logo, 512, 0.05).save(web / "icon.png")

og = Image.new("RGB", (1200, 630), (255, 255, 255))
og_logo = logo.copy()
og_logo.thumbnail((420, 420), Image.Resampling.LANCZOS)
ox = (1200 - og_logo.width) // 2
oy = (630 - og_logo.height) // 2
rgba = og_logo.convert("RGBA")
og.paste(rgba, (ox, oy), rgba)
og.save(web / "og.png", optimize=True)
fit_square(logo, 1024, 0.04).convert("RGB").save(web / "og-square.png", optimize=True)

ico48 = fit_square(logo, 48, 0.04)
ico48.save(
    web / "favicon.ico",
    format="ICO",
    sizes=[(16, 16), (32, 32), (48, 48)],
)

print("icons generated")
for p in sorted(res.rglob("ic_launcher*.png")):
    print(p.relative_to(res), Image.open(p).size)
print("web:", sorted(p.name for p in web.iterdir() if p.suffix))
