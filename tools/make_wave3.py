"""Wave 3: materials, more food, crafted gear (art, models, armour layers), recipes, lang."""
import json, os, sys, random
R = sys.argv[1]
exec(open(os.path.join(os.path.dirname(__file__), 'make_more.py')).read())
rr = random.Random(31)

# ---------- materials
def scrap():
    c = C()
    for (x0, y0, x1, y1, col) in ((2, 8, 8, 13, STEEL), (6, 4, 12, 9, BLK3), (9, 9, 14, 13, hexc('8a5a3a')), (3, 3, 6, 6, STEEL2)):
        c.rect(x0, y0, x1, y1, col, top=shade(col, 1.2), bot=shade(col, 0.7))
    c.px(4, 10, hexc('6a4a30')); c.px(10, 6, hexc('6a4a30'))
    c.outline(); return c
def electronics():
    c = C(); G = hexc('2a7a3a')
    c.rect(2, 3, 13, 12, G, top=shade(G, 1.2), bot=shade(G, 0.7))
    c.rect(4, 5, 7, 8, BLK); c.rect(9, 5, 11, 6, BLK2); c.rect(9, 8, 11, 10, hexc('c0a040'))
    for x in range(3, 13, 2): c.px(x, 11, hexc('d0c060'))
    c.line(4, 9, 8, 9, hexc('d0c060')); c.line(8, 9, 8, 11, hexc('d0c060'))
    c.outline(); return c
def tape():
    c = C(); G = hexc('9aa0a8')
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 2.5 <= d <= 6.5: c.px(x, y, G if d > 3.5 else shade(G, 0.7))
    c.rect(11, 9, 14, 10, G)
    c.outline(); return c
def nails():
    c = C()
    for i, (x, y) in enumerate(((3, 3), (7, 5), (11, 3), (5, 9), (10, 9))):
        c.rect(x, y, x + 2, y, STEEL2); c.line(x + 1, y + 1, x + 1, y + 4, STEEL)
    c.outline(); return c
def rag():
    c = C(); W = hexc('d8d0c0')
    for y in range(4, 13):
        for x in range(2, 14):
            if rr.random() > 0.1 and not (y == 4 and x % 3 == 0): c.px(x, y, W if (x + y) % 5 else shade(W, 0.8))
    c.px(5, 7, hexc('8a3030')); c.px(6, 8, hexc('8a3030'))
    c.outline(); return c
def gun_parts():
    c = C()
    c.rect(2, 3, 9, 5, BLK2, top=BLK3)       # slide
    c.rect(9, 7, 13, 9, BLK); c.rect(10, 9, 11, 13, BLK2)   # trigger group
    c.line(3, 9, 7, 13, STEEL)                # spring
    for k in range(0, 5, 2): c.px(3 + k, 9 + k, STEEL2)
    c.rect(2, 11, 4, 13, BLK3)
    c.outline(); return c
def car_battery():
    c = C(); B = hexc('2a2a2e')
    c.rect(2, 5, 13, 13, B, top=shade(B, 1.4)); c.rect(3, 3, 5, 4, hexc('c03030')); c.rect(10, 3, 12, 4, BLK3)
    c.rect(4, 8, 11, 10, hexc('e0c030'))
    c.outline(); return c
for n, fn in (('scrap_metal', scrap), ('electronics', electronics), ('duct_tape', tape), ('nails', nails), ('rag', rag),
              ('gun_parts', gun_parts), ('car_battery', car_battery)):
    fn().save(f'{ITEM}/{n}.png')
# ---------- food
def mre():
    c = C(); O = hexc('6a6a3a')
    c.rect(3, 2, 12, 14, O, top=shade(O, 1.2), bot=shade(O, 0.7)); c.rect(5, 5, 10, 8, hexc('2a2a1a')); c.rect(4, 2, 11, 3, shade(O, 0.8))
    c.outline(); return c
def tuna():
    c = C(); M = hexc('b8bcc4')
    c.rect(2, 6, 13, 12, M, top=hexc('e0e4ea'), bot=hexc('80848c')); c.rect(2, 8, 13, 10, hexc('3060a0')); c.rect(6, 8, 9, 10, hexc('e8e0d0'))
    c.outline(); return c
def edrink():
    c = C(); G = hexc('30c040')
    c.rect(5, 2, 10, 14, BLK2, top=STEEL2, bot=BLK); c.rect(5, 5, 10, 11, G); c.line(6, 10, 9, 6, hexc('f0f040'))
    c.outline(); return c
mre().save(f'{ITEM}/mre.png'); tuna().save(f'{ITEM}/canned_tuna.png'); edrink().save(f'{ITEM}/energy_drink.png')
# ---------- crafted weapons
def nail_board():
    c = C(); diag(c, 2, 13, 12, 3, WOOD2, 3)
    for (x, y) in ((9, 3), (11, 5), (8, 6), (12, 2), (10, 7)): c.px(x, y - 1, STEEL2)
    c.outline(); return c
def spear():
    c = C(); diag(c, 1, 14, 10, 5, WOOD2, 1); c.rect(9, 5, 10, 6, hexc('9aa0a8'))
    for i in range(4): c.px(11 + i, 4 - i, STEEL2); c.px(11 + i, 5 - i, STEEL)
    c.outline(); return c
def rbat():
    c = bat(True)
    for (x, y) in ((8, 6), (10, 4), (12, 3)): c.px(x, y, hexc('9aa0a8'))
    c.rect(5, 9, 6, 10, hexc('9aa0a8'))
    return c
nail_board().save(f'{ITEM}/nail_board.png'); spear().save(f'{ITEM}/makeshift_spear.png'); rbat().save(f'{ITEM}/reinforced_bat.png')
def bomb():
    c = C(); c.rect(4, 4, 11, 12, STEEL, top=STEEL2, bot=BLK3); c.rect(3, 4, 12, 5, BLK3); c.rect(3, 11, 12, 12, BLK3)
    c.rect(4, 7, 11, 8, hexc('9aa0a8')); c.line(8, 3, 10, 1, hexc('c0a040')); c.px(11, 0, hexc('f08030'))
    c.outline(); return c
def molotov():
    c = C(); G = (110, 160, 90, 220)
    c.rect(5, 6, 10, 14, G, top=(150, 200, 120, 230)); c.rect(6, 3, 9, 5, G); c.rect(5, 8, 10, 11, (190, 140, 40, 230))
    c.rect(6, 0, 8, 2, hexc('d8d0c0')); c.px(9, 1, hexc('f06020')); c.px(9, 0, hexc('f0c030'))
    c.outline(); return c
def noise():
    c = C(); c.rect(3, 5, 12, 13, hexc('c03030'), top=hexc('e05050'), bot=hexc('801818'))
    c.rect(5, 7, 10, 9, hexc('202020')); c.rect(6, 7, 6, 9, hexc('60f060')); c.px(4, 3, BLK3); c.px(11, 3, BLK3); c.rect(4, 4, 11, 4, BLK3)
    c.outline(); return c
bomb().save(f'{ITEM}/pipe_bomb.png'); molotov().save(f'{ITEM}/molotov.png'); noise().save(f'{ITEM}/noise_maker.png')
# ---------- crafted armour (item icons)
shirt(hexc('7a7f86'), stripes=hexc('9a6a3a')).save(f'{ITEM}/scrap_armor.png')
vest(hexc('2a2e24'), hexc('4a5038')).save(f'{ITEM}/kevlar_vest.png')
helmet(hexc('2e3226')).save(f'{ITEM}/tactical_helmet.png')
ng = C(); ng.rect(2, 6, 13, 10, BLK2, top=BLK3); ng.rect(3, 7, 6, 10, BLK); ng.rect(9, 7, 12, 10, BLK); ng.px(4, 8, hexc('40e060')); ng.px(10, 8, hexc('40e060'))
ng.rect(1, 5, 14, 5, BLK3); ng.outline(); ng.save(f'{ITEM}/night_vision_goggles.png')
# ---------- worn layers
c = C(64, 32); wear_body(c, hexc('7a7f86'), noise=0.12)
for (x0, y0, x1, y1) in ((20, 21, 24, 25), (25, 26, 28, 30), (33, 22, 37, 27)): region(c, x0, y0, x1, y1, hexc('8a5a3a'), 0.1)
for y in (24, 29): region(c, 16, y, 40, y + 1, hexc('9aa0a8'))
layers('scrap_armor', c)
k = C(64, 32); body_box(k, hexc('2a2e24'), hexc('4a5038')); head_box(k, hexc('2e3226'))
region(k, 8, 9, 16, 10, hexc('1a1c16')); k.save(f'{AR}/outbreak_kevlar_layer_1.png'); C(64, 32).save(f'{AR}/outbreak_kevlar_layer_2.png')
n = C(64, 32); region(n, 0, 9, 32, 10, hexc('2a2a2e')); region(n, 8, 10, 16, 13, BLK2)
region(n, 9, 10, 11, 12, BLK); region(n, 13, 10, 15, 12, BLK); n.px(10, 11, hexc('40e060')); n.px(14, 11, hexc('40e060'))
layers('night_vision_goggles', n)
# ---------- models
for m in ('nail_board', 'makeshift_spear', 'reinforced_bat'):
    json.dump({'parent': 'item/handheld', 'textures': {'layer0': f'outbreak:item/{m}'}}, open(f'{MD}/{m}.json', 'w'), indent=1)
for o in ('scrap_metal', 'electronics', 'duct_tape', 'nails', 'rag', 'gun_parts', 'car_battery', 'mre', 'canned_tuna', 'energy_drink',
          'pipe_bomb', 'molotov', 'noise_maker', 'scrap_armor', 'kevlar_vest', 'tactical_helmet', 'night_vision_goggles'):
    json.dump({'parent': 'item/generated', 'textures': {'layer0': f'outbreak:item/{o}'}}, open(f'{MD}/{o}.json', 'w'), indent=1)
# ---------- recipes
def raw(name, obj): json.dump(obj, open(f'{RC}/{name}.json', 'w'), indent=1)
def ing(x): return {'tag': x[1:]} if x.startswith('#') else {'item': x}
def sl(name, result, items, count=1): raw(name, {'type': 'minecraft:crafting_shapeless', 'ingredients': [ing(i) for i in items], 'result': {'item': result, 'count': count}})
def sh(name, result, pattern, keys, count=1):
    raw(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {k: ing(v) for k, v in keys.items()}, 'result': {'item': result, 'count': count}})
O = 'outbreak:'
# tier 0: basic materials
sl('rag', O + 'rag', ['#minecraft:wool'], 4)
sl('bandage_from_rags', O + 'bandage', [O + 'rag', O + 'rag'])
sl('nails', O + 'nails', ['minecraft:iron_ingot'], 12)
sl('duct_tape', O + 'duct_tape', ['minecraft:slime_ball', 'minecraft:paper', 'minecraft:string'], 2)
sl('gun_parts', O + 'gun_parts', [O + 'scrap_metal', O + 'scrap_metal', O + 'scrap_metal', 'minecraft:iron_ingot', O + 'duct_tape'])
raw('scrap_to_nuggets', {'type': 'minecraft:smelting', 'ingredient': {'item': O + 'scrap_metal'}, 'result': 'minecraft:iron_nugget', 'experience': 0.1, 'cookingtime': 160})
# tier 1: scrap gear
sl('nail_board', O + 'nail_board', ['#minecraft:planks', '#minecraft:planks', O + 'nails', O + 'nails'])
sl('makeshift_spear', O + 'makeshift_spear', ['minecraft:stick', 'minecraft:stick', O + 'kitchen_knife', O + 'duct_tape'])
sl('spiked_bat', O + 'spiked_bat', [O + 'baseball_bat', O + 'nails', O + 'nails', O + 'nails'])
sh('scrap_armor', O + 'scrap_armor', ['S S', 'SDS', 'SLS'], {'S': O + 'scrap_metal', 'D': O + 'duct_tape', 'L': 'minecraft:leather'})
sl('noise_maker', O + 'noise_maker', [O + 'electronics', O + 'scrap_metal', 'minecraft:redstone', O + 'duct_tape'])
sl('molotov', O + 'molotov', ['minecraft:glass_bottle', O + 'rag', 'minecraft:gunpowder'])
# tier 2: reinforced gear
sl('reinforced_bat', O + 'reinforced_bat', [O + 'spiked_bat', O + 'scrap_metal', O + 'scrap_metal', O + 'duct_tape'])
sl('pipe_bomb', O + 'pipe_bomb', [O + 'scrap_metal', 'minecraft:gunpowder', 'minecraft:gunpowder', O + 'nails', O + 'duct_tape'])
sh('kevlar_vest', O + 'kevlar_vest', ['SDS', 'RVR', 'SDS'], {'S': O + 'scrap_metal', 'D': O + 'duct_tape', 'R': O + 'rag', 'V': O + 'police_vest'})
sl('tactical_helmet', O + 'tactical_helmet', [O + 'riot_helmet', O + 'scrap_metal', O + 'scrap_metal', O + 'duct_tape'])
sl('tactical_helmet_military', O + 'tactical_helmet', [O + 'military_helmet', O + 'scrap_metal', O + 'scrap_metal', O + 'duct_tape'])
# tier 3: military-grade, built from gun parts and electronics
sh('night_vision_goggles', O + 'night_vision_goggles', ['GEG', 'DBD'], {'G': 'minecraft:glass_pane', 'E': O + 'electronics', 'D': O + 'duct_tape', 'B': O + 'car_battery'})
sl('night_vision_recharge', O + 'night_vision_goggles', [O + 'night_vision_goggles', O + 'car_battery'])
sl('craft_pistol', O + 'pistol', [O + 'gun_parts', O + 'gun_parts', O + 'scrap_metal', O + 'duct_tape'])
sl('craft_shotgun', O + 'shotgun', [O + 'gun_parts', O + 'gun_parts', O + 'scrap_metal', O + 'scrap_metal', '#minecraft:planks'])
sl('craft_hunting_rifle', O + 'hunting_rifle', [O + 'gun_parts', O + 'gun_parts', O + 'gun_parts', '#minecraft:planks', '#minecraft:planks', 'minecraft:glass_pane'])
sl('craft_assault_rifle', O + 'assault_rifle', [O + 'gun_parts', O + 'gun_parts', O + 'gun_parts', O + 'gun_parts', O + 'scrap_metal', O + 'scrap_metal', O + 'electronics'])
sl('ammo_9mm_scrap', O + 'ammo_9mm', [O + 'scrap_metal', 'minecraft:gunpowder'], 6)
# ---------- lang
lang = json.load(open(LP))
lang.update({f'item.outbreak.{k}': v for k, v in {
    'scrap_metal': 'Scrap Metal', 'electronics': 'Electronics', 'duct_tape': 'Duct Tape', 'nails': 'Nails', 'rag': 'Rag', 'gun_parts': 'Gun Parts',
    'car_battery': 'Car Battery', 'mre': 'MRE', 'canned_tuna': 'Canned Tuna', 'energy_drink': 'Energy Drink', 'nail_board': 'Nail Board',
    'makeshift_spear': 'Makeshift Spear', 'reinforced_bat': 'Reinforced Bat', 'scrap_armor': 'Scrap Armour', 'kevlar_vest': 'Kevlar Vest',
    'tactical_helmet': 'Tactical Helmet', 'night_vision_goggles': 'Night Vision Goggles', 'pipe_bomb': 'Pipe Bomb', 'molotov': 'Molotov',
    'noise_maker': 'Noise Maker'}.items()})
lang.update({
    'entity.outbreak.thrown_gear': 'Thrown Gear',
    'tooltip.outbreak.pipe_bomb': 'Throw: explodes on impact. Very loud.',
    'tooltip.outbreak.molotov': 'Throw: sets the ground where it lands on fire.',
    'tooltip.outbreak.noise_maker': 'Throw: beeps for 15 seconds and pulls every zombie nearby towards it.',
    'action.outbreak.salvage_vehicle': 'Salvage parts', 'action.outbreak.salvage_vehicle_no_tool': 'Salvage parts (needs a tool)',
    'action.outbreak.salvage_electronics': 'Salvage electronics', 'action.outbreak.salvage_electronics_no_tool': 'Salvage electronics (needs a tool)',
    'action.outbreak.salvage_metal': 'Scrap for metal', 'action.outbreak.salvage_metal_no_tool': 'Scrap for metal (needs a tool)',
    'action.outbreak.rip_clothes': 'Rip %s into rags',
    'msg.outbreak.need_tool': 'You need a crowbar, pipe wrench, hammer, sledgehammer or fire axe.',
    'msg.outbreak.airport_clear': 'Silence. The airport is clear.', 'msg.outbreak.airport_left': 'You can hear them all over the airport. %s still shambling around.',
    'msg.outbreak.stripped': 'This one has already been stripped.', 'msg.outbreak.salvaged': 'Salvaged.',
})
json.dump(lang, open(LP, 'w'), indent=1)
print('wave 3 ok')
