# Validation plan

The owner reports existing gameplay works. The first-start and storage change has not yet received new in-game acceptance; the targeted owner checks below remain pending. Automated fixtures do not establish in-game acceptance, authentication or native UI focus. Only the owner operates RuneScape; no computer-use gameplay or synthetic input is permitted.

## Development checks

Use incremental Java 11 compilation and directly affected test filters. Do not repeatedly run the complete local suite. The existing GitHub Actions workflow runs `gradlew build` on Windows and Ubuntu; both must pass for the exact source commit submitted to Plugin Hub. Never suppress failures, skip broken tests or weaken real assertions.

```powershell
.\gradlew.bat test --tests "com.fromscratch.bank.VirtualBankModeTest" --tests "com.fromscratch.bank.NativeBankSearchLifecycleTest" --quiet --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Documentation.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-StandardBuild.ps1
git diff --check
```

`Test-StandardBuild.ps1` obtains the current official Plugin Hub build/settings template and Hub client version, compiles only `src/main`, and records evidence under ignored `build/standard-check`. It also extracts the current packager identifier rule, checks the compiled descriptor against `from-scratch`, checks metadata/Java bytecode and inspects JAR resources and development-class isolation. It fails when compilation succeeds but identifier compatibility does not. This local preflight does not execute the hosted packager or establish security/policy approval or live testing. The production descriptor and intended manifest identifier are both `from-scratch`. The production artifact contains only the plugin; development and test tools remain outside it.

## Focused coverage

| Area | Fixtures and guarantees |
|---|---|
| Virtual Bank lifecycle | `VirtualBankModeTest`: complete-bank readiness, saved ON/OFF, committed membership, native tabs, independent/saved tags, stale temporary selections, public-service cleanup, re-entry, failed cleanup and Potion Storage transitions. Bank Tags owns its configuration; no direct cross-plugin config setter. |
| Native search | `NativeBankSearchLifecycleTest`: actual mode callbacks with bounded deferred queues and the pinned native input-closing branch; typed synthetic text, query/ownership intersection, repeated open/clear/close, empty bank/no matches, submitted text, tabs, Potion Storage, Full Bank, preference and durable balance stability. It failed before the search fix. The actual game interpreter/input focus remains owner-only verification. |
| Historical saves | `ExpandedBuybackPersistenceTest`, `SelectedBuybackPersistenceTest`, `StorageBuybackPersistenceTest` and existing repository/backup fixtures retain format-10 purchases, format-11 selected-copy evidence and storage declarations, without rewriting original bytes on load. Exercise continuation, failed publication/retry, restart and isolated export/import. |
| Buybacks | Model, pipeline and UI fixtures cover exact fixed prices, natural versus paid copies, complete sets, approved variants, GP limits, UUID duplication, quiver upgrades, declaration/native evidence, cancellation and failed saves. |
| Resources | `FoundationResourcesTest` verifies classpath PNG loading, dimensions, metadata and branding. Standard artifact inspection excludes previews/tests and Plugin service registration. |
| Directory capability boundary | Scoped Filepath fixtures verify that production roots cannot reach their parent or sibling. `ProgressStorageTest` verifies the fixed official child location and untouched unrelated files. |
| Official startup | `ProgressStorageTest` and `InitialSetupGuidanceTest` cover normal first setup without an extra acknowledgement, shared economic/playtime storage, restart persistence, preserved files and storage failure handling. |
| Broader progression | Final CI retains bank/potion signed deltas and doses, overlays/counters, overdrafts, GE attribution/history, corrections, wealth/time, migrations, backups/reset and character isolation tests. |

Fixtures use disposable synthetic characters and Filepath roots. Never substitute real profiles, progression or backup paths. Preserve migration fixtures even when historical production controllers are no longer used.

## Owner checks for first challenge and storage

Run `./gradlew run` (Windows: `.\gradlew.bat run`) only when the owner is ready to test. Use RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions; agents must never authenticate or operate the game.

Use a disposable challenge for these checks and keep valuable progression untouched:

1. With an empty official `from-scratch/progress/` location, open From Scratch. **Before Starting** appears directly, without an installation acknowledgement dialog.
2. Bank inventory and equipment, capture the complete normal bank and supported Potion Storage or explicit unavailable-unlock answer, then confirm the challenge rules. Filtering activates only after Start is successfully saved.
3. Restart RuneLite and confirm that the same official run, balances, history and host Time Played resume.
4. Cancel **Reset Progress** and confirm that nothing changes. On the disposable run, confirm Reset with its required typed confirmation, verify the recovery archive, and start another run through normal setup.
5. Confirm that unrelated files outside the official `progress` location are untouched. Do not open, transfer, delete or reset valuable data to demonstrate this.

Report owner results explicitly; passing compilation/headless tests is not live acceptance.

## Owner checks for native integration

Existing gameplay has owner-reported acceptance. For meaningful view integration changes, check Virtual Bank close/reopen and client restart with preferences ON/OFF; native search opening/typing/clearing/reopening; native tabs, independent Bank Tags and Potion Storage; gray physical/bright signed virtual counters; and unchanged balances/history. The configuration-ownership cleanup preserves gameplay/accounting and delegates existing cleanup through the public API. A native regression smoke check is recommended; no new gameplay feature is introduced. Headless tests cannot prove native focus/rendering on every client scale.

Use [manual gameplay checks](../MANUAL_TESTING.md), [installation](../INSTALLATION.md) and RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions. Destructive import/reset experiments require disposable progression or a private backup.

## Submission gate

Finish documentation/metadata/security review, focused tests and the official standard build. Commit and push; verify both CI platforms on that exact full SHA. Update only the manifest source SHA in existing PR #19824, preserving the re-enablement request. Keep native script/widget/service coupling documented in the [reviewer map](../PLUGIN_HUB_REVIEW.md). The owner controls all maintainer communications; agents must not generate or post moderator replies. Human review/merge and verified Hub availability precede v1.0.0 publication; see the [launch plan](../LAUNCH.md).
