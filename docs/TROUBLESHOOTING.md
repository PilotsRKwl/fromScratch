# Troubleshooting and reporting problems

Keep a private **Export Progress** backup before updates, restoration or reset. Do not delete progression files to resolve a display or loading problem.

## Windows blocks the beta installer

Friends Beta 10.8.1 is unsigned. Windows Application Control blocked execution on the release machine. **Manual installation, upgrade and uninstall testing is pending.** Static package, source, runtime and checksum checks do not establish that those execution tests passed.

Keep Windows security protections enabled. Do not disable Smart App Control, Defender or organizational policies to run the installer. Use your normal approved software process or ask the device administrator if applicable. A checksum verifies downloaded bytes; it does not provide a trusted publisher signature.

Use only the [published release and its checksum](https://github.com/PilotsRKwl/fromScratch/releases/tag/v0.10.8.1-buybacks-search-fix). See [installation](INSTALLATION.md) for the supported login and update flow. Plugin Hub availability is still pending; no official approval is claimed.

## The application cannot start or sign in

The beta launcher checks its bundled runtime and libraries before launching RuneLite. Missing or damaged files can prevent startup. Preserve your progression and note the exact error before reinstalling a trusted release through the normal approved installation process.

Sign in through RuneLite's **Jagex Accounts** panel using its [official instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). From Scratch does not ask for passwords or tokens. Never send an authentication panel, credential file or account export with a bug report.

## Start From Scratch is unavailable

Bank your inventory and equipment yourself, open the normal bank, and complete the **Before Starting** confirmation. A bank PIN or unavailable/incomplete bank data prevents a usable capture. Wait for the bank to be recorded rather than assuming missing data is an empty bank.

If Potion Storage is unlocked, follow its capture instructions. An unknown unlock or contents state needs the explicit setup answer or review shown by the panel. Reopening an old dialog after logout or changing characters cannot reuse another setup's evidence.

Do not start a new run to repair a failed load. Existing progression and recovery files must be preserved.

## Virtual Bank is empty or shows unexpected items

A new run starts with zero challenge ownership, so an empty Virtual Bank is expected. Bank newly earned items, then check the saved virtual quantities. Native tabs and the search query also narrow the results.

Turn off **Enable Virtual Bank View** in From Scratch's settings to inspect Full Bank. This changes presentation, not ownership. Record whether the issue occurs with Virtual Bank enabled, in Full Bank, on a particular native tab, during search, or after visiting Potion Storage.

Do not use Manual Bank Corrections merely to make a filtered item appear. First establish the correct ownership and physical quantity. Search or tab changes must never alter balances.

## Negative quantities or storage uncertainty

Negative challenge quantities are valid. They can represent legacy items withdrawn beyond your allowance or linked legacy contents offsets. Returning or replacing the corresponding stock normally clears the outstanding amount; the plugin does not prevent withdrawals.

Potion Storage uses doses rather than treating every bottle as an independent earned potion. Open storage when instructed to refresh supported evidence. Unverified contents are not automatically zero. Use **Manual Bank Corrections** only for a known correction or explicit supported storage review, and preserve earned stock and existing debt.

If you cannot explain a change, export a private backup and report the steps. Do not repeatedly reset, import or make speculative corrections against a valuable run.

## Progress will not load, save or import

Preserve current files, backups and recovery evidence. A failed load does not authorize a blank run. Close other clients that could be editing the same character, then use **Retry Loading** when appropriate.

Import accepts a compatible same-character backup after preview and confirmation. It replaces the selected run rather than merging histories. An older plugin may reject newer save evidence; do not downgrade against your only copy. Never attach saves or `.fsp` exports to a public issue.

For device transfers, use Export / Import Progress and keep one computer as the main copy. There is no automatic cloud sync. Mobile changes reconcile as the next observed net bank change; unseen trades and mobile playtime may be unavailable.

## Report a reproducible problem

Open a [bug report](https://github.com/PilotsRKwl/fromScratch/issues/new?template=bug_report.md) with:

- Plugin/beta and RuneLite versions, operating system and UI scale.
- Whether this concerns installation, setup, banking, search, buybacks, storage, history or backups.
- Minimal steps, expected result and actual result.
- Whether restarting, Full Bank or a particular native tab changes the result.
- A small cropped screenshot or short relevant redacted error excerpt, if useful.

Remove account/character identifiers, credentials, private paths, chat and authentication context. **Never upload saves, `.fsp` exports, whole logs, entire configuration folders, Jagex account data or tokens.** Logs and progression are private even when their filenames appear harmless.

```text
Plugin/beta version:
RuneLite version / OS / UI scale:
What I did:
What I expected:
What happened:
Does restart or Full Bank change it?
Relevant cropped screenshot or short redacted error excerpt:
```

For a freeze, note the last action and time. Do not keep reproducing it against valuable progression. Maintainers may request a narrowly scoped diagnostic; review and redact it before sharing.

[Gameplay guide](GAMEPLAY.md) · [Feature reference](FEATURES.md) · [Data and privacy](DATA_AND_PRIVACY.md)
