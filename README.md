# Detox Launcher

A customizable Android TV Home launcher with remote-first navigation, glass-style
settings, a Gaming library, live backgrounds and native clock faces.

## Install on your TV

Download the **signed release APK** and its SHA-256 file from this repository's
Releases page. You do not need to clone or compile the source to use Detox.
Follow [the installation and update guide](docs/INSTALL.md).

The public edition is **1.10.56 / build 112**. Public distribution is still
being prepared; a download is available only after a release is actually published.

## Features

- Installed-app launching, visibility, ordering, icon packs and artwork controls.
- Home and Gaming screens with image/video backgrounds.
- Three original still wallpapers and two silent, lightweight 1080p60 motion samples.
- 17 live native clock styles, including 12 premium faces, with preview and settings.
- Remote D-pad and pointer navigation, utility shortcuts and translucent menus.
- Wallpaper import, slideshow/framing controls and reduced-motion options.
- Settings backup/restore and named presentation profiles.
- Optional local-network Phone Companion with pairing and supported Detox controls.

Some utilities use installed Android system apps. Optional media-session and
accessibility integrations require explicit system access. Phone input controls
Detox; it does not promise arbitrary third-party app control.

## Compatibility and testing

- Designed for Android TV / Google TV with a remote or controller.
- Minimum declared Android version: Android 5.0 / API 21; target API 34.
- Java bytecode only, with no bundled native ABI-specific library.
- Video decoding and the ability to choose a default Home app depend on the device.
- Samsung Tizen and LG webOS are not Android and cannot install this APK directly.

The maintainer reports the installed Detox build works on their TV. This is
user-reported testing on one device, not a guarantee for every model. The exact
installed build was not rechecked during open-source preparation. Local validation
is separate from the public edition's automated validation. The public edition has
46 host suites; build results and device-only limitations are documented with the
release. Hardware behavior on other models is unverified.

## Build from source

The public samples are bundled directly. Git LFS is not required:

```sh
git clone https://github.com/rohitdev963/detox-launcher.git
cd detox-launcher
```

See [BUILDING.md](docs/BUILDING.md) for Java 17, Android SDK and Python requirements,
portable build commands, testing and signing. Contributors use their own signing
identity; the official release private key is never included in this repository.

## Contribute and report problems

Read [CONTRIBUTING.md](CONTRIBUTING.md). Include the TV model, Android version,
Detox version and steps to reproduce when reporting a bug. Review logs before
sharing them and remove pairing codes, personal paths and network information.
See [SECURITY.md](SECURITY.md) for sensitive reports.

## License and credits

Detox's original source, documentation and sample backgrounds are licensed under
the [MIT License](LICENSE). Third-party fonts and icons retain their own terms; it does
not grant rights to those works. See [NOTICE.md](NOTICE.md) and the
[third-party notices](docs/THIRD_PARTY_NOTICES.md).

The public build uses an exact asset allowlist. Undocumented personal wallpapers,
videos and music retained on the maintainer's laptop are excluded from the APK
and public source upload. No music is bundled; wallpaper/video import remains.
