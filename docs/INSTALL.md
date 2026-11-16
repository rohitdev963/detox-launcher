# Install and update Detox

## Requirements

An Android TV or Google TV device, Android API 21 or later, a remote/controller,
and free storage for the APK, installation and imported media. The public build is approximately 2.3 MB; leave additional free space for
Android's installer and your imported media.
Default-Home selection and video decoders vary by manufacturer. Non-Android TV
operating systems cannot run Android APKs directly.

## Install a published release

1. Open this repository's Releases page and download its signed `.apk` and checksum.
   GitHub's automatically generated source ZIP is not an installable application.
2. Transfer the APK to your TV with USB or a trusted local transfer/file-manager app.
3. In Android settings, allow that installer/file-manager to install unknown apps
   when prompted. Open the APK and confirm Android's installation prompt.
4. Press HOME and select Detox if the TV offers a Home chooser, or select it under
   the device's default-app/Home settings where available. These menus vary by TV.

Some TVs restrict third-party default launchers. Do not disable or uninstall the
stock launcher to work around this automatically. Keep it available for recovery.
If the device does not offer a supported chooser, report its model and Android
version; a universal default-Home switch is not promised.

## Optional ADB installation

Install Android SDK Platform Tools on your computer, enable the TV's supported
USB/network debugging mode, connect and accept the TV authorization prompt. Follow
the device's instructions for pairing/network ports rather than assuming a port.
Check `adb devices` and use the actual connected serial:

```sh
adb -s SERIAL install -r Detox-1.10.56-release.apk
adb -s SERIAL shell am start -n com.ansub.tvhome.rain/com.ansub.tvhome.MainActivity
```

The second command opens Detox; it does not change the default Home role. No root
or device reset is required. Turn debugging back off when it is no longer needed.

Official Android references: [ADB](https://developer.android.com/tools/adb) and
[app signing](https://developer.android.com/studio/publish/app-signing).

## Verify and update

Verify the APK SHA-256 using the checksum published with the same release:

```powershell
Get-FileHash .\Detox-1.10.56-release.apk -Algorithm SHA256
```

```sh
sha256sum Detox-1.10.56-release.apk
# macOS alternative: shasum -a 256 Detox-1.10.56-release.apk
```

Use Detox's Backup & Restore before updating when possible. Install the newer
official APK over the existing app. Android requires compatible signing identities
for in-place updates. A signature-conflict error can indicate a self-built or older
differently signed installation; stop and inspect it before changing anything.
Uninstalling deletes app-private data and is not a normal update instruction.

## Troubleshooting

| Symptom | Check |
|---|---|
| APK does not install | Android version, free space, checksum and installer permission |
| Signature/update conflict | APK source and certificate; do not clear data |
| Version downgrade rejected | Use the intended newer official release |
| Detox is installed but hard to open | Home/default-app chooser, or the explicit ADB launch above |
| No supported default-Home option | Manufacturer restriction; keep the stock launcher |
| Video background fails or stutters | Try a static wallpaper or another clip; decoder support varies |
| Clock alarm/timer does not open | An installed compatible system Clock application is required |

Report issues with model, Android version, Detox version and reproducible steps.
