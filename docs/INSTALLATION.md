# Installation

## Plugin Hub availability

From Scratch is distributed through RuneLite Plugin Hub. **Availability is temporarily paused pending review.** The initial [submission #19721](https://github.com/runelite/plugin-hub/pull/19721) was merged, then RuneLite [disabled distribution](https://github.com/runelite/plugin-hub/commit/58bc2829c4738785fe3d674c356802a09aace07d) with the reason `disallowed readme`. Re-enablement requires maintainer review; see [current status](LAUNCH.md#current-status).

Once RuneLite restores availability:

1. Open RuneLite.
2. Open **Plugin Hub** from the configuration panel.
3. Search for **From Scratch**.
4. Select **Install**.
5. Open the purple coin sidebar and follow **Before Starting**. Bank your inventory and equipment before confirming your starting snapshot. See [Gameplay](GAMEPLAY.md).

Use normal RuneLite for installation and authentication. RuneLite documents its supported login flow in [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## First challenge

v1.0.0 will be the first official public release. Open the plugin and complete **Before Starting**: bank your inventory and equipment, capture the complete bank and any supported Potion Storage, confirm the challenge rules, and start a new From Scratch run.

Starting a challenge changes only From Scratch's local challenge records. Your real OSRS items, levels, quests and unlocks remain yours. Once started, the challenge resumes normally on this computer. **Reset Progress** requires explicit confirmation and preserves a recovery archive; do not delete files to resolve a loading problem.

## Updating

Plugin updates are delivered through RuneLite Plugin Hub when available. Keep a private Export Progress backup before updating and close other clients using the same run. See [Data and privacy](DATA_AND_PRIVACY.md) for local storage and backup boundaries.

## Developers

Development commands belong in [Maintaining](MAINTAINING.md). Players do not need those tools. Actual game input, authentication and gameplay verification remain manual.
