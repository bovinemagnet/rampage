# Rampage — Roadmap

Author: Paul Snow
Version: 0.0.0
Date: 2026-05-14
Source PRD: `docs/prd/initial-prd.md`
Source Review: `docs/review/code-review.md`, `docs/review/requirements-traceability.md`

## Vision

Deliver a reusable, configuration-driven Gatling load testing framework where engineers add scenarios by writing YAML, GraphQL, and SQL — never Java. The Java engine remains thin, opinionated, and testable.

## Current State (snapshot)

PRD Phase 2 (YAML Loading) is complete; Phases 3–5 are partially implemented; Phase 6 (Reporting and CI) has only the disconnected `RunMetadataWriter`. See `docs/review/requirements-traceability.md` for the line-by-line status.

The MVP backbone runs end-to-end against a hand-rolled environment, but does not yet meet MVP acceptance criteria 4, 7, and 10 honestly.

## Milestones

The roadmap is broken into four milestones. Each maps to a GitHub milestone and groups the feature briefs under `docs/features/`. Order is meaningful: each milestone unblocks the next.

### M1 — MVP Honesty (close the credibility gap)

**Goal:** Every MVP acceptance criterion in PRD §20 holds without footnotes.

Themes:
- Reporting is wired (`RunMetadataWriter` actually runs).
- Secrets fail loud when missing or unresolved.
- Request bodies are valid JSON regardless of variable type.
- The validator catches what the PRD §18 list says it should.
- Scenario path resolution is unified between validate and run.

Features:
- F-001 Wire RunMetadataWriter into the simulation lifecycle
- F-002 Fail-fast secret resolution
- F-003 Jackson-based GraphQL body construction
- F-004 Strict config validation (files, enums, durations, mutating-vs-prod)
- F-005 Unified scenario path resolution
- F-006 Correlation ID session population
- F-007 Tighten redaction semantics in RunMetadataWriter
- F-008 ScenarioFactory test coverage

Exit criteria:
- Every PRD §20 MVP item is **Done** in the traceability matrix.
- A missing `API_TOKEN` causes `gatlingRun` to exit non-zero before any traffic.
- `run-metadata.json` is present in `build/reports/gatling/<run>/` after every successful run, with no secret values inside.
- `validateLoadTest` and `gatlingRun` agree on whether a config is valid.

### M2 — Operational Readiness

**Goal:** The framework is safe to point at non-prod systems unsupervised in CI.

Themes:
- Workload coverage matches PRD §11 for read-only operations.
- Safety controls actually gate execution.
- Connection pooling honoured.
- Logging is deterministic in CI.

Features:
- F-009 HikariCP-backed JDBC feeder pool
- F-010 Workload profile: spike
- F-011 Workload profile: stress
- F-012 Workload profile: baseline + closed-model
- F-013 Dry-run gate on `gatlingRun`
- F-014 Production-environment guardrails (`failIfEnvironmentAllowsProduction`, mutating-vs-allowProd)
- F-015 Header precedence layering + Authorization protection
- F-016 logback.xml + gatling.conf for predictable logs
- F-017 Scenario-level workload override (complete the D2 TODO)
- F-018 HTTP timeouts honoured (ENV-006)

Exit criteria:
- `run.safety.dryRun: true` produces a "would have run" summary and exits 0 without traffic.
- A run pointing at a `prod` environment without `safety.allowProduction: true` fails in the validator.
- All six PRD §11 workload types map to Gatling injection steps.
- HikariCP pool metrics observable in feeder logs.

### M3 — Multi-scenario, Multi-environment

**Goal:** Realistic load tests with several scenarios and richer auth.

Themes:
- Per-scenario base URLs and weights.
- OAuth client-credentials + token refresh.
- Feeder strategy parity with PRD §12.
- Direct (streaming) JDBC feeder mode.
- Variable substitution in YAML.

Features:
- F-019 Per-scenario HTTP protocol routing
- F-020 Scenario weighting in run config
- F-021 OAuth client-credentials token source
- F-022 Token refresh for long-running tests
- F-023 Feeder strategies: queue + true random
- F-024 Direct JDBC feeder mode (streaming)
- F-025 Feeder row cap (FDR-006)
- F-026 Feeder column validation (FDR-002)
- F-027 YAML placeholder expansion (`${run:id}`, `${secret:...}`, `${RUN_ID}`)
- F-028 Scenario-level assertions wired through

Exit criteria:
- A single `run.yaml` can drive two scenarios against two different `baseUrls` with different weights.
- OAuth credentials environments can run more than the token TTL without re-auth in the SUT.
- Feeder strategies match PRD §12.3 verbatim.

### M4 — Reporting, CI, and DX

**Goal:** Reports are useful artefacts; CI consumes them; engineers can spin up a new scenario in 10 minutes.

Themes:
- Sanitised config snapshot in reports.
- Build failure semantics aligned with assertions.
- GitHub Actions integration recipe.
- Antora docs site under `src/docs/`.
- Scenario scaffolding CLI task.

Features:
- F-029 Sanitised config snapshot writer
- F-030 Effective workload + scenario summary in run metadata
- F-031 GitHub Actions workflow + artifact upload
- F-032 Antora docs site scaffold under `src/docs/`
- F-033 Antora content migration (review + roadmap + features as `.adoc`)
- F-034 Scenario scaffolding Gradle task (`newScenario`)
- F-035 Auto-generated YAML schema docs from model classes
- F-036 Body redaction layer (SEC-008)
- F-037 Gatling plugin upgrade to current stable
- F-038 RampageSimulation integration test against WireMock

Exit criteria:
- A CI run uploads HTML report, `run-metadata.json`, and config snapshot as artefacts.
- Antora docs build cleanly via `./gradlew antora`.
- A new engineer can run `./gradlew newScenario -PscenarioId=foo` to bootstrap a YAML + GraphQL + SQL trio.

## Platform evolution

The milestones above close the known gaps in the current engine and tooling. This section looks further ahead: the evolution of Rampage from a configuration-driven Gatling wrapper with a thin live console into a LoadRunner-class load-testing *platform* — one that stores history, detects regressions automatically, provides a real-time operations dashboard, and integrates cleanly with the wider engineering toolchain.

Each theme below is presented with a goal, concrete capabilities, and an indicative effort/impact rating. The ratings reflect implementation complexity relative to the current codebase, not calendar time.

### Theme 1 — Results store, history and trends *(in progress)*

**Goal:** Give the console a memory.

A design specification exists and an implementation plan is being prepared.

- Persist every run — whether launched from the live console or executed as a headless CLI invocation — to an embedded H2 database via Quarkus Hibernate ORM Panache.
- Replace the filesystem-scan history page with a queryable, searchable, taggable history backed by the database.
- Side-by-side run-to-run comparison view with per-scenario metric deltas.
- Trend charts (P95, throughput, error rate over time) for a given run configuration.

**Impact:** High. **Effort:** Medium.
This theme is the data foundation on which every other platform theme builds.

### Theme 2 — Trend and regression analytics

**Goal:** Surface regression detection in the UI.

- Bring the CLI baseline-comparison logic (`RunSummaryComparator`) into the console as a first-class view.
- Automatic baseline selection per run configuration; flag P95 and error-rate regressions against it.
- Pass/fail regression badges on the history list.

**Impact:** High. **Effort:** Medium.

### Theme 3 — Live-dashboard depth

**Goal:** A real-time operations dashboard, not a snapshot.

- Replace the fixed six-cell metric grid with time-series charts covering throughput, latency percentiles, errors, and virtual users over time.
- Per-scenario and per-request live drill-down.
- Live SLA thresholds shown against assertions, with optional early-abort on breach.

**Impact:** High. **Effort:** Large.

### Theme 4 — Test management

**Goal:** Manage tests, not just launch them.

- Cron-scheduled runs with a schedule history.
- Parameterised launch — override workload and run parameters from the UI without editing YAML.
- Optional concurrent and distributed injector orchestration (today the console is single-slot FIFO).

**Impact:** Medium. **Effort:** Large.

### Theme 5 — Integration surface

**Goal:** Connect Rampage to the wider toolchain.

- Slack, email, and webhook notifications on run completion or threshold breach.
- A Prometheus/Grafana metrics exporter built on the console's existing Carbon receiver.

**Impact:** Medium. **Effort:** Small.

### Theme 6 — Engine-level gaps

**Goal:** Close known gaps in the load engine itself.

- Real secret-manager integration (Vault, AWS Secrets Manager, Azure Key Vault) — currently stubbed with `***REDACTED***`.
- Protocols beyond HTTP/GraphQL — gRPC and WebSocket.
- Finer assertion granularity — throughput SLOs, more percentiles, and per-step assertions.
- Wire the modelled-but-unused `includeRunMetadataHeaders` observability flag.

**Impact:** Medium. **Effort:** Medium.

### Follow-up briefs from the June 2026 review

A full codebase review (June 2026) confirmed all M1–M4 features are genuinely implemented and produced follow-up briefs `F-039`–`F-045` in `docs/features/`, mapped to the themes above: console authentication, resource bounds, and web-resource tests (Theme 4), and engine refinements — per-request correlation IDs, HTTP status range checks, request-timeout overrides, and wiring or removing the unused feeder exhaustion fields (Theme 6). The same review delivered Maven library publishing: the engine plus `RampageSimulation` now publish as `io.rampage:rampage` to GitHub Packages on tag push.

### Sequencing note

Theme 1 is being implemented first because the results store is the data foundation that every other platform theme depends on. Without persisted run history there is no baseline to compare against, no trend to chart, and no schedule history to display. The design specification is at `docs/superpowers/specs/2026-05-16-results-store-design.md`.

## Cross-cutting threads

The following are not standalone milestones but should be tracked across every PR:

- **TDD parity** — Every behaviour change in a factory needs a unit test added in the same PR.
- **British spelling** in user-facing strings and docs.
- **CLAUDE.md / AGENTS.md compatibility** — Conventions in `CLAUDE.md` apply to AI-assisted contributions.
- **PRD as spec, not status** — The PRD describes intent; this roadmap and the traceability matrix describe state. Keep them in sync.

## Sequencing rationale

M1 is the smallest set of changes that make the existing claims true. M2 stops the framework being dangerous to point at a real environment. M3 is where the framework starts paying off (multiple scenarios, real auth). M4 turns it into a product the team can hand to other engineers.

Each feature brief in `docs/features/` is sized to fit a single GitHub issue, with explicit acceptance criteria, suggested labels, and a recommended assignee skill profile.

## How to use this roadmap

1. Open the GitHub repo and create four milestones named **M1**, **M2**, **M3**, **M4**.
2. For each feature brief in `docs/features/F-NNN-*.md`, paste the body into a new issue, set the milestone, and apply the labels listed at the bottom of the brief.
3. Track progress against this roadmap by checking off features in `docs/features/README.md`.
4. Update the traceability matrix in `docs/review/requirements-traceability.md` as items move from **Partial/Missing** to **Done**.
