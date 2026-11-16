# Public release checklist

Planned repository: **rohitdev963/detox-launcher**, approved by the maintainer.
Original source, documentation and original sample media use the **MIT license**.
Third-party icon/font licenses and notices are preserved.

## Public edition

- Main source: 1.10.56 / build 112, package `com.ansub.tvhome.rain`.
- Three original stills: Aurora Veil, Twilight Peaks and Ember Horizon.
- Two original silent 1080p60 loops: Aurora Drift and Ember Drift.
- No bundled music; navigation sounds and wallpaper/video import are retained.
- Both build paths use the exact 41-file reviewed public asset manifest.
- Private personal media, APKs, signing files and local diagnostics are excluded.
- Original samples total less than 1 MB; ordinary Git is sufficient, with no LFS.

## Before upload

1. Run `python tools/check_public_release.py`, regression tests and full compilation.
2. Review the concrete public source file list. Initialize Git only in the actual
   project, never in a parent laptop workspace. Do not stage all local files blindly.
3. Check files and history for signing keys, credentials, personal backups and logs.
4. Verify the signed APK's package/version, certificate, alignment and contents.
   Confirm that no excluded entertainment media is in the APK.
5. Review README, installation guide, MIT license, third-party notices and release notes.
6. Enable a private security reporting route on GitHub.
7. Review the source/tag and release attachments, then obtain publication approval.

## Release attachments

- Official signed public APK and matching SHA-256 file.
- MIT LICENSE and third-party notices accompanying the binary.
- Sanitized release notes and installation/update guide.

The old 1.10.55 personal APK is preserved locally and must not be uploaded as the
public edition: it contains undocumented media. No private complete-laptop ZIP or
signing directory may be attached. No GitHub push, release upload or TV install has
occurred during preparation.

## Device-testing record

The maintainer reports their installed Detox works. This is user-reported testing
on their TV; the installed version was not independently rechecked here. The new
public edition has different defaults and requires its own device validation.
Other models, manufacturer Home restrictions and decoders remain device-dependent.

## Local validation completed

46 host suites, 5 staging tests, 4 build-helper tests, 6 video-policy tests and
the runtime harnesses pass. Signed PowerShell and unsigned portable Windows
builds pass, and both package exactly 41 reviewed assets with no personal media
or music. Both original clips fully decode at 1080p60. The signed public APK is
2,256,837 bytes. Device tests and publication remain pending.
