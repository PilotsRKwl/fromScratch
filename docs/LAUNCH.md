# Plugin Hub submission and v1.0.0 launch

## Current status

The initial [Plugin Hub submission #19721](https://github.com/runelite/plugin-hub/pull/19721) was merged. RuneLite maintainer abextm then [disabled From Scratch](https://github.com/runelite/plugin-hub/commit/58bc2829c4738785fe3d674c356802a09aace07d) with `disabled=disallowed readme`. **Distribution is temporarily paused pending review.** Review corrections are tracked in the existing [re-enablement PR #19824](https://github.com/runelite/plugin-hub/pull/19824).

Re-enablement requires an explicit maintainer decision. Local validation, CI and repository corrections do not establish approval, endorsement or current availability. Gameplay and accounting remain unchanged. The official release uses a new challenge location while retaining ordinary save integrity, restart persistence and explicit reset safeguards. v1.0.0 will be the first official public release, after re-enablement and verified availability.

## Before a source update submission

1. Complete the current official-rule audit and keep the [reviewer map](PLUGIN_HUB_REVIEW.md) matched to source. Disclose native bank rendering, filtering, Bank Tags/config interactions, script callbacks, title updates and supplemental Examine behavior. A supported public API alone does not prove policy compliance.
2. Resolve any published-rule violation through a compliant alternative. If a material policy uncertainty or major gameplay redesign remains, stop before submitting and report the precise concern; do not conceal it or remove defining functionality silently.
3. Preserve progression, identifiers, migration fixtures, receipts, backups and reset archives. Check the production JAR independently of preview/test source sets.
4. Run relevant focused checks and official standard-build compatibility for production changes, commit/push the final source, then require Windows and Ubuntu CI success on that exact full SHA. Documentation-only corrections use documentation/link and metadata checks; existing production build evidence remains valid. Compilation/headless fixtures do not prove in-game behavior. The changed first-start and storage flow requires targeted owner acceptance; compilation and headless tests are not that acceptance.
5. Update the existing re-enablement PR #19824 through its dedicated fork branch. Keep the upstream change limited to the manifest; do not create a replacement PR or modify upstream master directly.

The submission manifest is `plugins/from-scratch` and contains `repository=https://github.com/PilotsRKwl/fromScratch.git` plus the **actual verified 40-character source SHA**. The official descriptor/internal identifier is `from-scratch`. [Production storage and startup](PLUGIN_HUB_REVIEW.md#production-storage-and-startup) use its `progress` child for a new official challenge without reading or modifying files outside that location. Normal restart, import and explicit reset safeguards remain in place.

For this re-enablement request, finish documentation validation, commit/push the source and require successful Windows and Ubuntu CI on that exact SHA. Update only the source commit in `plugins/from-scratch` in the existing PR, preserving its explicit proposal to remove `disabled=disallowed readme` for maintainer approval. Do not bypass the restriction or open duplicate PRs. Never use a guessed future commit. Human review and merge remain outside our control.

The owner handles all RuneLite maintainer communications and submission prose. Agents must not post comments, replies, reviews, discussions or messages, or generate moderator replies. Preserve truthful existing AI attribution; manifest-update authorization does not authorize communication.

## During review

Keep one submission PR. Inspect automated build/security results and reviewer feedback. For necessary source changes, use focused validation, commit/push, require exact-commit CI and update the manifest in that same PR. Report human review as pending until it is actually accepted. Stop for a policy decision or major product redesign rather than unilaterally replacing the game mode.

No public v1.0.0 release, approval badge or Plugin Hub availability claim belongs before actual upstream merge and verified installation availability.

## After approval and verified availability

- Keep normal RuneLite Plugin Hub installation first in README/INSTALLATION.
- Replace pending-review language with the verified listing link, update metadata to v1.0.0 as appropriate and record the approved source in the changelog.
- Prepare one new immutable v1.0.0 tag/release using the appropriate verified commit. Existing releases/tags/assets require separate owner authorization for deletion; do not rewrite history.
- Recheck repository/listing author, description, tags, icon, links, license/notices and privacy statements. Publish only after the actual approval boundary has been met.
