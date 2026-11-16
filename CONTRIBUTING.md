# Contributing to Detox

Use Java 17, Android SDK platform 34 and build-tools 34.0.0. Read
[the build guide](docs/BUILDING.md) before making changes.

Keep changes focused. Preserve existing preference keys or provide a tested,
backward-compatible migration. Do not clear unrelated preferences, import old
activity snapshots wholesale or reintroduce retired features.

For UI changes, check D-pad and pointer navigation, focus restoration, BACK to the
immediate parent, saved values and reduced-motion behavior. For media changes,
check loading, pause/resume, decoder cleanup and fallback without overwriting the
user's saved choice. Add a regression test when a reproducible defect needs it.

Run the host validation suite and compile the Android source. State which tests
were run locally and which were exercised on actual hardware. Do not report device
success based only on compilation or host Android stand-ins.

Do not submit keystores, passwords, pairing/session secrets, generated build trees,
APKs, user backups or private logs. Source contributions are submitted under the
project's MIT license, subject to any preserved third-party terms. Asset additions
need an identified source, explicit compatible redistribution terms and attribution.

A useful bug report includes:
- Detox version and whether it is an official or self-built APK.
- TV manufacturer/model, Android version and remote/input method.
- Reproduction steps, expected behavior and actual behavior.
- Relevant sanitized error output or screenshots.

Maintain the existing package and official signing identity for official releases.
Local contributors must use their own key; they must not request the release key.
