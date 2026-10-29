# Artwork and attribution

## Original branding

From Scratch's purple coin icon was generated specifically for the project with imagegen and mechanically reduced with nearest-neighbor resampling. It is original project artwork, not an extracted RuneScape sprite. The design is three simple isometric purple coin stacks with transparent padding, intended to remain recognizable at sidebar size.

| File | Size | Purpose |
|---|---|---|
| `icon.png` | 48x48 PNG | Plugin Hub root icon |
| `src/main/resources/com/fromscratch/icon.png` | 24x24 PNG | Sidebar navigation and missing-sprite fallback |
| `beta/windows-native/FromScratch.ico` | 16/32/48/64/256 frames | Optional standalone launcher, Setup, uninstaller and shortcuts |

The PNGs are genuine PNG files. The ICO is derived from the root icon by `scripts/Prepare-WindowsIcon.ps1`; it does not introduce a different logo. These assets are supplied under the project's BSD-2-Clause terms to the extent applicable.

## Retired hero artwork

The former code-generated title banner and its generator have been retired. Git history preserves the original geometric chest/coin artwork and its provenance; it was original project artwork under BSD-2-Clause, not a RuneScape screenshot. The current coin logo and its attribution above remain unchanged. Branding implies no affiliation with or endorsement by Jagex, RuneLite or Steam.

## Interface illustrations

The PNG panel previews in `docs/media` were rendered from production Swing components using clearly synthetic sample progress and an isolated empty home. They are not live gameplay captures or a real account. Source: `src/preview/java/com/fromscratch/preview/InterfacePreviews.java`; reproduction: `scripts/Prepare-UiPreviews.ps1`. The dashboard overview is a crop of the full dashboard panel.

The existing buyback images predate the 10.8.1 category reorganization and catalog expansion. They remain identified as earlier-beta panel illustrations in [SCREENSHOTS](SCREENSHOTS.md), not evidence of the current menu order or complete item list.

Item sprites used inside those panels come from RuneLite's official `https://static.runelite.net/cache/item/icon/<itemId>.png` service, cached only under ignored `build/marketing/icons`. Those OSRS game assets belong to Jagex and are not relicensed as original From Scratch art. RuneLite fonts/look and feel retain their respective authors' rights. Runtime item icons use RuneLite's supported sprite API; no preview downloader or generation helper enters the production plugin.

No authentic RuneScape gameplay screenshot is currently included. [The capture checklist](SCREENSHOTS.md#owner-gameplay-capture-checklist) identifies what the owner can provide without exposing private data. Never fabricate gameplay or present synthetic previews as authentic screenshots.
