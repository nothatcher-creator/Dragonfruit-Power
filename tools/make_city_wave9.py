"""Wave 9: potholes with snow-like layers (asphalt and dirt), flies, neon lettering.

Usage: python3 make_city_wave9.py <resources dir>
"""
import os, sys, json, random
from PIL import Image, ImageDraw

RES = sys.argv[1]
A = os.path.join(RES, 'assets/vehicleforge'); D = os.path.join(RES, 'data/vehicleforge')
rnd = random.Random(909)


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2)


def cl(v): return max(0, min(255, int(v)))


def save(path, im):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)


# ---------------------------------------------------------------- textures
def pothole_top(base, dark, grit):
    """broken surface: the base colour at the edges, crumbling into a darker gravelly middle"""
    im = Image.new('RGBA', (16, 16)); px = im.load()
    cx, cy = 7.5 + rnd.uniform(-1, 1), 7.5 + rnd.uniform(-1, 1)
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 + rnd.uniform(-1.6, 1.6)
            if d < 5.5:
                c = grit if rnd.random() < 0.3 else dark
                n = rnd.randint(-10, 10)
            elif d < 6.8:
                c = tuple(cl((a + b) / 2) for a, b in zip(base, dark)); n = rnd.randint(-6, 6)
            else:
                c = base; n = rnd.randint(-7, 7)
            px[x, y] = tuple(cl(v + n) for v in c) + (255,)
    d = ImageDraw.Draw(im)
    for _ in range(4):                                     # cracks running out from the hole
        a = rnd.uniform(0, 6.28); import math
        x0, y0 = cx + math.cos(a) * 5, cy + math.sin(a) * 5
        x1, y1 = cx + math.cos(a) * 9, cy + math.sin(a) * 9
        d.line([(x0, y0), (x1, y1)], fill=tuple(cl(v - 18) for v in dark) + (255,))
    return im


TB = os.path.join(A, 'textures/block')
save(os.path.join(TB, 'pothole.png'), pothole_top((56, 57, 61), (34, 34, 36), (96, 94, 90)))
save(os.path.join(TB, 'dirt_pothole.png'), pothole_top((132, 102, 66), (84, 62, 40), (120, 112, 100)))

fly = Image.new('RGBA', (8, 8), (0, 0, 0, 0)); d = ImageDraw.Draw(fly)
d.rectangle([3, 3, 4, 4], fill=(20, 20, 22, 255)); d.point((2, 2), fill=(200, 210, 220, 150)); d.point((5, 2), fill=(200, 210, 220, 150))
save(os.path.join(A, 'textures/particle/fly.png'), fly)

ic = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(ic)
for _ in range(9):
    x, y = rnd.randint(2, 13), rnd.randint(2, 13)
    d.point((x, y), fill=(20, 20, 22, 255)); d.point((x + 1, y), fill=(20, 20, 22, 255))
    d.point((x - 1, y - 1), fill=(190, 205, 220, 160)); d.point((x + 2, y - 1), fill=(190, 205, 220, 160))
save(os.path.join(A, 'textures/item/flies.png'), ic)

ns = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(ns)
glow = (255, 90, 190, 110); core = (255, 210, 240, 255)
for c, w in ((glow, 3), (core, 1)):                       # a looping neon "N" with a halo
    d.line([(4, 12), (4, 4), (11, 12), (11, 4)], fill=c, width=w)
save(os.path.join(A, 'textures/item/neon_sign.png'), ns)


# ---------------------------------------------------------------- models and blockstates
def bs(name, variants): wj(os.path.join(A, 'blockstates', name + '.json'), {"variants": variants})
def model(name, obj): wj(os.path.join(A, 'models/block', name + '.json'), obj)
def item(name, obj): wj(os.path.join(A, 'models/item', name + '.json'), obj)
def loot(name):
    wj(os.path.join(D, 'loot_tables/blocks', name + '.json'), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "vehicleforge:" + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


for name, top, side in (('pothole', 'vehicleforge:block/pothole', 'vehicleforge:block/asphalt'),
                        ('dirt_pothole', 'vehicleforge:block/dirt_pothole', 'minecraft:block/dirt')):
    v = {}
    for l in range(1, 9):
        h = l * 2
        faces = {"down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"},
                 "up": {"uv": [0, 0, 16, 16], "texture": "#top"}}
        if h == 16: faces["up"]["cullface"] = "up"
        for f in ('north', 'south', 'east', 'west'):
            faces[f] = {"uv": [0, 16 - h, 16, 16], "texture": "#side", "cullface": f}
        model(f'{name}_height{h}', {"parent": "minecraft:block/block", "textures": {"top": top, "side": side, "particle": top},
                                     "elements": [{"from": [0, 0, 0], "to": [16, h, 16], "faces": faces}]})
        v[f'layers={l}'] = {"model": f'vehicleforge:block/{name}_height{h}'}
    bs(name, v)
    item(name, {"parent": f'vehicleforge:block/{name}_height10'})
    loot(name)

model('flies', {"textures": {"particle": "vehicleforge:item/flies"}})
bs('flies', {"": {"model": "vehicleforge:block/flies"}})
item('flies', {"parent": "minecraft:item/generated", "textures": {"layer0": "vehicleforge:item/flies"}})
wj(os.path.join(D, 'loot_tables/blocks/flies.json'), {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "vehicleforge:flies"}],
     "conditions": [{"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]}]})

model('neon_sign', {"textures": {"particle": "minecraft:block/glass"}})
bs('neon_sign', {f"facing={f}": {"model": "vehicleforge:block/neon_sign"} for f in ('north', 'south', 'east', 'west')})
item('neon_sign', {"parent": "minecraft:item/generated", "textures": {"layer0": "vehicleforge:item/neon_sign"}})
loot('neon_sign')

wj(os.path.join(A, 'particles/fly.json'), {"textures": ["vehicleforge:fly"]})

# ---------------------------------------------------------------- tags, recipes, lang
def tag(tool, names):
    p = os.path.join(RES, f'data/minecraft/tags/blocks/mineable/{tool}.json'); t = json.load(open(p))
    for n in names:
        if 'vehicleforge:' + n not in t['values']: t['values'].append('vehicleforge:' + n)
    json.dump(t, open(p, 'w'), indent=2)


tag('pickaxe', ['pothole'])
tag('shovel', ['dirt_pothole'])


def shaped(name, pattern, key, count=1):
    wj(os.path.join(D, 'recipes', name + '.json'), {"type": "minecraft:crafting_shaped", "pattern": pattern,
                                                     "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": "vehicleforge:" + name, "count": count}})


shaped('pothole', ['AG'], {'A': 'vehicleforge:asphalt', 'G': 'minecraft:gravel'}, 2)
shaped('dirt_pothole', ['DG'], {'D': 'minecraft:coarse_dirt', 'G': 'minecraft:gravel'}, 2)
shaped('flies', [' S ', 'SRS'], {'S': 'minecraft:string', 'R': 'minecraft:rotten_flesh'}, 2)
shaped('neon_sign', ['GDG', ' R '], {'G': 'minecraft:glass_pane', 'D': 'minecraft:glowstone_dust', 'R': 'minecraft:redstone'}, 2)

LP = os.path.join(A, 'lang/en_us.json'); lang = json.load(open(LP))
lang.update({
    'block.vehicleforge.pothole': 'Pothole', 'block.vehicleforge.dirt_pothole': 'Dirt Pothole', 'block.vehicleforge.flies': 'Flies',
    'block.vehicleforge.neon_sign': 'Neon Sign',
    'screen.vehicleforge.neon_sign': 'Neon Sign', 'screen.vehicleforge.neon_hint': "Type the text ( | starts a new line ), then tap a colour",
    'screen.vehicleforge.neon_size': 'Size: %s', 'screen.vehicleforge.neon_flicker_on': 'Flicker: On', 'screen.vehicleforge.neon_flicker_off': 'Flicker: Off',
})
json.dump(lang, open(LP, 'w'), indent=2)
print('wave 9 ok')
