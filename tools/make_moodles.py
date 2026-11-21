#!/usr/bin/env python3
"""Moodle glyphs for the Outbreak HUD (original art): white pictograms with a dark rim, 32x32, drawn at 16x16."""
import os
from PIL import Image, ImageDraw, ImageFilter
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'outbreak', 'textures', 'gui', 'moodle')
W = (245, 242, 232, 255)


def finish(img, name):
    a = img.split()[3]
    rim = Image.new('RGBA', img.size, (25, 20, 18, 0))
    rim.putalpha(a.filter(ImageFilter.MaxFilter(3)).point(lambda v: 200 if v else 0))
    rim.alpha_composite(img)
    rim.save(os.path.join(OUT, name + '.png'))


def hungry():
    i = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(i)
    for x in (8, 11, 14):                                      # fork tines
        d.line([x, 5, x, 13], fill=W, width=2)
    d.rounded_rectangle([7, 12, 15, 16], 2, fill=W); d.line([11, 15, 11, 27], fill=W, width=3)
    d.ellipse([18, 4, 25, 18], fill=W); d.line([21, 15, 21, 27], fill=W, width=3)   # knife/spoon
    finish(i, 'hungry')


def injured():
    i = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(i)
    d.rectangle([12, 5, 19, 26], fill=W); d.rectangle([5, 12, 26, 19], fill=W)
    finish(i, 'injured')


def queasy():
    i = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(i)
    d.ellipse([5, 5, 26, 26], outline=W, width=3)
    d.line([10, 12, 14, 14], fill=W, width=2); d.line([21, 12, 17, 14], fill=W, width=2)
    d.line([10, 21, 13, 19, 16, 21, 19, 19, 22, 21], fill=W, width=2)       # wavy mouth
    finish(i, 'queasy')


def panic():
    i = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(i)
    d.ellipse([5, 5, 26, 26], outline=W, width=3)
    d.ellipse([10, 10, 13, 14], fill=W); d.ellipse([18, 10, 21, 14], fill=W)
    d.ellipse([13, 17, 18, 23], outline=W, width=2)                       # open mouth
    finish(i, 'panic')


def wet():
    i = Image.new('RGBA', (32, 32)); d = ImageDraw.Draw(i)
    for (cx, cy, r) in ((11, 16, 5), (21, 12, 4), (20, 22, 4)):
        d.polygon([(cx, cy - r * 2), (cx - r, cy), (cx + r, cy)], fill=W)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=W)
    finish(i, 'wet')


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for f in (hungry, injured, queasy, panic, wet):
        f()
    sheet = Image.new('RGBA', (5 * 40, 40), (170, 80, 50, 255))
    for k, n in enumerate(['hungry', 'injured', 'queasy', 'panic', 'wet']):
        sheet.alpha_composite(Image.open(os.path.join(OUT, n + '.png')), (k * 40 + 4, 4))
    sheet.resize((400, 80), Image.NEAREST).save('/tmp/moodles.png')
