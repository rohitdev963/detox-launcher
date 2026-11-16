import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from public_assets import stage


class PublicAssetTests(unittest.TestCase):
    def fixture(self, root, name='public/original.txt'):
        path=root/'assets'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'original sample')
        rows=[{'path':name,'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}]
        (root/'tools').mkdir();(root/'tools/public-assets.json').write_text(json.dumps({'assets':rows}),encoding='utf-8')
        return path

    def test_private_files_never_swept_into_staging(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);self.fixture(root)
            private=root/'assets/gaming/music/private.m4a';private.parent.mkdir(parents=True);private.write_bytes(b'not public')
            destination=stage(root,root/'build/assets')
            self.assertEqual([p.relative_to(destination).as_posix() for p in destination.rglob('*') if p.is_file()],['public/original.txt'])

    def test_changed_asset_fails_closed(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);path=self.fixture(root);path.write_bytes(b'changed sample')
            with self.assertRaises(ValueError):stage(root,root/'build/assets')

    def test_private_namespace_is_rejected_even_with_a_valid_hash(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);self.fixture(root,'gaming/music/private.m4a')
            with self.assertRaises(ValueError):stage(root,root/'build/assets')

    def test_manifest_cannot_escape_assets(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);self.fixture(root)
            document=root/'tools/public-assets.json';rows=json.loads(document.read_text());rows['assets'][0]['path']='../outside'
            document.write_text(json.dumps(rows),encoding='utf-8')
            with self.assertRaises(ValueError):stage(root,root/'build/assets')

    def test_staging_rejects_existing_unlisted_content(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);self.fixture(root);target=root/'build/assets';target.mkdir(parents=True);(target/'leftover').write_bytes(b'old')
            with self.assertRaises(ValueError):stage(root,target)


if __name__=='__main__':unittest.main()
