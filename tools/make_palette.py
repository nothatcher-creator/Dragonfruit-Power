"""128x128 material palette for Vehicle Forge (8x8 cells of 16px)."""
import math, random, sys
from PIL import Image
out = sys.argv[1]
img = Image.new('RGBA', (128, 128), (255, 0, 255, 255)); px = img.load(); R = random.Random(3)
def cell(i, fn):
    cx, cy = (i % 8) * 16, (i // 8) * 16
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            px[cx + x, cy + y] = c if len(c) == 4 else c + (255,)
def noise(base, amt):
    return lambda x, y: tuple(max(0, min(255, int(v + (R.random() - 0.5) * amt))) for v in base)
def hexc(h): h = h.lstrip('#'); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16))
cell(0, noise((242,242,242), 8)); cell(1, noise((235,235,235), 10))
cell(2, lambda x,y: tuple(int(170 + 80*(1 - abs(y-5)/11)) for _ in range(3)))
cell(3, noise(hexc('1c1c1e'), 10))
cell(4, lambda x,y: (40,40,40) if (x + y//2) % 4 == 0 else (24,24,26))
cell(5, lambda x,y: (210,212,218) if (abs(math.hypot(x-7.5,y-7.5)-5) < 1.2 or x in (7,8) or y in (7,8)) else (150,152,158))
cell(6, lambda x,y: (190,225,245,105) if (x+y) % 11 not in (0,1) else (240,250,255,150))
cell(7, lambda x,y: (120,70,40) if (x % 5 == 0 or y % 5 == 0) else (107,62,38))
cell(8, lambda x,y: (60,60,66) if y in (3,12) else (42,42,46))
def gauge(x,y):
    d = math.hypot(x-7.5, y-7.5); a = math.atan2(y-7.5, x-7.5)
    if d > 7.4: return (90,90,90)
    if d > 5.6 and (int((a+math.pi)/(math.pi/8)) % 2 == 0): return (240,240,240)
    if d < 1.2: return (200,40,40)
    return (12,12,14)
cell(9, gauge)
cell(10, lambda x,y: (255,248,220) if math.hypot(x-7.5,y-7.5) > 3 else (255,255,255))
cell(11, lambda x,y: (230,24,24) if (x+y)%5 else (255,80,80))
cell(12, lambda x,y: (255,40,40)); cell(13, lambda x,y: (40,90,255))
cell(14, lambda x,y: (120,84,48) if y % 4 == 0 else (138,98,56) if (x + (y//4)*5) % 9 else (110,76,44))
cell(15, lambda x,y: (107,112,64) if (x+y)%2 else (96,101,56))
def camo(x,y):
    v = math.sin(x*0.8)+math.cos(y*0.7)+math.sin((x+y)*0.4)
    return (72,82,46) if v > 0.8 else (98,92,60) if v > -0.4 else (52,60,38)
cell(16, camo)
cell(17, noise((90,95,102), 14)); cell(18, noise((201,162,39), 16)); cell(19, noise((70,52,52), 10))
cell(20, noise((242,242,242), 4)); cell(21, noise((196,30,30), 8)); cell(22, noise((242,194,26), 8))
cell(23, noise((42,42,42), 6))
cell(24, lambda x,y: (255, int(120 + 120*(y/15)), int(30*(y/15)), 230))
cell(25, lambda x,y: (180,184,190) if y % 3 == 0 else (18,18,20))
cell(26, lambda x,y: (30,30,60) if (y in (0,15) or x in (0,15) or (5<=y<=10 and x%3!=0 and 2<=x<=13)) else (240,240,235))
cell(27, lambda x,y: (150,100,58) if y % 4 == 0 else (176,122,70))
cell(28, noise(hexc('1b2a4a'), 8)); cell(29, noise(hexc('f07a10'), 8)); cell(30, noise(hexc('e8dcc4'), 6))
cell(31, lambda x,y: (40,220,140) if (y in (4,9) and 2<=x<=13) or (x == 3 and 4<=y<=12) else (10,30,40))
cell(32, lambda x,y: (70,70,72) if (y % 4) < 2 else (40,40,42))
cell(33, lambda x,y: (43,43,43) if R.random() > 0.15 else (110,60,30))
cell(34, lambda x,y: (20,20,20) if (x*7+y*3)%5 else (50,50,50))
cell(35, noise((250,250,250), 4)); cell(36, noise((34,34,34), 6))
cell(37, lambda x,y: (170,210,235,120) if (x-y) % 9 else (240,250,255,170))
img.save(out); print('palette ok')
