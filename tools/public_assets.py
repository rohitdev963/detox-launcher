"""Exact public runtime asset manifest. Private local media is never swept into a build."""
import hashlib
import json
from pathlib import Path
import shutil


def entries(root):
    root=Path(root)
    document=json.loads((root/'tools/public-assets.json').read_text(encoding='utf-8'))
    seen=set()
    for row in document['assets']:
        name=row['path'];parts=Path(name).parts
        if not name or '\\' in name or Path(name).is_absolute() or '..' in parts or name in seen:
            raise ValueError('Invalid or duplicate public asset path')
        seen.add(name)
        if not (name=='phone_companion.html' or name.startswith(('public/','fonts/','licenses/','ui/'))):
            raise ValueError('Unexpected public asset namespace: '+name)
        path=(root/'assets'/name).resolve()
        if (root/'assets').resolve() not in path.parents or not path.is_file():
            raise ValueError('Public asset is missing or outside assets: '+name)
        with path.open('rb') as stream:
            if stream.read(80).startswith(b'version https://git-lfs.github.com/spec/'):
                raise ValueError('Asset is a Git LFS pointer, not media: '+name)
        if path.stat().st_size!=row['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest()!=row['sha256']:
            raise ValueError('Public asset changed; review and regenerate manifest: '+name)
        yield name,path


def stage(root,destination):
    destination=Path(destination)
    if destination.exists() and any(destination.iterdir()):
        raise ValueError('Public asset staging must start empty')
    for name,source in entries(root):
        target=destination/name;target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(source,target)
    return destination
