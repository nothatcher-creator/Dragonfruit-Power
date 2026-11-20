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

# ======================= wave 3: interactive variants, smoke, electricity =======================
save('screen_off', px_img(lambda x, y: (20, 20, 22, 255) if x in (0, 15) or y in (0, 15) else (cl(18 + (6 if x + y == 12 else 0)), cl(20 + (6 if x + y == 12 else 0)), cl(26 + (8 if x + y == 12 else 0)), 255)))
# animated TV picture: 4 frames of a newscast that keeps rolling static
tv = Image.new('RGBA', (16, 64)); d = ImageDraw.Draw(tv)
for f in range(4):
    oy = f * 16
    d.rectangle([0, oy, 15, oy + 15], fill=(16, 16, 18, 255))
    d.rectangle([1, oy + 1, 14, oy + 14], fill=(60, 90, 140, 255))
    d.rectangle([5, oy + 4, 10, oy + 11], fill=(200, 160, 130, 255)); d.rectangle([5, oy + 3, 10, oy + 5], fill=(60, 40, 30, 255))   # anchor
    d.rectangle([1, oy + 11, 14, oy + 14], fill=(170, 30, 30, 255))
    for x in range(1, 15):
        if (x + f * 3) % 5 < 3: d.point((x, oy + 12), fill=(240, 240, 240, 255))
    for k in range(10): d.point((r2.randint(1, 14), oy + r2.randint(1, 10)), fill=(220, 220, 225, 255))
    d.line([(1, oy + 2 + f * 3), (14, oy + 2 + f * 3)], fill=(120, 150, 190, 255))
tv.save(os.path.join(TEX, 'tv_on.png'))
json.dump({"animation": {"frametime": 4}}, open(os.path.join(TEX, 'tv_on.png.mcmeta'), 'w'))
# electricity
gb = solid((226, 186, 40), 5); d = ImageDraw.Draw(gb); d.rectangle([0, 0, 15, 1], fill=(60, 60, 64, 255)); d.rectangle([0, 14, 15, 15], fill=(60, 60, 64, 255))
for yy in range(4, 12, 2): d.line([(10, yy), (14, yy)], fill=(160, 130, 30, 255))
save('generator_body', gb)
for on in (0, 1):
    gf = solid((226, 186, 40), 5); d = ImageDraw.Draw(gf); d.rectangle([2, 3, 13, 12], fill=(50, 52, 56, 255))
    d.ellipse([3, 4, 7, 8], fill=(220, 220, 214, 255)); d.line([(5, 6), (6, 5)], fill=(200, 30, 30, 255))
    d.rectangle([9, 5, 11, 6], fill=(60, 230, 90, 255) if on else (30, 60, 34, 255)); d.rectangle([9, 9, 12, 10], fill=(150, 30, 30, 255))
    save(f'generator_front_{on}', gf)
sc = solid((24, 50, 110), 3); d = ImageDraw.Draw(sc)
for k in range(0, 16, 4): d.line([(k, 0), (k, 15)], fill=(160, 180, 200, 255)); d.line([(0, k), (15, k)], fill=(160, 180, 200, 255))
d.line([(1, 14), (14, 1)], fill=(70, 110, 170, 255)); save('solar_cells', sc)
save('concrete_light', solid((190, 190, 186), 4))
for lv in range(4):
    bf = solid((80, 84, 90), 3); d = ImageDraw.Draw(bf); d.rectangle([3, 2, 12, 13], fill=(30, 30, 34, 255))
    for k in range(3):
        col = (70, 210, 90) if k < lv else (40, 50, 44)
        d.rectangle([5, 10 - k * 3, 10, 11 - k * 3], fill=col + (255,))
    d.rectangle([6, 0, 9, 1], fill=(200, 40, 40, 255))
    save(f'battery_front_{lv}', bf)
save('pole_wood', px_img(lambda x, y: (cl(96 + (10 if x % 5 == 0 else 0) + r2.randint(-4, 4)), cl(74 + r2.randint(-3, 3)), cl(52 + r2.randint(-3, 3)), 255)))
save('insulator', solid((210, 214, 208), 3))
for on in (0, 1):
    el_ = solid((90, 92, 96), 2); d = ImageDraw.Draw(el_)
    d.rectangle([2, 2, 13, 13], fill=(255, 250, 225, 255) if on else (120, 122, 118, 255))
    if on: d.rectangle([4, 4, 11, 11], fill=(255, 255, 245, 255))
    for k in (5, 10): d.line([(2, k), (13, k)], fill=(70, 72, 76, 255))
    save(f'electric_light_{on}', el_)
ch = px_img(lambda x, y: (cl(30 + r2.randint(-8, 8)), cl(26 + r2.randint(-6, 6)), cl(24 + r2.randint(-6, 6)), 255) if r2.random() > 0.07 else (255, cl(120 + r2.randint(0, 80)), 30, 255))
save('charred', ch)
vg = solid((90, 92, 94), 3); d = ImageDraw.Draw(vg)
for x in range(2, 15, 3): d.line([(x, 1), (x, 14)], fill=(20, 20, 22, 255))
save('vent_grate', vg)
os.makedirs(os.path.join(A, 'textures/entity'), exist_ok=True)
solid((236, 238, 240), 2).save(os.path.join(A, 'textures/entity/wind_turbine.png'))

def facing_lit(name, off, on, key='lit'):
    v = {}
    for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
        for val, m in (('false', off), ('true', on)):
            e = {"model": "vehicleforge:block/" + m}
            if y: e["y"] = y
            v[f"facing={f},{key}={val}"] = e
    wj(os.path.join(A, 'blockstates', name + '.json'), {"variants": v})
# computer / tv on-off
model('computer_on', json.load(open(os.path.join(A, 'models/block/computer.json'))))
cm = json.load(open(os.path.join(A, 'models/block/computer.json'))); cm['textures']['s'] = tex('screen_off'); model('computer', cm)
facing_lit('computer', 'computer', 'computer_on')
tm = json.load(open(os.path.join(A, 'models/block/tv.json'))); model('tv', tm)
tm2 = json.loads(json.dumps(tm)); tm2['textures']['s'] = tex('tv_on'); model('tv_on', tm2)
facing_lit('tv', 'tv', 'tv_on')
# smoke and steam
model('smoke_source', {"parent": "minecraft:block/block", "textures": {"particle": tex('charred'), "c": tex('charred')},
    "elements": [box([2, 0, 3], [14, 2.5, 6], "#c", {"origin": [8, 1, 4.5], "axis": "y", "angle": 22.5}),
                 box([3, 0, 9], [13, 2.5, 12], "#c", {"origin": [8, 1, 10.5], "axis": "y", "angle": -22.5}),
                 box([5, 2, 5], [11, 4, 11], "#c")]})
simple_state('smoke_source'); item('smoke_source'); loot('smoke_source')
model('steam_vent', {"parent": "minecraft:block/block", "textures": {"particle": tex('vent_grate'), "g": tex('vent_grate')},
    "elements": [box([1, 0, 1], [15, 1, 15], "#g")]})
simple_state('steam_vent'); item('steam_vent'); loot('steam_vent')
# electricity
for on in (0, 1):
    model(f'generator_{on}', {"parent": "minecraft:block/block", "textures": {"particle": tex('generator_body'), "b": tex('generator_body'),
        "f": tex(f'generator_front_{on}'), "m": tex('lamp_metal')},
        "elements": [el([1, 1, 2], [15, 11, 14], faces("#f", "#b")), box([1, 0, 3], [3, 1, 13], "#m"), box([13, 0, 3], [15, 1, 13], "#m"),
                     box([2, 11, 4], [14, 12, 12], "#m"), box([3, 9, 13.5], [5, 15, 15.5], "#m"), box([10, 11, 6], [13, 13, 9], "#b")]})
facing_lit('generator', 'generator_0', 'generator_1', 'powered')
item('generator', 'generator_0'); loot('generator')
fm('solar_panel', {"c": tex('solar_cells'), "m": tex('metal_gray')}, [
    box([7, 0, 7], [9, 5, 9], "#m"), box([4, 0, 4], [12, 1, 12], "#m"),
    el([0, 5, 0], [16, 6.5, 16], {"up": "#c", "down": "#m", "north": "#m", "south": "#m", "east": "#m", "west": "#m"}, {"origin": [8, 6, 8], "axis": "x", "angle": -22.5})])
fm("wind_turbine", {"c": tex("concrete_light")}, [
    box([1, 0, 1], [15, 4, 15], "#c"), box([2, 4, 2], [14, 6, 14], "#c"), box([4.8, 6, 4.8], [11.2, 16, 11.2], "#c")])
for lv in range(4):
    model(f'battery_bank_{lv}', {"parent": "minecraft:block/block", "textures": {"particle": tex('metal_gray'), "f": tex(f'battery_front_{lv}'), "s": tex('metal_gray')},
        "elements": [el([1, 0, 3], [15, 14, 13], faces("#f", "#s"))]})
v = {}
for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
    for lv in range(4):
        e = {"model": f"vehicleforge:block/battery_bank_{lv}"}
        if y: e["y"] = y
        v[f"facing={f},level={lv}"] = e
wj(os.path.join(A, 'blockstates/battery_bank.json'), {"variants": v}); item('battery_bank', 'battery_bank_3'); loot('battery_bank')
fm('power_pole', {"p": tex('pole_wood'), "i": tex('insulator'), "m": tex('lamp_metal')}, [
    box([6, 0, 6], [10, 32, 10], "#p"), box([0, 26, 7], [16, 28, 9], "#p"), box([-6, 26, 7], [0, 28, 9], "#p"), box([16, 26, 7], [22, 28, 9], "#p"),
    box([-4, 28, 7.25], [-2.5, 31, 8.75], "#i"), box([3, 28, 7.25], [4.5, 31, 8.75], "#i"), box([11.5, 28, 7.25], [13, 31, 8.75], "#i"),
    box([18.5, 28, 7.25], [20, 31, 8.75], "#i"), box([9.5, 18, 5], [13.5, 23, 11], "#m")])
for on in (0, 1):
    model(f'electric_light_{on}', {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex(f'electric_light_{on}'), "bottom": tex(f'electric_light_{on}'), "side": tex('metal_gray')}})
wj(os.path.join(A, 'blockstates/electric_light.json'), {"variants": {"lit=false": {"model": "vehicleforge:block/electric_light_0"}, "lit=true": {"model": "vehicleforge:block/electric_light_1"}}})
item('electric_light', 'electric_light_1'); loot('electric_light')
# particles
P = os.path.join(A, 'particles'); os.makedirs(P, exist_ok=True)
for n in ('dark_smoke', 'steam'):
    json.dump({"textures": [f"minecraft:big_smoke_{i}" for i in range(12)]}, open(os.path.join(P, n + '.json'), 'w'), indent=2)
# loot for the props
def entry(item_, w_, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": item_, "weight": w_}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e
def table(path, rolls, entries):
    wj(os.path.join(D, 'loot_tables', path + '.json'), {"type": "minecraft:chest", "pools": [{"rolls": rolls, "entries": entries}]})
table('props/vending', 1, [entry("minecraft:cookie", 6, 1, 3), entry("minecraft:apple", 4), entry("minecraft:bread", 3), entry("minecraft:honey_bottle", 2),
                           entry("minecraft:dried_kelp", 3, 2, 4), entry("minecraft:melon_slice", 3, 1, 2), entry("minecraft:pumpkin_pie", 1)])
table('city/fridge', {"type": "minecraft:uniform", "min": 1, "max": 4}, [{"type": "minecraft:empty", "weight": 6}, entry("minecraft:apple", 4, 1, 3),
      entry("minecraft:carrot", 3, 1, 4), entry("minecraft:cooked_beef", 2, 1, 2), entry("minecraft:beef", 3, 1, 3), entry("minecraft:chicken", 3, 1, 2),
      entry("minecraft:egg", 3, 1, 6), entry("minecraft:milk_bucket", 1), entry("minecraft:cake", 1), entry("minecraft:melon_slice", 3, 2, 5),
      entry("minecraft:rotten_flesh", 4, 1, 3)])
table('city/kitchen', {"type": "minecraft:uniform", "min": 0, "max": 3}, [{"type": "minecraft:empty", "weight": 6}, entry("minecraft:bowl", 4, 1, 3),
      entry("minecraft:glass_bottle", 5, 1, 3), entry("minecraft:sugar", 3, 1, 4), entry("minecraft:wheat", 2, 1, 3), entry("minecraft:bread", 2),
      entry("minecraft:cocoa_beans", 1, 1, 3)])
table('city/mail', {"type": "minecraft:uniform", "min": 0, "max": 3}, [{"type": "minecraft:empty", "weight": 4}, entry("minecraft:paper", 8, 1, 6),
      entry("minecraft:book", 3), entry("minecraft:map", 2), entry("minecraft:writable_book", 1), entry("minecraft:name_tag", 1)])
NEW3 = ['smoke_source', 'steam_vent', 'generator', 'solar_panel', 'wind_turbine', 'battery_bank', 'power_pole', 'electric_light']
pk = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/pickaxe.json'); tg = json.load(open(pk))
tg['values'] += ['vehicleforge:' + n for n in NEW3 if n not in ('power_pole', 'smoke_source')]
json.dump(tg, open(pk, 'w'), indent=2)
ax = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/axe.json'); ta = json.load(open(ax)); ta['values'] += ['vehicleforge:power_pole', 'vehicleforge:smoke_source']
json.dump(ta, open(ax, 'w'), indent=2)
lang = json.load(open(LP))
lang.update({'block.vehicleforge.smoke_source': 'Smoke Source', 'block.vehicleforge.steam_vent': 'Steam Vent', 'block.vehicleforge.generator': 'Generator',
             'block.vehicleforge.solar_panel': 'Solar Panel', 'block.vehicleforge.wind_turbine': 'Wind Turbine', 'block.vehicleforge.battery_bank': 'Battery Bank',
             'block.vehicleforge.power_pole': 'Power Pole', 'block.vehicleforge.electric_light': 'Electric Light', 'entity.vehicleforge.seat': 'Seat',
             'msg.vehicleforge.no_power': 'No power. Hook it up to a generator, solar panel or wind turbine (within 16 blocks, or chain power poles).',
             'msg.vehicleforge.sold_out': 'Sold out.', 'msg.vehicleforge.atm_empty': 'The ATM is empty.',
             'msg.vehicleforge.atm': 'Cash out. Not that it\'s worth much any more.',
             'msg.vehicleforge.stove_hint': 'Hold raw food and use the stove to cook it.',
             'msg.vehicleforge.meter_expired': 'EXPIRED', 'msg.vehicleforge.meter_paid': '00:12 remaining. Someone paid.',
             'msg.vehicleforge.generator_fuel': 'Generator running - fuel %s%%', 'msg.vehicleforge.generator_empty': 'Out of fuel. Add coal, charcoal, lava or a fuel can.',
             'msg.vehicleforge.battery': 'Battery charge: %s%%', 'msg.vehicleforge.grid_live': 'Power: live', 'msg.vehicleforge.grid_dead': 'Power: dead'})
json.dump(lang, open(LP, 'w'), indent=2)
print('wave 3 ok')

# ======================= wave 4: secrets, suitcase, airport marker, new loot =======================
def secret(name, closed_model):
    model(name + '_open', {"textures": {"particle": closed_model[1]}, "elements": []})
    wj(os.path.join(A, 'blockstates', name + '.json'), {"variants": {"open=false": {"model": closed_model[0]}, "open=true": {"model": "vehicleforge:block/" + name + "_open"}}})
    wj(os.path.join(A, 'models/item', name + '.json'), {"parent": closed_model[0]})
    loot(name)
secret('secret_bookshelf', ("minecraft:block/bookshelf", "minecraft:block/bookshelf"))
secret('secret_quartz', ("minecraft:block/smooth_quartz", "minecraft:block/quartz_block_bottom"))
secret('secret_stone', ("minecraft:block/smooth_stone", "minecraft:block/smooth_stone"))
su = solid((40, 60, 110), 5); d = ImageDraw.Draw(su); d.line([(0, 8), (15, 8)], fill=(200, 200, 200, 255))
d.rectangle([1, 1, 14, 14], outline=(26, 40, 76, 255)); d.rectangle([6, 3, 9, 5], fill=(30, 30, 34, 255)); save('suitcase', su)
fm('suitcase', {"s": tex('suitcase'), "h": tex('fabric_black')}, [box([2, 0, 5], [14, 10, 11], "#s"), box([6, 10, 7.5], [10, 11.5, 8.5], "#h"),
                                                                box([2.5, 0, 5.5], [3.5, 0.5, 6.5], "#h"), box([12.5, 0, 5.5], [13.5, 0.5, 6.5], "#h")])
model('airport_marker', {"parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/dirt"}}); simple_state('airport_marker'); item('airport_marker')
wj(os.path.join(D, 'loot_tables/blocks/airport_marker.json'), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:dirt"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
def ench(item_, w_):
    return {"type": "minecraft:item", "name": item_, "weight": w_, "functions": [{"function": "minecraft:enchant_randomly"}]}
table('city/bunker', {"type": "minecraft:uniform", "min": 3, "max": 6}, [entry("minecraft:bread", 6, 2, 6), entry("minecraft:cooked_beef", 5, 2, 5),
      entry("minecraft:golden_carrot", 3, 2, 6), entry("minecraft:torch", 4, 4, 16), entry("minecraft:arrow", 4, 8, 24), entry("minecraft:iron_ingot", 4, 2, 8),
      entry("minecraft:coal", 4, 4, 12), entry("minecraft:potion", 2), entry("minecraft:compass", 1), entry("minecraft:bucket", 2)])
table('city/bunker_vault', {"type": "minecraft:uniform", "min": 3, "max": 6}, [entry("minecraft:diamond", 3, 1, 4), entry("minecraft:gold_ingot", 5, 2, 8),
      entry("minecraft:emerald", 4, 2, 10), entry("minecraft:golden_apple", 3), entry("minecraft:enchanted_golden_apple", 1), ench("minecraft:iron_sword", 2),
      ench("minecraft:diamond_chestplate", 1), entry("minecraft:totem_of_undying", 1), entry("minecraft:experience_bottle", 3, 4, 12), entry("minecraft:tnt", 2, 1, 4)])
table('city/crypt', {"type": "minecraft:uniform", "min": 3, "max": 7}, [entry("minecraft:gold_ingot", 6, 2, 8), entry("minecraft:gold_nugget", 6, 6, 20),
      ench("minecraft:book", 4), entry("minecraft:golden_apple", 2), entry("minecraft:bone", 6, 2, 8), entry("minecraft:candle", 4, 1, 4),
      entry("minecraft:ender_pearl", 2, 1, 3), entry("minecraft:diamond", 1, 1, 2), ench("minecraft:golden_sword", 2)])
table('city/armory', {"type": "minecraft:uniform", "min": 3, "max": 6}, [ench("minecraft:iron_sword", 3), entry("minecraft:iron_chestplate", 3),
      entry("minecraft:iron_helmet", 3), entry("minecraft:crossbow", 3), entry("minecraft:arrow", 6, 16, 32), entry("minecraft:tnt", 3, 2, 6),
      entry("minecraft:shield", 3), entry("minecraft:iron_ingot", 4, 4, 12), entry("minecraft:golden_apple", 1), entry("minecraft:spyglass", 2)])
table('city/luggage', {"type": "minecraft:uniform", "min": 1, "max": 4}, [{"type": "minecraft:empty", "weight": 3}, entry("minecraft:leather_chestplate", 3),
      entry("minecraft:leather_leggings", 3), entry("minecraft:leather_boots", 3), entry("minecraft:paper", 4, 1, 4), entry("minecraft:book", 2),
      entry("minecraft:cookie", 3, 1, 4), entry("minecraft:clock", 1), entry("minecraft:emerald", 1, 1, 3), entry("minecraft:map", 1)])
table('city/farm', {"type": "minecraft:uniform", "min": 2, "max": 5}, [entry("minecraft:wheat", 6, 4, 16), entry("minecraft:wheat_seeds", 5, 4, 12),
      entry("minecraft:carrot", 4, 2, 8), entry("minecraft:potato", 4, 2, 8), entry("minecraft:apple", 4, 2, 6), entry("minecraft:egg", 3, 2, 8),
      entry("minecraft:iron_hoe", 2), entry("minecraft:shears", 2), entry("minecraft:lead", 2, 1, 2), entry("minecraft:bone_meal", 3, 4, 12)])
table('city/cabin', {"type": "minecraft:uniform", "min": 2, "max": 5}, [entry("minecraft:cooked_porkchop", 4, 2, 5), entry("minecraft:arrow", 5, 6, 20),
      entry("minecraft:bow", 3), entry("minecraft:leather", 4, 2, 6), entry("minecraft:rabbit_hide", 3, 2, 6), entry("minecraft:torch", 4, 4, 12),
      entry("minecraft:iron_axe", 2), entry("minecraft:fishing_rod", 2), entry("minecraft:sweet_berries", 4, 4, 12)])
lang = json.load(open(LP))
lang.update({'block.vehicleforge.secret_bookshelf': 'Bookshelf', 'block.vehicleforge.secret_quartz': 'Smooth Quartz Block',
             'block.vehicleforge.secret_stone': 'Smooth Stone', 'block.vehicleforge.suitcase': 'Suitcase', 'block.vehicleforge.airport_marker': 'Dirt'})
json.dump(lang, open(LP, 'w'), indent=2)
pk = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/pickaxe.json'); tg = json.load(open(pk)); tg['values'] += ['vehicleforge:secret_quartz', 'vehicleforge:secret_stone']
json.dump(tg, open(pk, 'w'), indent=2)
ax_ = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/axe.json'); ta = json.load(open(ax_)); ta['values'] += ['vehicleforge:secret_bookshelf']
json.dump(ta, open(ax_, 'w'), indent=2)
sh_ = os.path.join(RES, 'data/minecraft/tags/blocks/mineable/shovel.json'); wj(sh_, {"replace": False, "values": ['vehicleforge:airport_marker']})
print('wave 4 ok')
