"""Wave 2 art + data: more guns, melee, clothing (items + worn layers), decay blocks, worldgen feature, loot, recipes, lang."""
import json, os, sys, random
R = sys.argv[1]
_argv = sys.argv
exec(open(os.path.join(os.path.dirname(__file__), 'make_assets.py')).read())   # wave 1 + helpers (C, hexc, shade...)
rnd = random.Random(5)

# ---------- more guns ----------
def silenced():
    c = pistol(); im = c.im
    c2 = C(32, 32); c2.im.paste(im.crop((0, 0, 28, 32)), (0, 0))
    c2.rect(27, 11, 31, 14, BLK, top=BLK3)
    c2.outline(); return c2
def revolver():
    c = C(32, 32)
    c.rect(18, 11, 28, 13, STEEL, top=STEEL2, bot=BLK3); c.px(27, 10, STEEL)          # barrel + front sight
    c.rect(18, 14, 24, 14, BLK3)                                                     # ejector rod
    c.rect(12, 10, 18, 16, BLK3, top=STEEL, bot=BLK2)                                # cylinder
    for y in (11, 13, 15): c.px(15, y, BLK)
    c.px(11, 9, BLK3); c.px(10, 9, BLK3)                                             # hammer
    c.rect(10, 10, 12, 16, BLK2)                                                     # frame
    for i in range(8): c.rect(8 + i // 4 - i // 6, 16 + i, 12 - i // 5, 16 + i, WOOD2 if i % 2 else WOOD)  # grip
    c.rect(13, 17, 16, 17, BLK); c.px(16, 18, BLK); c.rect(13, 19, 15, 19, BLK); c.px(14, 18, STEEL)
    c.outline(); return c
def double_barrel():
    c = C(32, 32)
    for i in range(8): c.rect(1 + i, 15 - i // 3, 1 + i, 20 - i // 2, WOOD2 if i % 2 else WOOD)
    c.rect(9, 13, 13, 17, BLK3, top=STEEL)
    c.rect(13, 12, 31, 13, STEEL, top=STEEL2); c.rect(13, 14, 31, 15, STEEL, bot=BLK3)
    c.rect(15, 16, 21, 17, WOOD2, top=WOOD)
    c.rect(10, 18, 12, 19, BLK)
    c.outline(); return c
def sniper():
    c = C(32, 32); OL = hexc('4b5232'); OL2 = hexc('5f6840')
    for i in range(8): c.rect(1 + i, 14 - i // 4, 1 + i, 20 - i // 2, OL2 if i % 2 else OL)
    c.rect(2, 12, 6, 13, OL)                                                         # cheek rest
    c.rect(9, 13, 18, 17, OL, top=OL2, bot=shade(OL, 0.7))
    c.rect(18, 14, 31, 15, BLK, top=BLK3); c.rect(29, 13, 31, 16, BLK2)              # barrel + brake
    c.rect(9, 8, 21, 11, BLK, top=BLK3); c.rect(7, 7, 9, 12, BLK2); c.rect(21, 7, 23, 12, BLK2)
    c.px(23, 9, hexc('6fb4e0')); c.px(7, 9, hexc('6fb4e0')); c.px(14, 7, BLK2); c.px(14, 6, BLK2)
    c.line(24, 16, 22, 21, BLK3); c.line(25, 16, 27, 21, BLK3)                       # bipod
    c.rect(11, 18, 13, 20, BLK)
    c.outline(); return c
def lmg():
    c = C(32, 32); OL = hexc('4b5232')
    c.rect(1, 13, 7, 18, BLK2, top=BLK3, bot=BLK)
    c.rect(7, 12, 20, 18, BLK2, top=BLK3, bot=BLK)
    c.rect(11, 9, 17, 10, BLK); c.px(11, 11, BLK); c.px(17, 11, BLK)                 # carry handle
    c.rect(20, 13, 27, 16, BLK3, top=STEEL, bot=BLK)
    for x in range(21, 27, 2): c.px(x, 14, BLK)
    c.rect(27, 14, 31, 15, BLK)
    c.rect(12, 19, 18, 24, OL, top=shade(OL, 1.2), bot=shade(OL, 0.7))               # box mag
    c.rect(8, 19, 10, 23, BLK2)
    c.line(25, 17, 23, 22, BLK3); c.line(26, 17, 28, 22, BLK3)
    c.outline(); return c
for name, fn in (('silenced_pistol', silenced), ('revolver', revolver), ('double_barrel', double_barrel),
                 ('sniper_rifle', sniper), ('light_machine_gun', lmg)):
    fn().save(f'{ITEM}/{name}.png')
def magnum():
    c = C()
    for i in range(3):
        x = 2 + i * 5
        c.rect(x, 7, x + 3, 13, hexc('c9a64a'), top=hexc('e0c060'), bot=hexc('8a6a28'))
        c.rect(x, 4, x + 3, 6, hexc('8a8f98')); c.rect(x + 1, 3, x + 2, 3, hexc('8a8f98'))
    c.outline(); return c
magnum().save(f'{ITEM}/ammo_magnum.png')

# ---------- more melee ----------
def handle(c, col, x0=2, y0=13, x1=7, y1=8): diag(c, x0, y0, x1, y1, col, 2)
def fire_axe():
    c = C(); handle(c, hexc('c42a2a'), 2, 13, 11, 4)
    c.rect(9, 1, 13, 5, hexc('c42a2a')); c.rect(12, 0, 14, 6, STEEL2); c.rect(8, 2, 9, 3, BLK3)
    c.outline(); return c
def knife():
    c = C(); handle(c, hexc('222222'), 2, 13, 5, 10)
    for i in range(8): c.rect(6 + i, 9 - i, 6 + i, 9 - i, STEEL2); c.px(6 + i, 10 - i, STEEL)
    c.outline(); return c
def pan():
    c = C(); handle(c, hexc('222222'), 1, 14, 6, 9)
    for y in range(16):
        for x in range(16):
            d = (x - 10.5) ** 2 + (y - 5.5) ** 2
            if d <= 20: c.px(x, y, BLK2 if d > 12 else BLK)
    c.outline(); return c
def golf():
    c = C(); diag(c, 2, 14, 12, 4, STEEL, 1); c.rect(1, 13, 3, 15, hexc('222222'))
    c.rect(11, 1, 14, 3, STEEL2); c.px(10, 2, STEEL)
    c.outline(); return c
def hockey():
    c = C(); diag(c, 2, 13, 11, 4, hexc('c89a5a'), 2); c.rect(1, 13, 3, 15, hexc('222222'))
    c.rect(11, 2, 15, 4, hexc('222222')); c.rect(14, 1, 15, 2, hexc('c89a5a'))
    c.outline(); return c
def wrench():
    c = C(); handle(c, hexc('c42a2a'), 2, 13, 10, 5)
    c.rect(9, 2, 12, 6, STEEL); c.rect(12, 1, 14, 3, STEEL2); c.rect(12, 5, 14, 6, STEEL2); c.px(11, 4, BLK3)
    c.outline(); return c
def baton():
    c = C(); diag(c, 2, 13, 13, 2, hexc('1a1a1a'), 2); c.rect(4, 9, 7, 10, hexc('333333')); c.px(13, 2, BLK3)
    c.outline(); return c
def hammer():
    c = C(); handle(c, hexc('222222'), 2, 13, 9, 6)
    c.rect(7, 2, 13, 4, STEEL); c.rect(13, 1, 14, 5, STEEL2); c.px(6, 1, STEEL); c.px(5, 0, STEEL); c.px(6, 5, STEEL); c.px(5, 6, STEEL)
    c.outline(); return c
for n, fn in (('fire_axe', fire_axe), ('kitchen_knife', knife), ('frying_pan', pan), ('golf_club', golf), ('hockey_stick', hockey),
              ('pipe_wrench', wrench), ('police_baton', baton), ('claw_hammer', hammer)):
    fn().save(f'{ITEM}/{n}.png')

# ---------- clothing icons ----------
def shirt(col, sleeves=True, hood=None, front=None, stripes=None, vest=False):
    c = C()
    c.rect(4, 3, 11, 14, col, top=shade(col, 1.2), bot=shade(col, 0.75))
    if sleeves and not vest:
        c.rect(1, 3, 3, 9, col, top=shade(col, 1.2)); c.rect(12, 3, 14, 9, col, top=shade(col, 1.2))
        c.rect(1, 9, 3, 9, shade(col, 0.75)); c.rect(12, 9, 14, 9, shade(col, 0.75))
    c.rect(6, 3, 9, 4, (0, 0, 0, 0)) if not hood else c.rect(5, 1, 10, 3, hood)
    if front: c.rect(7, 5, 8, 14, front)
    if stripes:
        for y in (9, 12): c.rect(4, y, 11, y, stripes)
        if sleeves and not vest: c.rect(1, 7, 3, 7, stripes); c.rect(12, 7, 14, 7, stripes)
    if vest: c.rect(6, 3, 9, 6, (0, 0, 0, 0))
    c.outline(); return c
def pants(col, pockets=False, stripe=None):
    c = C()
    c.rect(4, 1, 11, 4, col, top=shade(col, 0.6)); c.rect(4, 5, 7, 14, col); c.rect(8, 5, 11, 14, col)
    c.rect(7, 6, 8, 14, (0, 0, 0, 0)); c.rect(4, 14, 7, 14, shade(col, 0.7)); c.rect(8, 14, 11, 14, shade(col, 0.7))
    c.px(7, 5, shade(col, 0.8))
    if pockets: c.rect(4, 7, 5, 9, shade(col, 0.8)); c.rect(10, 7, 11, 9, shade(col, 0.8))
    if stripe: c.rect(4, 11, 7, 11, stripe); c.rect(8, 11, 11, 11, stripe)
    c.outline(); return c
def shoe(col, sole, tall=False, swoosh=None):
    c = C()
    top = 4 if tall else 8
    c.rect(4, top, 8, 12, col, top=shade(col, 1.2)); c.rect(8, 9, 13, 12, col, top=shade(col, 1.15))
    c.rect(3, 13, 13, 13, sole)
    if swoosh: c.line(5, 11, 11, 10, swoosh)
    if tall: c.rect(4, 6, 8, 6, shade(col, 0.7))
    c.outline(); return c
def hat(kind):
    c = C()
    if kind == 'beanie':
        col = hexc('2f3a4a')
        for y in range(3, 12):
            for x in range(2, 14):
                if (x - 7.5) ** 2 / 36 + (y - 11) ** 2 / 64 <= 1: c.px(x, y, col if y < 9 else shade(col, 1.3))
        c.rect(6, 2, 9, 3, hexc('c0c0c0'))
    elif kind == 'baseball_cap':
        col = hexc('b02a2a')
        for y in range(4, 11):
            for x in range(2, 12):
                if (x - 6.5) ** 2 / 25 + (y - 10) ** 2 / 36 <= 1: c.px(x, y, col)
        c.rect(10, 9, 15, 10, shade(col, 0.8)); c.px(6, 4, shade(col, 0.7))
    elif kind == 'hard_hat':
        col = hexc('f0c020')
        for y in range(3, 11):
            for x in range(3, 13):
                if (x - 7.5) ** 2 / 25 + (y - 10) ** 2 / 49 <= 1: c.px(x, y, col)
        c.rect(1, 10, 14, 11, shade(col, 0.85)); c.rect(7, 3, 8, 10, shade(col, 1.15))
    elif kind == 'firefighter_helmet':
        col = hexc('1a1a1a')
        for y in range(3, 11):
            for x in range(3, 13):
                if (x - 7.5) ** 2 / 25 + (y - 10) ** 2 / 49 <= 1: c.px(x, y, col)
        c.rect(1, 10, 15, 11, shade(col, 1.6)); c.rect(6, 5, 9, 8, hexc('e0b030'))
    elif kind == 'gas_mask':
        col = hexc('3a3f3a')
        for y in range(2, 14):
            for x in range(2, 14):
                if (x - 7.5) ** 2 / 30 + (y - 7.5) ** 2 / 36 <= 1: c.px(x, y, col)
        for cx in (5, 10):
            for y in range(4, 8):
                for x in range(cx - 2, cx + 2):
                    if (x - cx + 0.5) ** 2 + (y - 5.5) ** 2 <= 3: c.px(x, y, hexc('9ab8c8'))
        c.rect(6, 10, 9, 14, hexc('5a5a5a')); c.rect(6, 14, 9, 14, hexc('8a8a8a'))
    c.outline(); return c
shirt(hexc('5a6a7a'), hood=hexc('4a5868')).save(f'{ITEM}/hoodie.png')
shirt(hexc('4a3020'), front=hexc('1e1e1e')).save(f'{ITEM}/leather_jacket.png')
shirt(hexc('8a7a50'), stripes=hexc('e8e040')).save(f'{ITEM}/firefighter_jacket.png')
shirt(hexc('c8f020'), stripes=hexc('d8d8d8'), vest=True, sleeves=False).save(f'{ITEM}/hi_vis_vest.png')
pants(hexc('2f4f7f')).save(f'{ITEM}/jeans.png')
pants(hexc('7a7050'), pockets=True).save(f'{ITEM}/cargo_pants.png')
pants(hexc('8a7a50'), stripe=hexc('e8e040')).save(f'{ITEM}/firefighter_pants.png')
shoe(hexc('f0f0f0'), hexc('444444'), swoosh=hexc('2a6fd0')).save(f'{ITEM}/sneakers.png')
shoe(hexc('6a4422'), hexc('2a2a2a'), tall=True).save(f'{ITEM}/work_boots.png')
for k in ('beanie', 'baseball_cap', 'hard_hat', 'firefighter_helmet', 'gas_mask'): hat(k).save(f'{ITEM}/{k}.png')

# ---------- worn clothing layers (64x32) ----------
def region(c, x0, y0, x1, y1, col, noise=0):
    for y in range(y0, y1):
        for x in range(x0, x1):
            cc = col
            if noise: f = 1 + rnd.uniform(-noise, noise); cc = shade(col, f)
            c.px(x, y, cc)
BODY = [(20, 16, 28, 20), (28, 16, 36, 20), (16, 20, 40, 32)]
ARMS = [(44, 16, 48, 20), (48, 16, 52, 20), (40, 20, 56, 32)]
LEGS = [(4, 16, 8, 20), (8, 16, 12, 20), (0, 20, 16, 32)]
HEAD_TOP = (8, 0, 16, 8)
def head_band(c, col, rows, top=True, noise=0.04):
    if top: region(c, *HEAD_TOP, col, noise)
    region(c, 0, 8, 32, 8 + rows, col, noise)
def wear_body(c, col, arms=True, noise=0.05):
    for r in BODY: region(c, *r, col, noise)
    if arms:
        for r in ARMS: region(c, *r, col, noise)
def layers(name, l1=None, l2=None):
    (l1 or C(64, 32)).save(f'{AR}/outbreak_{name}_layer_1.png')
    (l2 or C(64, 32)).save(f'{AR}/outbreak_{name}_layer_2.png')
# tops
c = C(64, 32); g = hexc('5a6a7a'); wear_body(c, g)
region(c, 21, 27, 27, 31, shade(g, 0.85)); c.px(22, 21, hexc('e0e0e0')); c.px(25, 21, hexc('e0e0e0')); c.px(22, 22, hexc('e0e0e0')); c.px(25, 22, hexc('e0e0e0'))
region(c, 32, 20, 40, 23, shade(g, 0.8))  # hood bunched on the back
layers('hoodie', c)
c = C(64, 32); b = hexc('4a3020'); wear_body(c, b, noise=0.08)
region(c, 23, 20, 25, 32, hexc('1e1e1e')); region(c, 20, 20, 23, 22, shade(b, 1.25)); region(c, 25, 20, 28, 22, shade(b, 1.25))
for x in (21, 26): c.px(x, 28, hexc('b0b0b0'))
layers('leather_jacket', c)
c = C(64, 32); t = hexc('8a7a50'); Y = hexc('e8e040'); S = hexc('d8d8d8'); wear_body(c, t)
for y in (26, 30): region(c, 16, y, 40, y + 1, Y)
region(c, 40, 26, 56, 27, Y)
region(c, 23, 20, 25, 32, shade(t, 0.75))
layers('firefighter_jacket', c)
c = C(64, 32); v = hexc('c8f020'); wear_body(c, v, arms=False)
for y in (25, 29): region(c, 16, y, 40, y + 1, S)
region(c, 22, 20, 26, 23, (0, 0, 0, 0))
layers('hi_vis_vest', c)
# bottoms (layer 2: legs + waist of the body)
def pants_layer(col, stripe=None, pockets=False):
    c = C(64, 32)
    for r in LEGS: region(c, *r, col, 0.06)
    region(c, 16, 28, 40, 32, col, 0.04); region(c, 16, 28, 40, 29, shade(col, 0.55))  # waist + belt
    for x in (3, 7, 11, 15): region(c, x, 20, x + 1, 32, shade(col, 0.85))         # seams
    if stripe: region(c, 0, 28, 16, 29, stripe)
    if pockets: region(c, 0, 24, 4, 27, shade(col, 0.8)); region(c, 8, 24, 12, 27, shade(col, 0.8))
    return c
layers('jeans', None, pants_layer(hexc('2f4f7f')))
layers('cargo_pants', None, pants_layer(hexc('7a7050'), pockets=True))
layers('firefighter_pants', None, pants_layer(hexc('8a7a50'), stripe=hexc('e8e040')))
# shoes (layer 1, legs, bottom rows)
def shoes_layer(col, sole, rows, stripe=None):
    c = C(64, 32)
    region(c, 0, 32 - rows, 16, 32, col, 0.05); region(c, 0, 31, 16, 32, sole); region(c, 8, 16, 12, 20, sole)
    if stripe: region(c, 0, 30 - rows // 2, 16, 31 - rows // 2, stripe)
    return c
layers('sneakers', shoes_layer(hexc('f0f0f0'), hexc('444444'), 3, hexc('2a6fd0')))
layers('work_boots', shoes_layer(hexc('6a4422'), hexc('2a2a2a'), 5))
# headwear (layer 1, head)
c = C(64, 32); head_band(c, hexc('2f3a4a'), 4); region(c, 0, 11, 32, 12, shade(hexc('2f3a4a'), 1.3)); layers('beanie', c)
c = C(64, 32); head_band(c, hexc('b02a2a'), 2); region(c, 8, 10, 16, 11, shade(hexc('b02a2a'), 0.7)); layers('baseball_cap', c)
c = C(64, 32); head_band(c, hexc('f0c020'), 3, noise=0.02); region(c, 0, 11, 32, 12, shade(hexc('f0c020'), 0.8)); layers('hard_hat', c)
c = C(64, 32); head_band(c, hexc('1a1a1a'), 4, noise=0.02); region(c, 11, 9, 13, 11, hexc('e0b030')); region(c, 24, 11, 32, 13, hexc('2a2a2a')); layers('firefighter_helmet', c)
c = C(64, 32); M = hexc('3a3f3a')
region(c, 8, 11, 16, 16, M); region(c, 0, 11, 8, 12, M); region(c, 16, 11, 24, 12, M); region(c, 24, 11, 32, 12, M)
for cx in (9, 13): region(c, cx, 11, cx + 2, 13, hexc('9ab8c8'))
region(c, 11, 14, 13, 16, hexc('5a5a5a'))
layers('gas_mask', c)

# ---------- decay blocks ----------
BT = mk(os.path.join(A, 'textures/block'))
def blobs(cols, n, rmax, alpha=255, seed=0):
    rr = random.Random(seed); c = C()
    for _ in range(n):
        cx, cy, r = rr.uniform(2, 14), rr.uniform(2, 14), rr.uniform(0.8, rmax)
        col = rr.choice(cols)
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r: c.px(x, y, col[:3] + (alpha,))
    return c
blobs([hexc('6a0a0a'), hexc('8a1010'), hexc('4a0606')], 14, 2.6, 235, 3).save(f'{BT}/blood_splatter.png')
lit = C(); rr = random.Random(8)
for _ in range(9):
    x, y = rr.randint(0, 13), rr.randint(0, 13); w_, h_ = rr.randint(1, 3), rr.randint(1, 2)
    col = rr.choice([hexc('e8e8e0'), hexc('d8d0b8'), hexc('c03030'), hexc('3060c0'), hexc('d0b040'), hexc('a0a0a0')])
    lit.rect(x, y, x + w_, y + h_, col)
lit.save(f'{BT}/litter.png')
gl = C(); rr = random.Random(9)
for _ in range(12):
    x, y = rr.randint(1, 14), rr.randint(1, 14)
    gl.px(x, y, (200, 230, 240, 200)); gl.px(x + rr.choice((-1, 1)), y, (170, 210, 225, 180))
    if rr.random() < 0.5: gl.px(x, y + 1, (230, 245, 250, 220))
gl.save(f'{BT}/broken_glass.png')
sc = C()
for y in range(16):
    for x in range(16):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 + random.Random(x * 31 + y).uniform(-1.5, 1.5)
        if d < 7.5: sc.px(x, y, (18, 16, 14, int(230 * min(1, (7.5 - d) / 3))))
sc.save(f'{BT}/scorch_mark.png')
wd = C(); rr = random.Random(4)
for k in range(9):
    x = rr.randint(1, 14); h = rr.randint(5, 13); col = rr.choice([hexc('4a7a2a'), hexc('5a8a30'), hexc('3a6020'), hexc('7a8a3a')])
    for y in range(16 - h, 16):
        wd.px(x + (1 if (y < 16 - h * 2 // 3 and k % 2) else 0), y, col)
wd.save(f'{BT}/weeds.png')
tb = C()
for y in range(16):
    for x in range(16):
        v = 26 + int(18 * max(0, 1 - abs(x - 5 - y * 0.3) / 3)) + random.Random(x + y * 16).randint(-3, 3)
        tb.px(x, y, (v, v, v + 3, 255))
tb.save(f'{BT}/trash_bag.png')
BM = mk(os.path.join(A, 'models/block')); BS = mk(os.path.join(A, 'blockstates'))
for n in ('blood_splatter', 'litter', 'broken_glass', 'scorch_mark'):
    json.dump({'parent': 'block/block', 'ambientocclusion': False, 'textures': {'particle': f'outbreak:block/{n}', 't': f'outbreak:block/{n}'},
               'elements': [{'from': [0, 0.05, 0], 'to': [16, 0.1, 16], 'shade': False,
                             'faces': {'up': {'uv': [0, 0, 16, 16], 'texture': '#t'}, 'down': {'uv': [0, 16, 16, 0], 'texture': '#t'}}}]},
              open(f'{BM}/{n}.json', 'w'), indent=1)
    json.dump({'variants': {'': [{'model': f'outbreak:block/{n}', 'y': y} for y in (0, 90, 180, 270)]}}, open(f'{BS}/{n}.json', 'w'), indent=1)
    json.dump({'parent': 'item/generated', 'textures': {'layer0': f'outbreak:block/{n}'}}, open(f'{MD}/{n}.json', 'w'), indent=1)
json.dump({'parent': 'block/cross', 'textures': {'cross': 'outbreak:block/weeds'}}, open(f'{BM}/weeds.json', 'w'), indent=1)
json.dump({'variants': {'': {'model': 'outbreak:block/weeds'}}}, open(f'{BS}/weeds.json', 'w'), indent=1)
json.dump({'parent': 'item/generated', 'textures': {'layer0': 'outbreak:block/weeds'}}, open(f'{MD}/weeds.json', 'w'), indent=1)
def box(f, t):
    return {'from': f, 'to': t, 'faces': {d: {'texture': '#t'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}
json.dump({'parent': 'block/block', 'textures': {'particle': 'outbreak:block/trash_bag', 't': 'outbreak:block/trash_bag'},
           'elements': [box([3, 0, 3], [13, 8, 13]), box([4, 8, 4], [12, 9.5, 12]), box([6.5, 9.5, 6.5], [9.5, 11.5, 9.5])]},
          open(f'{BM}/trash_bag.json', 'w'), indent=1)
json.dump({'variants': {'': [{'model': 'outbreak:block/trash_bag', 'y': y} for y in (0, 90, 180, 270)]}}, open(f'{BS}/trash_bag.json', 'w'), indent=1)
json.dump({'parent': 'outbreak:block/trash_bag'}, open(f'{MD}/trash_bag.json', 'w'), indent=1)

# ---------- item models ----------
for g in ('silenced_pistol', 'revolver', 'double_barrel', 'sniper_rifle', 'light_machine_gun'):
    json.dump({'parent': 'item/generated', 'textures': {'layer0': f'outbreak:item/{g}'}, 'display': gun_display}, open(f'{MD}/{g}.json', 'w'), indent=1)
for m in ('fire_axe', 'kitchen_knife', 'frying_pan', 'golf_club', 'hockey_stick', 'pipe_wrench', 'police_baton', 'claw_hammer'):
    json.dump({'parent': 'item/handheld', 'textures': {'layer0': f'outbreak:item/{m}'}}, open(f'{MD}/{m}.json', 'w'), indent=1)
CLOTHES = ['beanie', 'baseball_cap', 'hard_hat', 'firefighter_helmet', 'gas_mask', 'hoodie', 'leather_jacket', 'firefighter_jacket',
           'hi_vis_vest', 'jeans', 'cargo_pants', 'firefighter_pants', 'sneakers', 'work_boots']
for o in CLOTHES + ['ammo_magnum']:
    json.dump({'parent': 'item/generated', 'textures': {'layer0': f'outbreak:item/{o}'}}, open(f'{MD}/{o}.json', 'w'), indent=1)

# ---------- sounds ----------
snd = json.load(open(os.path.join(A, 'sounds.json')))
for s in ('silenced_fire', 'revolver_fire', 'sniper_fire'):
    snd[s] = {'sounds': [{'name': f'outbreak:{s}', 'attenuation_distance': 24 if s == 'silenced_fire' else 96}], 'subtitle': f'subtitles.outbreak.{s}'}
json.dump(snd, open(os.path.join(A, 'sounds.json'), 'w'), indent=1)

# ---------- lang ----------
LP = os.path.join(A, 'lang/en_us.json'); lang = json.load(open(LP))
more = {'ammo_magnum': '.357 Magnum Rounds', 'silenced_pistol': 'Silenced Pistol', 'revolver': '.357 Revolver', 'double_barrel': 'Double-Barrel Shotgun',
        'sniper_rifle': 'Sniper Rifle', 'light_machine_gun': 'Light Machine Gun', 'fire_axe': 'Fire Axe', 'kitchen_knife': 'Kitchen Knife',
        'frying_pan': 'Frying Pan', 'golf_club': 'Golf Club', 'hockey_stick': 'Hockey Stick', 'pipe_wrench': 'Pipe Wrench', 'police_baton': 'Police Baton',
        'claw_hammer': 'Claw Hammer', 'beanie': 'Beanie', 'baseball_cap': 'Baseball Cap', 'hard_hat': 'Hard Hat', 'firefighter_helmet': 'Firefighter Helmet',
        'gas_mask': 'Gas Mask', 'hoodie': 'Hoodie', 'leather_jacket': 'Leather Jacket', 'firefighter_jacket': 'Firefighter Jacket', 'hi_vis_vest': 'Hi-Vis Vest',
        'jeans': 'Jeans', 'cargo_pants': 'Cargo Pants', 'firefighter_pants': 'Firefighter Pants', 'sneakers': 'Sneakers', 'work_boots': 'Work Boots'}
for k, v in more.items(): lang[f'item.outbreak.{k}'] = v
for k, v in {'blood_splatter': 'Blood Splatter', 'litter': 'Litter', 'broken_glass': 'Broken Glass', 'scorch_mark': 'Scorch Mark',
             'weeds': 'Weeds', 'trash_bag': 'Trash Bag'}.items():
    lang[f'block.outbreak.{k}'] = v
for k in [k for k in lang if k.startswith('gamerule.')]: del lang[k]
lang.update({
    'gamerule.outbreakZombies': 'Outbreak: zombies keep turning up', 'gamerule.outbreakDaylightZombies': 'Outbreak: zombies survive daylight',
    'gamerule.outbreakInfection': 'Outbreak: bites can infect', 'gamerule.outbreakMaxZombies': 'Outbreak: most zombies near each player',
    'gamerule.outbreakSprinterChance': 'Outbreak: % of zombies that sprint',
    'gamerule.outbreakAreaPopulation': 'Outbreak: zombies living in each 128x128 area',
    'key.outbreak.shove': 'Shove', 'key.outbreak.actions': 'Action menu (Ctrl also works when standing still)',
    'subtitles.outbreak.silenced_fire': 'Suppressed shot', 'subtitles.outbreak.revolver_fire': 'Gunshot', 'subtitles.outbreak.sniper_fire': 'Rifle shot',
    'action.outbreak.menu': 'Actions', 'action.outbreak.use': 'Using', 'action.outbreak.open': 'Open', 'action.outbreak.close': 'Close',
    'action.outbreak.search': 'Search', 'action.outbreak.loot_all': 'Loot all', 'action.outbreak.cook': 'Cook', 'action.outbreak.craft': 'Craft',
    'action.outbreak.sleep': 'Sleep', 'action.outbreak.smash': 'Smash window', 'action.outbreak.fill_bottle': 'Fill bottle with water',
    'action.outbreak.enter': 'Get in', 'action.outbreak.search_vehicle': 'Search vehicle', 'action.outbreak.shove': 'Shove',
    'action.outbreak.reload': 'Reload', 'action.outbreak.apply': 'Use %s', 'action.outbreak.take': 'Take %s', 'action.outbreak.eat': 'Eat %s',
    'action.outbreak.drink': 'Drink %s', 'action.outbreak.open_backpack': 'Open backpack', 'action.outbreak.listen': 'Listen for zombies',
    'action.outbreak.use_item': 'Using %s', 'action.outbreak.listening': 'Listening...',
    'msg.outbreak.cancelled': 'Action cancelled', 'msg.outbreak.cut_hand': 'You cut your hand on the glass.',
    'msg.outbreak.no_bottle': 'You need an empty glass bottle.', 'msg.outbreak.no_ammo': 'No ammo for this gun.',
    'msg.outbreak.not_hungry': 'You\'re not hungry.', 'msg.outbreak.empty': 'There\'s nothing in here.',
    'msg.outbreak.inventory_full': 'Took %s items. Your pockets are full.', 'msg.outbreak.took': 'Took %s items.',
    'msg.outbreak.vehicle_empty': 'Someone already went through this one.', 'msg.outbreak.vehicle_nothing': 'Nothing useful in here.',
    'msg.outbreak.vehicle_found': 'You found %s things in the vehicle.',
    'msg.outbreak.area_crawling': 'You can hear them everywhere. This area is crawling.',
    'msg.outbreak.area_busy': 'Plenty of shuffling and moaning nearby.',
    'msg.outbreak.area_thinning': 'Only a few left around here.',
    'key.outbreak.fog': 'Toggle eerie fog', 'msg.outbreak.fog_on': 'Fog on', 'msg.outbreak.fog_off': 'Fog off',
    'msg.outbreak.area_quiet': 'Quiet. You\'ve cleared most of this area.',
})
json.dump(lang, open(LP, 'w'), indent=1)

# ---------- loot + worldgen ----------
LT = mk(os.path.join(D, 'outbreak/loot_tables/gameplay'))
def entry(item, w, lo=1, hi=1):
    e = {'type': 'minecraft:item', 'name': item, 'weight': w}
    if hi > 1: e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    return e
json.dump({'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 3}, 'entries': [
    {'type': 'minecraft:empty', 'weight': 6}, entry('outbreak:bottled_water', 8, 1, 2), entry('outbreak:chips', 8, 1, 2), entry('outbreak:energy_bar', 5),
    entry('outbreak:bandage', 5, 1, 2), entry('outbreak:painkillers', 3), entry('outbreak:ammo_9mm', 4, 3, 10), entry('outbreak:ammo_shells', 2, 2, 5),
    entry('minecraft:paper', 5, 1, 3), entry('minecraft:string', 3, 1, 3), entry('outbreak:crowbar', 1), entry('outbreak:sunglasses' if False else 'outbreak:baseball_cap', 2),
    entry('minecraft:map', 2), entry('outbreak:claw_hammer', 1)]}]}, open(f'{LT}/vehicle_search.json', 'w'), indent=1)
LB = mk(os.path.join(D, 'outbreak/loot_tables/blocks'))
json.dump({'type': 'minecraft:block', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}, 'entries': [
    {'type': 'minecraft:empty', 'weight': 8}, entry('minecraft:paper', 6, 1, 3), entry('minecraft:rotten_flesh', 3), entry('minecraft:bone', 2),
    entry('outbreak:chips', 2), entry('minecraft:glass_bottle', 4), entry('minecraft:string', 2), entry('outbreak:bandage', 1)]}]},
    open(f'{LB}/trash_bag.json', 'w'), indent=1)
WG = os.path.join(D, 'outbreak/worldgen')
mk(f'{WG}/configured_feature'); mk(f'{WG}/placed_feature')
json.dump({'type': 'outbreak:overrun', 'config': {}}, open(f'{WG}/configured_feature/overrun.json', 'w'), indent=1)
json.dump({'feature': 'outbreak:overrun', 'placement': []}, open(f'{WG}/placed_feature/overrun.json', 'w'), indent=1)

# ---------- recipes ----------
shapeless('ammo_magnum', ['minecraft:iron_nugget', 'minecraft:iron_nugget', 'minecraft:gunpowder', 'minecraft:gunpowder'], 6)
shapeless('silenced_pistol', ['outbreak:pistol', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:black_wool'])
shaped('kitchen_knife', ['  I', ' I ', 'S  '], {'I': 'minecraft:iron_nugget', 'S': 'minecraft:stick'})
shaped('claw_hammer', ['II ', ' S ', ' S '], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'})
shaped('pipe_wrench', [' I ', ' II', 'I  '], {'I': 'minecraft:iron_ingot'})
shaped('frying_pan', [' II', ' II', 'S  '], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'})
shaped('hockey_stick', ['  S', ' S ', 'PP '], {'S': 'minecraft:stick', 'P': 'minecraft:oak_planks'})
shaped('golf_club', ['  I', ' S ', 'S  '], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'})
shapeless('fire_axe', ['minecraft:iron_axe', 'minecraft:red_dye'])
shaped('hoodie', ['W W', 'WWW', 'WWW'], {'W': 'minecraft:gray_wool'})
shaped('jeans', ['WWW', 'W W', 'W W'], {'W': 'minecraft:blue_wool'})
shaped('beanie', ['WWW', 'W W'], {'W': 'minecraft:black_wool'})
shaped('sneakers', ['W W', 'L L'], {'W': 'minecraft:white_wool', 'L': 'minecraft:leather'})
shaped('work_boots', ['L L', 'L L'], {'L': 'minecraft:leather'})
print('more assets ok')
