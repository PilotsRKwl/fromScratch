# Legacy buyback pricing

From Scratch uses **fixed gameplay costs in virtual GP**. Purchases never move real coins. Runtime Grand Exchange prices do not set or adjust these prices. Historical receipts remain authoritative: this update adds entries without changing catalog version 1, any existing price, old receipt encoding, or old payment history.

The original balancing reference was roughly 2M GP/hour with medium gear. Equipment with tradeable components used an approximate GE equivalent; untradeable grind rewards used discounted reacquisition time. Deliberate balancing overrides either estimate. The new 2M complete outfits and utility prices are intentional accessibility choices.

## New fixed costs

| Utility & Storage | Virtual GP |
|---|---:|
| Abyssal Lantern | 3,000,000 |
| Fish Barrel | 4,000,000 |
| Herb Sack | 2,500,000 |
| Plank Sack | 1,500,000 |
| Seed Box | 2,500,000 |
| Tackle Box | 4,000,000 |

Rune Pouch / Divine Rune Pouch stays 1,000,000 once; Looting Bag stays 10,000 once.

| Weapons | Virtual GP |
|---|---:|
| Blade of Saeldor (corrupted) | 110,000,000 |
| Bow of Faerdhinen (corrupted) | 130,000,000 |
| Emberlight | 45,000,000 |
| Keris Partisan of Breaching | 12,000,000 |
| Keris Partisan of Corruption | 12,000,000 |
| Keris Partisan of the Sun | 12,000,000 |

| Armour | Virtual GP |
|---|---:|
| Avernic Defender | 31,000,000 |
| Crystal Body | 18,000,000 |
| Crystal Helm | 6,000,000 |
| Crystal Legs | 12,000,000 |
| Dizana's Quiver | 15,000,000 |
| Dizana's Quiver (blessed), direct | 60,000,000 |
| Blessed upgrade after a paid regular quiver | 45,000,000 additional |
| Elidinis' Ward (f) | 105,000,000 |

The regular charged/uncharged quiver and their locked forms are level 1 of one role. Blessed infinite-charge quivers and their locked form are level 2. The existing tiered entitlement mechanism charges 15M + 45M = 60M and grants one shared copy. Broken, mangled and max-cape items are excluded. Upgrading cannot refill an already withdrawn paid copy.

| Imbued rings | Virtual GP |
|---|---:|
| Archers Ring (i) | 3,000,000 |
| Berserker Ring (i) | 4,000,000 |
| Granite Ring (i) | 1,000,000 |
| Ring of Suffering (i) | 15,000,000 |
| Ring of the Gods (i) | 2,000,000 |
| Ring of Wealth (i) | 1,000,000 |
| Seers Ring (i) | 1,500,000 |
| Treasonous Ring (i) | 1,100,000 |
| Tyrannical Ring (i) | 1,200,000 |
| Warrior Ring (i) | 1,100,000 |

The ring reference is the tradeable base ring plus roughly 1M, rounded for gameplay. Official GE pages inspected during implementation showed approximately [Archers 1.9M](https://secure.runescape.com/m=itemdb_oldschool/Archers+ring/viewitem?obj=6733), [Berserker 3M](https://secure.runescape.com/m=itemdb_oldschool/Berserker+ring/viewitem?obj=6737), [Granite 21K](https://secure.runescape.com/m=itemdb_oldschool/Granite+ring/viewitem?obj=21739), [Gods 1.1M](https://secure.runescape.com/m=itemdb_oldschool/Ring+of+the+gods/viewitem?obj=12601), [Seers 597K](https://secure.runescape.com/m=itemdb_oldschool/Seers+ring/viewitem?obj=6731), [Treasonous 85K](https://secure.runescape.com/m=itemdb_oldschool/Treasonous+ring/viewitem?obj=12605), [Tyrannical 190K](https://secure.runescape.com/m=itemdb_oldschool/Tyrannical+ring/viewitem?obj=12603) and [Warrior 53K](https://secure.runescape.com/m=itemdb_oldschool/Warrior+ring/viewitem?obj=6735). The requested Berserker 4M and Suffering 15M values are explicit balance choices; Suffering stays 15M despite the reference GE page moving. These links document the methodology, not a live pricing dependency.

Nightmare Zone, Soul Wars and PvP Arena imbues share one role per ring; Suffering includes recoil variants. The additional Ring of Wealth family includes all six imbued charge states for 1M, rounded from the [roughly 11K base ring](https://secure.runescape.com/m=itemdb_oldschool/Ring+of+wealth/viewitem?obj=2572) plus 1M. The public [tradeable-item mapping](https://prices.runescape.wiki/api/v1/osrs/mapping) lists the base ring and imbue scroll separately; the imbued charge states are untradeable and still require native definition verification. Only imbued forms qualify for purchase. Unimbued equivalents participate in copy accounting so removing/reapplying an imbue cannot duplicate ownership. Other rings, imbue scrolls, components, minigame-only duplicate IDs and unsupported forms are excluded.

| Complete Skilling Outfit | Virtual GP |
|---|---:|
| Carpenter's Outfit | 2,000,000 |
| Pyromancer Outfit | 2,000,000 |
| Raiments of the Eye | 2,000,000 |
| Smiths' Uniform | 2,000,000 |
| Zealot's Robes | 2,000,000 |

Each new set has four required roles. Eye dyes and Smiths ice gloves are equivalents within their role. Warm gloves and bruma torch are separate Wintertodt rewards and cannot replace Pyromancer boots. Graceful 1M, Prospector 1M, Angler 750K, Rogue 500K, Lumberjack 500K and Farmer 750K remain unchanged.

## Variants and storage safety

The explicit allowlist in [BuybackCatalog.java](../../src/main/java/com/fromscratch/buyback/BuybackCatalog.java) uses RuneLite 1.13.4 gameval constants. It includes supported corrupted crystal-weapon colours, charged/inactive crystal-armour colours, Avernic Ghommal cosmetics, fortified Ward ornament, lit/unlit Abyssal Lantern variants, and the named outfit equivalents. Tradeable weapon seeds, Tormented Synapse, Avernic hilt, ordinary/unfortified Ward, regular crystal weapons, and dummy/note/placeholder IDs do not qualify. Runtime native definition checks still reject unsupported tradeable entries.

Every new equivalent family pools observed bank deltas before payment, so opening a legacy container or changing an equivalent cosmetic form cannot create an unpaid positive copy. Paid roles use the existing one-copy allowance across all forms. This does not infer unseen inventory conversions or alter physical bank quantities.

Fish Barrel includes fish-sack-barrel and open/closed forms; Herb Sack includes open/closed silk equivalents; Seed Box includes open/closed forms. Storage may be shared between physical copies. None of these buybacks includes the stored items. The purchase workflow requires explicit declarations that the shared store is empty and that any legacy contents deposited during the run have already been accounted for through Manual Bank Corrections. Regular/blessed quiver purchases apply the same safeguard to shared ammunition. Available native nonempty evidence vetoes a declaration; unavailable evidence is never reported as verified empty. The receipt retains declaration provenance. See [accounting policy](ACCOUNTING.md) for the atomic payment and signed-balance rules.

| Store | Pinned RuneLite 1.13.4 exposure and chosen workflow |
|---|---|
| Fish Barrel | The mapped barrel varbits do not provide a complete typed 28-slot contents proof. Explicit empty/accounted declaration; no invented fish quantities. |
| Herb Sack | No complete authoritative on-demand sack contents container is available in the pinned gameval API. Explicit empty/accounted declaration, including silk equivalents. |
| Plank Sack | Seven `PLANK_SACK_*` quantity varbits exist. Positive available quantities veto; zeros do not prove a fresh complete historical capture. Explicit empty/accounted declaration. |
| Seed Box | `InventoryID.SEED_BOX` exposes contents after a native packet. Current-session positive contents veto; an absent or cached container is not a banked empty proof. Explicit empty/accounted declaration. |
| Tackle Box | `InventoryID.TACKLE_BOX` exposes a shared native inventory. Current-session positive contents veto; no per-copy contents multiplication. Explicit empty/accounted declaration. |
| Dizana's Quiver | `InventoryID.DIZANAS_QUIVER_AMMO` exposes shared ammunition after a native packet. Positive contents veto either tier; explicit empty/accounted declaration at purchase/upgrade. |

All reads are on demand on the client thread. The full bank remains the only ongoing item quantity authority. Emptying a container and correcting the exact legacy transfer uses existing bank accounting; purchase itself neither subtracts it twice nor unlocks stored items. Native packets/varbits are used only to reject a contradictory declaration, not to fabricate proof.

## Menu ordering and compatibility

The catalog defines Utility & Storage, Weapons, Armour, then Skilling Outfits. Entries sort case-insensitively by display name, with catalog ID as the deterministic secondary key. The tile view, dropdown and search use this order. Old entry IDs, prices, Slayer exact-ID selection and the selected-copy receipt format remain intact.
