# Interface previews and gameplay captures

## Existing sample panels

These images render actual production Swing components with **synthetic sample progression**. They are not live RuneScape screenshots, a real player's bank or proof of in-game behavior. The README uses the dashboard overview in its header and selected panels in the gallery, with this distinction stated explicitly.

| Image | What it shows |
|---|---|
| [Dashboard](media/dashboard.png) / [overview crop](media/dashboard-overview.png) | The sidebar and its six progress statistics. The overview is cropped from the same panel. |
| [Setup](media/setup.png) | The Before Starting panel with sample setup state. |
| [GE Purchases](media/ge-purchases.png) / [GE Sales](media/ge-sales.png) | Finalized example trades, including disclosed estimated tax. |
| [Bank History](media/bank-history.png) | Sample deposits, withdrawals and a legacy offset. |
| [Item History](media/item-history.png) | Sample item evidence, distinct from corrections. |
| [Buyback catalog](media/legacy-buybacks.png) / [utility view](media/legacy-buybacks-utility.png) | Refreshed current tiles, category order, fixed prices and synthetic availability. The first viewport does not show the complete catalog; see [all features](FEATURES.md#legacy-buybacks) and [prices](technical/BUYBACK_PRICING.md). |

Reproduce sample panels with `scripts/Prepare-UiPreviews.ps1`. The separate preview tool paints real components at 2x resolution with an empty home under ignored `build/marketing`; it reads no saves and never starts RuneScape. Inspect any refreshed image before replacing these assets, and keep its caption matched to the source and fixture used. Official item sprites/fonts have the attribution described in [ARTWORK](ARTWORK.md).

## Owner gameplay capture checklist

No live game captures are included yet. Only the owner operates RuneScape. Use a consistent RuneLite size and UI scale, crop to readable relevant areas, and remove account/character names, chat, friends/clan lists, authentication panels, private paths and unrelated windows. Do not alter displayed counts/statuses or reconstruct a bank for marketing. If using a disposable demonstration run, identify it in the caption.

| Proposed capture | Show |
|---|---|
| `gameplay-bank-quantities.png` | Legacy shading and gray physical/green virtual counts, including one mixed stack. Explain that normal withdrawal quantities remain available. |
| `gameplay-virtual-bank.png` | A useful earned bank in Virtual Bank, a native tab and an active native search. Capture Full Bank at the same real state if comparing views. |
| `gameplay-dashboard.png` | The progression sidebar beside the real bank, with readable statistics and host Time Played. |
| `plugin-options.png` | RuneLite's actual From Scratch settings and available controls, with unrelated profile/account information cropped. No fabricated settings panel. |
| `gameplay-buybacks.png` | Current Utility & Storage, Weapons, Armour and Skilling Outfits tiles, exact prices/statuses and a confirmation without purchasing for the picture. |
| `gameplay-storage.png` | Native Potion Storage amounts alongside the From Scratch dose/item labels. |
| `gameplay-history.png` | A genuine finalized trade or item-history row with relevant quantities visible and private details cropped. |
| `gameplay-examine.png` | The normal Examine response followed by one exact signed From Scratch line. |

Do not perform a valuable buyback, large trade, import or reset solely for a screenshot. Existing progression and safe ordinary actions are sufficient. After owner approval, add only the strongest useful captures to the README and label their source. The retired title artwork is not a gameplay capture and is not restored. [Gameplay](GAMEPLAY.md#screenshots-and-captures) has the player-facing capture summary.
