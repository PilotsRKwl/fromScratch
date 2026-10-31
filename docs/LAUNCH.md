# Plugin Hub submission and v1.0.0 launch

## Current status

The initial [Plugin Hub submission #19721](https://github.com/runelite/plugin-hub/pull/19721) was merged. RuneLite maintainer abextm then [disabled From Scratch](https://github.com/runelite/plugin-hub/commit/58bc2829c4738785fe3d674c356802a09aace07d) with `disabled=disallowed readme`. **Distribution is temporarily paused pending review.** Review corrections are tracked in the existing [re-enablement PR #19824](https://github.com/runelite/plugin-hub/pull/19824).

Re-enablement requires an explicit maintainer decision. Local validation, CI and repository corrections do not establish approval, endorsement or current availability. Production gameplay and progression compatibility remain unchanged. No v1.0.0 will be published before re-enablement and verified availability.

## Before a source update submission

1. Complete the current official-rule audit and keep the [reviewer map](PLUGIN_HUB_REVIEW.md) matched to source. Disclose native bank rendering, filtering, Bank Tags/config interactions, script callbacks, title updates and supplemental Examine behavior. A supported public API alone does not prove policy compliance.
2. Resolve any published-rule violation through a compliant alternative. If a material policy uncertainty or major gameplay redesign remains, stop before submitting and report the precise concern; do not conceal it or remove defining functionality silently.
3. Preserve progression, identifiers, migration fixtures, receipts, backups and reset archives. Check the production JAR independently of preview/test/offline utility source sets.
4. Run relevant focused checks and official standard-build compatibility for production changes, commit/push the final source, then require Windows and Ubuntu CI success on that exact full SHA. Documentation-only corrections use documentation/link and metadata checks; existing production build evidence remains valid. Compilation/headless fixtures do not prove in-game behavior. The owner reports successful migration of existing progression and completed manual acceptance of the production candidate; future material production changes need their own owner checks.
5. Check for an existing name/manifest/submission. Use one dedicated branch in a fork of `runelite/plugin-hub`; keep the upstream change limited to the required manifest and anything its current instructions explicitly require.

The submission manifest is `plugins/from-scratch` and contains `repository=https://github.com/PilotsRKwl/fromScratch.git` plus the **actual verified 40-character source SHA**. The official descriptor/internal storage identifier is `from-scratch`; legacy progression used `from_scratch`. The [implemented offline migration and production startup gate](PLUGIN_HUB_REVIEW.md#identifier-and-offline-migration-gate) preserve existing migrated progression and require explicit confirmation for genuinely new installations. The utility remains outside the official standard build, preserves the original legacy tree, and does not synchronize old and new histories.

For this re-enablement request, finish documentation validation, commit/push the source and require successful Windows and Ubuntu CI on that exact SHA. Update the existing re-enablement PR #19824, change only `plugins/from-scratch` and explicitly propose removal of `disabled=disallowed readme` for maintainer approval. Do not bypass the restriction or open duplicate PRs. Never use a guessed future commit. Include links to the original submission, disable commit, source correction commit and successful CI, plus the truthful footer `Generated-by: OpenAI Codex`. Human review and merge remain outside our control.

## During review

Keep one submission PR. Inspect automated build/security results and reviewer feedback. For necessary source changes, use focused validation, commit/push, require exact-commit CI and update the manifest in that same PR. Report human review as pending until it is actually accepted. Stop for a policy decision or major product redesign rather than unilaterally replacing the game mode.

No public v1.0.0 release, approval badge or Plugin Hub availability claim belongs before actual upstream merge and verified installation availability.

## After approval and verified availability

- Keep normal RuneLite Plugin Hub installation first in README/INSTALLATION.
- Replace pending-review language with the verified listing link, update metadata to v1.0.0 as appropriate and record the approved source in the changelog.
- Prepare one new immutable v1.0.0 tag/release using the appropriate verified commit. Existing releases/tags/assets require separate owner authorization for deletion; do not rewrite history.
- Recheck repository/listing author, description, tags, icon, links, license/notices and privacy statements. Publish only after the actual approval boundary has been met.

## Prepared launch copy

**From Scratch | Your Adventure Starts Here**

Restart item progression without restarting your account. Keep your character, achievements and old bank. Start earning useful items and wealth again, reclaim selected legacy gear, play solo or alongside friends, and reset whenever you want a new beginning.

**A fresh start without a fresh account.**

Keep the public story about the game mode, with technical details in the reviewer/developer guides. Explain that rules are voluntary and progression stays local; do not imply a shared bank, multiplayer synchronization or an integrated leaderboard.

## Repository presentation

Suggested concise description: **Start fresh item progression on your existing OSRS character. Track a Virtual Bank, reclaim legacy gear, and play solo or alongside friends.**

Useful topics: `runelite-plugin`, `old-school-runescape`, `osrs`, `runelite`, `challenge`, `bank`, `progression`, `java`. Keep the existing original branding and author **Pilots r kwl**. Use a verified current release/listing URL rather than inventing an approval or launch link. [Artwork and screenshot attribution](ARTWORK.md) remains part of the release record.
