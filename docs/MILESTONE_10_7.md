# Milestone 10.7: Potion Storage and Overdraft Protection

Candidate for one owner acceptance checkpoint. Eventual tag: **v0.10.7-potion-storage-overdraft**, not created. No commit, push, publication, submission or real-progression manipulation. A new local 10.7 test installer is prepared separately; the existing 10.6 installer remains unchanged.

## Final Looting Bag correction and local Friends Beta

The owner has accepted the other M10.7 gameplay. The remaining bag defect was reproduced in an isolated pipeline case: an explicit native empty confirmation with no item grid/container was rejected before the reader checked the confirmation. The adapter searched only children of the items widget, and the bank source discarded inspection evidence on every later bag packet/bank closure. The debugger launcher did not retain a usable isolated-test session; the failing Gradle reproduction and subsequent tests provide the runtime evidence, rather than a claimed live-client trace.

`LootingBagReader` now checks supported mounted bank-side and standalone bag interfaces, the bank-side text sibling and item-widget text. A current zero-item container or a specific native empty message after View verifies empty; absence alone is unknown. Both ordinary and filtered native game messages are supported, while player chat is rejected. Nonempty proof still requires complete matching native/server contents, or a fresh bank-associated packet. `BagObservation.State` exposes VERIFIED_EMPTY / VERIFIED_NONEMPTY / UNKNOWN without a save-format change.

The narrow runtime `LootingBagReader.Session` owns pending View/Check and packet/capture scope, replacing the equivalent bank-source fields. It waits for the following client tick and allows a bounded five-tick native build. Requesting a new View discards earlier packet freshness, including packets from before that click in the same tick. A verified capture keeps its original timestamp for at most two minutes through returning the bag to the bank and matching repeated packets. Expiry, logout/character/session changes, conflicting contents and observed contents-changing actions invalidate evidence. Final purchase still requires one banked family copy and a matching current preview. No ledger, financial, pouch or potion-accounting rule changes. Empty confirmation says **Your Looting Bag is empty. No legacy item adjustments are needed.**

The existing installer pins remain **v0.10.7-potion-storage-overdraft / 0.10.7-beta.1 / 0.10.7.0**, with unchanged official private Java 11 and Inno tools. The tag was absent locally and remotely. Local-test builds now use timestamped folders under the same release directory to preserve earlier candidates without changing their package version. They record base commit and actual uncommitted source hashes; they are not clean committed releases.

Java 11 incremental compilation passed. The final complete regression passed **1,034 tests in 75 classes**, with **zero failures/errors/skips**, in **2m 17s**. This includes **168 directly affected bag/buyback/pouch/storage cases**, including **25 BagPipelineTest cases**. The initial failing empty-grid reproduction, fixture-clock failures, passing focused rerun and final XML/logs are retained under `build/reports/m10-7-looting-bag/`. The fixture clock now models current client ticks independently of the cached bank observation timestamp. No accounting assertions were weakened.

The new local EXE is `build/distributions/v0.10.7-potion-storage-overdraft/local-test-0.10.7-beta.1-20261024-062313-191/FromScratch-Beta-Setup.exe` (**56,806,824 bytes**). Its adjacent `.sha256` file records **86d3f7760d837a6c86431a42f9135e30d538c07c11ef98fd93fd20c9b7fc25ac**. Base commit is **6536aa14061b62d673e66766e4746caed6d106e6**, with explicitly recorded uncommitted source and local-test flags. The build used `Build-NativeInstaller.ps1 -LocalTestCandidate -Offline`; cached RuneLite **1.13.3**, private Temurin **11.0.32.1+1**, Inno **6.7.3**, branding and existing installation identity are unchanged. Earlier candidates are preserved. The EXE is unsigned and remains unpublished.

All **15 isolated native checks passed**, including private-Java/no-PATH launch verification, GUI/icon/shortcut checks, payload/tamper rejection, Downloads/spaced paths, old-version upgrade, reinstall and safe uninstall. Synthetic progression/settings/exports/unrelated files and changed shortcuts were retained; no real data or normal installation was used. Inno pads the version-text field, so the test now compares all four numeric PE components exactly with the pin. The initial text-comparison failure is retained in the logs. All **398 image files** and **132 actual source hashes** verify; the corrected production classes are packaged without tests/previews/private data. The candidate folder includes file inventory, build/provenance metadata and validation evidence. Installer wizard visuals, actual client launch/login and in-game acceptance remain unverified by the agent.

Manual acceptance: on the affected character, choose native View on the empty bag, return it to the bank and confirm the 10K purchase with zero offsets. Check a disposable nonempty example for exact item/coin offsets and return protection. Repeat on characters with and without Potion Storage. Check open/closed family entitlement, repeated confirmation, restart persistence and a stale/changed observation requiring another View. Do not reset valuable progression. The installer wizard, normal client launch and authenticated login remain owner-only checks.

## Previous physical-overlay release preparation

The owner has accepted the other M10.7 gameplay systems. This final pass changes only the gray physical bank counter's drawing geometry and release tooling. No accounting, Potion Storage, Bank Tags, sprite, menu, persistence or reset behavior changes.

The physical painter used the tile's left edge and a fixed nine-pixel baseline. An isolated Java 11 probe against the installed RuneLite 1.13.3 small font measured an 11-pixel ascent and glyph ink starting one pixel above that tile. The counter now uses a two-pixel layout inset, an ascent-derived baseline and an inset clip intersecting the existing viewport. Space is reserved for the one-pixel shadow. Only physical labels wider than that interior are fitted horizontally, preserving native digit/K/M formatting. Gray remains **#A0A0A0**; locked shading and all bottom-right virtual geometry/colors remain unchanged.

The pinned installer configuration is now **v0.10.7-potion-storage-overdraft / 0.10.7-beta.1 / 0.10.7.0**. Official Temurin **11.0.32.1+1** and Inno Setup **6.7.3**, their URLs/checksums and licensing are unchanged. The existing build script already passes these pins to the launcher and installer. Inno now requires those explicit version arguments instead of silently defaulting to 10.6. Installer tests compare release/package metadata, GUI file versions and Installed apps registration against the current pin; all payload, security, upgrade and uninstall checks remain. The deliberate 0.10.5 upgrade fixture remains.

Use `powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Test-NativeInstaller.ps1 -ConfigurationOnly` to check syntax and release configuration without building, installing or launching an application. The current pins pass; isolated fixtures reject obsolete/mismatched release, package and Windows versions, a malformed checksum and an unofficial runtime source. Local and public remote tag lookups found no existing target tag. No tag was created.

No installer was built or installed in this final pass. The earlier local candidate described below predates this visual fix and has Windows version 0.10.7.1. It is retained unchanged as historical test evidence and must not be published as the final candidate. After owner acceptance, a clean committed source build must produce and validate the newly pinned release.

Java 11 incremental production/test compilation passed. The two affected overlay classes cover **59 cases**; one new fixture initially used the unsigned formatter for a negative balance, was corrected to supply its signed label, and passed its focused rerun. The final complete regression then passed **1,021 tests in 75 classes**, with **zero failures, errors or skipped tests**, in **2m 42s**. This supersedes the earlier partial full-suite result below. Six new painter cases cover complete glyph/shadow bounds, native compact stack sizes, moved slots, 100%/125%/150%/200% transforms, both viewport edges, unchanged virtual text/tint and restored graphics state. Installer configuration checks and all six isolated configuration fixtures passed; runtime/compiler pins match HEAD. Evidence and the initial fixture failure are retained under `build/reports/m10-7-final-release/`. No real game session, installer execution or progression data was used for these checks.

Owner visual acceptance: in Full Bank and Virtual Bank, inspect small, five-digit, K/M and maximum-size stacks, locked and available items, both viewport edges while scrolling, native tabs, and 100%/125%/150% scaling. Gray digits and shadows must stay inside each tile without changing sprites, shading or the bottom-right signed quantity. No progression reset is required.

## Previous setup and Friends Beta local candidate

This section records the previous setup pass and its local installer evidence. Its setup behavior remains accepted; installer version/provenance details are historical and superseded by the pinned release preparation above. The setup pass added no accounting rule, save migration, withdrawal behavior or unrelated feature.

### Setup behavior and safety

1. Bank everything from your inventory.
2. Unequip and bank all your gear. Confirm the existing purple banking card once.
3. Open your bank. A complete authoritative normal-bank observation becomes **Bank recorded**.
4. Only if Potion Storage is unlocked, open it. A complete current-attempt capture becomes **Potion Storage recorded**, with compact totals and Review Captured Potions. There is no second capture-confirmation checkbox and no return-to-bank instruction.

`BankPanel.render` previously cleared the banking checkbox whenever its presented observation was null, including ordinary storage-mode transitions. The component was not being recreated. Runtime-only `bank/BankSetup` now stores the attestation, pending full-bank observation, supported unlock evidence and optional unknown-state answer. The client-thread controller owns that immutable state; Swing projects it and sends explicit changes guarded by the attempt token and current character. Widget rebuilding, sidebar rendering and storage navigation cannot silently uncheck it. Logout/character change, reset or a new setup attempt establish fresh state; explicit unchecking remains supported.

The bank must first be recorded in Full Bank with From Scratch filtering inactive. Opening storage retains that proof independently of its widgets. Temporarily unavailable data retains the capture but blocks Start until the authoritative bank can be checked again. A changed complete container while in storage invalidates the bank capture and requests a new Full Bank observation. Full Bank can refresh its capture from a new complete observation. Start revalidates session/quantities against the full API container and the correct current setup/storage proof. Both exact baselines commit atomically through the existing repository, without a speculative run or new overdraft event. Starting directly from storage is permitted when those proofs remain valid. Persistence failure keeps recoverable setup evidence and exposes the error.

Supported `MM_POTION_STORAGE_UNLOCKED` evidence is retained for the attempt. Verified locked characters skip Step 4 entirely. Verified unlocked characters need a complete native capture. Unknown state shows one purple not-unlocked attestation; that answer is retained for the attempt, never inferred from a missing button or widget. Normal instructions all use the existing neutral instruction font/color. Original snapshot completeness, offscreen/zero types and exact doses/items are unchanged. Active runs, return protection, overlays, reset performance and save formats are unchanged.

### Validation

Java 11 incremental compilation and focused setup tests passed. One full release-candidate regression ran **1,015 cases: 1,009 passed and six obsolete expectations failed**. They expected green negative text (two cases), removal of the last acknowledged projection on a failed buyback, the old correction-purpose sentence, an extra locked-storage checkbox, or starting immediately after reset without recording the new setup bank. The assertions were updated to check the accepted/requested behavior, retaining their safety, color, clipping, failure and acknowledgment checks. **All six passed their targeted reruns.** The full suite was not run again. Raw full-run and rerun XML remains under `build/reports/m10-7-setup-release/` so the initial failures are not concealed.

The actual native installer passed **14 checks**, including private runtime, no system Java dependency, spaced paths/unrelated working directory, icon/GUI binaries, damaged/missing/unexpected library rejection, old-version fixture upgrade, reinstall, shortcut targets/icons, HKCU Installed apps registration and safe uninstall. Synthetic progression, settings, backups, unrelated files and changed shortcuts were preserved. No actual character data or normal installation was used. Inno logs confirm no administrative/machine-wide install. All **398 image files** and **132 actual build-source input hashes** were verified. The plugin JAR contains the setup/storage production classes and excludes test/preview/bootstrap/private-save content.

### Local installer and provenance

- Folder: `build/distributions/v0.10.7-potion-storage-overdraft/local-test-0.10.7-beta.1/`
- EXE: `FromScratch-Beta-Setup.exe` (**56,802,895 bytes**)
- SHA-256: `ea9db1d062e65b964d2614dc8b7c23f5ae9d58c816bd2a8a663d2935e0dcdf35`
- Checksum: `FromScratch-Beta-Setup.exe.sha256`
- Review evidence: `installer-files.json`, `installer-build.json`, `source-provenance.json`, `test-summary.json`, `native-validation.log`.
- Package: **0.10.7-beta.1**, Windows file version **0.10.7.1**, intended future tag **v0.10.7-potion-storage-overdraft**.
- Base commit: **6536aa14061b62d673e66766e4746caed6d106e6**. **Uncommitted changes are included.** This is not a binary matching committed GitHub master.
- Private verified Temurin runtime: **11.0.32.1+1**. Pinned runtime/license/compiler infrastructure and existing application identity are unchanged. The EXE remains **unsigned**.

Packaging adds only explicit version overrides and an opt-in `LocalTestCandidate` path through the existing scripts. The ordinary committed-source guard remains. SOURCE records actual source file hashes and explains that gameplayTree is the base committed tree. The local candidate is isolated below its versioned output directory, preserving earlier releases and the future clean release path. No conflicting local or remote M10.7 tag was found. No tag was created.

Do not overwrite this earlier EXE. The current installer tests intentionally reject its superseded Windows file version. Use the pinned clean-source build below only after owner approval.

### Owner acceptance

Double-click the local EXE from Downloads and inspect the wizard, desktop/Start Menu shortcuts, normal GUI launch and private-Java startup. Complete official RuneLite Jagex Accounts login yourself. A supported flow is documented by [RuneLite](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts); this packaged client's authenticated login has not been tested by the agent. The GUI wizard, actual shortcut double-click and live setup remain manual acceptance, rather than claims inferred from silent tests. Do not disable Windows security tools for the unsigned candidate.

Use disposable progression for setup/reset tests. Confirm the banking card stays checked through bank/storage/rebuilds, Step 4 appears only when applicable, review shows the captured amounts and Start works from storage. Change the bank after capture to check stale-start rejection. Check logout/character isolation, an unknown-state attestation and a failed save. Repeat the owner-confirmed banking, storage, overdraft, colors and correction golden paths on a private backup. Uninstall through Windows Settings and verify retained progression/exports/settings. No valuable run should be reset for acceptance.

### Commands after approval only

These are recommendations, not operations performed during this session. Review the complete pending M10.7 diff first:

```powershell
git diff --check
git add src/main/java src/test/java docs README.md beta/windows-native/toolchain.json beta/windows-native/FromScratch.iss scripts/Build-Beta.ps1 scripts/Build-NativeInstaller.ps1 scripts/Test-NativeInstaller.ps1 gradle/beta.gradle
git diff --cached --stat
git commit -m "Complete Milestone 10.7 potion storage and overdraft protection"
git push origin master
git rev-parse HEAD
git ls-remote origin refs/heads/master
git status --porcelain
```

Verify identical local/remote commits, a clean tree and Windows/Ubuntu CI before the clean release build. Then run the existing build without LocalTestCandidate, which writes to the parent release folder rather than overwriting the local test build:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Build-NativeInstaller.ps1
powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Test-NativeInstaller.ps1
git ls-remote --tags origin refs/tags/v0.10.7-potion-storage-overdraft
```

If the tag is absent and the clean committed candidate passes approval, the owner may create/push the annotated tag, draft the GitHub prerelease, upload only the clean EXE/checksum and test a fresh download. Never upload this uncommitted local candidate. If the beta version or tag is already taken, choose a new unique beta version rather than overwriting anything. Plugin Hub submission remains Milestone 11.

```powershell
git tag -a v0.10.7-potion-storage-overdraft -m "Milestone 10.7: Potion Storage and Overdraft Protection"
git push origin refs/tags/v0.10.7-potion-storage-overdraft
```

## Latest correction: synchronization, alignment, colors and setup

This section supersedes earlier acceptance notes about amber negative text and setup layout. Live acceptance is still required; no authenticated gameplay was operated by the agent.

### Confirmed checkpoint failure

An isolated real-repository regression reproduced the reported exception. A delayed storage deposit can resolve a prior bank potion debt while moving only earned doses into storage. `BankEngine.observe` calculated the correct balances and gain exclusions, but its zero-bank-delta branch attached the original `PotionUpdate` with empty exclusions. `BankRepository.validate` independently recomputed the exclusion and correctly refused the save. IntelliJ debugger evidence at `BankRepository.validate:150` showed **state equal=true, calculated exclusions={2434=3}, recorded exclusions={}**. The path was `save -> locked save operation -> validate`, on the isolated test worker. Three legacy plus three earned four-dose bottles were withdrawn; storage subsequently reported 24 doses. The resulting bank balance was zero and stored virtual balance 12, but missing audit evidence rejected that otherwise coherent transition.

The storage-only audit now carries the calculated exclusions exactly as the ordinary-bank branch does. Validation remains strict. A forged audit omitting them still fails, and the previous durable checkpoint remains byte-for-byte authoritative. No repair, balance grant, new transaction model, schema change or migration is introduced. Rejected operations do not publish speculative balances. A subsequent save error now retains the last acknowledged bank rendering/filter projection, while showing the error and marking current storage availability unverified. Failed initial loads still have no active run or projection. Retry Loading remains explicit; there is no automatic retry loop.

Ordinary bank and Potion Storage share native BANKMAIN, but bank quantities continue to come only from the complete `InventoryID.BANK` container, including while storage is selected. No native storage row, tab, search or filtered widget is a quantity authority. Missing containers suspend accounting; they are never synthesized as empty banks. Storage reads have their own build/varp/completeness state and no longer depend on bank-observer readiness merely to read storage. An incomplete native build retains the last verified copy and returns unavailable, retrying observation when the native rows catch up. Both authoritative observations must be usable before a combined transaction commits. Existing checked dose mappings and once-consumed same-session transfer budgets are preserved. Session-crossing or otherwise ambiguous history still requires explicit review; it is never automatically reconstructed as earnings.

The exact owner sequence without withdrawals passed before the patch in isolation; the missing-exclusion transfer is the confirmed reproduction of the same warning. This does not establish every event ordering in the owner's live session. No real save/log files were read or changed.

### Rendering and setup

The label painter previously centered all text in the quantity widget, ignoring its native vertical alignment. A tall top-aligned text widget therefore moved FS toward the next potion name. FS now uses the exact quantity widget's canvas bounds, public native font baseline/width and text alignment/line-height fields. Top-aligned native labels use widget top plus font baseline, without a second centering offset. Other single-line alignment modes use the available font metrics. The existing after-BANKMAIN draw hook, own-entry/column/heart limits, viewport clipping, compact notation and exact hover remain. No widgets, quantities, menus, actions or layout are modified. Actual alignment at client scales remains owner acceptance.

`negativeVirtualQuantityColor` is a new ordinary RuneLite color setting immediately after Virtual quantity color, default **#FF4040**. Nonnegative values retain **#1DFF00** or the saved custom positive color. Bank glyphs, storage FS labels, negative Examine numbers, signed bank tooltip numbers and outstanding quantities use the negative color. Names/normal words and gray physical counters retain their styling. Unknown FS uses neutral **#B0B0B0**. Config changes update presentation without ownership events or a restart.

Setup is numbered 1: bank inventory, 2: bank gear, 3: verify Potion Storage, 4: open the full bank and start. Pending storage action is red; verified totals and Review Captured Potions remain, followed by Return to your bank to start. The redundant bottom paragraph is removed. Supported `MM_POTION_STORAGE_UNLOCKED` evidence is considered known only with a logged-in character, available varps and mounted unlocked bank. Known unlocked shows only the captured confirmation; known locked shows only the not-unlocked confirmation. Unknown state offers one explicit verification choice and one relevant confirmation, never contradictory checkboxes. Both use the existing purple `BankAttestation` component. No old capture, reset, unavailable bank or unchecked confirmation can authorize Start. Snapshots and the exact reviewed capture still commit atomically before filtering.

### Focused validation and owner review

Java 11 incremental production/test compilation passed. The final changed-case set passed **65 tests**: 51 Potion Storage cases, nine overlay cases and five selected bank-tab/color/setup cases. A separate persistence/reset/backup pass covered **80 cases**; one old assertion expecting all rendering to disappear after a failed update was updated to require the last acknowledged projection, and that case passed on its isolated rerun. No other failures remain in those sets. Earlier directly affected Bank Tags, Examine and painter cases also passed. The full regression suite, installer and benchmark were not rerun. Prior reset optimization is unchanged. Evidence is under `build/reports/m10-7-sync-ui-focused`, `m10-7-sync-persistence` and `m10-7-sync-final`; earlier failed assertions are preserved with their successful follow-up results.

1. Use a known-good private backup, without resetting the valuable run. Deposit earned bank potions, visit storage, return to bank, switch tabs and relog. Check both balances, filtering, history and no unexpected warning. Retry Loading if the previous client is still displaying its rejected-save error. Report any remaining error without sending saves or credentials.
2. Inspect FS on native physical-quantity baselines in both columns, at the top/bottom of the viewport, while scrolling, changing doses, resizing and reopening. Check short/large, positive, zero, negative and unknown values, with hearts and native text unobstructed.
3. Change Negative Virtual Quantity Color. Check Full Bank negative glyphs, storage, Examine, hover and outstanding quantities; positive text and physical gray must remain unchanged. Existing notifications and repayment must still work with Virtual Bank both on and off.
4. Only on disposable progression, inspect steps 1–4, purple cards, current complete capture/review, unlocked/locked flow and pending action. Start must remain blocked until the relevant consent and full-bank snapshot are ready. Reset must invalidate old capture/consent.

This pass changes configuration/plugin wiring, bank controller/engine/view/source/setup/sidebar/attestation/outstanding presentation, potion source and overlay state/painters, plus Examine formatting. Bank Tags implementation, reset archive implementation, buybacks, GE and wealth engines are unchanged. Nine directly affected test files and four focused guides are updated. The inventory below includes earlier uncommitted M10.7 work; generated reports are local ignored artifacts.

## Observation and accounting

Resolved RuneLite **1.13.3** supplies `POTIONSTORE_POTIONS`, `POTIONSTORE_UNFINISHED_POTIONS`, `POTIONSTORE_DOSES`, native build/dose-change events and widget varp triggers. `potion/RuneLitePotionSource` uses the same supported read-only dose query as the installed core Bank Tags source. There are no menu actions, deposits/withdrawals, packets, reflection, widget creation or inventory/equipment quantity tracking.

Mappings come from the current cache enums, not a hardcoded four-dose list. Finished potions use each actual one/two/three/four-dose mapping, including supported mixtures/special types listed by that cache. Unfinished potions count individual items. Empty vials and items absent from those enums are excluded. Invalid, duplicate or missing mappings fail closed. Actual live cache coverage and native interface timing still require owner testing.

Opening native Potion Storage establishes a complete observation; selected withdrawal size does not change total units. Quantity-trigger changes invalidate the cached read. Before opening, only definitions are known, not quantities. Closed/logged-out data is labeled last observed, never current. A verified not-unlocked flag is distinct from unreadable/unopened contents. Storage errors wait for a native refresh rather than retrying every tick. Rendering does no storage/price/disk work.

`PotionState` is part of `BankRun`; `PotionUpdate` is evidence in the existing `BankAudit`. `PotionEngine` and ordinary bank movements save together under the existing revision/CAS, forced journal/checkpoint and atomic current replacement. There is no separately published potion ledger. Initial physical bank quantities remain immutable.

New challenges require a full bank plus verified unlocked-storage stock before Start; a character without the unlock needs no impossible storage step. Both legacy baselines commit before Virtual Bank activation. Existing runs retain bank balances, purchases/history and their original snapshot. Their first verified storage is unresolved, with no automatic credit or loss. **Manual Corrections → Potion Storage → Review Unresolved Storage** records known virtual units explicitly, bounded by verified physical stock. The separate first storage observation is retained. Unknown gains can be held, with physical movements retained in Bank History, rather than minting legacy stock. Uncertain bank counts use the existing corrections/history tools.

Same-tick opposite bank/storage changes transfer the available virtual portion; legacy doses remain legacy. Different event times are copied at the GameTick boundary. Once-consumed same-session withdrawal budgets cover delayed returns and different bottle sizes. Budgets express conservative attribution, not proof of inventory provenance; two possible source containers require review. A different login/session cannot reuse a stale budget. An observation session is stored separately from the bank-budget session, so a bank update cannot make old storage current.

Bank bottles keep real item IDs/quantities. Virtual doses can be redistributed between those variants. A partial owned bottle has a bounded remainder; the overlay/filter never fabricates a fractional bottle slot. The native dose display, corrections and pooled valuation retain its exact units. A bank correction clears affected remainder/transfer budgets, without altering stored stock. Consumption is not independently inferred: only observed net quantities and bounded returns are known.

## Warnings and UI

`bank/Overdrafts` serves ordinary bank and Potion Storage withdrawals. It compares acknowledged before/after signed availability and negative physical deltas, independent of 1/5/10/X/All and the Virtual Bank setting. New debt and increased debt warn; exact zero does not. No menu option is intercepted or altered.

After durable acknowledgment, the plugin queues one red local **From Scratch: LEGACY ITEMS OVERDRAWN** chat message per affected item/type and calls RuneLite `Notifier`. It reports actual observed units, available virtual units and outstanding debt. Existing transaction deduplication and continuous-session guards prevent repeats and initialization/reopen/import/offline alerts. Manual corrections, reviews, initial stock, contents offsets, unresolved observations and UI rebuilds do not produce new alerts. Multiple native changes coalesced before an authoritative observation are net changes; unseen gross actions cannot be reconstructed.

The amber warning section above the six dashboard tiles shows saved debt, including absent bank items and negative potion doses. Potion variants are grouped by type, with each debt counted once. Entries use cached item sprites and open existing item history. Returning items/doses reduces or clears the saved debt. No button manipulates gameplay. A mixed-stack hover tooltip explains the full physical quantity and Withdraw All behavior. Gray physical counts, configured virtual colors, native tabs/filtering and buyback behavior are preserved.

The active-run Potion Storage sidebar section is removed. Compact FS labels decorate native storage rows without modifying widgets or physical text. Manual Corrections exposes stored doses/items, cached status and explicit audited review/correction. Setup has a separately confirmed, reviewable native snapshot. The later negative quantity color is the only new presentation setting; there are no duplicate bank toggles or statistics pages. A narrow Virtual Bank guard pauses item filtering for native Potion Storage (core's special tab 15), preserves the preference, and resumes cached native-tab filtering on return. It clears state before deferred tag cleanup, creates no repeated activation loop and makes no synthetic menu action. Native special-storage interactions still require live acceptance.

## Persistence and values

Checkpoint **format 10** adds dose mappings, verified physical/virtual storage, the separate observation session, bounded transfer budgets and remainder/audit evidence. Format 9 loads without being rewritten; the next acknowledged transaction publishes format 10. Earlier migrations retain their exact archives. Existing format-9 `.fsp` imports remain accepted. Export/import carries the same complete state; imports invalidate live alert continuity. Reset clears the new state through the existing archive/acknowledgment path. No reset or old-run rebase is performed automatically.

Current wealth values bank plus stored signed doses once using a common cached representative bottle's per-dose guide estimate. Unknown prices remain unknown; fractional GP is conservatively floored. Unresolved/stale storage cannot raise a reliable pooled peak. Historical bank gain quotes remain auditable; transfers/conversions/returns are excluded from new bank gains. Storage withdrawals/debts are not spending. Virtual GP, GE finalized trades/tax and committed buyback payments remain unchanged. No new financial tile or separate economy is introduced.

## Compliance and distribution

The [official review guidance](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) and [restricted-feature list](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) were checked. This addition reads public enums/scripts/events and uses supported chat, notification and tooltip APIs. No automation, withdrawal blocking, outgoing player-chat modification, packet access, network endpoint or dependency was added. This is an engineering assessment, not Plugin Hub approval. Native render/filter code was not redesigned.

`src/beta`, Windows native launcher/installer, toolchain pins and packaging scripts are unchanged. No installer or beta ZIP is regenerated. Existing 10.6 binaries still contain their old plugin; the new candidate is source-only until owner acceptance and authorized packaging. Hosted CI and live gameplay are separate, pending checks.

## Earlier implementation validation (before this UI overhaul)

Focused new tests and affected regression classes use isolated temporary Filepath roots, never real profiles. **33 new tests** cover the storage/overdraft model, old-format durable transition and special-view lifecycle. The earlier selected feature regression run passed **150 tests**. The single complete checkpoint ran **966 tests**: 964 passed and two stale UI expectations failed (a former no-scroll-pane assertion and old setup wording). Those assertions now verify bounded secondary panels and the new setup instructions; both passed in the **72-test** affected UI/storage/view rerun. The final **25-test VirtualBankModeTest** run, including the additional close/reopen case, passed. The final storage run passed 28 cases; one additional combined-debt overflow rejection case passed separately. Migration fixture checks passed after their old-schema generators were updated to omit the new field. No full-suite rerun, clean build, disabled test or weakened production validation was used.

Final incremental **compileJava / compileTestJava passed with Java 11**. Documentation validation passed for 56 Markdown files, 277 local references and 10 PNGs; `git diff --check` passed. Actual game startup, storage script/widget timing, native transfer ordering, OS notifications, font/tooltip appearance and authentication remain owner acceptance checks. No new hosted CI result is claimed.

The earlier installer validation recorded `build/distributions/v0.10.6-friends-installer/FromScratch-Beta-Setup.exe` with SHA-256 `f7a590b75578f6205717b58dd09902cf3b62a90a6edba09bb603365b78d9d95d`. It was not rebuilt, installed or distributed during this UI pass. That ignored installer file is not present in this checkout, so its earlier hash was not reverified here.

1. Privately export your existing run. Launch the development client only when ready; follow the [official Jagex Accounts guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). The owner alone operates the game.
2. Open Potion Storage. Check every listed type and physical units, including empty stock, mixtures and unfinished potions if available. Review known challenge stock once on an existing run; keep old legacy stock at zero. Check any held bank counts explicitly. Do not reset the existing run.
3. Bank three earned four-dose potions, transfer twelve doses into storage, then withdraw four three-dose bottles. Try both directly paired and separate native actions. No extra value, debt or transfer alert should appear. Change selected dose size without moving items; expect no transaction.
4. Overdraw a mixed ordinary stack with All, then 1/5/10/X as practical. Verify one red chat warning per real increase, notification where enabled, exact saved debt, item-history entry, persistence after closing/restarting, and repayment. Repeat with Full Bank and Virtual Bank.
5. Repeat in storage using a known dose allowance. Test partial overdraft, increased debt, partial/full repayment, dose conversion and debt visibility without a native item slot. Verify no transfer/initial-observation/rebuild alert.
6. Close/reopen/restart and check last-observed labeling. Open storage after login before transfers. Changed unseen stock or competing source evidence must request review, not award stock, clear debt or produce a confirmed-overdraft alert.
7. Check current wealth/peak, GP, buybacks, Rune/Divine Pouch banked observation, Looting Bag, finalized GE history, gray/green quantities and native tabs. Keep exports private. Report one acceptance/correction list. No new installer distribution or Milestone 11 work follows automatically.

Relevant implementation: [potion package](../src/main/java/com/fromscratch/potion), [BankEngine](../src/main/java/com/fromscratch/bank/BankEngine.java), [BankController](../src/main/java/com/fromscratch/bank/BankController.java), [Overdrafts](../src/main/java/com/fromscratch/bank/Overdrafts.java), [checkpoint codec](../src/main/java/com/fromscratch/persistence/BankRunCodec.java), [feature reference](FEATURES.md), [accounting](technical/ACCOUNTING.md) and [beta checklist](BETA_TESTING.md).

## Previous UI overhaul candidate

The owner accepted the earlier overdraft behavior. This pass preserves its detector, chat/notification delivery and persistent Outstanding Legacy Items section. No Windows installer is rebuilt.

### Setup and completeness

`PotionSetupPanel` preserves the inventory/equipment attestation and adds separate recorded-storage consent. `Review Captured Potions` lists all enum types, exact units including zero stock, type count and local observation time. The confirmation is enabled only for a valid runtime `PotionCapture`, bound to character and setup attempt. Changing or invalidating that capture clears consent. A mutually exclusive, explicit not-unlocked choice records an attestation; a known unlock cannot use that shortcut.

The source must see `POTIONSTORE_BUILD` before dose changes can establish freshness. Every supported enum is queried for its complete total, independent of viewport/search. Setup proof additionally requires the real visible native storage tab, nonempty quantity triggers, complete five-child row groups, a recognized potion row, exact agreement of row labels with queried totals, correct dose/item labels and a valid timestamp. Sparse zero quantities are known zero because the full type mapping was queried. Missing/dirty/wrong-character/previous-session data cannot authorize Start. A presentation build that lags a valid total query only invalidates setup/label proof; it does not change the previously accepted accounting path. A new native refresh is required to retry incomplete proof, without continuous polling.

The player returns to the ordinary full bank after capture. Start validates that bank again and commits the exact reviewed storage baseline in the same atomic `BankRun` initialization. START evidence records capture confirmation/time/type coverage or the not-unlocked attestation. Failed commits publish neither baseline. Existing runs are not reset or rebased; untracked/ambiguous stock still needs an explicit reviewed baseline.

### Native display and corrections

`PotionStorageOverlay` is a read-only Overlay drawn after the complete native bank interface. It does not add/change game widgets. Public native font metrics place compact FS text after physical text, within the row/heart/parent clipping bounds. The configured color applies to verified zero/positive balances; negative/unverified text uses the existing outstanding-heading amber `#FFB368`. Selected bottle size does not change stored units. Quantity-based single-unit and unfinished entries say items; normal potions use doses. Vials are excluded. Narrow rows show FS with the exact amount on hover; decoration is omitted if even that marker has no safe space. Reopening cannot duplicate labels, and disabling unregisters the overlay.

The active sidebar storage list, timestamp and review button are removed. Six statistics and all existing navigation remain; outstanding potion debt stays near the top.

Existing Manual Bank Corrections opens a Bank Items / Potion Storage selector with search, sprites, exact physical/virtual units, outstanding state and readable verification time. Closed storage is labeled cached; unknown physical stock is not shown as zero. Individual signed edits require a reviewed observation and confirmation, reject over-physical values/overflow, and use existing `STORAGE_REVIEW` audit evidence. Character/run/revision guards reject stale edits. Unresolved stock has an explicit review in this same dialog. History derives prior/new amounts and exact adjustment from predecessor checkpoints; item history retains the reason and absolute reviewed amounts. No new ledger or schema is introduced. Corrections preserve original snapshots, do not make cached data newly current, do not alert as withdrawals, do not replay on unchanged observations, and preserve unrelated transfer budgets. Changed types invalidate their own budgets. Pooled wealth sees the saved units once, without new gain or spending evidence.

### Current validation and manual review

**262 distinct targeted cases passed across 15 affected test classes over the incremental runs**, including 41 PotionStorageTest cases, four native-overlay painter/state cases and the affected setup, bank, buyback, Rune Pouch, Looting Bag, GE, wealth and background-IO fixtures. Sixteen cases were added for this UI pass. One initial failure exposed a setup hint masking the attestation error; the message precedence was corrected and that case passed. Setup image inspection also found low contrast/overlap; the corrected narrow-sidebar fixture was rendered and inspected. Final Java 11 `compileJava` and `compileTestJava` passed. Native-view/unlock checks are logged-in guarded so setup publication cannot query varbits during logged-out startup. The Gradle HTML shows the last filtered run; the counts above span the recorded incremental runs. The full suite was not rerun. The earlier full-checkpoint results above are historical, not a full run of this exact UI revision.

Owner acceptance:

1. Export a private backup. Keep the valuable existing run intact. Use disposable progression only for new-run setup tests.
2. Before Start, confirm inventory/equipment consent is still required. Check that storage consent cannot be selected before capture, and known unlocked storage cannot claim not unlocked. Open storage, review exact zero/nonzero doses and item entries, then confirm the capture. Return to the normal full bank and start. Confirm the saved baseline precedes filtering. Test the explicit not-unlocked path on a suitable character; never misstate it on the real run.
3. In the existing active run, inspect FS labels at zero, positive, negative and unverified states. Try custom color, both columns, scrolling, window sizes, hearts, dose selection, closing/reopening and plugin disable. No native label, sprite or click area may overlap or change. Hover a narrow FS marker for its exact amount.
4. Verify there is no active sidebar storage list/timestamp/review button. Six statistics, buybacks/history/corrections and outstanding potion warnings must remain.
5. Open Manual Bank Corrections → Potion Storage. Search dose and item entries, including zero-stock debt. Inspect current/cached status. Confirm a small signed edit, inspect Manual Corrections/item history, restart and check persistence. A refresh must not replay it or warn that a withdrawal occurred. Restore the intended reviewed balance explicitly.
6. Repeat the already accepted ordinary/storage overdraft and repayment checks, transfers across bottle sizes, native tab switching and bank reopen. Verify wealth/peak and buybacks remain consistent. Only the owner operates the game; native geometry and gameplay are not certified by fixtures.

Current source remains uncommitted on master. No commit, push, tag, real progression operation, distribution, installer rebuild or Plugin Hub submission occurred. The eventual tag remains `v0.10.7-potion-storage-overdraft`.

## Previous acceptance correction: editor, native labels and reset

All three requested corrections are implemented for owner review. Ordinary/storage accounting, initial capture verification, overdraft delivery, buybacks, GE economics and formats are unchanged. No installer rebuild or real progression operation occurred.

### Manual Corrections

The old GridLayout gave a wrapped explanation the same height as every toolbar control, and another equal-row grid consumed the lower dialog. BorderLayout left only a small center list. The replacement has a short header, one selector/search toolbar, a weighted two-column body (roughly 60/40) and stable Cancel/Save footer. Sprite/name selection fills the available body; selected name/sprite, physical/virtual amounts, readable observation time and correction controls stay compact at the top right. A reusable renderer explicitly lays out its sprite/name; the list uses compact, font-aware cell height. Below 700 logical pixels the body stacks; screen fitting and the existing outer-scroll fallback protect small displays. Default dialog size is 940×660, with a screen-bounded minimum.

At 940×660 the isolated component test measured a 572-pixel-wide list with useful heights **569 / 557 / 545 pixels** at 12 / 15 / 18-point fonts, respectively, showing **14 complete rows** in each case. Search, category selection, signed adjustment arithmetic, caps, audit and confirmation remain unchanged. Storage details say **Potion Storage last verified** and disclose cached stock. The internal `build/reports/manual-corrections-fixture.png` is a production-component rendering with synthetic bank entries and fallback branding sprites, not a live account or gameplay screenshot.

### Native Potion Storage

The previous code incorrectly treated child +0 as a full-row rectangle. Installed core Bank Tags identifies +0 as the withdrawal control, while core BankPlugin reads item +1 and physical text +3 in five-child groups. The old containment check rejects every label beyond a small/empty control; this rejection is reproduced in the test fixture. There were also silent exits for missing native font metrics and reliance on a specific LAYER draw hook. Without authenticated native widget inspection, which exact gate fired in the owner's session cannot be proved here.

The repaired renderer draws once after the complete BANKMAIN interface in MANUAL mode. It derives row space from the actual sprite, name, physical label and heart bounds, clamps to observed column boundaries and clips through native layer viewports. It does not use the withdrawal control as a row. Native font width is preferred; a conservative supported RuneLite-font fallback avoids silently dropping labels. Committed mappings match every supported bottle variant to the logical storage type. Exact/color/compact labels are cached at projection changes. Zero/positive use the configured color; negative/unverified use warning amber. Large values compact only when necessary, with exact units on hover. No widgets, quantities, slots, menus or client actions are modified.

Tests now invoke **Overlay.render**, not only a text painter: interface-hook registration, visibility/identity/run gates, both columns, missing-font fallback, normalized IDs, zero/negative/unknown/custom color, scrolling and rebuild replacement are exercised. A capped six-change DEBUG diagnostic distinguishes inactive projection, disabled quantity display, hidden/changed identity, empty viewport and entry/draw counts. It logs no item callbacks, quantities, accounts or paths. This remains for the next live check and is not continuous polling. Actual visible native labels are still **pending owner acceptance**.

### Reset measurements and safety

The actual existing path was traced: typed confirmation → client-thread immutable reset preparation → existing storage worker → CAS/head validation → archive → forced reset journal/checkpoint → atomic current publication → acknowledged overlay/sidebar change. Bank/Potion/GE/buyback/financial cleanup is empty-state construction, not separate ledger scans. Time and peak remain run-scoped; old files/backups are retained and a subsequent run uses a new identity. The expensive operation was archive creation, not Potion Storage cleanup.

Isolated benchmark: **240 real transactions, 1,200 physical item types, 484 JSON files, 10,141,525 bytes**.

| Stage | Before | After |
|---|---:|---:|
| Reset preparation | 0.652 ms | 0.571 ms |
| Durable repository reset | 4,942.957 ms | 1,106.459 ms |
| Total | **4,943.609 ms** | **1,107.030 ms** |

A finer baseline run measured archive copy 645.8 ms, force 647.5 ms, source/copy hashing 3,275.3 ms and manifest/reverification 109.3 ms. Archive work consumed about 98% of reset time. The final implementation streams source bytes/hash once, forces each destination, rewinds that same handle for verified readback, then writes/forces/readback-verifies the checksum manifest. It removes redundant reopen/rehash work and per-byte Formatter allocation. It **does not skip a force or destination checksum check**. Existing archive layout and SHA-256 values remain compatible. Temporary production timing instrumentation was removed; the repeatable test benchmark remains. Single-machine timings are diagnostic, not CI limits or a guarantee for larger/slower stores.

Reset now immediately displays **Resetting progress...**, disables duplicate requests via the existing busy guard, and acknowledges completion only after durable success. Cache/projection cleanup and the sidebar transition are published once. A failed save retains the old run, reports the error and prevents a new run. Disk work stays on the existing worker; tests hold it pending while the controller remains usable. Actual client responsiveness and Bank Tags redraw timing still require owner confirmation. The archival implementation is also covered by exact-byte manifest, migration, failed-publication, export and character-isolation tests.

### Validation and owner steps

**133 focused tests passed**, plus the reset benchmark, with only changed-case reruns afterward. Production and test compilation passed with Java 11. The focused set includes 45 PotionStorageTest, seven whole-overlay/painter cases, 44 bank pipeline cases, 26 persistence cases, nine backup cases and two reset-specific buyback/GE cases. XML evidence is retained in `build/reports/m10-7-ui-reset-targeted/`. The full suite was not run. Fixtures never accessed real progression. Windows installer sources and artifacts were not rebuilt.

1. Use the existing development run task and official Jagex login guide. Only the owner operates the game.
2. Review Manual Corrections in Bank Items and Potion Storage. Search, resize and inspect physical/virtual units, sprites, last-verified/cached text and bottom-right actions. Verify absolute and signed preview/confirmation on a known test balance.
3. Open native storage and visually confirm FS amounts in both columns. Test zero/positive/negative/unknown, custom color, dose selection, scrolling, reopen, resize and plugin disable. If absent, send only the capped `From Scratch potion overlay:` DEBUG lines and a private-information-free crop. The code-level defects above are fixed, but native visibility cannot be certified by fixtures.
4. On disposable progression only, confirm Reset, observe pending/completion and responsiveness, then start a new run. Check debts, storage, buybacks/spending, GE history, time and peak are fresh; exports, old recovery files and another character remain safe. Do not reset the valuable run for this test.
5. Repeat ordinary/storage overdraft and repayment checks to confirm the owner-accepted protection remains intact. Send one consolidated acceptance/correction list.

This pass changes six production areas: BankCorrectionDialog, PotionStorageOverlay, BankOverlayState, BankController, BankPanel and BankRepository. Eight directly related test files and focused documentation were updated. The full worktree inventory below includes earlier uncommitted M10.7 work.

## Files changed

Current worktree, including earlier uncommitted M10.7 implementation:

```text
 M README.md
 M docs/BETA_TESTING.md
 M docs/DATA_AND_PRIVACY.md
 M docs/FEATURES.md
 M docs/INSTALLATION.md
 M docs/PLUGIN_HUB_REVIEW.md
 M docs/README.md
 M docs/technical/ACCOUNTING.md
 M docs/technical/ARCHITECTURE.md
 M docs/technical/TEST_PLAN.md
 M gradle/beta.gradle
 M scripts/Build-Beta.ps1
 M scripts/Build-NativeInstaller.ps1
 M scripts/Test-NativeInstaller.ps1
 M src/main/java/com/fromscratch/FromScratchConfig.java
 M src/main/java/com/fromscratch/FromScratchPlugin.java
 M src/main/java/com/fromscratch/bank/BankActions.java
 M src/main/java/com/fromscratch/bank/BankAttestation.java
 M src/main/java/com/fromscratch/bank/BankAudit.java
 M src/main/java/com/fromscratch/bank/BankController.java
 M src/main/java/com/fromscratch/bank/BankCorrectionDialog.java
 M src/main/java/com/fromscratch/bank/BankEngine.java
 M src/main/java/com/fromscratch/bank/BankPanel.java
 M src/main/java/com/fromscratch/bank/BankRun.java
 M src/main/java/com/fromscratch/bank/BankSidebar.java
 M src/main/java/com/fromscratch/bank/BankSource.java
 M src/main/java/com/fromscratch/bank/BankView.java
 M src/main/java/com/fromscratch/bank/ItemHistory.java
 M src/main/java/com/fromscratch/bank/RuneLiteBankSource.java
 M src/main/java/com/fromscratch/bank/VirtualBankMode.java
 M src/main/java/com/fromscratch/examine/ExamineFormatter.java
 M src/main/java/com/fromscratch/examine/NativeExamine.java
 M src/main/java/com/fromscratch/overlay/BankItemPainter.java
 M src/main/java/com/fromscratch/overlay/BankOverlayRegistration.java
 M src/main/java/com/fromscratch/overlay/BankOverlayState.java
 M src/main/java/com/fromscratch/overlay/VirtualBankOverlay.java
 M src/main/java/com/fromscratch/persistence/BankRepository.java
 M src/main/java/com/fromscratch/persistence/BankRunCodec.java
 M src/main/java/com/fromscratch/persistence/PeakProgressRepository.java
 M src/main/java/com/fromscratch/persistence/ProgressBackup.java
 M src/main/java/com/fromscratch/wealth/BankValuation.java
 M src/main/java/com/fromscratch/wealth/FinancialEngine.java
 M src/test/java/com/fromscratch/bank/BagPipelineTest.java
 M src/test/java/com/fromscratch/bank/BankFixtures.java
 M src/test/java/com/fromscratch/bank/BankOnlyPipelineTest.java
 M src/test/java/com/fromscratch/bank/BankPolishTest.java
 M src/test/java/com/fromscratch/bank/BuybackPipelineTest.java
 M src/test/java/com/fromscratch/bank/FinalCorrectionsUiTest.java
 M src/test/java/com/fromscratch/bank/GePipelineTest.java
 M src/test/java/com/fromscratch/bank/MilestoneFivePolishTest.java
 M src/test/java/com/fromscratch/bank/PouchPipelineTest.java
 M src/test/java/com/fromscratch/bank/VirtualBankModeTest.java
 M src/test/java/com/fromscratch/bank/WealthPipelineTest.java
 M src/test/java/com/fromscratch/examine/NativeExamineTest.java
 M src/test/java/com/fromscratch/overlay/BankItemPainterTest.java
 M src/test/java/com/fromscratch/overlay/BankOverlayStateTest.java
 M src/test/java/com/fromscratch/overlay/LockedTintSettingsTest.java
 M src/test/java/com/fromscratch/overlay/OsrsQuantityFormatterTest.java
 M src/test/java/com/fromscratch/persistence/BankPerformanceTest.java
 M src/test/java/com/fromscratch/persistence/BankPersistenceTest.java
 M src/test/java/com/fromscratch/persistence/BuybackPersistenceTest.java
 M src/test/java/com/fromscratch/persistence/FinancialPersistenceTest.java
 M src/test/java/com/fromscratch/persistence/GePersistenceTest.java
 M src/test/java/com/fromscratch/persistence/PouchPersistenceTest.java
 M src/test/java/com/fromscratch/persistence/ProgressBackupTest.java
?? docs/MILESTONE_10_7.md
?? src/main/java/com/fromscratch/bank/BankSetup.java
?? src/main/java/com/fromscratch/bank/OutstandingPanel.java
?? src/main/java/com/fromscratch/bank/Overdrafts.java
?? src/main/java/com/fromscratch/bank/PotionSetupPanel.java
?? src/main/java/com/fromscratch/overlay/PotionStorageOverlay.java
?? src/main/java/com/fromscratch/persistence/PotionCodec.java
?? src/main/java/com/fromscratch/potion/
?? src/test/java/com/fromscratch/bank/PotionStorageTest.java
?? src/test/java/com/fromscratch/overlay/PotionStorageOverlayTest.java
```

## Final release hardening: reset archive throughput and fresh JUnit evidence

On 24 October 2026, isolated Java 11 fixtures reproduced the owner's reset delay with a larger retained history. Typed confirmation, immutable empty-state construction, the existing background storage queue, CAS validation, archive creation, forced journal/checkpoint publication and guarded UI acknowledgment were traced. There is no deliberate five-second wait. The diagnostic five-second interval only rate-limits DEBUG logging. Bank Tags/sidebar updates do not perform the archive IO.

| Identical reset fixture | Serial archive baseline | Bounded archive copies |
| --- | ---: | ---: |
| 8 transactions, 20 JSON files, 1,200 bank types | 183.498 ms | 143.457 ms |
| 240 transactions, 484 JSON files, 1,200 bank types | 796.054 ms | 329.199 ms |
| 1,200 transactions, 2,404 JSON files, 1,200 bank types | 4,689.128 ms | 1,534.425 ms |

For the large baseline, the archive took 4,651.695 ms: copy/open work 1,248.891 ms, forced flushes 3,049.525 ms, verified readback 86.248 ms, manifest publication 19.465 ms. Reset preparation took 0.172 ms. The bottleneck is serial per-file archive IO as retained history grows, including history intentionally kept across runs. These are local measurements, not live-client timings or CI thresholds.

`BankRepository.archive()` now uses at most four short-lived daemon workers for independent Filepath copies. The existing storage worker waits for **all** copies, including failures, before it can publish the forced manifest and then the reset checkpoint. Each destination still receives `force(true)` and exact-byte SHA-256 readback verification. Archive format, filenames, retained originals, exports and the final CAS/atomic publication rules are unchanged. Workers observe storage-owner interruption and shut down after the copy batch. No client APIs or Swing operations run on those workers. Temporary timing probes and debugger logpoints were removed; the repeatable test benchmark has no timing pass/fail threshold.

The existing immediate **Resetting progress...** notice and busy guard remain. Neither duplicates nor a new challenge can start while reset is pending. Persistence failure leaves the prior run authoritative and reports an error. Character/context/lifecycle guards reject stale acknowledgments. Focused tests additionally cover incomplete archive failure, reset acknowledgment after character switching/logout, durable Potion Storage/debt cleanup, and a fresh run after reloading the reset checkpoint. Existing export, playtime, buyback and GE reset regressions remain required. Actual responsiveness and Bank Tags appearance still require owner acceptance on disposable progression.

The old `BagPipelineTest.java:64` failure was located both in the archived reproduction and IntelliJ's saved test history, generated on 24 October at 01:06. It was not deleted. A forced native Gradle run started at **2026-10-24 19:18:58 UTC**, recompiled all inputs, and finished at **19:19:41 UTC** with exit 0: **25 tests, zero failures/errors/skips**. No Looting Bag source change was needed for this hardening pass. Current command, source hashes, timestamps and preserved XML/HTML reports are under the local ignored `build/reports/m10-7-release-hardening-20261024-191858/` directory.

Release configuration remains `v0.10.7-potion-storage-overdraft`, `0.10.7-beta.1`, Windows `0.10.7.0`, pinned Temurin `11.0.32.1+1` and Inno Setup `6.7.3`. Tests derive expected release/PE versions from `toolchain.json`. The target tag was absent locally and remotely and the public release lookup returned HTTP 404 at this check. Windows/Ubuntu hosted validation is pending an owner-approved commit/push. Any installer made from this dirty tree is only an unpublished local test candidate; rebuild the release from the clean accepted commit before tagging or uploading.

### Final validation for this hardening pass

- The forced complete command `./gradlew.bat build --rerun-tasks --no-daemon --console=plain` ran from **19:31:54 to 19:36:46 UTC**, exit **0**. All six tasks executed: Java compilation and **1,038 tests in 75 suites**, **zero failures, errors or skipped tests**. Source hashes captured before the command still matched after execution. Compiler unchecked/deprecation notices are warnings, not test failures.
- Focused checks covered 227 cases. Initially 225 passed and two newly added fixtures failed because they supplied another character's bank contents and a sequence-1 start after an existing reset. Their fixtures were corrected to preserve unavailable bank observations and the production continuation sequence; both reran successfully. No production safeguard or existing assertion was weakened. The complete fresh run subsequently passed both cases and every other test.
- The benchmark also runs during the full suite. Under that heavier run it measured **82.922 ms**, **688.917 ms**, and **3,424.659 ms** for the same 8/240/1,200-transaction fixtures. Archive performance still depends on disk and system load; larger retained histories can exceed one second. The isolated before/after table above is the comparable throughput measurement. Live reset responsiveness remains an owner test.
- `git diff --check` passed. The branch remains `master` at base commit `6536aa14061b62d673e66766e4746caed6d106e6`, with the existing uncommitted M10.7 work retained: 70 modified tracked files, 17 untracked source/document files and zero staged files. This pass modifies only `BankRepository`, `BankPerformanceTest`, `BankPersistenceTest`, `BankOnlyPipelineTest`, the existing potion reset test, and these implementation notes. The Looting Bag implementation and its test were unchanged.
- The previous metadata-referenced EXE was absent, so an additional local candidate was built using the existing scripts after the full regression passed. It is at `build/distributions/v0.10.7-potion-storage-overdraft/local-test-0.10.7-beta.1-20261024-193659-168/FromScratch-Beta-Setup.exe`, with adjacent `.exe.sha256`, `installer-build.json` and `installer-files.json`. SHA-256: `78a40f8e2277a1b86eb90bfa69ab4686cec3ec47efcff46d79670ff9a7c35f5a`.
- **All 15 native installer checks passed** for that candidate: payload/source integrity and private-data exclusion, pinned runtime, GUI/icon, unrelated working directory/spaced paths without system Java, actionable corruption/missing-file diagnostics, Downloads install, older-version upgrade, desktop/Start Menu references, HKCU registration, reinstall, and uninstall preserving synthetic progression/settings/backups/unrelated files. No real progression or authentication data was accessed. The installer is unsigned and explicitly records the dirty base-source provenance. Graphical wizard, actual client launch/login and in-game reset/buyback behavior remain owner acceptance requirements.
- No commit, push, tag, upload, distribution or Plugin Hub submission was performed. The final public installer must be rebuilt from the clean approved commit, followed by the usual source/checksum and hosted Windows/Ubuntu CI verification.
