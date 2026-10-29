# Validation plan

The owner reports the published Friends Beta gameplay works. Automated fixtures do not establish in-game acceptance, authentication or native UI focus. Only the owner operates RuneScape; no computer-use gameplay or synthetic input is permitted.

## Development checks

Use incremental Java 11 compilation and directly affected test filters. Do not repeatedly run the complete local suite. The existing GitHub Actions workflow runs `gradlew build` on Windows and Ubuntu; both must pass for the exact source commit submitted to Plugin Hub. Never suppress failures, skip broken tests or weaken real assertions.

```powershell
.\gradlew.bat test --tests "com.fromscratch.bank.VirtualBankModeTest" --tests "com.fromscratch.bank.NativeBankSearchLifecycleTest" --quiet --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Documentation.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-StandardBuild.ps1
git diff --check
```

`Test-StandardBuild.ps1` obtains the current official Plugin Hub build/settings template and Hub client version, compiles only `src/main`, and records tooling/template evidence under ignored `build/standard-check`. Inspect the resulting JAR for resources and beta/preview/test isolation. This is build compatibility, not policy approval or live testing.

## Focused coverage

| Area | Fixtures and guarantees |
|---|---|
| Virtual Bank lifecycle | `VirtualBankModeTest`: complete-bank readiness, saved ON/OFF, committed membership, native tabs, independent/saved tags, stale temporary selections, public-service cleanup, re-entry, failed cleanup and Potion Storage transitions. Bank Tags owns its configuration; no direct cross-plugin config setter. |
| Native search | `NativeBankSearchLifecycleTest`: actual mode callbacks with bounded deferred queues and the pinned native input-closing branch; typed synthetic text, query/ownership intersection, repeated open/clear/close, empty bank/no matches, submitted text, tabs, Potion Storage, Full Bank, preference and durable balance stability. It failed before the search fix. The actual game interpreter/input focus remains owner-only verification. |
| Historical saves | `ExpandedBuybackPersistenceTest`, `SelectedBuybackPersistenceTest`, `StorageBuybackPersistenceTest` and existing repository/backup fixtures retain 10.7 format-10 purchases, 10.8 selected-copy evidence and 10.8.1 declarations, without rewriting original bytes on load. Exercise continuation, failed publication/retry, restart and isolated export/import. |
| Buybacks | Model, pipeline and UI fixtures cover exact fixed prices, natural versus paid copies, complete sets, approved variants, GP limits, UUID duplication, quiver upgrades, declaration/native evidence, cancellation and failed saves. |
| Resources | `FoundationResourcesTest` verifies classpath PNG loading, dimensions, metadata and branding. Standard artifact inspection excludes beta/preview/bootstrap/tests and Plugin service registration. |
| Broader progression | Final CI retains bank/potion signed deltas and doses, overlays/counters, overdrafts, GE attribution/history, corrections, wealth/time, migrations, backups/reset and character isolation tests. |

Fixtures use disposable synthetic characters and Filepath roots. Never substitute real profiles, progression or backup paths. Preserve migration fixtures even when historical production controllers are no longer used.

## Owner checks for native integration

After a meaningful view integration change, check Virtual Bank close/reopen and client restart with preferences ON/OFF; native search opening/typing/clearing/reopening; native tabs, independent Bank Tags and Potion Storage; gray physical/bright signed virtual counters; and unchanged balances/history. The configuration-ownership cleanup preserves gameplay/accounting and delegates existing cleanup through the public API. A native regression smoke check is recommended; no new gameplay feature is introduced. Headless tests cannot prove native focus/rendering on every client scale.

Use [beta checks](../BETA_TESTING.md), [installation](../INSTALLATION.md) and RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions. Destructive import/reset experiments require disposable progression or a private backup. Optional native installer execution checks remain separately deferred because local Windows Application Control blocked Setup; do not count them as passing.

## Submission gate

Finish documentation/metadata/security review, focused tests and the official standard build. Commit and push; verify both CI platforms on that exact full SHA. Submit only the required Hub manifest after name/PR conflict checks and resolution of material compliance issues. Disclose native script/widget/service coupling in the [reviewer map](../PLUGIN_HUB_REVIEW.md). Human review/merge and verified Hub availability precede v1.0.0 publication; see the [launch plan](../LAUNCH.md).
