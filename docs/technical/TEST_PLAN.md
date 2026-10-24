# Validation plan

Milestone 10.7: Potion Storage and Overdraft Protection is an authorized gameplay extension awaiting owner acceptance. Earlier beta/distribution evidence remains in [MILESTONE_10.md](../MILESTONE_10.md); historical results stay in the [archive](../history/TEST_PLAN_PRE_M10.md).

## Current verified baseline

GitHub Actions [run 39868207087](https://github.com/PilotsRKwl/fromScratch/actions/runs/39868207087) at `2bb5dbd4cb7973a05b9f449be70e5adcc0101d06` passed both `windows-latest` and `ubuntu-latest` for the earlier baseline. This is historical evidence, not hosted validation of the new 10.7 source.

Documentation, screenshot and isolated preview-tool changes do not justify another full gameplay regression. Compile the preview source and execute its explicit capture task; run documentation/asset checks and the isolated beta integration script. Do not interact with RuneScape through automation or touch actual progression during tests.

For 10.7, `bank/PotionStorageTest` exercises supported observation freshness, dose identity, unfinished units, simultaneous/delayed transfers, unknown stock, positive/zero/negative overdrafts, dose remainders, repayments, failed saves, persistent sidebar entries and actual export/reload. Existing bank, overlay, buyback, GE, wealth and persistence fixtures remain in use. Historical schema-generating fixtures explicitly remove the new field before backdating their envelope, keeping strict production decoding. The initial implementation already had its single complete checkpoint. This UI overhaul uses focused setup/storage/overlay/correction and affected pipeline tests; it retains format 10. Do not repeat the full suite for this pass. [Current results and manual steps](../MILESTONE_10_7.md).

## Maintainer checks

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Prepare-UiPreviews.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Documentation.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Build-Beta.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Beta.ps1
git diff --check
```

Preview rendering uses unchanged production Swing components, explicit sample data, an isolated empty user home and official cached item sprites. It does not open the game. Visually inspect PNGs for clipping, missing text and accurate captions. Native bank overlays and gameplay require owner captures.

Beta tests use disposable paths with spaces and a fake desktop. They verify manifest/hash allowlists, script parsing, prerequisites/classpath, actual shortcut creation in the isolated directory, repeated install, update, safe uninstall, tamper rejection and an isolated launcher help smoke. They do not authenticate or validate live gameplay. Never substitute real profile paths in failure-injection tests.

`scripts/Test-StandardBuild.ps1` checks the official pinned standard template with production source only. Reuse the documented valid result when production source and standard-build inputs are unchanged. This is not Plugin Hub approval or hosted scanning.

## If production changes are later authorized

Use Java 11 and incremental `compileJava compileTestJava`. Discover and filter only affected test classes during iteration. Preserve coverage for signed deltas, immutable baselines, complete-bank observation, atomic/replayed buybacks, contents offsets, GE lifecycle/partial fills, crash recovery, character isolation, native-tab intersections and re-entrant shutdown. Run the full regression once at a changed release checkpoint, rather than after every edit. Do not weaken tests or disable either CI platform.

## Owner acceptance

Follow [BETA_TESTING.md](../BETA_TESTING.md) after the [authoritative installation guide](../INSTALLATION.md). Check the actual desktop shortcut, supported Jagex login, before-start full-bank snapshot, active Virtual Bank/native tabs, gray/bright quantities, completed GE history, buybacks and restart persistence. Use disposable progression for destructive restore/reset experiments; an ordinary uninstall must preserve real saves/settings/backups.

The owner alone operates the game. A compile, unit fixture, help response or logged-out startup is not live acceptance. Launch/authentication and actual gameplay remain pending until explicitly confirmed.
