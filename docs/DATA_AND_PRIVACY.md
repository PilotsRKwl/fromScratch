# Data and privacy

From Scratch has no backend, account registration, analytics endpoint or cloud synchronization. Its production plugin does not ask for passwords/tokens or access credential stores. It sends no bank contents, character keys, progression, chat, exports or screenshots to a From Scratch service.

## Local progression

The production plugin uses the `progress` child of RuneLite's normal scoped Filepath directory: `.runelite/plugin-data/from-scratch/progress`. It does not read, copy or remove progression outside that child. A first installation begins a new challenge through guided setup; an existing official challenge resumes normally.

Character-specific records contain run identity/start, the original legacy bank snapshot, signed/current bank quantities, journal/checkpoint history, manual corrections, buyback payments/entitlements/declarations/contents offsets, observed GE evidence, financial/peak records and host playtime. Potion Storage records include verified quantities, dose definitions, timestamps and bounded transfer/review evidence. Character identifiers, save formats and configuration keys remain stable.

Economic progression and playtime share the verified official directory. Incomplete, corrupt or conflicting save evidence blocks loading instead of initializing a replacement challenge. No installation receipt or extra first-use acknowledgement is required. Reset and import remain explicit confirmations with recovery data preserved.

Identifiers and quantities are private progression, not anonymous statistics. Checksums, atomic saves, revision checks and retained archives protect integrity rather than confidentiality. Saves and `.fsp` exports are **not encrypted**. Secure your local account and backups, and never attach those files to a public issue.

Export includes only the selected character's required progression evidence, not credentials or unrelated settings. Import checks format, same-character identity, checksums, bounded paths/size, complete evidence and revision, then previews replacement and preserves the previous run. It never merges histories or silently creates a new run when loading fails. Current limits include 256 MB total and 4 MB per component. Transfer requires user-selected local files.

## Network requests and authentication

The plugin requests RuneLite's bulk public item-price feed through the existing ItemClient on a separate worker. Requests contain no player holdings or character key. Prices are cached; rendering and Examine perform no synchronous HTTP.

RuneLite itself handles its normal game downloads/connections, authentication, updates and Plugin Hub traffic. Those services are not From Scratch progression synchronization. Use RuneLite's supported login flow; From Scratch has no custom sign-in or account system.

## Reporting safely

Hidden developer diagnostics are off by default and use local rate-limited DEBUG logs. Errors and client logs may contain private context. Share only a short relevant redacted excerpt: remove account/character identifiers, private paths, authentication details and progression. Do not post whole logs, settings folders, save exports or authentication screenshots.

[Technical reviewer map](PLUGIN_HUB_REVIEW.md) documents the source boundaries. [Troubleshooting](TROUBLESHOOTING.md) explains useful public reports.
