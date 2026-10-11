"""Vervangt de witte S in het SmartTube-tv-icoon door een wit afspeeldriehoekje."""
import sys
from PIL import Image, ImageDraw

def process(src, dst):
    im = Image.open(src).convert("RGBA")
    w, h = im.size
    px = im.load()

    # Schermkleur = meest voorkomende verzadigde kleur
    counts = {}
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 200 and min(r, g, b) < 180:
                key = (r // 8, g // 8, b // 8)
                counts[key] = counts.get(key, 0) + 1
    key = max(counts, key=counts.get)
    screen = None
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if (r // 8, g // 8, b // 8) == key and a > 200:
                screen = (r, g, b, a)
                break
        if screen:
            break

    def is_screen(c, tol=40):
        return c[3] > 200 and all(abs(c[i] - screen[i]) <= tol for i in range(3))

    # Rijen met veel schermkleur = het scherm (de voet is een dunne losse balk)
    rows = [sum(1 for x in range(w) if is_screen(px[x, y])) for y in range(h)]
    thr = max(rows) * 0.5
    blocks, start = [], None
    for y, c in enumerate(rows + [0]):
        if c >= thr and start is None:
            start = y
        elif c < thr and start is not None:
            blocks.append((start, y - 1)); start = None
    top, bottom = max(blocks, key=lambda b: b[1] - b[0])
    cols = [x for x in range(w) if any(is_screen(px[x, y]) for y in range(top, bottom + 1))]
    left, right = min(cols), max(cols)

    # S wegwerken: binnenkant van het scherm (rondingen ontzien) volledig schermkleur maken
    mx = int((right - left) * 0.12)
    my = int((bottom - top) * 0.12)
    x0, x1 = left + mx, right - mx
    for y in range(top + my, bottom - my + 1):
        cl = px[x0 - 1, y]
        cr = px[x1 + 1, y]
        for x in range(x0, x1 + 1):
            t = (x - x0) / max(1, (x1 - x0))
            px[x, y] = tuple(int(cl[i] * (1 - t) + cr[i] * t) for i in range(4))

    # Driehoek tekenen met supersampling voor nette randen
    sw, sh = right - left, bottom - top
    tri_h = sh * 0.46
    tri_w = tri_h * 0.88
    cx = left + sw / 2 + tri_w * 0.08  # optisch midden iets naar rechts
    cy = top + sh / 2
    scale = 4
    layer = Image.new("L", (w * scale, h * scale), 0)
    d = ImageDraw.Draw(layer)
    pts = [((cx - tri_w / 2) * scale, (cy - tri_h / 2) * scale),
           ((cx - tri_w / 2) * scale, (cy + tri_h / 2) * scale),
           ((cx + tri_w / 2) * scale, cy * scale)]
    d.polygon(pts, fill=255)
    mask = layer.resize((w, h), Image.LANCZOS)
    white = Image.new("RGBA", (w, h), (255, 255, 255, 255))
    im.paste(white, (0, 0), mask)
    im.save(dst)
    print("ok", dst, "scherm", (left, top, right, bottom), "kleur", screen[:3])

for path in sys.argv[1:]:
    process(path, path)
