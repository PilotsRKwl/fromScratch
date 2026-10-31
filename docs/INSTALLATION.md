# Installation

## Plugin Hub availability

From Scratch is distributed through RuneLite Plugin Hub. **Availability is temporarily paused pending review.** The initial [submission #19721](https://github.com/runelite/plugin-hub/pull/19721) was merged, then RuneLite [disabled distribution](https://github.com/runelite/plugin-hub/commit/58bc2829c4738785fe3d674c356802a09aace07d) with the reason `disallowed readme`. Re-enablement requires maintainer review; see [current status](LAUNCH.md#current-status).

Once RuneLite restores availability:

1. Open RuneLite.
2. Open **Plugin Hub** from the configuration panel.
3. Search for **From Scratch**.
4. Select **Install**.
5. Open the purple coin sidebar and follow **Before Starting**. Bank your inventory and equipment before confirming your starting snapshot. See [Gameplay](GAMEPLAY.md).

Use normal RuneLite for installation and authentication. No separate From Scratch installer or launcher is required. RuneLite documents its supported login flow in [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Existing Friends Beta progression

Published Friends Beta 10.8.1 uses `.runelite/plugin-data/from_scratch/`; the production plugin uses `.runelite/plugin-data/from-scratch/`. These are separate histories. Keep the original beta directory and private backups.

**Already played the beta?** Keep a private backup, close all RuneLite and From Scratch clients, and complete the [offline migration utility](MAINTAINING.md#offline-progression-migration) before loading the production plugin. The utility copies and verifies the complete progression tree, preserves the original directory, and writes a completion receipt. The new plugin does not copy data or inspect the old sibling directory. It cannot tell whether an empty canonical directory belongs to a new player or an unmigrated beta player.

**Genuinely new installation?** Read the first-run migration guidance, explicitly confirm the new installation, then follow normal **Before Starting** bank setup. New-installation confirmation is not a recovery option for existing data: missing or invalid evidence alongside canonical progression blocks loading instead of silently starting another challenge.

After a successful transfer, continue that challenge only in the canonical version. Returning to the obsolete beta writes a separate old history; no synchronization or history merging exists. First validate the workflow on a disposable copy before using real progression. The owner reports successful migration of existing progression and completed manual acceptance of the production candidate. Native first-run behavior remains an explicit check for a genuinely clean installation; never reset an existing character to test it.

## Updating

Plugin updates are delivered through RuneLite Plugin Hub when available. Keep a private Export Progress backup before updating and close other clients using the same run. See [Data and privacy](DATA_AND_PRIVACY.md) for local storage and backup boundaries.

## Historical beta and development reference

[Historical beta documentation](HISTORICAL_BETA.md) preserves existing-installation maintenance, compatibility limits and installer testing notes. Releases, tags, Windows installer sources and packaging infrastructure remain unchanged. The historical standalone beta is not an alternative installation recommendation while Plugin Hub distribution is disabled.

Build and packaging commands belong in [Maintaining](MAINTAINING.md). Players do not need those tools. Actual game input, authentication and gameplay verification remain manual.
