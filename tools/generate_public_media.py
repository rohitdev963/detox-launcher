#!/usr/bin/env python3
"""Author Detox's original abstract sample media; no downloaded art, characters, music or textures.

Requires Pillow and an FFmpeg executable for regeneration only. Generated assets
are committed so ordinary source builds need neither Pillow nor FFmpeg.
Copyright (c) 2026 Rohit and Detox contributors. MIT licensed.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
import shutil
import subprocess
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
WIDTH, HEIGHT = 1920, 1080


def gradient(top, bottom):
    image = Image.new('RGB', (WIDTH, HEIGHT))
    draw = ImageDraw.Draw(image)
    for y in range(HEIGHT):
        t = y / (HEIGHT - 1)
        color = tuple(round(a * (1-t) + b*t) for a, b in zip(top, bottom))
        draw.line((0, y, WIDTH, y), fill=color)
    return image.convert('RGBA')


def glow(image, center, radius, color):
    layer = Image.new('RGBA', image.size)
    draw = ImageDraw.Draw(layer)
    x, y = center
    for i in range(80, 0, -1):
        r = radius * i / 80
        alpha = round((1-i/81)**2 * color[3])
        draw.ellipse((x-r, y-r, x+r, y+r), fill=(*color[:3], alpha))
    return Image.alpha_composite(image, layer.filter(ImageFilter.GaussianBlur(24)))


def aurora():
    image = gradient((8, 15, 38), (5, 17, 30))
    image = glow(image, (1320, 280), 770, (66, 75, 185, 95))
    ribbon = Image.new('RGBA', image.size)
    draw = ImageDraw.Draw(ribbon)
    for band in range(9):
        points=[]
        for x in range(-40, WIDTH+41, 8):
            y=300 + math.sin(x/WIDTH*3.2 + .35)*170 + band*15
            points.append((x,y))
        points += [(x, y+90+band*3) for x,y in reversed(points)]
        draw.polygon(points, fill=(43+band*7, 171-band*7, 193+band*5, 8))
    return Image.alpha_composite(image, ribbon.filter(ImageFilter.GaussianBlur(26)))


def peaks():
    image = glow(gradient((20, 22, 57),(68, 49, 77)),(1350,240),540,(195,122,155,70))
    draw=ImageDraw.Draw(image)
    draw.ellipse((1330,190,1430,290),fill=(231,208,197,220))
    for base,amplitude,color,phase in [(630,190,(37,42,69,255),.3),(770,145,(23,34,57,255),1.5),(910,90,(12,25,43,255),2.4)]:
        points=[(-10,HEIGHT)]
        for x in range(-10,WIDTH+11,8):
            y=base-amplitude*abs(math.sin(x/410+phase)) - amplitude*.18*math.sin(x/125)
            points.append((x,y))
        points.append((WIDTH+10,HEIGHT));draw.polygon(points,fill=color)
    return image


def ember():
    image=gradient((20,14,30),(9,17,34))
    image=glow(image,(1420,400),720,(190,69,42,105))
    image=glow(image,(650,800),820,(45,56,124,65))
    layer=Image.new('RGBA',image.size);draw=ImageDraw.Draw(layer)
    for index in range(5):
        points=[]
        for x in range(-40,WIDTH+41,8):
            points.append((x,700+index*43+math.sin(x/600+index*.17)*80))
        points += [(WIDTH+40,HEIGHT),(-40,HEIGHT)]
        draw.polygon(points,fill=(45-index*5,30-index*3,48-index*2,105))
    return Image.alpha_composite(image,layer)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ffmpeg',type=Path,required=True)
    args=parser.parse_args()
    folder=ROOT/'assets/public'
    (folder/'wallpapers').mkdir(parents=True,exist_ok=True)
    (folder/'videos').mkdir(exist_ok=True)
    entries=[]
    for name,render in [('aurora-veil',aurora),('twilight-peaks',peaks),('ember-horizon',ember)]:
        path=folder/'wallpapers'/f'{name}.jpg'
        render().convert('RGB').save(path,quality=94,subsampling=0,optimize=True)
        entries.append({'path':path.relative_to(ROOT/'assets').as_posix(),'kind':'image','width':WIDTH,'height':HEIGHT})
    for name,still,title in [('aurora-drift','aurora-veil','Aurora Drift'),('ember-drift','ember-horizon','Ember Drift')]:
        path=folder/'videos'/f'{name}.mp4'
        movement="zoompan=z=1.04:x='(iw-iw/zoom)/2+12*sin(2*PI*on/480)':y='(ih-ih/zoom)/2+6*cos(2*PI*on/480)':d=1:s=1920x1080:fps=60,scale=in_range=pc:out_range=tv,format=yuv420p,setparams=range=limited"
        subprocess.run([str(args.ffmpeg),'-hide_banner','-loglevel','error','-y','-loop','1','-framerate','60','-i',str(folder/'wallpapers'/f'{still}.jpg'),'-vf',movement,'-frames:v','480','-an','-c:v','libx264','-preset','medium','-crf','24','-pix_fmt','yuv420p','-profile:v','high','-level:v','4.2','-refs','3','-threads','2','-movflags','+faststart','-metadata',f'title={title}','-metadata','comment=Original procedural Detox artwork; MIT licensed.',str(path)],check=True)
        entries.append({'path':path.relative_to(ROOT/'assets').as_posix(),'kind':'video','width':WIDTH,'height':HEIGHT,'fps':60,'duration_seconds':8,'audio':False})
    for entry in entries:
        path=ROOT/'assets'/entry['path'];entry['sha256']=hashlib.sha256(path.read_bytes()).hexdigest();entry['bytes']=path.stat().st_size
    shutil.copyfile(ROOT/'LICENSE',folder/'LICENSE.txt')
    (folder/'NOTICE.md').write_text('# Original Detox sample backgrounds\n\nCopyright (c) 2026 Rohit and Detox contributors. MIT licensed.\n\nOriginal gradients, geometric silhouettes and motion authored by\n`tools/generate_public_media.py`. No source photos, external artwork, characters,\nlogos, soundtracks, stock textures or downloaded media were used. Motion samples\nare silent, periodic pan animations of the original still artwork.\n',encoding='utf-8')
    (folder/'PROVENANCE.json').write_text(json.dumps({'license':'MIT','author':'Rohit and Detox contributors','source':'tools/generate_public_media.py','external_media_inputs':[],'assets':entries},indent=2)+'\n',encoding='utf-8')
    print(f'Generated {len(entries)} original samples: {sum(entry["bytes"] for entry in entries):,} bytes')


if __name__=='__main__':main()
