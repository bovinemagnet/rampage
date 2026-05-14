# Rampage

A configuration-driven Gatling load testing framework. YAML describes the environment, run, and scenarios; a thin Java engine resolves them into Gatling protocols, scenarios, feeders, injection profiles, and assertions.

- **Author:** Paul Snow
- **Version:** 0.0.0
- **Java:** 25 (Adoptium toolchain)
- **Gatling:** 3.15.0
- **Build:** Gradle 9.x via the wrapper

## What it does

Engineers add new load test scenarios by writing YAML, GraphQL, and SQL — never Java. The framework handles:

- Three-file config split: `environment.yaml` (where), `run.yaml` (what + scale), `scenarios/*.yaml` (how).
- GraphQL POST requests with externalised query files and JSON variables.
- JDBC feeders (preload or streaming) backed by HikariCP, with column validation, sessionKey remapping, row caps, and queue/shuffle/random/circular strategies.
- Workload profiles: smoke, baseline, constant, ramp-and-hold, spike, stress, soak — in both open and closed injection models.
- Auth: env-sourced bearer tokens and OAuth client-credentials with background refresh.
- Per-scenario workload overrides; weighted scenario mixes; per-scenario HTTP protocols.
- Header layering env → run → scenario with Authorization-override protection.
- YAML placeholder expansion: `${run:id}`, `${env:NAME}`, `${sys:NAME}`, `${secret:path}`.
- Strict preflight validation: missing files, unknown workload types, malformed durations, mutating-vs-production conflicts, unresolved required secrets — all fail before any traffic.
- Dry-run mode that writes a summary and exits 0 without firing a request.
- Run metadata, dry-run summary, and sanitised config snapshot written alongside the Gatling HTML report.

## Quick start

```bash
# Validate the bundled smoke config (no traffic):
gradle21w validateLoadTest -Dloadtest.env=config/environments/local.yaml \
                           -Dloadtest.run=config/runs/smoke.yaml

# Run the smoke test against a local target on http://localhost:8080:
gradle21w gatlingRun -Dloadtest.env=config/environments/local.yaml \
                     -Dloadtest.run=config/runs/smoke.yaml

# Dry-run a load profile without traffic:
gradle21w gatlingRun -Dloadtest.env=config/environments/local.yaml \
                     -Dloadtest.run=config/runs/load.yaml \
                     -Dloadtest.dryRun=true

# Scaffold a new scenario from templates:
gradle21w newScenario -PscenarioId=customer-search
```

The Gatling HTML report, `run-metadata.json`, `dry-run-summary.json`, and `config-snapshot.yaml` land under `build/reports/gatling/`.

## Layout

```
src/main/java/io/rampage/         # engine: ConfigLoader, factories, writers
src/gatling/java/io/rampage/      # RampageSimulation (the Gatling entry point)
src/gatling/resources/            # default classpath YAML + gatling.conf + logback.xml
src/test/java/io/rampage/         # JUnit 5 + AssertJ tests
src/docs/                         # Antora documentation component
config/environments/              # filesystem env YAMLs
config/runs/                      # filesystem run YAMLs (smoke, load, ...)
config/scenarios/                 # filesystem scenario YAMLs
config/graphql/                   # GraphQL query files referenced by scenarios
config/queries/                   # SQL feeder files
config/templates/                 # scaffolding templates used by `newScenario`
docs/                             # planning artefacts (PRD, review, roadmap, features)
.github/workflows/                # CI workflow + manual smoke workflow
```

## Documentation

- Antora site under `src/docs/` — getting started, configuration reference, scenario authoring, workload profiles, security, reporting, troubleshooting.
- `docs/prd/initial-prd.md` — the product spec.
- `docs/review/code-review.md` — defects and gaps at the start of work.
- `docs/review/requirements-traceability.md` — PRD requirement → status mapping.
- `docs/roadmap/roadmap.md` — milestone plan (M1-M4).
- `docs/features/` — feature briefs shaped as paste-into-GitHub-issue bodies.

Regenerate the AsciiDoc configuration reference from the model classes via `gradle21w generateSchemaDocs` (output in `build/schema/`).

## Project status

| Milestone | Theme | State |
|---|---|---|
| M1 | MVP Honesty (close credibility gap) | Done |
| M2 | Operational Readiness | Done |
| M3 | Multi-scenario, Multi-environment | Done |
| M4 | Reporting, CI, and DX | Done |

Tests: 164 passing across `ConfigLoader`, `ConfigValidator`, `FeederFactory`, `SecretResolver`, `ScenarioFactory`, `WorkloadFactory`, `OAuthClientCredentialsTokenProvider`, `TokenRefresher`, `DataSourceRegistry`, `HeaderResolver`, `PlaceholderSubstitutor`, `BodyRedactor`, `AssertionFactory`, `RunMetadataWriter`, `DryRunSummaryWriter`, `ConfigSnapshotWriter`, and an end-to-end wiring integration test.

See `docs/review/requirements-traceability.md` for a row-by-row PRD coverage map.

## Conventions

- British spelling throughout user-facing strings and docs.
- Default author is **Paul Snow**, default version is **0.0.0** unless the work specifies otherwise.
- Local development uses `gradle21w` (a wrapper that sets up the Java 21 launcher for the Gradle 9 daemon, then drives the Java 25 toolchain). CI uses `./gradlew` so that no machine-local symlink is required.
- AsciiDoc work targets the Antora component at `src/docs/`; Mermaid diagrams are externalised into `src/docs/modules/ROOT/examples/`.

## Licence

See `LICENSE`.
