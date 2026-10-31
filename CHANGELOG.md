# Changelog

RuneLite Plugin Hub distribution is temporarily paused pending review after the initial submission was merged and then disabled with the reason `disallowed readme`. See [current status](docs/LAUNCH.md#current-status). No v1.0.0 has been published. Git history preserves the earlier development chronology.

## Unreleased: Plugin Hub review corrections

- Prioritize normal RuneLite Plugin Hub installation, clearly marked unavailable pending review.
- Retain the production plugin, UI preview tools, legacy progression compatibility, offline migration utility and startup completion gate.
- Preserve gameplay, progression formats, historical prices and original data. No player progression is accessed or changed by repository cleanup.

## Plugin Hub preparation

- Explain the voluntary fresh-start game mode, three-step onboarding and separate solo/friends challenges.
- Consolidate current gameplay, troubleshooting, maintenance, privacy, review and launch documentation.
- Improve matching plugin discovery descriptions/tags without changing the plugin name, author, configuration or storage identifiers.
- Remove direct Bank Tags configuration writes. Its public open/close service owns remembered-selection persistence and stale temporary-view cleanup; native search, tabs, saved user tags, preferences and accounting remain intact.
- Keep Java 11/standard build and production artifact isolation. Add Windows CI documentation/link/image checks alongside the complete existing Windows/Ubuntu regression.

## Gameplay history: 10.8.1

- Add 34 fixed-price utility, weapon, armour and imbued-ring entries. Five new complete skilling outfits cost 2M virtual GP each. Existing 31 entries and historical prices remain unchanged.
- Order Utility & Storage, Weapons, Armour and Skilling Outfits with deterministic alphabetical entries. Preserve exact-copy Slayer selection and confirmation.
- Fix native Virtual Bank search input closing during a bank build while retaining native-query/virtual-ownership intersection.
- Use supported contents evidence or explicit empty/accounted declarations; unknown container contents never become owned automatically. Quiver direct/upgrade payments total 60M, with no charge for an independently earned blessed copy.

## Gameplay history: 10.8

- Add one-copy-at-a-time Black Mask/Slayer Helmet buybacks for 1M and Elite Black Knight pieces for 30K each, with exact-item tile selection.
- Introduce format 11 selected-copy receipt/count evidence while retaining formats 5 through 10, old family purchases and private export/import.
- Add native-search ownership intersections; 10.8.1 later corrects the input-closing lifecycle issue.

## Gameplay history: 10.7

- Add dose-based Potion Storage, setup/review, synchronized transfers and persistent overdraft warnings. Preserve existing bank, GE, buyback, save and local backup behavior.

See [maintenance](docs/MAINTAINING.md) for current tooling and [the launch plan](docs/LAUNCH.md) for the future v1.0.0 release gates.
