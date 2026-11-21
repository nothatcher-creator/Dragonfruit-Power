#!/usr/bin/env python3
"""
Builds the "Zomboid Look" resource pack that ships inside Outbreak (src/main/resources/resourcepacks/zomboid_look).

The hand-painted 64px textures (grass, dirt, asphalt, sidewalk, floorboards, bricks, siding, shingles, carpet,
wallpaper, kitchen tiles, corrugated metal) come from tools/zomboid_src: seamless 256px masters cut from images the
author generated with ChatGPT. Everything else is generated here: soft, painterly, low-saturation 32x32 textures in a muted palette
(the feel of Project Zomboid's hand-painted tiles), dull olive grass and foliage colour maps, dark slate UI widgets,
and a colour-grading post shader (desaturate, warm olive tint, softer contrast, vignette, a little grain).

The vanilla UI sheet is only read for where its pieces sit; none of its pixels are copied.

Usage: python3 tools/make_zomboid_pack.py <vehicle-forge resources dir> [vanilla widgets.png for layout]
"""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'resourcepacks', 'zomboid_look')
VF = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/vf-push/src/main/resources'
N = 32  # texture size


def out(path, img):
    p = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


# ---------------------------------------------------------------- noise and paint

def vnoise(size, cells, rng):
    """tileable value noise: a random grid, smoothly interpolated, wrapping at the edges"""
    g = rng.random((cells, cells))
    xs = np.arange(size) * cells / size
    i0 = np.floor(xs).astype(int)
    f = xs - i0
    f = f * f * (3 - 2 * f)
    i1 = (i0 + 1) % cells
    a = g[np.ix_(i0, i0)]
    b = g[np.ix_(i0, i1)]
    c = g[np.ix_(i1, i0)]
    d = g[np.ix_(i1, i1)]
    fy = f[:, None]
    fx = f[None, :]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def fbm(size, rng, octaves=((2, .5), (4, .3), (8, .15), (16, .05))):
    n = sum(w * vnoise(size, c, rng) for c, w in octaves)
    return (n - n.min()) / max(1e-6, n.max() - n.min())


def hexc(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


def shade(base, n, amount):
    """base colour lightened/darkened by noise n (0..1) around 0.5"""
    return base[None, None, :] * (1 + (n[..., None] - 0.5) * amount)


def to_img(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    if alpha is None:
        return Image.fromarray(rgb, 'RGB').convert('RGBA')
    a = np.clip(alpha, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([rgb, a]), 'RGBA')


def strokes(rgb, rng, count, length, color_jitter, horizontal=None, alpha=0.35):
    """soft dabs of slightly different colour, like a painted texture"""
    h, w, _ = rgb.shape
    for _ in range(count):
        x, y = rng.integers(0, w), rng.integers(0, h)
        ang = rng.random() * math.pi if horizontal is None else (0 if horizontal else math.pi / 2) + (rng.random() - .5) * .3
        L = rng.integers(max(2, length // 2), length + 1)
        jit = (rng.random(3) - .5) * color_jitter
        for t in range(L):
            px = int(round(x + math.cos(ang) * t)) % w
            py = int(round(y + math.sin(ang) * t)) % h
            rgb[py, px] = rgb[py, px] * (1 - alpha) + (rgb[py, px] + jit) * alpha
    return rgb


def speckle(rgb, rng, count, dark, size=1):
    h, w, _ = rgb.shape
    for _ in range(count):
        x, y = rng.integers(0, w), rng.integers(0, h)
        for dy in range(size):
            for dx in range(size):
                rgb[(y + dy) % h, (x + dx) % w] *= dark
    return rgb


def blur(rgb, k=1):
    """a soft wrap-around box blur"""
    acc = np.zeros_like(rgb)
    n = 0
    for dy in range(-k, k + 1):
        for dx in range(-k, k + 1):
            acc += np.roll(np.roll(rgb, dy, 0), dx, 1)
            n += 1
    return acc / n


# ---------------------------------------------------------------- materials

def concrete(col, seed, stains=True, grain=0.10):
    rng = np.random.default_rng(seed)
    base = hexc(col)
    n = fbm(N, rng)
    n = fbm(N, rng, ((4, .45), (8, .35), (16, .2)))                 # no tile-sized blotches (they show as a grid)
    rgb = shade(base, n, 0.12)
    rgb = rgb * (1 + (rng.random((N, N, 1)) - .5) * grain)
    if stains:
        s = fbm(N, rng, ((2, .7), (4, .3)))
        rgb *= (1 - np.clip(s - 0.7, 0, 1)[..., None] * 0.3)
    rgb = speckle(rgb, rng, 10, 0.9)
    return to_img(blur(rgb, 0) * 0.3 + rgb * 0.7)


def bricks(mortar, brick, rows, seed, jitter=0.12, cols=2):
    rng = np.random.default_rng(seed)
    m = hexc(mortar)
    rgb = shade(m, fbm(N, rng), 0.15)
    bh = N // rows
    bw = N // cols
    for r in range(rows):
        off = (bw // 2) * (r % 2)
        for c in range(cols + 1):
            x0 = c * bw - off
            tone = hexc(brick) * (1 + (rng.random() - .5) * jitter * 2) + (rng.random(3) - .5) * 8
            n = fbm(N, rng, ((4, .6), (8, .4)))
            for y in range(r * bh + 1, (r + 1) * bh):
                for x in range(x0 + 1, x0 + bw):
                    xx = x % N
                    rgb[y, xx] = tone * (1 + (n[y, xx] - .5) * 0.22)
    rgb = speckle(rgb, rng, 14, 0.85)
    return to_img(rgb)


def planks(col, seed, boards=4, grain_dark=0.78):
    rng = np.random.default_rng(seed)
    base = hexc(col)
    rgb = np.zeros((N, N, 3))
    bh = N // boards
    for b in range(boards):
        tone = base * (1 + (rng.random() - .5) * 0.18)
        # grain: stretched noise along x
        g = vnoise(N, 4, rng)[:, :1].repeat(N, 1) * 0.5 + vnoise(N, 16, rng) * 0.2 + fbm(N, rng) * 0.3
        for y in range(b * bh, (b + 1) * bh):
            for x in range(N):
                streak = 0.5 + 0.5 * math.sin((y * 2.1 + g[y, x] * 6) + b)
                rgb[y, x] = tone * (0.9 + 0.12 * streak) * (1 + (g[y, x] - .5) * 0.15)
        rgb[b * bh, :] *= 0.62                                     # seam
        joint = rng.integers(4, N - 4)
        rgb[b * bh:(b + 1) * bh, joint] *= grain_dark              # end joint
        rgb[b * bh + bh - 1, :] *= 0.9
    rgb = strokes(rgb, rng, 30, 8, 14, horizontal=True, alpha=.4)
    return to_img(rgb)


def grass_top(seed):
    """grayscale (the game tints it with the grass colour map)"""
    rng = np.random.default_rng(seed)
    n = fbm(N, rng)
    v = 150 + (n - .5) * 46
    rgb = np.dstack([v, v, v]).astype(float)
    for _ in range(170):                                            # little blades
        x, y = rng.integers(0, N), rng.integers(0, N)
        d = 0.75 + rng.random() * 0.4
        L = rng.integers(2, 4)
        for t in range(L):
            rgb[(y - t) % N, (x + (t if rng.random() < .3 else 0)) % N] *= d
    return to_img(blur(rgb, 1) * 0.35 + rgb * 0.65)


def dirt(seed, col='#6b5238'):
    rng = np.random.default_rng(seed)
    rgb = shade(hexc(col), fbm(N, rng), 0.35)
    rgb = speckle(rgb, rng, 40, 0.78)
    for _ in range(9):                                              # pebbles
        x, y = rng.integers(0, N), rng.integers(0, N)
        c = hexc('#8a7a66') * (0.85 + rng.random() * .3)
        for dy in range(2):
            for dx in range(2):
                rgb[(y + dy) % N, (x + dx) % N] = c * (0.9 if dy else 1.05)
    return to_img(rgb)


def grass_side(seed, fringe_rgb):
    """dirt with a ragged fringe on top; also returns the grayscale overlay mask for the tinted layer"""
    rng = np.random.default_rng(seed)
    d = np.array(dirt(seed + 1).convert('RGB'), dtype=float)
    depth = (6 + 3 * vnoise(N, 8, rng)[0] + rng.random(N) * 3).astype(int)
    over = np.zeros((N, N, 4))
    for x in range(N):
        for y in range(depth[x]):
            v = 150 + (rng.random() - .5) * 40
            over[y, x] = [v, v, v, 255]
            d[y, x] = fringe_rgb * (v / 150)
    return to_img(d), Image.fromarray(over.astype(np.uint8), 'RGBA')


def gravel(seed):
    rng = np.random.default_rng(seed)
    rgb = shade(hexc('#7a7670'), fbm(N, rng), 0.25)
    for _ in range(60):
        x, y = rng.integers(0, N), rng.integers(0, N)
        c = hexc('#8f8a82') * (0.7 + rng.random() * 0.5)
        r = rng.integers(1, 3)
        for dy in range(-r, r):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r:
                    rgb[(y + dy) % N, (x + dx) % N] = c * (1.08 if dy < 0 else 0.86)
    return to_img(rgb)


def smooth_stone(seed, frame=True):
    rng = np.random.default_rng(seed)
    rgb = shade(hexc('#a7a59e'), fbm(N, rng), 0.12)
    if frame:
        rgb[0, :] *= 0.82
        rgb[-1, :] *= 0.78
        rgb[:, 0] *= 0.82
        rgb[:, -1] *= 0.78
    rgb = speckle(rgb, rng, 8, 0.9)
    return to_img(rgb)


def polished(col, seed):
    rng = np.random.default_rng(seed)
    rgb = shade(hexc(col), fbm(N, rng, ((2, .4), (4, .4), (8, .2))), 0.22)
    rgb = strokes(rgb, rng, 40, 6, 18, alpha=.3)
    rgb[0, :] *= 1.06
    rgb[:, 0] *= 1.06
    rgb[-1, :] *= 0.86
    rgb[:, -1] *= 0.86
    return to_img(rgb)


def log_side(seed, col='#5a4632'):
    rng = np.random.default_rng(seed)
    base = hexc(col)
    rgb = np.zeros((N, N, 3))
    ridges = vnoise(N, 8, rng)[0]
    for x in range(N):
        for y in range(N):
            rgb[y, x] = base * (0.75 + 0.5 * ridges[x]) * (1 + (rng.random() - .5) * 0.08)
    rgb = strokes(rgb, rng, 40, 8, 16, horizontal=False, alpha=.5)
    return to_img(rgb)


def log_top(seed, bark='#5a4632', wood='#a88a5e'):
    rng = np.random.default_rng(seed)
    rgb = np.zeros((N, N, 3))
    c = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - c, y - c)
            if max(abs(x - c), abs(y - c)) > N / 2 - 2.5:
                rgb[y, x] = hexc(bark) * (0.85 + rng.random() * .2)
            else:
                ring = 0.5 + 0.5 * math.sin(r * 1.5 + rng.random() * 0.6)
                rgb[y, x] = hexc(wood) * (0.9 + 0.07 * ring) * (1 + (rng.random() - .5) * 0.06)
    return to_img(rgb)


def leaves(seed):
    """grayscale clumps with gaps (tinted by the foliage colour map)"""
    rng = np.random.default_rng(seed)
    n = fbm(N, rng, ((4, .5), (8, .35), (16, .15)))
    v = 120 + n * 90
    rgb = np.dstack([v, v, v])
    alpha = np.where(n > 0.28, 255, 0)
    for _ in range(40):
        x, y = rng.integers(0, N), rng.integers(0, N)
        rgb[y, x] *= 0.7
    return to_img(rgb, alpha)


def glass(seed):
    rng = np.random.default_rng(seed)
    rgb = np.zeros((N, N, 3)) + hexc('#c9d6d4')
    a = np.zeros((N, N)) + 26
    a[0, :] = a[-1, :] = a[:, 0] = a[:, -1] = 150                   # frame
    rgb[0, :] = rgb[-1, :] = rgb[:, 0] = rgb[:, -1] = hexc('#7f8a88')
    for k in (-6, 3):                                               # two faint glints
        for t in range(N):
            x, y = t, t + k
            if 1 <= x < N - 1 and 1 <= y < N - 1:
                a[y, x] = max(a[y, x], 48)
    return to_img(rgb, a)


def stone_bricks(seed):
    rng = np.random.default_rng(seed)
    m = hexc('#5f5d58')
    rgb = shade(m, fbm(N, rng), .1)
    for r in range(2):
        off = 8 * (r % 2)
        for c in range(3):
            x0 = c * 16 - off
            tone = hexc('#8d8a83') * (1 + (rng.random() - .5) * .14)
            n = fbm(N, rng)
            for y in range(r * 16 + 1, r * 16 + 15):
                for x in range(x0 + 1, x0 + 15):
                    xx = x % N
                    edge = 0.9 if (y == r * 16 + 1 or x == x0 + 1) else (0.8 if (y == r * 16 + 14 or x == x0 + 14) else 1)
                    rgb[y, xx] = tone * (1 + (n[y, xx] - .5) * .25) * edge
    rgb = speckle(rgb, rng, 18, 0.82)
    return to_img(rgb)


def iron(seed):
    rng = np.random.default_rng(seed)
    rgb = shade(hexc('#9a9c9c'), fbm(N, rng), .1)
    rgb = strokes(rgb, rng, 60, 10, 10, horizontal=True, alpha=.35)
    for x in (0, 15, 16, 31):
        rgb[:, x] *= 0.8
    for y in (0, 15, 16, 31):
        rgb[y, :] *= 0.8
    return to_img(rgb)


# ---------------------------------------------------------------- Vehicle Forge's street (repainted, same layout)

def paint_mask(path, kind):
    """which columns/pixels of the original 16px texture carry paint, scaled to 32px"""
    im = Image.open(path).convert('RGB').resize((N, N), Image.NEAREST)
    a = np.array(im, dtype=float)
    if kind == 'yellow':
        m = (a[..., 0] > 150) & (a[..., 1] > 120) & (a[..., 2] < 110)
    else:
        m = a.mean(axis=2) > 170
    col = m.mean(axis=0) > 0.45                                     # columns that are mostly paint
    full = np.zeros((N, N), bool)
    full[:, col] = True
    return full


def asphalt(seed):
    rng = np.random.default_rng(seed)
    rgb = shade(hexc('#3a3b3c'), fbm(N, rng), .22)
    for _ in range(70):                                             # aggregate
        x, y = rng.integers(0, N), rng.integers(0, N)
        rgb[y, x] *= 0.75 + rng.random() * 0.6
    if rng.random() < 1:                                            # a hairline crack
        x, y = rng.integers(0, N), rng.integers(0, N)
        for t in range(rng.integers(8, 18)):
            rgb[y % N, x % N] *= 0.6
            x += rng.integers(0, 2)
            y += rng.integers(-1, 2)
    return rgb


def road_line(base_seed, mask, paint):
    rng = np.random.default_rng(base_seed + 99)
    rgb = asphalt(base_seed)
    p = hexc(paint)
    wear = fbm(N, rng)
    for y in range(N):
        for x in range(N):
            if mask[y, x] and wear[y, x] > 0.22:
                rgb[y, x] = p * (0.86 + rng.random() * .14)
    return to_img(rgb)


def sidewalk(seed):
    rng = np.random.default_rng(seed)
    rgb = shade(hexc('#a9a69e'), fbm(N, rng), .14)
    for k in (0, 16):                                               # slab joints, 2x2 per block like the original
        rgb[k, :] *= 0.72
        rgb[:, k] *= 0.72
    rgb = speckle(rgb, rng, 20, 0.88)
    return to_img(rgb)


# ---------------------------------------------------------------- colour maps

def colormap(dry, wet):
    """256x256: the game samples it by temperature and rainfall; keep it all in a dull olive range"""
    d, w = hexc(dry), hexc(wet)
    img = np.zeros((256, 256, 3))
    for y in range(256):
        for x in range(256):
            t = (x / 255) * 0.6 + (y / 255) * 0.4
            img[y, x] = d * (1 - t) + w * t
    return to_img(img)


# ---------------------------------------------------------------- UI widgets (original art, vanilla layout)

PANEL = (34, 36, 35, 235)
PANEL_HI = (58, 56, 48, 245)
EDGE = (92, 94, 90, 255)
EDGE_DARK = (18, 19, 19, 255)
AMBER = (214, 166, 72, 255)


def rect(d, x0, y0, x1, y1, fill=None, outline=None):
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=fill, outline=outline)


def button(d, x, y, w, h, state):
    fill = {'off': (26, 27, 27, 200), 'on': PANEL, 'hot': PANEL_HI}[state]
    rect(d, x, y, x + w, y + h, fill=fill)
    rect(d, x, y, x + w, y + 1, fill=(110, 112, 106, 255) if state != 'off' else (60, 61, 60, 255))
    rect(d, x, y + h - 1, x + w, y + h, fill=EDGE_DARK)
    if state == 'hot':
        rect(d, x, y, x + w, y + h, outline=AMBER)


def slot(d, x, y, s=20):
    rect(d, x, y, x + s, y + s, fill=(20, 21, 21, 205), outline=(78, 80, 75, 235))
    rect(d, x + 1, y + 1, x + s - 1, y + 2, fill=(10, 10, 10, 220))


def globe(d, x, y):
    d.ellipse([x + 4, y + 4, x + 15, y + 15], fill=(92, 120, 128, 255), outline=(200, 200, 190, 255))
    d.line([x + 4, y + 10, x + 15, y + 10], fill=(200, 200, 190, 255))
    d.ellipse([x + 7, y + 4, x + 12, y + 15], outline=(200, 200, 190, 255))


def lock(d, x, y, opened, tone):
    body = tuple(int(c * tone) for c in (180, 176, 160)) + (255,)
    rect(d, x + 5, y + 9, x + 15, y + 17, fill=body)
    rect(d, x + 9, y + 12, x + 11, y + 15, fill=(40, 40, 40, 255))
    if opened:
        d.arc([x + 9, y + 2, x + 17, y + 12], 180, 360, fill=body, width=2)
    else:
        d.arc([x + 6, y + 2, x + 14, y + 13], 180, 360, fill=body, width=2)


def widgets():
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # hotbar 182x22: nine slots
    rect(d, 0, 0, 182, 22, fill=(16, 17, 17, 170))
    for i in range(9):
        slot(d, 1 + i * 20, 1)
    # selected slot 24x24 (0,22)
    rect(d, 0, 22, 24, 46, outline=AMBER)
    rect(d, 1, 23, 23, 45, outline=(250, 230, 180, 200))
    # off-hand slots
    for x0 in (24, 60):
        rect(d, x0, 22, x0 + 22, 46, fill=(16, 17, 17, 170))
        slot(d, x0 + 1, 23, 20)
    # buttons: disabled (0,46), normal (0,66), hovered (0,86), 200x20
    button(d, 0, 46, 200, 20, 'off')
    button(d, 0, 66, 200, 20, 'on')
    button(d, 0, 86, 200, 20, 'hot')
    # language button (0,106) normal, (0,126) hovered
    for k, st in ((0, 'on'), (1, 'hot')):
        button(d, 0, 106 + 20 * k, 20, 20, st)
        globe(d, 0, 106 + 20 * k)
    # lock buttons: normal (0,146), hovered (0,166), disabled (0,186); locked at x 0, unlocked at x 20
    for row, st, tone in ((0, 'on', 1.0), (1, 'hot', 1.1), (2, 'off', 0.6)):
        for col in range(2):
            button(d, col * 20, 146 + 20 * row, 20, 20, st)
            lock(d, col * 20, 146 + 20 * row, col == 1, tone)
    # Realms status dots and the "+" (top right), news envelopes and the notification counts
    for k, c in enumerate([(150, 54, 44, 255), (88, 150, 90, 255), (70, 72, 70, 255)]):
        d.ellipse([193 + k * 16, 1, 206 + k * 16, 14], fill=c, outline=(20, 20, 20, 255))
    rect(d, 247, 6, 254, 8, fill=(220, 220, 210, 255))
    rect(d, 250, 3, 252, 11, fill=(220, 220, 210, 255))
    for k in range(2):
        x0 = 167 + k * 16
        rect(d, x0, 26, x0 + 13, 35, fill=(220, 214, 196, 255))
        d.line([x0, 26, x0 + 6, 31, x0 + 12, 26], fill=(120, 110, 96, 255))
    for k in range(6):
        d.ellipse([198 + k * 8, 22, 205 + k * 8, 29], fill=(150, 46, 38, 255))
    return img


# ---------------------------------------------------------------- hand-painted textures (tools/zomboid_src)
# 256px seamless masters cut from images the author generated with ChatGPT; scaled to 64px for the game.

SRC = os.path.join(os.path.dirname(__file__), 'zomboid_src')
P = 64


def painted(name, tint=(1.0, 1.0, 1.0), size=P, desat=0.0):
    path = os.path.join(SRC, name + '_256.png')
    if not os.path.exists(path):
        return None
    im = Image.open(path).convert('RGB').resize((size, size), Image.LANCZOS)
    a = np.asarray(im, float)
    if desat:
        lum = (a * np.array([0.299, 0.587, 0.114])).sum(axis=2, keepdims=True)
        a = a * (1 - desat) + lum * desat
    a = a * np.array(tint)[None, None, :]
    return np.clip(a, 0, 255)


def painted_road(name_mask_paint, seed):
    """the painted asphalt with Vehicle Forge's lane paint laid over it (same columns as the original)"""
    base = painted('asphalt')
    if base is None:
        return None
    path, kind, paint = name_mask_paint
    im = Image.open(path).convert('RGB').resize((P, P), Image.NEAREST)
    a = np.asarray(im, float)
    m = (a[..., 0] > 150) & (a[..., 1] > 120) & (a[..., 2] < 110) if kind == 'yellow' else a.mean(axis=2) > 170
    cols = m.mean(axis=0) > 0.45
    rng = np.random.default_rng(seed)
    wear = fbm(P, rng)
    rgb = base.copy()
    p = hexc(paint)
    for x in np.nonzero(cols)[0]:
        for y in range(P):
            if wear[y, x] > 0.2:
                # paint picks up the texture of the road underneath
                rgb[y, x] = p * (0.78 + 0.22 * base[y, x].mean() / 90.0) * (0.9 + rng.random() * 0.1)
    return to_img(np.clip(rgb, 0, 255))


# ---------------------------------------------------------------- the pack

def main():
    if os.path.isdir(ROOT):
        import shutil
        shutil.rmtree(ROOT)
    os.makedirs(ROOT)
    json.dump({"pack": {"pack_format": 15, "description": "Outbreak: muted, painted, Zomboid-style look"}},
              open(os.path.join(ROOT, 'pack.mcmeta'), 'w'), indent=2)

    B = 'assets/minecraft/textures/block/'
    olive = hexc('#7d8350')
    tex = {
        'white_concrete': concrete('#cfccc2', 1), 'light_gray_concrete': concrete('#9a978f', 2),
        'gray_concrete': concrete('#5a5a57', 3), 'black_concrete': concrete('#242525', 4),
        'red_concrete': concrete('#6e3d33', 5), 'yellow_concrete': concrete('#b9973f', 6),
        'blue_concrete': concrete('#3c4d63', 7), 'green_concrete': concrete('#4f5c39', 8),
        'brown_concrete': concrete('#5d4632', 9), 'orange_concrete': concrete('#a3622f', 10),
        'stone_bricks': stone_bricks(11), 'bricks': bricks('#8a8379', '#6c4536', 4, 12),
        'smooth_stone': smooth_stone(13), 'smooth_stone_slab_side': smooth_stone(14),
        'polished_andesite': polished('#85857f', 15), 'polished_diorite': polished('#b9b6ad', 16),
        'oak_planks': planks('#9a7a4e', 17), 'spruce_planks': planks('#6e5338', 18),
        'birch_planks': planks('#b8a678', 19), 'dark_oak_planks': planks('#4a3624', 20),
        'grass_block_top': grass_top(21), 'dirt': dirt(22), 'gravel': gravel(23),
        'oak_log': log_side(24), 'oak_log_top': log_top(25), 'spruce_log': log_side(26, '#3f3226'),
        'spruce_log_top': log_top(27, '#3f3226', '#8c6d4a'), 'oak_leaves': leaves(28),
        'glass': glass(29), 'iron_block': iron(30), 'stone': concrete('#7f7d77', 31, stains=False, grain=.16),
        'cobblestone': gravel(32), 'dirt_path_top': dirt(33, '#7c6646'), 'dirt_path_side': dirt(34, '#6b5238'),
        'clay': concrete('#8f9296', 35), 'quartz_block_side': polished('#d2cfc6', 36),
        'quartz_block_bottom': polished('#d2cfc6', 37), 'hay_block_side': planks('#a89254', 38, boards=8),
    }
    for name, img in tex.items():
        out(B + name + '.png', img)
    side, overlay = grass_side(40, olive)
    out(B + 'grass_block_side.png', side)
    out(B + 'grass_block_side_overlay.png', overlay)

    # Vehicle Forge's street, repainted with the same paint layout
    VB = os.path.join(VF, 'assets/vehicleforge/textures/block/')
    VO = 'assets/vehicleforge/textures/block/'
    B = 'assets/minecraft/textures/block/'
    if os.path.isdir(VB):
        out(VO + 'asphalt.png', to_img(asphalt(50)))
        out(VO + 'asphalt_white_line.png', road_line(51, paint_mask(VB + 'asphalt_white_line.png', 'white'), '#d8d6cc'))
        out(VO + 'asphalt_yellow_line.png', road_line(52, paint_mask(VB + 'asphalt_yellow_line.png', 'yellow'), '#c9a23e'))
        out(VO + 'asphalt_double_yellow_line.png', road_line(53, paint_mask(VB + 'asphalt_double_yellow_line.png', 'yellow'), '#c9a23e'))
        out(VO + 'asphalt_crosswalk.png', road_line(54, paint_mask(VB + 'asphalt_crosswalk.png', 'white'), '#d8d6cc'))
        out(VO + 'sidewalk.png', sidewalk(55))
        out(VO + 'concrete_light.png', concrete('#aaa79f', 56))
        out(VO + 'curb.png', smooth_stone(57))
        out(VO + 'curb_side.png', smooth_stone(58))

    # ---- the hand-painted set, where present, replaces the generated textures above
    grass_tint = None
    g = painted('grass')
    if g is not None:
        # the game multiplies grass by the colour map, so divide the map's colour out of the painting
        # brighter than the lawn itself, so tall grass and ferns (grey in the game files) come out the same olive
        grass_tint = (np.percentile(g.reshape(-1, 3), 97, axis=0) * 1.4).clip(60, 255)
        out(B + 'grass_block_top.png', to_img(g / grass_tint[None, None, :] * 255))
    d = painted('dirt')
    if d is not None:
        out(B + 'dirt.png', to_img(d))
        if g is not None:
            rng = np.random.default_rng(41)
            side = d.copy()
            over = np.zeros((P, P, 4))
            depth = (12 + 6 * vnoise(P, 8, rng)[0] + rng.random(P) * 5).astype(int)
            for x in range(P):
                for y in range(depth[x]):
                    c = g[(y * 3) % P, (x * 5) % P]
                    side[y, x] = c
                    over[y, x, :3] = np.clip(c / grass_tint * 255, 0, 255)
                    over[y, x, 3] = 255
            out(B + 'grass_block_side.png', to_img(side))
            out(B + 'grass_block_side_overlay.png', Image.fromarray(over.astype(np.uint8), 'RGBA'))
    for name, targets in {
        'planks': {'spruce_planks': (1.0, 1.0, 1.0), 'oak_planks': ((1.2, 1.14, 1.04), 0.2), 'birch_planks': ((1.55, 1.52, 1.42), 0.45),
                   'dark_oak_planks': ((0.72, 0.68, 0.64), 0.1)},
        'bricks': {'bricks': (1.0, 1.0, 1.0)},
        'siding': {'light_blue_terracotta': (1.0, 1.0, 1.0)},
        'shingles': {'deepslate_tiles': (1.0, 1.0, 1.0)},
        'carpet': {'brown_wool': (1.0, 1.0, 1.0)},
        'wallpaper': {'pink_terracotta': (1.0, 1.0, 1.0)},
        'tiles': {'quartz_block_bottom': (1.0, 1.0, 1.0)},
        'corrugated': {'iron_block': (1.0, 1.0, 1.0)},
    }.items():
        for target, tint in targets.items():
            ds = 0.0
            if isinstance(tint[0], tuple):
                tint, ds = tint
            t = painted(name, tint, desat=ds)
            if t is not None:
                out(B + target + '.png', to_img(t))
    if os.path.isdir(VB):
        a = painted('asphalt')
        if a is not None:
            out(VO + 'asphalt.png', to_img(a))
            for fname, kind, paint, seed in (('asphalt_white_line', 'white', '#d8d6cc', 61), ('asphalt_yellow_line', 'yellow', '#c9a23e', 62),
                                             ('asphalt_double_yellow_line', 'yellow', '#c9a23e', 63), ('asphalt_crosswalk', 'white', '#d8d6cc', 64)):
                out(VO + fname + '.png', painted_road((VB + fname + '.png', kind, paint), seed))
        sw = painted('sidewalk')
        if sw is not None:
            out(VO + 'sidewalk.png', to_img(sw))

    # dull olive grass and foliage everywhere (grass matches the painted lawn when there is one)
    if grass_tint is not None:
        gt = '#%02x%02x%02x' % tuple(int(v) for v in grass_tint)
        out('assets/minecraft/textures/colormap/grass.png', colormap(gt, gt))
    else:
        out('assets/minecraft/textures/colormap/grass.png', colormap('#8d8a52', '#5e6a3c'))
    out('assets/minecraft/textures/colormap/foliage.png', colormap('#7f7c48', '#4f5c34'))

    # dark slate UI
    out('assets/minecraft/textures/gui/widgets.png', widgets())

    # the colour grade
    post = {"targets": ["swap"], "passes": [
        {"name": "outbreak_zomboid", "intarget": "minecraft:main", "outtarget": "swap"},
        {"name": "blit", "intarget": "swap", "outtarget": "minecraft:main"}]}
    prog = {"blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"}, "vertex": "sobel", "fragment": "outbreak_zomboid",
            "attributes": ["Position"], "samplers": [{"name": "DiffuseSampler"}],
            "uniforms": [{"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]},
                         {"name": "InSize", "type": "float", "count": 2, "values": [1, 1]},
                         {"name": "OutSize", "type": "float", "count": 2, "values": [1, 1]},
                         {"name": "Time", "type": "float", "count": 1, "values": [0]}]}
    fsh = """#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

// Outbreak's Zomboid look: washed-out colour, a warm olive cast, softer contrast, a vignette and a little grain.
void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, 0.64);
    c = c * vec3(1.03, 1.0, 0.86) + vec3(0.012, 0.010, 0.0);
    c = (c - 0.5) * 0.93 + 0.5;
    vec2 d = (texCoord - 0.5) * vec2(1.15, 1.0);
    float vig = smoothstep(0.82, 0.28, length(d));
    c *= mix(0.74, 1.0, vig);
    float n = fract(sin(dot(floor(texCoord * InSize) + floor(Time * 977.0), vec2(12.9898, 78.233))) * 43758.5453);
    c += (n - 0.5) * 0.022;
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""
    out_json = lambda path, obj: (os.makedirs(os.path.dirname(os.path.join(ROOT, path)), exist_ok=True),
                                  json.dump(obj, open(os.path.join(ROOT, path), 'w'), indent=2))
    out_json('assets/minecraft/shaders/post/outbreak_zomboid.json', post)
    out_json('assets/minecraft/shaders/program/outbreak_zomboid.json', prog)
    open(os.path.join(ROOT, 'assets/minecraft/shaders/program/outbreak_zomboid.fsh'), 'w').write(fsh)

    # pack icon
    icon = Image.new('RGBA', (64, 64), (24, 26, 24, 255))
    dd = ImageDraw.Draw(icon)
    dd.ellipse([36, 6, 56, 26], fill=(120, 38, 28, 255))
    for k, (x, w, h) in enumerate([(4, 10, 30), (15, 8, 40), (24, 12, 26), (37, 9, 34), (47, 13, 22)]):
        rect(dd, x, 58 - h, x + w, 58, fill=(10, 11, 11, 255))
    rect(dd, 0, 58, 64, 64, fill=(40, 44, 34, 255))
    icon.save(os.path.join(ROOT, 'pack.png'))
    print('wrote', ROOT)


if __name__ == '__main__':
    main()
