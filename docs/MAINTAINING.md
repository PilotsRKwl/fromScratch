# Maintaining

Use [CONTRIBUTING](../CONTRIBUTING.md) for development changes and [the validation plan](technical/TEST_PLAN.md) for relevant tests. Production, UI previews and offline progression migration have separate build boundaries.

## Source sets and dependencies

- `src/main` is the ordinary RuneLite plugin. The official Plugin Hub standard build compiles only this source.
- `src/test` contains headless behavior, persistence and historical migration fixtures. Old models remain test-only where those fixtures need them.
- `src/preview` renders actual Swing components with synthetic progress and an isolated empty home. It never starts RuneScape or reads a player profile.
- `src/migration` is a separately executed, Java-standard-library offline transfer utility. Its disposable tests live in `src/migrationTest`; neither source set enters the standard plugin artifact.

Keep Java 11 compatibility, existing RuneLite-provided dependency boundaries, `build=standard`, package/config identifiers and BSD-2-Clause notices. The official identifier and scoped directory are `from-scratch`; legacy progression in `from_scratch` remains preserved separately. Build output, logs, sample homes and test progression stay under ignored `build/`.

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

## Offline progression migration

The utility transfers legacy progression from `.runelite/plugin-data/from_scratch/` to `.runelite/plugin-data/from-scratch/`. The production plugin uses the official `from-scratch` identifier and RuneLite's normal scoped directory. Its shared storage gate must succeed before either economic progression or playtime accesses that directory. Validate the transition with a disposable copy and retain a private backup before transferring a real run; automated checks do not establish owner acceptance of the full workflow.

Players with progression in the legacy directory must close every client and complete the utility before loading the production plugin. The plugin cannot inspect the legacy sibling directory and cannot automatically detect unmigrated legacy progression. A genuinely new installation instead requires the explicit first-run new-installation confirmation, followed by normal bank setup. That confirmation is available only for an empty canonical directory; it cannot replace missing/invalid migration evidence beside existing data. A migration receipt or the recorded fresh-install decision permits subsequent startup; incomplete/conflicting evidence blocks storage rather than creating a new challenge.

After transferring, use only the canonical progression tree for that challenge. Later changes to the preserved old tree neither synchronize nor merge into the canonical history.

Build and test only the offline utility:

```powershell
.\gradlew.bat migrationTest migrationJar --console=plain
java -jar build/libs/from-scratch-migration.jar --dry-run
java -jar build/libs/from-scratch-migration.jar
```

Java 11 or later is required. The default base is the current user's `.runelite/plugin-data` directory. An alternate RuneLite home can be selected explicitly with `--plugin-data <directory>`; the source and destination remain the two fixed child names. The utility has no network or telemetry and does not request account credentials or authenticate. Process metadata is inspected locally only to refuse active clients; process arguments are not recorded or transmitted.

Close **all RuneLite processes and From Scratch project tools**, including other accounts, before either command, and leave them closed until the utility exits. Normal execution asks for the exact confirmation `MIGRATE FROM SCRATCH`; there is no unattended `--yes` option. A dry run inventories and checks the proposed transfer without creating destination, staging or completion data. Process inspection is an additional safeguard, not a substitute for the offline confirmation; unavailable or ambiguous inspection can require stopping and checking the local environment.

Migration copies opaque file contents and the complete directory structure, including every character's journals, histories, archives, backups, incomplete recovery/import evidence and playtime. It does not repair or reinterpret save schemas. SHA-256 checks verify copies before completion. Existing identical destination files can be retained; conflicting files are refused rather than overwritten. Empty operational `run.lock` and `playtime.lock` files are held as locks where present and omitted from the copy; nonempty reserved lock files are refused. Original file bytes and modification times are not intentionally changed or deleted; operating-system last-access timestamps can change when files are read.

The sibling `.from-scratch-migration` staging directory and its manifest support interrupted-transfer retries. Preserve them after a failure, resolve the reported cause, then rerun the same command. Verified staged files and the completion receipt are published through atomic, no-clobber hard links on the same volume. Hard-link support is required, as on ordinary Windows NTFS and Linux filesystems; an unsupported filesystem fails safely without claiming completion. Staging is retained transaction evidence and can share file contents with the destination; it is **not an independent backup**. The utility does not hard-link migrated files to the original legacy files.

The destination's `.from-scratch-migration-complete` receipt records successful transfer and is read inside the plugin's scoped directory. It contains the `FROM_SCRATCH_OFFLINE_MIGRATION_V1` header and the migration manifest's SHA-256 digest; it is not a continually recomputed checksum of legitimate later progression. That name is reserved migration metadata; a legacy collision is refused before copying. The staging directory and utility transaction lock remain siblings outside the destination. After completion the destination is authoritative: subsequent execution must not reimport stale legacy files, repair canonical data from staging or replace newer canonical progress with the original snapshot. Keep the original directory and independent private backups. Do not post manifests, saves or detailed local paths in public issues.

Symlinks, junctions/reparse points and unsupported filesystem objects are rejected, including existing ancestors of the selected location. Java provider behavior differs across platforms; headless disposable tests do not establish native link behavior on every filesystem or prove safety against an actively hostile process modifying paths concurrently. The utility requires an offline, locally controlled tree. No real-player migration, authentication, installation or in-game acceptance is implied by automated tests.

## Images and screenshots

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Prepare-UiPreviews.ps1
```

Preview capture uses unchanged production components and explicitly synthetic data; inspect generated PNGs for layout and truthful captions. The previews are not real RuneScape screenshots. Keep [artwork attribution](ARTWORK.md) and [capture provenance](SCREENSHOTS.md) accurate.

## Housekeeping

Use canonical links to `PilotsRKwl/fromScratch`, author **Pilots r kwl**, and no guessed future commit. Public issues should request concise redacted evidence, not credentials or private saves. Apply repository descriptions/topics only when authorized. Release/submission and future approved v1.0.0 gates are in [Launch](LAUNCH.md); only RuneLite review/merge and verified listing availability can establish official publication.
