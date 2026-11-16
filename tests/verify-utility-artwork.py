"""Offline validation of bundled artwork, normalized transparency and recorded provenance."""
from pathlib import Path
import hashlib,json
from PIL import Image

root=Path(__file__).resolve().parents[1]
seen=set();total=0
for family in ('3dicons','fluent-system-icons','simple-icons'):
    directory=root/'assets/licenses'/family
    record=json.loads((directory/'PROVENANCE.json').read_text(encoding='utf-8'))
    assert len(record['revision'])==40 and record['repository'].startswith('https://github.com/')
    assert (directory/('MIT.txt' if family=='fluent-system-icons' else 'CC0-1.0.md' if family=='simple-icons' else 'CC0-1.0.txt')).stat().st_size>1000
    for entry in record['assets']:
        path=root/entry['asset'];data=path.read_bytes()
        assert path not in seen;seen.add(path)
        assert hashlib.sha256(data).hexdigest()==entry['sha256'],path
        assert len(data)==entry['bytes'] and 100<len(data)<100000,path
        with Image.open(path) as image:
            assert image.format=='WEBP' and image.size==(256,256),path
            rgba=image.convert('RGBA');assert rgba.getchannel('A').getextrema()==(0,255),path
            x0,y0,x1,y1=rgba.getbbox()
            assert min(x0,y0)>=16 and max(x1,y1)<=240,path
            assert max(x1-x0,y1-y0)>=220,path
            if entry.get('tintable'):
                for count,(r,g,b,a) in rgba.getcolors(256*256):
                    assert a==0 or r==g==b==255,(path,'monochrome tint input')
        total+=len(data)
actual=set((root/'assets/ui/3dicons').glob('*.webp'))|set((root/'assets/ui/utility-fluent').glob('*.webp'))|set((root/'assets/ui/utility-simpleicons').glob('*.webp'))
assert actual==seen and len(seen)==13
print(f'PASS: {len(seen)} offline WebP assets; {total:,} bytes; dimensions, alpha, padding, color/tint inputs, licenses and SHA-256 provenance verified.')
