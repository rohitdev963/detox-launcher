"""Small build-tool regressions, independent of Android SDK and private signing files."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile

spec = importlib.util.spec_from_file_location('detox_portable_build', Path(__file__).resolve().parents[1] / 'tools/build.py')
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)


class PortableBuildTests(unittest.TestCase):
    def test_archive_paths_bytes_and_uncompressed_video(self):
        with tempfile.TemporaryDirectory() as folder:
            source, target = Path(folder) / 'source.zip', Path(folder) / 'target.zip'
            with zipfile.ZipFile(source, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
                archive.writestr('assets\\video name.mp4', b'original\0bytes')
            builder.normalized_archive(source, target)
            with zipfile.ZipFile(target) as archive:
                self.assertEqual(archive.namelist(), ['assets/video name.mp4'])
                self.assertEqual(archive.read('assets/video name.mp4'), b'original\0bytes')
                self.assertEqual(archive.getinfo('assets/video name.mp4').compress_type, zipfile.ZIP_STORED)

    def test_normalization_cannot_hide_duplicate_asset(self):
        with tempfile.TemporaryDirectory() as folder:
            source, target = Path(folder) / 'source.zip', Path(folder) / 'target.zip'
            with warnings.catch_warnings(), zipfile.ZipFile(source, 'w') as archive:
                warnings.simplefilter('ignore', UserWarning)
                archive.writestr('assets\\same.mp4', b'first')
                archive.writestr('assets/same.mp4', b'second')
            with self.assertRaises(ValueError):
                builder.normalized_archive(source, target)

    def test_unsafe_archive_path_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            source, target = Path(folder) / 'source.zip', Path(folder) / 'target.zip'
            with zipfile.ZipFile(source, 'w') as archive:
                archive.writestr('../outside', b'bad')
            with self.assertRaises(ValueError):
                builder.normalized_archive(source, target)

    def test_javac_response_keeps_space_and_unicode_path(self):
        with tempfile.TemporaryDirectory() as folder:
            response = Path(folder) / 'javac.args'
            builder.java_response(response, ['-d', r'C:\A folder\classes', 'source ü.java'])
            self.assertEqual(response.read_text(encoding='utf-8'), '"-d"\n"C:/A folder/classes"\n"source ü.java"\n')
            with self.assertRaises(ValueError):
                builder.java_response(response, ['a\nb.java'])


if __name__ == '__main__':
    unittest.main()
