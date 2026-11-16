import unittest
from video_media_policy import validate_avc


class AvcPolicyTest(unittest.TestCase):
    def video(self, **values):
        result = dict(codec_name="h264", pix_fmt="yuv420p", profile="High", level=41,
                      width=1920, height=1080, avg_frame_rate="30/1")
        result.update(values)
        return result

    def test_valid_1080p30(self):
        validate_avc(self.video(), 4)

    def test_invalid_level_four(self):
        with self.assertRaises(AssertionError): validate_avc(self.video(level=4), 4)

    def test_excessive_references(self):
        with self.assertRaises(AssertionError): validate_avc(self.video(avg_frame_rate="24/1", level=40), 5)

    def test_sixty_requires_42(self):
        with self.assertRaises(AssertionError): validate_avc(self.video(avg_frame_rate="60/1"), 4)
        validate_avc(self.video(avg_frame_rate="60/1", level=42), 4)

    def test_other_incompatible_formats(self):
        for values in [dict(pix_fmt="yuv420p10le"), dict(profile="High 10"), dict(width=3840), dict(height=0)]:
            with self.assertRaises(AssertionError): validate_avc(self.video(**values), 4)

    def test_fractional_cadence(self):
        validate_avc(self.video(avg_frame_rate="49050000/1635019", profile="Main"), 4)
        validate_avc(self.video(avg_frame_rate="60000/1001", level=42), 4)


if __name__ == "__main__": unittest.main()
