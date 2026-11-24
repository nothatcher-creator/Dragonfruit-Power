"""Wave 8 city blocks: parking stops, road signs, billboards, street decor, elevator pads and smooth slopes.

Usage: python3 make_city_wave8.py <resources dir> [<outbreak zomboid_look pack dir>]
The optional second path writes the Outbreak variants (defaced billboards, graffiti and rust on the signs) into
Outbreak's built-in resource pack, so they replace the clean ones whenever Outbreak is installed.
"""
import os, sys, json, random
from PIL import Image, ImageDraw

RES = sys.argv[1]
A = os.path.join(RES, 'assets/vehicleforge'); D = os.path.join(RES, 'data/vehicleforge')
TEX = os.path.join(A, 'textures/block')
rnd = random.Random(808)


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2)


def cl(v): return max(0, min(255, int(v)))


def noisy(base, var, size=16, a=255):
    im = Image.new('RGBA', (size, size)); px = im.load()
    for y in range(size):
        for x in range(size):
            n = rnd.randint(-var, var); px[x, y] = tuple(cl(v + n) for v in base) + (a,)
    return im


# ---------------------------------------------------------------- tiny 3x5 pixel font
F = {
    'A': ['010', '101', '111', '101', '101'], 'B': ['110', '101', '110', '101', '110'], 'C': ['011', '100', '100', '100', '011'],
    'D': ['110', '101', '101', '101', '110'], 'E': ['111', '100', '110', '100', '111'], 'F': ['111', '100', '110', '100', '100'],
    'G': ['011', '100', '101', '101', '011'], 'H': ['101', '101', '111', '101', '101'], 'I': ['111', '010', '010', '010', '111'],
    'J': ['001', '001', '001', '101', '010'], 'K': ['101', '101', '110', '101', '101'], 'L': ['100', '100', '100', '100', '111'],
    'M': ['101', '111', '111', '101', '101'], 'N': ['110', '101', '101', '101', '101'], 'O': ['010', '101', '101', '101', '010'],
    'P': ['110', '101', '110', '100', '100'], 'Q': ['010', '101', '101', '110', '011'], 'R': ['110', '101', '110', '101', '101'],
    'S': ['011', '100', '010', '001', '110'], 'T': ['111', '010', '010', '010', '010'], 'U': ['101', '101', '101', '101', '111'],
    'V': ['101', '101', '101', '101', '010'], 'W': ['101', '101', '111', '111', '101'], 'X': ['101', '101', '010', '101', '101'],
    'Y': ['101', '101', '010', '010', '010'], 'Z': ['111', '001', '010', '100', '111'], '0': ['111', '101', '101', '101', '111'],
    '1': ['010', '110', '010', '010', '111'], '2': ['110', '001', '010', '100', '111'], '3': ['110', '001', '010', '001', '110'],
    '4': ['101', '101', '111', '001', '001'], '5': ['111', '100', '110', '001', '110'], '6': ['011', '100', '111', '101', '111'],
    '7': ['111', '001', '010', '010', '010'], '8': ['111', '101', '111', '101', '111'], '9': ['111', '101', '111', '001', '110'],
    '!': ['010', '010', '010', '000', '010'], '.': ['000', '000', '000', '000', '010'], '-': ['000', '000', '111', '000', '000'],
    '$': ['011', '110', '010', '011', '110'], "'": ['010', '010', '000', '000', '000'], '?': ['110', '001', '010', '000', '010'],
    ' ': ['000', '000', '000', '000', '000'],
}


def text_w(s, sc=1): return (len(s) * 4 - 1) * sc


def text(im, s, x, y, col, sc=1, jitter=0):
    px = im.load()
    for i, ch in enumerate(s.upper()):
        g = F.get(ch, F[' '])
        oy = rnd.randint(-jitter, jitter) if jitter else 0
        for r, row in enumerate(g):
            for c, bit in enumerate(row):
                if bit == '1':
                    for dy in range(sc):
                        for dx in range(sc):
                            xx, yy = x + (i * 4 + c) * sc + dx, y + oy + r * sc + dy
                            if 0 <= xx < im.width and 0 <= yy < im.height: px[xx, yy] = col


def ctext(im, s, y, col, sc=1, x0=0, w=None, jitter=0):
    w = w or im.width
    text(im, s, x0 + (w - text_w(s, sc)) // 2, y, col, sc, jitter)


def save(n, im, root=TEX):
    os.makedirs(root, exist_ok=True); im.save(os.path.join(root, n + '.png'))


# ---------------------------------------------------------------- model helpers (same conventions as make_city_blocks.py)
def tex(n): return n if ':' in n else 'vehicleforge:block/' + n


def fit(a, b):
    lo, hi = min(a, b), max(a, b)
    if hi - lo > 16: lo, hi = 0, 16
    if lo < 0: hi -= lo; lo = 0
    if hi > 16: lo -= hi - 16; hi = 16
    return (lo, hi) if a <= b else (hi, lo)


def uvfor(face, f, t):
    x1, y1, z1 = f; x2, y2, z2 = t
    u = {'down': [x1, 16 - z2, x2, 16 - z1], 'up': [x1, z1, x2, z2], 'north': [16 - x2, 16 - y2, 16 - x1, 16 - y1],
         'south': [x1, 16 - y2, x2, 16 - y1], 'west': [z1, 16 - y2, z2, 16 - y1], 'east': [16 - z2, 16 - y2, 16 - z1, 16 - y1]}[face]
    a, b = fit(u[0], u[2]); c, d = fit(u[1], u[3])
    return [a, c, b, d]


def el(frm, to, faces, rot=None, uv=None):
    e = {"from": frm, "to": to, "faces": {}}
    for f, t in faces.items():
        e["faces"][f] = {"texture": t, "uv": (uv or {}).get(f) or uvfor(f, frm, to)}
    if rot: e["rotation"] = rot
    return e


def allf(t, skip=()): return {f: t for f in ['north', 'south', 'east', 'west', 'up', 'down'] if f not in skip}
def box(f, t, tx, rot=None): return el(f, t, allf(tx), rot)
def faces(front, other, top=None): return {"north": front, "south": other, "east": other, "west": other, "up": top or other, "down": other}


def model(name, obj): wj(os.path.join(A, 'models/block', name + '.json'), obj)


def facing_state(name):
    v = {}
    for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
        v[f"facing={f}"] = {"model": "vehicleforge:block/" + name, "y": y} if y else {"model": "vehicleforge:block/" + name}
    wj(os.path.join(A, 'blockstates', name + '.json'), {"variants": v})


def item(name, parent=None, display=None):
    o = {"parent": "vehicleforge:block/" + (parent or name)}
    if display: o["display"] = display
    wj(os.path.join(A, 'models/item', name + '.json'), o)


def loot(name):
    wj(os.path.join(D, 'loot_tables/blocks', name + '.json'), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "vehicleforge:" + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


SMALL = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.32, 0.32, 0.32]},
         "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.12, 0.12, 0.12]},
         "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
         "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.15, 0.15, 0.15]},
         "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.2, 0.2, 0.2]}}
TALL = {"gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.42, 0.42, 0.42]}}


def fm(name, textures, elements, display=None):
    textures = dict(textures); textures.setdefault('particle', list(textures.values())[0])
    model(name, {"parent": "minecraft:block/block", "textures": textures, "elements": elements})
    facing_state(name); item(name, display=display); loot(name)


# ================================================================ textures
def concrete(c=(168, 168, 162)): return noisy(c, 7)


ps = concrete(); d = ImageDraw.Draw(ps)
for x0 in (0, 12):
    d.rectangle([x0, 0, x0 + 3, 15], fill=(232, 192, 44, 255))
for x, y in ((6, 7), (9, 7)): d.point((x, y), fill=(90, 70, 50, 255))
save('parking_stop', ps)

metal = noisy((150, 154, 160), 4); save('sign_metal', metal)


def white_sign(border=(20, 20, 20, 255)):
    im = Image.new('RGBA', (16, 16), (240, 240, 236, 255)); d = ImageDraw.Draw(im)
    d.rectangle([2, 0, 13, 15], outline=border); return im


sp = white_sign(); ctext(sp, '50', 1, (20, 20, 20, 255), 2, 2, 12); ctext(sp, 'KMH', 11, (20, 20, 20, 255), 1, 2, 12)
save('speed_limit_sign', sp)
npk = white_sign(); d = ImageDraw.Draw(npk)
text(npk, 'P', 6, 6, (20, 20, 20, 255), 1)
d.ellipse([3, 3, 12, 12], outline=(200, 30, 30, 255)); d.ellipse([4, 4, 11, 11], outline=(200, 30, 30, 255)); d.line([(4, 11), (11, 4)], fill=(200, 30, 30, 255))
d.line([(4, 10), (10, 4)], fill=(200, 30, 30, 255)); text(npk, 'NO', 5, -9, (0, 0, 0, 0))
save('no_parking_sign', npk)
ow = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(ow)
d.rectangle([0, 5, 15, 10], fill=(18, 18, 18, 255)); d.rectangle([1, 6, 14, 9], fill=(240, 240, 236, 255))
d.line([(2, 7), (11, 7)], fill=(18, 18, 18, 255)); d.line([(2, 8), (11, 8)], fill=(18, 18, 18, 255))
d.polygon([(11, 6), (14, 7), (14, 8), (11, 9)], fill=(18, 18, 18, 255))
save('one_way_sign', ow)
yl = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(yl)
d.polygon([(0, 1), (15, 1), (8, 15), (7, 15)], fill=(200, 30, 30, 255)); d.polygon([(3, 3), (12, 3), (8, 11), (7, 11)], fill=(244, 244, 240, 255))
save('yield_sign', yl)
sn = Image.new('RGBA', (32, 32), (24, 110, 60, 255))
ImageDraw.Draw(sn).rectangle([0, 0, 31, 6], outline=(230, 230, 230, 255)); ctext(sn, 'MAIN ST', 1, (240, 240, 240, 255))
ImageDraw.Draw(sn).rectangle([0, 8, 31, 14], outline=(230, 230, 230, 255)); ctext(sn, 'ELM ST', 9, (240, 240, 240, 255))
save('street_name_sign', sn)

# billboards: 64x64 textures, the ad lives in the top 64x32 (the board is 48x24 model pixels -> 2:1)
ADS = {
    'soda': ((210, 30, 40), (250, 250, 250), ['FIZZ', 'COLA'], 'ICE COLD!'),
    'burger': ((245, 190, 40), (120, 40, 20), ['BIG', 'BITE'], 'OPEN 24H'),
    'insurance': ((30, 70, 150), (250, 250, 250), ['KNOX', 'LIFE'], 'WE COVER IT'),
    'radio': ((30, 30, 34), (240, 120, 30), ['ROCK', '101.5'], 'FM RADIO'),
}


def billboard(bg, fg, lines, tag, defaced=None):
    im = Image.new('RGBA', (64, 64), (50, 52, 56, 255)); d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 63, 31], fill=bg + (255,))
    for y in range(32):
        for x in range(64):
            if rnd.random() < 0.08:
                p = im.getpixel((x, y)); im.putpixel((x, y), tuple(cl(v + rnd.randint(-10, 10)) for v in p[:3]) + (255,))
    if lines[0] == 'FIZZ':     # bottle
        d.rectangle([48, 6, 55, 28], fill=(70, 30, 20, 255)); d.rectangle([50, 2, 53, 6], fill=(70, 30, 20, 255)); d.rectangle([48, 14, 55, 18], fill=(250, 250, 250, 255))
    elif lines[0] == 'BIG':    # burger
        d.ellipse([44, 6, 60, 16], fill=(200, 130, 50, 255)); d.rectangle([44, 15, 60, 17], fill=(60, 160, 40, 255))
        d.rectangle([44, 18, 60, 21], fill=(110, 50, 25, 255)); d.rectangle([44, 22, 60, 25], fill=(200, 130, 50, 255))
    elif lines[0] == 'KNOX':   # umbrella
        d.pieslice([42, 4, 62, 24], 180, 360, fill=(250, 250, 250, 255)); d.line([(52, 14), (52, 27)], fill=(250, 250, 250, 255)); d.point((51, 27), fill=(250, 250, 250, 255))
    else:                      # radio waves
        for r in (4, 8, 12): d.arc([52 - r, 16 - r, 52 + r, 16 + r], 300, 60, fill=fg + (255,))
        d.ellipse([50, 14, 54, 18], fill=fg + (255,))
    text(im, lines[0], 4, 4, fg + (255,), 2); text(im, lines[1], 4, 16, fg + (255,), 2)
    text(im, tag, 4, 26, tuple(cl(v * 0.85) for v in fg) + (255,))
    if defaced:
        for _ in range(240):        # torn and faded paper
            x, y = rnd.randint(0, 63), rnd.randint(0, 31)
            if rnd.random() < 0.5:
                im.putpixel((x, y), (cl(180 + rnd.randint(-20, 20)),) * 3 + (255,))
        for _ in range(5):
            x, y = rnd.randint(0, 56), rnd.randint(0, 26)
            d.rectangle([x, y, x + rnd.randint(3, 8), y + rnd.randint(2, 5)], fill=(205, 200, 185, 255))
        for _ in range(30):         # rust streaks running down
            x = rnd.randint(0, 63); y0 = rnd.randint(0, 20)
            d.line([(x, y0), (x, y0 + rnd.randint(3, 11))], fill=(120, 60, 30, 200))
        col, msg = defaced
        wash = (232, 228, 214) if sum(col[:3]) < 300 else (36, 34, 32)
        for i, ln in enumerate(msg):           # a rough painted-over patch behind each spray-painted line
            w = text_w(ln, 2); x0 = (64 - w) // 2 - 2
            for yy in range(2 + i * 11, 15 + i * 11):
                a0 = x0 + rnd.randint(-2, 1); a1 = x0 + w + 3 + rnd.randint(-1, 2)
                d.line([(a0, yy), (a1, yy)], fill=wash + (255,))
        for i, ln in enumerate(msg):
            ctext(im, ln, 3 + i * 11, col, 2, jitter=1)
    return im


for k, (bg, fg, lines, tag) in ADS.items():
    save('billboard_' + k, billboard(bg, fg, lines, tag))
bbm = noisy((60, 62, 66), 3); d = ImageDraw.Draw(bbm)
for y in range(0, 16, 4): d.line([(0, y), (15, y)], fill=(44, 46, 50, 255))
save('billboard_back', bbm)
grate = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(grate)
for i in range(0, 16, 3): d.line([(i, 0), (i, 15)], fill=(46, 48, 52, 255)); d.line([(0, i), (15, i)], fill=(56, 58, 62, 255))
d.rectangle([0, 0, 15, 15], outline=(40, 42, 46, 255)); save('grate', grate)
yb = noisy((236, 190, 40), 6); d = ImageDraw.Draw(yb); d.rectangle([0, 3, 15, 5], fill=(240, 240, 236, 255)); save('bollard', yb)
red = noisy((176, 28, 30), 5); save('booth_red', red)
ph = noisy((176, 28, 30), 5); d = ImageDraw.Draw(ph); d.rectangle([0, 3, 15, 12], fill=(20, 20, 20, 255)); ctext(ph, 'PHONE', 6, (250, 250, 250, 255))
save('booth_sign', ph)
bp = noisy((150, 150, 150), 3); d = ImageDraw.Draw(bp); d.rectangle([5, 2, 10, 13], fill=(30, 30, 32, 255))
for yy in (8, 10, 12):
    for xx in (6, 8): d.point((xx, yy), fill=(200, 200, 200, 255))
d.rectangle([6, 3, 9, 6], fill=(70, 120, 90, 255)); save('booth_phone', bp)
glass = Image.new('RGBA', (16, 16)); px = glass.load()
for y in range(16):
    for x in range(16): px[x, y] = (215, 235, 245, 80) if (x + y) % 16 in (4, 5) else (200, 225, 240, 34)
save('booth_glass', glass)
bag = noisy((30, 30, 34), 6); d = ImageDraw.Draw(bag)
for _ in range(6): x, y = rnd.randint(1, 14), rnd.randint(1, 14); d.line([(x, y), (x + 2, y + 1)], fill=(70, 70, 78, 255))
save('trash_bag', bag)
tie = noisy((200, 170, 40), 8); save('trash_tie', tie)
eb = noisy((70, 110, 70), 4); d = ImageDraw.Draw(eb)
d.rectangle([0, 0, 15, 15], outline=(50, 80, 50, 255)); d.line([(8, 1), (8, 14)], fill=(50, 80, 50, 255))
d.polygon([(3, 4), (6, 4), (4.5, 1)], fill=(240, 200, 30, 255)); d.point((4, 3), fill=(20, 20, 20, 255))
save('electrical_box', eb)
dish = noisy((228, 228, 226), 3); d = ImageDraw.Draw(dish); d.ellipse([0, 0, 15, 15], outline=(190, 190, 190, 255)); d.ellipse([6, 6, 9, 9], fill=(150, 150, 150, 255))
save('dish', dish)
for nm, c1, c2 in (('awning_red', (190, 30, 34), (240, 236, 228)),):
    aw = Image.new('RGBA', (16, 16)); px = aw.load()
    for y in range(16):
        for x in range(16): px[x, y] = tuple(cl(v + rnd.randint(-5, 5)) for v in (c1 if (x // 4) % 2 == 0 else c2)) + (255,)
    save(nm, aw)
lv = noisy((160, 164, 170), 4); d = ImageDraw.Draw(lv)
for y in range(1, 16, 3): d.line([(0, y), (15, y)], fill=(100, 104, 110, 255))
save('vent_louvre', lv)
et = noisy((120, 124, 130), 3); d = ImageDraw.Draw(et)
d.rectangle([0, 0, 15, 15], outline=(240, 190, 40, 255)); d.rectangle([1, 1, 14, 14], outline=(40, 40, 44, 255))
d.polygon([(4, 7), (7, 3), (10, 7)], fill=(240, 190, 40, 255)); d.polygon([(4, 9), (10, 9), (7, 13)], fill=(240, 190, 40, 255))
save('elevator_top', et)
es = noisy((170, 174, 180), 3); d = ImageDraw.Draw(es)
for y in (0, 15): d.line([(0, y), (15, y)], fill=(90, 94, 100, 255))
d.rectangle([6, 5, 9, 10], fill=(60, 60, 64, 255)); d.point((7, 6), fill=(255, 210, 60, 255)); d.point((8, 9), fill=(255, 210, 60, 255))
save('elevator_side', es)
sh = Image.new('RGBA', (16, 16)); px = sh.load()
for y in range(16):
    off = 4 if (y // 4) % 2 else 0
    for x in range(16):
        base = 58 if y % 4 == 3 else 78
        if (x + off) % 8 == 0: base = 50
        px[x, y] = (cl(base + rnd.randint(-6, 6)), cl(base + 2 + rnd.randint(-6, 6)), cl(base + 8 + rnd.randint(-6, 6)), 255)
save('shingle', sh)
rt = Image.new('RGBA', (16, 16)); px = rt.load()
for y in range(16):
    for x in range(16):
        k = (x % 4); shade = [0, 18, 24, 6][k] - (14 if y % 4 == 3 else 0)
        px[x, y] = (cl(170 + shade + rnd.randint(-6, 6)), cl(70 + shade // 2 + rnd.randint(-4, 4)), cl(46 + rnd.randint(-4, 4)), 255)
save('red_tile', rt)
cm = Image.new('RGBA', (16, 16)); px = cm.load()
for y in range(16):
    for x in range(16):
        shade = [0, 16, 24, 16, 0, -14, -20, -14][x % 8]
        px[x, y] = (cl(150 + shade + rnd.randint(-3, 3)), cl(156 + shade + rnd.randint(-3, 3)), cl(162 + shade + rnd.randint(-3, 3)), 255)
save('corrugated_metal', cm)

# ================================================================ models
M = 'lamp_metal'
fm('parking_stop', {"c": tex('parking_stop')}, [box([1, 0, 5], [15, 3, 11], "#c"), box([0.5, 0, 6], [1, 2, 10], "#c"), box([15, 0, 6], [15.5, 2, 10], "#c")])
pole = lambda h=24: box([7.25, 0, 7.25], [8.75, h, 8.75], "#m")
fm('speed_limit_sign', {"s": tex('speed_limit_sign'), "m": tex(M)}, [pole(), el([3, 10, 6.5], [13, 24, 7], {"north": "#s", "south": "#m", "east": "#m", "west": "#m", "up": "#m", "down": "#m"},
                                                                            uv={"north": [2, 0, 14, 16]})], TALL)
fm('no_parking_sign', {"s": tex('no_parking_sign'), "m": tex(M)}, [pole(), el([3, 10, 6.5], [13, 24, 7], {"north": "#s", "south": "#m", "east": "#m", "west": "#m", "up": "#m", "down": "#m"},
                                                                          uv={"north": [2, 0, 14, 16]})], TALL)
fm('one_way_sign', {"s": tex('one_way_sign'), "m": tex(M)}, [pole(20), el([0, 16, 6.5], [16, 21, 7], {"north": "#s", "south": "#s", "up": "#m", "down": "#m"},
                                                                         uv={"north": [0, 5, 16, 10.5], "south": [16, 5, 0, 10.5]})], TALL)
fm('yield_sign', {"s": tex('yield_sign'), "m": tex(M)}, [pole(16), el([2, 12, 6.5], [14, 24, 6.6], {"north": "#s", "south": "#s"}, uv={"north": [0, 0, 16, 16], "south": [16, 0, 0, 16]})], TALL)
fm('street_name_sign', {"s": tex('street_name_sign'), "m": tex(M)}, [pole(28),
    el([-4, 20, 7.5], [20, 23.5, 8.5], {"north": "#s", "south": "#s", "up": "#m", "down": "#m", "east": "#m", "west": "#m"}, uv={"north": [0, 0, 16, 3.5], "south": [0, 0, 16, 3.5]}),
    el([7.5, 24, -4], [8.5, 27.5, 20], {"east": "#s", "west": "#s", "up": "#m", "down": "#m", "north": "#m", "south": "#m"}, uv={"east": [0, 4, 16, 7.5], "west": [0, 4, 16, 7.5]}),
    box([7, 28, 7], [9, 29, 9], "#m")], TALL)

BB_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.24, 0.24, 0.24]},
              "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.1, 0.1, 0.1]},
              "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
              "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.12, 0.12, 0.12]},
              "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.15, 0.15, 0.15]}}
for k in ADS:
    n = 'billboard_' + k
    fm(n, {"a": tex(n), "b": tex('billboard_back'), "m": tex(M), "g": tex('grate')}, [
        el([-16, 5, 7], [32, 29, 8.5], {"north": "#a", "south": "#b", "up": "#b", "down": "#b", "east": "#b", "west": "#b"}, uv={"north": [0, 0, 16, 8]}),
        box([-16.5, 4, 6.5], [32.5, 5, 9], "#m"), box([-16.5, 29, 6.5], [32.5, 30, 9], "#m"),
        box([-16.5, 5, 6.5], [-16, 29, 9], "#m"), box([32, 5, 6.5], [32.5, 29, 9], "#m"),
        el([-16, 2.5, 1], [32, 3, 7], {"up": "#g", "down": "#g"}),                                  # catwalk
        box([-16, 3, 1], [-15.5, 6, 1.5], "#m"), box([31.5, 3, 1], [32, 6, 1.5], "#m"), box([-16, 5.5, 1], [32, 6, 1.5], "#m"),
        box([-8, 29.5, 2], [-6, 30.5, 7], "#m"), box([7, 29.5, 2], [9, 30.5, 7], "#m"), box([22, 29.5, 2], [24, 30.5, 7], "#m"),  # light arms
        box([-9, 28, 1.5], [-5, 29.5, 3.5], "#m"), box([6, 28, 1.5], [10, 29.5, 3.5], "#m"), box([21, 28, 1.5], [25, 29.5, 3.5], "#m"),
        box([4, 0, 8.5], [6, 5, 10.5], "#m"), box([10, 0, 8.5], [12, 5, 10.5], "#m")], BB_DISPLAY)

fm('bollard', {"b": tex('bollard'), "m": tex(M)}, [box([5.5, 0, 5.5], [10.5, 13, 10.5], "#b"), box([5, 13, 5], [11, 14, 11], "#m"), box([6, 14, 6], [10, 14.5, 10], "#m")])
booth = [box([1, 0, 1], [15, 1, 15], "#r"), box([0.5, 30, 0.5], [15.5, 32, 15.5], "#r"),
         el([1, 27, 0.4], [15, 30, 1], {"north": "#s"}, uv={"north": [0, 2, 16, 14]})]
for x0, z0 in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)): booth.append(box([x0, 1, z0], [x0 + 1.5, 30, z0 + 1.5], "#r"))
booth += [el([1.5, 1, 14], [14.5, 30, 15], {"north": "#p", "south": "#r", "up": "#r"}, uv={"north": [0, 0, 16, 16]}),
          el([1, 4, 2.5], [1.5, 27, 13.5], {"east": "#g", "west": "#g"}), el([14.5, 4, 2.5], [15, 27, 13.5], {"east": "#g", "west": "#g"}),
          box([1, 1, 2.5], [1.5, 4, 13.5], "#r"), box([14.5, 1, 2.5], [15, 4, 13.5], "#r"),
          box([5, 14, 12], [11, 15, 14], "#r")]
fm('phone_booth', {"r": tex('booth_red'), "s": tex('booth_sign'), "p": tex('booth_phone'), "g": tex('booth_glass')}, booth, TALL)
fm('trash_bags', {"b": tex('trash_bag'), "t": tex('trash_tie')}, [box([1, 0, 2], [8, 7, 9], "#b"), box([3.5, 7, 4.5], [5.5, 8.5, 6.5], "#t"),
                                                                  box([8, 0, 6], [15, 8, 14], "#b"), box([10.5, 8, 9], [12.5, 9.5, 11], "#t"),
                                                                  box([2, 0, 10], [8, 5, 15], "#b"), box([4, 5, 12], [5.5, 6, 13.5], "#t")])
rack = [box([0, 0, 7], [16, 1, 9], "#m")]
for x in (1.5, 5.5, 9.5, 13.5):
    rack += [box([x, 1, 4], [x + 1, 10, 5], "#m"), box([x, 1, 11], [x + 1, 10, 12], "#m"), box([x, 9, 5], [x + 1, 10, 11], "#m")]
fm('bike_rack', {"m": tex(M)}, rack)
fm('electrical_box', {"e": tex('electrical_box'), "m": tex(M)}, [el([2, 1, 4], [14, 15, 12], faces("#e", "#m")), box([1.5, 0, 3.5], [14.5, 1, 12.5], "#m"),
                                                                 box([1.5, 15, 3.5], [14.5, 16, 12.5], "#m")])
fm('satellite_dish', {"d": tex('dish'), "m": tex(M)}, [box([6, 0, 9], [10, 1, 13], "#m"), box([7.5, 1, 10.5], [8.5, 8, 11.5], "#m"),
                                                       el([2, 3, 8], [14, 15, 9], {"north": "#d", "south": "#d", "up": "#m", "down": "#m", "east": "#m", "west": "#m"},
                                                          {"origin": [8, 9, 8.5], "axis": "x", "angle": -22.5}, uv={"north": [0, 0, 16, 16], "south": [16, 0, 0, 16]}),
                                                       box([7.5, 10, 2], [8.5, 11, 8], "#m"), box([7, 9.5, 1.5], [9, 11.5, 3], "#m")])
fm('awning', {"a": tex('awning_red'), "m": tex(M)}, [
    el([0, 12, -2], [16, 12.8, 18], {"up": "#a", "down": "#a"}, {"origin": [8, 14, 16], "axis": "x", "angle": -22.5, "rescale": False}),
    el([0, 8.5, -1.8], [16, 11.5, -1.3], {"north": "#a", "south": "#a"}, uv={"north": [0, 0, 16, 3], "south": [0, 0, 16, 3]}),
    box([0, 11, -1.8], [0.5, 16, 16], "#m"), box([15.5, 11, -1.8], [16, 16, 16], "#m")])
fe = [el([0, 0, 0], [16, 1, 16], {"up": "#g", "down": "#g", "north": "#m", "east": "#m", "west": "#m"})]
fe += [box([0, 1, 0], [1, 16, 1], "#m"), box([15, 1, 0], [16, 16, 1], "#m"), box([0, 15, 0], [16, 16, 1], "#m"), box([0, 8, 0], [16, 8.5, 1], "#m"),
       box([0, 15, 1], [1, 16, 16], "#m"), box([15, 15, 1], [16, 16, 16], "#m")]
for x in (4, 8, 12): fe.append(box([x, 1, 0.2], [x + 0.5, 15, 0.7], "#m"))
for x in (2, 4.5):   # ladder going down at the left
    pass
fe += [box([2, -16, 0.5], [2.5, 0, 1], "#m"), box([6, -16, 0.5], [6.5, 0, 1], "#m")] + [box([2.5, y, 0.5], [6, y + 0.5, 1], "#m") for y in range(-15, 0, 3)]
fm('fire_escape', {"g": tex('grate'), "m": tex(M)}, fe)
fm('roof_vent', {"v": tex('vent_louvre'), "m": tex(M)}, [box([5, 0, 5], [11, 4, 11], "#m"), el([4, 4, 4], [12, 10, 12], faces("#v", "#v", "#m")),
                                                          box([4.5, 10, 4.5], [11.5, 11, 11.5], "#m"), box([7, 11, 7], [9, 12, 9], "#m")])
model('elevator', {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex('elevator_top'), "bottom": tex('elevator_side'), "side": tex('elevator_side')}})
wj(os.path.join(A, 'blockstates/elevator.json'), {"variants": {"": {"model": "vehicleforge:block/elevator"}}})
item('elevator'); loot('elevator')

SLOPES = {'slope_shingle': ('shingle', 'minecraft:block/dark_oak_planks'), 'slope_red_tile': ('red_tile', 'minecraft:block/bricks'),
          'slope_wood': ('minecraft:block/oak_planks', 'minecraft:block/oak_planks'), 'slope_spruce': ('minecraft:block/spruce_planks', 'minecraft:block/spruce_planks'),
          'slope_stone': ('minecraft:block/smooth_stone', 'minecraft:block/stone'), 'slope_metal': ('corrugated_metal', 'minecraft:block/iron_block')}
for n, (top, side) in SLOPES.items():
    # the roof surface: a plate along the diagonal, tilted 45 degrees so the north edge is up (like a rising rail)
    els = [el([0, 7.5, 0], [16, 8.5, 16], {"up": "#t", "down": "#s"}, {"origin": [8, 8, 8], "axis": "x", "angle": 45, "rescale": True})]
    for z0 in range(0, 16, 2):      # stepped fill underneath so the gable ends are solid
        h = 14 - z0
        if h > 0: els.append(el([0, 0, z0], [16, h, z0 + 2], {"east": "#s", "west": "#s", "north": "#s", "down": "#s"}))
    els.append(el([0, 0, 0], [16, 16, 0.01], {"north": "#s"}))
    textures = {"t": tex(top), "s": tex(side), "particle": tex(top)}
    model(n, {"parent": "minecraft:block/block", "textures": textures, "elements": els})
    facing_state(n); item(n); loot(n)

# manhole cover that lifts off: open = just the rim, so you can climb down into the sewer
model('manhole_open', {"parent": "minecraft:block/block", "textures": {"a": tex('asphalt'), "particle": tex('asphalt')}, "elements": [
    box([0, 0, 0], [16, 16, 2], "#a"), box([0, 0, 14], [16, 16, 16], "#a"), box([0, 0, 2], [2, 16, 14], "#a"), box([14, 0, 2], [16, 16, 14], "#a")]})
wj(os.path.join(A, 'blockstates/manhole_cover.json'), {"variants": {"open=false": {"model": "vehicleforge:block/manhole_cover"},
                                                                    "open=true": {"model": "vehicleforge:block/manhole_open"}}})

# ================================================================ tags, recipes, lang
pk = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/pickaxe.json'); tg = json.load(open(pk))
PICK = ['parking_stop', 'speed_limit_sign', 'one_way_sign', 'no_parking_sign', 'yield_sign', 'street_name_sign'] + ['billboard_' + k for k in ADS] + \
       ['bollard', 'phone_booth', 'bike_rack', 'electrical_box', 'satellite_dish', 'fire_escape', 'roof_vent', 'elevator', 'slope_shingle', 'slope_red_tile', 'slope_stone', 'slope_metal']
for n in PICK:
    if 'vehicleforge:' + n not in tg['values']: tg['values'].append('vehicleforge:' + n)
json.dump(tg, open(pk, 'w'), indent=2)
ax = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/axe.json'); ta = json.load(open(ax))
for n in ('slope_wood', 'slope_spruce', 'awning', 'trash_bags'):
    if 'vehicleforge:' + n not in ta['values']: ta['values'].append('vehicleforge:' + n)
json.dump(ta, open(ax, 'w'), indent=2)


def shaped(name, pattern, key, count=1):
    wj(os.path.join(D, 'recipes', name + '.json'), {"type": "minecraft:crafting_shaped", "pattern": pattern,
                                                     "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": "vehicleforge:" + name, "count": count}})


I, W, Y = 'minecraft:iron_ingot', 'minecraft:white_dye', 'minecraft:yellow_dye'
shaped('parking_stop', ['SYS'], {'S': 'minecraft:smooth_stone_slab', 'Y': Y}, 2)
shaped('speed_limit_sign', ['W', 'B', 'I'], {'W': W, 'B': 'minecraft:black_dye', 'I': I})
shaped('one_way_sign', ['BW', 'I '], {'W': W, 'B': 'minecraft:black_dye', 'I': I})
shaped('no_parking_sign', ['WR', 'I '], {'W': W, 'R': 'minecraft:red_dye', 'I': I})
shaped('yield_sign', ['RW', 'I '], {'W': W, 'R': 'minecraft:red_dye', 'I': I})
shaped('street_name_sign', ['GW', 'I '], {'W': W, 'G': 'minecraft:green_dye', 'I': I})
for k, dye in zip(ADS, ('minecraft:red_dye', 'minecraft:orange_dye', 'minecraft:blue_dye', 'minecraft:black_dye')):
    shaped('billboard_' + k, ['PPP', 'PDP', ' I '], {'P': 'minecraft:paper', 'D': dye, 'I': I})
shaped('bollard', ['Y', 'I', 'I'], {'Y': Y, 'I': I}, 2)
shaped('phone_booth', ['RRR', 'GBG', 'RRR'], {'R': 'minecraft:red_concrete', 'G': 'minecraft:glass_pane', 'B': 'minecraft:stone_button'})
shaped('trash_bags', ['KK'], {'K': 'minecraft:black_wool'}, 2)
shaped('bike_rack', ['I I', 'III'], {'I': 'minecraft:iron_bars'})
shaped('electrical_box', ['III', 'IRI', 'III'], {'I': I, 'R': 'minecraft:redstone'})
shaped('satellite_dish', [' Q', 'I '], {'Q': 'minecraft:quartz', 'I': I})
shaped('awning', ['RWR'], {'R': 'minecraft:red_wool', 'W': 'minecraft:white_wool'}, 3)
shaped('fire_escape', ['B B', 'III'], {'B': 'minecraft:iron_bars', 'I': I}, 4)
shaped('roof_vent', [' I ', 'III'], {'I': I}, 2)
shaped('elevator', ['IEI', 'IRI', 'III'], {'I': I, 'E': 'minecraft:ender_pearl', 'R': 'minecraft:redstone_block'}, 2)
for n, mat in (('slope_shingle', 'minecraft:deepslate_tiles'), ('slope_red_tile', 'minecraft:terracotta'), ('slope_wood', 'minecraft:oak_planks'),
               ('slope_spruce', 'minecraft:spruce_planks'), ('slope_stone', 'minecraft:smooth_stone'), ('slope_metal', I)):
    shaped(n, ['  M', ' MM', 'MMM'], {'M': mat}, 8)

LP = os.path.join(A, 'lang/en_us.json'); lang = json.load(open(LP))
names = {'parking_stop': 'Parking Stop', 'speed_limit_sign': 'Speed Limit Sign', 'one_way_sign': 'One Way Sign', 'no_parking_sign': 'No Parking Sign',
         'yield_sign': 'Yield Sign', 'street_name_sign': 'Street Name Sign', 'billboard_soda': 'Billboard (Fizz Cola)', 'billboard_burger': 'Billboard (Big Bite)',
         'billboard_insurance': 'Billboard (Knox Life)', 'billboard_radio': 'Billboard (Rock 101.5)', 'bollard': 'Bollard', 'phone_booth': 'Phone Booth',
         'trash_bags': 'Trash Bags', 'bike_rack': 'Bike Rack', 'electrical_box': 'Electrical Box', 'satellite_dish': 'Satellite Dish', 'awning': 'Awning',
         'fire_escape': 'Fire Escape', 'roof_vent': 'Roof Vent', 'elevator': 'Elevator Pad', 'slope_shingle': 'Shingle Roof Slope',
         'slope_red_tile': 'Red Tile Roof Slope', 'slope_wood': 'Oak Slope', 'slope_spruce': 'Spruce Slope', 'slope_stone': 'Stone Slope', 'slope_metal': 'Metal Roof Slope'}
for k, v in names.items(): lang['block.vehicleforge.' + k] = v
lang['tooltip.vehicleforge.elevator'] = 'Stand on it: jump to go up a floor, sneak to go down'
json.dump(lang, open(LP, 'w'), indent=2)
print('wave 8 ok', len(names))

# ================================================================ Outbreak variants
if len(sys.argv) > 2:
    OT = os.path.join(sys.argv[2], 'assets/vehicleforge/textures/block')
    DEF = {'soda': ((20, 20, 20, 255), ['THE END', 'IS NEAR']), 'burger': ((170, 20, 20, 255), ['HELP', 'US']),
           'insurance': ((20, 20, 20, 255), ['EVACUATE', 'NORTH']), 'radio': ((230, 230, 230, 255), ['STAY', 'INSIDE'])}
    for k, (bg, fg, lines, tag) in ADS.items():
        save('billboard_' + k, billboard(bg, fg, lines, tag, DEF[k]), OT)

    def grime(src, spray=None, scol=(170, 20, 20, 255)):
        im = Image.open(os.path.join(TEX, src + '.png')).convert('RGBA'); px = im.load()
        for _ in range(int(im.width * im.height * 0.18)):
            x, y = rnd.randrange(im.width), rnd.randrange(im.height)
            if px[x, y][3] > 0:
                r, g, b, a = px[x, y]; px[x, y] = (cl(r * 0.7 + 40), cl(g * 0.6 + 20), cl(b * 0.5 + 8), a)
        if spray:
            d = ImageDraw.Draw(im)
            for i in range(len(spray[0]) - 1):
                (x0, y0), (x1, y1) = spray[0][i], spray[0][i + 1]
                d.line([(x0, y0), (x1, y1)], fill=scol)
        save(src, im, OT)

    grime('speed_limit_sign', ([(3, 13), (6, 10), (9, 13), (12, 10)],))
    grime('no_parking_sign', ([(3, 3), (12, 12)],), (20, 20, 20, 255))
    grime('one_way_sign')
    grime('yield_sign', ([(5, 4), (10, 4)],), (20, 20, 20, 255))
    grime('stop_sign') if os.path.exists(os.path.join(TEX, 'stop_sign.png')) else None
    grime('street_name_sign')
    grime('booth_red'); grime('booth_glass'); grime('electrical_box', ([(3, 9), (5, 13), (7, 9), (9, 13)],), (30, 30, 200, 255))
    grime('bollard'); grime('parking_stop'); grime('awning_red')
    print('outbreak variants ok')
