"""Wave 7 assets: melee sprites, skill books, feral dog and rat skins, decorations and traps, recipes, loot, lang."""
import json, os, sys, random
from PIL import Image, ImageDraw
R = sys.argv[1]
A = os.path.join(R, 'src/main/resources/assets/outbreak'); D = os.path.join(R, 'src/main/resources/data/outbreak')
IT, BT, ET = f'{A}/textures/item', f'{A}/textures/block', f'{A}/textures/entity'
for d in (IT, BT, ET, f'{A}/models/item', f'{A}/models/block', f'{A}/blockstates', f'{D}/loot_tables/blocks', f'{D}/recipes'): os.makedirs(d, exist_ok=True)
rr = random.Random(77)
def cl(v): return max(0, min(255, int(v)))
def jit(c, a=10): return tuple(cl(x + rr.uniform(-a, a)) for x in c[:3]) + (255,)
OUT = (28, 28, 32, 255)

def outline(im):
    px = im.load(); o = im.copy(); op = o.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                op[x, y] = OUT
    return o

def diag(px, x0, y0, x1, y1, col, w=1):
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        x = x0 + (x1 - x0) * i // max(n, 1); y = y0 + (y1 - y0) * i // max(n, 1)
        for k in range(w):
            if 0 <= x + k < 16 and 0 <= y < 16: px[x + k, y] = jit(col, 8)

def item(name, draw):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load(); draw(px)
    outline(im).save(f'{IT}/{name}.png')
    json.dump({'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'outbreak:item/{name}'}}, open(f'{A}/models/item/{name}.json', 'w'), indent=1)

STEEL, DARK, WOOD, WOODD, BLACK, RED = (196, 200, 206), (90, 94, 100), (150, 104, 60), (100, 68, 40), (40, 40, 44), (170, 40, 36)
def katana(px):
    diag(px, 2, 14, 5, 11, BLACK, 1); px[5, 10] = jit((200, 170, 60)); px[6, 10] = jit((200, 170, 60))
    diag(px, 6, 9, 14, 1, STEEL, 1); diag(px, 7, 9, 14, 2, (230, 232, 236), 1)
def hatchet(px):
    diag(px, 2, 14, 10, 6, WOOD, 1)
    for y in range(2, 8):
        for x in range(9, 9 + (y - 1)):
            if x < 15: px[x, y] = jit(STEEL if x > 9 + (y - 3) else DARK)
def cleaver(px):
    diag(px, 2, 14, 5, 11, BLACK, 2)
    for y in range(2, 10):
        for x in range(6, 14):
            if x - 6 + (9 - y) < 12: px[x, y] = jit((210, 212, 218) if y < 9 else (150, 152, 158))
    px[12, 3] = (0, 0, 0, 0)
def tire_iron(px):
    diag(px, 2, 14, 12, 4, DARK, 1); diag(px, 3, 14, 13, 4, STEEL, 1); diag(px, 11, 3, 14, 6, DARK, 2)
def pick_handle(px): diag(px, 2, 14, 12, 3, WOOD, 2); diag(px, 11, 3, 13, 1, WOODD, 2)
def barbed_bat(px):
    diag(px, 2, 14, 13, 3, WOOD, 2)
    for i in range(6, 13, 2): px[i - 1, 16 - i - 2] = jit((120, 124, 130)); px[i + 1, 16 - i] = jit((120, 124, 130))
    diag(px, 3, 14, 5, 12, BLACK, 1)
def rolling_pin(px): diag(px, 1, 14, 3, 12, WOODD, 1); diag(px, 4, 11, 11, 4, (214, 180, 130), 3); diag(px, 12, 3, 14, 1, WOODD, 1)
def shovel(px):
    diag(px, 1, 15, 9, 7, WOOD, 1)
    for y in range(1, 8):
        for x in range(8, 15):
            if abs((x - 11) + (y - 4)) < 4 and abs((x - 11) - (y - 4)) < 4: px[x, y] = jit(DARK if (x + y) % 5 == 0 else (150, 154, 160))
for n, f in (('katana', katana), ('hatchet', hatchet), ('meat_cleaver', cleaver), ('tire_iron', tire_iron), ('pickaxe_handle', pick_handle),
             ('barbed_bat', barbed_bat), ('rolling_pin', rolling_pin), ('shovel_weapon', shovel)): item(n, f)

# ---- books: a cover colour per skill, white spine bands = volume
SK = {'aiming': (60, 90, 60), 'melee': (140, 40, 36), 'fitness': (40, 110, 160), 'carpentry': (150, 100, 50), 'mechanics': (90, 90, 100),
      'first_aid': (220, 220, 214), 'scavenging': (120, 100, 40)}
for sk, col in SK.items():
    for v in (1, 2, 3):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = im.load()
        for y in range(2, 15):
            for x in range(3, 13): px[x, y] = jit(col, 8)
            px[3, y] = jit(tuple(c * 0.6 for c in col), 4); px[12, y] = (235, 230, 215, 255) if y not in (2, 14) else jit(col)
        for k in range(v): 
            for x in range(4, 12): px[x, 4 + k * 2] = (240, 230, 200, 255)
        mark = (40, 40, 40, 255) if sk == 'first_aid' else (240, 230, 200, 255)
        if sk == 'first_aid':
            for i in range(6, 10): px[i, 10] = (200, 40, 40, 255); px[8, i + 2] = (200, 40, 40, 255)
        else:
            for x in range(6, 10): px[x, 11] = mark
        name = f'book_{sk}_{v}'
        outline(im).save(f'{IT}/{name}.png')
        json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'outbreak:item/{name}'}}, open(f'{A}/models/item/{name}.json', 'w'), indent=1)

# ---- animals: fur everywhere on the UV sheet, eyes on the face
def fur(path, base, eyes):
    im = Image.new('RGBA', (64, 32)); px = im.load()
    for y in range(32):
        for x in range(64):
            g = 1 + rr.uniform(-0.18, 0.12)
            c = tuple(b * g for b in base)
            if rr.random() < 0.06: c = (110, 20, 18)               # blood and sores
            elif rr.random() < 0.05: c = (150, 120, 120)            # bald patches
            px[x, y] = tuple(cl(v) for v in c) + (255,)
    for (x, y) in eyes: px[x, y] = (230, 40, 30, 255)
    im.save(path)
fur(f'{ET}/feral_dog.png', (92, 78, 62), [(5, 6), (8, 6)])
fur(f'{ET}/infected_rat.png', (78, 70, 66), [(1, 2), (3, 2)])

# ---- blocks
def tex(name, f):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16): px[x, y] = f(x, y)
    im.save(f'{BT}/{name}.png')
tex('sandbags', lambda x, y: jit((178, 160, 112), 10) if (y % 4) and ((x + (y // 4) * 4) % 8) else jit((130, 116, 80), 6))
tex('wooden_crate', lambda x, y: jit((120, 84, 48), 6) if x in (0, 15) or y in (0, 15) or x == y or x == 15 - y else jit((170, 126, 76), 10))
tex('oil_drum', lambda x, y: jit((150, 36, 30), 12) if y not in (3, 12) else jit((90, 24, 20), 6))
tex('oil_drum_top', lambda x, y: jit((70, 70, 74), 8) if not (5 <= x <= 7 and 5 <= y <= 7) else (30, 30, 30, 255))
tex('rubble', lambda x, y: jit(rr.choice([(130, 128, 124), (110, 108, 104), (150, 90, 70), (90, 88, 84)]), 8))
tex('mattress', lambda x, y: jit((214, 210, 196), 8) if (x + y) % 5 else jit((170, 160, 140), 6) if rr.random() > 0.1 else (150, 110, 60, 255))
tex('help_sign', lambda x, y: jit((150, 110, 70), 8))
tex('caution_tape', lambda x, y: (230, 200, 40, 255) if ((x + y) // 3) % 2 else (30, 30, 30, 255))
tex('body_bag', lambda x, y: jit((34, 36, 40), 6) if x != 8 else (120, 120, 124, 255))
tex('bear_trap', lambda x, y: jit((90, 92, 96), 12) if (x in (1, 14) or y in (1, 14) or (x + y) % 4 == 0) else (0, 0, 0, 0))
tex('spike_strip', lambda x, y: (190, 192, 198, 255) if x % 3 == 1 and y % 3 == 1 else jit((140, 100, 60), 8))
tex('barbed_wire', lambda x, y: (150, 152, 158, 255) if (x + y) % 8 == 0 or (x - y) % 8 == 0 or (x % 8 == 4 and y % 8 == 4) else (0, 0, 0, 0))
tex('tin_can', lambda x, y: jit((180, 182, 188), 14) if y not in (0, 15) else jit((120, 120, 126), 6))
# HELP painted on the sign
im = Image.open(f'{BT}/help_sign.png'); px = im.load()
for (x, y) in [(2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (3, 7), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9),
               (6, 5), (6, 6), (6, 7), (6, 8), (6, 9), (7, 5), (7, 7), (7, 9), (9, 5), (9, 6), (9, 7), (9, 8), (9, 9), (10, 9),
               (12, 5), (12, 6), (12, 7), (12, 8), (12, 9), (13, 5), (14, 6), (13, 7)]: px[x, y] = (200, 30, 30, 255)
im.save(f'{BT}/help_sign.png')

def faces(t, uv=None):
    return {d: {'texture': t, **({'uv': uv} if uv else {})} for d in ('north', 'south', 'east', 'west', 'up', 'down')}
def model(name, elements, tex_map, ao=True):
    json.dump({'parent': 'block/block', 'ambientocclusion': ao, 'textures': tex_map, 'elements': elements}, open(f'{A}/models/block/{name}.json', 'w'), indent=1)
def el(f, t, tx='#t'): return {'from': f, 'to': t, 'faces': faces(tx)}
model('sandbags', [el([0, 0, 0], [16, 6, 16]), el([1, 6, 1], [15, 12, 15])], {'t': 'outbreak:block/sandbags', 'particle': 'outbreak:block/sandbags'})
model('wooden_crate', [el([1, 0, 1], [15, 14, 15])], {'t': 'outbreak:block/wooden_crate', 'particle': 'outbreak:block/wooden_crate'})
model('oil_drum', [{'from': [2, 0, 2], 'to': [14, 15, 14], 'faces': {**{d: {'texture': '#t'} for d in ('north', 'south', 'east', 'west')}, 'up': {'texture': '#top'}, 'down': {'texture': '#top'}}}],
      {'t': 'outbreak:block/oil_drum', 'top': 'outbreak:block/oil_drum_top', 'particle': 'outbreak:block/oil_drum'})
model('rubble_pile', [el([0, 0, 0], [16, 3, 16]), el([2, 3, 3], [12, 6, 13]), el([5, 6, 5], [9, 8, 10])], {'t': 'outbreak:block/rubble', 'particle': 'outbreak:block/rubble'})
model('mattress', [el([0, 0, 0], [16, 4, 16])], {'t': 'outbreak:block/mattress', 'particle': 'outbreak:block/mattress'})
model('help_sign', [el([0, 0, 14], [16, 16, 16])], {'t': 'outbreak:block/help_sign', 'particle': 'outbreak:block/help_sign'})
model('caution_tape', [el([0, 9, 7.9], [16, 11, 8.1])], {'t': 'outbreak:block/caution_tape', 'particle': 'outbreak:block/caution_tape'}, False)
model('body_bag', [el([2, 0, 0], [14, 3, 16]), el([4, 3, 1], [12, 4, 15])], {'t': 'outbreak:block/body_bag', 'particle': 'outbreak:block/body_bag'})
model('bear_trap', [el([2, 0, 2], [14, 1, 14]), el([2, 1, 2], [14, 3, 3]), el([2, 1, 13], [14, 3, 14])], {'t': 'outbreak:block/bear_trap', 'particle': 'outbreak:block/bear_trap'}, False)
model('bear_trap_sprung', [el([2, 0, 2], [14, 1, 14]), el([2, 1, 7], [14, 5, 9])], {'t': 'outbreak:block/bear_trap', 'particle': 'outbreak:block/bear_trap'}, False)
model('spike_strip', [el([0, 0, 0], [16, 1, 16]), *[el([x, 1, z], [x + 1, 2.5, z + 1]) for x in (1, 5, 9, 13) for z in (1, 5, 9, 13)]],
      {'t': 'outbreak:block/spike_strip', 'particle': 'outbreak:block/spike_strip'})
json.dump({'parent': 'minecraft:block/cross', 'textures': {'cross': 'outbreak:block/barbed_wire'}}, open(f'{A}/models/block/barbed_wire.json', 'w'))
model('tin_can_alarm', [el([0, 7, 7.9], [16, 7.2, 8.1]), *[el([x, 3, 7], [x + 2, 7, 9]) for x in (2, 7, 12)]],
      {'t': 'outbreak:block/tin_can', 'particle': 'outbreak:block/tin_can'}, False)
model('tin_can_alarm_rattled', [el([0, 7, 7.9], [16, 7.2, 8.1]), *[el([x, 2, 6.5], [x + 2, 6, 8.5]) for x in (1, 7, 13)]],
      {'t': 'outbreak:block/tin_can', 'particle': 'outbreak:block/tin_can'}, False)

ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
def facing_state(name):
    json.dump({'variants': {f'facing={f}': {'model': f'outbreak:block/{name}', **({'y': y} if y else {})} for f, y in ROT.items()}}, open(f'{A}/blockstates/{name}.json', 'w'), indent=1)
for n in ('sandbags', 'wooden_crate', 'oil_drum', 'rubble_pile', 'mattress', 'help_sign', 'caution_tape', 'body_bag'): facing_state(n)
json.dump({'variants': {'armed=true': {'model': 'outbreak:block/bear_trap'}, 'armed=false': {'model': 'outbreak:block/bear_trap_sprung'}}}, open(f'{A}/blockstates/bear_trap.json', 'w'), indent=1)
json.dump({'variants': {'': {'model': 'outbreak:block/spike_strip'}}}, open(f'{A}/blockstates/spike_strip.json', 'w'))
json.dump({'variants': {'': {'model': 'outbreak:block/barbed_wire'}}}, open(f'{A}/blockstates/barbed_wire.json', 'w'))
json.dump({'variants': {f'facing={f},rattled={r}': {'model': 'outbreak:block/tin_can_alarm' + ('_rattled' if r == 'true' else ''), **({'y': (y + 90) % 360} if (y + 90) % 360 else {})}
                        for f, y in ROT.items() for r in ('true', 'false')}}, open(f'{A}/blockstates/tin_can_alarm.json', 'w'), indent=1)
BLOCKS = ['sandbags', 'wooden_crate', 'oil_drum', 'rubble_pile', 'mattress', 'help_sign', 'caution_tape', 'body_bag', 'bear_trap', 'spike_strip', 'barbed_wire', 'tin_can_alarm']
for n in BLOCKS:
    if n == 'barbed_wire':
        json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'outbreak:block/barbed_wire'}}, open(f'{A}/models/item/{n}.json', 'w'))
    else:
        json.dump({'parent': f'outbreak:block/{n}'}, open(f'{A}/models/item/{n}.json', 'w'))
    json.dump({'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'outbreak:{n}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]},
              open(f'{D}/loot_tables/blocks/{n}.json', 'w'))

def shaped(name, pattern, key, count=1):
    json.dump({'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': v}) for k, v in key.items()},
               'result': {'item': f'outbreak:{name}', 'count': count}}, open(f'{D}/recipes/{name}.json', 'w'), indent=1)
shaped('katana', ['  I', ' I ', 'L  '], {'I': 'minecraft:iron_ingot', 'L': 'minecraft:leather'})
shaped('hatchet', ['II', ' S'], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'})
shaped('meat_cleaver', ['II', 'S '], {'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'})
shaped('tire_iron', ['  I', ' I ', 'I  '], {'I': 'outbreak:scrap_metal'})
shaped('pickaxe_handle', ['  S', ' S ', 'S  '], {'S': '#minecraft:logs'})
shaped('barbed_bat', ['W', 'B'], {'W': 'outbreak:barbed_wire', 'B': 'outbreak:baseball_bat'})
shaped('rolling_pin', ['SPS'], {'S': 'minecraft:stick', 'P': '#minecraft:planks'})
shaped('shovel_weapon', ['T', 'S'], {'T': 'outbreak:duct_tape', 'S': 'minecraft:iron_shovel'})
shaped('sandbags', ['SSS', 'SSS'], {'S': '#minecraft:sand'}, 2)
shaped('wooden_crate', ['PPP', 'P P', 'PPP'], {'P': 'minecraft:stick'})
shaped('oil_drum', ['S', 'S'], {'S': 'outbreak:scrap_metal'})
shaped('rubble_pile', ['CC'], {'C': 'minecraft:cobblestone'}, 2)
shaped('mattress', ['WWW'], {'W': '#minecraft:wool'})
shaped('help_sign', ['P', 'D'], {'P': '#minecraft:planks', 'D': 'minecraft:red_dye'})
shaped('caution_tape', ['YBY'], {'Y': 'minecraft:yellow_dye', 'B': 'minecraft:string'}, 6)
shaped('body_bag', ['LLL'], {'L': 'minecraft:black_wool'})
shaped('bear_trap', ['I I', 'SIS'], {'I': 'minecraft:iron_ingot', 'S': 'outbreak:scrap_metal'})
shaped('spike_strip', ['NNN', 'PPP'], {'N': 'outbreak:nails', 'P': '#minecraft:wooden_slabs'}, 2)
shaped('barbed_wire', ['N N', ' S ', 'N N'], {'N': 'minecraft:iron_nugget', 'S': 'outbreak:scrap_metal'}, 4)
shaped('tin_can_alarm', ['SSS', 'N N'], {'S': 'minecraft:string', 'N': 'minecraft:iron_nugget'}, 2)

LP = f'{A}/lang/en_us.json'; lang = json.load(open(LP))
NAMES = {'katana': 'Katana', 'hatchet': 'Hatchet', 'meat_cleaver': 'Meat Cleaver', 'tire_iron': 'Tire Iron', 'pickaxe_handle': 'Pickaxe Handle',
         'barbed_bat': 'Barbed Wire Bat', 'rolling_pin': 'Rolling Pin', 'shovel_weapon': 'Taped Shovel'}
for k, v in NAMES.items(): lang[f'item.outbreak.{k}'] = v
SKN = {'aiming': ('Aiming', ['Point and Shoot', 'Steady Hands', 'One Shot']), 'melee': ('Melee', ['Swing Away', 'Back to Basics', 'The Long Arm']),
       'fitness': ('Fitness', ['Couch to 5K', 'Running Free', 'Marathon Man']), 'carpentry': ('Carpentry', ['Nails and Boards', 'The Handy Man', 'Master Carpenter']),
       'mechanics': ('Mechanics', ['Under the Hood', 'Grease Monkey', 'Engine Rebuild']), 'first_aid': ('First Aid', ['Band-Aids 101', 'Field Medic', 'Trauma Surgery']),
       'scavenging': ('Scavenging', ['Finders Keepers', 'Dumpster Diving', 'Leave Nothing'])}
for k, (sk, titles) in SKN.items():
    for v in (1, 2, 3): lang[f'item.outbreak.book_{k}_{v}'] = f'{titles[v - 1]} ({sk} Vol. {v})'
BN = {'sandbags': 'Sandbags', 'wooden_crate': 'Wooden Crate', 'oil_drum': 'Oil Drum', 'rubble_pile': 'Rubble Pile', 'mattress': 'Dirty Mattress',
      'help_sign': 'HELP Sign', 'caution_tape': 'Caution Tape', 'body_bag': 'Body Bag', 'bear_trap': 'Bear Trap', 'spike_strip': 'Spike Strip',
      'barbed_wire': 'Barbed Wire', 'tin_can_alarm': 'Tin Can Alarm'}
for k, v in BN.items(): lang[f'block.outbreak.{k}'] = v
lang.update({
 'entity.outbreak.feral_dog': 'Feral Dog', 'entity.outbreak.infected_rat': 'Infected Rat',
 'item.outbreak.skill_book.tip': '%s: for levels %s to %s. Hold right-click to read.',
 'msg.outbreak.too_dark': "It's too dark to read.",
 'msg.outbreak.book_read': "You've already read this one.",
 'msg.outbreak.book_hard': 'This is too advanced for you yet.',
 'msg.outbreak.book_easy': "There's nothing in here you don't already know.",
 'msg.outbreak.book_learned': 'You learned a lot about %s.',
 'msg.outbreak.alarm': 'Your tin can alarm rattled at %s, %s!',
 'action.outbreak.world_map': 'World map',
 'map.outbreak.title': 'Map', 'map.outbreak.centre': 'Centre', 'map.outbreak.safehouse': 'Safehouse',
 'key.outbreak.map': 'World map',
 'guide.outbreak.11.title': 'Books, animals, traps and the map',
 'guide.outbreak.11.body': "Skill books come in three volumes per skill. Hold right-click to read one in good light: if it's pitched at your level you learn a level's worth. Each book teaches you once.\nThe infection reached the animals: feral dogs hunt in packs outdoors, mostly at night, and infected rats swarm in dark buildings. Both can infect you with a bite.\nProtect your safehouse with traps: bear traps snap shut on whatever steps in them (sneak to step over), spike strips hurt anything walking across, barbed wire tangles and cuts, and a tin can alarm tells you when something walks through it.\nYour world map (J, or the action menu) fills in as you explore and is saved for each world.",
})
json.dump(lang, open(LP, 'w'), indent=2, ensure_ascii=False)
print('ok')
