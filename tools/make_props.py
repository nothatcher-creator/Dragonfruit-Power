"""Wave 2 city blocks: seamless glass, furniture, street furniture, church pieces, park leaves."""
import os, sys, json, random, math
exec(open(os.path.join(os.path.dirname(__file__), 'make_city_blocks.py')).read())
from PIL import Image, ImageDraw
r2 = random.Random(77)
def solid(c, var=4):
    return noisy(c, var)
def save(n, im): im.save(os.path.join(TEX, n + '.png'))
def px_img(fn):
    im = Image.new('RGBA', (16, 16)); p = im.load()
    for y in range(16):
        for x in range(16): p[x, y] = fn(x, y)
    return im
cl = lambda v: max(0, min(255, int(v)))

# ---------- glass (no borders, diagonal sheen with a 16px period so it tiles seamlessly)
save('clear_glass', px_img(lambda x, y: (215, 235, 245, 70) if (x + y) % 16 in (4, 5) else (225, 240, 250, 40) if (x + y) % 16 == 11 else (200, 225, 240, 22)))
def refl(x, y):
    d = (x + y) % 16
    if d in (3, 4): return (190, 215, 235, 235)
    if d == 5: return (120, 155, 185, 225)
    if d == 11: return (95, 130, 165, 222)
    n = r2.randint(-3, 3)
    return (cl(52 + n), cl(84 + n), cl(112 + n), 215)
save('reflective_glass', px_img(refl))

# ---------- furniture textures
save('wood_light', px_img(lambda x, y: (cl(196 + (6 if y % 5 == 0 else 0) + r2.randint(-5, 5)), cl(160 + r2.randint(-5, 5)), cl(112 + r2.randint(-4, 4)), 255)))
save('wood_dark', px_img(lambda x, y: (cl(92 + (8 if y % 4 == 0 else 0) + r2.randint(-4, 4)), cl(60 + r2.randint(-3, 3)), cl(36 + r2.randint(-3, 3)), 255)))
save('panel_gray', solid((150, 152, 156), 3))
save('metal_gray', solid((128, 132, 138), 4))
save('fabric_black', solid((40, 40, 44), 5))
save('fabric_blue', solid((52, 78, 128), 6))
s = Image.new('RGBA', (16, 16), (20, 20, 22, 255)); d = ImageDraw.Draw(s)
d.rectangle([1, 1, 14, 14], fill=(40, 110, 180, 255))
for yy in range(3, 13, 3): d.line([(3, yy), (3 + r2.randint(5, 10), yy)], fill=(220, 235, 250, 255))
save('screen', s)
save('screen_back', solid((26, 26, 28), 2))
c = solid((128, 132, 138), 3); d = ImageDraw.Draw(c)
for yy in (0, 5, 10, 15): d.line([(0, yy), (15, yy)], fill=(88, 90, 96, 255))
for yy in (2, 7, 12): d.line([(6, yy), (9, yy)], fill=(210, 210, 214, 255))
save('cabinet_front', c)
save('cooler_white', solid((232, 232, 228), 3)); save('cooler_bottle', solid((150, 200, 235), 6))
save('pot', solid((170, 92, 60), 6))
pl = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(pl)
for k in range(9):
    cx, cy = r2.randint(2, 13), r2.randint(1, 12)
    col = r2.choice([(46, 110, 40), (62, 132, 50), (38, 92, 34)])
    d.ellipse([cx - 2, cy - 1, cx + 2, cy + 2], fill=col + (255,))
d.line([(8, 15), (8, 6)], fill=(60, 90, 30, 255)); save('plant_leaves', pl)
t = solid((16, 16, 18), 2); d = ImageDraw.Draw(t); d.line([(2, 13), (11, 2)], fill=(44, 46, 54, 255)); d.line([(4, 14), (13, 3)], fill=(34, 36, 42, 255)); save('tv_screen', t)
save('countertop', px_img(lambda x, y: (200, 200, 205, 255) if (x * 3 + y * 7) % 23 == 0 else (cl(236 + r2.randint(-4, 2)),) * 3 + (255,)))
cf = solid((230, 228, 220), 3); d = ImageDraw.Draw(cf); d.rectangle([1, 1, 7, 14], outline=(190, 188, 180, 255)); d.rectangle([8, 1, 14, 14], outline=(190, 188, 180, 255))
d.line([(6, 6), (6, 9)], fill=(120, 120, 124, 255)); d.line([(9, 6), (9, 9)], fill=(120, 120, 124, 255)); save('cabinet_wood_front', cf)
save('cabinet_side', solid((230, 228, 220), 3))
ff = solid((238, 238, 236), 2); d = ImageDraw.Draw(ff); d.line([(0, 6), (15, 6)], fill=(190, 190, 192, 255)); d.line([(13, 1), (13, 4)], fill=(150, 150, 154, 255)); d.line([(13, 8), (13, 13)], fill=(150, 150, 154, 255)); save('fridge_front', ff)
save('appliance_white', solid((238, 238, 236), 2))
st = solid((236, 236, 234), 2); d = ImageDraw.Draw(st)
for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)): d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(30, 30, 32, 255)); d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(70, 70, 74, 255))
save('stove_top', st)
sf = solid((236, 236, 234), 2); d = ImageDraw.Draw(sf); d.rectangle([2, 5, 13, 13], fill=(30, 30, 34, 255)); d.line([(3, 3), (12, 3)], fill=(150, 150, 154, 255))
for xx in (3, 6, 9, 12): d.point((xx, 1), fill=(40, 40, 44, 255))
save('stove_front', sf)
save('shelf_metal', solid((214, 216, 220), 3))
pr = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(pr)
x = 0
while x < 16:
    wdt = r2.randint(2, 4); col = r2.choice([(200, 40, 40), (40, 90, 190), (230, 190, 40), (60, 150, 70), (240, 240, 240), (230, 120, 30), (120, 60, 150)])
    top = r2.randint(2, 7); d.rectangle([x, top, x + wdt - 1, 15], fill=col + (255,)); d.line([(x, top), (x + wdt - 1, top)], fill=tuple(min(255, v + 40) for v in col) + (255,))
    x += wdt
save('products', pr)
rg = solid((70, 72, 78), 3); d = ImageDraw.Draw(rg); d.rectangle([3, 2, 12, 7], fill=(40, 160, 90, 255))
for yy in (9, 11, 13):
    for xx in (4, 7, 10): d.point((xx, yy), fill=(200, 200, 204, 255))
save('register', rg)
mb = solid((34, 70, 150), 4); d = ImageDraw.Draw(mb); d.rectangle([3, 3, 12, 4], fill=(14, 20, 40, 255)); d.rectangle([4, 8, 11, 11], fill=(220, 220, 225, 255)); save('mailbox', mb)
nb = solid((190, 36, 36), 4); d = ImageDraw.Draw(nb); d.rectangle([2, 2, 13, 9], fill=(170, 190, 200, 255)); d.rectangle([3, 3, 12, 8], fill=(225, 222, 210, 255))
for yy in (4, 6): d.line([(4, yy), (11, yy)], fill=(80, 80, 84, 255))
save('newsbox_front', nb); save('newsbox_side', solid((190, 36, 36), 4))
def vend(top):
    v = solid((180, 30, 34), 4); d = ImageDraw.Draw(v); d.rectangle([1, 0 if not top else 2, 10, 15], fill=(30, 40, 50, 255))
    for yy in range(1 if not top else 3, 15, 4):
        for xx in range(2, 10, 2): d.rectangle([xx, yy, xx, yy + 2], fill=r2.choice([(220, 60, 60), (60, 120, 220), (240, 200, 60), (80, 180, 80)]) + (255,))
    if not top: d.rectangle([12, 2, 14, 9], fill=(40, 40, 44, 255)); d.rectangle([2, 13, 9, 14], fill=(14, 14, 16, 255))
    else: d.rectangle([12, 4, 14, 6], fill=(240, 240, 240, 255))
    return v
save('vending_bottom', vend(False)); save('vending_top', vend(True)); save('vending_side', solid((180, 30, 34), 4))
at = solid((150, 154, 160), 3); d = ImageDraw.Draw(at); d.rectangle([3, 2, 12, 7], fill=(40, 110, 170, 255)); d.rectangle([4, 9, 11, 13], fill=(60, 60, 64, 255))
for yy in (10, 12):
    for xx in (5, 7, 9): d.point((xx, yy), fill=(210, 210, 214, 255))
save('atm_front', at)
mt = solid((110, 114, 120), 3); d = ImageDraw.Draw(mt); d.rectangle([4, 3, 11, 7], fill=(150, 190, 120, 255)); d.rectangle([7, 10, 8, 12], fill=(30, 30, 30, 255)); save('meter_front', mt)
ss = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(ss)
d.polygon([(5, 0), (10, 0), (15, 5), (15, 10), (10, 15), (5, 15), (0, 10), (0, 5)], fill=(200, 30, 30, 255), outline=(240, 240, 240, 255))
d.rectangle([3, 6, 12, 9], fill=(240, 240, 240, 255)); save('stop_sign', ss)
save('planter_stone', solid((176, 172, 164), 5))
hd = Image.new('RGBA', (16, 16)); p = hd.load()
for y in range(16):
    for x in range(16): p[x, y] = r2.choice([(56, 112, 44), (66, 128, 52), (44, 94, 36), (76, 140, 60)]) + (255,)
save('hedge', hd)
ac = solid((176, 180, 184), 3); d = ImageDraw.Draw(ac)
for yy in range(2, 14, 2): d.line([(1, yy), (14, yy)], fill=(120, 124, 128, 255))
save('ac_side', ac)
at2 = solid((176, 180, 184), 3); d = ImageDraw.Draw(at2); d.ellipse([2, 2, 13, 13], fill=(40, 40, 44, 255))
d.line([(3, 8), (12, 8)], fill=(150, 150, 154, 255)); d.line([(8, 3), (8, 12)], fill=(150, 150, 154, 255)); save('ac_top', at2)
save('spout_stone', solid((196, 194, 186), 5))
save('altar_cloth', solid((240, 238, 230), 3))
tr = solid((150, 24, 30), 4); d = ImageDraw.Draw(tr); d.line([(7, 2), (7, 13)], fill=(220, 180, 60, 255)); d.line([(8, 2), (8, 13)], fill=(220, 180, 60, 255)); d.line([(4, 5), (11, 5)], fill=(220, 180, 60, 255)); save('altar_trim', tr)
gs = solid((128, 128, 124), 7); save('gravestone_side', gs)
gf = solid((128, 128, 124), 7); d = ImageDraw.Draw(gf); d.line([(8, 3), (8, 10)], fill=(80, 80, 78, 255)); d.line([(6, 5), (10, 5)], fill=(80, 80, 78, 255))
d.line([(5, 12), (11, 12)], fill=(96, 96, 92, 255)); save('gravestone_front', gf)
def leaves(cols, holes=0.12):
    im = Image.new('RGBA', (16, 16)); p = im.load()
    for y in range(16):
        for x in range(16): p[x, y] = (0, 0, 0, 0) if r2.random() < holes else r2.choice(cols) + (255,)
    return im
save('maple_leaves', leaves([(196, 72, 30), (220, 110, 34), (170, 50, 28), (230, 150, 40), (150, 40, 26)]))
save('linden_leaves', leaves([(70, 140, 52), (86, 160, 60), (58, 118, 44), (100, 172, 70)]))

# ---------- models
def box(f, t, tx, rot=None): return el(f, t, allf(tx), rot)
def fm(name, textures, elements, part=None):
    textures = dict(textures); textures.setdefault('particle', list(textures.values())[0])
    model(name, {"parent": "minecraft:block/block", "textures": textures, "elements": elements})
    facing_state(name); item(name); loot(name)
def faces(front, other, top=None):
    return {"north": front, "south": other, "east": other, "west": other, "up": top or other, "down": other}
W = {"w": tex('wood_light'), "p": tex('panel_gray')}
fm('desk', W, [box([0, 12, 0], [16, 14, 16], "#w"), box([0, 0, 1], [2, 12, 15], "#p"), box([14, 0, 1], [16, 12, 15], "#p"), box([2, 4, 13], [14, 12, 14], "#p")])
fm('office_chair', {"f": tex('fabric_black'), "m": tex('lamp_metal')}, [
    box([3, 6, 3], [13, 8.5, 13], "#f"), box([3.5, 8.5, 12], [12.5, 19, 14], "#f"), box([7, 2, 7], [9, 6, 9], "#m"),
    box([2, 0, 7], [14, 1.5, 9], "#m"), box([7, 0, 2], [9, 1.5, 14], "#m"), box([2, 8, 5], [3, 11, 11], "#m"), box([13, 8, 5], [14, 11, 11], "#m")])
fm('computer', {"s": tex('screen'), "b": tex('screen_back'), "k": tex('panel_gray')}, [
    box([4, -2, 7], [12, -1, 11], "#b"), box([7, -1, 8.5], [9, 2, 9.5], "#b"),
    el([2, 2, 7.5], [14, 10, 9], faces("#s", "#b")), box([3, -2, 2], [13, -1.4, 5.5], "#k")])
fm('filing_cabinet', {"f": tex('cabinet_front'), "m": tex('metal_gray')}, [el([1, 0, 2], [15, 16, 15], faces("#f", "#m"))])
fm('water_cooler', {"w": tex('cooler_white'), "b": tex('cooler_bottle')}, [box([4, 0, 4], [12, 10, 12], "#w"), box([5, 10, 5], [11, 16, 11], "#b"), box([6.5, 6, 3], [9.5, 7.5, 4], "#w")])
fm('potted_plant', {"p": tex('pot'), "l": tex('plant_leaves'), "d": "minecraft:block/dirt"}, [
    el([4, 0, 4], [12, 6, 12], faces("#p", "#p", "#d")),
    el([1, 6, 8], [15, 20, 8], {"north": "#l", "south": "#l"}, {"origin": [8, 8, 8], "axis": "y", "angle": 45}),
    el([8, 6, 1], [8, 20, 15], {"east": "#l", "west": "#l"}, {"origin": [8, 8, 8], "axis": "y", "angle": 45})])
F = {"f": tex('fabric_blue')}
fm('sofa', F, [box([0, 2, 1], [16, 7, 14], "#f"), box([0, 7, 11], [16, 15, 15], "#f"), box([0, 7, 1], [2.5, 11, 11], "#f"), box([13.5, 7, 1], [16, 11, 11], "#f"),
               box([1, 0, 2], [3, 2, 4], "#f"), box([13, 0, 2], [15, 2, 4], "#f"), box([1, 0, 12], [3, 2, 14], "#f"), box([13, 0, 12], [15, 2, 14], "#f")])
fm('tv', {"s": tex('tv_screen'), "b": tex('screen_back'), "d": tex('wood_dark')}, [box([1, 0, 6], [15, 5, 13], "#d"), box([7, 5, 9], [9, 6, 10], "#b"),
                                                                            el([-2, 6, 8.5], [18, 17, 10], faces("#s", "#b"))])
fm('kitchen_counter', {"f": tex('cabinet_wood_front'), "s": tex('cabinet_side'), "t": tex('countertop')}, [
    el([0, 1, 1], [16, 14, 16], faces("#f", "#s")), el([0, 14, 0], [16, 15, 16], allf("#t")), el([0, 0, 2], [16, 1, 16], allf("#s"))])
fm('fridge', {"f": tex('fridge_front'), "s": tex('appliance_white')}, [el([1, 0, 1], [15, 28, 15], faces("#f", "#s"))])
fm('stove', {"f": tex('stove_front'), "s": tex('appliance_white'), "t": tex('stove_top')}, [el([0, 0, 0], [16, 15, 16], faces("#f", "#s", "#t")),
                                                                                    box([0, 15, 14], [16, 19, 16], "#s")])
fm('store_shelf', {"m": tex('shelf_metal'), "p": tex('products')}, [
    box([0, 0, 7], [16, 16, 9], "#m")] + [box([0, y, 1], [16, y + 1, 15], "#m") for y in (0, 5, 10, 15)] +
    [el([0.5, y + 1, 1.5], [15.5, y + 4.5, 6.5], faces("#p", "#p")) for y in (0, 5, 10)] +
    [el([0.5, y + 1, 9.5], [15.5, y + 4.5, 14.5], {"south": "#p", "north": "#p", "east": "#p", "west": "#p", "up": "#p", "down": "#p"}) for y in (0, 5, 10)])
fm('checkout_counter', {"s": tex('panel_gray'), "t": tex('countertop'), "r": tex('register'), "b": tex('screen_back')}, [
    box([0, 0, 0], [16, 13, 16], "#s"), box([0, 13, 0], [16, 14, 16], "#t"),
    el([4, 14, 6], [12, 17, 11], faces("#r", "#b")), el([5, 17, 9], [11, 22, 10], faces("#r", "#b"))])
fm('mailbox', {"f": tex('mailbox'), "s": tex('fabric_blue')}, [el([3, 2, 3], [13, 13, 13], faces("#f", "#s")), box([3.5, 13, 3.5], [12.5, 15, 12.5], "#s"),
                                                            box([3, 0, 3], [5, 2, 5], "#s"), box([11, 0, 3], [13, 2, 5], "#s"), box([3, 0, 11], [5, 2, 13], "#s"), box([11, 0, 11], [13, 2, 13], "#s")])
fm('newspaper_box', {"f": tex('newsbox_front'), "s": tex('newsbox_side')}, [el([3, 0, 4], [13, 12, 12], faces("#f", "#s"))])
fm('vending_machine', {"b": tex('vending_bottom'), "t": tex('vending_top'), "s": tex('vending_side')}, [
    el([1, 0, 2], [15, 16, 14], faces("#b", "#s")), el([1, 16, 2], [15, 30, 14], faces("#t", "#s"))])
fm('atm', {"f": tex('atm_front'), "s": tex('metal_gray')}, [el([2, 0, 4], [14, 16, 13], faces("#f", "#s")), box([2, 9, 2], [14, 10, 4], "#s")])
fm('parking_meter', {"f": tex('meter_front'), "m": tex('lamp_metal')}, [box([7, 0, 7], [9, 10, 9], "#m"), el([5, 10, 6], [11, 15, 10], faces("#f", "#m")),
                                                                    box([5.5, 15, 6.5], [10.5, 16, 9.5], "#m")])
fm('stop_sign', {"s": tex('stop_sign'), "m": tex('lamp_metal')}, [box([7, 0, 7], [9, 16, 9], "#m"), box([7, 16, 7], [9, 20, 9], "#m"),
                                                               el([2, 12, 6.5], [14, 24, 7], {"north": "#s", "south": "#m"})])
fm('picnic_table', {"w": tex('bench_wood')}, [box([0, 9, 4], [16, 10.5, 12], "#w"), box([0, 5, 0], [16, 6.5, 3], "#w"), box([0, 5, 13], [16, 6.5, 16], "#w"),
                                              box([2, 0, 1], [3.5, 9, 15], "#w"), box([12.5, 0, 1], [14, 9, 15], "#w")])
fm('planter', {"s": tex('planter_stone'), "h": tex('hedge'), "d": "minecraft:block/dirt"}, [el([1, 0, 1], [15, 8, 15], faces("#s", "#s", "#d")), box([2, 8, 2], [14, 15, 14], "#h")])
fm('ac_unit', {"s": tex('ac_side'), "t": tex('ac_top')}, [el([0, 0, 1], [16, 12, 15], faces("#s", "#s", "#t"))])
fm('fountain_spout', {"s": tex('spout_stone'), "w": "minecraft:block/water_still"}, [
    box([5, 0, 5], [11, 8, 11], "#s"), box([3, 8, 3], [13, 10, 13], "#s"), box([6.5, 10, 6.5], [9.5, 13, 9.5], "#s")])
fm('pew', {"w": tex('wood_dark')}, [box([0, 5, 2], [16, 7, 12], "#w"), box([0, 7, 12], [16, 16, 13.5], "#w"),
                                    box([0, 0, 2], [1.5, 12, 14], "#w"), box([14.5, 0, 2], [16, 12, 14], "#w"), box([1.5, 3, 12], [14.5, 4, 13], "#w")])
fm('altar', {"c": tex('altar_cloth'), "t": tex('altar_trim'), "s": tex('spout_stone')}, [
    box([1, 0, 3], [15, 13, 13], "#s"), box([0, 13, 2], [16, 14, 14], "#c"), el([4, 5, 1.9], [12, 13, 2.5], {"north": "#t"})])
fm('gravestone', {"f": tex('gravestone_front'), "s": tex('gravestone_side')}, [
    box([3, 0, 5.5], [13, 2, 10.5], "#s"), el([4, 2, 7], [12, 12, 9], faces("#f", "#s")), el([5, 12, 7], [11, 14, 9], faces("#s", "#s"))])
for n in ('maple_leaves', 'linden_leaves'):
    model(n, {"parent": "minecraft:block/leaves", "textures": {"all": tex(n)}}); simple_state(n); item(n); loot(n)
for n in ('clear_glass', 'reflective_glass'):
    model(n, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(n)}}); simple_state(n); item(n); loot(n)

NEW = ['desk', 'office_chair', 'computer', 'filing_cabinet', 'water_cooler', 'potted_plant', 'sofa', 'tv', 'kitchen_counter', 'fridge', 'stove',
       'store_shelf', 'checkout_counter', 'mailbox', 'newspaper_box', 'vending_machine', 'atm', 'parking_meter', 'stop_sign', 'picnic_table',
       'planter', 'ac_unit', 'fountain_spout', 'pew', 'altar', 'gravestone']
pk = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/pickaxe.json'); tg = json.load(open(pk))
tg['values'] += ['vehicleforge:' + n for n in NEW if n not in ('desk', 'sofa', 'picnic_table', 'pew', 'potted_plant', 'office_chair')]
tg['values'] += ['vehicleforge:garage_door']
json.dump(tg, open(pk, 'w'), indent=2)
wj(os.path.join(RES, 'data/minecraft/tags/blocks/mineable/axe.json'), {"replace": False, "values": ['vehicleforge:' + n for n in ('desk', 'sofa', 'picnic_table', 'pew', 'office_chair')]})
wj(os.path.join(RES, 'data/minecraft/tags/blocks/mineable/hoe.json'), {"replace": False, "values": ['vehicleforge:maple_leaves', 'vehicleforge:linden_leaves']})
wj(os.path.join(RES, 'data/minecraft/tags/blocks/leaves.json'), {"replace": False, "values": ['vehicleforge:maple_leaves', 'vehicleforge:linden_leaves']})
LP = os.path.join(A, 'lang/en_us.json'); lang = json.load(open(LP))
names = {'clear_glass': 'Clear Glass (seamless)', 'reflective_glass': 'Reflective Glass (seamless)', 'desk': 'Desk', 'office_chair': 'Office Chair',
         'computer': 'Computer', 'filing_cabinet': 'Filing Cabinet', 'water_cooler': 'Water Cooler', 'potted_plant': 'Potted Plant', 'sofa': 'Sofa',
         'tv': 'TV', 'kitchen_counter': 'Kitchen Counter', 'fridge': 'Fridge', 'stove': 'Stove', 'store_shelf': 'Store Shelf',
         'checkout_counter': 'Checkout Counter', 'mailbox': 'Mailbox', 'newspaper_box': 'Newspaper Box', 'vending_machine': 'Vending Machine',
         'atm': 'ATM', 'parking_meter': 'Parking Meter', 'stop_sign': 'Stop Sign', 'picnic_table': 'Picnic Table', 'planter': 'Planter',
         'ac_unit': 'Rooftop AC Unit', 'fountain_spout': 'Fountain Spout', 'pew': 'Pew', 'altar': 'Altar', 'gravestone': 'Gravestone',
         'maple_leaves': 'Maple Leaves', 'linden_leaves': 'Linden Leaves'}
for k, v in names.items(): lang['block.vehicleforge.' + k] = v
json.dump(lang, open(LP, 'w'), indent=2)
print('props ok', len(names))
