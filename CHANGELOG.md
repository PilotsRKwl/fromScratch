# Changelog

RuneLite Plugin Hub distribution is temporarily paused pending review after the initial submission was merged and then disabled with the reason `disallowed readme`. See [current status](docs/LAUNCH.md#current-status). Friends Beta 10.8.1 is preserved as a historical standalone release. No v1.0.0 has been published. Git history preserves the earlier development chronology.

## Unreleased: README compliance corrections

- Prioritize normal RuneLite Plugin Hub installation, clearly marked unavailable pending review. Remove README promotion of the external Windows beta installer and standalone launcher.
- Correct outdated submission/availability statements and retain beta maintenance and migration guidance as historical reference.
- Production plugin, gameplay, progression, releases, tags and packaging tools are unchanged.

## Unreleased: Plugin Hub preparation

- Explain the voluntary fresh-start game mode, three-step onboarding and separate solo/friends challenges. Keep beta installation and current approval status accurate.
- Consolidate current gameplay, troubleshooting, maintenance, privacy, review and launch documentation. Remove superseded milestone/history instructions after preserving useful guidance. Retain active beta/preview tooling, all progression migrations/tests, notices and released artifacts.
- Improve matching plugin discovery descriptions/tags without changing the plugin name, author, configuration or storage identifiers.
- Remove direct Bank Tags configuration writes. Its public open/close service now owns remembered-selection persistence and stale temporary-view cleanup; native search, tabs, saved user tags, preferences and accounting remain intact.
- Keep Java 11/standard build and production artifact isolation. Add Windows CI documentation/link/image checks alongside the complete existing Windows/Ubuntu regression.
- No new gameplay feature, progression schema or historical price changes. No v1.0.0 release or official approval claim before actual acceptance.

## Friends Beta 10.8.1: More Buybacks & Bank Search Fix

[Published release and installer](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.8.1-buybacks-search-fix)

- Add 34 fixed-price utility, weapon, armour and imbued-ring entries. Five new complete skilling outfits cost 2M virtual GP each. Existing 31 entries and historical prices remain unchanged.
- Order Utility & Storage, Weapons, Armour and Skilling Outfits with deterministic alphabetical entries. Preserve exact-copy Slayer selection and confirmation.
- Fix native Virtual Bank search input closing during a bank build while retaining native-query/virtual-ownership intersection.
- Use supported contents evidence or explicit empty/accounted declarations; unknown container contents never become owned automatically. Quiver direct/upgrade payments total 60M, with no charge for an independently earned blessed copy.
- Retain package 0.10.8-beta.2, Windows 0.10.8.1 and the existing save format. Final installer metadata, runtime/manifests and public asset hashes were verified.
- **Deferred: blocked by local Windows Application Control. Manual installation, upgrade, and uninstall testing pending.** No installer execution success is claimed.

## Friends Beta 10.8: Bug Fixes & Buyback Additions

[Published release](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.8-bug-fixes-buybacks)

- Add one-copy-at-a-time Black Mask/Slayer Helmet buybacks for 1M and Elite Black Knight pieces for 30K each, with exact-item tile selection.
- Introduce format 11 selected-copy receipt/count evidence while retaining formats 5 through 10, old family purchases and private export/import.
- Add native-search ownership intersections; 10.8.1 later corrects the input-closing lifecycle issue.

## Friends Beta 10.7: Potion Storage and Overdraft Protection

[Published release](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.7-potion-storage-overdraft)

- Add dose-based Potion Storage, setup/review, synchronized transfers and persistent overdraft warnings. Preserve existing bank, GE, buyback, save and local backup behavior.

## Earlier Friends Betas

- [10.6 native installer](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.6-friends-installer): per-user Windows launcher/wizard, bundled private runtime and retained notices.
- [10.5 original Friends Beta](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.5-friends-beta): the earlier standalone distribution remains available for historical users.

Published releases/tags are immutable. See [maintenance](docs/MAINTAINING.md) for current tooling and [the launch plan](docs/LAUNCH.md) for the future approved v1.0.0 release.
