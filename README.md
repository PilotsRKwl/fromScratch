# From Scratch

**A fresh start without a fresh account.**

You've spent years building your Old School RuneScape account. Your bank is packed with equipment, supplies and wealth, but sometimes you want the excitement of starting over without making another character.

From Scratch lets you do exactly that. Keep your levels, quests, achievements, account unlocks and every item you've already earned. When you begin a challenge, your existing bank becomes your **legacy collection**: the items remain physically yours, but you set them aside under the challenge rules.

Start gathering supplies and earning equipment again as if your bank were empty. Rebuild your wealth, work toward new goals, and reclaim certain difficult-to-reacquire legacy items through virtual GP buybacks. Play alone or alongside friends running their own challenges, and reset whenever you want another fresh start.

**Your account stays the same. Your item progression starts over.**

From Scratch tracks your progress; you choose to follow the rules. It does not prevent withdrawals or stop you using legacy equipment, and it does not turn your character into an Ironman.

## Your challenge in three steps

1. **Keep your character.** Retain all levels, quests, achievements and account unlocks.
2. **Start with zero items.** Your starting bank becomes legacy gear. Everything stays yours, but those items are considered unavailable for the challenge.
3. **Earn everything again.** Play normally, bank newly earned items, track your challenge ownership and build toward your next upgrade.

Start another run at any time with **Reset Progress**. Your real items and account are untouched.

## Try the Friends Beta

From Scratch is **not yet available or approved on the official RuneLite Plugin Hub**. The current download is [Friends Beta 10.8.1](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.8.1-buybacks-search-fix) for 64-bit Windows.

1. Download [**FromScratch-Beta-Setup.exe**](https://github.com/PilotsRKwl/fromScratch/releases/download/v0.10.8.1-buybacks-search-fix/FromScratch-Beta-Setup.exe) and follow the setup wizard.
2. Open **From Scratch Beta** from your desktop and sign in through RuneLite's official Jagex Accounts panel.
3. Enable From Scratch, open its purple coin sidebar, and follow **Before Starting**. Bank your inventory and equipment before confirming your starting snapshot.

[Installation and update help](docs/INSTALLATION.md) · [Release notes and checksum](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.8.1-buybacks-search-fix)

The installer is unsigned. Local Windows Application Control blocked installer execution testing; manual installation, upgrade and uninstall checks remain pending. Keep Windows security protections enabled and see [troubleshooting](docs/TROUBLESHOOTING.md) if installation is blocked.

## See your fresh start grow

<p align="center"><img src="docs/media/dashboard-overview.png" alt="From Scratch dashboard with six progress tiles rendered using synthetic sample values" width="360" /></p>

*The actual dashboard rendered with synthetic sample progress. This is an interface preview, not a live RuneScape screenshot.*

Genuine gameplay captures are still needed to show the native bank, mixed item quantities and Virtual Bank in action. The [capture checklist](docs/GAMEPLAY.md#screenshots-and-captures) describes exactly what the owner can provide without exposing private account information.

## Build a bank worth using again

- **Find your available gear.** Virtual Bank filters to item types with positive challenge ownership while keeping native bank tabs and search. Turn off **Enable Virtual Bank View** to see your full bank.
- **Know what you can use.** Bright virtual counts show your challenge balance; gray counts show the physical stack. Shading identifies unavailable legacy items. Outstanding legacy withdrawals appear as signed debt until you return or replace them.
- **Set your next goal.** Follow bank value, available virtual GP, unlocked items, Peak Progress Value and Time Played in the sidebar. Item values are estimates; time counts play on this computer.
- **Review your journey.** Browse bank and item history, inspect exact quantities with normal Examine, and review completed Grand Exchange purchases and sales observed while the plugin is running.
- **Keep useful storage.** Supported Potion Storage uses dose-based progress. Rune Pouches, Looting Bags and other supported containers have safeguards so a buyback does not also unlock old contents.
- **Keep your run.** Progress saves locally for each character. Export a private backup, import it for the same character, or reset when you want a fresh challenge.

For example, your physical bank might contain 110 sharks while only 10 belong to the challenge. Both counts remain visible. You decide to withdraw the earned 10; normal banking still allows you to withdraw the whole stack.

[How to play](docs/GAMEPLAY.md) · [Detailed features and limits](docs/FEATURES.md)

## Reclaim your legacy equipment

Save virtual GP for **Legacy Buybacks** and give familiar gear a new place on your goal list. Browse alphabetized **Utility & Storage**, **Weapons**, **Armour** and **Skilling Outfits**, then review the price before confirming a purchase.

| Goal | Fixed virtual GP cost |
| --- | ---: |
| Fire Cape | 1,000,000 |
| Rune Pouch / Divine Rune Pouch family | 1,000,000 once |
| Looting Bag family | 10,000 once |
| One selected Black Mask / Slayer Helmet copy | 1,000,000 |
| Carpenter, Pyromancer, Raiments of the Eye, Smiths' Uniform or Zealot complete set | 2,000,000 each |

The catalog also includes corrupted crystal weapons, Emberlight, crystal armour, imbued rings, Dizana's Quiver and gathering containers. Full outfits require every qualifying piece, and copy limits still apply.

These are fixed gameplay costs paid with **virtual GP**. Buybacks leave real coins and equipment untouched. Equipment earned normally during the challenge is available without a buyback, and a storage unlock never includes its legacy contents.

[All fixed prices and supported variants](docs/technical/BUYBACK_PRICING.md)

## Your adventure, alone or alongside friends

From Scratch is a solo challenge you can also share with friends. Start on the same day, compare virtual bank values, race to earn a particular weapon, or set milestones for your next boss trip. Agree on your goals and celebrate each other's progress.

Each player keeps their own progression locally. There is no shared bank, automatic multiplayer synchronization or integrated leaderboard. Compare progress yourselves, and keep account details and private saves out of shared screenshots.

## FAQ

**Does it delete my bank or change my account?**

No. Your real items, levels, quests, achievements and account unlocks stay yours. From Scratch tracks a separate challenge balance.

**Does it stop me using legacy gear?**

No. This is a voluntary game mode. The plugin distinguishes legacy items from earned challenge ownership, but normal withdrawal and equipment controls remain available.

**How do I earn challenge items and GP?**

Play normally and bank what you earn. From Scratch updates your challenge balances from bank changes. Available virtual GP is your challenge coin balance after committed buyback spending. See [the gameplay guide](docs/GAMEPLAY.md) for examples.

**Can I use the Grand Exchange?**

Yes. Normal buying and selling are part of the challenge. Completed trades the plugin observes are shown in its history.

**Can I play on mobile or another computer?**

The next bank observation on your usual computer reconciles the net bank change, but cannot reconstruct every unseen transaction or mobile playtime. Use private Export / Import Progress when moving your main run between computers; there is no automatic sync.

**Can I start over?**

Yes. **Reset Progress** ends the current run after confirmation and keeps a recovery archive. Export a private backup first if you want your own copy. Your actual items remain untouched.

## Privacy and safety

Progress stays on your computer. From Scratch has no account registration, cloud progression or analytics service, and it does not ask for passwords or tokens. It uses RuneLite's public item prices without uploading your holdings. Saves and exports are private and unencrypted, so keep them safe and never attach them to a public issue.

[Data and privacy](docs/DATA_AND_PRIVACY.md) · [Safe troubleshooting and bug reports](docs/TROUBLESHOOTING.md)

## Support and documentation

[Report a bug or request a feature](https://github.com/PilotsRKwl/fromScratch/issues/new/choose) · [Gameplay](docs/GAMEPLAY.md) · [Installation](docs/INSTALLATION.md) · [Feature reference](docs/FEATURES.md) · [Documentation](docs/README.md) · [Contributing](CONTRIBUTING.md)

Created by **Pilots r kwl**. [BSD 2-Clause license](LICENSE) · [Artwork credits](docs/ARTWORK.md) · [Plugin Hub reviewer guide](docs/PLUGIN_HUB_REVIEW.md)

RuneScape and Old School RuneScape are trademarks of Jagex Ltd. Item artwork belongs to Jagex. From Scratch is an independent community project with no Jagex or RuneLite affiliation.
