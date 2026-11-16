# Security

Do not post signing keys, passwords, pairing PINs, authentication tokens or private
logs in public issues or pull requests. The official release key is not part of
the public repository. Ignore rules are an extra precaution, not a substitute
for inspecting files and Git history before publication.

Use [GitHub's private vulnerability reporting feature](https://github.com/rohitdev963/detox-launcher/security/advisories/new),
enabled for this repository. If no private report option is available, open a minimal issue asking
the maintainer for a private contact without including exploit details or secrets.

When reporting, include the affected version, reproduction conditions and impact.
Remove device addresses and personal data unless they are essential and the
maintainer has provided a private channel.

Download official APKs from the repository's Releases page and verify the published
SHA-256. Official updates must retain the package and release certificate. A
self-built APK has a different signing identity and cannot normally update an
official installation. Do not uninstall or clear app data as an automatic remedy.
