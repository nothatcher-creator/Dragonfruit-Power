"""3D gun models: each gun is built from boxes in the plane of its old sprite (barrel towards +X, grip down),
with a little thickness in Z, and keeps the display transforms the flat model had."""
import json, os, sys, random
from PIL import Image

R = sys.argv[1]
A = os.path.join(R, 'src/main/resources/assets/outbreak')
MD, TX = f'{A}/models/item', f'{A}/textures/item'
rr = random.Random(5)

# ---------- material swatches: a 16x16 texture of 4x4 cells
MAT = {  # name: (cell x, cell y, colour)
    'black': (0, 0, (34, 35, 38)), 'dark': (4, 0, (58, 60, 64)), 'gunmetal': (8, 0, (78, 84, 92)), 'silver': (12, 0, (170, 172, 176)),
    'wood': (0, 4, (128, 80, 44)), 'darkwood': (4, 4, (84, 52, 30)), 'olive': (8, 4, (82, 88, 56)), 'tan': (12, 4, (176, 150, 104)),
    'rubber': (0, 8, (26, 26, 28)), 'brass': (4, 8, (190, 150, 60)), 'glass': (8, 8, (60, 110, 150)), 'red': (12, 8, (190, 40, 30)),
    'steel': (0, 12, (110, 114, 120)), 'polymer': (4, 12, (46, 46, 50)),
}
img = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
for name, (cx, cy, col) in MAT.items():
    for y in range(4):
        for x in range(4):
            g = 1 + rr.uniform(-0.07, 0.07) + (0.06 if y == 0 else -0.05 if y == 3 else 0)
            if name in ('wood', 'darkwood') and (x + y * 3) % 4 == 0: g *= 0.88      # grain
            if name == 'rubber' and (x + y) % 2 == 0: g *= 1.3                          # checkering
            img.putpixel((cx + x, cy + y), tuple(max(0, min(255, int(c * g))) for c in col) + (255,))
img.save(f'{TX}/gun_parts.png')

def box(x0, x1, y0, y1, z0=7, z1=9, mat='black', rot=None):
    cx, cy, col = MAT[mat]
    uv = [cx + 0.25, cy + 0.25, cx + 3.75, cy + 3.75]
    e = {'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': {f: {'uv': uv, 'texture': '#0'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}
    if rot: e['rotation'] = {'angle': rot[0], 'axis': 'z', 'origin': [rot[1], rot[2], 8]}
    return e

def grip(x0, x1, ytop, ybot, mat, z0=7.1, z1=8.9, angle=-22.5):
    return box(x0, x1, ybot, ytop, z0, z1, mat, (angle, (x0 + x1) / 2, ytop))

def guard(x0, x1, y):
    return [box(x0, x1, y - 0.5, y, 7.6, 8.4, 'black'), box(x1 - 0.5, x1, y - 0.5, y + 1, 7.6, 8.4, 'black'), box(x0 + 0.6, x0 + 1, y, y + 0.8, 7.8, 8.2, 'dark')]

def pistol(silenced=False):
    e = [box(3.5, 11.5, 9.5, 11.5, 7.2, 8.8, 'black'),            # slide
         box(4, 11, 8.5, 9.5, 7.3, 8.7, 'dark'),                    # frame
         box(11.5, 12, 10, 11, 7.6, 8.4, 'dark'),                   # muzzle
         box(4, 4.6, 11.5, 12, 7.5, 8.5, 'black'), box(10.6, 11, 11.5, 12, 7.8, 8.2, 'black'),   # sights
         box(5, 7, 10.5, 11.5, 8.8, 8.9, 'dark'),                   # slide serrations
         grip(4, 6.5, 8.5, 3, 'rubber')] + guard(6.5, 8.6, 8)
    if silenced: e.append(box(12, 16.5, 9.6, 11.4, 7.1, 8.9, 'gunmetal'))
    return e

def revolver():
    return [box(9, 15, 10, 11, 7.5, 8.5, 'silver'), box(9, 14, 11, 11.4, 7.8, 8.2, 'silver'),       # barrel and rib
            box(6, 9, 8.6, 11, 6.9, 9.1, 'steel'), box(6.2, 8.8, 8.8, 10.8, 6.8, 9.2, 'gunmetal'),   # cylinder
            box(4, 6, 8.5, 11.5, 7.3, 8.7, 'silver'), box(6, 9, 11, 11.5, 7.4, 8.6, 'silver'),       # frame, top strap
            box(3.6, 4.4, 11, 12.2, 7.7, 8.3, 'steel'), box(14.4, 14.8, 11.4, 12, 7.8, 8.2, 'silver'),
            grip(3.5, 6, 8.6, 3.5, 'wood', 7.0, 9.0)] + guard(6, 8.2, 8.3)

def smg():
    return [box(3, 12, 8.5, 11, 7, 9, 'black'), box(12, 14.8, 9.4, 10.4, 7.5, 8.5, 'dark'),
            box(9, 12, 8.2, 8.6, 7.2, 8.8, 'dark'),
            box(7.2, 8.8, 3, 8.5, 7.3, 8.7, 'dark'), box(7.1, 8.9, 2.6, 3.1, 7.2, 8.8, 'black'),      # magazine
            box(-1, 3, 9.6, 10.1, 7.6, 8.4, 'steel'), box(-1, 3, 7.4, 7.9, 7.6, 8.4, 'steel'), box(-1.4, -0.8, 7.2, 10.4, 7.2, 8.8, 'rubber'),
            box(4, 5, 11, 11.8, 7.6, 8.4, 'black'), box(11, 11.5, 11, 11.8, 7.8, 8.2, 'black'),
            grip(4.2, 6, 8.5, 4.5, 'polymer')] + guard(5.8, 7.2, 8)

def assault_rifle():
    return [box(-1, 3.5, 6.5, 10, 7.1, 8.9, 'polymer', (-22.5, 3.5, 10)), box(-1.5, -0.8, 5.5, 10, 7, 9, 'rubber', (-22.5, 3.5, 10)),
            box(3.5, 10, 8, 11, 6.9, 9.1, 'black'),                                                   # receiver
            box(10, 14, 8.3, 10.6, 6.7, 9.3, 'dark'),                                                 # handguard
            box(14, 17.5, 9, 9.8, 7.6, 8.4, 'black'), box(17.5, 18.4, 8.8, 10, 7.5, 8.5, 'dark'),     # barrel, flash hider
            box(13, 13.5, 10.6, 12.4, 7.8, 8.2, 'black'), box(5, 9, 11, 12, 7.5, 8.5, 'black'), box(5.2, 6, 12, 12.6, 7.7, 8.3, 'black'),
            box(7.8, 9.6, 3.6, 8, 7.3, 8.7, 'dark', (22.5, 8.7, 8)),                                 # curved mag
            box(8.4, 10.2, 1.4, 4, 7.3, 8.7, 'dark', (45, 9.3, 4)),
            grip(5, 6.8, 8, 4.4, 'polymer')] + guard(6.6, 7.8, 7.8)

def shotgun():
    return [box(-1, 4, 6, 9.4, 7.1, 8.9, 'wood', (-22.5, 4, 9.4)), box(-1.5, -0.8, 5, 9.6, 7, 9, 'rubber', (-22.5, 4, 9.4)),
            box(3, 5, 7.2, 9, 7.2, 8.8, 'wood'),
            box(4.6, 8.4, 7.8, 10.6, 7.1, 8.9, 'black'),
            box(8.4, 16.6, 9.5, 10.5, 7.5, 8.5, 'black'), box(8.4, 15.2, 8.6, 9.5, 7.6, 8.4, 'dark'),
            box(10, 13.6, 8.1, 9.9, 6.8, 9.2, 'wood'), box(16, 16.4, 10.5, 11, 7.8, 8.2, 'brass')] + guard(5, 7, 7.8)

def double_barrel():
    return [box(-1, 5, 6, 9.4, 7.1, 8.9, 'darkwood', (-22.5, 5, 9.4)), box(-1.5, -0.8, 5, 9.6, 7, 9, 'rubber', (-22.5, 5, 9.4)),
            box(4, 5.6, 7.2, 9.2, 7.2, 8.8, 'darkwood'), box(5, 8.4, 8, 10.6, 7, 9, 'silver'),
            box(8.4, 17, 9.2, 10.3, 6.9, 8, 'black'), box(8.4, 17, 9.2, 10.3, 8, 9.1, 'black'),
            box(8.4, 17, 10.3, 10.6, 7.7, 8.3, 'dark'), box(8.4, 12.4, 8.3, 9.2, 7.1, 8.9, 'darkwood'),
            box(6, 6.6, 10.6, 11.2, 7.3, 7.8, 'steel'), box(6, 6.6, 10.6, 11.2, 8.2, 8.7, 'steel')] + guard(5.6, 7.4, 8)

def hunting_rifle():
    return [box(-1, 5, 6.2, 9.2, 7.1, 8.9, 'wood', (-22.5, 5, 9.2)), box(-1.5, -0.8, 5, 9.4, 7, 9, 'rubber', (-22.5, 5, 9.2)),
            box(4, 13, 8, 9.2, 7.1, 8.9, 'wood'),                                                     # stock to forend
            box(5, 9.5, 9, 10.2, 7.2, 8.8, 'black'), box(9.5, 18, 9.2, 9.9, 7.6, 8.4, 'black'),
            box(6.2, 6.8, 9.2, 9.8, 8.8, 10.4, 'steel'), box(6.1, 6.9, 9.1, 9.9, 10.2, 10.8, 'silver'),  # bolt handle
            box(5.5, 10, 10.6, 11.6, 7.4, 8.6, 'black'), box(5, 6, 10.4, 11.8, 7.25, 8.75, 'black'), box(9.8, 10.8, 10.4, 11.8, 7.25, 8.75, 'black'),
            box(6.4, 6.9, 10.2, 10.6, 7.7, 8.3, 'black'), box(8.8, 9.3, 10.2, 10.6, 7.7, 8.3, 'black'),
            box(10.8, 10.9, 10.6, 11.6, 7.5, 8.5, 'glass')] + guard(5.6, 7.6, 7.9)

def sniper_rifle():
    return [box(-1, 4.5, 6.2, 9.6, 7.1, 8.9, 'olive', (-22.5, 4.5, 9.6)), box(-1.5, -0.8, 5, 9.6, 7, 9, 'rubber', (-22.5, 4.5, 9.6)),
            box(-0.5, 3.5, 9.6, 10.2, 7.3, 8.7, 'olive'),                                             # cheek rest
            box(4, 11, 7.8, 9.4, 7, 9, 'olive'), box(5, 10, 9.2, 10.4, 7.2, 8.8, 'black'),
            box(10, 18.5, 9, 9.9, 7.5, 8.5, 'black'), box(18.5, 19.6, 8.7, 10.2, 7.3, 8.7, 'dark'),
            box(4.5, 11, 11, 12.2, 7.4, 8.6, 'black'), box(10.4, 12, 10.7, 12.5, 7.15, 8.85, 'black'), box(3.6, 5, 10.8, 12.4, 7.2, 8.8, 'black'),
            box(12, 12.1, 10.8, 12.4, 7.3, 8.7, 'glass'), box(6, 6.6, 10.4, 11, 7.6, 8.4, 'black'), box(9.4, 10, 10.4, 11, 7.6, 8.4, 'black'),
            box(7.4, 8.2, 12.2, 12.8, 7.7, 8.3, 'black'), box(7.4, 8.2, 11.4, 12, 8.6, 9.2, 'black'),     # turrets
            box(6.5, 8.3, 4.8, 7.8, 7.3, 8.7, 'black'),                                               # box mag
            box(13, 13.5, 3.8, 9, 6.6, 7.1, 'steel', (22.5, 13.25, 9)), box(13, 13.5, 3.8, 9, 8.9, 9.4, 'steel', (22.5, 13.25, 9)),
            grip(4.6, 6.2, 7.8, 4.4, 'olive')] + guard(6.2, 6.6, 7.6)

def light_machine_gun():
    return [box(-1, 3, 6.4, 10.4, 7, 9, 'polymer', (-22.5, 3, 10.4)), box(-1.5, -0.8, 5.4, 10.6, 6.9, 9.1, 'rubber', (-22.5, 3, 10.4)),
            box(3, 11, 8, 11.5, 6.8, 9.2, 'black'), box(3.4, 10.6, 11.5, 11.9, 7.2, 8.8, 'dark'),     # receiver, feed cover
            box(11, 15, 8.7, 10.5, 7.1, 8.9, 'gunmetal'), box(11, 18.6, 9.2, 10, 7.6, 8.4, 'black'), box(18.6, 19.6, 9, 10.2, 7.4, 8.6, 'dark'),
            box(6.5, 10.5, 4, 8, 6, 10, 'olive'), box(6.8, 10.2, 7.6, 8.1, 5.8, 10.2, 'dark'),        # ammo box
            box(7, 10, 12.2, 12.6, 7.7, 8.3, 'black'), box(7, 7.4, 11.9, 12.4, 7.7, 8.3, 'black'), box(9.6, 10, 11.9, 12.4, 7.7, 8.3, 'black'),
            box(4, 5.4, 11.9, 12.7, 7.6, 8.4, 'black'), box(17.6, 18, 10, 11.2, 7.8, 8.2, 'black'),
            box(14.6, 15.1, 3.6, 8.8, 6.4, 6.9, 'steel', (22.5, 14.85, 8.8)), box(14.6, 15.1, 3.6, 8.8, 9.1, 9.6, 'steel', (22.5, 14.85, 8.8)),
            grip(4, 5.8, 8, 4.4, 'polymer')] + guard(5.6, 6.6, 7.8)

GUNS = {'pistol': pistol(), 'silenced_pistol': pistol(True), 'revolver': revolver(), 'smg': smg(), 'assault_rifle': assault_rifle(),
        'shotgun': shotgun(), 'double_barrel': double_barrel(), 'hunting_rifle': hunting_rifle(), 'sniper_rifle': sniper_rifle(),
        'light_machine_gun': light_machine_gun()}
for name, els in GUNS.items():
    p = f'{MD}/{name}.json'
    old = json.load(open(p))
    display = old.get('display', {})
    display.setdefault('gui', {'rotation': [0, 0, 0], 'scale': [1, 1, 1]})
    for e in els:
        for i in range(3):
            assert -16 <= e['from'][i] <= 32 and -16 <= e['to'][i] <= 32, (name, e)
    json.dump({'credit': 'Outbreak', 'gui_light': 'front',
               'textures': {'0': 'outbreak:item/gun_parts', 'particle': f'outbreak:item/{name}'},
               'elements': els, 'display': display}, open(p, 'w'), indent=1)
print('models:', ', '.join(GUNS))
