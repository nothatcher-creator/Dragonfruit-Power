#!/usr/bin/env python3
"""
Big ground decals for Outbreak (original art): blood pools, sprays, drag trails, dried blood, dirt scuffs, oil
stains and skid marks. Each texture spans up to 3x3 blocks in the game (the block itself is still one block), with
soft edges so they sit naturally on the ground. Also writes their block models, blockstates and item models.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageFilter

R = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'outbreak')
S = 96  # texture size (3 blocks at 32px a block)


def noise(rng, cells, size=S):
    g = rng.random((cells + 1, cells + 1))
    xs = np.linspace(0, cells, size, endpoint=False)
    i = xs.astype(int); f = xs - i; f = f * f * (3 - 2 * f)
    a = g[np.ix_(i, i)]; b = g[np.ix_(i, i + 1)]; c = g[np.ix_(i + 1, i)]; d = g[np.ix_(i + 1, i + 1)]
    return (a * (1 - f[None]) + b * f[None]) * (1 - f[:, None]) + (c * (1 - f[None]) + d * f[None]) * f[:, None]


def blob(rng, cx, cy, r, wobble=0.35):
    """an irregular round mask (0..1) centred at cx,cy with radius r (in pixels)"""
    y, x = np.mgrid[0:S, 0:S].astype(float)
    ang = np.arctan2(y - cy, x - cx)
    dist = np.hypot(x - cx, y - cy)
    k = sum(rng.random() * math.sin(n * ang_ + rng.random() * 6.28) for n, ang_ in []) if False else 0
    edge = r * (1 + wobble * (0.6 * np.sin(3 * ang + rng.random() * 6) + 0.4 * np.sin(5 * ang + rng.random() * 6)
                              + 0.3 * np.sin(8 * ang + rng.random() * 6)) / 1.3)
    n = noise(np.random.default_rng(rng.randrange(1 << 30)), 6) * 0.25 * r
    return np.clip((edge + n - dist) / 3.0, 0, 1)


def drops(rng, m, n, cx, cy, spread, rmin=0.8, rmax=2.6, dirx=None):
    y, x = np.mgrid[0:S, 0:S].astype(float)
    for _ in range(n):
        if dirx is None:
            a = rng.random() * 6.283; d = spread * (0.4 + rng.random() * 0.8)
        else:
            a = dirx + rng.gauss(0, 0.45); d = spread * rng.random() ** 0.6
        px, py = cx + math.cos(a) * d, cy + math.sin(a) * d
        rr = rmin + rng.random() * (rmax - rmin)
        if dirx is not None:                                  # stretched along the throw
            dx, dy = x - px, y - py
            u = dx * math.cos(a) + dy * math.sin(a); v = -dx * math.sin(a) + dy * math.cos(a)
            mm = np.clip((rr - np.hypot(u / 1.8, v)) / 1.2, 0, 1)
        else:
            mm = np.clip((rr - np.hypot(x - px, y - py)) / 1.2, 0, 1)
        m = np.maximum(m, mm)
    return m


def colour(mask, rgb, rng, shade=0.25, alpha=235, rim=None, gloss=False):
    n = noise(rng, 8)
    c = np.array(rgb, float)[None, None, :] * (1 - shade / 2 + shade * n[..., None])
    if rim is not None:                                       # edges a touch lighter/darker than the middle
        edge = np.clip(1 - mask * 1.6, 0, 1)[..., None]
        c = c * (1 - edge) + np.array(rim, float)[None, None, :] * edge
    if gloss:
        c += (noise(rng, 14) > 0.82)[..., None] * 25 * mask[..., None]
    a = (mask * alpha).astype(float)
    img = np.dstack([np.clip(c, 0, 255), a])
    return Image.fromarray(img.astype(np.uint8), 'RGBA').filter(ImageFilter.GaussianBlur(0.4))


def pool(seed):
    rng = random.Random(seed); nrng = np.random.default_rng(seed)
    m = blob(rng, S / 2, S / 2, S * 0.3, 0.3)
    for _ in range(4):
        m = np.maximum(m, blob(rng, S / 2 + rng.uniform(-18, 18), S / 2 + rng.uniform(-18, 18), S * rng.uniform(0.12, 0.18)))
    m = drops(rng, m, 30, S / 2, S / 2, S * 0.46, 1.0, 3.2)
    return colour(m, (96, 10, 8), nrng, 0.3, 238, rim=(130, 22, 18), gloss=True)


def spray(seed):
    rng = random.Random(seed); nrng = np.random.default_rng(seed)
    cx, cy = S * 0.3, S * 0.5
    cx = S * 0.22
    m = blob(rng, cx, cy, S * 0.12, 0.5)
    m = drops(rng, m, 130, cx, cy, S * 0.76, 0.7, 3.4, dirx=0.0)
    m = drops(rng, m, 40, cx + 12, cy, S * 0.3, 0.8, 2.2)
    return colour(m, (118, 14, 12), nrng, 0.25, 228)


def drag(seed):
    rng = random.Random(seed); nrng = np.random.default_rng(seed)
    y, x = np.mgrid[0:S, 0:S].astype(float)
    centre = S / 2 + 4 * np.sin(y / S * 5 + rng.random() * 6)
    width = 10 + 7 * noise(np.random.default_rng(seed + 1), 5)
    m = np.clip((width - np.abs(x - centre)) / 3, 0, 1)
    m *= np.clip(np.where(y < S * 0.15, y / (S * 0.15), 1) * np.where(y > S * 0.8, (S - y) / (S * 0.2), 1), 0, 1)
    m = m * (0.75 + 0.25 * noise(np.random.default_rng(seed + 4), 10))
    for k in (-5, 0, 5):                                          # finger streaks at the end of the drag
        m = np.maximum(m, np.clip((1.6 - np.abs(x - centre - k)) / 1.2, 0, 1) * (y > S * 0.7) * (y < S * 0.96))
    m = drops(rng, m, 14, S / 2, S * 0.2, 12)
    return colour(m, (100, 12, 10), nrng, 0.3, 240)


def dried(seed):
    rng = random.Random(seed); nrng = np.random.default_rng(seed)
    m = blob(rng, S / 2, S / 2, S * 0.32, 0.25)
    inner = blob(rng, S / 2, S / 2, S * 0.2, 0.25)
    m = np.clip(m - inner * 0.35, 0, 1)                           # dried into a darker ring
    m = drops(rng, m, 18, S / 2, S / 2, S * 0.46, 1.0, 3.0)
    return colour(m, (66, 24, 18), nrng, 0.35, 225)


def scuff(seed):
    rng = random.Random(seed); nrng = np.random.default_rng(seed)
    y, x = np.mgrid[0:S, 0:S].astype(float)
    m = np.zeros((S, S))
    for _ in range(15):                                           # smears of mud and boot scuffs
        cx, cy = rng.uniform(S * 0.18, S * 0.82), rng.uniform(S * 0.18, S * 0.82)
        a = rng.uniform(0, math.pi); L = rng.uniform(12, 30); W = rng.uniform(3, 7)
        dx, dy = x - cx, y - cy
        u = dx * math.cos(a) + dy * math.sin(a); v = -dx * math.sin(a) + dy * math.cos(a)
        m = np.maximum(m, np.clip(1 - (u / L) ** 2 - (v / W) ** 2, 0, 1) * rng.uniform(0.5, 1))
    m *= 0.6 + 0.4 * noise(np.random.default_rng(seed + 2), 10)
    return colour(m, (66, 54, 40), nrng, 0.3, 195)


def oil(seed):
    rng = random.Random(seed); nrng = np.random.default_rng(seed)
    m = blob(rng, S / 2, S / 2, S * 0.3, 0.3)
    m = np.maximum(m, blob(rng, S / 2 + 12, S / 2 - 9, S * 0.18, 0.3))
    img = colour(m, (24, 22, 20), nrng, 0.2, 200, gloss=True)
    a = np.asarray(img).astype(float)                             # a faint rainbow sheen
    y, x = np.mgrid[0:S, 0:S].astype(float)
    sheen = (np.sin((x + y) / 5) * 0.5 + 0.5) * (blob(rng, S / 2 + 3, S / 2 - 3, S * 0.09, 0.2))
    a[..., 0] += sheen * 26; a[..., 1] += sheen * 10; a[..., 2] += sheen * 34
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA')


def skid(seed):
    rng = random.Random(seed); nrng = np.random.default_rng(seed)
    y, x = np.mgrid[0:S, 0:S].astype(float)
    m = np.zeros((S, S))
    bend = rng.uniform(-10, 10)
    for off in (-14, 14):
        cx = S / 2 + off + bend * (y / S) ** 2
        m = np.maximum(m, np.clip((5 - np.abs(x - cx)) / 2, 0, 1))
    m *= np.clip(np.minimum(y / 12, (S - y) / 30), 0, 1)
    m *= 0.55 + 0.45 * noise(np.random.default_rng(seed + 3), 12)
    return colour(m, (20, 20, 22), nrng, 0.2, 170)


# name: (painter, model x range, model z range, height)  ranges in 1/16ths of a block
DECALS = {
    'blood_pool': (pool, (-16, 32), (-16, 32), 0.07),
    'blood_spray': (spray, (-16, 32), (-16, 32), 0.11),
    'blood_drag': (drag, (-16, 32), (-16, 32), 0.15),
    'dried_blood': (dried, (-16, 32), (-16, 32), 0.09),
    'dirt_scuff': (scuff, (-16, 32), (-16, 32), 0.04),
    'oil_stain': (oil, (-8, 24), (-8, 24), 0.05),
    'skid_marks': (skid, (-16, 32), (-16, 32), 0.03),
}


def main():
    for name, (paint, xr, zr, h) in DECALS.items():
        paint(sum(ord(ch) * (i + 7) for i, ch in enumerate(name))).save(os.path.join(R, 'textures', 'block', name + '.png'))
        model = {"ambientocclusion": False, "textures": {"particle": "outbreak:block/" + name, "t": "outbreak:block/" + name},
                 "elements": [{"from": [xr[0], h, zr[0]], "to": [xr[1], h + 0.01, zr[1]], "shade": False,
                               "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#t"}}}]}
        json.dump(model, open(os.path.join(R, 'models', 'block', name + '.json'), 'w'), indent=1)
        json.dump({"variants": {"": [{"model": "outbreak:block/" + name, "y": y} for y in (0, 90, 180, 270)]}},
                  open(os.path.join(R, 'blockstates', name + '.json'), 'w'), indent=1)
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "outbreak:block/" + name}},
                  open(os.path.join(R, 'models', 'item', name + '.json'), 'w'), indent=1)
    sheet = Image.new('RGBA', (len(DECALS) * 100, 100), (66, 66, 64, 255))
    for k, name in enumerate(DECALS):
        sheet.alpha_composite(Image.open(os.path.join(R, 'textures', 'block', name + '.png')), (k * 100 + 2, 2))
    sheet.save('/tmp/decals.png')


if __name__ == '__main__':
    main()
