# Installation

## Plugin Hub availability

The first official Plugin Hub submission is being prepared. From Scratch is **not yet approved or available through Plugin Hub**. Review and acceptance are separate from Friends Beta publication. When approval and availability are confirmed, this guide will put ordinary RuneLite installation first; the standalone Windows EXE will not be required.

## Friends Beta for Windows

The current standalone release is [Friends Beta 10.8.1](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.8.1-buybacks-search-fix), internal package **0.10.8-beta.2**. Earlier releases remain available separately.

The beta supports 64-bit Windows 10 and Windows 11 and includes its own Java runtime. Players do not need developer tools or a terminal.

1. Download [FromScratch-Beta-Setup.exe](https://github.com/PilotsRKwl/fromScratch/releases/download/v0.10.8.1-buybacks-search-fix/FromScratch-Beta-Setup.exe) and its [SHA-256 checksum](https://github.com/PilotsRKwl/fromScratch/releases/download/v0.10.8.1-buybacks-search-fix/FromScratch-Beta-Setup.exe.sha256).
2. Run Setup and follow **Welcome to From Scratch Beta**. Installation is for your Windows user, without administrator rights.
3. Open **From Scratch Beta** from your desktop or Start Menu.
4. Use RuneLite's **Jagex Accounts** panel to sign in and choose your character. Follow RuneLite's [official login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). The installer never asks for passwords or tokens.
5. Enable From Scratch, open the purple coin sidebar and follow **Before Starting**, or resume your existing saved challenge. See [Gameplay](GAMEPLAY.md) for setup and voluntary rules.

The installer is unsigned. Windows may display an unknown-publisher warning or block execution. Do not disable Smart App Control, Defender, SmartScreen or other security policies. See [Troubleshooting](TROUBLESHOOTING.md) if it is blocked.

**Installer execution testing for the published 10.8.1 build: Deferred: blocked by local Windows Application Control. Manual installation, upgrade, and uninstall testing pending.** Source/version metadata, bundled runtime, packaging manifests and published checksums were verified; that does not claim those execution tests passed.

## Preparing the official-version storage transition

This applies to the prepared production source, not an already approved Plugin Hub release. Published Friends Beta 10.8.1 uses `.runelite/plugin-data/from_scratch/`; the prepared official version uses `.runelite/plugin-data/from-scratch/`.

**Already played the beta?** Keep a private backup, close all RuneLite and From Scratch clients, and complete the [offline migration utility](MAINTAINING.md#offline-progression-migration) before loading the prepared version. The utility copies and verifies the complete progression tree, preserves the original directory, and writes a completion receipt. The new plugin does not copy data or inspect the old sibling directory. It cannot tell whether an empty canonical directory belongs to a new player or an unmigrated beta player.

**Genuinely new installation?** Read the first-run migration guidance, explicitly confirm the new installation, then follow normal **Before Starting** bank setup. New-installation confirmation is not a recovery option for existing data: missing or invalid evidence alongside canonical progression blocks loading instead of silently starting another challenge.

After a successful transfer, continue that challenge only in the canonical version. Returning to the obsolete beta writes a separate old history; no synchronization or history merging exists. First validate the workflow on a disposable copy before using real progression. Native first-run and migrated-run acceptance remain owner checks.

## Updating

Export Progress privately before updating and close other clients using the same run. Run the newer installer to update the per-user application and shortcuts. Existing progression is separate from program files. The beta uses the normal RuneLite home; do not move or delete its existing From Scratch data.

Version 10.8 introduced save format 11, retained by 10.8.1. Keep a compatible private backup before returning to an older beta. Never run an older client against a newer save unless compatibility is established. The old 10.5 ZIP and other published installers are preserved; a current installer does not remove that separate old installation.

## Uninstalling the beta

Close the application, then open **Windows Settings > Apps > Installed apps > From Scratch Beta > Uninstall**.

The uninstaller is designed to remove its registered program files, matching shortcuts and Installed apps entry. It preserves changed shortcuts, unrelated files, From Scratch progression, exports, RuneLite settings and account data, other plugins and Java installations. Published 10.8.1 execution testing remains deferred as stated above.

Program files live under `%LOCALAPPDATA%\Programs\FromScratchBeta`. Published beta progression lives under RuneLite's scoped `.runelite/plugin-data/from_scratch` directory; the prepared official version uses the canonical `from-scratch` directory described above. Launcher logs remain under `%LOCALAPPDATA%\FromScratchBeta\logs`. No system Java or persistent PATH changes are made. See [Data and privacy](DATA_AND_PRIVACY.md) for what those files contain.

## Developers

Build and packaging commands belong in [Maintaining](MAINTAINING.md). Players do not need those tools. Actual game input, authentication and gameplay verification remain manual.
