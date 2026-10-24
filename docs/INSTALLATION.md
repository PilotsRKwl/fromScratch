# Installation

## Windows beta

The **v0.10.7-potion-storage-overdraft** local test candidate (**0.10.7-beta.1**) is being prepared for owner testing. It includes uncommitted changes and must be rebuilt from the approved committed source before distribution. The existing 10.6 installer remains a separate earlier candidate. It has not been published or approved by RuneLite Plugin Hub. Download links will appear on [GitHub Releases](https://github.com/PilotsRKwl/fromScratch/releases) only after release approval.

The installer supports **64-bit Windows 10 and Windows 11**. Java is included. You do not need to install Java, change PATH, use a terminal or install developer software. Windows' built-in .NET Framework 4.x runs the small application launcher.

1. Download the approved **FromScratch-Beta-Setup.exe**. Keep its accompanying SHA-256 checksum if you want to verify the download.
2. Double-click the EXE and follow **Welcome to From Scratch Beta**. It installs for your Windows user without administrator rights.
3. Leave **Open From Scratch Beta** selected at the end, or use **From Scratch Beta** on your desktop or Start Menu.
4. In RuneLite, open **Jagex Accounts**, choose **Add Jagex Account**, complete the official login window yourself, select your character and choose **Play Now**. See RuneLite's [official login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
5. Enable From Scratch in RuneLite, open its purple coin panel and follow **Before Starting**, or resume that character's saved challenge.

The installer never asks for passwords or tokens. It does not export credentials or use insecure credential writing. Its bundled client uses RuneLite's existing developer-mode login route. Actual graphical launch and account authentication remain owner acceptance requirements; a successful install or verification check does not prove login works.

This candidate is **unsigned**. Windows may display an unknown publisher or reputation warning. Check the source and checksum, and stop if uncertain. Do not disable Defender, SmartScreen or antivirus software. No authenticated publisher identity is claimed.

## Updating

Export Progress privately before updating. Close From Scratch Beta, then run the newer approved installer. It updates the same per-user application and shortcuts. Previous owned program payloads may remain until uninstall; saved progression is separate and unchanged. Do not run an older beta against a newer save without a verified compatible backup.

The old v0.10.5 ZIP is retained separately. Its installed program files are not deleted by this installer. A verified old From Scratch shortcut may be replaced by the new EXE shortcut; unrelated shortcuts with the same name are preserved and installation is blocked with an explanation. [Historical script instructions](history/INSTALLATION_SCRIPTS_10_5.md) are reference only.

## Uninstalling

Close the application. Open **Windows Settings > Apps > Installed apps > From Scratch Beta > Uninstall** and confirm.

The uninstaller removes its registered program files, matching desktop/Start Menu shortcuts and Installed apps entry. It preserves unrelated files and changed shortcuts. It does not delete:

- From Scratch progression under `%USERPROFILE%\.runelite\plugin-data\from_scratch`.
- RuneLite settings, account data or other plugins under RuneLite's normal user directories.
- Exported backups wherever you saved them.
- Launcher diagnostics under `%LOCALAPPDATA%\FromScratchBeta\logs`.
- Other Java installations or the earlier v0.10.5 installation.

Program files live under `%LOCALAPPDATA%\Programs\FromScratchBeta`. The bundled runtime is private to the application; no system or user environment variables are changed.

## If the app cannot start

The launcher checks required files before opening RuneLite. A missing/damaged runtime or library produces an error dialog and a local diagnostic log. Reinstall a trusted beta if instructed. Keep logs private and redact account names, local paths and authentication-related context before sharing a short relevant excerpt. Do not send whole logs, credential files or progression exports.

## For maintainers

The [Milestone 10.6 implementation report](MILESTONE_10_6.md) lists the pinned runtime, developer compiler setup, reproducible packaging command and isolated tests. End users do not need those tools. [Beta acceptance](BETA_TESTING.md) covers actual gameplay and restart checks.
