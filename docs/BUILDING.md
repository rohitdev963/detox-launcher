# Build Detox from source

## Requirements

- Git. The original samples are small, so Git LFS is not required.
- Java Development Kit **17** (`JAVA_HOME` points to the JDK).
- Android SDK command-line tools with **platforms;android-34** and
  **build-tools;34.0.0** (`ANDROID_HOME` points to the SDK).
- Python **3.9+** for the portable build helper; no Python packages are required
  to build the APK. Some optional artwork verification scripts use Pillow.

This project uses native Java and the Android SDK tools directly. It does not use
Gradle and does not require the maintainer's private Windows account or keystore.
Install SDK packages with Android Studio's SDK Manager or `sdkmanager`:

```sh
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

Use your shell's normal commands to set `JAVA_HOME` and `ANDROID_HOME`. The helper
also accepts `--java-home PATH` and `--android-sdk PATH`. Windows, Linux and macOS
use the same Python entry point; SDK tools must match the operating system.

## Compile without signing

```sh
python tools/build.py --unsigned
```

This compiles resources and Java, builds DEX, assembles and aligns an unsigned
debug APK. **Unsigned APKs cannot be installed.** The helper prints its output
path. It does not install anything or change the main manifest. Generated scratch
files are removed after a build. Existing output APKs are never overwritten.

## Build an installable development APK

Create your own development key in a private directory outside the repository,
using Java's `keytool` in an interactive terminal. For example:

```sh
keytool -genkeypair -keystore /YOUR/PRIVATE/PATH/detox-dev.jks -alias detox-dev -keyalg RSA -keysize 2048 -validity 10000
```

Use a real private path suitable for your OS. `keytool` prompts for credentials;
use the same password for the key entry and keystore. Keep that key for updates
to your own build. Do not commit it or use it to impersonate the official release.

```sh
python tools/build.py --keystore /YOUR/PRIVATE/PATH/detox-dev.jks --alias detox-dev
```

The helper prompts for the password only when running in an interactive terminal.
It does not echo the password or pass it as a command-line value. For automation,
provide a protected `DETOX_BUILD_PASSWORD` environment variable; never put its value
in source, workflow YAML or logs. `--password-environment NAME` selects a different
variable. No key is generated automatically, and no hidden input prompt is opened.

For your own signed release build, add `--configuration Release`. Use `--output PATH`
when you need a different filename. The official release signing key is private;
self-built APKs cannot normally update an official installation. See
[INSTALL.md](INSTALL.md) before using them on a device with Detox already installed.

## Regression tests

On Windows or with PowerShell 7:

```powershell
./validate.ps1 -JavaHome $env:JAVA_HOME
```

The validator compiles/runs the host Java tests, parses XML and checks expected
media inventories. The optional Python runtime harnesses execute actual production
classes against small Android stand-ins; they are not emulator or hardware tests.

```sh
python tests/verify-gaming-video-runtime.py --java-home "$JAVA_HOME"
python tests/verify-photo-metadata-runtime.py --java-home "$JAVA_HOME"
python tests/verify-menu-typography-runtime.py --java-home "$JAVA_HOME"
python tests/verify-companion-input-runtime.py --java-home "$JAVA_HOME"
python -m unittest discover -s tests -p test_video_media_policy.py -v
```

The build stages only files in `tools/public-assets.json` and verifies their hashes.
Additional private files elsewhere in `assets/` are ignored. Do not remove this
allowlist or replace it with a recursive copy of all assets. No sample media is
downloaded during a build. If a tracked sample is missing or changed, restore it
from the repository or review the change and update provenance and manifest hashes.

To regenerate the original backgrounds, install Pillow and FFmpeg, then run
`python tools/generate_public_media.py --ffmpeg PATH_TO_FFMPEG`. These tools are for
artwork regeneration only; ordinary builds use the committed samples. Review the
new files, update `tools/public-assets.json` hashes and run the media checks after
regeneration. The samples contain original procedural art and no soundtrack.

An optional browser test uses Node.js 22+ and an installed Chromium browser:

```sh
node tests/verify-companion-browser.mjs
```

It starts a loopback mock TV and a fresh generated browser profile under `build/`.
Set `DETOX_TEST_BROWSER` to your Chromium executable; the Windows default is Edge.
It does not connect to a real TV or use your normal browser profile. This checks
the web client, not Android's hardware input stack.

## Optional local media edition

Public remains the default. `--edition personal` explicitly opts into a private
`assets/wallpapers/personal-catalog.json` profile whose files and hashes must match.
It generates only four media catalogs under `build/`; Home and Gaming activities
are compiled directly from the current source. A profile is not included here,
and neither personal media nor signing credentials are needed for a public build.
Do not publish a personal APK or profile without reviewing its contents and rights.
Local-profile tests skip when that private profile is absent.

## Release identity

The current official identity is `com.ansub.tvhome.rain`. Official releases preserve
that package, increase versionCode and use the same release certificate. Release
APK checksums and certificate fingerprints are published with releases. Build
scripts do not change the default Home launcher, access a TV or publish to GitHub.
