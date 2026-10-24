# Bank accounting policy

Accepted rules with the authorized Milestone 10.7 Potion Storage extension, pending owner acceptance. Earlier equations, migrations and superseded experiments are preserved in [ACCOUNTING_PRE_M10.md](../history/ACCOUNTING_PRE_M10.md). [Architecture](ARCHITECTURE.md) and the [reviewer source map](../PLUGIN_HUB_REVIEW.md) provide implementation pointers.

## Signed bank progress

Let `P` be the last complete physical bank, `P'` the next complete physical bank, and `B` the saved signed virtual balance. Ordinary changes use checked long arithmetic:

```text
B' = B + (P' - P)
```

Initial `B` is zero. The confirmed initial legacy snapshot is immutable. Negative balances are valid; they are not automatically clamped or forgiven. Positive corrections cannot exceed the current observed physical quantity. Placeholder templates contribute zero physical quantity. Normalization for rendering does not merge unrelated accounting IDs.

Bank tabs, searches and Virtual Bank visibility never alter `P`. The complete API container remains authoritative. Missing data is unknown, never an empty bank. Withdrawals are not evidence of consumption, spending, equipment use or provenance. Offline/mobile reconciliation records the next net bank change, without inventing unseen transactions.

## Potion Storage and overdrafts

### Potion doses and transfers

Ordinary items retain the equation above. Supported potion types additionally have signed stored units in the same `BankRun`. Finished potions use their enum-defined doses (including one/two-dose mixtures); unfinished categories use single items. Vials, noted items and unknown definitions are not potion doses. Bank bottles retain physical IDs and quantities. Reconciliation can redistribute virtual doses between bottle variants without inventing physical bottles. Fewer than one full bottle's earned doses remain as a bounded dose remainder, included in valuation and overdraft calculations.

Opposite bank/storage deltas copied at one GameTick form one transfer. Only the source's positive virtual portion moves; its legacy portion remains legacy. Delayed returns use bounded per-type, once-consumed withdrawal budgets from the same continuous bank session. These are conservative attribution evidence, not proof of inventory contents or exact item provenance. Multiple possible source containers, changed unseen stock or changed definitions require review. Budgets are not reused across login sessions. A return/conversion cannot independently create a quoted gain.

New runs require the inventory/equipment banking attestation, a current-setup full-bank capture, and a complete storage capture when unlocked. A supported verified not-unlocked flag skips storage; unknown unlock state requires explicit player attestation. The captured bank proof is retained across bank modes but must still match the complete authoritative container/session at Start. The exact storage capture and bank snapshot commit together. A missing, dirty, incomplete or prior-login capture cannot authorize Start. Existing runs retain all bank ownership/history and original snapshots. Metadata alone never means empty stock. First verified contents are retained as a separate observation baseline with zero inferred ownership until an explicit storage review in Manual Corrections. Uncertain potion additions can be held without awarding ownership; Bank History retains physical/progress differences.

Manual Corrections has separate Bank Items and Potion Storage categories. A storage correction changes signed stored units through existing `STORAGE_REVIEW` audit evidence, without changing bank bottles, the original baseline or physical observations. It records previous/new amounts, checked adjustment, units and time under the character/run. Positive values cannot exceed last verified physical stock; cached amounts are disclosed. A correction does not refresh observation-session identity or generate an overdraft warning. Transfer budgets for changed types are invalidated; unrelated types remain intact. Paged history derives exact deltas from the preceding durable checkpoint. The accepted pooled wealth model receives the saved balance once, without new gain/spending evidence. This UI update retains format 10.

Current wealth sums bank and stored signed doses once, using a common cached representative bottle price divided by its dose count. Fractional GP is conservatively floored. Missing prices remain unknown; unresolved storage does not contribute a known zero and blocks an unreliable peak. Fresh verified storage is required to raise a pooled peak. The former historical bank high-water quotes remain auditable; storage transfer/conversion returns are excluded from new bank gains. No storage withdrawal, legacy deficit or transfer becomes permanent spending. No GP wallet, GE trade or buyback payment is replayed.

### Overdraft warnings

Warnings compare committed before/after signed availability with an actual negative physical delta. They report newly negative debt or the increase of existing debt, in items or potion units. Menu quantities are irrelevant. The controller's continuous-observation flag suppresses first/reopened/offline/import snapshots; identical snapshots are already deduplicated by the transaction queue. Manual changes, reviews, buyback contents offsets, initial baselines and unresolved attribution do not notify. Debts remain visible even without a physical slot, and potion debts are grouped by type rather than repeated per bottle variant. Deposits reduce signed debt normally; they do not independently earn a second gain.

## GP and paid legacy copies

Raw signed coins remain part of `B`. Available virtual GP is raw signed coins minus cumulative committed buyback payments. Effective projections subtract the payment once and grant only the catalog's qualifying physical-copy roles; they never fabricate physical items or give a buyback a GE market value.

A purchase validates current bank/identity/revision, legacy possession, approved variants, definitions, funds and any storage-content proof. The payment, entitlement, adjustments and audit commit atomically. A failed commit spends nothing and grants nothing. Unique purchase IDs and durable entitlements prevent a second charge after restart or a family variant change. Naturally earned copies do not require a legacy buyback.

Rune Pouch/Divine Rune Pouch share one **1,000,000 GP** payment. Closed/open Looting Bag share one **10,000 GP** payment. A storage buyback unlocks the equipment, not its pre-existing contents. Verified contents create linked negative signed balances in the purchase transaction, including coins for the bag. Unknown/stale contents are not assumed empty. Looting Bag proof distinguishes verified empty, verified nonempty and unknown. A fresh zero-item packet or explicit native View empty message can verify an empty bag even without item widgets. A character/session-bound View capture survives returning the bag to the bank for at most two minutes; it is invalidated by expiry, identity/session changes, conflicting contents packets or observed contents-changing actions. Matching repeats do not discard or renew the captured proof. A final banked-family/count and preview check still precedes the unchanged atomic payment/entitlement/offset transaction. No Potion Storage unlock condition is involved.

Example: earned nature runes `+100`, legacy pouch contents `500`. Purchase records `-500`, leaving `-400`. Depositing the old 500 produces `+100`. The earned 100 survive; the legacy return is neutral. Offsets are not consumable losses, income or additional GP charges. Later refills do not create new offsets. Old legitimate purchases/adjustments remain preserved; no historical contents are invented.

For a Looting Bag that exposes no reliable native contents, an explicit empty-bag checkbox is permitted as a **player attestation**, not automatic proof. The four evidence states are `UNKNOWN`, `VERIFIED_EMPTY`, `VERIFIED_NONEMPTY`, and `PLAYER_CONFIRMED_EMPTY`. The controller rechecks current native evidence at confirmation; verified nonempty contents always block the empty declaration. One physical supported bag must still be present in the current complete bank, with the same active character/run revision, verified definition, unpurchased family entitlement and 10K available virtual GP. Native verified contents retain their original offset behavior.

The attested empty purchase uses the same atomic payment/entitlement/audit transaction, with zero contents adjustments, existing character/run identity and timestamp. It records `bag.confirmation: PLAYER_CONFIRMED_EMPTY` in its existing receipt and exposes that provenance in Buyback History. The optional receipt field is read backward-compatibly: old native receipts retain their encoding and meaning, and historical purchases without contents evidence remain unknown. Format 10, original legacy snapshots, pooled wealth, return protection and Potion Storage state in the same BankRun are unchanged. A player declaration does not establish historical contents or forgive previously recorded debt.

## Grand Exchange

GE observations record cumulative executed quantity/reported GP, direction, state, durable local slot generation and uncertainty. They do not directly mutate bank quantities or GP. Bounded, once-consumed possible conversion budgets prevent reliable observed exchanges from creating duplicate financial gains; they do not assert collection destination or realized profit.

Player history is a derived finalized-offer view over retained audit evidence. Completed offers and canceled positive partial fills produce one row per lifecycle. Pending/active partials, zero fills, duplicates and slot clears do not appear. Restart preserves evidence and slot reuse creates a separate generation. Aggregating history creates no new economic transaction.

Tax is estimated where the client does not expose a verified settlement. Labels disclose that fact. Pending reserved GP and unexecuted canceled quantities are not permanent expenditures. A GP/item conversion is not new wealth merely because it later reaches the bank.

## Values and progress

Current Bank Net Worth uses the existing **signed** effective balances, including available GP once, with cached guide prices. Unknown, zero and stale prices remain distinct. Missing prices yield a disclosed subtotal; stale prices are estimates. Untradeable entitlements are not added as market assets.

Peak Progress Value is the monotonic highest reliable priced virtual wealth plus confirmed permanent progression expenditures. Committed buybacks count once. Legacy offsets, returns, ordinary bank transfers and GE conversions do not independently earn progress. Missing/stale checkpoints cannot invent a reliable new peak. Older runs begin with a conservative measured baseline; unknown historic peaks are not reconstructed as exact.

Total Value Lost / Spent includes confirmed expenditures and clearly labeled supported estimates, not ordinary withdrawals or legacy-content deficits. A genuinely new run displays `0 GP`; an estimate uses a leading `~`. Incomplete historical measurement is disclosed rather than fabricated as zero.

Earlier item high-water/fixed-price gain evidence is retained for audit and compatibility. Its historical lifetime-gain tile was replaced by Peak Progress Value; it is not another current wealth metric.

## Durability and correction

Forced journals/checkpoints, atomic replacement, CAS and guarded callbacks protect each commit. Migration archives original evidence before conversion and never silently rebases a run. Corruption/load failure preserves evidence and requires safe recovery; reset requires successful archive and confirmation.

Manual Bank Corrections remain explicit signed-balance corrections. Export/import replaces a verified same-character run after preview/confirmation and backs up existing progression; it never merges divergent histories. All records are local to the designated host. Time Played is an independent logged-in host-tick counter, not a financial input.

Storage-only observations must persist the same calculated gain exclusions as ordinary-bank observations. Exclusion replay is verified before commit, even when physical bank deltas are zero and a transfer resolves prior signed debt. Missing or incorrect evidence is rejected without advancing the durable head. Incomplete native storage builds are unavailable rather than zero; last acknowledged balances are retained and the storage display becomes unverified. This correction adds no save fields or migration.
