"""Optional media checks. Read-only on assets; writes JSON evidence under build.

Usage: python tests/verify-wallpaper-media.py --ffmpeg-dir <bin> --decode
No APK creation, networking or Android device access.
"""
import argparse
import concurrent.futures
import json
import struct
import re
import subprocess
import os
from fractions import Fraction
from pathlib import Path
from video_media_policy import validate_avc

parser = argparse.ArgumentParser()
parser.add_argument("--ffmpeg-dir", type=Path, required=True)
parser.add_argument("--decode", action="store_true")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
suffix = '.exe' if os.name == 'nt' else ''
ffmpeg = args.ffmpeg_dir / ('ffmpeg'+suffix)
ffprobe = args.ffmpeg_dir / ('ffprobe'+suffix)
assets = sorted((root / "assets/public/videos").glob("*.mp4"))
assert len(assets) == 2, "Public video inventory changed"


def fast_start(path):
    """Seek top-level boxes without loading large MP4 files into memory."""
    boxes = []
    with path.open("rb") as stream:
        while stream.tell() + 8 <= path.stat().st_size:
            header = stream.read(8)
            size, kind = struct.unpack(">I4s", header)
            header_size = 8
            if size == 1:
                size = struct.unpack(">Q", stream.read(8))[0]
                header_size = 16
            if size == 0:
                size = path.stat().st_size - stream.tell() + header_size
            assert size >= header_size, f"Invalid MP4 box: {path.name}"
            boxes.append(kind.decode("ascii", errors="replace"))
            stream.seek(size - header_size, 1)
    return "moov" in boxes and "mdat" in boxes and boxes.index("moov") < boxes.index("mdat")


def verify(path):
    raw = subprocess.run(
        [str(ffprobe), "-v", "error", "-show_streams", "-show_format", "-of", "json", str(path)],
        check=True, capture_output=True, text=True,
    )
    metadata = json.loads(raw.stdout)
    tracks = [s for s in metadata["streams"] if s["codec_type"] == "video"]
    assert len(tracks) == 1, path.name
    video = tracks[0]
    fps = float(Fraction(video["avg_frame_rate"]))
    headers = subprocess.run(
        [str(ffmpeg), "-hide_banner", "-i", str(path), "-map", "0:v:0", "-c", "copy",
         "-bsf:v", "trace_headers", "-frames:v", "1", "-f", "null", "-"],
        check=True, capture_output=True, text=True,
    ).stderr
    refs = re.search(r"max_num_ref_frames.*= (\d+)", headers)
    assert refs, f"No AVC SPS reference-frame declaration: {path.name}"
    reference_frames = int(refs[1])
    validate_avc(video, reference_frames)
    assert video["codec_name"] == "h264" and video["pix_fmt"] == "yuv420p", path.name
    assert video["width"] <= 1920 and video["height"] <= 1080, f"4K decoder load remains: {path.name}"
    assert 1 <= fps <= 60.01, path.name
    assert video["width"] == 1920 and video["height"] == 1080 and abs(fps - 60) < .001
    assert abs(float(video["duration"]) - 8) <= 1 / 60, "Sample duration changed"
    assert not any(s['codec_type']=='audio' for s in metadata['streams']), "Public samples must be silent"
    record = {
        "asset": path.relative_to(root).as_posix(), "bytes": path.stat().st_size,
        "codec": video["codec_name"], "profile": video["profile"], "level": video["level"],
        "width": video["width"], "height": video["height"], "pix_fmt": video["pix_fmt"],
        "fps": video["avg_frame_rate"], "duration_seconds": metadata["format"]["duration"],
        "audio_streams": sum(s["codec_type"] == "audio" for s in metadata["streams"]),
        "fast_start": fast_start(path),
        "sps_reference_frames": reference_frames,
    }
    if args.decode:
        decoded = subprocess.run(
            [str(ffmpeg), "-hide_banner", "-loglevel", "error", "-nostdin", "-xerror", "-threads", "2",
             "-i", str(path), "-map", "0:v:0", "-map", "0:a?", "-f", "null", "-"],
            capture_output=True, text=True,
        )
        record.update(decode_exit=decoded.returncode, decode_errors=decoded.stderr.strip())
        assert decoded.returncode == 0, record
    print("PASS " + path.name + " (" + video["avg_frame_rate"] + " FPS)", flush=True)
    return record


with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    records = list(pool.map(verify, assets))
out = root / "build/wallpaper-update"
out.mkdir(parents=True, exist_ok=True)
(out / "media-validation.json").write_text(json.dumps(records, indent=2), encoding="utf-8")
print(f"PASS: {len(records)} original silent H.264 samples verified 1080p60 / 8 seconds.")
