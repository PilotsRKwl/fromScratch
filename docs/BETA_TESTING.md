# Manual beta checks

The owner reports that the currently implemented Friends Beta gameplay functions correctly. This checklist is for relevant future changes and reproducible issue reports; it is not a claim that automated tests operated RuneScape. Only the player performs authentication and gameplay.

Use [Installation](INSTALLATION.md), [Gameplay](GAMEPLAY.md) and [Troubleshooting](TROUBLESHOOTING.md). Export a private backup before updating. Use disposable progression for reset/import-replacement experiments; never reset a real run to create a screenshot.

## Bank views and search

1. Start only after banking inventory/equipment and confirming the complete bank and any unlocked Potion Storage capture. Before Start, the full bank remains available.
2. Check physical/virtual quantities and locked shading, including a mixed stack. Normal withdrawals still operate on the real stack.
3. With Virtual Bank enabled, open native Search, type an owned item, search a legacy-only item and a nonexistent name, clear, close and reopen repeatedly. Check focus, matching results and the saved preference.
4. Change native tabs, visit and return from Potion Storage, close/reopen the bank, and compare ordinary Full Bank search. Presentation alone must not change balances or history.
5. Examine an item normally: game/core messages come first, followed by one exact signed From Scratch line.

## Progress and persistence

- Bank a new item, withdraw within its virtual allowance, then return it. On disposable progression, check one deliberate overdraft and repayment; warnings remain visible when the item slot disappears.
- Inspect buyback categories, search and confirmation. Cancel first. A purchase must charge once, unlock only its permitted copy and survive restart. Do not spend valuable virtual GP just to create a demonstration.
- Container purchases exclude legacy contents. Verify native offsets or the explicit empty/accounted declaration as applicable; never guess unreadable contents.
- Check supported bank/Potion Storage transfers across dose sizes without duplicate value, new ownership or presentation-triggered warnings.
- Inspect completed GE purchases/sales and positive partial cancellations. Pending/unfilled offers should not appear as completed trades.
- Restart and confirm saved quantities, entitlements, history, settings, peak and Time Played. Preview/cancel an import; test actual replacement/reset only on disposable progression.

## Windows installer execution

**Deferred: blocked by local Windows Application Control. Manual installation, upgrade, and uninstall testing pending** for the published 10.8.1 installer.

Where an approved Windows environment permits the unsigned EXE, manually check ordinary installation, existing-run upgrade, shortcuts, launch/login and uninstall preservation. Keep security protections enabled. Confirm progression, backups, RuneLite settings/account data, other plugins and unrelated files remain intact. Automated manifest/runtime checks are separate from executing Setup.

Report the version, relevant view, steps, expected/actual behavior and a small redacted screenshot or error excerpt through [Issues](https://github.com/PilotsRKwl/fromScratch/issues). Never upload credentials, whole settings/log folders or `.fsp` exports. See the [privacy boundaries](DATA_AND_PRIVACY.md).
