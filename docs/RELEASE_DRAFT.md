# Friends Beta 10.8.1 release notes

Publication source: [player-facing 10.8.1 notes](RELEASE_NOTES_10_8_1.md). Target title: **From Scratch | Friends Beta 10.8.1 — More Buybacks & Bank Search Fix**. Tag: `v0.10.8.1-buybacks-search-fix`. Final publication requires the complete local regression, exact-commit Windows/Ubuntu CI and final committed-source installer checks. Live game acceptance remains an owner-only check.

## Historical 10.8 release notes

Publication source for v0.10.8-bug-fixes-buybacks. Final publication is gated on the validation in [Milestone 10.8](MILESTONE_10_8.md).

## Your bank is full. Your adventure starts at zero.

**Friends Beta 10.8 is here.**

Your next adventure just got smoother.

- **Search your Virtual Bank** without losing your From Scratch view.
- **Choose your Slayer gear.** Unlock one exact Black Mask or Slayer Helmet copy for 1M virtual GP, including supported cosmetic and imbued versions. Each additional copy requires its own purchase.
- **Reclaim your Elite Black Knight armour** with individual 30K GP buybacks for each helm, body or pair of legs.

Your skills, quests and achievements remain yours. Your item progression starts fresh.

**Download FromScratch-Beta-Setup.exe below and continue your adventure.** Windows 64-bit. Java is included.

Existing local progression is preserved when upgrading. Export a backup before updating; older betas cannot read the newer save format.

From Scratch is a Friends Beta and is not yet available on the official RuneLite Plugin Hub. The installer is unsigned. Authentication uses RuneLite's official sign-in flow.

---

## Historical 10.6 draft

# From Scratch: Windows beta v0.10.6-friends-installer

**LOCAL DRAFT — not published, distributed or Plugin Hub approved.** Upload only after the owner's consolidated acceptance. The actual future release URL is not known yet.

**Your bank is full. Your adventure starts at zero.**

Rediscover the thrill of starting with nothing without abandoning your existing OSRS character. Your real items stay put; From Scratch tracks a separate, signed bank challenge as you rebuild.

## In this beta

- Native bank overlays: gray physical counts, bright configurable virtual counts and shaded legacy items.
- Interactive Virtual Bank with native tabs, ordinary banking and a single Full Bank setting.
- Exact signed quantities and buyback status following normal Examine.
- Categorized fixed-price legacy buybacks, one-time Rune/Divine Pouch and open/closed Looting Bag families, with verified old-contents offsets.
- Finalized GE purchases/sales, item history, Peak Progress Value and host Time Played.
- Character-specific local saves, recovery safeguards and explicit Export/Import Progress.

Gameplay is frozen at the accepted M9.2 source baseline; this milestone prepares distribution and documentation.

## Installation

Windows 10/11 x64. Download the approved **FromScratch-Beta-Setup.exe** and checksum, double-click the setup file and follow the wizard. Java is included. Open **From Scratch Beta** from the desktop or Start Menu and use RuneLite's official **Jagex Accounts** panel to sign in. Enable From Scratch and follow Before Starting. No terminal or developer tools are needed. See [installation](INSTALLATION.md) and [beta guide](BETA_TESTING.md). This unsigned candidate still requires actual launch/login acceptance before distribution.

Proposed assets (already prepared locally, not uploaded):

- `FromScratch-Beta-Setup.exe`
- `FromScratch-Beta-Setup.exe.sha256`

`installer-files.json` records the installed image's files/hashes; `installer-build.json` and the embedded `SOURCE.json`/`native-build.json` record the baseline commit, frozen gameplay tree and uncommitted packaging inputs. Do not describe the current candidate as a fully committed release. Regenerate after any approved release changes.

## Important limitations

This is an independent, voluntary, unreviewed challenge. It does not prevent withdrawals or change real ownership. A mixed stack still exposes the full physical amount. Bank-only observation cannot reconstruct unseen activity; host Time Played excludes mobile/other clients. Price/tax values are estimates where labeled. Search/other Bank Tags may return to Full Bank. Unknown storage contents block buyback rather than being assumed empty.

**Progress is local to this computer.** Exports contain private, unencrypted progression. Back up before updates, keep exports private, and avoid concurrent clients on the same run. No cloud/device sync. Update with the newer approved setup EXE; uninstall through Windows Settings > Installed apps. Challenge saves, settings, credentials, backups, launcher logs and unrelated files remain. The earlier v0.10.5 ZIP is not replaced or deleted.

Fourteen native packaging checks passed locally, including private-runtime bootstrap help, actual isolated setup/upgrade/reinstall, shortcut/Installed apps registration and safe uninstall. These checks did not start the game or sign in. Windows and Ubuntu CI passed for the frozen source. Actual wizard visuals, normal shortcut launch and authentication remain owner acceptance gates; see [Milestone 10.6](MILESTONE_10_6.md). Do not present these as passed until verified.

## Reporting a problem

Use the [repository issue form](https://github.com/PilotsRKwl/fromScratch/issues/new/choose) with beta/RuneLite/Java/Windows versions and concise reproduction steps. Redact character/account names, private chat, paths and progression. Never upload credentials, whole client folders or `.fsp` exports. The short template is in [BETA_TESTING.md](BETA_TESTING.md).

From Scratch by **Pilots r kwl**. [BSD-2-Clause](../LICENSE). Not affiliated with or endorsed by Jagex, RuneLite or Steam. Official Plugin Hub submission belongs to Milestone 11; no approval is claimed.
