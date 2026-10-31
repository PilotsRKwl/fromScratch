# Historical Friends Beta for Windows

This document preserves compatibility and maintenance information for existing Friends Beta installations and development reference. New installations use [normal RuneLite Plugin Hub distribution](INSTALLATION.md), whose availability is temporarily paused pending review. The standalone beta is not recommended as an alternative while distribution is disabled. Existing releases, tags, installer sources and packaging tools remain preserved.

## Published beta

The latest published standalone beta release was [Friends Beta 10.8.1](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.8.1-buybacks-search-fix), internal package **0.10.8-beta.2**. Earlier releases remain available separately.

The beta supports 64-bit Windows 10 and Windows 11 and includes its own Java runtime. Players do not need developer tools or a terminal.

The historical setup wizard installed a per-user application and desktop/Start Menu shortcuts without administrator rights. The standalone launcher is not the installation route for the Plugin Hub plugin. Do not use it as a workaround for paused Hub availability.

The installer is unsigned. Windows may display an unknown-publisher warning or block execution. Do not disable Smart App Control, Defender, SmartScreen or other security policies. See [Troubleshooting](TROUBLESHOOTING.md) if it is blocked.

**Installer execution testing for the published 10.8.1 build: Deferred: blocked by local Windows Application Control. Manual installation, upgrade, and uninstall testing pending.** Source/version metadata, bundled runtime, packaging manifests and published checksums were verified; that does not claim those execution tests passed.

## Historical updates

Historical beta updates used the installer to replace the per-user application and shortcuts. Existing users should preserve a private Export Progress backup and close other clients using the same run before maintenance. Existing progression is separate from program files. The beta uses the normal RuneLite home; do not move or delete its existing From Scratch data.

Version 10.8 introduced save format 11, retained by 10.8.1. Keep a compatible private backup before returning to an older beta. Never run an older client against a newer save unless compatibility is established. The old 10.5 ZIP and other published installers are preserved; a current installer does not remove that separate old installation.

## Uninstalling the beta

Close the application, then open **Windows Settings > Apps > Installed apps > From Scratch Beta > Uninstall**.

The uninstaller is designed to remove its registered program files, matching shortcuts and Installed apps entry. It preserves changed shortcuts, unrelated files, From Scratch progression, exports, RuneLite settings and account data, other plugins and Java installations. Published 10.8.1 execution testing remains deferred as stated above.

Program files live under `%LOCALAPPDATA%\Programs\FromScratchBeta`. Published beta progression lives under RuneLite's scoped `.runelite/plugin-data/from_scratch` directory; the production plugin uses the canonical `from-scratch` directory described in [Installation](INSTALLATION.md#existing-friends-beta-progression). Launcher logs remain under `%LOCALAPPDATA%\FromScratchBeta\logs`. No system Java or persistent PATH changes are made. See [Data and privacy](DATA_AND_PRIVACY.md) for what those files contain.

## Development reference

Build and packaging commands remain in [Maintaining](MAINTAINING.md#standalone-beta-distribution). Existing beta players should follow the [offline progression migration guidance](INSTALLATION.md#existing-friends-beta-progression) before switching to the production plugin. Original beta and production histories do not synchronize or merge.
