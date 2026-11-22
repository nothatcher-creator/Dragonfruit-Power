"""Barricade assets: board and sheet-metal textures, block models and blockstate, the sheet metal item, recipe, lang."""
import json, os, sys, random
from PIL import Image

R = sys.argv[1]
A = os.path.join(R, 'src/main/resources/assets/outbreak')
D = os.path.join(R, 'src/main/resources/data/outbreak')
def mk(p):
    os.makedirs(p, exist_ok=True); return p
BT, IT = mk(f'{A}/textures/block'), mk(f'{A}/textures/item')
BM, IM, BS = mk(f'{A}/models/block'), mk(f'{A}/models/item'), mk(f'{A}/blockstates')
rr = random.Random(7)

def clamp(v): return max(0, min(255, int(v)))

# ---------- textures
board = Image.new('RGBA', (16, 16))
base = (178, 146, 104)
for y in range(16):
    row = (y % 4)
    for x in range(16):
        g = 1.0 + 0.06 * ((x * 7 + y * 3) % 5 - 2) / 2 + rr.uniform(-0.05, 0.05)
        if row == 0: g *= 0.72                    # gap shadow between grain bands
        if rr.random() < 0.05: g *= 0.8           # knots and grime
        board.putpixel((x, y), tuple(clamp(c * g) for c in base) + (255,))
for (x, y) in ((1, 2), (14, 6), (2, 10), (13, 14)):  # nail heads
    board.putpixel((x, y), (70, 70, 74, 255)); board.putpixel((x, y - 1 if y else y), (110, 110, 116, 255))
for x in range(14, 16):
    for y in range(14, 16): board.putpixel((x, y), (60, 60, 64, 255))
board.save(f'{BT}/barricade_board.png')

metal = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        ridge = 1.12 if x % 4 == 0 else 0.86 if x % 4 == 2 else 1.0
        v = 150 * ridge + rr.uniform(-8, 8)
        c = [v, v + 4, v + 10]
        if rr.random() < 0.08 or (y > 11 and rr.random() < 0.25):     # rust, heavier at the bottom
            c = [v * 0.95 + 30, v * 0.6, v * 0.35]
        metal.putpixel((x, y), tuple(clamp(t) for t in c) + (255,))
for x in (1, 7, 14):
    for y in (1, 14): metal.putpixel((x, y), (90, 92, 98, 255))
metal.save(f'{BT}/barricade_metal.png')

item = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y in range(3, 13):
    for x in range(1, 15):
        sh = x - (y - 3) // 3
        if sh < 1 or sh > 13: continue
        ridge = 1.15 if sh % 3 == 0 else 0.85
        v = clamp(150 * ridge)
        col = (v, v + 4, clamp(v + 12), 255)
        if rr.random() < 0.08: col = (150, 90, 50, 255)
        item.putpixel((x, y), col)
px = item.load()
out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in range(16):
        if px[x, y][3]: out.putpixel((x, y), px[x, y])
        elif any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            out.putpixel((x, y), (34, 36, 40, 255))
out.save(f'{IT}/sheet_metal.png')

# ---------- models (drawn for facing north: the boards sit on the north face, just outside the block)
TEX = {'board': 'outbreak:block/barricade_board', 'metal': 'outbreak:block/barricade_metal', 'particle': 'outbreak:block/barricade_board'}
LAYER = {'A': (-1.5, 0.0), 'B': (-3.0, -1.5)}

def board_el(y0, angle, layer, row):
    z0, z1 = LAYER[layer]
    v0 = row * 4
    rot = {'origin': [8, y0 + 1.5, (z0 + z1) / 2], 'axis': 'z', 'angle': angle}
    faces = {
        'north': {'uv': [0, v0, 16, v0 + 3], 'texture': '#board'},
        'south': {'uv': [16, v0, 0, v0 + 3], 'texture': '#board'},
        'up': {'uv': [0, v0, 16, v0 + 1.5], 'texture': '#board'},
        'down': {'uv': [0, v0 + 1.5, 16, v0 + 3], 'texture': '#board'},
        'east': {'uv': [0, v0, 1.5, v0 + 3], 'texture': '#board'},
        'west': {'uv': [0, v0, 1.5, v0 + 3], 'texture': '#board'},
    }
    els = [{'from': [-1, y0, z0], 'to': [17, y0 + 3, z1], 'rotation': rot, 'faces': faces}]
    for nx in (0.5, 14.5):
        nf = {f: {'uv': [14, 14, 16, 16], 'texture': '#board'} for f in ('north', 'up', 'down', 'east', 'west')}
        els.append({'from': [nx, y0 + 1, z0 - 0.4], 'to': [nx + 1, y0 + 2, z0], 'rotation': rot, 'faces': nf})
    return els

# board slots: (y, angle, layer, texture row). Single blocks take four; a door spreads them over both halves.
SINGLE = [(9, 22.5, 'A', 0), (4, -22.5, 'B', 1), (13, -22.5, 'B', 2), (0, 22.5, 'A', 3)]
LOWER = [(9, 22.5, 'A', 0), (2, -22.5, 'B', 2)]
UPPER = [(4, -22.5, 'B', 1), (11, 22.5, 'A', 3)]

def model(name, slots):
    els = []
    for s in slots: els += board_el(*s)
    json.dump({'ambientocclusion': False, 'textures': TEX, 'elements': els}, open(f'{BM}/{name}.json', 'w'), indent=1)

for n in range(0, 5): model(f'barricade_single_{n}', SINGLE[:n])
for n in range(0, 3): model(f'barricade_lower_{n}', LOWER[:n]); model(f'barricade_upper_{n}', UPPER[:n])
plate = {'from': [-1, 0, -1], 'to': [17, 16, 0], 'faces': {
    'north': {'uv': [0, 0, 16, 16], 'texture': '#metal'}, 'south': {'uv': [16, 0, 0, 16], 'texture': '#metal'},
    'up': {'uv': [0, 0, 16, 1], 'texture': '#metal'}, 'down': {'uv': [0, 15, 16, 16], 'texture': '#metal'},
    'east': {'uv': [0, 0, 1, 16], 'texture': '#metal'}, 'west': {'uv': [15, 0, 16, 16], 'texture': '#metal'}}}
json.dump({'ambientocclusion': False, 'textures': TEX, 'elements': [plate]}, open(f'{BM}/barricade_metal.json', 'w'), indent=1)

ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
variants = {}
for f, y in ROT.items():
    for b in range(5):
        for metal in ('false', 'true'):
            for part in ('single', 'lower', 'upper'):
                if metal == 'true': m = 'barricade_metal'
                elif part == 'single': m = f'barricade_single_{b}'
                elif part == 'lower': m = f'barricade_lower_{(b + 1) // 2}'
                else: m = f'barricade_upper_{b // 2}'
                v = {'model': f'outbreak:block/{m}'}
                if y: v['y'] = y
                variants[f'boards={b},facing={f},metal={metal},part={part}'] = v
json.dump({'variants': variants}, open(f'{BS}/barricade.json', 'w'), indent=1)
json.dump({'parent': 'item/generated', 'textures': {'layer0': 'outbreak:item/sheet_metal'}}, open(f'{IM}/sheet_metal.json', 'w'), indent=1)

# ---------- recipe: four bits of scrap hammered flat
json.dump({'type': 'minecraft:crafting_shaped', 'pattern': ['SS', 'SS'], 'key': {'S': {'item': 'outbreak:scrap_metal'}},
           'result': {'item': 'outbreak:sheet_metal', 'count': 1}}, open(f'{D}/recipes/sheet_metal.json', 'w'), indent=1)

# ---------- lang
LP = f'{A}/lang/en_us.json'
lang = json.load(open(LP))
lang.update({
    'block.outbreak.barricade': 'Barricade',
    'item.outbreak.sheet_metal': 'Sheet Metal',
    'action.outbreak.barricade': 'Boarding up',
    'action.outbreak.barricade_metal': 'Putting up sheet metal',
    'action.outbreak.unbarricade': 'Prying it off',
    'action.outbreak.board_up': 'Board up',
    'action.outbreak.board_up_need': 'Board up (needs %s)',
    'action.outbreak.add_plank': 'Add a plank',
    'action.outbreak.add_plank_need': 'Add a plank (needs %s)',
    'action.outbreak.cover_metal': 'Cover with sheet metal',
    'action.outbreak.cover_metal_need': 'Cover with sheet metal (needs %s)',
    'action.outbreak.pry_plank': 'Pry off a plank',
    'action.outbreak.pry_plank_need': 'Pry off a plank (needs a hammer or crowbar)',
    'action.outbreak.pry_metal': 'Pry off the sheet metal',
    'action.outbreak.pry_metal_need': 'Pry off the sheet metal (needs a hammer or crowbar)',
    'need.outbreak.hammer': 'a claw hammer',
    'need.outbreak.planks': 'a plank',
    'need.outbreak.nails': 'nails',
    'need.outbreak.sheet_metal': 'sheet metal',
    'msg.outbreak.need_hammer': 'You need a claw hammer.',
    'msg.outbreak.need_planks': 'You need a plank.',
    'msg.outbreak.need_nails': 'You need nails: 2 for a plank, 4 for sheet metal.',
    'msg.outbreak.need_sheet_metal': 'You need sheet metal.',
    'msg.outbreak.need_pry': 'You need a claw hammer or a crowbar.',
    'msg.outbreak.cant_barricade': "That can't be barricaded.",
    'msg.outbreak.barricade_full': "It's barricaded as far as it'll go.",
    'msg.outbreak.barricade_removed': 'Barricade taken down.',
    'msg.outbreak.barricade_boards': 'Barricade: %s planks (%s%%)',
    'msg.outbreak.barricade_metal': 'Sheet metal barricade (%s%%)',
    'guide.outbreak.9.title': 'Barricades',
    'guide.outbreak.9.body': "Board up doors, windows and gaps in walls: look at one, open the action menu and pick Board up. You need a claw hammer, a plank and 2 nails each time, and up to 4 planks fit. Sneaking and using a claw hammer on a door or window does the same.\nSheet metal (4 scrap metal, or salvaged from cars) takes 4 nails and is far tougher than wood.\nZombies chasing you, or a noise behind a barricade, will beat on it until it gives. Each plank breaks in turn, so listen for the banging and nail on more.\nHammering is loud. Barricade before dark, and pry planks off with a hammer or crowbar to get them back.",
})
json.dump(lang, open(LP, 'w'), indent=2, ensure_ascii=False)
print('barricade assets done')
