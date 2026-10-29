# Maintaining and packaging

Use [CONTRIBUTING](../CONTRIBUTING.md) for development changes and [the validation plan](technical/TEST_PLAN.md) for relevant tests. The standard plugin and standalone beta have separate build boundaries.

## Source sets and dependencies

- `src/main` is the ordinary RuneLite plugin. The official Plugin Hub standard build compiles this source independently of the beta launcher.
- `src/test` contains headless behavior, persistence and historical migration fixtures. Old models remain test-only where those fixtures need them.
- `src/beta` and `beta/windows-native` are owner-side standalone distribution tools.
- `src/preview` renders actual Swing components with synthetic progress and an isolated empty home. It never starts RuneScape or reads a player profile.

`gradle/beta.gradle` stages `beta/windows`; its `Common.ps1` is also consumed by the native build. The script install/launch/uninstall helpers still have packaging and integration-test consumers. Do not delete them merely because the native EXE is now the player download. Neither optional source set enters the production plugin JAR.

Keep Java 11 compatibility, existing RuneLite-provided dependency boundaries, `build=standard`, package/config/storage identifiers and BSD-2-Clause notices. Build output, downloaded tools, runtimes, logs, sample homes and test progression stay under ignored `build/`.

## Validation commands

Use the existing wrapper and Java 11. During development run incremental compilation and directly affected test filters; use exact-commit Windows and Ubuntu GitHub Actions for the complete final regression rather than repeating it after documentation edits.

```powershell
.\gradlew.bat compileJava compileTestJava --console=plain
.\gradlew.bat test --tests "com.fromscratch.bank.NativeBankSearchLifecycleTest" --quiet --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Documentation.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-StandardBuild.ps1
git diff --check
```

The focused test above is an example for search changes, not a required unrelated test on every edit. `Test-StandardBuild.ps1` obtains the official standard template and current Hub client version, compiles only production source and records tooling/template evidence under `build/standard-check`. It does not submit or establish policy approval. Detailed logs belong under ignored build directories.

## Standalone beta distribution

The authoritative version/runtime/tool pins live in [toolchain.json](../beta/windows-native/toolchain.json). The existing native build reuses `Build-Beta.ps1`, stages allowlisted dependencies and licenses, builds the GUI launcher, verifies the private runtime and compiles Setup with pinned Inno Setup. Developer prerequisites are Windows x64, Java 11, the Windows .NET Framework compiler and an approved Inno Setup compiler. Players need none of these developer tools.

Use a new unique release label/package/Windows version for a new release. Preserve existing outputs, Git tags, releases and user data. Final installers require a clean committed source tree validated by CI; `LocalTestCandidate` is only for explicitly unpublished work and must never be distributed as a final release.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Build-NativeInstaller.ps1 -Offline
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-NativeInstaller.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Beta.ps1
```

Omit `-Offline` only when the approved dependency/toolchain downloads are required. An existing Inno compiler can be passed with `-InnoCompiler`. The build does not install developer tools or publish a release automatically. It refuses to overwrite an existing version's final installer.

Output is `build/distributions/<release-tag>/FromScratch-Beta-Setup.exe` and its `.sha256` sidecar. `installer-build.json`, `installer-files.json` and embedded `SOURCE.json`, `DEPENDENCIES.json`, `native-build.json`, `payload.json` and `release.json` record source/tree, package/release/Windows versions, dependency hashes and runtime/tool provenance. Validate the actual generated path, final-source status, executable metadata, payload hashes, runtime/legal notices and public download checksum. Compiler timestamps mean separate EXE builds are not promised byte-identical.

The native tests use marker-guarded disposable paths, fake desktop/Start Menu targets and a separate uninstall identity. `--verify-only` runs packaged bootstrap help with an empty isolated home, never authentication/gameplay. No test should use a real RuneLite profile or progression.

For published Friends Beta 10.8.1: **Deferred: blocked by local Windows Application Control. Manual installation, upgrade, and uninstall testing pending.** Static source/version/runtime/manifests and public asset hashes were checked; Setup execution was not declared passing. Preserve this distinction and never disable security policy to finish a test.

## Images and screenshots

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Prepare-UiPreviews.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Prepare-Marketing.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Prepare-WindowsIcon.ps1
```

Preview capture uses unchanged production components and explicitly synthetic data; inspect generated PNGs for layout and truthful captions. Marketing reproduces the original hero SVG/PNG, and WindowsIcon derives the ICO from the root icon. None produces a real RuneScape screenshot. Keep [artwork attribution](ARTWORK.md) and [capture provenance](SCREENSHOTS.md) accurate.

## Housekeeping

Use canonical links to `PilotsRKwl/fromScratch`, author **Pilots r kwl**, and no guessed future commit. Public issues should request concise redacted evidence, not credentials or private saves. Apply repository descriptions/topics only when authorized. Release/submission and future approved v1.0.0 gates are in [Launch](LAUNCH.md); only RuneLite review/merge and verified listing availability can establish official publication.
