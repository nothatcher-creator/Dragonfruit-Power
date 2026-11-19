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
    elif kind == 'police':
        d.rectangle([1,8,14,11], fill=p); d.polygon([(4,8),(6,5),(10,5),(12,8)], fill=p); d.polygon([(5,8),(6,6),(8,6),(8,8)], fill=GLS); d.polygon([(9,8),(9,6),(10,6),(11,8)], fill=GLS)
        d.rectangle([5,9,10,10], fill=BLK); d.point((7,4), fill=RED); d.point((8,4), fill=(40,80,255,255)); d.point((14,9), fill=YEL); wheel(d,4,12); wheel(d,11,12)
    elif kind == 'taxi':
        d.rectangle([1,8,14,11], fill=p); d.polygon([(4,8),(6,5),(10,5),(12,8)], fill=p); d.polygon([(5,8),(6,6),(8,6),(8,8)], fill=GLS); d.polygon([(9,8),(9,6),(10,6),(11,8)], fill=GLS)
        d.rectangle([7,3,8,4], fill=WHT)
        for x in range(1,15,2): d.point((x,9), fill=BLK)
        wheel(d,4,12); wheel(d,11,12)
    elif kind == 'hatch':
        d.rectangle([2,8,13,11], fill=p); d.polygon([(3,8),(4,5),(10,5),(12,8)], fill=p); d.polygon([(4,8),(5,6),(7,6),(7,8)], fill=GLS); d.polygon([(8,8),(8,6),(10,6),(11,8)], fill=GLS); wheel(d,4,12); wheel(d,11,12)
    elif kind == 'suv':
        d.rectangle([1,6,14,11], fill=p); d.rectangle([2,3,11,6], fill=p); d.rectangle([3,4,6,6], fill=GLS); d.rectangle([7,4,10,6], fill=GLS); d.line([(2,2),(10,2)], fill=CHR); wheel(d,4,12,3); wheel(d,11,12,3)
    elif kind == 'muscle':
        d.rectangle([0,8,15,11], fill=p); d.polygon([(5,8),(7,5),(10,5),(11,8)], fill=p); d.polygon([(7,8),(8,6),(10,6),(10,8)], fill=GLS); d.line([(11,8),(15,8)], fill=WHT); d.rectangle([12,7,13,7], fill=BLK); wheel(d,3,12); wheel(d,12,12,3)
    elif kind == 'roadster':
        d.rectangle([2,8,13,10], fill=p); d.rectangle([10,6,14,9], fill=p); d.line([(9,4),(9,8)], fill=CHR); d.rectangle([2,7,6,8], fill=(70,40,20,255)); d.point((15,7), fill=YEL)
        wheel(d,4,12,3); wheel(d,12,12,3); d.line([(4,9),(11,9)], fill=BLK)
    elif kind == 'limo':
        d.rectangle([0,8,15,11], fill=p); d.polygon([(2,8),(3,6),(12,6),(13,8)], fill=p)
        for x in (3,6,9,11): d.rectangle([x,7,x+1,7], fill=(90,100,120,255))
        d.line([(0,10),(15,10)], fill=CHR); wheel(d,2,12); wheel(d,13,12)
    elif kind == 'atv':
        d.rectangle([4,8,11,10], fill=p); d.rectangle([2,7,5,8], fill=BLK); d.rectangle([10,7,13,8], fill=BLK); d.line([(10,7),(11,4)], fill=BLK); d.line([(9,4),(12,4)], fill=BLK); d.rectangle([6,7,9,7], fill=BLK); wheel(d,3,12,3); wheel(d,12,12,3)
    elif kind == 'ambulance':
        d.rectangle([1,4,13,11], fill=p); d.polygon([(13,6),(15,8),(15,11),(13,11)], fill=p); d.polygon([(13,7),(14,8),(13,8)], fill=GLS); d.line([(1,8),(15,8)], fill=RED)
        d.line([(6,5),(6,7)], fill=RED); d.line([(5,6),(7,6)], fill=RED); d.point((12,3), fill=RED); d.point((13,3), fill=(40,80,255,255)); wheel(d,4,12); wheel(d,12,12)
    elif kind == 'bus':
        d.rectangle([0,3,15,11], fill=p)
        for x in range(1,14,3): d.rectangle([x,5,x+1,7], fill=GLS)
        d.rectangle([14,5,15,9], fill=GLS); d.line([(0,9),(15,9)], fill=WHT); wheel(d,3,12); wheel(d,12,12)
    elif kind == 'schoolbus':
        d.rectangle([0,4,12,11], fill=p); d.rectangle([12,7,15,11], fill=p)
        for x in range(1,12,3): d.rectangle([x,5,x+1,7], fill=GLS)
        d.line([(0,9),(15,9)], fill=BLK); d.point((1,8), fill=RED); wheel(d,3,12); wheel(d,13,12)
    elif kind == 'dump':
        d.rectangle([11,4,14,11], fill=p); d.rectangle([12,5,13,7], fill=GLS); d.polygon([(0,4),(10,4),(10,10),(1,10)], fill=dark(p,0.7)); d.polygon([(1,4),(4,2),(8,2),(10,4)], fill=(60,60,60,255)); wheel(d,3,12); wheel(d,7,12); wheel(d,13,12)
    elif kind == 'tractor':
        d.rectangle([7,6,14,10], fill=p); d.rectangle([2,2,8,3], fill=p); d.line([(3,3),(3,8)], fill=BLK); d.line([(7,3),(7,8)], fill=BLK); d.rectangle([4,4,6,7], fill=GLS); d.line([(12,2),(12,6)], fill=BLK)
        wheel(d,4,11,4); wheel(d,13,12,2)
    elif kind == 'monster':
        d.rectangle([2,4,13,7], fill=p); d.rectangle([6,2,10,4], fill=p); d.rectangle([7,3,9,4], fill=GLS); d.line([(4,7),(4,9)], fill=CHR); d.line([(11,7),(11,9)], fill=CHR); wheel(d,4,11,4); wheel(d,11,11,4)
    elif kind == 'scooter':
        wheel(d,3,12,2); wheel(d,12,12,2); d.polygon([(1,10),(2,7),(6,7),(6,10)], fill=p); d.rectangle([6,10,11,11], fill=p); d.line([(11,10),(12,5)], fill=CHR); d.rectangle([2,6,6,6], fill=BLK); d.line([(11,5),(13,5)], fill=BLK)
    elif kind == 'apc':
        d.polygon([(0,6),(12,6),(15,9),(15,11),(0,11)], fill=p); d.rectangle([5,4,8,6], fill=p); d.line([(8,5),(13,5)], fill=BLK); d.line([(11,7),(13,8)], fill=GLS)
        for x in (2,6,10,13): wheel(d,x,12)
    elif kind == 'airliner':
        d.rounded_rectangle([1,6,15,9], 2, fill=p); d.line([(3,7),(13,7)], fill=GLS); d.polygon([(5,9),(10,9),(4,13)], fill=dark(p,0.8)); d.polygon([(1,6),(1,2),(4,6)], fill=(40,80,160,255)); d.rectangle([6,10,8,11], fill=(150,150,160,255)); d.point((14,7), fill=GLS)
    elif kind == 'jetski':
        d.polygon([(0,10),(15,9),(13,12),(2,12)], fill=WHT); d.polygon([(4,10),(11,9),(12,7),(8,8)], fill=p); d.rectangle([3,8,7,9], fill=BLK); d.line([(10,7),(11,5)], fill=BLK)
    elif kind == 'sub':
        d.rounded_rectangle([0,6,13,11], 3, fill=p); d.ellipse([11,6,15,11], fill=GLS); d.rectangle([5,3,8,6], fill=dark(p,0.7)); d.line([(7,1),(7,3)], fill=CHR)
        for x in (3,6,9): d.point((x,8), fill=YEL)
    elif kind == 'diesel':
        d.rectangle([0,5,10,11], fill=p); d.rectangle([10,3,14,11], fill=p); d.rectangle([11,4,13,6], fill=GLS); d.line([(0,8),(10,8)], fill=WHT); d.point((14,9), fill=YEL); wheel(d,3,12); wheel(d,6,12); wheel(d,10,12); wheel(d,13,12)
    elif kind == 'bullet':
        d.polygon([(0,4),(8,4),(15,10),(15,11),(0,11)], fill=p); d.polygon([(9,5),(11,5),(13,8),(11,8)], fill=GLS); d.line([(0,8),(15,8)], fill=(27,42,120,255))
        for x in (1,4,7): d.rectangle([x,6,x+1,6], fill=GLS)
        d.line([(1,12),(14,12)], fill=BLK)
    elif kind == 'fuelcan':
        d.rectangle([3,4,12,14], fill=p); d.rectangle([4,5,11,13], outline=dark(p,0.6)); d.line([(4,5),(11,13)], fill=dark(p,0.6)); d.line([(11,5),(4,13)], fill=dark(p,0.6))
        d.rectangle([5,2,9,3], fill=BLK); d.rectangle([10,1,12,3], fill=(230,200,60,255))
    elif kind == 'ostrich':
        d.ellipse([3,5,11,10], fill=(40,36,34,255)); d.rectangle([1,5,4,7], fill=WHT); d.line([(10,6),(11,1)], fill=(214,170,150,255), width=1); d.rectangle([10,0,12,1], fill=(214,170,150,255)); d.point((13,1), fill=(240,120,30,255))
        d.line([(6,10),(5,15)], fill=(214,170,150,255)); d.line([(8,10),(9,15)], fill=(214,170,150,255)); d.rectangle([5,4,8,5], fill=(120,70,40,255))
    elif kind == 'chrono':
        d.polygon([(0,10),(2,8),(6,7),(9,5),(11,7),(15,9),(15,11),(0,11)], fill=p); d.polygon([(7,7),(9,6),(10,7)], fill=GLS); d.line([(0,9),(15,9)], fill=(150,152,158,255))
        d.point((1,8), fill=(190,90,255,255)); d.point((2,8), fill=(190,90,255,255)); wheel(d,3,12); wheel(d,12,12); d.point((3,12), fill=(190,90,255,255)); d.point((12,12), fill=(190,90,255,255))
    elif kind == 'shrinkcar':
        d.rounded_rectangle([3,6,12,11], 3, fill=p); d.rounded_rectangle([5,4,10,8], 2, fill=GLS); d.line([(8,1),(8,4)], fill=CHR); d.point((8,0), fill=(90,255,230,255)); wheel(d,5,12,1); wheel(d,10,12,1)
    elif kind == 'ufo':
        d.ellipse([0,7,15,11], fill=(154,164,176,255)); d.ellipse([4,3,11,9], fill=GLS); d.point((2,9), fill=(90,255,230,255)); d.point((7,10), fill=(90,255,230,255)); d.point((12,9), fill=(90,255,230,255))
        d.polygon([(5,11),(10,11),(13,15),(2,15)], fill=(90,255,230,90))
    elif kind == 'carpet':
        for x in range(1,15):
            y = 9 + (1 if (x//3)%2 else 0); d.line([(x,y),(x,y+1)], fill=p if x%3 else (196,140,40,255))
        d.point((0,9), fill=(242,194,26,255)); d.point((15,10), fill=(242,194,26,255))
    elif kind == 'hamster':
        d.ellipse([1,1,14,14], fill=(190,225,245,140), outline=p); d.line([(7,1),(7,14)], fill=p); d.ellipse([5,8,10,13], fill=(200,150,90,255))
    elif kind == 'cart':
        d.rectangle([3,5,13,10], outline=(180,184,190,255)); d.line([(3,7),(13,7)], fill=(180,184,190,255)); d.line([(6,5),(6,10)], fill=(180,184,190,255)); d.line([(10,5),(10,10)], fill=(180,184,190,255))
        d.line([(13,5),(15,3)], fill=RED); d.rectangle([0,6,3,8], fill=RED); d.point((0,7), fill=(255,150,30,255)); wheel(d,4,12,1); wheel(d,12,12,1)
    elif kind == 'mower':
        d.rectangle([2,9,13,11], fill=(60,90,40,255)); d.rectangle([7,6,13,9], fill=p); d.rectangle([2,6,5,9], fill=p); d.rectangle([3,3,5,6], fill=BLK); d.line([(8,6),(8,3)], fill=BLK); wheel(d,3,12,2); wheel(d,12,12,1)
    elif kind == 'kart':
        d.polygon([(1,10),(14,10),(15,8),(10,8),(8,9),(4,9)], fill=p); d.rectangle([5,6,6,9], fill=BLK); d.line([(9,8),(10,6)], fill=BLK); wheel(d,3,11,2); wheel(d,12,11,2)
    elif kind == 'golf':
        d.rectangle([1,8,14,11], fill=p); d.rectangle([1,3,13,4], fill=p); d.line([(2,4),(2,8)], fill=BLK); d.line([(12,4),(13,8)], fill=BLK); d.rectangle([4,7,9,8], fill=(140,90,60,255)); d.rectangle([0,5,1,10], fill=BLK); wheel(d,3,12); wheel(d,12,12)
    elif kind == 'garbage':
        d.rectangle([0,3,11,11], fill=p); d.rectangle([11,4,15,11], fill=p); d.rectangle([12,5,14,7], fill=GLS); d.line([(0,6),(11,6)], fill=WHT); d.rectangle([0,4,1,10], fill=(150,152,158,255)); wheel(d,3,12); wheel(d,6,12); wheel(d,13,12)
    elif kind == 'mixer':
        d.rectangle([11,4,14,11], fill=(242,242,242,255)); d.rectangle([12,5,13,7], fill=GLS); d.ellipse([1,3,11,10], fill=p); d.line([(3,4),(9,9)], fill=(240,122,16,255)); d.rectangle([0,9,11,11], fill=(90,95,102,255)); wheel(d,3,12); wheel(d,7,12); wheel(d,13,12)
    elif kind == 'forklift':
        d.rectangle([3,7,10,11], fill=p); d.line([(4,2),(4,7)], fill=BLK); d.line([(9,2),(9,7)], fill=BLK); d.line([(4,2),(9,2)], fill=BLK); d.line([(11,2),(11,11)], fill=(90,95,102,255)); d.line([(11,10),(15,10)], fill=(150,152,158,255)); d.rectangle([12,7,15,9], fill=(138,98,56,255)); wheel(d,5,12,2); wheel(d,10,12,2)
    elif kind == 'snowmobile':
        d.polygon([(2,10),(4,7),(10,7),(14,9),(14,10)], fill=p); d.rectangle([3,6,8,7], fill=BLK); d.line([(10,6),(11,4)], fill=GLS); d.rectangle([2,10,9,12], fill=(40,40,42,255)); d.line([(10,12),(15,12)], fill=CHR); d.point((15,11), fill=CHR)
    elif kind == 'hovercraft':
        d.rectangle([0,9,15,11], fill=(34,34,34,255)); d.rectangle([1,7,13,9], fill=p); d.rectangle([7,5,11,7], fill=GLS); d.ellipse([0,1,6,7], outline=dark(p,0.6)); d.line([(3,2),(3,6)], fill=(60,60,60,255))
    elif kind == 'balloon':
        d.ellipse([2,0,13,10], fill=p); d.line([(5,1),(5,9)], fill=(250,210,40,255)); d.line([(10,1),(10,9)], fill=(250,210,40,255)); d.line([(5,10),(6,12)], fill=BLK); d.line([(10,10),(9,12)], fill=BLK); d.rectangle([6,12,9,15], fill=(176,130,70,255))
    elif kind == 'excavator':
        d.rectangle([0,10,9,12], fill=(40,40,42,255)); d.rectangle([1,6,8,10], fill=p); d.rectangle([5,3,8,6], fill=GLS); d.line([(8,7),(12,2)], fill=p, width=2); d.line([(12,2),(14,8)], fill=p, width=2); d.rectangle([12,8,15,11], fill=(90,95,102,255))
    elif kind == 'dozer':
        d.rectangle([2,10,11,12], fill=(40,40,42,255)); d.rectangle([3,6,10,10], fill=p); d.rectangle([4,2,8,6], outline=BLK); d.rectangle([5,3,7,5], fill=GLS); d.rectangle([12,6,14,12], fill=p); d.line([(10,9),(12,9)], fill=(90,95,102,255))
    elif kind == 'loader':
        d.rectangle([1,6,8,10], fill=p); d.rectangle([2,2,6,6], fill=GLS); d.line([(8,8),(12,5)], fill=(90,95,102,255), width=2); d.polygon([(11,4),(15,4),(15,8),(12,8)], fill=(90,95,102,255)); wheel(d,3,12,2); wheel(d,10,12,2)
    elif kind == 'roller':
        d.ellipse([9,7,15,13], fill=(110,112,118,255), outline=BLK); d.rectangle([1,6,9,10], fill=p); d.rectangle([3,2,7,6], outline=BLK); wheel(d,3,12,2)
    elif kind == 'crane':
        d.rectangle([0,8,12,11], fill=p); d.rectangle([12,6,15,11], fill=p); d.rectangle([13,7,14,8], fill=GLS); d.line([(3,7),(13,0)], fill=(242,194,0,255), width=2); d.line([(13,1),(13,5)], fill=BLK); d.point((13,6), fill=YEL)
        wheel(d,2,12,1); wheel(d,5,12,1); wheel(d,10,12,1); wheel(d,13,12,1)
    elif kind == 'tow':
        d.rectangle([6,6,14,10], fill=p); d.rectangle([9,4,13,6], fill=p); d.rectangle([10,5,12,6], fill=GLS); d.line([(6,5),(3,9)], fill=(60,60,60,255), width=1); d.line([(3,9),(3,11)], fill=BLK); d.line([(1,11),(5,11)], fill=(90,95,102,255)); d.point((11,3), fill=(255,150,20,255)); wheel(d,7,12); wheel(d,13,12)
    img.save(os.path.join(ITEM, name + '.png'))
specs = [('classic_cruiser','#5FB8B0','classic'),('sedan','#2E4E8F','car'),('sports_car','#E0301E','sports'),('pickup_truck','#3C6E3A','pickup'),
 ('van','#F2F2F2','van'),('semi_truck','#B0122A','semi'),('fire_truck','#C81E1E','fire'),('bicycle','#2A9DF4','bicycle'),('motorbike','#262626','motorbike'),
 ('dirt_bike','#F07A10','dirt'),('army_jeep','#5B6B3A','jeep'),('tank','#4F5D33','tank'),('rocket_artillery','#55643A','artillery'),('helicopter','#1E2A3A','heli'),
 ('prop_plane','#E8C21A','prop'),('jet','#7E8A96','jet'),('rocket_ship','#F2F2F2','rocket'),('rv','#F2EEE2','rv'),('steam_train','#1E3A2A','train'),
 ('train_carriage','#7A1E1E','carriage'),('speedboat','#E0301E','speedboat'),('yacht','#FAFAFA','yacht'),
 ('police_car','#F5F5F5','police'),('taxi','#F2C21B','taxi'),('hatchback','#3FA34D','hatch'),('suv','#4A5560','suv'),('muscle_car','#F07818','muscle'),
 ('vintage_roadster','#6B1E2A','roadster'),('limousine','#1C1C22','limo'),('atv','#B71C1C','atv'),('ambulance','#F4F4F4','ambulance'),('city_bus','#1F6FB5','bus'),
 ('school_bus','#F2B90F','schoolbus'),('dump_truck','#E0A020','dump'),('tractor','#2F7D32','tractor'),('monster_truck','#7B2FBE','monster'),('scooter','#8FD3C8','scooter'),
 ('apc','#5B6B3A','apc'),('airliner','#F4F6F8','airliner'),('jet_ski','#1565C0','jetski'),('submarine','#2F3E46','sub'),('diesel_locomotive','#1F4E8C','diesel'),
 ('bullet_train','#F4F6F8','bullet'),('fuel_can','#C62828','fuelcan'),
 ('ostrich','#2A2422','ostrich'),('chrono_car','#2A1E3C','chrono'),('shrink_car','#3EC1D3','shrinkcar'),('ufo','#9AA4B0','ufo'),('flying_carpet','#8E1B3A','carpet'),
 ('hamster_ball','#F0A030','hamster'),('rocket_cart','#B0B4BA','cart'),('lawn_mower','#C62828','mower'),('go_kart','#2E7D32','kart'),('golf_cart','#F4F4F0','golf'),
 ('ice_cream_truck','#F8C8DC','van'),('garbage_truck','#2E7D32','garbage'),('cement_mixer','#E8E8E8','mixer'),('forklift','#F2B90F','forklift'),('snowmobile','#1E88E5','snowmobile'),
 ('hovercraft','#E65100','hovercraft'),('hot_air_balloon','#D32F2F','balloon'),
 ('excavator','#F2A900','excavator'),('bulldozer','#F2A900','dozer'),('wheel_loader','#F2A900','loader'),('road_roller','#F2A900','roller'),('mobile_crane','#F2C200','crane'),('tow_truck','#F57C00','tow')]
for s in specs: make(*s)
door = Image.new('RGBA', (16,16), (120,84,48,255)); dd = ImageDraw.Draw(door)
dd.rectangle([0,0,15,15], outline=(70,48,28,255)); dd.rectangle([3,2,12,7], fill=(150,210,240,255)); dd.rectangle([11,9,12,10], fill=(220,200,80,255)); door.save(os.path.join(BLOCK, 'rv_door.png'))
icon = Image.new('RGBA', (128,128), (24,26,34,255))
for i, n in enumerate(['sports_car','helicopter','tank','rv']):
    im = Image.open(os.path.join(ITEM, n + '.png')).resize((60,60), Image.NEAREST)
    icon.alpha_composite(im, (4 + (i % 2) * 62, 4 + (i // 2) * 62))
icon.save(os.path.join(ROOT, 'icon.png'))
prev = Image.new('RGBA', (11*68+8, 7*68+8), (50,50,58,255))
for i, s in enumerate(specs): prev.alpha_composite(Image.open(os.path.join(ITEM, s[0] + '.png')).resize((64,64), Image.NEAREST), (6 + (i%11)*68, 6 + (i//11)*68))
prev.save('/tmp/vicons.png'); print('icons ok')

eff = Image.new('RGBA', (18,18), (0,0,0,0)); de = ImageDraw.Draw(eff)
de.ellipse([1,1,16,16], outline=(62,193,211,255)); de.rectangle([7,9,10,15], fill=(200,150,120,255)); de.ellipse([7,5,10,8], fill=(200,150,120,255))
de.line([(2,2),(5,5)], fill=(255,255,255,255)); de.line([(16,2),(13,5)], fill=(255,255,255,255))
os.makedirs(os.path.join(ROOT, 'textures/mob_effect'), exist_ok=True); eff.save(os.path.join(ROOT, 'textures/mob_effect/shrunk.png'))
