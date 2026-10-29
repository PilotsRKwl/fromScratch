# Milestone 10.8: Bug Fixes and Buyback Additions

Target: `v0.10.8-bug-fixes-buybacks`, Friends Beta pre-release. Package `0.10.8-beta.1`, Windows `0.10.8.0`. Author: Pilots r kwl. The owner authorized source publication and beta distribution; official Plugin Hub submission remains Milestone 11.

## Changes

- [Issue #1](https://github.com/PilotsRKwl/fromScratch/issues/1): `VirtualBankMode.searchSelected()` previously selected Full Bank and cleared the saved preference. RuneLite 1.13.4 already closes its tag without relayout on native search. The plugin now keeps its cached native-tab ownership intersection after that close. Its existing priority -1 filter only vetoes nonmembers after the native/core search result. It never sets a native miss to true, changes input, reopens tags or observes quantities from widgets. Independent user tags, explicit OFF, bank lifecycle and Potion Storage guards remain intact.
- [Issue #2](https://github.com/PilotsRKwl/fromScratch/issues/2): `SelectedBuybacks` explicitly lists 104 current IDs: 44 Black Masks (charges 0 through 10, ordinary/Nightmare Zone/Soul Wars/Emir's Arena) and 60 helmets (15 appearances, each ordinary and three imbue sources). The appearances include regular, black, green, red, purple, turquoise, hydra, twisted, TzTok, vampyric, TzKal, araxyte, hooded, oathplate and radiant. The tile chooser purchases one exact ID/copy for 1M GP. A single eligible variant goes directly to its named confirmation. Other physical variants and duplicate copies stay locked until separately earned or purchased. Only the eleven explicit ordinary-mask IDs receive the narrowly scoped tradeability exception; notes/placeholders remain excluded.
- [Issue #3](https://github.com/PilotsRKwl/fromScratch/issues/3): independent Elite Black Full Helm, Platebody and Platelegs buybacks cost 30K each, per copy. They use `ItemID.ELITE_BLACK_FULL_HELM`, `ELITE_BLACK_PLATEBODY`, `ELITE_BLACK_PLATELEGS` (29560/29562/29564). Elite Void entries, prices and upgrades are unchanged.
- No Achievement Diary category is added. Newly obtained replacement rewards follow ordinary bank deltas; existing diary completion never unlocks legacy copies.

## Accounting and compatibility

Historical family roles retain their level and one-copy meaning. New exact-ID roles count receipt-backed paid copies. Each new receipt has a distinct `selected` object containing item ID and quantity 1, while existing pouch/bag evidence is unchanged. Existing forced journal/checkpoint publication, CAS, revision/character guards and save acknowledgment remain the sole progression authority. Payment, selected entitlement and history commit atomically; no raw physical/balance mutation or acquisition is manufactured.

The bank-floor rule preserves signed returns and caps projection by physical stock. A paid copy currently withdrawn must return before another purchase of the same ID. A receipt never licenses every variant. Bank-only observation cannot prove unseen assembly, charging, imbuing or recolouring: normal exact-ID bank deltas apply, and entitlement is not automatically transferred across IDs.

Format 11 is explicit and backward compatible with formats 5 through 10. A format-10 load preserves original bytes until the next acknowledged transaction, retaining the previous checkpoint and journals. New selected evidence cannot be disguised as an older format. Full receipt validation rejects unknown IDs, wrong categories/quantities/floors/payment totals, duplicate identities and unsupported fields. Existing exports/imports carry these same receipts and checksums. Export before upgrading; older betas cannot read format 11.

## Validation and release gates

Use the Gradle wrapper with Temurin Java 11. Focused tests exercise the real search callback, every approved variant, chooser and final confirmation, controller/worker/repository pipeline, duplicate submissions, changed bank/character, save failure/retry, paid/earned/negative quantities, restart, historical receipts and export/import. The final complete build and exact-commit Windows/Ubuntu CI must pass before final packaging and publication. Detailed logs/XML, source metadata, manifests and published-download hashes remain in the ignored `build/reports/m10.8` and existing packaging output directories.

The existing native installer is reused with pinned Temurin 11.0.32.1+1 and Inno Setup 6.7.3. Its validation uses isolated synthetic data, including a 10.7-version upgrade fixture, per-user shortcuts, paths with spaces, private runtime, damaged-payload rejection, safe reinstall/uninstall and preserved settings/progression. This fixture checks the installer transition using the current payload; actual older-client upgrade and authentication remain player checks. Final source and tree hashes must match the committed CI-validated release; dirty candidates are not published. Generated binaries, logs, caches and test saves never enter Git.

## Plugin Hub preparation

Production remains Java 11 with no added dependencies, input injection, packets, process execution or reflection. Local UI actions change only challenge bookkeeping. The production JAR excludes beta launcher and native tools; the official standard-template compilation is separate from Friends Beta packaging. [Reviewer documentation](PLUGIN_HUB_REVIEW.md) discloses the supported Bank Tags callbacks and public quantity-display mode. No Hub manifest, fork, pull request, submission or approval is created or claimed.

References reviewed against the installed RuneLite 1.13.4 API: [official gameval constants](https://github.com/runelite/runelite/blob/runelite-parent-1.13.4/runelite-api/src/main/java/net/runelite/api/gameval/ItemID.java), [native search lifecycle](https://github.com/runelite/runelite/blob/runelite-parent-1.13.4/runelite-client/src/main/java/net/runelite/client/plugins/banktags/tabs/TabInterface.java), [Slayer helmet variants](https://oldschool.runescape.wiki/w/Slayer_helmet), [Elite black armour](https://oldschool.runescape.wiki/w/Elite_black_armour), and [official review guidance](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review).

## Owner gameplay checks

1. Search with Virtual Bank ON: owned match, legacy-only match, empty results, clear search, repeat, change native tabs, visit Potion Storage, close/reopen. Confirm query/focus and preference remain intact, while OFF and independent tags work normally.
2. Bank several legacy variants and identical masks. Switch tiles and cancel both stages; confirm no payment. Buy one exact copy for 1M, verify other variants stay locked, repeat for another identical copy, withdraw/return, restart and inspect history. Verify earned copies need no payment.
3. Buy each Elite Black Knight piece independently for 30K; confirm duplicates require another payment and Elite Void is unchanged.
4. Export privately, close the client and upgrade from 10.7. Verify existing progression, synchronization, overdrafts, pouch/bag entitlements and history. Uninstall preserves progression and unrelated RuneLite data.

Only the owner performs RuneScape gameplay and authentication. Automated validation does not establish live acceptance. Issues remain open for these checks.

## Local release checkpoint

- Temurin 11.0.32.1+1; incremental Java compilation passed.
- Focused search/buyback/persistence/backup run: 419 passed, zero failures/errors/skips. Additional metadata and new-copy coverage passed before the complete run.
- Final command: `.\gradlew.bat build --rerun-tasks --no-daemon --console=plain`. Exit 0; **1,291 passed, zero failures/errors/skips**, 1m 39s. An earlier run found an obsolete 0.10.0 metadata assertion; it now compares metadata to the pinned current toolchain configuration. No test was disabled or assertion removed.
- Official standard template: RuneLite 1.13.4, tooling revision 034eaffd5bce7bb678d35287b547874f3dba6b3b. Production compile passed separately; this is not a Plugin Hub submission or policy approval.
- Current installer configuration checks passed (2); final installer validation and exact-commit CI remain publication gates. Logs/manifests record their actual results rather than treating earlier 10.7 runs as evidence for 10.8.
- Changed-document relative links and assets resolved; root icon is a 48x48 PNG. Restricted-API production scan found no reflection, runtime code loading, process execution, synthetic actions, packet calls or direct Files IO. Existing supported overlay/Bank Tags APIs remain disclosed for review.
