# From Scratch agent guidelines

## Scope and release authority

- Milestone 11 prepares the first official RuneLite Plugin Hub submission. The owner authorizes routine implementation, audited cleanup, commits, pushes and the initial manifest-only submission once its validation and compliance gates pass.
- Preserve published Friends Beta releases/tags and Git history. Never modify real progression, backups, credentials or account settings. No force push or history rewrite.
- Do not publish v1.0.0, claim RuneLite approval or advertise Hub availability before acceptance and verified availability. Stop before submission if a material compliance concern remains. Do not silently remove defining gameplay features to avoid review.
- Read `docs/technical/ARCHITECTURE.md`, `ACCOUNTING.md` and `TEST_PLAN.md` before behavior changes. Current documentation is indexed in `docs/README.md`; Git history preserves superseded milestone notes.
- Identity stays From Scratch, author Pilots r kwl, repository PilotsRKwl/fromScratch, package `com.fromscratch`, config `fromscratch`, internal/storage name `from_scratch`.

## Gameplay and accounting invariants

- From Scratch is a voluntary item-progression challenge. It never blocks withdrawals, enforces game actions or changes real ownership. Bank is the only ongoing item quantity authority: complete API container, never visible tabs/search/widgets. Mounted normal bank, no PIN and the next complete GameTick observation establish readiness. Empty non-null arrays are valid; missing/partial data is unknown.
- Signed progress is B' = B + (P' - P), with checked long arithmetic. Negative balances are valid. Positive corrections cannot exceed current physical stock. The initial confirmed legacy snapshot is immutable. Withdrawals do not prove consumption/spending; offline reconciliation observes the next net bank change.
- Initial setup requires the banking attestation, complete bank and supported Potion Storage capture/explicit unavailable-unlock answer, then confirmed Start. Filtering activates only after successful baseline persistence. No run/settings OFF means Full Bank; preserve explicit OFF, native-tab/search intersections, accurate title and re-entrant cleanup. No duplicate toggles or activation loops.
- Production uses BankController, RuneLiteBankSource, BankSidebar, BankRepository and the GE/potion engines. Historical personal-container/controller models stay test-only for verified migrations.
- All progression IO uses Filepath on the storage worker. Forced journals/checkpoints precede atomic current replacement; only acknowledged saves become UI/rendering authority. CAS and character/run/lifecycle guards reject stale callbacks. Preserve corrupt/newer/interrupted evidence; failed load never creates a new run. Reset archives before acknowledgment.
- Preserve existing formats/migrations, historical purchase prices, UUID identity, original archives, finite copy entitlements and atomic GP/entitlement/audit commits. Do not rename save/config identifiers or introduce schemas for cosmetic work.
- Fixed buyback costs are gameplay prices, never live GE calculations. Rune/Divine pouch share one 1M payment; open/closed Looting Bag share one 10K payment. Verified legacy contents offset atomically; unknown is not empty. Explicit empty/accounted declarations remain distinct from native proof. No repeated offsets, second family payment or invented historical contents.
- Preserve exact-copy Slayer/Black Mask/Elite Black Knight behavior and complete outfit roles. New equivalent families cannot duplicate ownership through transforms. Quiver direct/upgrade totals 60M, never 75M or an unnecessary charge for an independently earned blessed copy.
- GE evidence uses durable slot generations and bounded once-consumed uncertain attribution, never direct bank changes. History shows one completed/canceled-positive-partial trade per lifecycle, no guessed destination or verified-tax claim for estimates.
- Signed bank/potion wealth counts each balance once. Peak Progress Value remains monotonic at reliable checkpoints, plus confirmed expenditures once. Ordinary withdrawals, corrections, legacy offsets and GE conversions do not independently earn value.
- Time Played counts logged-in host ticks, not wall-clock/mobile time. Export/import is private local replacement after same-character preview/confirmation and backup, never merged histories or cloud synchronization.

## RuneLite integrations and safety

- Use Java 11 and public supported APIs/gameval constants. No reflection, JNI/JNA/Unsafe, Java object serialization, custom classloaders, downloaded code, external processes, gameplay input, packets or synthetic menu actions in production.
- Do not directly modify another plugin's configuration or core RuneLite overlay anchors/root UI. Review-sensitive public Bank Tags service operations, native bank script callbacks, title and quantity-display flags must be explicitly disclosed in `docs/PLUGIN_HUB_REVIEW.md`.
- Preserve native geometry/clipping, sprites, slots, quantities, menus and input. Quantity-display flags must restore on deactivation; physical counters remain gray, positive/negative virtual text configurable, locked tint defaults #333333 at 50%. Placeholders contribute zero. Do not fabricate widgets for absent debt.
- Examine remains one local supplemental message after the normal native/core-price responses, with bounded balanced markup and exact grouped signed quantities. No new Examine menu or outgoing player chat.
- Use injected Gson/OkHttp or RuneLite's existing ItemClient feed. No new transitive dependencies already supplied by RuneLite. Price HTTP runs on its separate worker, never client/EDT/storage/render threads. Unknown, zero and stale prices differ; no holdings/identity sent.
- Client APIs stay on the client thread, Swing on EDT, disk/network on workers. Do not block startup/shutdown; cancel scheduled work and call shutdownNow without awaitTermination. Use CompletableFuture.allOf for batching. No Thread.sleep.
- Keep rendering cheap and based on cached committed state. No per-frame scene scans or definition/disk/HTTP work in filter predicates. Diagnostics use DEBUG and remain local/rate-limited without account identifiers, snapshots, credentials or private paths.
- Keep permissive licensing/notices, genuine PNG icons and JAR-safe resource loading. No Plugin service-provider file. `build=standard` compiles only production; beta/preview source sets and Windows tools stay outside that artifact.
- Never use computer-use tools to operate RuneScape or authenticate. The owner reports the released beta gameplay works. Material production behavior changes require specific owner acceptance before submission; compilation/headless tests are not in-game evidence.

## Workflow and validation

- Inspect consumers before deleting. Retain migration/compatibility tests and current packaging/preview dependencies. Keep source diffs focused; no unrelated reformatting.
- Run incremental compilation and directly affected test filters during iteration. Do not repeatedly run the full local suite; final exact-commit Windows and Ubuntu CI supplies the complete regression gate. Never skip failures or weaken genuine assertions.
- Run documentation/link/image checks and the official standard production build before submission. Keep detailed diagnostics and generated artifacts under ignored build/. Never commit saves, secrets, runtime files, installers, caches or diagnostic output.
- If tracked sensitive data is found, stop and identify only its location/category and remediation requirements, never secret values. Do not silently rewrite history.
- Keep issue reports privacy-safe; do not request full logs, configs, authentication data or progression exports publicly.
- Plugin Hub submission uses only its required manifest with the actual canonical Git URL and verified 40-character commit, after checking name/PR conflicts. Preserve a single submission PR and disclose sensitive APIs and validation limits. Human review/merge remains outside our control.
- Disclose AI assistance truthfully in the submission footer: `Generated-by: OpenAI Codex`. Do not copy a different tool's attribution from a template.
- For a material gameplay change, offer `./gradlew run`, point to RuneLite's Using Jagex Accounts instructions and list precise owner checks. Do not claim live acceptance until the owner confirms it.
