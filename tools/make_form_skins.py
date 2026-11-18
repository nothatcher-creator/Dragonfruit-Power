"""64x64 elemental body skins (base layer only) for the Transform forms."""
import math, os, random, sys
from PIL import Image
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)
REGIONS = [(0,0,32,16),(16,16,40,32),(40,16,56,32),(0,16,16,32),(16,48,32,64),(32,48,48,64)]
def inside(x,y): return any(x0<=x<x1 and y0<=y<y1 for x0,y0,x1,y1 in REGIONS)
def make(name, fn, eye):
    img = Image.new('RGBA',(64,64),(0,0,0,0)); px = img.load(); rnd = random.Random(name)
    for y in range(64):
        for x in range(64):
            if inside(x,y): px[x,y] = fn(x,y,rnd) + (255,)
    for x,y in [(9,12),(10,12),(13,12),(14,12)]: px[x,y] = eye + (255,)
    img.save(os.path.join(OUT, name + '.png'))
def flame(x,y,r):
    v = 0.5 + 0.5*math.sin(x*0.9 + y*0.35) * math.cos(y*0.5) + r.random()*0.25
    v = min(1, max(0, v))
    return (255, int(90 + 150*v), int(20 + 60*v*v))
def ice(x,y,r):
    v = r.random()
    base = (150, 210, 245) if v > 0.25 else (110, 180, 230)
    if (x*3 + y) % 11 == 0 or v > 0.95: base = (235, 250, 255)
    return base
def magma(x,y,r):
    crack = abs(math.sin(x*0.7 + math.sin(y*0.5)*2)) < 0.18 or abs(math.sin(y*0.8 + x*0.2)) < 0.12
    if crack: return (255, 120 + r.randrange(60), 20)
    g = 30 + r.randrange(25)
    return (g, int(g*0.7), int(g*0.6))
def thunder(x,y,r):
    line = abs(math.sin(x*1.3 + y*0.9)) < 0.12 or abs(math.sin(x*0.4 - y*1.1)) < 0.1
    if line: return (210, 240, 255)
    return (30 + r.randrange(20), 60 + r.randrange(30), 140 + r.randrange(40))
def light(x,y,r):
    v = 0.75 + 0.25*math.sin((x+y)*0.6) + r.random()*0.1
    v = min(1, v)
    return (255, int(235*v), int(150*v))
make('flame', flame, (255,255,220)); make('ice', ice, (40,90,160)); make('magma', magma, (255,230,120))
make('thunder', thunder, (255,255,255)); make('light', light, (255,255,255))
print('ok')
