# Rampage

A configuration-driven Gatling load testing framework. YAML describes the environment, run, and scenarios; a thin Java engine resolves them into Gatling protocols, scenarios, feeders, injection profiles, and assertions.

| | |
|---|---|
| **Java** | 25 (Adoptium toolchain, provisioned by the build) |
| **Gatling** | 3.15.0 |
| **Build** | Gradle 9.x via the included wrapper (`./gradlew`) |
| **Distribution** | `io.rampage:rampage` on GitHub Packages |

## What it does

Engineers add new load test scenarios by writing YAML, GraphQL, and SQL — no Java required. The framework provides:

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

The only prerequisite is a JDK capable of launching Gradle (Java 21 or later); the build provisions its own Java 25 toolchain.

> **New to Rampage?** The [Getting started guide](src/docs/modules/ROOT/pages/getting-started.adoc) walks through local set-up, validation, a first run, and where the reports land, step by step.

```bash
# Validate the bundled smoke configuration (no traffic is generated):
./gradlew validateLoadTest -Dloadtest.env=config/environments/local.yaml \
                           -Dloadtest.run=config/runs/smoke.yaml

# Run the smoke test against a local target on http://localhost:8080:
./gradlew gatlingRun -Dloadtest.env=config/environments/local.yaml \
                     -Dloadtest.run=config/runs/smoke.yaml

# Dry-run a load profile without generating traffic:
./gradlew gatlingRun -Dloadtest.env=config/environments/local.yaml \
                     -Dloadtest.run=config/runs/load.yaml \
                     -Dloadtest.dryRun=true

# Scaffold a new scenario from templates:
./gradlew newScenario -PscenarioId=customer-search
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

## Use as a library

Rampage is published as a Maven library (`io.rampage:rampage`) so other projects can drive it from their own Gatling builds. Releases are published to GitHub Packages on tag push; `./gradlew publishToMavenLocal` works for local consumption.

```kotlin
// settings: add the repository (GitHub Packages requires authentication)
repositories {
    mavenLocal()
    maven {
        url = uri("https://maven.pkg.github.com/bovinemagnet/rampage")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

// build.gradle.kts of a consumer project using the Gatling Gradle plugin
dependencies {
    gatling("io.rampage:rampage:0.1.0")
}
```

The JAR ships the engine (factories, config models, validators) and `io.rampage.simulation.RampageSimulation`. Gatling itself is **not** a transitive dependency — the consumer's Gatling plugin provides it — and no JDBC driver is bundled, so add the one your feeders need (H2, PostgreSQL, …). A consumer points the simulation at its own YAML with `-Dloadtest.env=` / `-Dloadtest.run=`, or subclasses it in their `src/gatling/java`:

```java
public class MyLoadTest extends io.rampage.simulation.RampageSimulation {
}
```

See the Antora documentation (`src/docs/`) page “Using Rampage as a library” for the full consumer guide.

## Documentation

The full documentation is an Antora component under `src/docs/`; build the site with `./gradlew antora` (output in `build/site/`). The key guides are also readable directly on GitHub:

- [Getting started](src/docs/modules/ROOT/pages/getting-started.adoc) — local set-up, validation, first run, and report output.
- [Scenario authoring](src/docs/modules/ROOT/pages/scenario-authoring.adoc) — writing the YAML, GraphQL, and SQL for a new scenario.
- [Configuration reference](src/docs/modules/ROOT/pages/configuration-reference.adoc) — every field in the environment, run, and scenario YAML.
- [Workload profiles](src/docs/modules/ROOT/pages/workloads.adoc) — choosing and tuning injection profiles.
- [Security and secrets](src/docs/modules/ROOT/pages/security.adoc) — credentials, tokens, and production guardrails.
- [Using Rampage as a library](src/docs/modules/ROOT/pages/library-usage.adoc) — consuming the published Maven artefact from your own project.
- [Reporting and CI](src/docs/modules/ROOT/pages/reporting.adoc) and [Troubleshooting](src/docs/modules/ROOT/pages/troubleshooting.adoc).

Project planning artefacts:

- `docs/prd/initial-prd.md` — the product specification.
- `docs/review/code-review.md` — defects and gaps identified at the start of development.
- `docs/review/requirements-traceability.md` — PRD requirement → status mapping.
- `docs/roadmap/roadmap.md` — milestone plan and platform evolution themes.
- `docs/features/` — feature briefs, each shaped as a ready-to-file GitHub issue.

Regenerate the AsciiDoc configuration reference from the model classes with `./gradlew generateSchemaDocs` (output in `build/schema/`).

## Project status

| Milestone | Theme | State |
|---|---|---|
| M1 | MVP Honesty (close credibility gap) | Done |
| M2 | Operational Readiness | Done |
| M3 | Multi-scenario, Multi-environment | Done |
| M4 | Reporting, CI, and DX | Done |

The unit suite (280+ tests, `./gradlew test`) covers the configuration loaders, validators, factories, secret resolution, token refresh, and reporting writers. A WireMock-backed integration suite (`./gradlew integrationTest`) drives the full simulation in-process and exercises every check kind end-to-end.

See `docs/review/requirements-traceability.md` for a row-by-row PRD coverage map.

## Conventions

- British spelling throughout user-facing strings and documentation.
- All build and run commands use the included Gradle wrapper, `./gradlew`.
- AsciiDoc documentation targets the Antora component at `src/docs/`; Mermaid diagrams are externalised into `src/docs/modules/ROOT/examples/`.

## Licence

See `LICENSE`.
