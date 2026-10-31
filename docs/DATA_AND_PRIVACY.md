# Data and privacy

From Scratch has no backend, account registration, analytics endpoint or cloud synchronization. Its production plugin does not ask for passwords/tokens or access credential stores. It sends no bank contents, character keys, progression, chat, exports or screenshots to a From Scratch service.

## Local progression

The production plugin uses RuneLite's normal scoped Filepath directory `.runelite/plugin-data/from-scratch` beneath the normal user home. Published Friends Beta versions use the historical sibling `.runelite/plugin-data/from_scratch`. The production plugin does not inspect, move or delete that old directory. A separately executed, explicitly authorized [offline utility](MAINTAINING.md#offline-progression-migration) copies and verifies it before an existing beta player switches.

Character-specific records contain run identity/start, the original legacy bank snapshot, signed/current bank quantities, journal/checkpoint history, manual corrections, buyback payments/entitlements/declarations/contents offsets, observed GE evidence, financial/peak records and host playtime. Potion Storage records include verified quantities, dose definitions, timestamps and bounded transfer/review evidence. Existing character identifiers, save formats and configuration keys are unchanged by the directory cutover.

The canonical root also contains local migration-completion evidence or an explicitly confirmed new-installation record. Both economic progression and playtime share the verified directory. Incomplete/conflicting evidence blocks storage instead of initializing a new challenge. The plugin cannot automatically distinguish an empty new installation from unmigrated beta data outside its scope; first-run guidance makes the beta player's transfer responsibility explicit. Retained old-beta and canonical histories do not synchronize or merge.

Identifiers and quantities are private progression, not anonymous statistics. Checksums, atomic saves, revision checks and retained archives protect integrity rather than confidentiality. Saves and `.fsp` exports are **not encrypted**. Secure your local account and backups, and never attach those files to a public issue.

Export includes only the selected character's required progression evidence, not credentials or unrelated settings. Import checks format, same-character identity, checksums, bounded paths/size, complete evidence and revision, then previews replacement and preserves the previous run. It never merges histories or silently creates a new run when loading fails. Current limits include 256 MB total and 4 MB per component. Transfer requires user-selected local files.

## Network requests and authentication

The plugin requests RuneLite's bulk public item-price feed through the existing ItemClient on a separate worker. Requests contain no player holdings or character key. Prices are cached; rendering and Examine perform no synchronous HTTP.

RuneLite itself handles its normal game downloads/connections, authentication, updates and Plugin Hub traffic. Those services are not From Scratch progression synchronization. Use RuneLite's supported login flow; From Scratch has no custom sign-in or account system.

## Historical standalone beta

These details document existing beta installations and developer tools. They do not recommend a standalone launcher as an alternative to [Plugin Hub installation](INSTALLATION.md).

The Windows launcher and developer bootstrap are separate from the production Plugin Hub plugin. They invoke an unmodified official RuneLite runtime through its developer entry point and use RuneLite's Jagex Accounts panel. They do not extract tokens, copy credential files, enable insecure credential writing or scrape another process's environment.

The beta installs per-user under `%LOCALAPPDATA%\Programs\FromScratchBeta` with owned version payloads, a private checksum-verified Temurin Java 11 runtime, desktop/Start Menu shortcuts and its own Installed apps entry. It uses the normal RuneLite home so existing local progression remains in place. Close other clients before restoring or using the same run. No persistent PATH or system Java change is made.

The uninstaller is designed to remove owned program files and matching shortcuts while preserving progression, exports, RuneLite account/settings files, other plugins, unrelated files and Java installations. See [the historical beta reference](HISTORICAL_BETA.md) for the explicit 10.8.1 installer execution-testing deferral.

Launcher diagnostics are local under `%LOCALAPPDATA%\FromScratchBeta\logs`, bounded and retained after uninstall. No log is uploaded automatically. Developer verification uses disposable homes and never opens a personal account.

## Reporting safely

Hidden developer diagnostics are off by default and use local rate-limited DEBUG logs. Errors and client logs may contain private context. Share only a short relevant redacted excerpt: remove account/character identifiers, private paths, authentication details and progression. Do not post whole logs, settings folders, save exports or authentication screenshots.

[Technical reviewer map](PLUGIN_HUB_REVIEW.md) documents the source boundaries. [Troubleshooting](TROUBLESHOOTING.md) explains useful public reports.
