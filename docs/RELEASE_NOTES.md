# Detox Launcher 1.10.57 â€” Public Preview

Package: `com.ansub.tvhome.rain` Â· versionCode: `113` Â· minimum Android API: `21`.

This release brings the latest launcher source to the public edition. It keeps
three original still wallpapers, two silent 1080p60 motion samples, five fonts,
and the licensed local utility artwork. No personal entertainment media, bundled
music, TV settings exports, diagnostic logs or private signing files are included.
The previous v1.10.56 preview remains available.

## Changes

- Rebuilt Phone Companion web remote with a thumb-friendly D-pad, separate Remote,
  Apps and Send media views, connection feedback and accessible button labels.
- Bounded held-button repeats and queued input; stale commands are dropped instead
  of replayed after a connection delay. Expired sessions return to pairing, and
  restricted browser session storage has an in-memory fallback.
- Added the Android focus-navigation fallback bypassed by direct Activity/Dialog
  dispatch, while retaining modal ownership and existing custom key handlers.
- Phone Home explicitly returns to Detox. The launcher Settings command targets
  Detox, and connection status distinguishes Detox input from external apps.
- Enabling an existing photo slideshow restores its selected photo and disables
  a covering video wallpaper. Ordinary menus retain the slideshow deadline;
  preview/crop screens and Activity pause still suspend it safely.
- Improved bitmap downscaling hints for utility artwork and independent drawable
  state for live line icons. Dynamic network information remains dynamic.
- Added an explicit opt-in local media build profile; public builds continue to
  package only the reviewed public allowlist. No old Activity snapshot is used.

Existing preference keys, package ID and release certificate are retained. Use an
in-place update from the official v1.10.56 APK; no uninstall or data clearing is
required when the installed certificate matches. Other historical signing keys
may be incompatible: follow [INSTALL.md](INSTALL.md) and preserve your settings.

## Bundled public media

- Aurora Veil, Twilight Peaks, Ember Horizon â€” original 1080p stills.
- Aurora Drift, Ember Drift â€” original silent 8-second 1080p60 motion samples.
- No bundled music. The personal edition's music compatibility workaround is not
  present in this public APK because its soundtrack is not redistributable here.

Original media is MIT licensed; third-party font and icon licenses remain intact.
All assets are local and work without downloading media at runtime.

## Validation and remaining checks

- 47 Java host suites, five XML documents and the 41-asset public hash inventory.
- 21 Python tests on the maintainer's laptop (including six private-profile tests;
  those skip on a public-only checkout).
- Targeted Android stand-in tests: companion input 20 assertions, Gaming video 18,
  photo metadata 19 and menu typography 18.
- 16 isolated Edge browser checks against a local mock TV, including PIN/QR client
  flow, held directions, queue expiry, session loss and mobile/desktop layout.
- Full public Android release compilation, APK signing, package/version inspection,
  alignment, exact asset inventory and comparison with the v1.10.56 certificate
  all passed. The attached APK checksum and build report record those results.

The personal v1.10.57 APK was installed in place and launched on two TVs; TV 219's
installed APK hash and default Home were verified. That is not a device test of
this exact public APK. Pairing and authenticated status were exercised on a TV,
but complete real-device focus/Back, audible music smoothness and slideshow timing
checks were interrupted and remain unverified. Browser mocks and compilation are
not proof of those hardware behaviors. This release remains a Public Preview.

Phone direction/select controls operate inside Detox. Arbitrary third-party app
control is not promised; volume/media support depends on Android and the device.
