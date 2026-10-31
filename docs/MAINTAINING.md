# Maintaining

Use [CONTRIBUTING](../CONTRIBUTING.md) for development changes and [the validation plan](technical/TEST_PLAN.md) for relevant tests. Production and UI previews have separate build boundaries.

## Source sets and dependencies

- `src/main` is the ordinary RuneLite plugin. The official Plugin Hub standard build compiles only this source.
- `src/test` contains headless behavior, persistence and historical migration fixtures. Old models remain test-only where those fixtures need them.
- `src/preview` renders actual Swing components with synthetic progress and an isolated empty home. It never starts RuneScape or reads a player profile.

Keep Java 11 compatibility, existing RuneLite-provided dependency boundaries, `build=standard`, package/config identifiers and BSD-2-Clause notices. The official identifier is `from-scratch`; challenge records use its scoped `progress` child directory. Files outside that child are not read, copied or removed. Build output, logs, sample homes and test progression stay under ignored `build/`.

## Validation commands

Use the existing wrapper and Java 11. During development run incremental compilation and directly affected test filters; use exact-commit Windows and Ubuntu GitHub Actions for the complete final regression rather than repeating it after documentation edits.

For normal RuneLite development, use `.\gradlew.bat run` (or `./gradlew run`). Authentication and gameplay checks remain owner-operated.

```powershell
.\gradlew.bat compileJava compileTestJava --console=plain
.\gradlew.bat test --tests "com.fromscratch.bank.NativeBankSearchLifecycleTest" --quiet --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Documentation.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-StandardBuild.ps1
git diff --check
```

The focused test above is an example for search changes, not a required unrelated test on every edit. `Test-StandardBuild.ps1` obtains the official standard template and current Hub client version, compiles only production source and records tooling/template evidence under `build/standard-check`. It does not submit or establish policy approval. Detailed logs belong under ignored build directories.

## Images and screenshots

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Prepare-UiPreviews.ps1
```

Preview capture uses unchanged production components and explicitly synthetic data; inspect generated PNGs for layout and truthful captions. The previews are not real RuneScape screenshots. Keep [artwork attribution](ARTWORK.md) and [capture provenance](SCREENSHOTS.md) accurate.

## Housekeeping

Use canonical links to `PilotsRKwl/fromScratch`, author **Pilots r kwl**, and no guessed future commit. Public issues should request concise redacted evidence, not credentials or private saves. The owner controls RuneLite maintainer communications; agents update only the authorized manifest and do not generate or post moderator replies. Release/submission and future approved v1.0.0 gates are in [Launch](LAUNCH.md); only RuneLite review/merge and verified listing availability can establish official publication.
