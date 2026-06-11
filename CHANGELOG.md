# Changelog

All notable changes to Rampage are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] — 2026-06-12

First versioned release. Rampage is a configuration-driven Gatling load testing
framework: environments, runs, and scenarios are described in YAML, GraphQL, and
SQL, and a thin Java engine compiles them into Gatling simulations.

### Added

- **Configuration-driven engine** — three-layer YAML model (environment, run,
  scenario) compiled at runtime into Gatling protocols, scenarios, feeders,
  injection profiles, and assertions.
- **Workload profiles** — smoke, baseline, constant, ramp-and-hold, spike,
  stress, and soak, in both open and closed injection models, with per-scenario
  overrides and weighted scenario mixes.
- **JDBC feeders** — SQL-file-backed, HikariCP-pooled, preload or streaming
  modes, column validation with session-key remapping, row caps, and
  queue/shuffle/random/circular strategies.
- **Checks and assertions** — HTTP status, JSONPath, regex, header, body
  substring, and response-time checks; response extraction into session keys;
  global and per-scenario response-time and error-rate assertions.
- **Security** — environment-sourced bearer tokens, OAuth client-credentials
  with background refresh, fail-fast secret resolution, header-override
  protection, production-environment guardrails, mutating-request approval,
  and secret redaction in reports and snapshots.
- **Strict preflight validation** — missing files, unknown workload types,
  feeder strategies, execution modes, extract types and check expectations,
  malformed durations, assertion threshold ranges, and dangling session
  references all fail before any traffic is generated.
- **Reporting** — run metadata (git commit/branch, effective workload, feeder
  row counts), sanitised config snapshots, dry-run summaries, run summary
  generation and baseline comparison.
- **Scaffolding** — `newScenario` template task, HAR and OpenAPI importers,
  auto-generated configuration reference from the model classes.
- **Web console** (unpublished subproject) — live dashboard, run orchestration
  and queueing, run history, config browsing and editing, and metrics streaming.
- **Maven library distribution** — published as `io.rampage:rampage` to GitHub
  Packages (and `mavenLocal`) with sources and javadoc JARs; the artefact ships
  the engine and `RampageSimulation` so consumer projects add one dependency and
  a three-line simulation subclass. Gatling and JDBC drivers remain
  consumer-supplied.
- **Documentation** — Antora site (`./gradlew antora`) covering getting
  started, scenario authoring, configuration reference, workloads, security,
  reporting, troubleshooting, and library consumption.
- **CI** — build/test workflow, manual smoke-run workflow, and tag-triggered
  publishing to GitHub Packages.

[0.1.0]: https://github.com/bovinemagnet/rampage/releases/tag/v0.1.0
