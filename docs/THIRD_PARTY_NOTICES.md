# Third-party notices and media rights

## Included license documentation

| Component | Terms | Local documentation |
|---|---|---|
| realvjy/3Dicons | CC0 1.0 | `assets/licenses/3dicons/` |
| Microsoft Fluent System Icons | MIT | `assets/licenses/fluent-system-icons/` |
| Simple Icons | CC0 1.0; trademarks remain separate | `assets/licenses/simple-icons/` |
| Nunito, Orbitron, Pacifico, Playfair Display, Poppins | Individual SIL OFL notices | `assets/fonts/*-OFL.txt` |
| Premium clock scenery | Original project code/artwork, MIT | `docs/licenses/PREMIUM_CLOCK_ARTWORK.md` |

Retain the license files, attribution and provenance with distributed copies.

## Original public samples

`assets/public/` contains three original 1920×1080 still wallpapers and two silent
8-second 1080p60 video loops. They were authored procedurally by
`tools/generate_public_media.py`, without downloaded art, characters, logos,
stock photos, textures or audio. The root MIT license applies to this artwork;
its copy, provenance and file hashes are bundled with the samples.

## Excluded personal media

The maintainer clarified that no source/license/redistribution documentation is
available for the previously supplied entertainment wallpapers, videos and music.
Those files are retained locally for the personal build but are not part of the
public source or runtime asset manifest. Both build tools stage only the 41 files
listed in `tools/public-assets.json`. No personal music is bundled, and the public
UI hides music controls rather than presenting a broken player.

Do not reintroduce excluded files into a source upload or binary release without
an identified origin and adequate redistribution terms, including underlying
artwork/music rights where relevant. User-imported wallpapers/videos remain the
user's responsibility and are never relicensed by this project.
