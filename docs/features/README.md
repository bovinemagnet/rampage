# Rampage — Feature Index

Author: Paul Snow
Version: 0.1.0
Date: 2026-05-14

This index lists every feature with its milestone, suggested labels, one-line summary, and current status. Milestones M1–M4 are complete; the open items are the platform-evolution follow-ups (F-039 onwards). Each brief is shaped as a self-contained GitHub issue body — paste the body of any `F-NNN-*.md` into a new issue, attach the listed labels, and assign the milestone.

See:
- `docs/review/code-review.md` — defects and rationale
- `docs/review/requirements-traceability.md` — PRD coverage status
- `docs/roadmap/roadmap.md` — milestone narrative

## How to create the milestones

In GitHub, create four milestones with the exact names below so that the `milestone:` labels in each brief resolve cleanly:

- **M1 — MVP Honesty**
- **M2 — Operational Readiness**
- **M3 — Multi-scenario, Multi-environment**
- **M4 — Reporting, CI, and DX**

## Suggested label set

Create labels for:

- Areas: `area:reporting`, `area:security`, `area:simulation`, `area:feeder`, `area:workload`, `area:safety`, `area:cli`, `area:config`, `area:validation`, `area:http`, `area:assertions`, `area:logging`, `area:docs`, `area:ci`, `area:dx`, `area:tooling`, `area:build`, `area:test`
- Types: `type:bug`, `type:feature`, `type:enhancement`, `type:refactor`, `type:chore`, `type:test`
- Priority: `priority:high`, `priority:medium`, `priority:low`

## M1 — MVP Honesty (close the credibility gap)

| ID | Title | Type | Priority | Status |
|---|---|---|---|---|
| [F-001](F-001-wire-run-metadata-writer.md) | Wire RunMetadataWriter into the simulation lifecycle | bug | high | Done |
| [F-002](F-002-fail-fast-secret-resolution.md) | Fail-fast secret resolution | bug | high | Done |
| [F-003](F-003-jackson-graphql-body.md) | Jackson-based GraphQL body construction | bug | high | Done |
| [F-004](F-004-strict-config-validation.md) | Strict config validation | enhancement | high | Done |
| [F-005](F-005-unified-scenario-path-resolution.md) | Unified scenario path resolution | refactor | medium | Done |
| [F-006](F-006-correlation-id-session-population.md) | Correlation ID session population | bug | medium | Done |
| [F-007](F-007-tighten-redaction-semantics.md) | Tighten redaction semantics in RunMetadataWriter | bug | medium | Done |
| [F-008](F-008-scenario-factory-tests.md) | ScenarioFactory test coverage | test | medium | Done |

**Exit:** Every MVP acceptance criterion in PRD §20 is honestly **Done** in the traceability matrix.

## M2 — Operational Readiness

| ID | Title | Type | Priority | Status |
|---|---|---|---|---|
| [F-009](F-009-hikaricp-feeder-pool.md) | HikariCP-backed JDBC feeder pool | enhancement | medium | Done |
| [F-010](F-010-workload-spike.md) | Workload profile: spike | feature | medium | Done |
| [F-011](F-011-workload-stress.md) | Workload profile: stress | feature | medium | Done |
| [F-012](F-012-workload-baseline-and-closed-model.md) | Workload profile: baseline + closed-model | feature | medium | Done |
| [F-013](F-013-dry-run-gate.md) | Dry-run gate on gatlingRun | feature | high | Done |
| [F-014](F-014-production-environment-guardrails.md) | Production-environment guardrails | enhancement | high | Done |
| [F-015](F-015-header-precedence-and-auth-protection.md) | Header precedence + Authorization protection | enhancement | medium | Done |
| [F-016](F-016-logback-and-gatling-conf.md) | logback.xml + gatling.conf | enhancement | low | Done |
| [F-017](F-017-scenario-workload-override.md) | Scenario-level workload override | bug | high | Done |
| [F-018](F-018-http-timeouts-honoured.md) | HTTP timeouts honoured | bug | medium | Done |

**Exit:** The framework is safe to run unsupervised against any non-prod environment.

## M3 — Multi-scenario, Multi-environment

| ID | Title | Type | Priority | Status |
|---|---|---|---|---|
| [F-019](F-019-per-scenario-http-protocol-routing.md) | Per-scenario HTTP protocol routing | bug | medium | Done |
| [F-020](F-020-scenario-weighting.md) | Scenario weighting in run config | feature | medium | Done |
| [F-021](F-021-oauth-client-credentials.md) | OAuth client-credentials token source | feature | high | Done |
| [F-022](F-022-token-refresh.md) | Token refresh for long-running tests | feature | medium | Done |
| [F-023](F-023-feeder-strategies.md) | Feeder strategies: queue + true random | feature | low | Done |
| [F-024](F-024-direct-jdbc-feeder-mode.md) | Direct JDBC feeder mode (streaming) | feature | medium | Done |
| [F-025](F-025-feeder-row-cap.md) | Feeder row cap (FDR-006) | feature | medium | Done |
| [F-026](F-026-feeder-column-validation.md) | Feeder column validation (FDR-002) | enhancement | medium | Done |
| [F-027](F-027-yaml-placeholder-expansion.md) | YAML placeholder expansion | feature | medium | Done |
| [F-028](F-028-scenario-level-assertions.md) | Scenario-level assertions | feature | medium | Done |

**Exit:** Realistic multi-scenario, real-auth load tests run with confidence.

## M4 — Reporting, CI, and DX

| ID | Title | Type | Priority | Status |
|---|---|---|---|---|
| [F-029](F-029-sanitised-config-snapshot.md) | Sanitised config snapshot writer | feature | medium | Done |
| [F-030](F-030-effective-workload-summary.md) | Effective workload + scenario summary in metadata | enhancement | low | Done |
| [F-031](F-031-github-actions-workflow.md) | GitHub Actions workflow + artifact upload | feature | high | Done |
| [F-032](F-032-antora-docs-scaffold.md) | Antora docs site scaffold under src/docs/ | feature | medium | Done |
| [F-033](F-033-antora-content-migration.md) | Migrate review, roadmap, features into Antora | chore | low | Done |
| [F-034](F-034-scenario-scaffolding-task.md) | Scenario scaffolding Gradle task | feature | low | Done |
| [F-035](F-035-yaml-schema-docs.md) | Auto-generated YAML schema docs | feature | low | Done |
| [F-036](F-036-body-redaction-in-logs.md) | Body redaction in logs (SEC-008) | feature | medium | Done |
| [F-037](F-037-gatling-plugin-upgrade.md) | Gatling plugin upgrade to current stable | chore | low | Done |
| [F-038](F-038-rampage-simulation-integration-test.md) | RampageSimulation integration test | test | medium | Done |

**Exit:** Reports are useful artefacts; CI consumes them; engineers can spin up a new scenario in under 10 minutes.

## Platform evolution — follow-up briefs (June 2026 review)

A full codebase review in June 2026 confirmed F-001 through F-038 are implemented and raised the following follow-ups. They slot under the roadmap's "Platform evolution" themes rather than a new milestone.

| ID | Title | Type | Priority | Status |
|---|---|---|---|---|
| [F-039](F-039-console-authentication.md) | Console authentication | enhancement | high | Open |
| [F-040](F-040-console-resource-bounds.md) | Console resource bounds: run queue and process output | enhancement | medium | Open |
| [F-041](F-041-console-web-resource-tests.md) | Console web-resource test coverage | test | medium | Open |
| [F-042](F-042-per-request-correlation-id.md) | Optional per-request correlation ID | enhancement | low | Open |
| [F-043](F-043-http-status-range-checks.md) | HTTP status range checks | feature | low | Open |
| [F-044](F-044-request-timeout-override.md) | Per-scenario and per-step request timeout override | feature | low | Open |
| [F-045](F-045-wire-or-remove-feeder-exhaustion-fields.md) | Wire or remove `feeder.onExhaustion` | enhancement | low | Open |

## Dependency notes

- F-007 depends on F-001 landing first.
- F-013 (dry-run) is easier after F-004 (strict validation), since the dry-run summary needs to know what "valid" means.
- F-022 depends on F-021.
- F-024 depends on F-009.
- F-033 depends on F-032.
- F-038 should be the last item in M4 so it can assert F-001, F-029, F-030 output shapes.

## Tracking progress

As features land:

1. Flip the **Status** column for the entry in this index from Open to Done.
2. Tick the acceptance criteria in the feature's brief; leave any genuinely unmet bullet unticked with a note pointing at its follow-up brief.
3. Update `docs/review/requirements-traceability.md` — flip the relevant PRD row from **Partial/Missing** to **Done**.
