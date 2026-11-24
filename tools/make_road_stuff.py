"""Upgrade and trailer item icons; fuel pump, railroad crossing, parking barrier and garage door blocks."""
import json, os, sys, random
from PIL import Image, ImageDraw
R = sys.argv[1]
A = os.path.join(R, 'src/main/resources/assets/vehicleforge'); D = os.path.join(R, 'src/main/resources/data/vehicleforge')
for d in ('textures/item', 'textures/block', 'models/item', 'models/block', 'blockstates'): os.makedirs(f'{A}/{d}', exist_ok=True)
for d in ('loot_tables/blocks', 'recipes'): os.makedirs(f'{D}/{d}', exist_ok=True)
rr = random.Random(9)
def cl(v): return max(0, min(255, int(v)))
def icon(name, draw):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im); draw(d)
    px = im.load(); o = im.copy(); op = o.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + a < 16 and 0 <= y + b < 16 and px[x + a, y + b][3] for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))): op[x, y] = (30, 30, 34, 255)
    o.save(f'{A}/textures/item/{name}.png')
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'vehicleforge:item/{name}'}}, open(f'{A}/models/item/{name}.json', 'w'))
G, DG, Y, RD, W, BK, WD = (150, 154, 160, 255), (100, 104, 110, 255), (240, 200, 40, 255), (200, 50, 40, 255), (235, 235, 230, 255), (40, 40, 44, 255), (150, 104, 60, 255)
icon('armor_plating', lambda d: (d.rectangle([2, 3, 13, 12], fill=G), [d.point((x, y), fill=DG) for x in (3, 12) for y in (4, 11)]))
icon('spiked_bumper', lambda d: (d.rectangle([1, 8, 14, 11], fill=G), [d.polygon([(x, 8), (x + 1, 3), (x + 2, 8)], fill=W) for x in (2, 6, 10)]))
icon('window_bars', lambda d: (d.rectangle([2, 2, 13, 13], outline=BK), [d.line([(x, 2), (x, 13)], fill=BK) for x in (5, 8, 11)]))
icon('roof_rack', lambda d: (d.rectangle([1, 9, 14, 10], fill=BK), d.rectangle([3, 4, 9, 8], fill=WD), d.rectangle([10, 5, 13, 8], fill=RD)))
icon('snow_plow', lambda d: (d.polygon([(1, 4), (14, 2), (14, 12), (1, 13)], fill=Y), d.line([(1, 13), (14, 12)], fill=G, width=2)))
def trailer(col, extra):
    def f(d):
        d.rectangle([2, 4, 12, 11], fill=col); d.line([(12, 10), (15, 10)], fill=BK); d.ellipse([5, 10, 9, 14], fill=BK); extra(d)
    return f
icon('cargo_trailer', trailer((90, 110, 140, 255), lambda d: d.rectangle([3, 5, 11, 6], fill=(60, 80, 110, 255))))
icon('camper_trailer', trailer(W, lambda d: (d.rectangle([4, 6, 6, 8], fill=(120, 170, 210, 255)), d.rectangle([8, 6, 10, 8], fill=(120, 170, 210, 255)), d.line([(2, 9), (12, 9)], fill=(60, 120, 160, 255)))))
icon('boat_trailer', lambda d: (d.line([(1, 11), (14, 11)], fill=G, width=2), d.polygon([(2, 6), (13, 6), (11, 10), (3, 10)], fill=W), d.line([(2, 6), (13, 6)], fill=(40, 90, 160, 255)), d.ellipse([5, 10, 9, 14], fill=BK)))

def tex(name, f):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16): px[x, y] = f(x, y)
    im.save(f'{A}/textures/block/{name}.png')
def j(c, a=8): return tuple(cl(v + rr.uniform(-a, a)) for v in c) + (255,)
tex('fuel_pump', lambda x, y: (30, 30, 30, 255) if 4 <= x <= 11 and 3 <= y <= 6 else j((70, 200, 90), 4) if 5 <= x <= 10 and y == 4 else j((200, 40, 36)) if y > 8 or y < 2 else j((230, 230, 226)))
tex('crossing', lambda x, y: (250, 250, 250, 255) if abs(x - y) <= 1 or abs(x - (15 - y)) <= 1 else (20, 20, 20, 255))
tex('crossing_light', lambda x, y: (230, 30, 20, 255) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 30 else (25, 25, 25, 255))
tex('crossing_dark', lambda x, y: (70, 20, 18, 255) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 30 else (25, 25, 25, 255))
tex('barrier_arm', lambda x, y: (220, 40, 36, 255) if (x // 4) % 2 else (240, 240, 236, 255))
tex('roller_door', lambda x, y: j((200, 200, 196), 4) if y % 4 else j((150, 150, 148), 4))
tex('road_metal', lambda x, y: j((120, 122, 128), 8))

T = lambda **k: {**k}
def el(f, t, tx, rot=None):
    e = {'from': f, 'to': t, 'faces': {d: {'texture': tx} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}
    if rot: e['rotation'] = rot
    return e
def model(name, els, textures):
    json.dump({'parent': 'block/block', 'textures': {**textures, 'particle': list(textures.values())[0]}, 'elements': els}, open(f'{A}/models/block/{name}.json', 'w'), indent=1)
model('fuel_pump', [el([3, 0, 3], [13, 22, 13], '#p'), el([2, 22, 2], [14, 24, 14], '#m'), el([13, 8, 6], [15, 16, 10], '#m')],
      {'p': 'vehicleforge:block/fuel_pump', 'm': 'vehicleforge:block/road_metal'})
for ph in range(3):
    l = 'vehicleforge:block/crossing_light' if ph == 1 else 'vehicleforge:block/crossing_dark'
    r = 'vehicleforge:block/crossing_light' if ph == 2 else 'vehicleforge:block/crossing_dark'
    arm = el([8, 9, 7], [32, 11, 9], '#a') if ph else el([7, 11, 7], [9, 32, 9], '#a')
    model(f'railroad_crossing_{ph}', [el([7, 0, 7], [9, 28, 9], '#m'), el([1, 24, 7.5], [15, 27, 8.5], '#x', {'angle': 45, 'axis': 'z', 'origin': [8, 25.5, 8]}),
                                      el([1, 24, 7.5], [15, 27, 8.5], '#x', {'angle': -45, 'axis': 'z', 'origin': [8, 25.5, 8]}),
                                      el([2, 17, 6], [6, 21, 7], '#l'), el([10, 17, 6], [14, 21, 7], '#r'), el([6, 18, 6.5], [10, 19, 7], '#m'), arm],
          {'m': 'vehicleforge:block/road_metal', 'x': 'vehicleforge:block/crossing', 'l': l, 'r': r, 'a': 'vehicleforge:block/barrier_arm'})
model('parking_barrier_closed', [el([4, 0, 4], [12, 12, 12], '#m'), el([10, 9, 7], [32, 11, 9], '#a')], {'m': 'vehicleforge:block/road_metal', 'a': 'vehicleforge:block/barrier_arm'})
model('parking_barrier_open', [el([4, 0, 4], [12, 12, 12], '#m'), el([7, 11, 7], [9, 32, 9], '#a')], {'m': 'vehicleforge:block/road_metal', 'a': 'vehicleforge:block/barrier_arm'})
model('roller_door_closed', [el([0, 0, 0], [16, 16, 3], '#d')], {'d': 'vehicleforge:block/roller_door'})
model('roller_door_open', [el([0, 13, 0], [16, 16, 16], '#d')], {'d': 'vehicleforge:block/roller_door'})
ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
def rv(m, y): return {'model': f'vehicleforge:block/{m}', **({'y': y} if y else {})}
json.dump({'variants': {f'facing={f}': rv('fuel_pump', y) for f, y in ROT.items()}}, open(f'{A}/blockstates/fuel_pump.json', 'w'), indent=1)
json.dump({'variants': {f'facing={f},phase={p}': rv(f'railroad_crossing_{p}', y) for f, y in ROT.items() for p in range(3)}}, open(f'{A}/blockstates/railroad_crossing.json', 'w'), indent=1)
json.dump({'variants': {f'facing={f},open={o}': rv('parking_barrier_' + ('open' if o == 'true' else 'closed'), y) for f, y in ROT.items() for o in ('true', 'false')}}, open(f'{A}/blockstates/parking_barrier.json', 'w'), indent=1)
json.dump({'variants': {f'facing={f},open={o}': rv('roller_door_' + ('open' if o == 'true' else 'closed'), y) for f, y in ROT.items() for o in ('true', 'false')}}, open(f'{A}/blockstates/roller_door.json', 'w'), indent=1)
for n, m in (('fuel_pump', 'fuel_pump'), ('railroad_crossing', 'railroad_crossing_1'), ('parking_barrier', 'parking_barrier_closed'), ('roller_door', 'roller_door_closed')):
    json.dump({'parent': f'vehicleforge:block/{m}'}, open(f'{A}/models/item/{n}.json', 'w'))
    json.dump({'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'vehicleforge:{n}'}]}]}, open(f'{D}/loot_tables/blocks/{n}.json', 'w'))
def sh(name, pattern, key, n=1):
    json.dump({'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': v}) for k, v in key.items()},
               'result': {'item': f'vehicleforge:{name}', 'count': n}}, open(f'{D}/recipes/{name}.json', 'w'), indent=1)
sh('armor_plating', ['III', 'III'], {'I': 'minecraft:iron_ingot'})
sh('spiked_bumper', ['N N', 'III'], {'N': 'minecraft:iron_nugget', 'I': 'minecraft:iron_ingot'})
sh('window_bars', ['BBB', 'BBB'], {'B': 'minecraft:iron_bars'})
sh('roof_rack', ['I I', 'III'], {'I': 'minecraft:iron_ingot'})
sh('snow_plow', ['  I', ' YI', 'III'], {'I': 'minecraft:iron_ingot', 'Y': 'minecraft:yellow_dye'})
sh('cargo_trailer', ['P P', 'PCP', 'IBI'], {'P': '#minecraft:planks', 'C': 'minecraft:chest', 'I': 'minecraft:iron_ingot', 'B': 'minecraft:iron_block'})
sh('camper_trailer', ['WGW', 'WBW', 'IXI'], {'W': 'minecraft:white_wool', 'G': 'minecraft:glass_pane', 'B': '#minecraft:beds', 'I': 'minecraft:iron_ingot', 'X': 'minecraft:iron_block'})
sh('boat_trailer', ['BBB', 'IXI'], {'B': '#minecraft:boats', 'I': 'minecraft:iron_ingot', 'X': 'minecraft:iron_block'})
sh('fuel_pump', ['IGI', 'RBR', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass_pane', 'R': 'minecraft:red_concrete', 'B': 'minecraft:bucket'})
sh('railroad_crossing', ['RIR', ' I ', ' I '], {'R': 'minecraft:redstone_lamp', 'I': 'minecraft:iron_ingot'})
sh('parking_barrier', ['WRW', ' I ', ' I '], {'W': 'minecraft:white_concrete', 'R': 'minecraft:red_concrete', 'I': 'minecraft:iron_ingot'})
sh('roller_door', ['II', 'II', 'II'], {'I': 'minecraft:iron_ingot'}, 3)
LP = f'{A}/lang/en_us.json'; lang = json.load(open(LP))
lang.update({
 'item.vehicleforge.armor_plating': 'Armor Plating', 'item.vehicleforge.armor_plating.tip': 'Right-click a vehicle to fit. It takes 40% less damage.',
 'item.vehicleforge.spiked_bumper': 'Spiked Bumper', 'item.vehicleforge.spiked_bumper.tip': 'Right-click a vehicle to fit. Running things over hits twice as hard.',
 'item.vehicleforge.window_bars': 'Window Bars', 'item.vehicleforge.window_bars.tip': 'Right-click a vehicle to fit. Zombies do half damage to it.',
 'item.vehicleforge.roof_rack': 'Roof Rack', 'item.vehicleforge.roof_rack.tip': 'Right-click a vehicle to fit. Two more rows of storage.',
 'item.vehicleforge.snow_plow': 'Snow Plow', 'item.vehicleforge.snow_plow.tip': 'Right-click a vehicle to fit. Shoves everything aside much harder.',
 'item.vehicleforge.cargo_trailer': 'Cargo Trailer', 'item.vehicleforge.cargo_trailer.tip': 'Right-click a road vehicle to hitch: three more rows of storage. Use it again to unhitch.',
 'item.vehicleforge.camper_trailer': 'Camper Trailer', 'item.vehicleforge.camper_trailer.tip': 'Right-click a road vehicle to hitch. Sneak + right-click the vehicle to go inside.',
 'item.vehicleforge.boat_trailer': 'Boat Trailer', 'item.vehicleforge.boat_trailer.tip': 'Right-click a road vehicle to hitch. Takes a boat anywhere.',
 'block.vehicleforge.fuel_pump': 'Fuel Pump', 'block.vehicleforge.railroad_crossing': 'Railroad Crossing',
 'block.vehicleforge.parking_barrier': 'Parking Barrier', 'block.vehicleforge.roller_door': 'Roller Door',
 'msg.vehicleforge.pump_dry': 'This pump has run dry.', 'msg.vehicleforge.pump_left': 'Filled a fuel can. %s litres left in this pump.',
})
json.dump(lang, open(LP, 'w'), indent=2, ensure_ascii=False)
print('ok')
