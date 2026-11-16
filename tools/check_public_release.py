#!/usr/bin/env python3
"""Read-only publication preflight. Does not commit, upload, sign or install anything."""
import hashlib
import json
from pathlib import Path
import sys
from public_assets import entries

ROOT = Path(__file__).resolve().parents[1]


def main():
    problems = []
    for name in ['LICENSE', 'README.md', 'NOTICE.md', '.gitignore', '.gitattributes', 'CONTRIBUTING.md', 'SECURITY.md', 'docs/INSTALL.md', 'docs/BUILDING.md']:
        if not (ROOT / name).is_file():
            problems.append('Missing public document: ' + name)
    try:
        assets=dict(entries(ROOT))
        if len(assets)!=41:problems.append('Unexpected reviewed public asset count')
        provenance=json.loads((ROOT/'assets/public/PROVENANCE.json').read_text(encoding='utf-8'))
        if provenance['license']!='MIT' or provenance['external_media_inputs']!=[]:
            problems.append('Original samples lack the expected provenance')
        if (ROOT/'LICENSE').read_bytes()!=(ROOT/'assets/public/LICENSE.txt').read_bytes():
            problems.append('Original artwork license differs from project MIT text')
        for row in provenance['assets']:
            if row['path'] not in assets or hashlib.sha256(assets[row['path']].read_bytes()).hexdigest()!=row['sha256']:
                problems.append('Original sample/provenance mismatch: '+row['path'])
        if len(provenance['assets'])!=5:problems.append('Expected three original stills and two motion samples')
        ignored=(ROOT/'.gitignore').read_text(encoding='utf-8')
        for path in ['/signing/','/assets/wallpapers/','/assets/gaming/videos/','/assets/gaming/music/','/output/','/docs/MEDIA_RIGHTS.csv']:
            if path not in ignored:problems.append('Private local content is not excluded: '+path)
        print(f'Read-only preflight: {len(assets)} reviewed public assets; 5 original media samples; no supplied music.')
    except (OSError,ValueError,KeyError) as error:
        problems.append(str(error))
    for problem in problems:
        print('NOT READY: ' + problem)
    if problems:
        return 1
    print('File/rights preflight passed. Review permission evidence, staged files, Git history, repository settings and APK separately before publication.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
