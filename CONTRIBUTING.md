# Contributing

From Scratch is by **Pilots r kwl**, under the [BSD 2-Clause license](LICENSE). The initial RuneLite Plugin Hub submission was merged, then distribution was disabled with the reason `disallowed readme`. Re-enablement requires maintainer review; see [current status](docs/LAUNCH.md#current-status). Do not publish v1.0.0 before verified availability.

Read [AGENTS.md](AGENTS.md), the [architecture](docs/technical/ARCHITECTURE.md), [accounting policy](docs/technical/ACCOUNTING.md) and [test plan](docs/technical/TEST_PLAN.md) before changing behavior. The [reviewer map](docs/PLUGIN_HUB_REVIEW.md) identifies production paths and review-sensitive integrations.

## Development and validation

Use Java 11 and the existing Gradle wrapper. Compile incrementally and run the test classes affected by your change. Use documentation/metadata checks for copy or asset updates. Do not repeatedly clean or run the full gameplay suite; final Windows and Ubuntu CI must pass on the exact source commit proposed for Plugin Hub.

Keep changes focused and reviewable. Preserve existing save migrations, prices, receipts, character isolation, backups and configuration identifiers. Keep disk and price IO off the client thread and EDT, use the complete bank for item quantities, and acknowledge persistence before displaying committed ownership. Follow RuneLite's current public APIs and dependency rules.

The standard production plugin must work independently of the beta launcher. Separate beta and preview sources must not enter its JAR. Do not add gameplay automation, synthetic input, withdrawal blocking, credential access, reflection/native calls or unnecessary third-party dependencies.

## Documentation and images

Keep the [README](README.md) player-focused. [Gameplay](docs/GAMEPLAY.md), [installation](docs/INSTALLATION.md), [troubleshooting](docs/TROUBLESHOOTING.md) and [privacy](docs/DATA_AND_PRIVACY.md) explain how to use the plugin; implementation detail belongs under `docs/technical` and the reviewer guide.

Check local links and image references with `scripts/Test-Documentation.ps1`. Preserve license/notices and artwork attribution. Existing UI previews render real plugin components with synthetic sample data and must be labeled accordingly. Obtain owner-provided, redacted captures for actual gameplay; never fabricate RuneScape screenshots or read real profiles for marketing. See the [capture checklist](docs/GAMEPLAY.md#screenshots-and-captures).

## Safety, reporting and submission

Only the owner operates RuneScape for login and gameplay acceptance. Use disposable synthetic progression for tests and keep private files out of Git. A compile or headless test is not an in-game result.

[Bug reports](https://github.com/PilotsRKwl/fromScratch/issues/new?template=bug_report.md) should provide reproducible steps and minimal redacted evidence. Do not request credentials, account data, whole logs, saves or progression exports. See [safe reporting](docs/TROUBLESHOOTING.md#report-a-reproducible-problem).

The owner has authorized Milestone 11 cleanup, source commits/pushes and the initial Plugin Hub submission after its compliance and validation gates pass. Do not claim approval or publish v1.0.0 before upstream acceptance and verified availability. Material policy uncertainty or a major gameplay redesign requires an explicit owner decision. Routine documentation changes do not authorize separate releases, destructive progression changes or unrelated remote writes.
