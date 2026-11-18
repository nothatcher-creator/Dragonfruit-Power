"""16x16 side-view icons for every vehicle item + RV door block + mod icon."""
import os, sys
from PIL import Image, ImageDraw
ROOT = sys.argv[1]
ITEM = os.path.join(ROOT, 'textures/item'); BLOCK = os.path.join(ROOT, 'textures/block')
os.makedirs(ITEM, exist_ok=True); os.makedirs(BLOCK, exist_ok=True)
def hexc(h): h=h.lstrip('#'); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), 255)
def dark(c, f=0.6): return (int(c[0]*f), int(c[1]*f), int(c[2]*f), 255)
BLK=(25,25,28,255); GLS=(150,210,240,255); CHR=(200,200,210,255); WHT=(240,240,240,255); RED=(210,30,30,255); YEL=(255,230,150,255)
def wheel(d, x, y, r=2):
    d.ellipse([x-r, y-r, x+r, y+r], fill=BLK); d.point((x, y), fill=CHR)
def make(name, paint, kind):
    img = Image.new('RGBA', (16,16), (0,0,0,0)); d = ImageDraw.Draw(img); p = hexc(paint); pd = dark(p)
    if kind == 'car':
        d.rectangle([1,8,14,11], fill=p); d.polygon([(4,8),(6,5),(10,5),(12,8)], fill=p); d.polygon([(5,8),(6,6),(8,6),(8,8)], fill=GLS); d.polygon([(9,8),(9,6),(10,6),(11,8)], fill=GLS)
        d.point((14,9), fill=YEL); d.point((1,9), fill=RED); wheel(d,4,12); wheel(d,11,12)
    elif kind == 'classic':
        d.rectangle([0,8,15,11], fill=p); d.polygon([(4,8),(6,5),(10,5),(11,8)], fill=p); d.polygon([(5,8),(6,6),(9,6),(10,8)], fill=GLS)
        d.polygon([(0,8),(2,6),(3,8)], fill=p); d.line([(0,10),(15,10)], fill=CHR); wheel(d,4,12); wheel(d,12,12)
    elif kind == 'sports':
        d.polygon([(0,10),(2,8),(6,7),(9,5),(11,7),(15,9),(15,11),(0,11)], fill=p); d.polygon([(7,7),(9,6),(10,7)], fill=GLS)
        d.line([(0,7),(2,7)], fill=pd); wheel(d,3,12); wheel(d,12,12)
    elif kind == 'pickup':
        d.rectangle([1,7,15,11], fill=p); d.rectangle([7,4,11,7], fill=p); d.rectangle([8,5,10,7], fill=GLS); d.rectangle([1,6,6,7], fill=pd); wheel(d,4,12); wheel(d,12,12)
    elif kind == 'van':
        d.rectangle([1,4,13,11], fill=p); d.polygon([(13,4),(15,8),(15,11),(13,11)], fill=p); d.polygon([(13,5),(14,8),(13,8)], fill=GLS); d.rectangle([3,6,6,7], fill=GLS); d.rectangle([8,6,11,7], fill=GLS); wheel(d,4,12); wheel(d,12,12)
    elif kind == 'semi':
        d.rectangle([8,3,13,11], fill=p); d.rectangle([13,6,15,11], fill=p); d.rectangle([11,4,13,7], fill=GLS); d.rectangle([0,9,8,10], fill=dark(p,0.3)); d.line([(8,0),(8,4)], fill=CHR); wheel(d,3,12); wheel(d,6,12); wheel(d,13,12)
    elif kind == 'fire':
        d.rectangle([0,5,15,11], fill=p); d.rectangle([11,3,15,7], fill=p); d.rectangle([12,4,14,6], fill=GLS); d.line([(0,4),(10,4)], fill=CHR); d.point((12,2), fill=RED); d.point((14,2), fill=(40,80,255,255)); d.line([(1,8),(10,8)], fill=WHT); wheel(d,3,12); wheel(d,12,12)
    elif kind == 'bicycle':
        wheel(d,3,11,3); wheel(d,12,11,3); d.ellipse([0,8,6,14], outline=BLK); d.ellipse([9,8,15,14], outline=BLK)
        d.line([(3,11),(7,11),(10,6),(5,6),(3,11)], fill=p); d.line([(10,6),(12,11)], fill=CHR); d.line([(5,5),(6,5)], fill=BLK); d.line([(10,4),(11,6)], fill=BLK)
    elif kind in ('motorbike', 'dirt'):
        r = 3; wheel(d,3,12,r); wheel(d,12,12,r); d.polygon([(4,9),(7,7),(11,7),(12,9),(9,11),(5,11)], fill=p); d.rectangle([3,7,7,8], fill=BLK); d.line([(11,7),(12,12)], fill=CHR); d.point((13,7), fill=YEL)
        if kind == 'dirt': d.line([(10,6),(14,7)], fill=p)
    elif kind == 'jeep':
        d.rectangle([1,7,15,11], fill=p); d.rectangle([3,4,10,5], fill=dark(p,0.8)); d.line([(10,5),(10,7)], fill=BLK); d.line([(4,3),(8,3)], fill=BLK); wheel(d,4,12,3); wheel(d,12,12,3)
    elif kind == 'tank':
        d.rounded_rectangle([0,9,15,13], 2, fill=(40,40,40,255)); d.rectangle([1,7,14,9], fill=p); d.rectangle([4,4,10,7], fill=p); d.line([(10,5),(15,5)], fill=pd, width=1)
        for x in range(2,15,3): d.point((x,11), fill=CHR)
    elif kind == 'artillery':
        d.rectangle([0,8,15,11], fill=p); d.rectangle([12,5,15,8], fill=p); d.rectangle([13,6,14,7], fill=GLS); d.polygon([(1,8),(4,3),(9,3),(9,8)], fill=pd)
        for y in (4,6): d.line([(5,y),(8,y)], fill=BLK)
        wheel(d,3,12); wheel(d,7,12); wheel(d,13,12)
    elif kind == 'heli':
        d.ellipse([5,6,13,11], fill=p); d.polygon([(10,7),(13,8),(12,10),(10,10)], fill=GLS); d.line([(0,8),(6,8)], fill=p); d.line([(0,6),(0,9)], fill=p); d.line([(1,4),(15,4)], fill=BLK); d.line([(9,4),(9,6)], fill=BLK); d.line([(5,13),(13,13)], fill=BLK)
    elif kind == 'prop':
        d.polygon([(1,8),(13,7),(14,9),(1,10)], fill=p); d.line([(5,9),(10,9)], fill=pd, width=1); d.polygon([(7,7),(9,5),(10,7)], fill=GLS); d.polygon([(0,6),(2,8),(1,8)], fill=p); d.line([(15,5),(15,11)], fill=BLK)
    elif kind == 'jet':
        d.polygon([(0,9),(4,7),(12,7),(15,8),(12,9),(4,10)], fill=p); d.polygon([(5,9),(9,9),(4,12)], fill=pd); d.polygon([(1,8),(1,4),(3,8)], fill=p); d.polygon([(9,7),(11,6),(12,7)], fill=GLS); d.point((0,9), fill=(255,140,30,255))
    elif kind == 'rocket':
        d.rectangle([6,4,9,12], fill=p); d.polygon([(6,4),(7,1),(8,1),(9,4)], fill=RED); d.polygon([(6,10),(4,13),(6,13)], fill=RED); d.polygon([(9,10),(11,13),(9,13)], fill=RED); d.point((7,6), fill=GLS); d.point((8,6), fill=GLS); d.line([(7,13),(8,15)], fill=(255,150,30,255))
    elif kind == 'rv':
        d.rectangle([0,4,13,11], fill=p); d.polygon([(13,6),(15,8),(15,11),(13,11)], fill=p); d.rectangle([13,7,14,8], fill=GLS); d.rectangle([2,6,4,7], fill=GLS); d.rectangle([7,6,9,7], fill=GLS); d.line([(0,9),(13,9)], fill=(60,90,140,255)); d.rectangle([10,6,11,10], fill=(140,100,60,255)); wheel(d,3,12); wheel(d,11,12)
    elif kind == 'train':
        d.rectangle([1,4,5,11], fill=p); d.rectangle([5,6,13,11], fill=p); d.rectangle([11,3,12,6], fill=BLK); d.rectangle([2,5,4,7], fill=GLS); d.polygon([(13,9),(15,12),(13,12)], fill=RED); wheel(d,4,12); wheel(d,8,12); wheel(d,11,12)
    elif kind == 'carriage':
        d.rectangle([0,5,15,11], fill=p); d.rectangle([0,7,15,9], fill=WHT)
        for x in range(1,15,3): d.rectangle([x,7,x+1,8], fill=GLS)
        wheel(d,3,12); wheel(d,12,12)
    elif kind == 'speedboat':
        d.polygon([(0,8),(15,8),(12,12),(2,12)], fill=WHT); d.line([(1,9),(14,9)], fill=p); d.polygon([(8,8),(10,5),(11,8)], fill=GLS); d.rectangle([0,6,1,10], fill=BLK)
    elif kind == 'yacht':
        d.polygon([(0,8),(15,7),(13,12),(2,12)], fill=WHT); d.line([(1,10),(14,10)], fill=(27,42,74,255)); d.rectangle([4,5,11,8], fill=WHT); d.rectangle([5,6,10,7], fill=GLS); d.line([(7,2),(7,5)], fill=BLK)
    img.save(os.path.join(ITEM, name + '.png'))
specs = [('classic_cruiser','#5FB8B0','classic'),('sedan','#2E4E8F','car'),('sports_car','#E0301E','sports'),('pickup_truck','#3C6E3A','pickup'),
 ('van','#F2F2F2','van'),('semi_truck','#B0122A','semi'),('fire_truck','#C81E1E','fire'),('bicycle','#2A9DF4','bicycle'),('motorbike','#262626','motorbike'),
 ('dirt_bike','#F07A10','dirt'),('army_jeep','#5B6B3A','jeep'),('tank','#4F5D33','tank'),('rocket_artillery','#55643A','artillery'),('helicopter','#1E2A3A','heli'),
 ('prop_plane','#E8C21A','prop'),('jet','#7E8A96','jet'),('rocket_ship','#F2F2F2','rocket'),('rv','#F2EEE2','rv'),('steam_train','#1E3A2A','train'),
 ('train_carriage','#7A1E1E','carriage'),('speedboat','#E0301E','speedboat'),('yacht','#FAFAFA','yacht')]
for s in specs: make(*s)
door = Image.new('RGBA', (16,16), (120,84,48,255)); dd = ImageDraw.Draw(door)
dd.rectangle([0,0,15,15], outline=(70,48,28,255)); dd.rectangle([3,2,12,7], fill=(150,210,240,255)); dd.rectangle([11,9,12,10], fill=(220,200,80,255)); door.save(os.path.join(BLOCK, 'rv_door.png'))
icon = Image.new('RGBA', (128,128), (24,26,34,255))
for i, n in enumerate(['sports_car','helicopter','tank','rv']):
    im = Image.open(os.path.join(ITEM, n + '.png')).resize((60,60), Image.NEAREST)
    icon.alpha_composite(im, (4 + (i % 2) * 62, 4 + (i // 2) * 62))
icon.save(os.path.join(ROOT, 'icon.png'))
prev = Image.new('RGBA', (len(specs)*68+8, 76), (50,50,58,255))
for i, s in enumerate(specs): prev.alpha_composite(Image.open(os.path.join(ITEM, s[0] + '.png')).resize((64,64), Image.NEAREST), (6 + i*68, 6))
prev.save('/tmp/vicons.png'); print('icons ok')
