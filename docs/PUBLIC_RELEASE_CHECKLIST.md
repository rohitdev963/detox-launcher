# Public release checklist

Repository: [rohitdev963/detox-launcher](https://github.com/rohitdev963/detox-launcher).
The maintainer authorized publishing the latest code and public release on
2026-11-16. Original source and sample media use MIT; third-party terms remain.

## v1.10.57 / build 113

- Preserve `com.ansub.tvhome.rain` and the existing official release certificate.
- Public builds use exactly 41 reviewed assets: three original stills, two silent
  motion samples, five fonts, licensed utility artwork, notices and companion UI.
- Exclude signing files, local media/profile, private logs, TV data and backups.
- Publish the new public APK, not the 580 MB personal APK installed on the TVs.
- Keep previous public release attachments available for recovery/reference.

## Checks before publication

1. Run source/asset publication preflight and inspect the exact staged file list.
2. Run regression tests, targeted runtime checks and full Android compilation.
3. Verify APK package/version, certificate, signing schemes, alignment and hashes.
4. Verify the APK contains the exact public allowlist and no personal media/music.
5. Inspect README, release notes, installation instructions and license notices.
6. Publish source/tag and release attachments, then verify public downloads by hash.
7. Retain the Public Preview label until the exact public APK has hardware coverage.

Private vulnerability reporting was enabled for the repository during the first
publication. Historical v1.10.56 attachments were independently downloaded and
hash-verified. The new release's publication report records its own verification.

## Release attachments

Signed public APK, SHA-256 checksum, installation/update guide, LICENSE, NOTICE,
third-party notices, release notes and sanitized build/test report. Never attach a
complete laptop source ZIP, release keystore, password or personal diagnostic log.

## Testing boundary

Installation/launch of the personal build on two TVs is separate from public APK
validation. Detailed playback and real-device phone navigation checks remain
pending. See [RELEASE_NOTES.md](RELEASE_NOTES.md) for actual results and limitations.
