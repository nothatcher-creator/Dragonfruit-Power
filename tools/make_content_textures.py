"""Item/block textures for ship, katanas, seastone, cuffs, plus 64x64 fruit body-effect overlays."""
import math, os, random, sys
from PIL import Image

ROOT = sys.argv[1]  # assets/dragonfruit
ITEM = os.path.join(ROOT, 'textures/item'); BLOCK = os.path.join(ROOT, 'textures/block'); AURA = os.path.join(ROOT, 'textures/entity/aura')
for d in (ITEM, BLOCK, AURA): os.makedirs(d, exist_ok=True)
def hexc(h, a=255): h=h.lstrip('#'); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), a)

# ---------- katanas (diagonal blade, bottom-left handle to top-right tip)
def katana(name, blade, edge, guard, wrap, wrap2):
    img = Image.new('RGBA', (16,16), (0,0,0,0)); px = img.load()
    for i in range(5, 15):              # blade
        x, y = i, 15 - i
        px[x, y] = hexc(blade)
        if y + 1 < 16: px[x, y+1] = hexc(edge)
    px[15, 0] = hexc(edge); px[14,0] = hexc(blade)
    for gx, gy in [(3,10),(4,11),(5,12),(4,9),(6,11),(5,10)]:  # guard
        px[gx, gy] = hexc(guard)
    for i in range(0, 5):               # wrapped handle
        x, y = i, 15 - i + 0
        if 0 <= y < 16 and x < 4: px[x, y] = hexc(wrap if i % 2 == 0 else wrap2)
    px[0,15] = hexc(guard)
    img.save(os.path.join(ITEM, name + '.png'))
katana('white_katana', '#e8eef5', '#ffffff', '#d4af37', '#f5f5f5', '#c9c9d6')
katana('crimson_katana', '#c9ced8', '#ffffff', '#3a3a3a', '#b0102a', '#6e0a1a')
katana('black_katana', '#2a2833', '#8a4cff', '#c9a227', '#1a1a1a', '#5a2aa0')

# ---------- seastone block: pale sea-green stone with darker veins
random.seed(7)
img = Image.new('RGBA', (16,16)); px = img.load()
for y in range(16):
    for x in range(16):
        n = random.random()
        base = (88, 156, 150) if n > 0.2 else (72, 132, 128)
        if n > 0.93: base = (150, 214, 205)
        px[x,y] = base + (255,)
for x,y in [(2,3),(3,4),(4,4),(5,5),(9,10),(10,10),(11,11),(12,12),(7,1),(7,2),(13,3),(14,4),(3,12),(4,13)]:
    px[x,y] = (46, 92, 92, 255)
img.save(os.path.join(BLOCK, 'seastone.png'))

# ---------- cuffs
img = Image.new('RGBA', (16,16), (0,0,0,0)); px = img.load()
def ring(cx, cy, r, col):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if r - 0.9 <= d <= r + 0.5: px[x,y] = col
ring(4.5, 9.5, 3.2, (88,156,150,255)); ring(11.5, 9.5, 3.2, (88,156,150,255))
for x in range(7, 10): px[x, 7] = (120,120,128,255)
px[6,7] = px[10,7] = (90,90,98,255)
for x,y in [(3,7),(10,7),(4,12),(11,12)]: px[x,y] = (160,220,210,255)
img.save(os.path.join(ITEM, 'seastone_cuffs.png'))

# ---------- ship item: little ship silhouette
img = Image.new('RGBA', (16,16), (0,0,0,0)); px = img.load()
hull = (110,70,40,255); dark = (70,45,25,255); sail = (240,240,240,255); pink=(214,60,170,255); green=(60,150,60,255)
for x in range(2, 15): px[x, 12] = hull
for x in range(3, 14): px[x, 13] = dark
for x in range(1, 15): px[x, 11] = hull
px[15,10] = green; px[14,10] = green; px[15,9] = green  # dragon figurehead
for y in range(2, 11): px[8, y] = dark
for y in range(3, 9):
    for x in range(5, 12):
        px[x, y] = pink if y in (5,6) and x % 2 == 0 else sail
px[9,1] = (20,20,20,255); px[10,1] = pink
img.save(os.path.join(ITEM, 'pirate_ship.png'))

# ---------- 64x64 fruit body overlays (additive: black = invisible), 4 frames each
def save_aura(name, painter):
    for f in range(4):
        img = Image.new('RGBA', (64,64), (0,0,0,255)); px = img.load()
        rnd = random.Random(hash(name) % 1000 + f * 31)
        painter(px, f, rnd)
        img.save(os.path.join(AURA, f'{name}_{f}.png'))

def flame(px, f, rnd):
    for x in range(64):
        h = int(3 + 5 * abs(math.sin(x * 0.7 + f * 1.3)) + rnd.random() * 3)
        for band in range(0, 64, 16):
            for k in range(h):
                y = band + 15 - k
                t = k / max(1, h)
                col = (255, int(200 - 150 * t), int(60 - 60 * t), 255) if t < 0.95 else (255, 60, 0, 255)
                px[x, y] = tuple(int(c * (0.9 - 0.6 * t)) for c in col[:3]) + (255,)
def ice(px, f, rnd):
    for _ in range(26):
        x, y = rnd.randrange(64), rnd.randrange(64)
        for k in range(rnd.randrange(3, 8)):
            if 0 <= x < 64 and 0 <= y < 64: px[x, y] = (150, 220, 255, 255) if k % 2 else (230, 250, 255, 255)
            x += rnd.choice([-1, 1]); y += rnd.choice([-1, 0, 1])
    for _ in range(6 + f * 2):
        px[rnd.randrange(64), rnd.randrange(64)] = (255, 255, 255, 255)
def light(px, f, rnd):
    g = 0.25 + 0.15 * math.sin(f * math.pi / 2)
    for y in range(64):
        for x in range(64):
            v = int(255 * g * (0.5 + 0.5 * math.sin((x + y) * 0.5 + f)))
            px[x, y] = (v, int(v * 0.85), int(v * 0.35), 255)
    for _ in range(20):
        px[rnd.randrange(64), rnd.randrange(64)] = (255, 250, 200, 255)
def quake(px, f, rnd):
    for _ in range(8):
        x, y = rnd.randrange(64), rnd.randrange(64)
        for k in range(10):
            if 0 <= x < 64 and 0 <= y < 64: px[x, y] = (200, 200, 190, 255)
            x += rnd.choice([-1, 0, 1]); y += rnd.choice([-1, 1])
def thunder(px, f, rnd):
    for _ in range(9):
        x, y = rnd.randrange(64), rnd.randrange(64)
        for k in range(12):
            if 0 <= x < 64 and 0 <= y < 64:
                px[x, y] = (140, 210, 255, 255) if k % 3 else (255, 255, 255, 255)
            x += rnd.choice([-1, 1]); y += rnd.choice([-1, 0, 1, 1])
def magma(px, f, rnd):
    rnd2 = random.Random(11)
    glow = [0.75, 1.0, 0.85, 0.6][f]
    for _ in range(14):
        x, y = rnd2.randrange(64), rnd2.randrange(64)
        for k in range(14):
            if 0 <= x < 64 and 0 <= y < 64:
                px[x, y] = (int(255 * glow), int(110 * glow), int(20 * glow), 255)
            x += rnd2.choice([-1, 0, 1]); y += rnd2.choice([-1, 0, 1])
def gravity(px, f, rnd):
    for y in range(64):
        for x in range(64):
            cx, cy = (x % 16) - 8, (y % 16) - 8
            a = math.atan2(cy, cx); d = math.hypot(cx, cy)
            v = math.sin(a * 2 + d * 0.9 - f * 1.4)
            if v > 0.8: px[x, y] = (150, 100, 255, 255)
            elif v > 0.55: px[x, y] = (70, 40, 140, 255)
for n, fn in [('flame', flame), ('ice', ice), ('light', light), ('quake', quake), ('thunder', thunder), ('magma', magma), ('gravity', gravity)]:
    save_aura(n, fn)

# Willpower Armor: dark glossy arms only (translucent layer: transparent elsewhere)
img = Image.new('RGBA', (64,64), (0,0,0,0)); px = img.load()
regions = [(40,16,56,32), (32,48,48,64)]  # right arm, left arm (base layer)
for (x0,y0,x1,y1) in regions:
    for y in range(y0, y1):
        for x in range(x0, x1):
            shine = 1 if (x + y) % 7 == 0 else 0
            px[x, y] = (60 + 70*shine, 58 + 70*shine, 80 + 80*shine, 235)
img.save(os.path.join(AURA, 'armor.png'))
print('ok')
