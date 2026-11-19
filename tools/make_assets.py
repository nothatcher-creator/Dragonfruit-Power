"""Draws Outbreak's pixel-art textures and writes item models, sounds.json, lang, recipes and data files."""
import json, os, sys
from PIL import Image
R = sys.argv[1]  # src/main/resources
A = os.path.join(R, 'assets/outbreak'); D = os.path.join(R, 'data')
def mk(p): os.makedirs(p, exist_ok=True); return p
def hexc(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)
def shade(c, f): return tuple(max(0, min(255, int(v*f))) for v in c[:3]) + (c[3],)

class C:
    def __init__(s, w=16, h=16): s.im = Image.new('RGBA', (w, h), (0, 0, 0, 0)); s.w, s.h = w, h
    def px(s, x, y, c):
        if 0 <= x < s.w and 0 <= y < s.h: s.im.putpixel((x, y), c)
    def rect(s, x0, y0, x1, y1, c, top=None, bot=None):
        for y in range(y0, y1+1):
            for x in range(x0, x1+1):
                cc = c
                if top and y == y0: cc = top
                elif bot and y == y1: cc = bot
                s.px(x, y, cc)
    def line(s, x0, y0, x1, y1, c, th=1):
        dx, dy = abs(x1-x0), -abs(y1-y0); sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1; e = dx+dy
        while True:
            for o in range(th): s.px(x0+o, y0, c)
            if x0 == x1 and y0 == y1: break
            e2 = 2*e
            if e2 >= dy: e += dy; x0 += sx
            if e2 <= dx: e += dx; y0 += sy
    def outline(s, c=(20, 20, 22, 255)):
        src = s.im.copy(); W, H = s.w, s.h
        for y in range(H):
            for x in range(W):
                if src.getpixel((x, y))[3] == 0:
                    for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                        xx, yy = x+dx, y+dy
                        if 0 <= xx < W and 0 <= yy < H and src.getpixel((xx, yy))[3] > 0:
                            s.im.putpixel((x, y), c); break
    def save(s, path): mk(os.path.dirname(path)); s.im.save(path)

ITEM = os.path.join(A, 'textures/item')
BLK = hexc('2a2b2f'); BLK2 = hexc('3d3f45'); BLK3 = hexc('55585f'); STEEL = hexc('8a8f98'); STEEL2 = hexc('b9bec6')
WOOD = hexc('7a4a24'); WOOD2 = hexc('9a6433'); WOOD3 = hexc('5c3519')

# ---------- guns (32x32, muzzle to the right) ----------
def pistol():
    c = C(32, 32)
    c.rect(8, 11, 26, 15, BLK2, top=BLK3, bot=BLK)          # slide
    for x in range(18, 24, 2): c.rect(x, 12, x, 14, BLK)     # serrations
    c.rect(26, 12, 27, 14, BLK)                               # muzzle
    c.px(25, 10, BLK3); c.px(9, 10, BLK3)                     # sights
    c.rect(9, 16, 21, 17, BLK)                                # frame
    for i in range(8): c.rect(9+i//3, 18+i, 14+i//3, 18+i, BLK2 if i % 2 else BLK3)  # grip, raked back
    c.rect(15, 18, 19, 18, BLK); c.px(19, 19, BLK); c.px(19, 20, BLK); c.rect(15, 21, 18, 21, BLK)  # trigger guard
    c.px(16, 19, STEEL)                                       # trigger
    c.outline(); return c
def smg():
    c = C(32, 32)
    c.rect(7, 12, 24, 17, BLK2, top=BLK3, bot=BLK)
    c.rect(24, 13, 29, 15, BLK); c.rect(29, 13, 30, 15, BLK3)  # barrel + compensator
    c.rect(10, 10, 11, 11, BLK3); c.rect(21, 10, 22, 11, BLK3)  # sights
    c.rect(15, 18, 18, 27, BLK, top=BLK2)                     # magazine
    c.rect(9, 18, 12, 23, BLK2)                               # grip
    c.line(1, 13, 7, 13, STEEL); c.line(1, 17, 7, 16, STEEL); c.line(1, 13, 1, 17, STEEL)  # wire stock
    c.outline(); return c
def shotgun():
    c = C(32, 32)
    for i in range(7): c.rect(1+i, 15-i//3, 1+i, 20-i//2, WOOD2 if i % 2 else WOOD)   # stock
    c.rect(1, 17, 7, 17, WOOD3)
    c.rect(8, 13, 15, 17, BLK2, top=BLK3, bot=BLK)          # receiver
    c.rect(15, 13, 31, 14, STEEL, top=STEEL2)                # barrel
    c.rect(15, 15, 29, 15, BLK)                              # mag tube
    c.rect(17, 16, 24, 17, WOOD2, top=WOOD)                  # pump
    for x in (18, 20, 22): c.px(x, 17, WOOD3)
    c.rect(10, 18, 13, 19, BLK); c.px(11, 18, STEEL)        # trigger guard
    c.outline(); return c
def hunting_rifle():
    c = C(32, 32)
    for i in range(9): c.rect(1+i, 15-i//3, 1+i, 21-i//2, WOOD2 if i % 2 else WOOD)
    c.rect(10, 14, 22, 17, WOOD2, top=WOOD, bot=WOOD3)       # forestock
    c.rect(10, 13, 16, 14, BLK2)                             # action
    c.px(13, 12, STEEL2); c.px(14, 11, STEEL2)               # bolt handle
    c.rect(16, 13, 31, 14, BLK, top=BLK3)                    # barrel
    c.rect(11, 9, 21, 11, BLK, top=BLK3)                     # scope
    c.rect(9, 8, 11, 12, BLK2); c.rect(21, 8, 23, 12, BLK2)  # scope bells
    c.px(23, 10, hexc('6fb4e0')); c.px(9, 10, hexc('6fb4e0'))
    c.rect(13, 12, 13, 12, BLK); c.rect(19, 12, 19, 12, BLK) # mounts
    c.rect(11, 18, 13, 19, BLK)
    c.outline(); return c
def assault_rifle():
    c = C(32, 32)
    c.rect(1, 13, 7, 18, BLK2, top=BLK3, bot=BLK)            # stock
    c.rect(3, 15, 6, 16, BLK)
    c.rect(7, 14, 9, 15, BLK)                                # buffer tube
    c.rect(9, 12, 19, 17, BLK2, top=BLK3, bot=BLK)           # receiver
    c.rect(19, 12, 27, 16, BLK3, top=STEEL, bot=BLK)         # handguard
    for x in range(21, 27, 2): c.px(x, 14, BLK)
    c.rect(27, 13, 31, 14, BLK)                              # barrel
    c.rect(28, 11, 28, 12, BLK)                              # front sight
    c.rect(11, 10, 16, 11, BLK)                              # rail / rear sight
    for i in range(9): c.rect(13+i//3, 18+i, 16+i//3, 18+i, BLK if i % 2 else BLK2)  # curved mag
    c.rect(9, 18, 11, 23, BLK2)                              # grip
    c.outline(); return c
for name, fn in (('pistol', pistol), ('smg', smg), ('shotgun', shotgun), ('hunting_rifle', hunting_rifle), ('assault_rifle', assault_rifle)):
    fn().save(f'{ITEM}/{name}.png')

# ---------- ammo ----------
def rounds(case, tip, n, long=False):
    c = C()
    for i in range(n):
        x = 3 + i*4; top = 4 if long else 6
        c.rect(x, top+3, x+2, 13, case, top=shade(case, 1.2), bot=shade(case, 0.7))
        c.rect(x, top, x+2, top+2, tip); c.px(x+1, top-1, tip)
        c.px(x, top+3, shade(case, 1.3))
    c.outline(); return c
rounds(hexc('c9a64a'), hexc('9a6b3a'), 3).save(f'{ITEM}/ammo_9mm.png')
rounds(hexc('b8b0a0'), hexc('a03030'), 3, True).save(f'{ITEM}/ammo_rifle.png')
def shells():
    c = C()
    for i in range(2):
        x = 3 + i*6
        c.rect(x, 3, x+3, 10, hexc('b02a2a'), top=hexc('d04040'))
        c.rect(x, 11, x+3, 13, hexc('d4af37'), bot=hexc('9a7a20'))
    c.outline(); return c
shells().save(f'{ITEM}/ammo_shells.png')

# ---------- melee (diagonal, handle bottom-left) ----------
def diag(c, x0, y0, x1, y1, col, th=2):
    c.line(x0, y0, x1, y1, col, th)
def bat(spiked=False):
    c = C()
    diag(c, 2, 13, 6, 9, hexc('3a2a1a'), 2)      # tape grip
    diag(c, 6, 9, 9, 6, WOOD2, 2)
    for i in range(5): c.rect(9+i, 3+(4-i)-1, 10+i, 5+(4-i), hexc('b07a44'))
    c.rect(13, 1, 14, 3, hexc('c48a50'))
    if spiked:
        for (x, y) in ((10, 3), (12, 2), (14, 5), (11, 6), (13, 7), (9, 5)): c.px(x, y, STEEL2)
    c.outline(); return c
def crowbar():
    c = C()
    diag(c, 2, 13, 12, 3, hexc('c0282a'), 2)
    c.rect(12, 1, 14, 2, hexc('c0282a')); c.px(14, 3, hexc('c0282a')); c.px(1, 14, hexc('8a1a1a'))
    c.outline(); return c
def machete():
    c = C()
    diag(c, 2, 13, 5, 10, hexc('222222'), 2)
    c.rect(5, 9, 7, 11, STEEL)
    for i in range(8): c.rect(7+i, 8-i, 8+i, 9-i, STEEL2 if i < 6 else STEEL)
    for i in range(7): c.px(8+i, 10-i, STEEL)
    c.outline(); return c
def sledge():
    c = C()
    diag(c, 2, 13, 10, 5, WOOD2, 2)
    for i in range(5): c.rect(8+i, 1+i, 9+i, 4+i, BLK3 if i % 2 else STEEL)
    c.rect(9, 1, 13, 8, BLK2); c.rect(9, 1, 13, 1, STEEL); c.rect(11, 2, 14, 7, BLK3)
    c.outline(); return c
bat().save(f'{ITEM}/baseball_bat.png'); bat(True).save(f'{ITEM}/spiked_bat.png')
crowbar().save(f'{ITEM}/crowbar.png'); machete().save(f'{ITEM}/machete.png'); sledge().save(f'{ITEM}/sledgehammer.png')

# ---------- armour items ----------
def vest(col, accent, text=None):
    c = C()
    c.rect(3, 3, 12, 13, col, top=shade(col, 1.25), bot=shade(col, 0.7))
    c.rect(6, 2, 9, 4, (0, 0, 0, 0)); c.rect(3, 2, 4, 3, col); c.rect(11, 2, 12, 3, col)  # neck hole, straps
    c.rect(4, 8, 11, 9, accent)
    for x in (4, 7, 10): c.rect(x, 10, x+1, 12, shade(col, 0.8))  # pouches
    c.outline(); return c
def helmet(col, visor=None):
    c = C()
    c.rect(3, 4, 12, 10, col, top=shade(col, 1.25)); c.rect(4, 3, 11, 3, shade(col, 1.25)); c.rect(2, 9, 13, 11, shade(col, 0.8))
    if visor: c.rect(4, 7, 11, 12, visor, top=shade(visor, 1.4))
    c.outline(); return c
vest(hexc('1f2a44'), hexc('e8e8e8')).save(f'{ITEM}/police_vest.png')
vest(hexc('4f5a2e'), hexc('3a4220')).save(f'{ITEM}/military_vest.png')
helmet(hexc('56602f')).save(f'{ITEM}/military_helmet.png')
helmet(hexc('202226'), (90, 120, 150, 200)).save(f'{ITEM}/riot_helmet.png')

# ---------- medical / food / backpack ----------
def bandage():
    c = C(); W = hexc('ece8dc')
    c.rect(3, 5, 12, 12, W, top=hexc('ffffff'), bot=hexc('c8c2b2'))
    for y in (7, 9, 11): c.rect(3, y, 12, y, hexc('d8d2c2'))
    c.rect(12, 9, 14, 12, W)
    c.outline(); return c
def bottle(body, cap, label):
    c = C()
    c.rect(4, 5, 11, 14, body, top=shade(body, 1.2), bot=shade(body, 0.7)); c.rect(5, 2, 10, 4, cap, top=shade(cap, 1.3))
    c.rect(4, 8, 11, 11, label)
    c.outline(); return c
def first_aid():
    c = C(); RED = hexc('c42a2a')
    c.rect(1, 4, 14, 13, RED, top=hexc('e04848'), bot=hexc('8a1a1a')); c.rect(5, 2, 10, 3, hexc('333333'))
    c.rect(6, 6, 9, 11, hexc('ffffff')); c.rect(4, 7, 11, 10, hexc('ffffff'))
    c.outline(); return c
def can(label, band):
    c = C(); M = hexc('b8bcc4')
    c.rect(3, 3, 12, 13, M, top=hexc('e0e4ea'), bot=hexc('80848c'))
    c.rect(3, 5, 12, 11, label); c.rect(3, 7, 12, 8, band)
    c.outline(); return c
def chips():
    c = C(); B = hexc('e0b020')
    c.rect(3, 2, 12, 14, B, top=hexc('c02020'), bot=hexc('c02020'))
    c.rect(5, 6, 10, 10, hexc('f6e080')); c.px(6, 7, hexc('d09030')); c.px(8, 8, hexc('d09030')); c.px(9, 7, hexc('d09030'))
    c.outline(); return c
def bar():
    c = C()
    for i in range(10): c.rect(2+i, 9-i//2, 3+i, 12-i//2, hexc('2a6fb0') if 2 < i < 8 else hexc('d0d0d8'))
    c.rect(5, 8, 9, 9, hexc('f0c040'))
    c.outline(); return c
def water():
    c = C(); W = (150, 200, 240, 210)
    c.rect(5, 5, 10, 14, W, top=(190, 225, 250, 220)); c.rect(6, 2, 9, 4, hexc('2a6fd0'))
    c.rect(5, 8, 10, 10, hexc('3a90e0')); c.px(6, 11, (230, 245, 255, 230))
    c.outline(); return c
def backpack():
    c = C(); G = hexc('4a5a36')
    c.rect(3, 3, 12, 14, G, top=shade(G, 1.25), bot=shade(G, 0.7)); c.rect(5, 1, 10, 2, shade(G, 0.8))
    c.rect(4, 8, 11, 13, shade(G, 0.85), top=shade(G, 1.1)); c.rect(7, 8, 8, 10, hexc('c0b070'))
    c.outline(); return c
bandage().save(f'{ITEM}/bandage.png')
bottle(hexc('e8e8e8'), hexc('d03030'), hexc('3070c0')).save(f'{ITEM}/painkillers.png')
bottle(hexc('d08a30'), hexc('f0f0f0'), hexc('f0f0f0')).save(f'{ITEM}/antibiotics.png')
first_aid().save(f'{ITEM}/first_aid_kit.png')
can(hexc('b04020'), hexc('e8c040')).save(f'{ITEM}/canned_beans.png')
can(hexc('c02828'), hexc('f0f0f0')).save(f'{ITEM}/canned_soup.png')
chips().save(f'{ITEM}/chips.png'); bar().save(f'{ITEM}/energy_bar.png'); water().save(f'{ITEM}/bottled_water.png')
backpack().save(f'{ITEM}/backpack.png')

# effect icon
e = C(18, 18); G = hexc('5a8a2a')
for y in range(18):
    for x in range(18):
        if (x-9)**2 + (y-9)**2 <= 30: e.px(x, y, G if (x*7+y*3) % 5 else hexc('7ab040'))
for (x, y) in ((6, 7), (11, 7), (8, 11), (9, 11), (10, 11)): e.px(x, y, hexc('1a2a0a'))
e.outline(); e.save(os.path.join(A, 'textures/mob_effect/infection.png'))

# ---------- armour model layers (64x32, vanilla biped layout) ----------
AR = os.path.join(R, 'assets/minecraft/textures/models/armor')
def body_box(c, col, accent, camo=None):
    # body cube at u16 v16, 8x12x4: top 20-27/16-19, faces row y20-31: right 16-19, front 20-27, left 28-31, back 32-39
    for x in range(16, 40):
        for y in range(20, 32):
            cc = col
            if camo and ((x*5 + y*3) % 7 < 2 or (x*2 + y*7) % 9 == 0): cc = camo
            c.px(x, y, cc)
    for x in range(20, 28): c.px(x, 16, col); c.px(x, 19, col)
    for x in range(20, 28):
        for y in (16, 17, 18, 19): c.px(x, y, col)
    for x in range(28, 36):
        for y in range(16, 20): c.px(x, y, col)
    for x in range(16, 40):  # chest band
        c.px(x, 26, accent); c.px(x, 27, accent)
    for x in (21, 24, 33, 36):  # pouches front/back
        for y in (28, 29, 30): c.px(x, y, shade(col, 0.75)); c.px(x+1, y, shade(col, 0.75))
    for x in range(22, 26): c.px(x, 20, (0, 0, 0, 0))  # collar dip
def head_box(c, col, visor=None, camo=None):
    # head cube u0 v0 8x8x8 scaled up as armour: top 8-15/0-7, bottom 16-23/0-7, faces y8-15: right 0-7, front 8-15, left 16-23, back 24-31
    for x in range(8, 16):
        for y in range(0, 8): c.px(x, y, shade(col, 1.15))
    for x in range(0, 32):
        for y in range(8, 12):
            cc = col
            if camo and (x*3 + y*5) % 7 < 2: cc = camo
            c.px(x, y, cc)
        c.px(x, 12, shade(col, 0.8))
    if visor:
        for x in range(8, 16):
            for y in range(10, 16): c.px(x, y, visor)
            c.px(x, 10, shade(visor, 1.5))
pol = C(64, 32); body_box(pol, hexc('1f2a44'), hexc('e8e8e8')); pol.save(f'{AR}/outbreak_police_layer_1.png')
mil = C(64, 32); body_box(mil, hexc('4f5a2e'), hexc('3a4220'), camo=hexc('6b6a3a')); head_box(mil, hexc('56602f'), camo=hexc('3f4720')); mil.save(f'{AR}/outbreak_military_layer_1.png')
riot = C(64, 32); head_box(riot, hexc('202226'), visor=(90, 120, 150, 150)); riot.save(f'{AR}/outbreak_riot_layer_1.png')
for n in ('police', 'military', 'riot'):
    C(64, 32).save(f'{AR}/outbreak_{n}_layer_2.png')

# ---------- item models ----------
MD = mk(os.path.join(A, 'models/item'))
GUNS = ['pistol', 'smg', 'shotgun', 'hunting_rifle', 'assault_rifle']
MELEE = ['baseball_bat', 'spiked_bat', 'crowbar', 'machete', 'sledgehammer']
OTHER = ['ammo_9mm', 'ammo_shells', 'ammo_rifle', 'police_vest', 'military_vest', 'military_helmet', 'riot_helmet',
         'bandage', 'painkillers', 'antibiotics', 'first_aid_kit', 'canned_beans', 'canned_soup', 'chips', 'energy_bar',
         'bottled_water', 'backpack']
gun_display = {
    'thirdperson_righthand': {'rotation': [0, -90, 0], 'translation': [0, 2.5, 1.5], 'scale': [0.85, 0.85, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [0, 2.5, 1.5], 'scale': [0.85, 0.85, 0.85]},
    'firstperson_righthand': {'rotation': [0, -90, 0], 'translation': [1.5, 3.4, 0.5], 'scale': [0.8, 0.8, 0.8]},
    'firstperson_lefthand': {'rotation': [0, 90, 0], 'translation': [1.5, 3.4, 0.5], 'scale': [0.8, 0.8, 0.8]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.6, 0.6, 0.6]},
    'fixed': {'rotation': [0, 180, 0], 'scale': [1, 1, 1]},
    'gui': {'rotation': [0, 0, 0], 'scale': [1, 1, 1]},
}
for g in GUNS:
    json.dump({'parent': 'item/generated', 'textures': {'layer0': f'outbreak:item/{g}'}, 'display': gun_display}, open(f'{MD}/{g}.json', 'w'), indent=1)
for m in MELEE:
    json.dump({'parent': 'item/handheld', 'textures': {'layer0': f'outbreak:item/{m}'}}, open(f'{MD}/{m}.json', 'w'), indent=1)
for o in OTHER:
    json.dump({'parent': 'item/generated', 'textures': {'layer0': f'outbreak:item/{o}'}}, open(f'{MD}/{o}.json', 'w'), indent=1)

# ---------- sounds.json ----------
snd = {s: {'sounds': [{'name': f'outbreak:{s}', 'attenuation_distance': 64 if 'fire' in s else 16}], 'subtitle': f'subtitles.outbreak.{s}'}
       for s in ('pistol_fire', 'shotgun_fire', 'rifle_fire', 'smg_fire', 'reload', 'empty_click')}
json.dump(snd, open(os.path.join(A, 'sounds.json'), 'w'), indent=1)

# ---------- lang ----------
names = {
    'ammo_9mm': '9mm Rounds', 'ammo_shells': 'Shotgun Shells', 'ammo_rifle': 'Rifle Rounds',
    'pistol': 'Pistol', 'smg': 'Submachine Gun', 'shotgun': 'Pump Shotgun', 'hunting_rifle': 'Hunting Rifle', 'assault_rifle': 'Assault Rifle',
    'baseball_bat': 'Baseball Bat', 'spiked_bat': 'Spiked Bat', 'crowbar': 'Crowbar', 'machete': 'Machete', 'sledgehammer': 'Sledgehammer',
    'police_vest': 'Police Vest', 'military_vest': 'Military Vest', 'military_helmet': 'Military Helmet', 'riot_helmet': 'Riot Helmet',
    'bandage': 'Bandage', 'painkillers': 'Painkillers', 'antibiotics': 'Antibiotics', 'first_aid_kit': 'First Aid Kit',
    'canned_beans': 'Canned Beans', 'canned_soup': 'Canned Soup', 'chips': 'Bag of Chips', 'energy_bar': 'Energy Bar',
    'bottled_water': 'Bottled Water', 'backpack': 'Backpack'}
lang = {f'item.outbreak.{k}': v for k, v in names.items()}
lang.update({
    'itemGroup.outbreak': 'Outbreak',
    'effect.outbreak.infection': 'Infection',
    'death.attack.outbreak.infection': '%1$s succumbed to the infection',
    'death.attack.outbreak.infection.player': '%1$s succumbed to the infection',
    'key.outbreak.reload': 'Reload', 'category.outbreak': 'Outbreak',
    'tooltip.outbreak.ammo': 'Loaded: %s / %s  (%s)', 'tooltip.outbreak.reload': 'R to reload, or fire when empty',
    'tooltip.outbreak.scope': 'Sneak to aim down the scope',
    'tooltip.outbreak.bandage': 'Heals a little and stops the bleeding',
    'tooltip.outbreak.painkillers': 'Dulls the pain: regeneration and resistance',
    'tooltip.outbreak.antibiotics': 'Cures zombie infection',
    'tooltip.outbreak.first_aid': 'Heals a lot and clears poison',
    'tooltip.outbreak.backpack': 'Use to open. %s of 27 slots used',
    'msg.outbreak.bitten': 'You\'ve been bitten... the wound is burning.',
    'msg.outbreak.infection_1': 'You feel feverish.',
    'msg.outbreak.infection_2': 'The fever is getting worse. Find antibiotics.',
    'msg.outbreak.infection_3': 'You can barely stand. You don\'t have long.',
    'msg.outbreak.cured': 'The fever breaks. You\'re going to live.',
    'gamerule.outbreakZombies': 'Outbreak: zombie hordes', 'gamerule.outbreakDaylightZombies': 'Outbreak: zombies survive daylight',
    'gamerule.outbreakInfection': 'Outbreak: bites can infect', 'gamerule.outbreakZombieCap': 'Outbreak: zombies around each player',
    'gamerule.outbreakSprinterPercent': 'Outbreak: % of zombies that sprint',
})
for s in ('pistol_fire', 'shotgun_fire', 'rifle_fire', 'smg_fire'): lang[f'subtitles.outbreak.{s}'] = 'Gunshot'
lang['subtitles.outbreak.reload'] = 'Gun reloads'; lang['subtitles.outbreak.empty_click'] = 'Gun clicks empty'
json.dump(lang, open(os.path.join(mk(os.path.join(A, 'lang')), 'en_us.json'), 'w'), indent=1)

# ---------- recipes ----------
RC = mk(os.path.join(D, 'outbreak/recipes'))
def shaped(name, pattern, keys, count=1):
    json.dump({'type': 'minecraft:crafting_shaped', 'pattern': pattern,
               'key': {k: {'item': v} for k, v in keys.items()}, 'result': {'item': f'outbreak:{name}', 'count': count}},
              open(f'{RC}/{name}.json', 'w'), indent=1)
def shapeless(name, items, count=1):
    json.dump({'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': i} for i in items],
               'result': {'item': f'outbreak:{name}', 'count': count}}, open(f'{RC}/{name}.json', 'w'), indent=1)
shapeless('ammo_9mm', ['minecraft:iron_nugget', 'minecraft:iron_nugget', 'minecraft:gunpowder'], 8)
shapeless('ammo_shells', ['minecraft:paper', 'minecraft:iron_nugget', 'minecraft:gunpowder'], 4)
shapeless('ammo_rifle', ['minecraft:copper_ingot', 'minecraft:iron_nugget', 'minecraft:gunpowder'], 6)
shaped('baseball_bat', ['  P', ' P ', 'S  '], {'P': 'minecraft:oak_planks', 'S': 'minecraft:stick'})
shapeless('spiked_bat', ['outbreak:baseball_bat', 'minecraft:iron_nugget', 'minecraft:iron_nugget', 'minecraft:iron_nugget'])
shaped('crowbar', ['  I', ' I ', 'I  '], {'I': 'minecraft:iron_ingot'})
shaped('machete', ['  I', ' I ', 'L  '], {'I': 'minecraft:iron_ingot', 'L': 'minecraft:leather'})
shaped('sledgehammer', ['III', ' S ', ' S '], {'I': 'minecraft:iron_block', 'S': 'minecraft:stick'})
shapeless('bandage', ['minecraft:white_wool', 'minecraft:string'], 3)
shaped('first_aid_kit', ['LBL', 'BRB', 'LBL'], {'L': 'minecraft:leather', 'B': 'outbreak:bandage', 'R': 'minecraft:red_dye'})
shaped('backpack', ['SLS', 'L L', 'LLL'], {'S': 'minecraft:string', 'L': 'minecraft:leather'})

# ---------- damage type + tags ----------
dt = mk(os.path.join(D, 'outbreak/damage_type'))
json.dump({'message_id': 'outbreak.infection', 'scaling': 'never', 'exhaustion': 0.0}, open(f'{dt}/infection.json', 'w'), indent=1)
tg = mk(os.path.join(D, 'minecraft/tags/damage_type'))
for t in ('bypasses_armor', 'bypasses_effects', 'bypasses_resistance', 'bypasses_enchantments', 'bypasses_shield', 'no_impact', 'bypasses_invulnerability'):
    json.dump({'replace': False, 'values': ['outbreak:infection']}, open(f'{tg}/{t}.json', 'w'), indent=1)
print('assets ok')
