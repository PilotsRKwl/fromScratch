# Friend-and-family beta testing

This is an unpublished, unreviewed candidate until the owner approves distribution. Install only the approved owner-provided **FromScratch-Beta-Setup.exe** from the [canonical repository](https://github.com/PilotsRKwl/fromScratch). [Installation and login](INSTALLATION.md) is the authoritative guide for setup, login, updates and uninstall. The private runtime is included; no developer tools or Java configuration are needed to play.

## Your first session

1. Launch From Scratch Beta and log in through RuneLite's Jagex Accounts panel. Enable the plugin; open the purple coin sidebar.
2. Before starting, bank inventory and equipment yourself. The complete physical bank must remain visible even with Virtual Bank checked in Settings. Read/confirm the attestation and Start confirmation.
3. After the initial save, Virtual Bank activates if its saved setting is ON. Switch native tabs and All. Only positive virtual item types should appear. Disable the single Settings checkbox to see Full Bank.
4. Bank some newly acquired items. Confirm gray physical quantities and bright virtual quantities, locked shading, correct signed counts and responsive normal banking. Mixed physical stacks still permit normal Withdraw-All: respect the virtual count yourself.
5. Examine items normally. Expect the game's description/core price messages, then one From Scratch line with exact signed quantities. No extra menu option.

## A few useful trips

- GE: pending/active partial offers do not appear in history. Complete a buy and sale: one executed row each. Cancel an unfilled offer: no row. Cancel a partial: one partial row for only executed units. Collect/bank normally; no duplicate GP/items/tax or peak increase from merely viewing history.
- Buybacks: inspect catalog categories/search and a confirmation; Cancel must change nothing. With sufficient earned virtual GP, purchase one qualifying legacy item. Charge/entitlement should occur once. Do not spend extra just for testing on a valuable run.
- Pouch/bag: buybacks unlock the equipment, not old contents. Review verified negative offsets, purchase once, then empty/bank the old contents to neutralize them. Never guess unreadable contents. Opening/closing or Rune Pouch upgrades do not require another family payment.
- Close/reopen the bank, switch tabs/views, disable/re-enable the plugin and restart RuneLite. Counts, entitlements, offsets, history, settings, peak and Time Played should persist without freezes.
- Export Progress through the header's Progress files menu. Keep it private. Test Import preview/cancel freely; actually replacing/resetting a run should use disposable progression. Imports replace, not merge, same-character histories.
- Update by running the newer approved setup EXE. Confirm the shortcut and saved challenge still work. Use Windows Settings to uninstall a disposable beta installation: progression/other plugins must remain.

## Reporting a problem

Use [Issues](https://github.com/PilotsRKwl/fromScratch/issues) with the beta version, RuneLite version, Windows version, UI scale, steps, expected/actual behavior and whether restart helps. A cropped screenshot or short redacted error excerpt is useful. Do not attach passwords, tokens, authentication panels, whole logs/config folders or `.fsp` saves.

For a freeze, record the last action and time. An advanced owner can capture `jcmd <RuneLite PID> Thread.print` locally using the Java 11 JDK; it shows stack traces, not an automatic diagnosis. Redact paths/account context and share only relevant threads. Do not keep force-reproducing a freeze against real progression.

Copyable report:

```text
Beta / RuneLite / Windows / UI scale:
What I did:
What I expected:
What happened:
Does restart help? Does Full Bank help?
Relevant redacted screenshot/error excerpt:
```

Mobile/other-PC changes update at the next complete host bank observation. The plugin cannot reconstruct all unseen trades or mobile playtime. Normal bank withdrawals are not proof of consumption; signed deficits are valid. [Full boundaries](FEATURES.md#boundaries).

## Milestone 10.7: Potion Storage and Overdraft Protection

This source candidate is not included in the preserved 10.6 installer. Do not distribute a new beta before acceptance. Export a private backup before testing. Only the owner operates RuneScape.

1. On an existing run, open Potion Storage and inspect **FS:** amounts next to native amounts. Use **Manual Corrections → Potion Storage → Review Unresolved Storage** if a baseline needs review. Confirm known challenge stock; leave legacy stock at zero. Never guess unknown earned history. Check uncertain bank bottles under Bank Items. Your real run must not be reset.
2. On disposable progression, bank inventory/gear and check the purple confirmation once. Open Full Bank for Bank recorded. An unlocked character then opens Potion Storage; inspect exact counts, zero/offscreen types, dose/item units and Review Captured Potions. Start directly there while the bank proof remains valid. Verified locked accounts skip storage. Unknown unlock status needs explicit attestation. Confirmations and captured bank must survive normal interface changes; logout/character/reset must not reuse them.
3. Earn three four-dose potions, bank them, then transfer twelve doses into storage. Check no extra wealth, spending, warnings or debt. Withdraw four three-dose bottles into the bank. Check twelve total earned doses and unchanged estimated pooled value.
4. Change selected withdrawal dose, switch bank tabs/search/views, close/reopen and restart. No new transaction or alert should arise merely from presentation. Last-observed/unresolved storage must be labeled. Reopen storage after login before transfers; unseen changes may require review.
5. Use a mixed ordinary stack with a small virtual allowance. Withdraw beyond it using 1, 5, 10, X and All where practical. Each real debt increase should produce one red chat message and a RuneLite notification when enabled. The warning section must remain after bank closure, with Virtual Bank OFF, and after restarting. Exact-zero withdrawals must be quiet.
6. Repeat with a known virtual potion-dose allowance in storage. Inspect the potion name, observed doses withdrawn, available doses and resulting debt. Increase debt once, return part, then return enough to clear it. No transfer, dose selection or first observation should create a confirmed-overdraft alert.
7. Inspect mixed-stack hover text, gray physical quantities, configured virtual color, debt item-history entry and readable sidebar scrolling. No withdrawal is intercepted or blocked.
8. Export/restart and verify the same run, balances, contents offsets, entitlements, GE history and peak. Keep backups private. Wizard, authentication and accepted buyback behavior remain owner tests; no automated game input is used.

9. Check native storage labels while scrolling, resizing, changing dose size and reopening. Native text, sprites, hearts and both columns must stay unobscured. Check default/custom colors, zero, negative and unverified values. Hover a compact FS marker for its exact amount. Disable the plugin and check complete overlay removal.
10. In Manual Corrections, search for dose-based and item-based entries, including zero physical stock with outstanding virtual units. Check cached status with storage closed. Confirm a signed correction, inspect its before/after/change in Manual Corrections history, restart and verify persistence. An unchanged storage refresh must not replay the correction or create a withdrawal warning. Confirm the dashboard has six tiles and no storage list/button.

### Current correction/display/reset acceptance

- In Manual Corrections, confirm the large sprite/name list, grouped right-hand details and bottom-right Cancel / Save. Resize and search in both categories. Potion Storage must say last verified, with cached status when closed. Absolute values and signed adjustments retain their existing meaning.
- Open native Potion Storage and confirm visible FS labels in BOTH columns, including zero, positive, negative and unverified states. Change doses, scroll, resize, reopen and disable the plugin. Real dose quantities, hearts and interactions must stay intact. This remains an in-game acceptance check.
- If FS is still absent, the existing development run task already enables DEBUG. Report ONLY lines beginning 'From Scratch potion overlay:' (at most six per client launch), plus a cropped screenshot without private account information. Status distinguishes projection/config/visibility/viewport gates and entry/draw counts. Do not share whole logs, credentials or saves.
- Test Reset only on disposable progression, without resetting your valuable run. Confirm immediate Resetting progress feedback, disabled Reset, responsive client and no new challenge until success. Check a new run starts with zero progress, no debts/purchases/GE history and fresh run-scoped time/peak. Exports, historical files and another character's progression must remain intact. Slow disks may take longer than the measured fixture.

### Bank/storage synchronization and final setup acceptance

- Deposit earned potions in the ordinary bank, visit Potion Storage, return to Full/Virtual Bank and switch native tabs. Relog and verify both balances and history. Use a private known-good backup; do not reset the valuable run. A prior rejected-save error requires Retry Loading after updating.
- Check FS on the SAME baseline as native doses/items in both columns, including first/last clipped rows. Scroll, resize, change withdrawal doses and reopen. Check positive, zero, negative, large and unknown values; native text/hearts must remain clear.
- Change Negative Virtual Quantity Color (default #FF4040) and inspect Full Bank, storage, Examine, hover and outstanding quantities. Positive color and physical gray stay unchanged. Repeat accepted overdraft and repayment tests.
- On disposable setup only, confirm Steps 1–3 appear first. After Bank recorded, Step 4 appears only when storage is unlocked or unknown. Ordinary instructions use normal text color. There is no second capture-confirmation card or return-to-bank instruction. The banking card must stay checked through mode changes. An unknown unlock state uses one purple attestation; Start remains blocked without valid matching snapshots.

## Empty Looting Bag confirmation acceptance

1. Keep one supported Looting Bag in the ordinary bank with at least 10,000 virtual GP. Select it in Legacy Buybacks. If native contents cannot be read, leave **I confirm my Looting Bag is empty** unchecked and verify Confirm Buyback is disabled. Canceling spends nothing.
2. Only if the bag really is empty, check the box and confirm. Verify one 10K GP payment, one shared bag entitlement, no legacy contents offsets, and **Player confirmed empty** in Buyback History. Restart and check the same receipt and balance remain.
3. With a populated bag, use native Check or View and return it to the bank. Reopen Legacy Buybacks, inspect the exact contents, and confirm the normal negative offsets. There must be no empty-declaration option while contents are verified. If a previously unknown dialog is still open when populated contents become verified, its empty declaration must be rejected without spending GP.
4. Check open/closed bag forms share the same entitlement and a repeat attempt cannot charge again. Accounts with and without Potion Storage use the same flow. Do not add items merely to make an empty bag readable, and do not reset valuable progression for this test.
