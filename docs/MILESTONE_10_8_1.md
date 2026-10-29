# Friends Beta 10.8.1

This update adds 34 entries to the existing buyback catalog and fixes Virtual Bank native search. The release remains a Friends Beta; no Plugin Hub submission or approval is implied. [Fixed prices and supported variants](technical/BUYBACK_PRICING.md) include the additional eligible Ring of Wealth (i). Existing 31 entries and prices remain intact.

Categories are Utility & Storage, Weapons, Armour and Skilling Outfits, in that order. Tiles and category/search results use case-insensitive display-name order with ID as the stable secondary key. Complete new outfits cost 2M; quiver direct/upgrade payments total 60M. All new families share one-copy accounting across approved forms. An independently earned blessed quiver cannot be charged as an upgrade merely because a regular quiver was purchased earlier.

The five gathering containers and quiver ammunition use the explicit empty/shared-store declaration workflow described in the [accounting policy](technical/ACCOUNTING.md). Native nonempty evidence vetoes payment; unknown contents are never automatically empty. Declaration provenance, entitlement and GP payment commit together through the existing worker/CAS path. Old 10.7 checkpoints and 10.8 receipts retain their original prices and evidence; export/import and failed-publication recovery use the existing repository.

## Native search correction

The 10.8 input-closing regression was caused by forcing `bankBuildTab` to single-tab mode during a native search. In the pinned [RuneLite 1.13.4 BankMainBuild script](https://github.com/runelite/runelite/blob/runelite-parent-1.13.4/runelite-client/src/main/scripts/BankMainBuild.rs2asm), a callback result of 1 jumps to `singletabbuildmode`, then `LABEL879`. If the script's cached `bankmain_searching` result is 1, that path invokes script 299 (`meslayer_close`). The first native timer or deferred bank layout after opening search therefore closed the message layer. A correct item-filter predicate did not prevent this earlier layout-side effect.

Core [Bank Tags](https://github.com/runelite/runelite/blob/runelite-parent-1.13.4/runelite-client/src/main/java/net/runelite/client/plugins/banktags/tabs/TabInterface.java) deliberately calls `closeTag(false)` at search-toggle pre-fire, clearing its active tag without resetting or rebuilding the native search. From Scratch retains its cached ownership intersection through that temporary null tag. It now also preserves the native whole-bank build path while the native input mode is Search, or when a submitted native query remains. This mirrors `bankmain_searching`, including an empty open input. The existing priority -1 `bankSearchFilter` callback still vetoes items outside positive virtual ownership after native/core search matching. Clearing or closing search resumes the existing Virtual Bank layout; search does not change its preference.

No input is injected or reopened, no bank tag is repeatedly activated, and no quantities or physical observation sources are changed. Native tabs, Full Bank and Potion Storage retain their existing lifecycle guards.

## Search regression coverage

`NativeBankSearchLifecycleTest` combines actual `VirtualBankMode` callbacks, a bounded deferred client queue, synthetic message-layer state and the input-closing branch from the pinned RuneLite script. Before the correction, the first opening test failed with `Opening search must survive the native bank build`. Its coverage includes:

- Opening search, completing native builds and draining all queued callbacks.
- Character-by-character text entry, ownership/native-query intersections, legacy-only and no-native-match results.
- Repeated open, clear, close and reopen operations, including an entirely empty virtual bank.
- Submitted query retention, native tabs, Potion Storage transitions and Full Bank search.
- Membership refreshes queued during text entry, bounded rebuilds and unchanged preferences.
- A controller/worker/repository integration proving search leaves acknowledged progression unchanged.

Existing `VirtualBankModeTest` remains in use for readiness, authoritative bank observations, tab intersections, independent tags, explicit OFF, re-entry and lifecycle failures. Developer debugger probes were attempted against synthetic tests; the available IDE launch ran tests without attaching the installed logpoints, so no debugger runtime evidence is claimed.

## Automated local validation

- Java 11 incremental compilation and 402 focused tests across 18 suites passed, with zero failures, errors or skips.
- The complete `gradlew build` regression passed: 1,337 tests across 84 suites, zero failures, errors or skips. This includes bank/potion/GE accounting, overlays, corrections, character isolation, old saves, backups and buyback persistence.
- Standard production-only compilation passed against official Plugin Hub tooling revision `034eaffd5bce7bb678d35287b547874f3dba6b3b`, RuneLite `1.13.4`; standard template SHA-256 `62af0c5e207ffb0285773ecb6346ebc8f29d676691ffd919dde0cb26ab73a3a5`. No submission was created.
- Documentation/local-link/PNG checks, PowerShell syntax and pinned release/package/Windows version checks passed. Detailed local logs stay under ignored `build/release-logs/`.

Publication additionally requires both hosted platforms on the final commit and the existing committed-source beta/native installer checks. The installer embeds its source SHA/tree, package/release versions, dependency manifests and pinned runtime hashes; public downloads must match the validated local EXE and sidecar. Previous releases are preserved. Build evidence is distinct from the native gameplay checks below.

## Owner-only verification

This is **Case A: a genuine production lifecycle defect**, supported by the pinned native close branch and a failing-before/passing-after regression. The native game script interpreter and actual input focus cannot be fully executed by these headless fixtures. The owner explicitly deferred manual native search validation and authorized release after the automated gates pass; no manual verification is claimed. Only the owner operates RuneScape. In the development client, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) for authentication, then verify:

1. With Virtual Bank enabled, open the native Search input and type an owned item name. Keep typing after multiple refreshes; focus and query must remain intact.
2. Search a legacy-only item and a nonexistent name, clear the query, close and reopen search repeatedly, and repeat with no positive virtual items.
3. Change native tabs, visit and return from Potion Storage, close and reopen the bank, then test ordinary search with Virtual Bank disabled. Preserve native navigation behavior and the saved preference.
4. Confirm searching changes neither signed balances nor buyback GP, history or entitlements.

Automated tests, compilation, a launcher smoke check or a logged-out client start are not live gameplay acceptance. Final test counts, exact-commit CI and installer/publication evidence belong in the release validation record once those gates have run.
