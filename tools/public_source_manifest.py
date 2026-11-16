#!/usr/bin/env python3
"""List exact reviewed publication candidates; never stage, copy or upload source."""
import argparse
import hashlib
import json
from pathlib import Path
import re
from public_assets import entries

ROOT=Path(__file__).resolve().parents[1]
TOP=['.gitignore','.gitattributes','AndroidManifest.xml','apps.txt','LICENSE','NOTICE.md','README.md','CONTRIBUTING.md','SECURITY.md','build.ps1','normalize-apk-assets.ps1','validate.ps1']
DOCS=['docs/INSTALL.md','docs/BUILDING.md','docs/THIRD_PARTY_NOTICES.md','docs/PUBLIC_RELEASE_CHECKLIST.md','docs/RELEASE_NOTES.md','docs/licenses/PREMIUM_CLOCK_ARTWORK.md']


def candidates(root=ROOT):
    root=Path(root)
    files={root/name for name in TOP+DOCS}
    for folder,extensions in [('src',{'.java'}),('res',{'.xml'}),('tests',{'.java','.py','.mjs'}),('tools',{'.py'})]:
        files.update(p for p in (root/folder).rglob('*') if p.is_file() and p.suffix in extensions and '__pycache__' not in p.parts)
    files.update(p for p in (root/'.github').rglob('*') if p.is_file())
    files.add(root/'tools/public-assets.json')
    files.update(path for _,path in entries(root))
    return sorted(files,key=lambda p:p.relative_to(root).as_posix())


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args()
    records=[]
    for path in candidates():
        if not path.is_file() or path.is_symlink():raise ValueError('Missing file or symlink in public source manifest: '+str(path))
        name=path.relative_to(ROOT).as_posix()
        if name.startswith(('signing/','build/','output/','Detox_TV_Backup/','assets/wallpapers/','assets/gaming/')) or path.suffix.lower() in {'.jks','.dpapi','.key','.apk','.idsig','.pfx','.p12'}:
            raise ValueError('Private file in public source manifest: '+name)
        data=path.read_bytes()
        if path.suffix.lower() in {'.java','.py','.mjs','.md','.txt','.json','.ps1','.xml','.yml','.yaml'}:
            text=data.decode('utf-8-sig')
            if re.search(r'[A-Z]:/+Users/+[^/\s]+/+(?:Documents|Downloads|Desktop)/',text.replace('\\','/'),re.I) or re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',text):
                raise ValueError('Private laptop detail or key material in publication candidate: '+name)
        records.append({'path':name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
    document={'format':'detox.public.source','repository':'rohitdev963/detox-launcher','files':records,'total_bytes':sum(row['bytes'] for row in records)}
    if args.output:
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(json.dumps(document,indent=2)+'\n',encoding='utf-8')
    print(f'Public source candidates: {len(records)} files; {document["total_bytes"]:,} bytes. No private media, signing files, APKs or laptop paths included.')


if __name__=='__main__':main()
