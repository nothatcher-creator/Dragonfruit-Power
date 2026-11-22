"""Rain barrel assets: a blue plastic drum with a rim, water inside at four levels."""
import json, os, sys, random
from PIL import Image
R = sys.argv[1]
A = os.path.join(R, 'src/main/resources/assets/outbreak'); D = os.path.join(R, 'src/main/resources/data/outbreak')
rr = random.Random(3)
def tex(name, f):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16): im.putpixel((x, y), f(x, y))
    im.save(f'{A}/textures/block/{name}.png')
def cl(v): return max(0, min(255, int(v)))
def side(x, y):
    g = 1 + rr.uniform(-0.04, 0.04) - (0.18 if y in (3, 4, 11, 12) else 0) - (0.1 if x in (0, 15) else 0)
    if y > 13 and rr.random() < 0.3: g *= 0.8
    return (cl(40 * g), cl(86 * g), cl(160 * g), 255)
def top(x, y):
    edge = x in (0, 15) or y in (0, 15)
    g = 1 + rr.uniform(-0.05, 0.05)
    return (cl(52 * g), cl(98 * g), cl(172 * g), 255) if edge else (cl(20 * g), cl(30 * g), cl(44 * g), 255)
tex('rain_barrel_side', side); tex('rain_barrel_top', top)
T = {'side': 'outbreak:block/rain_barrel_side', 'top': 'outbreak:block/rain_barrel_top', 'water': 'minecraft:block/water_still', 'particle': 'outbreak:block/rain_barrel_side'}
def box(f, t, tex_side='#side', tex_top='#top', tint=False):
    faces = {}
    for d in ('north', 'south', 'east', 'west'): faces[d] = {'uv': [f[0], 16 - t[1], t[0], 16 - f[1]], 'texture': tex_side}
    for d in ('up', 'down'): faces[d] = {'uv': [f[0], f[2], t[0], t[2]], 'texture': tex_top}
    if tint:
        for v in faces.values(): v['tintindex'] = 0
    return {'from': f, 'to': t, 'faces': faces}
for lvl in range(4):
    els = [box([2, 0, 2], [14, 1, 14]),
           box([2, 1, 2], [3, 15, 14]), box([13, 1, 2], [14, 15, 14]), box([3, 1, 2], [13, 15, 3]), box([3, 1, 13], [13, 15, 14])]
    if lvl:
        h = 1 + lvl * 4.4
        w = box([3, 1, 3], [13, h, 13], '#water', '#water', True)
        els.append(w)
    json.dump({'parent': 'block/block', 'textures': T, 'elements': els}, open(f'{A}/models/block/rain_barrel_{lvl}.json', 'w'), indent=1)
json.dump({'variants': {f'level={l}': {'model': f'outbreak:block/rain_barrel_{l}'} for l in range(4)}}, open(f'{A}/blockstates/rain_barrel.json', 'w'), indent=1)
json.dump({'parent': 'outbreak:block/rain_barrel_0'}, open(f'{A}/models/item/rain_barrel.json', 'w'))
os.makedirs(f'{D}/loot_tables/blocks', exist_ok=True)
json.dump({'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'outbreak:rain_barrel'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]},
          open(f'{D}/loot_tables/blocks/rain_barrel.json', 'w'), indent=1)
json.dump({'type': 'minecraft:crafting_shaped', 'pattern': ['S S', 'S S', 'SBS'], 'key': {'S': {'item': 'outbreak:scrap_metal'}, 'B': {'item': 'minecraft:bucket'}},
           'result': {'item': 'outbreak:rain_barrel'}}, open(f'{D}/recipes/rain_barrel.json', 'w'), indent=1)
LP = f'{A}/lang/en_us.json'; lang = json.load(open(LP))
lang.update({
 'block.outbreak.rain_barrel': 'Rain Barrel',
 'msg.outbreak.rain_barrel_0': 'The rain barrel is empty. Leave it out in the rain.',
 'msg.outbreak.rain_barrel_1': 'A little rain water in the bottom.',
 'msg.outbreak.rain_barrel_2': 'The rain barrel is half full.',
 'msg.outbreak.rain_barrel_3': 'The rain barrel is full.',
 'action.outbreak.claim_safehouse': 'Claim this building as your safehouse',
 'action.outbreak.move_safehouse': 'Make this your safehouse instead',
 'action.outbreak.abandon_safehouse': 'Abandon your safehouse',
 'action.outbreak.character_sheet': 'Character sheet and skills',
 'msg.outbreak.safehouse_claimed': 'This is your safehouse now. No more zombies will turn up inside it, you rest and heal faster here, and you wake up here.',
 'msg.outbreak.safehouse_outside': 'Go inside a building to claim it.',
 'msg.outbreak.safehouse_taken': 'Someone else already calls this place home.',
 'msg.outbreak.safehouse_abandoned': 'You leave the safehouse behind.',
 'msg.outbreak.skill_up': '%s went up to level %s.',
 'skills.outbreak.title': 'Character sheet',
 'skills.outbreak.no_safehouse': 'No safehouse yet: go inside a building and claim it from the action menu.',
 'skills.outbreak.in_safehouse': "You're in your safehouse.",
 'skills.outbreak.safehouse_at': 'Your safehouse is at %s, %s.',
 'skill.outbreak.aiming': 'Aiming', 'skill.outbreak.melee': 'Melee', 'skill.outbreak.fitness': 'Fitness',
 'skill.outbreak.carpentry': 'Carpentry', 'skill.outbreak.mechanics': 'Mechanics', 'skill.outbreak.first_aid': 'First Aid',
 'skill.outbreak.scavenging': 'Scavenging',
 'guide.outbreak.10.title': 'Skills and safehouses',
 'guide.outbreak.10.body': "Skills go up as you use them, from 0 to 10. Aiming (shooting zombies): tighter, harder-hitting shots. Melee (killing zombies hand to hand): harder hits. Fitness (sprinting): faster on foot. Carpentry, Mechanics, First Aid and Scavenging (barricading, salvaging, using medicine, searching): those jobs go quicker. Your occupation gives you a head start. See them all on your character sheet in the action menu.\nFind a building you like, barricade it, and claim it as your safehouse from the action menu. No zombies spawn inside it, you heal while you rest there if you're fed, it's marked green on your minimap, and you wake up there. Put a rain barrel outside for water.",
})
json.dump(lang, open(LP, 'w'), indent=2, ensure_ascii=False)
print('ok')
