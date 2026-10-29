# Playing From Scratch

**Your bank is full. Your adventure starts at zero.**

From Scratch restarts item progression on the character you already enjoy. Keep your levels, quests, achievements, account unlocks and physical bank. Set aside your existing items under the challenge rules, then earn supplies, equipment and wealth again through normal play.

The plugin tracks your progress. Following the challenge is voluntary: it does not block withdrawals or stop you equipping legacy items.

## Before your first run

1. Enable From Scratch and open its purple coin sidebar.
2. Bank your entire inventory and equipped items before starting. Items outside the bank are not continuously tracked, so do this yourself before confirming that they are banked.
3. Open the bank and follow **Before Starting**. Confirm that you have banked your items and wait for the bank snapshot to be recorded. The full bank stays visible until setup commits successfully.
4. If Potion Storage is unlocked, open it and follow the contents capture instructions. Unknown storage is not assumed empty; review the displayed setup information before proceeding.
5. Select **Start From Scratch**, review the confirmation and start the run. Your initial bank snapshot is the legacy baseline. Starting challenge ownership is zero.

You do not need to remove or destroy legacy possessions. Do not reset an existing valuable run simply to try setup again; export a private backup before any destructive experiment.

## Earn and use your challenge items

Gather, fight, quest, trade and explore normally. Bank what you earn so From Scratch can update your progress. Ordinary bank changes update signed challenge quantities; tabs, search and filters do not define what you own.

Suppose your starting bank has 100 sharks. After you earn and bank 10 more, the physical stack is 110 and your challenge allowance is 10. Withdraw up to those earned 10 for your next trip. The game still permits withdrawing all 110, so respect the challenge count yourself.

Positive virtual quantities identify available challenge stock. Zero means no available challenge quantity. A negative amount means legacy stock is outstanding; returning or replacing items moves that balance back toward zero. A normal withdrawal is not proof that an item was consumed or permanently spent.

Supported Potion Storage tracks the same idea in doses, so changing bottle size does not earn extra potions. If its contents are unverified, follow the storage review instructions rather than guessing historical ownership.

## Read the bank

- **Virtual Bank** shows item types with positive challenge ownership and intersects with native bank tabs and search. Use **Enable Virtual Bank View** in settings to switch back to Full Bank.
- **Gray quantities** show physical bank stacks. Bright virtual quantities show challenge ownership; colors are configurable. Shaded items identify unavailable legacy stock.
- **Normal Examine** adds an exact signed From Scratch quantity after the usual game and RuneLite messages.
- **Outstanding legacy items** remain visible in the sidebar when supported bank withdrawals exceed your allowance. Notifications inform you; they do not intercept the withdrawal.

Native banking controls remain available, including full-stack withdrawals. A visual filter is a guide to your challenge, not an enforcement system.

## Virtual GP and buybacks

Bank coins earned during your run to build your challenge coin balance. **Available virtual GP** accounts for committed buyback payments. Paying for a buyback reduces that virtual allowance; it never removes real coins from your bank or buys an item from the game.

Open **Legacy Buybacks**, select a tile, review eligibility and the fixed virtual GP price, then confirm. Canceling sends no purchase. The required legacy equipment must be physically banked, and complete outfit purchases require every qualifying piece. Naturally earned equipment needs no buyback.

Most family purchases grant one shared copy across supported equivalent forms. Slayer Helmets and Black Masks use exact-item selection and one payment per selected copy. Return a withdrawn paid copy before attempting another purchase where instructed. Quiver upgrades charge only the remaining amount for the same entitlement.

Storage purchases unlock the container, not its old contents. Rune Pouches and Looting Bags use supported contents evidence or explicit confirmations. Gathering containers and quiver ammunition require an empty shared store and a declaration that legacy contents are already accounted for. If you banked legacy contents after starting, correct those credited quantities through **Manual Bank Corrections** before confirming. Do not subtract the same contents twice, and cancel if their quantities or origin are uncertain.

[Fixed prices and supported variants](technical/BUYBACK_PRICING.md) · [Detailed accounting](technical/ACCOUNTING.md)

## Goals and history

Use the dashboard to follow your available GP, bank value, unlocked items and Time Played. Prices are estimates; Peak Progress Value retains the highest reliable measured progress and committed permanent buyback spending. Host Time Played counts logged-in play on the computer running the plugin, not mobile time or the age of your run.

**Bank History** and item history show recorded changes. **Grand Exchange History** shows completed offers and canceled offers with positive partial fills that the plugin observed. It cannot reconstruct every trade completed while the plugin was absent, and estimated tax is not a verified settlement.

**Manual Bank Corrections** is for correcting a known mistake or explicitly reviewing supported storage, not routine synchronization. Review the current balance and intended change carefully. Unknown ownership should stay unknown until you can account for it.

## Play alongside friends

Choose a shared starting date or goal, then keep your own run. Compare virtual bank values, race to earn a particular weapon, or work toward a first boss trip with newly earned supplies. Agree on any extra house rules yourselves.

Progress is local and separate for every player. From Scratch has no shared progression, multiplayer synchronization, shared bank or integrated leaderboard.

## Back up, move or reset a run

Use the panel's **Progress files** menu to **Export Progress** before an update, a computer move or a reset. Keep the `.fsp` file private. Saves and exports are not encrypted.

**Import Progress** validates the same character, previews replacement and preserves the existing run before restoring the selected backup. It replaces the active history rather than merging divergent copies. Use one computer as the main copy, and close other clients before restoring.

**Reset Progress** ends your current challenge after a typed confirmation. A recovery archive is retained. A new run takes a new starting snapshot with zero challenge ownership; real OSRS items and other characters' runs remain untouched.

[Installation and updates](INSTALLATION.md) · [Troubleshooting](TROUBLESHOOTING.md) · [Data and privacy](DATA_AND_PRIVACY.md)

## Screenshots and captures

Existing images are genuine plugin components rendered with synthetic sample data. They are interface previews, not authentic RuneScape gameplay captures. The hero banner is original artwork.

The owner can complete the public showcase with these real, cropped PNG captures at a consistent RuneLite size and scale:

| Capture | What should be visible |
| --- | --- |
| Full Bank with mixed ownership | An unavailable legacy item and a mixed stack with legible gray physical and bright virtual quantities. |
| Virtual Bank and native search | The Virtual Bank title, a native tab and a typed query showing only qualifying earned items. |
| Dashboard beside a useful earned bank | Current progress tiles and a small collection rebuilt through normal play. |
| Legacy Buybacks | Current categories, real eligible equipment, fixed prices and a purchase confirmation that can be canceled. |
| Completed GE or bank history | Existing observed activity, with no transaction performed only for a screenshot. |

Crop out account and character names, chat, friend/clan lists, authentication panels, private paths and unrelated windows. Do not edit quantities or statuses to simulate progression. Label any disposable demonstration run in its caption. Never reset valuable progression, make a valuable purchase or automate game input to create a picture. Only the owner operates RuneScape.
