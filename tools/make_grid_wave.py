"""Power & water outages and traffic: unlit lamp and dead signal models, the kitchen sink, and the messages.

Usage: python3 make_grid_wave.py <resources dir>
"""
import os, sys, json
from PIL import Image

RES = sys.argv[1]
A = os.path.join(RES, 'assets/vehicleforge'); D = os.path.join(RES, 'data/vehicleforge')
TB = os.path.join(A, 'textures/block')


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2)


def rj(path): return json.load(open(path))


def darken(src, dst, k=0.28, grey=0.6):
    im = Image.open(os.path.join(TB, src + '.png')).convert('RGBA'); px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            m = (r + g + b) / 3
            r, g, b = [int((c * (1 - grey) + m * grey) * k + 18) for c in (r, g, b)]
            px[x, y] = (min(255, r), min(255, g), min(255, b), a)
    im.save(os.path.join(TB, dst + '.png'))


# unlit street lamp: the same lamp with a dead grey lens
darken('lamp_light', 'lamp_light_off')
m = rj(os.path.join(A, 'models/block/street_lamp.json')); m['textures']['l'] = 'vehicleforge:block/lamp_light_off'
wj(os.path.join(A, 'models/block/street_lamp_off.json'), m)
v = {}
for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    for lit in ('true', 'false'):
        e = {"model": "vehicleforge:block/street_lamp" + ("" if lit == 'true' else "_off")}
        if y: e["y"] = y
        v[f"facing={f},lit={lit}"] = e
wj(os.path.join(A, 'blockstates/street_lamp.json'), {"variants": v})

# dead traffic signal: all three lamps dark
darken('traffic_face_red', 'traffic_face_off', 0.22, 0.9)
m = rj(os.path.join(A, 'models/block/traffic_light_2.json')); m['textures']['f'] = 'vehicleforge:block/traffic_face_off'
wj(os.path.join(A, 'models/block/traffic_light_off.json'), m)
old = rj(os.path.join(A, 'blockstates/traffic_light.json'))['variants']
v = {}
for k, e in old.items():
    if 'powered=' in k: continue
    v[k + ',powered=true'] = e
    off = dict(e); off['model'] = 'vehicleforge:block/traffic_light_off'
    v[k + ',powered=false'] = off
wj(os.path.join(A, 'blockstates/traffic_light.json'), {"variants": v})

# neon: lit doesn't change the (empty) model
wj(os.path.join(A, 'blockstates/neon_sign.json'), {"variants": {f"facing={f}": {"model": "vehicleforge:block/neon_sign"} for f in ('north', 'south', 'east', 'west')}})

# kitchen sink: a counter cabinet with a steel basin and a tap
st = Image.new('RGBA', (16, 16)); px = st.load()
for y in range(16):
    for x in range(16):
        c = 176 + ((x * 7 + y * 3) % 5) * 4 - (14 if x in (0, 15) or y in (0, 15) else 0)
        px[x, y] = (c, c + 3, c + 7, 255)
st.save(os.path.join(TB, 'steel.png'))
T = {"f": "vehicleforge:block/cabinet_wood_front", "s": "vehicleforge:block/cabinet_side", "t": "vehicleforge:block/countertop",
     "m": "vehicleforge:block/steel", "particle": "vehicleforge:block/countertop"}


def box(frm, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    def uv(f):
        x1, y1, z1 = frm; x2, y2, z2 = to
        return {'down': [x1, 16 - z2, x2, 16 - z1], 'up': [x1, z1, x2, z2], 'north': [16 - x2, 16 - y2, 16 - x1, 16 - y1],
                'south': [x1, 16 - y2, x2, 16 - y1], 'west': [z1, 16 - y2, z2, 16 - y1], 'east': [16 - z2, 16 - y2, 16 - z1, 16 - y1]}[f]
    t = tex if isinstance(tex, dict) else {f: tex for f in faces}
    return {"from": frm, "to": to, "faces": {f: {"texture": t[f], "uv": [max(0, min(16, c)) for c in uv(f)]} for f in t}}


els = [box([0, 0, 1], [16, 12, 16], {"north": "#f", "south": "#s", "east": "#s", "west": "#s", "down": "#s"}),
       box([0, 12, 1], [16, 15, 4], "#t"), box([0, 12, 13], [16, 15, 16], "#t"), box([0, 12, 4], [3, 15, 13], "#t"), box([13, 12, 4], [16, 15, 13], "#t"),
       box([3, 9, 4], [13, 12, 13], {"up": "#m", "north": "#m", "south": "#m", "east": "#m", "west": "#m"}),     # basin
       box([3, 9, 4], [13, 9.01, 13], {"up": "#m"}),
       box([7.25, 15, 13.5], [8.75, 19, 15], "#m"), box([7.25, 17.5, 9.5], [8.75, 19, 13.5], "#m"),       # tap
       box([4.5, 15, 13.5], [6, 16, 15], "#m"), box([10, 15, 13.5], [11.5, 16, 15], "#m")]
wj(os.path.join(A, 'models/block/sink.json'), {"parent": "minecraft:block/block", "textures": T, "elements": els})
v = {}
for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
    e = {"model": "vehicleforge:block/sink"}
    if y: e["y"] = y
    v[f"facing={f}"] = e
# InteractiveProp has a STOCK property too; partial keys match every value
wj(os.path.join(A, 'blockstates/sink.json'), {"variants": v})
wj(os.path.join(A, 'models/item/sink.json'), {"parent": "vehicleforge:block/sink"})
wj(os.path.join(D, 'loot_tables/blocks/sink.json'), {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "vehicleforge:sink"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(os.path.join(D, 'recipes/sink.json'), {"type": "minecraft:crafting_shaped", "pattern": ["I I", "IBI", "PPP"],
                                            "key": {"I": {"item": "minecraft:iron_ingot"}, "B": {"item": "minecraft:bucket"}, "P": {"tag": "minecraft:planks"}},
                                            "result": {"item": "vehicleforge:sink"}})
pk = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/pickaxe.json'); t = rj(pk)
if 'vehicleforge:sink' not in t['values']: t['values'].append('vehicleforge:sink')
json.dump(t, open(pk, 'w'), indent=2)

LP = os.path.join(A, 'lang/en_us.json'); lang = rj(LP)
lang.update({
    'block.vehicleforge.sink': 'Kitchen Sink',
    'msg.vehicleforge.taps_dry': 'The taps are dry: the water has been shut off',
    'msg.vehicleforge.elevator_dead': 'The elevator has no power',
    'msg.vehicleforge.pump_no_power': 'The pump has no power: hook up a generator',
    'msg.vehicleforge.power_out': 'The lights flicker and die. The power grid is down.',
    'msg.vehicleforge.power_back': 'The power is back on.',
    'msg.vehicleforge.water_out': 'The pipes shudder and go quiet. The water has been shut off.',
    'msg.vehicleforge.water_back': 'Water is running again.',
    'msg.vehicleforge.grid_status': 'Day %s. Power: %s%s. Water: %s%s.',
    'msg.vehicleforge.grid_on': 'on', 'msg.vehicleforge.grid_off': 'off',
    'msg.vehicleforge.grid_until': ' (fails on day %s)',
    'msg.vehicleforge.grid_scheduled': 'Power fails on day %s, water on day %s',
    'gamerule.cityTraffic': 'City traffic',
    'broadcast.vehicleforge.title': '[EMERGENCY BROADCAST]',
    'broadcast.vehicleforge.frame': 'Day %s. %s %s',
    'broadcast.vehicleforge.day0': 'Reports of violent attacks across the city. Residents are urged to stay indoors and lock their doors.',
    'broadcast.vehicleforge.day1': 'A quarantine zone has been declared. Roads out of the city are closed. Do not approach the infected.',
    'broadcast.vehicleforge.day2': 'Hospitals are no longer accepting patients. Shelter in place and ration food and water.',
    'broadcast.vehicleforge.day3': 'Emergency services are overwhelmed. Military evacuation points have been suspended.',
    'broadcast.vehicleforge.day4': 'This is an automated message. Stay indoors. Do not travel. Help is not coming soon.',
    'broadcast.vehicleforge.day5': 'This is an automated message. Stay indoors. Conserve food and water.',
    'broadcast.vehicleforge.water_soon': 'Water service will be suspended within %s days: fill every container you can.',
    'broadcast.vehicleforge.water_today': 'Water service is being suspended today.',
    'broadcast.vehicleforge.water_gone': 'Water service has been suspended.',
    'broadcast.vehicleforge.power_soon': 'The power grid is failing: outages are expected within %s days.',
    'broadcast.vehicleforge.power_today': 'Grid operators expect a total power failure tonight.',
    'broadcast.vehicleforge.static': '...kssshhhhhhh... ...',
})
json.dump(lang, open(LP, 'w'), indent=2)
print('grid wave ok')
