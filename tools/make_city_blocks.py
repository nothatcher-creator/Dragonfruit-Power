"""Textures, block models, blockstates, item models and loot tables for the Vehicle Forge city blocks."""
import os, sys, json, random
from PIL import Image, ImageDraw
RES = sys.argv[1]
A = os.path.join(RES, 'assets/vehicleforge'); D = os.path.join(RES, 'data/vehicleforge')
TEX = os.path.join(A, 'textures/block'); os.makedirs(TEX, exist_ok=True)
rnd = random.Random(42)
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2)
def noisy(base, var, spk=0.0, spk_col=None):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-var, var)
            c = tuple(max(0, min(255, v + n)) for v in base)
            if spk and rnd.random() < spk: c = spk_col
            px[x, y] = c + (255,)
    return im
def paint(im, cols, color, wear=0.12):
    px = im.load()
    for y in range(16):
        for x in cols:
            if rnd.random() > wear:
                n = rnd.randint(-12, 6)
                px[x, y] = tuple(max(0, min(255, v + n)) for v in color) + (255,)
    return im
ASPH = (56, 57, 61)
def asphalt(): return noisy(ASPH, 7, 0.06, (92, 92, 96))
WHITE = (232, 232, 228); YEL = (236, 190, 40)
T = {}
T['asphalt'] = asphalt()
T['asphalt_white_line'] = paint(asphalt(), range(6, 10), WHITE)
T['asphalt_yellow_line'] = paint(asphalt(), range(6, 10), YEL)
T['asphalt_double_yellow_line'] = paint(paint(asphalt(), range(3, 6), YEL), range(10, 13), YEL)
T['asphalt_crosswalk'] = paint(asphalt(), range(3, 13), WHITE, 0.08)
m = asphalt(); d = ImageDraw.Draw(m)
d.ellipse([0, 0, 15, 15], fill=(62, 62, 66, 255), outline=(40, 40, 42, 255))
for i in range(2, 14, 3): d.line([(3, i), (12, i)], fill=(48, 48, 52, 255))
d.ellipse([6, 6, 9, 9], outline=(80, 80, 84, 255)); T['manhole_cover'] = m
g = noisy((150, 150, 146), 6); d = ImageDraw.Draw(g)
d.rectangle([2, 3, 13, 12], fill=(30, 30, 32, 255))
for x in range(3, 13, 2): d.line([(x, 3), (x, 12)], fill=(85, 85, 88, 255))
T['storm_drain'] = g
s = noisy((172, 172, 168), 5); d = ImageDraw.Draw(s)
d.line([(0, 0), (15, 0)], fill=(140, 140, 136, 255)); d.line([(0, 0), (0, 15)], fill=(140, 140, 136, 255))
d.line([(0, 8), (15, 8)], fill=(150, 150, 146, 255)); d.line([(8, 0), (8, 15)], fill=(150, 150, 146, 255)); T['sidewalk'] = s
c = noisy((190, 190, 186), 4); d = ImageDraw.Draw(c); d.rectangle([0, 0, 15, 15], outline=(150, 150, 146, 255)); T['curb'] = c
T['curb_side'] = noisy((165, 165, 160), 5)
met = Image.new('RGBA', (16, 16)); px = met.load()
for y in range(16):
    for x in range(16):
        v = 52 + int(10 * (1 - abs(x - 7.5) / 8)) + rnd.randint(-2, 2); px[x, y] = (v, v + 2, v + 6, 255)
T['lamp_metal'] = met
li = Image.new('RGBA', (16, 16)); px = li.load()
for y in range(16):
    for x in range(16):
        dd = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5; v = max(0, 1 - dd / 11)
        px[x, y] = (255, int(225 + 25 * v), int(160 + 70 * v), 255)
T['lamp_light'] = li
T['traffic_housing'] = noisy((34, 34, 30), 3)
for i, name in enumerate(['green', 'yellow', 'red']):
    f = noisy((24, 24, 22), 2); d = ImageDraw.Draw(f)
    lamps = [(3, (255, 50, 40), (70, 18, 16)), (8, (255, 200, 40), (70, 56, 14)), (13, (60, 255, 120), (14, 60, 30))]
    on = {'green': 2, 'yellow': 1, 'red': 0}[name]
    for k, (cy, bright, dim) in enumerate(lamps):
        col = bright if k == on else dim
        d.ellipse([5, cy - 2, 10, cy + 2], fill=col + (255,))
        if k == on: d.point((6, cy - 1), fill=(255, 255, 255, 255))
    T['traffic_face_' + name] = f
co = Image.new('RGBA', (16, 16), (240, 110, 20, 255)); d = ImageDraw.Draw(co); d.rectangle([0, 6, 15, 9], fill=(245, 245, 245, 255)); T['cone'] = co
T['cone_base'] = noisy((30, 30, 30), 3)
T['barrier'] = noisy((182, 180, 172), 7, 0.04, (150, 148, 140))
rb = Image.new('RGBA', (16, 16)); px = rb.load()
for y in range(16):
    for x in range(16):
        px[x, y] = (240, 110, 20, 255) if ((x + y) // 4) % 2 == 0 else (245, 245, 245, 255)
T['roadblock'] = rb
T['roadblock_leg'] = noisy((235, 235, 230), 4)
hy = Image.new('RGBA', (16, 16)); px = hy.load()
for y in range(16):
    for x in range(16):
        v = int(30 * (1 - abs(x - 6) / 8)); px[x, y] = (min(255, 180 + v), 28 + v // 3, 28, 255)
T['hydrant'] = hy
bi = Image.new('RGBA', (16, 16)); px = bi.load()
for y in range(16):
    for x in range(16):
        g2 = 70 + (12 if x % 3 == 0 else 0) + rnd.randint(-3, 3); px[x, y] = (34, g2, 46, 255)
T['trash_bin'] = bi
bt = noisy((30, 60, 40), 3); d = ImageDraw.Draw(bt); d.rectangle([4, 4, 11, 11], fill=(12, 12, 12, 255)); T['trash_bin_top'] = bt
bw = Image.new('RGBA', (16, 16)); px = bw.load()
for y in range(16):
    for x in range(16):
        v = 150 + (10 if y % 4 == 0 else 0) + rnd.randint(-6, 6); px[x, y] = (v, int(v * 0.72), int(v * 0.45), 255)
T['bench_wood'] = bw
bs = Image.new('RGBA', (16, 16), (30, 90, 170, 255)); d = ImageDraw.Draw(bs)
d.rectangle([0, 0, 15, 15], outline=(240, 240, 240, 255)); d.rectangle([3, 4, 12, 10], fill=(240, 240, 240, 255))
d.rectangle([4, 5, 11, 7], fill=(30, 90, 170, 255)); d.point((5, 11), fill=(240, 240, 240, 255)); d.point((10, 11), fill=(240, 240, 240, 255))
T['bus_sign'] = bs
for k, im in T.items(): im.save(os.path.join(TEX, k + '.png'))

def tex(n): return 'vehicleforge:block/' + n
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
def el(frm, to, faces, rot=None):
    e = {"from": frm, "to": to, "faces": {}}
    for f, t in faces.items():
        e["faces"][f] = {"texture": t, "uv": uvfor(f, frm, to)}
    if rot: e["rotation"] = rot
    return e
def allf(t, skip=()): return {f: t for f in ['north', 'south', 'east', 'west', 'up', 'down'] if f not in skip}
def model(name, obj): wj(os.path.join(A, 'models/block', name + '.json'), obj)
def simple_state(name, m=None): wj(os.path.join(A, 'blockstates', name + '.json'), {"variants": {"": {"model": "vehicleforge:block/" + (m or name)}}})
def facing_state(name, m=None, extra=None):
    v = {}
    for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
        if extra:
            for key, mm in extra.items():
                v[f"facing={f},{key}"] = {"model": "vehicleforge:block/" + mm, "y": y} if y else {"model": "vehicleforge:block/" + mm}
        else:
            v[f"facing={f}"] = {"model": "vehicleforge:block/" + (m or name), "y": y} if y else {"model": "vehicleforge:block/" + (m or name)}
    wj(os.path.join(A, 'blockstates', name + '.json'), {"variants": v})
def item(name, parent=None): wj(os.path.join(A, 'models/item', name + '.json'), {"parent": "vehicleforge:block/" + (parent or name)})
def loot(name, slab=False):
    entry = {"type": "minecraft:item", "name": "vehicleforge:" + name}
    if slab:
        entry["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                               "conditions": [{"condition": "minecraft:block_state_property", "block": "vehicleforge:" + name, "properties": {"type": "double"}}]},
                              {"function": "minecraft:explosion_decay"}]
    wj(os.path.join(D, 'loot_tables/blocks', name + '.json'), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# full blocks
for n in ['asphalt', 'sidewalk']:
    model(n, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(n)}}); simple_state(n); item(n); loot(n)
for n, side in [('manhole_cover', 'asphalt'), ('storm_drain', 'asphalt')]:
    model(n, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex(n), "bottom": tex(side), "side": tex(side)}}); simple_state(n); item(n); loot(n)
# painted asphalt with an axis
for n in ['asphalt_white_line', 'asphalt_yellow_line', 'asphalt_double_yellow_line', 'asphalt_crosswalk']:
    model(n, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex(n), "bottom": tex('asphalt'), "side": tex('asphalt')}})
    wj(os.path.join(A, 'blockstates', n + '.json'), {"variants": {"axis=z": {"model": "vehicleforge:block/" + n},
                                                                   "axis=x": {"model": "vehicleforge:block/" + n, "y": 90}}})
    item(n); loot(n)
# slabs
for n, top, side in [('sidewalk_slab', 'sidewalk', 'sidewalk'), ('curb', 'curb', 'curb_side')]:
    model(n, {"parent": "minecraft:block/slab", "textures": {"bottom": tex(side), "top": tex(top), "side": tex(side)}})
    model(n + '_top', {"parent": "minecraft:block/slab_top", "textures": {"bottom": tex(side), "top": tex(top), "side": tex(side)}})
    model(n + '_double', {"parent": "minecraft:block/cube_bottom_top", "textures": {"bottom": tex(side), "top": tex(top), "side": tex(side)}})
    wj(os.path.join(A, 'blockstates', n + '.json'), {"variants": {
        "type=bottom": {"model": "vehicleforge:block/" + n}, "type=top": {"model": "vehicleforge:block/" + n + "_top"},
        "type=double": {"model": "vehicleforge:block/" + n + "_double"}}})
    item(n); loot(n, slab=True)
M = tex('lamp_metal')
model('lamp_post', {"parent": "minecraft:block/block", "textures": {"particle": M, "m": M},
                    "elements": [el([6, 0, 6], [10, 16, 10], allf("#m"))]})
simple_state('lamp_post'); item('lamp_post'); loot('lamp_post')
model('street_lamp', {"parent": "minecraft:block/block", "textures": {"particle": M, "m": M, "l": tex('lamp_light')},
    "elements": [
        el([6, 0, 6], [10, 13, 10], allf("#m")),
        el([7, 11, 6], [9, 13.5, 10], allf("#m")),
        el([7, 12, -8], [9, 14, 6], allf("#m")),
        el([4, 10, -16], [12, 13, -6], allf("#m")),
        el([5, 9.9, -15], [11, 10, -7], {"down": "#l", "up": "#l"}),
        el([5.5, 13, -13], [10.5, 13.6, -9], allf("#m")),
    ]})
facing_state('street_lamp'); item('street_lamp'); loot('street_lamp')
for i, name in enumerate(['green', 'yellow', 'red']):
    model(f'traffic_light_{i}', {"parent": "minecraft:block/block", "textures": {"particle": tex('traffic_housing'), "h": tex('traffic_housing'), "f": tex('traffic_face_' + name)},
        "elements": [
            el([5, 0, 7], [11, 16, 11], {"north": "#f", "south": "#h", "east": "#h", "west": "#h", "up": "#h", "down": "#h"}),
            el([4.5, 14.5, 5], [11.5, 15, 7], allf("#h")),
            el([4.5, 9.5, 5], [11.5, 10, 7], allf("#h")),
            el([4.5, 4.5, 5], [11.5, 5, 7], allf("#h")),
            el([3, 0, 7.5], [5, 16, 10.5], allf("#h")),
            el([11, 0, 7.5], [13, 16, 10.5], allf("#h")),
        ]})
facing_state('traffic_light', extra={f"phase={i}": f"traffic_light_{i}" for i in range(3)}); item('traffic_light', 'traffic_light_0'); loot('traffic_light')
model('traffic_cone', {"parent": "minecraft:block/block", "textures": {"particle": tex('cone'), "c": tex('cone'), "b": tex('cone_base')},
    "elements": [el([2, 0, 2], [14, 1.5, 14], allf("#b")), el([4.5, 1.5, 4.5], [11.5, 6, 11.5], allf("#c")),
                 el([5.5, 6, 5.5], [10.5, 10, 10.5], allf("#c")), el([6.5, 10, 6.5], [9.5, 13, 9.5], allf("#c"))]})
facing_state('traffic_cone'); item('traffic_cone'); loot('traffic_cone')
model('road_barrier', {"parent": "minecraft:block/block", "textures": {"particle": tex('barrier'), "b": tex('barrier'), "c": tex('cone')},
    "elements": [el([0, 0, 3], [16, 5, 13], allf("#b")), el([0, 5, 5], [16, 16, 11], allf("#b")),
                 el([6, 11, 4.6], [10, 13, 5], {"north": "#c", "south": "#c"})]})
facing_state('road_barrier'); item('road_barrier'); loot('road_barrier')
model('roadblock', {"parent": "minecraft:block/block", "textures": {"particle": tex('roadblock'), "s": tex('roadblock'), "l": tex('roadblock_leg')},
    "elements": [el([1, 0, 5], [3, 14, 7], allf("#l")), el([1, 0, 9], [3, 14, 11], allf("#l")),
                 el([13, 0, 5], [15, 14, 7], allf("#l")), el([13, 0, 9], [15, 14, 11], allf("#l")),
                 el([0, 10, 7], [16, 13.5, 9], allf("#s")), el([0, 4, 7], [16, 7.5, 9], allf("#s"))]})
facing_state('roadblock'); item('roadblock'); loot('roadblock')
model('fire_hydrant', {"parent": "minecraft:block/block", "textures": {"particle": tex('hydrant'), "h": tex('hydrant')},
    "elements": [el([4, 0, 4], [12, 2, 12], allf("#h")), el([5, 2, 5], [11, 11, 11], allf("#h")),
                 el([6, 11, 6], [10, 13, 10], allf("#h")), el([7, 13, 7], [9, 14, 9], allf("#h")),
                 el([3, 6, 7], [5, 8, 9], allf("#h")), el([11, 6, 7], [13, 8, 9], allf("#h")),
                 el([6.5, 5, 3], [9.5, 8, 5], allf("#h"))]})
facing_state('fire_hydrant'); item('fire_hydrant'); loot('fire_hydrant')
model('trash_bin', {"parent": "minecraft:block/block", "textures": {"particle": tex('trash_bin'), "b": tex('trash_bin'), "t": tex('trash_bin_top')},
    "elements": [el([3, 0, 3], [13, 13, 13], {"north": "#b", "south": "#b", "east": "#b", "west": "#b", "down": "#b", "up": "#t"}),
                 el([2.5, 13, 2.5], [13.5, 14, 13.5], {"north": "#b", "south": "#b", "east": "#b", "west": "#b", "down": "#b", "up": "#t"})]})
facing_state('trash_bin'); item('trash_bin'); loot('trash_bin')

model('bench', {"parent": "minecraft:block/block", "textures": {"particle": tex('bench_wood'), "w": tex('bench_wood'), "m": M},
    "elements": [el([0, 5, 2], [16, 7, 12], allf("#w")), el([0, 8, 11], [16, 15, 12.5], allf("#w")),
                 el([1, 0, 3], [3, 5, 5], allf("#m")), el([1, 0, 9], [3, 8, 11], allf("#m")),
                 el([13, 0, 3], [15, 5, 5], allf("#m")), el([13, 0, 9], [15, 8, 11], allf("#m"))]})
facing_state('bench'); item('bench'); loot('bench')
model('bus_stop_sign', {"parent": "minecraft:block/block", "textures": {"particle": M, "m": M, "s": tex('bus_sign')},
    "elements": [el([7, 0, 7], [9, 16, 9], allf("#m")), el([2, 9, 7.5], [14, 16, 8.5], {"north": "#s", "south": "#s", "east": "#m", "west": "#m", "up": "#m", "down": "#m"})]})
facing_state('bus_stop_sign'); item('bus_stop_sign'); loot('bus_stop_sign')

names = ['bench', 'bus_stop_sign', 'asphalt', 'asphalt_white_line', 'asphalt_yellow_line', 'asphalt_double_yellow_line', 'asphalt_crosswalk', 'manhole_cover', 'storm_drain',
         'sidewalk', 'sidewalk_slab', 'curb', 'lamp_post', 'street_lamp', 'traffic_light', 'traffic_cone', 'road_barrier', 'roadblock', 'fire_hydrant', 'trash_bin']
wj(os.path.join(RES, 'data/minecraft/tags/blocks/mineable/pickaxe.json'), {"replace": False, "values": ["vehicleforge:" + n for n in names] + ["vehicleforge:rv_door"]})
wj(os.path.join(RES, 'data/minecraft/tags/blocks/slabs.json'), {"replace": False, "values": ["vehicleforge:sidewalk_slab", "vehicleforge:curb"]})
print('city blocks ok', len(T), 'textures')
