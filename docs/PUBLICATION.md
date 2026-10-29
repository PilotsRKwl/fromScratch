# Publication preparation

Friends Beta 10.8 is published. The owner authorized Milestone 10.8.1 source commit, push, exact-commit CI, installer packaging and GitHub pre-release publication once validation passes. **Milestone 11 exclusively owns official Plugin Hub submission/review/publication.** See the [10.8.1 release notes](RELEASE_NOTES_10_8_1.md) and [10.8 baseline](MILESTONE_10_8.md).

## Current release gates

1. Complete regression passes without skipped/disabled failures; source/config/documentation reviewed and private/generated files excluded.
2. Ordinary non-force master push; both Windows and Ubuntu CI pass for the exact release SHA.
3. Clean-source native installer has matching source/tree/version manifests, pinned private Java, payload hashes and safe isolated install/upgrade/uninstall validation.
4. New unique `v0.10.8.1-buybacks-search-fix` tag; public pre-release with EXE/checksum. Public downloads must hash-match the validated local installer. Preserve previous releases and assets.
5. Keep issues open for owner gameplay verification. No automatic game input or authentication. No Plugin Hub submission.

## Historical Milestone 10 preparation record

The sections below retain the earlier preparation chronology and original evidence. They do not describe the current beta's availability or authorize separate Hub submission.

## Prepared locally

- Canonical repository [PilotsRKwl/fromScratch](https://github.com/PilotsRKwl/fromScratch); `master` revision `2bb5dbd4cb7973a05b9f449be70e5adcc0101d06`, with the accepted M9.2 production tree unchanged. Older milestone tags have not been moved.
- From Scratch / **Pilots r kwl** / `com.fromscratch` / `fromscratch` / `from_scratch`, Java 11, BSD-2-Clause, existing small PNG icons.
- Version `0.10.0-beta.1`, separate bootstrap and unmodified RuneLite 1.13.3 runtime JARs, manifest/checksums/provenance, installer/desktop shortcut/update/safe uninstall. See [installation](INSTALLATION.md).
- Promotional [README](../README.md), original title/social artwork, pending real-screenshot list, [release draft](RELEASE_DRAFT.md) and [metadata recommendations](REPOSITORY_METADATA.md).
- [Reviewer map](PLUGIN_HUB_REVIEW.md), [features](FEATURES.md), [privacy](DATA_AND_PRIVACY.md) and [beta checklist](BETA_TESTING.md). Runtime widget-display and filtering behavior is disclosed explicitly.
- Local official standard-template compile passed. This supports `build=standard` suitability; it is not hosted packaging/scanning or Plugin Hub approval.

## Owner release gates

1. Review [Milestone 10 evidence](MILESTONE_10.md); test actual beta graphical launch, official Jagex authentication and gameplay, preserving real progression. A help smoke alone is insufficient.
2. Both Windows and Ubuntu passed [hosted run 39868207087](https://github.com/PilotsRKwl/fromScratch/actions/runs/39868207087) at the current source revision. Gameplay is unchanged; documentation/preview tooling does not require another full gameplay regression. See [current evidence](MILESTONE_10.md#baseline-and-ci).
3. Review the new actual-component sample previews and rendered README. Capture/redact the remaining genuine [bank/gameplay images](SCREENSHOTS.md#owner-gameplay-capture-checklist).
4. Review the diff and private-data exclusions. After explicit approval to commit, record the real resulting revision, rebuild provenance/checksum and run only validation invalidated by changes. No tag rewriting.
5. Approve the GitHub Release/assets and About/topics separately. Use actual download links only after publication. A checksum is not a publisher signature. Friend distribution remains unauthorized until approved.
6. In Milestone 11, recheck current [Hub rules](https://github.com/runelite/plugin-hub), [review process](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) and [rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features), then obtain approval for the publishing PR.

## Future submission

The eventual `plugins/from_scratch` Hub marker must use the canonical Git URL and an **approved, existing immutable full commit**. None is prepared with a guessed future SHA. Root `runelite-plugin.properties` requests the standard build; the Hub replaces local Gradle configuration and compiles production source only. No service-provider registration file is included.

The eventual PR should disclose scoped physical-counter display flags, native Bank Tags/tab filtering, local informational chat, Filepath persistence/imports, the price feed, finite buyback offsets and beta/bootstrap separation. Include validation, limitations, artwork/license provenance and any attribution footer required by applicable instructions at submission time. No PR has been opened; review can request changes.
