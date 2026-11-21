#!/usr/bin/env python3
"""
Civilian zombie skins for Outbreak (original pixel art, generated): pale, decayed townsfolk in everyday clothes, with
dark eye sockets, a bloody mouth, grime, torn clothing and blood spatter. Each zombie in the game picks one by its UUID.

Layout is the 64x64 biped skin (the zombie model): head (0,0), body (16,16), arm (40,16), leg (0,16); the left
arm and leg mirror the right. The hat layer is left empty.

Usage: python3 tools/make_zombies.py   -> src/main/resources/assets/outbreak/textures/entity/zombie/civilian_N.png
"""
import os
import random

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'outbreak', 'textures', 'entity', 'zombie')
COUNT = 16

SKINS = [(214, 182, 160), (196, 160, 136), (160, 118, 90), (122, 86, 62), (92, 64, 48), (222, 196, 176)]
ROT = np.array([128, 136, 118], float)                 # the grey-green of the dead
HAIR = [(40, 30, 24), (70, 48, 30), (110, 80, 46), (170, 140, 90), (30, 30, 32), (120, 120, 116), (140, 60, 30)]
SHIRTS = [(196, 196, 188), (120, 122, 124), (52, 62, 92), (128, 44, 40), (62, 82, 54), (176, 156, 82), (36, 36, 40),
          (96, 120, 150), (150, 100, 120), (180, 110, 60)]
PANTS = [(58, 74, 108), (64, 82, 120), (150, 134, 100), (42, 42, 46), (110, 110, 112), (90, 80, 60)]
SHOES = [(30, 28, 28), (90, 60, 40), (190, 190, 186), (60, 60, 64)]
BLOOD = np.array([104, 18, 14], float)
BLOOD_DRY = np.array([70, 22, 18], float)


def faces(u, v, w, h, d):
    """the six faces of a model box in the texture: name -> (x, y, width, height)"""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h)}


SHADE = {'top': 1.08, 'bottom': 0.7, 'front': 1.0, 'back': 0.88, 'right': 0.92, 'left': 0.92}


def make(seed):
    r = random.Random(seed)
    img = np.zeros((64, 64, 4), float)

    def put(x, y, c, a=255):
        img[y, x, :3] = c
        img[y, x, 3] = a

    base = np.array(r.choice(SKINS), float)
    skin = base * 0.45 + ROT * 0.55
    skin *= 0.88 + r.random() * 0.1
    hair = np.array(r.choice(HAIR), float)
    style = r.choice(['short', 'short', 'long', 'bald', 'cap', 'buzz'])
    cap = np.array(r.choice(SHIRTS), float)
    shirt = np.array(r.choice(SHIRTS), float)
    under = np.array(r.choice(SHIRTS), float)
    sleeve = r.choice(['tee', 'tee', 'long', 'tank', 'jacket'])
    pants = np.array(r.choice(PANTS), float)
    shorts = r.random() < 0.2
    shoe = np.array(r.choice(SHOES), float)
    plaid = r.random() < 0.18

    def grime(c, x, y, amount=0.12):
        n = (r.random() - 0.5) * amount
        return np.clip(c * (1 + n), 0, 255)

    def skin_at(x, y):
        c = skin.copy()
        if r.random() < 0.08:
            c = c * 0.78 + np.array([90, 60, 90]) * 0.22                # bruising
        return grime(c, x, y, 0.16)

    def cloth(c, x, y):
        cc = c.copy()
        if plaid and c is shirt and ((x // 2 + y // 2) % 2 == 0):
            cc = cc * 0.72
        return grime(cc, x, y, 0.14)

    # ---- head 8x8x8 at (0,0)
    for name, (fx, fy, fw, fh) in faces(0, 0, 8, 8, 8).items():
        for j in range(fh):
            for i in range(fw):
                c = skin_at(i, j)
                if name == 'top':
                    if style in ('short', 'long', 'buzz'):
                        c = grime(hair, i, j) if style != 'buzz' else c * 0.6 + hair * 0.4
                    if style == 'cap':
                        c = grime(cap, i, j)
                elif name in ('right', 'left', 'back'):
                    if style == 'short' and j < 3:
                        c = grime(hair, i, j)
                    if style == 'long' and j < 7:
                        c = grime(hair, i, j)
                    if style == 'buzz' and j < 2:
                        c = c * 0.6 + hair * 0.4
                    if style == 'cap' and j < 2:
                        c = grime(cap, i, j)
                elif name == 'front':
                    if style in ('short', 'long') and j == 0:
                        c = grime(hair, i, j)
                    if style == 'long' and (i == 0 or i == 7) and j < 6:
                        c = grime(hair, i, j)
                    if style == 'cap' and j == 0:
                        c = grime(cap * 0.8, i, j)
                    if j == 3 and i in (1, 2, 5, 6):                          # sunken eyes
                        c = np.array([38, 26, 26]) if i in (2, 5) else skin * 0.55
                    if j == 4 and i in (2, 5) and r.random() < 0.7:
                        c = skin * 0.6                                       # dark rings
                    if j == 4 and i in (3, 4):
                        c = skin * 0.85                                      # nose shadow
                    if j == 6 and 2 <= i <= 5:
                        c = np.array([46, 16, 16])                           # open mouth
                    if j == 7 and 2 <= i <= 5 and r.random() < 0.8:
                        c = BLOOD * (0.9 + r.random() * 0.2)                 # blood down the chin
                    if j == 5 and i in (2, 5) and r.random() < 0.3:
                        c = BLOOD_DRY
                put(fx + i, fy + j, c * SHADE[name])

    # ---- body 8x12x4 at (16,16)
    for name, (fx, fy, fw, fh) in faces(16, 16, 8, 12, 4).items():
        for j in range(fh):
            for i in range(fw):
                if name in ('top', 'bottom'):
                    c = cloth(shirt, i, j) if name == 'top' else cloth(pants, i, j)
                else:
                    c = cloth(shirt, i, j)
                    if sleeve == 'tank' and name == 'front' and j < 2 and 2 <= i <= 5:
                        c = skin_at(i, j)
                    if sleeve == 'jacket' and name == 'front' and 3 <= i <= 4:
                        c = cloth(under, i, j)
                    if name in ('front', 'back') and j == 0 and 3 <= i <= 4 and sleeve != 'jacket':
                        c = skin_at(i, j)                                    # collar
                    if j >= 10:
                        c = cloth(pants, i, j) if j == 11 else c * 0.85       # waistband
                    if j == 10 and name in ('front', 'back'):
                        c = np.array([40, 32, 26])                           # belt
                    if r.random() < 0.05:
                        c = skin_at(i, j) * 0.9                              # a tear
                put(fx + i, fy + j, c * SHADE[name])

    # ---- arm 4x12x4 at (40,16)
    sleeve_len = {'tee': 4, 'long': 11, 'tank': 0, 'jacket': 11}[sleeve]
    for name, (fx, fy, fw, fh) in faces(40, 16, 4, 12, 4).items():
        for j in range(fh):
            for i in range(fw):
                if name == 'top':
                    c = cloth(shirt, i, j) if sleeve != 'tank' else skin_at(i, j)
                elif name == 'bottom':
                    c = skin_at(i, j) * 0.8                                  # hand
                else:
                    c = cloth(shirt, i, j) if j < sleeve_len else skin_at(i, j)
                    if j >= 10:
                        c = skin_at(i, j) * 0.95                             # hands
                    if j >= 10 and r.random() < 0.35:
                        c = BLOOD * (0.85 + r.random() * 0.2)                # bloody hands
                put(fx + i, fy + j, c * SHADE[name])

    # ---- leg 4x12x4 at (0,16)
    for name, (fx, fy, fw, fh) in faces(0, 16, 4, 12, 4).items():
        for j in range(fh):
            for i in range(fw):
                if name == 'top':
                    c = cloth(pants, i, j)
                elif name == 'bottom':
                    c = shoe * 0.6
                else:
                    c = cloth(pants, i, j)
                    if shorts and j >= 5:
                        c = skin_at(i, j)
                    if not shorts and j in (5, 6) and name == 'front' and pants[2] > pants[0] + 20:
                        c = c * 1.1                                          # faded jeans knees
                    if j >= 10:
                        c = grime(shoe, i, j)
                    if j == 11 and shoe.mean() > 150:
                        c = shoe * 0.7                                       # sneaker sole
                put(fx + i, fy + j, c * SHADE[name])

    # ---- blood: a spray across the chest and down the front, a few spots elsewhere
    def splat(fx, fy, fw, fh, n, cx=None, cy=None):
        cx = r.randrange(fw) if cx is None else cx
        cy = r.randrange(fh) if cy is None else cy
        for _ in range(n):
            x = min(fw - 1, max(0, cx + int(r.gauss(0, 1.4))))
            y = min(fh - 1, max(0, cy + int(r.gauss(0, 1.8))))
            c = BLOOD if r.random() < 0.6 else BLOOD_DRY
            img[fy + y, fx + x, :3] = img[fy + y, fx + x, :3] * 0.25 + c * 0.75
    splat(20, 20, 8, 12, 10 + r.randrange(10), r.randrange(2, 6), r.randrange(0, 4))    # chest
    for _ in range(r.randrange(1, 4)):
        splat(*r.choice([(20, 20, 8, 12), (32, 20, 8, 12), (4, 20, 4, 12), (44, 20, 4, 12)]), 4 + r.randrange(6))
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


def front_view(skin):
    """a quick standing front view for checking: head, body, arms, legs"""
    a = skin
    v = Image.new('RGBA', (16, 32), (0, 0, 0, 0))
    v.paste(a.crop((8, 8, 16, 16)), (4, 0))
    v.paste(a.crop((20, 20, 28, 32)), (4, 8))
    arm = a.crop((44, 20, 48, 32))
    v.paste(arm, (0, 8))
    v.paste(arm.transpose(Image.FLIP_LEFT_RIGHT), (12, 8))
    leg = a.crop((4, 20, 8, 32))
    v.paste(leg, (4, 20))
    v.paste(leg.transpose(Image.FLIP_LEFT_RIGHT), (8, 20))
    return v


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    sheet = Image.new('RGBA', (COUNT * 18 * 4, 34 * 4), (60, 66, 60, 255))
    for k in range(COUNT):
        s = make(9000 + k * 31)
        s.save(os.path.join(OUT, 'civilian_%d.png' % k))
        sheet.alpha_composite(front_view(s).resize((64, 128), Image.NEAREST), (k * 72, 4))
    sheet.save('/tmp/zombie_sheet.png')
    print('wrote', COUNT, 'skins to', OUT)
