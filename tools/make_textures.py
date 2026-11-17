"""Generates animated 16x16 dragonfruit item textures (4 frames each) + mod icon."""
import json, math, os, sys
from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

def hexc(h): h = h.lstrip('#'); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), 255)
def mix(a, b, t): return tuple(int(a[i] + (b[i]-a[i])*t) for i in range(3)) + (255,)

PAL = {
 'rubber': dict(out='#4a0f3a', dark='#9c1e6e', mid='#d23b95', light='#ee6cb6', hi='#ffc2e4',
                leaf='#2f8a37', leafl='#5cc04a', tip='#c6f56a', accent='#ffe3f3'),
 'flame':  dict(out='#5a1500', dark='#b03200', mid='#e8590c', light='#ff9330', hi='#ffe08a',
                leaf='#c43a00', leafl='#ff7a1a', tip='#ffe14a', accent='#fff3b0'),
 'ice':    dict(out='#1e4a6b', dark='#3d86b8', mid='#7fc6ef', light='#bfe9ff', hi='#ffffff',
                leaf='#3a8fc4', leafl='#8fd6ff', tip='#ffffff', accent='#ffffff'),
 'light':  dict(out='#6b4d00', dark='#c99500', mid='#f2c522', light='#ffe26b', hi='#ffffff',
                leaf='#d9a400', leafl='#ffe066', tip='#ffffff', accent='#fffbe0'),
 'quake':  dict(out='#4b463b', dark='#9d9686', mid='#d8d2c2', light='#f1ede2', hi='#ffffff',
                leaf='#6e8a5a', leafl='#9cbf83', tip='#f4ffe8', accent='#4b463b'),
}

CX, CY, RX, RY = 7.6, 9.6, 5.3, 5.7

def inside(x, y, grow=0.0):
    return ((x+0.5-CX)/(RX+grow))**2 + ((y+0.5-CY)/(RY+grow))**2 <= 1.0

# bracts: (base x, base y, tip x, tip y) - small flaps on the skin pointing outward/up
BRACTS = [(4,8,2,7),(11,7,13,6),(5,12,3,12),(11,12,13,12),(6,6,5,4),(9,5,10,3),(8,10,8,8),(7,13,6,15) ]
CROWN = [(7,3),(8,2),(6,2),(9,3),(10,2),(5,3),(8,1)]

def frame(name, f):
    p = {k: hexc(v) for k, v in PAL[name].items()}
    img = Image.new('RGBA', (16,16), (0,0,0,0))
    px = img.load()
    # body with shading
    for y in range(16):
        for x in range(16):
            if not inside(x, y): continue
            dx = (x+0.5-CX)/RX; dy = (y+0.5-CY)/RY
            shade = -0.55*dx - 0.65*dy  # light from top-left
            if shade > 0.55: c = p['hi'] if (x+y)%2==0 and shade > 0.75 else p['light']
            elif shade > 0.05: c = p['mid']
            elif shade > -0.45: c = mix(p['mid'], p['dark'], 0.55)
            else: c = p['dark']
            px[x,y] = c
    # outline
    for y in range(16):
        for x in range(16):
            if inside(x,y): continue
            if any(0<=x+ox<16 and 0<=y+oy<16 and inside(x+ox,y+oy) for ox,oy in ((1,0),(-1,0),(0,1),(0,-1))):
                px[x,y] = p['out']
    # moving shimmer band (diagonal)
    band = [-3, 0, 3, 6][f]
    for y in range(16):
        for x in range(16):
            if inside(x, y, -0.8) and (x - y) == band - 6 + 4:
                px[x,y] = mix(px[x,y], p['hi'], 0.65)
    # per-fruit skin details
    if name == 'rubber':
        curls = [(5,9),(6,9),(6,10),(5,11),(9,7),(10,7),(10,8),(9,9),(8,12),(9,12),(10,12),(10,11)]
        for x,y in curls:
            if inside(x,y): px[x,y] = mix(px[x,y], p['accent'], 0.75)
    if name == 'ice':
        for i,(x,y) in enumerate([(6,8),(9,10),(7,12),(10,7),(5,10)]):
            if (i + f) % 2 == 0: px[x,y] = p['hi']
    if name == 'light':
        glow = 0.25 + 0.2*math.sin(f*math.pi/2)
        for y in range(16):
            for x in range(16):
                if inside(x,y,-1.5): px[x,y] = mix(px[x,y], p['hi'], glow)
    if name == 'quake':
        crack = [(8,5),(8,6),(7,7),(8,8),(9,9),(8,10),(7,11),(9,10),(10,11),(6,8),(5,9)]
        for x,y in crack:
            if inside(x,y): px[x,y] = p['accent']
        if f % 2 == 0:
            for x,y in [(9,9),(8,8)]: px[x,y] = (255,255,255,255)
    if name == 'flame':
        for y in range(16):
            for x in range(16):
                if inside(x,y,-2.2) and (x*3+y*5+f) % 7 == 0: px[x,y] = p['hi']
    # bracts
    for i,(bx,by,tx,ty) in enumerate(BRACTS):
        wob = 0
        if name == 'flame': wob = [0,-1,0,1][(f+i)%4] if i%2==0 else 0
        px[bx,by] = p['leaf']
        mx, my = (bx+tx)//2, (by+ty)//2
        if 0<=mx<16 and 0<=my<16: px[mx,my] = p['leafl']
        ty2 = max(0, min(15, ty + wob))
        if 0<=tx<16: px[tx,ty2] = p['tip'] if (name != 'ice' or (i+f)%2==0) else p['leafl']
    # crown
    for i,(x,y) in enumerate(CROWN):
        px[x,y] = p['leafl'] if i%2 else p['leaf']
    tip_y = 0 if (name == 'flame' and f % 2) else 1
    px[8, tip_y] = p['tip']
    if tip_y == 0: px[8,1] = p['leafl']
    px[5,2] = p['tip']; px[10,1] = p['tip']
    return img

for name in PAL:
    strip = Image.new('RGBA', (16, 64))
    for f in range(4):
        strip.paste(frame(name, f), (0, f*16))
    strip.save(os.path.join(OUT, f'{name}_dragonfruit.png'))
    with open(os.path.join(OUT, f'{name}_dragonfruit.png.mcmeta'), 'w') as fh:
        json.dump({"animation": {"frametime": 5, "interpolate": True}}, fh, indent=2)

# icon 128x128: 2x2-ish arrangement of fruits on a dark rounded tile
icon = Image.new('RGBA', (128,128), (0,0,0,0))
bg = Image.new('RGBA', (128,128), (24,18,32,255))
mask = Image.new('L', (128,128), 0)
from PIL import ImageDraw
ImageDraw.Draw(mask).rounded_rectangle((0,0,127,127), radius=22, fill=255)
icon.paste(bg, (0,0), mask)
big = frame('rubber', 0).resize((96,96), Image.NEAREST)
icon.alpha_composite(big, (16,14))
for i,n in enumerate(['flame','ice','light','quake']):
    sm = frame(n,0).resize((32,32), Image.NEAREST)
    icon.alpha_composite(sm, (4 + i*30, 94))
icon.save(sys.argv[2])

# preview sheet for review
prev = Image.new('RGBA', (5*16*8 + 40, 4*16*8 + 30), (40,40,48,255))
for c,name in enumerate(PAL):
    for f in range(4):
        prev.alpha_composite(frame(name,f).resize((128,128), Image.NEAREST), (8 + c*136, 8 + f*132))
prev.save(sys.argv[3])
print('ok')
