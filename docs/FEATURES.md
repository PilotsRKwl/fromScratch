# Feature guide

From Scratch is a voluntary fresh-item challenge on an existing Old School RuneScape character. Keep your levels, quests, account unlocks and real possessions; distinguish your legacy bank from what you earn in the challenge. [Gameplay](GAMEPLAY.md) explains the rules and first run.

## Start a fresh run

Bank inventory and equipment yourself, then confirm the setup instructions. From Scratch records the complete bank as your legacy baseline. If Potion Storage is unlocked, its captured contents are included before Start. Missing or incomplete observations are never treated as empty. The original baseline remains unchanged throughout the run.

New bank deposits increase challenge progress; withdrawals reduce it. Spending more items than your available challenge quantity creates a signed deficit that remains visible until returned or replaced. Withdrawals do not prove consumption. Reset archives the old run after confirmation so you can begin again.

## Read your bank

- **Physical quantities** show how many real items are banked, in gray during the overlay.
- **Virtual quantities** show challenge ownership, green by default; negative quantities use the configured warning color.
- **Locked shading** marks items without positive challenge ownership. Mixed stacks retain the whole physical stack and show your smaller virtual allowance separately.
- **Virtual Bank** shows positive challenge-owned item types within the native tab or search. Turn its single Settings checkbox off to use Full Bank. Search and view changes do not change bank balances.
- **Examine** keeps normal game/core messages and adds one local From Scratch line with exact signed quantities and relevant buyback information.

Normal menus, quantities and withdrawals remain available. The plugin does not enforce the challenge for you.

## Progress and histories

The sidebar tracks Peak Progress Value, Current Bank Net Worth, Bank GP, Items Unlocked, Time Played and Total Value Lost / Spent. Guide prices and tax/loss estimates are labeled where appropriate. Missing prices are disclosed rather than invented. Time Played counts logged-in ticks on this host, not mobile time or wall-clock run age.

Bank and item histories show recorded changes and corrections. GE Purchases and Sales show finalized offers with nonzero executed quantities, including canceled partial fills. Pending offers and unfilled cancellations do not become completed-trade rows. Viewing history never replays a payment or changes ownership.

Overdraft warnings use newly saved negative changes. Outstanding items remain visible in the sidebar even without a physical bank slot or with Virtual Bank disabled. Potion debts are grouped by potion type and dose count. These are warnings, not withdrawal restrictions.

## Legacy Buybacks

Buybacks use fixed **virtual GP**, never real coins. Tiles appear in Utility & Storage, Weapons, Armour and Skilling Outfits, alphabetized within each category. Purchases require qualifying legacy equipment in the bank, enough virtual GP and explicit confirmation. Canceling spends nothing; a failed save grants nothing and spends nothing.

The catalog includes equipment, complete skilling outfits and utility/storage rewards. New complete outfits cost 2M each. Regular Dizana's Quiver costs 15M; blessed costs 60M directly or 45M more after a regular purchase. Existing prices are unchanged. [All exact prices and supported variants](technical/BUYBACK_PRICING.md) are documented separately.

Rune Pouch/Divine Rune Pouch share one 1M family payment, and open/closed Looting Bag share one 10K family payment. Supported Black Mask/Slayer Helmet copies cost 1M each through exact-item selection; Elite Black Knight pieces cost 30K each per copy. Naturally earned copies need no buyback. A payment never unlocks unlimited physical copies or unrelated variants.

## Storage and contents

Potion Storage displays an **FS:** quantity beside native amounts, using doses or items as appropriate. Unverified amounts are marked unknown. Bank/storage transfers preserve earned ownership across supported dose sizes. Existing unresolved stock can be reviewed through **Manual Corrections > Potion Storage** without resetting the run.

A container buyback unlocks the equipment, not its old contents. Verified Rune Pouch/Looting Bag contents receive linked negative legacy adjustments once; banking those contents clears the corresponding debt without awarding them again. Unknown contents are never automatically empty.

Fish Barrel, Herb Sack, Plank Sack, Seed Box, Tackle Box and quiver ammunition require explicit empty/shared-store and legacy-accounted declarations. Empty the entire shared store, including other copies. If legacy contents were banked after Start, correct those transferred amounts out of virtual ownership first. Cancel when their quantity or origin is uncertain. Available native nonempty evidence blocks a contradictory declaration. [Accounting details](technical/ACCOUNTING.md) explain these safeguards.

## Progress files and corrections

**Export Progress** creates a private local backup. Import previews a compatible same-character run, requires confirmation, preserves the existing run and replaces rather than merges histories. Manual Bank Corrections repair known errors; they are not an automatic way to earn legacy items. Positive corrections cannot exceed verified physical stock.

## Boundaries

- Progression is local and character-specific, with no cloud sync, shared progression or integrated leaderboard.
- The bank is the ongoing item-quantity authority. No continuous inventory/equipment ownership tracking or outside-bank wallet exists.
- Offline/mobile activity reconciles the next observed net bank change; unseen transactions and playtime cannot be reconstructed.
- Normal gameplay remains under the player's control. No synthetic input, game automation, packet manipulation or server ownership changes are performed.
- Exports are unencrypted private progression. Keep them out of public bug reports and use disposable runs for destructive testing.
