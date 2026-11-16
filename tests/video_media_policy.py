"""Declared H.264 level, macroblock rate and decoded-picture-buffer validation."""
from fractions import Fraction


def validate_avc(video, reference_frames):
    assert video["codec_name"] == "h264" and video["pix_fmt"] == "yuv420p", "Expected 8-bit 4:2:0 H.264"
    assert video["profile"] in {"Main", "High", "Baseline", "Constrained Baseline"}, "Unsupported AVC profile"
    limits = {40: (8192, 245760, 32768), 41: (8192, 245760, 32768), 42: (8704, 522240, 34816)}
    level = video["level"]
    assert level in limits, f"Invalid/unexpected H.264 level_idc {level}; 4 is not level 4.0 (40)"
    width, height = video["width"], video["height"]
    assert 0 < width <= 1920 and 0 < height <= 1080, "Expected 1080p or lower"
    frame_mbs = ((width + 15) // 16) * ((height + 15) // 16)
    max_frame, max_rate, max_dpb = limits[level]
    fps = float(Fraction(video["avg_frame_rate"]))
    assert 1 <= fps <= 60.01 and frame_mbs <= max_frame and frame_mbs * fps <= max_rate + 1, "Level cannot represent frame size/rate"
    assert 0 <= reference_frames <= min(16, max_dpb // frame_mbs), "Reference frames exceed declared level DPB"
