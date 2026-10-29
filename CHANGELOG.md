# Changelog

Current status: Friends Beta 10.7 is published. Milestone 10.8 adds the three requested fixes and buybacks; official RuneLite Plugin Hub submission remains Milestone 11. Historical entries below retain their original development context.

## Milestone 10.8: Bug Fixes & Buyback Additions

- Native bank search intersects its results with Virtual Bank ownership while retaining the preference and native input. Independent Bank Tags and Full Bank choices keep their existing behavior.
- Black Masks (all charges and supported imbues) and Slayer Helmets (including current cosmetics) cost 1M virtual GP for one exact selected copy. A tile chooser handles multiple banked variants; duplicates require separate payments.
- Elite Black Full Helm, Platebody and Platelegs each cost 30K per exact copy. Elite Void prices and upgrades are unchanged.
- Format 11 records selected-copy identity and quantity in existing buyback receipts. Formats 5 through 10 remain readable, with historical family receipts unchanged; exports retain full payment and journal evidence. Back up before updating and do not load newer saves in an older beta.
- Windows x64 version 0.10.8.0 / package 0.10.8-beta.1, private pinned Java 11 and existing per-user installer. See [Milestone 10.8](docs/MILESTONE_10_8.md) for evidence and remaining live checks.

## Milestone 10.7: Potion Storage and Overdraft Protection

- Published [Friends Beta 10.7](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.7-potion-storage-overdraft), based on source 92d055a0c4d7bf3fb2104734e3c0d976827c789b, following owner gameplay acceptance and passing Windows/Ubuntu CI.
- Potion Storage doses, native quantities, setup verification, manual corrections, synchronized bank transfers and persistent overdraft warnings. Safe reset, empty Looting Bag attestation and existing buybacks retained.

## Milestone 10 — Windows beta and publication preparation (owner review pending)

- Freeze accepted M9.2 gameplay; verify canonical PilotsRKwl repository/master and disclose inaccurate historical tags without rewriting them.
- Prepare version 0.10.0-beta.1: thin production plugin plus separate developer bootstrap/unmodified RuneLite JARs, safe versioned Windows installer/desktop shortcut/update/uninstall, checksums and provenance. Use the official Jagex Accounts panel; no credentials or user data bundled. Graphical authentication remains manual acceptance.
- Replace the early README with a promotional homepage and original title/social artwork. Genuine gameplay screenshots remain explicitly pending; no beta asset URL is invented.
- Add reviewer/source map, feature/privacy/install/test guides, release draft and metadata recommendations. Verify production with the official standard build template. Hosted CI failures remain separately disclosed; no release or Hub submission occurs.

## Milestone 9.2 — Subdued physical bank counters (owner-accepted baseline)

- Draw physical bank quantities in #A0A0A0 after locked-item shading. Use the public quantity-display mode to suppress the original counter before native drawing and restore it when inactive; no sprite masks, quantity changes or new setting. Keep virtual counters and their configurable color unchanged.
- Preserve native physical stack thresholds, clipping, Full Bank/Virtual Bank behavior and native counters during dragging. Incremental compilation and 61 focused tests passed, including seven new visual cases; live font/scaling acceptance remains required. See `docs/MILESTONE_9_2.md`.

## Milestone 9.1 — Native tabs and finalized GE history (owner-accepted baseline)

- Intersect native item tabs with cached positive virtual eligibility through RuneLite's bank-build/filter callbacks; preserve native banking and lifecycle guards. Native search and other user tags explicitly return to Full Bank.
- Derive one completed/partial-cancelled GE row per durable offer generation, using cumulative execution quantities/GP. Hide active, zero-fill and lifecycle rows without rewriting audit evidence or replaying accounting. Paginate finalized trades with bounded background reads.
- Incremental compilation and 63 distinct focused tests passed; full regression and live-client acceptance remain separate. See `docs/MILESTONE_9_1.md`.

## Milestone 9 — Hardening (owner-accepted baseline)

- Hide zero-value GE slot-clears without deleting lifecycle evidence; center numeric columns and group average GP amounts.
- Replace the gain tile with durable Peak Progress Value; preserve old gains. New-run spending starts at 0 GP and estimated fees use a leading tilde.
- Fix fresh banked-bag observation without an inventory-side panel; retain explicit native inspection/empty evidence when needed and show a clear empty confirmation. Add the 10K shared open/closed Looting Bag legacy buyback, verified native contents offsets (including coins), earned-copy status and categorized responsive catalog cards. Unknown contents block purchase. Include peak checkpoints in local export/import.

- Restore native Virtual Bank, default on/first setting with saved off retained. Cache immutable membership/placeholder aliases; defer and guard native operations, respect Full Bank and user tags, and latch failures instead of retry loops. Live freeze acceptance remains pending.
- Show exact grouped signed quantities in Examine; keep compact overlays and accepted chat ordering/colors/statuses. Hide the remaining public developer-diagnostics setting.
- Add local checksummed Export/Import Progress, explicit verified previous-save recovery, stale-preview rejection, retained pre-import files and a single atomic restore selector for bank/history/playtime. Keep schemas 9/1; fail safely when atomic replacement is unsupported.
- Preserve both CI platforms/Java 11/cache; avoid redundant successful exact-commit tag builds and retain detailed failure artifacts. Ubuntu root cause remains blocked on authenticated logs, not attributed to migration notices or permissions.

## Milestone 8 — Native information and playtime (owner-accepted baseline)

- Fix disappearing Examine prefixes: native game chat treats the first pipe as a name/body delimiter. Use semicolons, balanced numeric highlights, compact GE/HA labels and whole optional-field limits; retain required name/quantity/status/payment. Add final-payload/native-split regression checks for all five reported items. No economic or persistence changes.
- Examine polish: queue after RuneLite's optional price line; use the item-price style with signed virtual bank counts, GE/HA totals, live quantity-color numerical highlights and normal-color escaped words/names. Catalog receipts distinguish locked prices from paid/shared/set/upgrade entitlements, without new accounting.
- Replace the earlier sidebar Virtual Bank button with default-off **Enable Virtual Bank View**. Supported native Bank Tags filtering opens automatically, respects active user tags and unregisters on close/disable. No fake bank or user layout edits.
- Correction validation: 15 directly affected tests passed; incremental production/test compile passed. Time Played, item history, persistence and financial logic are unchanged in this correction.

- Append a single bank-balance/GE/HA line after a correlated native item Examine response, without a new menu option or altering core Examine output. Skip uncertain/expired contexts.
- Add an optional native Bank Tags Virtual Bank filter for committed positive effective balances, with normal bank actions and preserved mixed physical stacks. No fake bank or withdrawal restrictions.
- Add paged item-specific evidence from Bank History, including manual/automatic adjustments, pouch offsets, buybacks and observed GE trades without claiming verified bank provenance.
- Replace wall-clock Active Run Duration with Time Played: logged-in host GameTicks, truncated minutes/hours/days, independent checked/Filepath checkpoints and conservative concurrent-client CAS. Existing run start, quantities, entitlements and financial records are unchanged.
- Twenty focused feature tests plus directly affected bank/sidebar/persistence regressions; full release regression and live acceptance remain separate.

## Rune Pouch simplification and sidebar rename (owner-accepted baseline)

- Replace manual pouch preparation UI with fresh opened-pouch/display/varbit verification and one atomic 1M family purchase plus signed legacy-rune deficits. Ordinary bank deposits restore balances without repeating gains; failed/stale/duplicate purchases cannot partially charge or unlock.
- Reuse format9 with optional purchase receipt evidence; preserve old entitlements, payments, signed adjustments and historical pending reservations. No inferred old contents or repeat charge.
- Add purchase-linked Automatic — Legacy Rune Pouch history and a brief acknowledged-success dialog. Rename only the Grand Exchange heading to Grand Exchange History.
- Validate with incremental compile and directly affected test filters only; full checkpoint regression is deferred as requested.

## Post-Milestone 7 — Compact layout and legacy pouch runes (owner-accepted baseline)

- Reduce standard statistic tiles from 112 to 98 pixels, action rows from 102 to 90, and Played Elsewhere from 132 to a 116-pixel minimum. Natural content/font size can expand them. Retain both columns, all icons, text, actions and existing render geometry.
- Order sections: From Scratch/statistics, Legacy Buybacks, Played Elsewhere?, Grand Exchange, Bank History, Reset Progress.
- Add a relevant-only pouch preparation/review inside Legacy Buybacks. Read supported RuneLite 1.13.3 varbit/enum metadata on request, require player verification even for reported empty contents, and record finite legacy rune quantities before moving them.
- Atomically hold matching new bank arrivals out of virtual progress, high-water gains, market worth and unlock counts. Preserve pre-existing earned runes. Explicitly restore unrelated earnings or retire unarrived contents; no blanket deductions or permanent phantom debts.
- Require an empty-pouch confirmation before a new 1M family payment. Keep standard/Divine/Trouver single-copy and upgrade/revert rules. Existing purchases retain payments and entitlements and receive review without inferred historical debits.
- Format 9 archives complete verified format-8 checkpoints/journals exactly before migration; pending/completed preparation and audit evidence use the existing worker/FIFO/CAS/recovery protocol.
- See [API evidence, accounting, tests, limitations and user-only acceptance](docs/history/POST_MILESTONE_7.md). No M8, commits, cloud or gameplay automation.


## Milestone 7 — Legacy Buybacks (historical development record)

- Add the curated Combat & Utility, Void Knight and complete Skilling Outfit shop, verified RuneLite item IDs, explicit one-copy equipment roles and cached OSRS sprites. No generic untradeable purchases or real game actions.
- Rune Pouch / Divine Rune Pouch is one permanent 1M family entitlement across standard/Divine/Trouver forms, with one payment/history record and bounded physical-copy allocation. Notes and minigame substitutes do not qualify; pouch contents are untouched.
- Preserve raw signed bank progress; available GP subtracts durable cumulative commitments. Bank GP and coin counters show available GP. Net worth subtracts expenditure once; confirmed spending increases once; existing historical gains and GE records stay unchanged.
- Support exact-price/insufficient/duplicate/stale safeguards, atomic complete outfits, Elite Void 1.8M outright or 1M after paid base, failed-write recovery, withdrawal/return and variant substitution without a second grant.
- Format 8 verifies/archives M6 files byte-for-byte before migration, retaining identity/start/immutable baseline/raw bank/financial/GE history and corrections. No new bank anchor, reset, cloud or personal-container gate.
- Add Legacy Buybacks shop/history tiles below GE; compact explicit confirmations and separate readable purchase history. Preserve M3 geometry, custom colors, quantity formatting, setup/reset and serial background persistence.
- See [catalog, verified IDs, tests, measurements and manual acceptance](docs/history/MILESTONE_7.md). No commit, publication or M8.

## Milestone 6 — Grand Exchange integration (historical development record)

- Preserve accepted M5 checkpoint v0.5.0-wealth and bank-only P/B authority. GE events never independently debit/credit bank coins/items or include uncollected offers in bank worth.
- Add passive RuneLite 1.13.3 offer observation, local slot generations, incremental quantity/reported-GP records, partial fills, completion/cancellation and uncertainty for initial/offline/reused offers. Ignore login-time EMPTY clears; refresh connection context without replaying fills.
- Associate compatible trades/refunds/returns with bank changes once using bounded, explicitly possible matches. Exclude that portion from new high-water estimates while preserving the ceiling and all earlier history; do not invent realized profit or exact collection destinations.
- Record clearly estimated seller fees once, using 2% average-price flooring, per-item 5M cap and published tool/bond/Mind exemptions. Keep estimated fees separate from manual expenses and physical Bank GP; unsupported exemptions/rates/gross-net semantics remain disclosed.
- Add Purchases/Sales tiles and read-only searched/sorted/filtered paginated histories, with reported GP/average, estimated tax, observed time and provenance. Retain accepted dashboard, separate Diary/partyhat manual icons and overlays.
- Extend forced local journal/checkpoint/CAS recovery to format 7; verify and archive original M5 files byte-for-byte before migration, keeping run/start/legacy bank/P/B/financial history unchanged. Preserve the latest per-slot totals under extreme event bursts and reuse unchanged overlay labels.
- See [M6 implementation, limits, tests and manual procedure](docs/history/MILESTONE_6.md). No M7, commit, publication, reset, cloud or personal-container ownership.

## Milestone 5 — Final withdrawal and UI corrections (historical development record)

- Fix native placeholder counters resolving the placeholder variant rather than the original item's saved balance. Final withdrawals retain/render the full signed deficit; absent slots keep their saved balance in valuation and Manual Bank Corrections. Physical aggregation, signed arithmetic, historical values and format 6 are unchanged.
- Share integer K/M formatting between item counters and all compact GP tiles: 1,000,000 is 1000K GP, 9,999,999 is 9999K GP and 10,000,000 is 10M GP. No decimals or rounding up; grouped exact tooltips remain.
- Use the verified Purple partyhat (1046) for both manual-history and correction-action sprites, cached once per item ID.
- Clarify the optional GE/other-storage legacy limitation without observing or gating GE. Keep attestation near step 2 and add a 14-pixel gap before the green Start action. Played Elsewhere explicitly says progress updates automatically.
- See [the final correction report](docs/history/MILESTONE_5_FINAL_CORRECTIONS.md) for regression evidence and manual tests. No saves were reset or schema changed; M6 remains unauthorized.

## Milestone 5 — Final UI/quantity corrections (historical development record)

- Replace wide financial debug tooltips with escaped, wrapped Swing HTML, exact GP and correct singular/plural text. Hide technical timestamps/migration diagnostics from ordinary tooltips; underlying measurement data is unchanged.
- Center Played Elsewhere instructions and reset/Start actions. Remove ordinary Active/Not Started subtitles while retaining actionable notices. Compact setup spacing, adapt checkbox height and size the setup card without reserving the hidden dashboard.
- Make Start From Scratch solid green/white with distinct hover, pressed and disabled styling. Preserve attestation, complete-bank gating and the original confirmation; reset still requires typed RESET and its existing archive safeguards.
- Correct item-only formatting: 0–9,999 exact, 10,000–9,999,999 uppercase K, ten million+ uppercase M. Thus 1,000,000 is 1000K, 1,740,000 is 1740K and 10,000,000 is 10M. Truncate, preserve signs/long bounds and fit exceptional wide labels inside the existing slot/viewport.
- Preserve monetary formatting, signed accounting, valuations, historical quotes, price cache, format 6 and all existing local saves. See [polish evidence](docs/history/MILESTONE_5_POLISH.md).

## Milestone 5 — Bank wealth and UI corrections (historical development record)

- M4 and its polished UI are user accepted. Preserve bank-only signed deltas, local saves, responsiveness, draw-only clipping, custom colors, navigation artwork and setup/reset safeguards.
- Use OSRS integer stack labels: exact below 10,000, truncated lowercase k below one million, truncated lowercase m thereafter; preserve signs and long bounds. Money has a separate compact/exact formatter.
- Rename the tool Manual Bank Corrections, add Fix an incorrect count and purpose text, and use verified Papyrus (970). Preserve the Diary history sprite and automatic mobile-return behavior.
- Cache RuneLite bulk GE-guide prices on a separate worker; distinguish unknown, verified zero and stale data, normalize note price identity only, and never fetch during banking/rendering.
- Value signed bank balances with exact one-GP coins, negative contributions, priced-subtotal/stale indicators and exact tooltips. Replace Spendable GP with Bank GP, without inventing a wallet.
- Add durable conservative per-ID high-water gains with fixed historical quotes/unpriced evidence. Transfers and corrections do not repeatedly generate gains. Record only explicitly confirmed statistical losses/expenses; no records means Not tracked.
- Add format 6 with exact-byte verified pre-migration archives, preserved bank state/history and an explicit imported financial baseline. Atomic commits/recovery/idempotency/character isolation remain mandatory.
- See [M5 report](docs/history/MILESTONE_5.md) for validation, timing, known estimation limits and manual acceptance steps. No M6, GE transactions, buybacks, cloud or personal-container accounting.

## Milestone 4 — UI polish pass (historical development record)

- Default quantity text to #1DFF00 and locked shading to #333333 / 50%; retain existing custom settings and apply edits live.
- Replace bundled navigation/Hub artwork with transparent purple pixel-art coins, without quantity labels. Keep 24x24/48x48 dimensions and optimized PNGs.
- Use verified RuneLite 1.13.3 Banker’s note (28767), Horror from the Deep Diary (3846) and Enchanted quill (21260) sprites for automatic history, manual history and corrections.
- Replace Played Elsewhere text/button with equal-width information and correction tiles; retain six statistic tiles and Coming soon financial placeholders.
- Make setup attestation clearly outlined with a bright checked state, hover/focus feedback and normal label/Space activation. Keep Start gated by bank readiness and active attestation.
- Paint the reset action solid red with centered white text and distinct hover/pressed states; retain typed RESET, revision checks and archive safeguards.
- Preserve bank-only accounting, local saves, history, scheduling and accepted overlay geometry. Full build evidence and manual steps: [UI polish report](docs/history/MILESTONE_4_UI_POLISH.md).

## Milestone 4 — Final UI/performance polish (historical development record)

- Remove a debugger-confirmed first-scan/two-extra-tick wait; capture complete banks at the next GameTick, cache until invalidation, and move automatic accounting to the storage worker. Queue complete snapshots FIFO during writes; publish only durable acknowledgments.
- Reuse/coalesce sidebar presentation and committed overlay projections. Add opt-in sampled monotonic phase timing without account contents. Native bank lag still needs user enabled/disabled comparison.
- Add six two-column OSRS-icon tiles; financial values say Coming soon. Replace the feed/dropdown with two read-only paged history views, move corrections into Played Elsewhere, and add a bottom danger area requiring typed RESET. Preserve archives and reset safeguards.
- Fix short text confirmations, shared wrapping/sizing/resizing/screen limits and keyboard/close handling. Add live locked-item color/opacity (#444444, 45%) independent of purple text, preserving full-slot clipping and interactions.
- Final clean build: 524 passed, 0 failures/errors/skipped in 36 suites, including 1,200-item/FIFO/durability and 1,240-entry history coverage. Retain bank-only equation, local format-5 saves/history/migration and Java 11/Wrapper. No M5, external service or new dependency. Current measurements, tests and manual procedure: docs/MILESTONE_4_POLISH.md.


## Milestone 4 — Bank-only rearchitecture (historical development record)

- Replace live inventory/equipment acquisition attribution, multi-container correlation, review/session gates and Update Progress with a single complete-bank authority. Retired controllers/readers/editor/reducer are test-only historical fixtures, preserving earlier regression evidence without shipping their gameplay path.
- Add checked signed progress B' = B + (P' - P), immutable initial bank, automatic complete/quiet bank observation and atomic cursor/balance/audit publication. Null/partial/PIN is unknown; verified empty is valid. No lifetime acquisition, consumption, spending or wealth inference.
- Add attested bank-only Start, bank-based dashboard, retained automatic first-session On return summary, direct searchable signed Bank Corrections and secondary protected reset. Keep accepted M3 tint/geometry/partial-row clipping, quantities, input and config keys.
- Add format 5 with exact-byte forced/manifest-verified archives of old save/checkpoints/journals. Preserve original run/start/legacy document and signed committed bank display. Require one confirmed bank anchor; listed excess positives need explicit approval rather than silent clamping. No recurring inventory/equipment walkthrough.
- Add 83 bank-only controller/observer/Filepath/actual-renderer integration, contract, Swing, corruption/migration and interrupted-save regressions. Final clean build: 500 passed, 0 failures/errors/skipped across 32 suites; original 417 retained. Current evidence and manual procedure are in docs/MILESTONE_4.md and TEST_PLAN.md. Earlier M4 designs below are historical and superseded.
- Keep storage local-only, Java 11, existing Gradle Wrapper/CI, metadata, author, license and artwork. Existing Lombok also processes historical test fixtures; no production dependency or external service was introduced. No M5, commit, push, tag, reset of private data or publication.


## Milestone 4 correction pass — Historical development record

The initial 376-test implementation failed live acceptance. This correction does not claim the user's exact shield path is proven without a live trace.

- Reproduce pickup followed by deposit through the actual observation/controller/storage/renderer path. Retain pre-bank receipt evidence and read-only ground Take hints until acknowledgment; ordinary transfers still conserve ownership and a deposit alone earns nothing.
- Automatically reopen tracking for unchanged or fully matched transfer-only session comparisons. Meaningful unresolved changes use secondary Account Tools → Update Progress, not a compulsory primary-dashboard approval.
- Add durable nonnegative outstanding legacy bank withdrawals, separate from earned quantities. Signed bank display is earned bank minus deficit; virtual-first movements affect D only for legacy copies. Returning legacy copies repays D without an acquisition. Retain absent-slot deficits in Account Tools. Preserve accepted geometry, clipping, tint, ordinary quantities and inputs.
- Replace the full-bank numeric editor with changed-only named rows, search/sort/Gains/Losses/Needs Review, applicable decisions, bulk selection, optional unchanged/advanced quantities, apply summary and non-mutating cancel/stale rejection. Keep previously owned Elsewhere copies intact. Collapse history at the bottom of Account Tools and remove the primary Lost / Spent placeholder.
- Add schema 4 / journal 2. Force exact-byte backups, verify established M4 format-3 digests with the old encoding and append a migration marker without rewriting prior segments/history. Start previously unrecorded deficits at zero rather than guessing; permit explicit audited declaration/resolution. Preserve all failed/interrupted evidence.
- Add opt-in rate-limited local DEBUG tracing for one selected item (default wooden shield 1171): observed before/after, gate/context, classification, review, durable commit and actual renderer value. No whole-bank/identity logging or player-data transmission.
- Add 41 regressions across four suites, retaining the earlier 376 cases with intended-rule assertion updates. Final clean build: 417 passed, 0 failed/errors/skipped. Current evidence/manual procedure: docs/MILESTONE_4_CORRECTIONS.md. No M5, commit, push, tag, reset or publication.

## Milestone 4 — Historical development record

M1–M3 passed user acceptance; M3 was committed by the user as `5b7c49f`. M4 has automated evidence only. No agent commit, push, tag, publication or progression reset was performed.

- Replace technical setup availability labels with checked inventory/equipment/bank steps. Add the same guided comparison at each new host login/re-enable, with explicit review/confirmation before new item credit.
- Observe complete API containers with session/time context, verified empty personal locations, event invalidation, a full quiet tick for transfer correlation and throttled availability-only DEBUG diagnostics. Preserve reliable observations on temporary absence and normal world loading/hops.
- Add pure item accounting for acquisitions, virtual-first bank/inventory/equipment transfers, mixed stacks and explicitly linked noted/unnoted conversions. Preserve exact raw IDs and immutable legacy snapshots.
- Add matching one-item Eat consumption and Drop loss/return identity. Legacy-first returns, duplicate/out-of-order guards and conservative review prevent ordinary movement/pickups from creating another acquisition. Elsewhere preservation and return conserve known ownership.
- Route ambiguous decreases, unsupported collection, coin-funded outputs, transformations and unmatched banking increases to review. Add bounded editable virtual-location quantities, required correction reasons, permanent-loss choice and auditable adjustments. Rejected proposed gains leave no false acquisition/unlock.
- Add separate immutable local item ledger segments and bounded checkpoint histories. Envelope format 3 migrates verified formats 1/2 only after exact-byte forced backups, preserving character/run/baseline and existing M3 ownership. Validate journal chain, deltas, revisions/checksums; preserve corrupt/incomplete/orphan evidence without blank replacement.
- Feed committed bank quantities into the unchanged accepted M3 renderer. Show lifetime Items Unlocked, recent acquisitions, transaction history and elapsed run duration (including offline time). Financial metrics remain zero and explicitly deferred.
- Add 91 M4 regression tests alongside all 285 prior cases; see TEST_PLAN.md and docs/MILESTONE_4.md for final build evidence and user-only acceptance procedure.

No GP/GE/valuation/buyback engine, cloud service, external transmission, automated game action or grant-items test command was added. Real-client attribution, rendering integration and performance require user acceptance.

## Milestone 3 — Unreleased

Historical M3 record: M1/M2 passed user acceptance; M2 was committed as `9e6b757`. The user subsequently accepted the final M3 visuals and committed M3 as `5b7c49f`. No commit or publication was performed by the agent.

- Add immutable exact-ID virtual totals and independent bank/inventory/equipment/future-GE allocations, checked longs, sparse zeros and validation independent of physical legacy quantities.
- Add persistence envelope format 2; migrate verified M2 saves only after a forced exact-byte backup, preserving run identity, dates, immutable baseline and older recovery files. Reject corrupt/incomplete/future data intact.
- Register one manual normal-bank Overlay with purple bottom-right counts and clipped draw-only fading. Keep original quantity values readable without modifying items, sprites, menus or click zones. Placeholders display zero; fillers/empty slots/BLANKOBJECT are skipped.
- Correct partially visible rows: RuneLite 1.13.3's callback list requires the item's origin inside the clip. Read the actual bank item widgets instead, using their bounds intersecting the content viewport. Clip both viewport edges precisely, preserve full-bounds label coordinates and use RuneLite-provided drag rectangles where available. A clipped-origin drag without supplied bounds temporarily omits its effect rather than painting a ghost.
- Refine locked visuals: the original partial black mask became a full-bounds 30% gray tint; the latest request uses darker #505050 at approximately 48% opacity, closer to the bank background. Ordinary quantity glyphs retain approximately 52% contrast and purple counters are painted last. No sprite regeneration/caching is introduced. Add overlay-pass regressions for partial rows, controls/viewport exclusion, scrolling, searches, resizing, graphics state and lifecycle, plus a background-contrast regression. Ownership, accounting and persistence are unchanged by these refinements.
- Add live quantity/dimming toggles and text color. Remove detailed legacy summaries and temporary milestone/debug cards from active dashboards; preserve initialization previews, errors and dashboard metrics.
- Add model, migration/recovery, Java2D and overlay lifecycle/configuration tests while retaining prior M1/M2 cases. Details and exact final totals are in docs/MILESTONE_3.md.

No acquisition/transfer engine, mobile reconciliation, buyback/GE/valuation engine or grant-items control was added. Storage remains local-only on the designated host. Existing metadata, license, author Pilots r kwl, icons, Java 11 Wrapper and CI are preserved.

## Milestone 2 — Unreleased

M1 passed user-run acceptance. The user isolated the remaining M2 failure to empty inventory/equipment. The empty-state correction below awaits live retesting. No publication or release tag has been created.

### Empty personal-container correction

- Keep valid zero-length/all-empty-slot API containers accepted. No positive item count is required.
- Independently verify null empty INV/WORN through complete initialized personal interfaces on two different game ticks. Require all 28 inventory slots or all 11 logical equipment slots; reject missing/default/hidden views and populated widgets without API data. Never infer bank contents from widgets.
- Preserve three-location previews, explicit confirmation, cache age/change guards and existing persistence/schema. Players do not need to carry or equip an item to start.
- Add 29 regressions covering all four empty/populated combinations through both observed API data and null-container verification, unavailable/incomplete data, contradictory occupied slots, ring/ammo, settling and confirmation/codec preservation. All previous 173 tests remain (202 total).
- Permanently designate local-only storage on one host computer; exclude cloud synchronization, credentials and backends. Reconciliation after gameplay elsewhere remains future M4 work.

### Multi-phase personal-container correction (historical; empty-case follow-up below)

- Replace the simultaneous three-container requirement with validated inventory/equipment observations collected throughout setup, including before bank opening. Cache for at most 60 seconds; expire on changes, invalid data, identity/session changes or setup cancellation. Always read the current complete open bank.
- Copy valid personal ItemContainerChanged payloads, settle related updates on a game tick, invalidate personal caches after a previously observed bank changes, and revalidate on confirmation. Missing containers remain unknown.
- Show independent Bank, Inventory and Equipment readiness plus partial summaries and the manual interface step for missing data. No duplicate setup section.
- Add rate-limited DEBUG diagnostics for login/setup state, mounted bank, container sizes, cached observation ages and exact rejection predicate. No item lists or identity are logged.
- Add 26 regressions, retaining all 147 previous tests (173 total). Persistence implementation/schema and existing saves are unchanged. Automated results and remaining live uncertainty are documented in MILESTONE_2.md.

### First bank-readiness correction (historical; failed subsequent live acceptance)

- Remove the requirement for ItemContainerChanged on every bank opening. Detect the mounted normal bank, visible item layer and current containers, reject PIN/partial/null data, and allow a game-tick settling boundary.
- Retry unavailable capture data on game ticks; already-open and unchanged reopened banks require no quantity-change event. Show actionable diagnostics without repeating identical Swing updates.
- Show Bank snapshot: Ready with summaries and enabled confirmation. Closing the bank invalidates the preview; confirmation still rereads all contents.
- Replace duplicate instruction sections with one short message before initialization, capture status during initialization and no setup instructions after activation. Preserve the existing preference key for detailed hover help.
- Add 26 regressions; full suite is 147 tests, including the previous 121. Persistence code, save schema, valid runs and real items are unchanged.

- Add Begin/review/confirm workflow using the full normal bank, inventory and equipment containers; preserve exact IDs and checked long quantities in an immutable legacy baseline.
- Exclude placeholders, empty slots, bank fillers and special storage. Revalidate identity/contents on confirmation; no incomplete or automatic activation.
- Verify RuneLite character identity and separate account/profile-type storage with opaque keys.
- Add versioned checksummed Filepath checkpoints on a bounded background worker, recoverable replacement, explicit error/retry states and protected archival reset.
- Preserve active baselines across logout/restart/disable/profile switches. Reject duplicate starts and stale callbacks. Initial virtual economy remains zero despite legacy GP.
- Extend sidebar summaries, instructions, test coverage and documentation. The existing help config key, Java 11 target, metadata version and Gradle Wrapper are preserved.

No acquisition accounting, restrictions, overlays, buybacks, GE or valuation were added. Hosted CI and M2 manual acceptance remain pending.

## 0.1.0-dev — Unreleased

Foundation milestone; subsequently accepted by the user before M2 development.

- Rename package/plugin/config/project/metadata to From Scratch by Pilots r kwl.
- Add original purple coin assets, navigation lifecycle, placeholder dashboard, and local help preference.
- Define a validated immutable schema-v1 lifecycle header and stable statuses.
- Document architecture, accounting, milestones, tests, limitations, artwork, contributions, and publication gates.
- Add 27 foundation tests and Java 11 Linux/Windows CI configuration.

Local clean builds passed against RuneLite 1.13.3 and the observed Hub target 1.13.2. Hosted CI and in-game acceptance remain pending. No snapshots, virtual accounting, progression persistence, buybacks, bank overlays, or GE tracking are implemented yet.
