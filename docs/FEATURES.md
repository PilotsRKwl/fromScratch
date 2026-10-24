# From Scratch — feature guide

Milestone 10.7 adds Potion Storage and Overdraft Protection to the accepted challenge. This update awaits live acceptance. The plugin is a voluntary challenge, not an official account mode.

## Start with the character you already have

Bank all inventory/equipment yourself and confirm the setup attestation. A complete normal-bank API snapshot establishes immutable legacy quantities; filtering cannot start before its save succeeds. Levels/quests and physical possessions are untouched. Bank PIN/missing/incomplete observations block initialization; a genuinely empty complete bank is valid.

Signed progress follows `B' = B + (P' - P)`, where P is the complete physical bank. A legacy axe withdrawn from baseline becomes -1, then returns to zero when banked. New deposits increase progress. Unknown containers never mean zero. Placeholders represent no physical copies and retain their original item's signed overlay. Absent deficits remain in saved balances/corrections.

## Read the bank at a glance

- Physical top-left counts: #A0A0A0 during an active overlay, full physical quantity with native compact formatting. Dragged items keep native counters.
- Virtual bottom-right counts: configurable, #1DFF00 by default; compact integer K/M notation with no upward rounding.
- Zero/negative items: configurable locked tint, #333333 / 50% by default. Item shapes, menus and quantities are unchanged.
- Virtual Bank: default-ON preference, active runs only; positive effective types intersect native tabs 1–9 or All. Full Bank remains available through the single Settings checkbox. Search/other Bank Tags explicitly exit filtering. Separate special storage interfaces are outside this filter.
- Mixed stacks keep native physical quantities: gray 110 / green 10 means only 10 challenge copies, despite the full physical stack remaining withdrawable.
- Normal item Examine keeps native/core messages, then adds one From Scratch line with exact signed bank quantities, cached values and entitlement status. It adds no menu option.

## Progress dashboard

### Outstanding legacy items

An excessive confirmed withdrawal produces a red local chat warning and a RuneLite notification where enabled. The amber warning near the top of the sidebar lists outstanding items even when their bank slots disappear or Virtual Bank is disabled. Potion debts use doses, grouped by potion type. Return or replace items to reduce the signed debt; an entry opens recorded item history. Warnings do not block any native withdrawal. Mixed-stack tooltips explain that Withdraw All takes the full physical stack.

### Potion Storage

Open Mixology Potion Storage to see a compact **FS:** amount beside each native physical amount. It uses your virtual quantity color; outstanding amounts use Negative Virtual Quantity Color (red by default). Dose-based potions show doses. Unfinished and single-unit entries show items. Large values use compact bank notation where needed. A narrow row can show **FS** with the exact amount on hover. Unverified ownership shows **FS: ?**. Empty vials are excluded. The native quantities and controls stay unchanged.

Before a new challenge, bank your inventory and gear, confirm those steps, then open your bank. If Potion Storage is unlocked, open it next and review the captured amounts if needed. Once both are recorded, you can start directly from Potion Storage while the bank capture remains valid. A character without the unlock skips that step. Only an unknown unlock state needs your explicit answer. Both legacy snapshots save together before Virtual Bank activates; your banking confirmation stays checked while switching interfaces.

Existing challenges keep their original snapshots and earned balances. Use **Manual Corrections → Potion Storage** to review unresolved stock or edit a known virtual balance. The searchable list combines bottle sizes into one potion type and includes outstanding amounts even when physical stock is zero. Closed storage shows its last verified amounts as cached. Signed edits are confirmed, limited by verified physical stock and recorded in history. They change virtual ownership, never real potions. Old stock is never automatically awarded as earned. The main sidebar keeps outstanding warnings, without a separate storage list.

Observed bank/storage transfers preserve ownership across dose sizes. Read storage before transferring after login. Same-session withdrawal budgets prevent delayed returns or conversions from creating new ownership. If both containers are possible sources, the plugin requests review instead of guessing. Storage is never represented by fabricated native bank slots. Verified pooled potion doses contribute once to current wealth and reliable peak checkpoints, using an estimated per-dose guide price.

### Wealth and time

Exactly six primary statistics: Peak Progress Value, Current Bank Net Worth, Bank GP, Items Unlocked, Time Played, Total Value Lost / Spent. Prices are cached GE-guide estimates; unknown/stale prices are disclosed. Signed deficits subtract value. Bank GP is raw signed coins minus cumulative committed buyback payments, counted once. Untradeable buyback prices are not market values.

Peak Progress Value is the monotonic recorded high-water of priced signed bank wealth plus confirmed permanent progression expenditures. Old unknown peaks are not invented. Purchases of ordinary GE items merely convert assets; neither gross sale proceeds nor buyback entitlements create fresh wealth. Ordinary withdrawals are not proof of loss/consumption. Seller fees remain estimated where actual tax is unavailable; leading `~` identifies estimates.

Time Played accumulates logged-in host RuneLite gameplay ticks, persists independently and truncates whole minutes/hours/days. Prior wall-clock run age is not converted. The run's historical start timestamp remains intact.

## Legacy Buybacks

Curated cards group Combat Gear (including Void), Skilling Outfits, and Utility & Storage. Eligibility comes from the catalog/entitlement engine, not item-name guessing. Purchases require a qualifying observed legacy copy, an active run and sufficient available virtual GP. Confirmation revalidates funds/ownership/revision; GP commitment, role entitlement and audit/contents offsets commit atomically. Physical coins/items never move.

- Fire Cape 1000K; Infernal Cape 15M; Imbued God Cape 750K; Ava's Assembler 1000K; Dragon Defender 250K; Fighter Torso 2000K; Salve Amulet (ei) 500K; Ancient Sceptre 500K.
- Rune Pouch / Divine Rune Pouch: one 1000K family payment, genuine supported locked variants included; notes/vouchers/runes are not equipment entitlements.
- Looting Bag: closed/open variants share one 10K family payment. Naturally earned qualifying copies need no buyback; one earned copy does not grant every legacy copy.
- Void helms 300K each, top/robe 800K each, gloves 200K. Elite top/robe 1800K outright or +1000K with the corresponding purchased base role; no duplicate base payment/copy.
- Complete outfits: Graceful 1000K (six pieces), Prospector 1000K, Angler 750K, Rogue 500K, Lumberjack 500K, Farmer 750K. All qualifying pieces must be present; no partial charge/unlock.
- Barrows Gloves and Royal Seed Pod are excluded. No arbitrary untradeable grants or automatic refunds.

The authoritative catalog and exact approved variants are in [BuybackCatalog](../src/main/java/com/fromscratch/buyback/BuybackCatalog.java).

## Pouch and bag contents

A storage buyback unlocks the equipment, not preexisting contents. Rune Pouch uses a fresh bank-associated client/server varbit read and RuneLite's rune enum; no forced pouch withdrawal. Looting Bag uses current bank-session container 516 or explicitly observed native inspection/empty evidence. Missing data is not assumed empty. A purchase is blocked when the intended contents cannot be established.

Verified old contents create purchase-linked signed deficits in the same transaction. Existing earned nature runes +100 minus old pouch contents 500 becomes -400; depositing those 500 restores +100 without fresh gains. Coins in a bag affect raw signed bank coins separately from the 10K payment. Offsets persist, are applied once, and are not ordinary spending. Future refills never create another charge/offset. Earlier legitimate purchases are preserved.

## Grand Exchange and histories

The GE observer records cumulative offer quantities/GP, persistent generation identities, partial fills, completion/cancellation and uncertainty. GE events never independently alter bank item/coin quantities. Complete bank observations handle actual collections/deposits normally; bounded possible attribution avoids reclassifying conversions as new gains without claiming certainty about identical copies.

Purchases/Sales show one finalized nonzero executed offer: Bought, Sold, Bought (Partial), Sold (Partial). Active partials remain internal until full execution/cancellation. Executed quantity/GP and weighted average use cumulative observations; zero-fill cancellations and lifecycle noise are hidden. Generation IDs deduplicate pages/restarts/slot reuse. Original append-only audit evidence remains. Taxes shown for completed offers are average-price estimates, not verified settlement, and can differ from retained per-fill financial estimates. Realized profit and collection destination are not invented.

Bank History and item-specific history retain recorded deposits, withdrawals, manual corrections, automatic storage offsets, buybacks and associated observed trades. Manual Bank Corrections fix errors; they are not a normal synchronization step. History is paged/read-only.

## Boundaries

- Complete bank quantities plus verified Potion Storage; no continuous inventory/equipment ownership or outside-bank wallet. Other storage remains excluded except for the existing on-demand buyback contents proofs.
- Local-only, character-specific host storage. Other-device returns reconcile the next complete bank's net change, not unseen transaction details or mobile playtime.
- Unobserved offers completed/removed while away may have no reconstructable trade history. Unknown origin/tax/profit stays uncertain.
- No gameplay automation, synthetic actions, withdrawal blocking, server ownership changes or packet manipulation.
- No cloud sync. Export/Import Progress is explicit, private local transfer for the same character, replacing rather than merging runs.
- Reset archives progression after a typed confirmation. Use disposable runs for destructive testing; never reset a real run for screenshots.
