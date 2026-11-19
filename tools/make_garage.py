"""Garage door textures, models, blockstate and item model; garage loot table; extra lang lines."""
import json, os, sys, random
from PIL import Image
R = sys.argv[1]; A = os.path.join(R, 'assets/vehicleforge'); D = os.path.join(R, 'data/vehicleforge')
def wj(p, o): os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=2)
rnd = random.Random(3)
def slats(window=False, handle=False):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            v = 226 + rnd.randint(-4, 4)
            if y % 4 == 3: v -= 38           # shadow under each slat
            elif y % 4 == 0: v += 12          # highlight on top of each slat
            px[x, y] = (v, v, v - 4, 255)
    if window:
        for x0 in (1, 9):
            for y in range(5, 11):
                for x in range(x0, x0 + 6):
                    edge = y in (5, 10) or x in (x0, x0 + 5)
                    px[x, y] = (150, 150, 146, 255) if edge else (70, 90, 110, 255)
    if handle:
        for x in range(5, 11): px[x, 11] = (60, 60, 64, 255); px[x, 12] = (90, 90, 94, 255)
    return im
T = os.path.join(A, 'textures/block'); os.makedirs(T, exist_ok=True)
slats().save(f'{T}/garage_door.png'); slats(window=True).save(f'{T}/garage_door_window.png'); slats(handle=True).save(f'{T}/garage_door_handle.png')
def tex(n): return 'vehicleforge:block/' + n
def panel(t):
    return {"parent": "minecraft:block/block", "textures": {"particle": tex('garage_door'), "f": tex(t), "e": tex('garage_door')},
            "elements": [{"from": [0, 0, 7], "to": [16, 16, 9], "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#f"}, "south": {"uv": [0, 0, 16, 16], "texture": "#f"},
                "up": {"uv": [0, 7, 16, 9], "texture": "#e"}, "down": {"uv": [0, 7, 16, 9], "texture": "#e"},
                "east": {"uv": [7, 0, 9, 16], "texture": "#e"}, "west": {"uv": [7, 0, 9, 16], "texture": "#e"}}}]}
roll = {"parent": "minecraft:block/block", "textures": {"particle": tex('garage_door'), "f": tex('garage_door')},
        "elements": [{"from": [0, 12, 0], "to": [16, 16, 16], "faces": {d: {"texture": "#f"} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
empty = {"textures": {"particle": tex('garage_door')}, "elements": []}
M = os.path.join(A, 'models/block')
wj(f'{M}/garage_door.json', panel('garage_door')); wj(f'{M}/garage_door_window.json', panel('garage_door_window'))
wj(f'{M}/garage_door_handle.json', panel('garage_door_handle')); wj(f'{M}/garage_door_roll.json', roll); wj(f'{M}/garage_door_open.json', empty)
v = {}
for axis, y in (('x', 0), ('z', 90)):
    for part in range(9):
        for op in ('false', 'true'):
            if op == 'true': m = 'garage_door_roll' if part >= 6 else 'garage_door_open'
            else: m = 'garage_door_window' if part >= 6 else 'garage_door_handle' if part == 1 else 'garage_door'
            e = {"model": "vehicleforge:block/" + m}
            if y: e["y"] = y
            v[f"axis={axis},open={op},part={part}"] = e
wj(os.path.join(A, 'blockstates/garage_door.json'), {"variants": v})
wj(os.path.join(A, 'models/item/garage_door.json'), {"parent": "vehicleforge:block/garage_door_handle"})
# garage cupboard loot (Outbreak adds its own tools on top)
def entry(item, w, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": item, "weight": w}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e
wj(os.path.join(D, 'loot_tables/city/garage.json'), {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "entries": [
    {"type": "minecraft:empty", "weight": 6}, entry("vehicleforge:fuel_can", 6, 1, 2), entry("minecraft:iron_ingot", 5, 1, 4), entry("minecraft:iron_nugget", 5, 3, 9),
    entry("minecraft:bucket", 3), entry("minecraft:iron_shovel", 2), entry("minecraft:iron_axe", 1), entry("minecraft:shears", 2), entry("minecraft:string", 4, 2, 6),
    entry("minecraft:lead", 2), entry("minecraft:redstone", 2, 2, 6), entry("minecraft:torch", 4, 2, 8), entry("minecraft:oak_planks", 4, 4, 12)]}]})
LP = os.path.join(A, 'lang/en_us.json'); lang = json.load(open(LP))
lang.update({"block.vehicleforge.garage_door": "Garage Door (3x3)",
             "msg.vehicleforge.wrecked": "It's a burnt-out wreck. Fix it up with iron ingots (or search it).",
             "msg.vehicleforge.wreck_fixed": "Good as new... well, it runs. Fill it up with fuel."})
json.dump(lang, open(LP, 'w'), indent=2)
print('garage assets ok')
