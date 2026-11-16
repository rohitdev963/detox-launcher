"""Local-edition packaging boundaries; generated catalog files only, never Activities."""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import personal_media as media
import build


class PersonalMediaTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.profile = ROOT / 'assets/wallpapers/personal-catalog.json'
        if not cls.profile.exists():
            raise unittest.SkipTest('Private media profile is laptop-only')
        cls.config = json.loads(cls.profile.read_text(encoding='utf-8'))

    def test_public_stays_default(self):
        self.assertEqual(build.arguments(['--unsigned']).edition, 'public')

    def test_exact_local_inventory(self):
        config = media.load_profile(ROOT, self.profile)
        self.assertEqual([len(config[k]['names']) for k in ['wallpapers','live','gaming','music']], [11,12,2,1])
        self.assertEqual(len(config['assets']), 25)
        self.assertTrue(config['music']['assets'][0].endswith('.ogg'))

    def test_generated_catalogs_only(self):
        with tempfile.TemporaryDirectory(prefix='media-test-', dir=ROOT/'build') as folder:
            folder = Path(folder)
            with patch.object(media.shutil, 'copyfile') as copier:
                result = media.prepare(ROOT, self.profile, folder/'assets', folder/'gen')
            self.assertEqual(set(p.name for p in (folder/'gen').iterdir()), media.CATALOGS)
            self.assertFalse(any('Activity.java' in name for name in result['overrides']))
            self.assertEqual(result['catalog_counts'], {'wallpapers':10,'live':12,'gaming':2,'music':1})
            self.assertFalse(any(name.startswith('public/') for name in result['assets']))
            self.assertEqual(len(result['assets']), len(set(result['assets'])))
            self.assertEqual(copier.call_count, len(result['assets']))
            wallpaper = (folder/'gen/BundledWallpaperCatalog.java').read_text()
            self.assertIn('Krishna', wallpaper)
            self.assertIn('Midnight Mountain', wallpaper)
            self.assertNotIn('return false;', wallpaper.split('landscapeRotation')[1].split('}')[0])

    def test_unsafe_paths_rejected(self):
        for name in ['../signing/password.dpapi','C:/secrets','wallpapers/../../bad','wallpapers\\bad']:
            with self.assertRaises(ValueError): media._asset(ROOT, name)

    def test_profile_outside_assets_rejected(self):
        with self.assertRaises(ValueError): media.load_profile(ROOT, ROOT/'tools/public-assets.json')

    def test_hash_and_duplicate_manifest_rejected(self):
        for mutation in ['hash','duplicate','catalog']:
            config = copy.deepcopy(self.config)
            if mutation=='hash':config['assets'][0]['sha256']='0'*64
            if mutation=='duplicate':config['assets'].append(config['assets'][0])
            if mutation=='catalog':config['gaming']['files'][0]=config['gaming']['files'][1]
            with patch.object(media.json, 'loads', return_value=config):
                with self.assertRaises(ValueError):media.load_profile(ROOT, self.profile)


if __name__=='__main__':unittest.main()
