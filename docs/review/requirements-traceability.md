# Rampage — Requirements Traceability Matrix

Author: Paul Snow
Version: 0.0.0
Date: 2026-05-14
Source PRD: `docs/prd/initial-prd.md`

Status legend:
- **Done** — implemented and exercised by tests
- **Partial** — implemented for the happy path but with gaps or defects (see `docs/review/code-review.md`)
- **Missing** — no implementation in tree
- **N/A** — out of scope for current state

## Environment Requirements

| ID | Requirement | Status | Evidence / Gap |
|---|---|---|---|
| ENV-001 | Multiple named base URLs | Done | `EnvironmentConfig.baseUrls: Map<String,String>`; `HttpProtocolFactory.build` resolves by `endpointRef` with fallback to `rest`. |
| ENV-002 | Shared HTTP headers | Partial | `SecurityConfig.headers` and `ObservabilityConfig.correlationIdHeader` injected. No layering rules with run/scenario headers (PRD §14). |
| ENV-003 | Auth: bearer token, JWT, OAuth client creds, custom provider | Partial | Only `mode: bearer-token` with env-sourced static token. JWT generation, OAuth, custom providers missing. |
| ENV-004 | Database connection definitions | Done | `DatabaseConfig`, `PoolConfig`, `CredentialConfig` parsed and consumed by `FeederFactory` (pool fields ignored — see D7). |
| ENV-005 | No raw secrets in env files | Partial | Sample YAMLs use `source: env` references; `SecretResolver` resolves them. No CI lint to enforce, no SM/Vault integration. |
| ENV-006 | Timeout settings | Partial | Parsed into `HttpConfig`; `HttpProtocolFactory` only applies `acceptHeader` and `contentTypeHeader`. `connectTimeoutMillis` / `requestTimeoutMillis` parsed but not passed to Gatling's `http.requestTimeout(...)`. |
| ENV-007 | Safety controls (prod protection, mutating approval) | Partial | `SafetyConfig.allowProduction` checked by string-match in `ConfigValidator`; `requireApprovalForMutatingRequests` parsed but never enforced. |

## Run Requirements

| ID | Requirement | Status | Evidence / Gap |
|---|---|---|---|
| RUN-001 | Reference one or more scenarios | Done | `RunConfig.scenarios: List<ScenarioRef>`; `RampageSimulation` iterates. |
| RUN-002 | Global and scenario-level assertions | Partial | `AssertionsConfig` parsed; only global p95/p99/error% wired. Scenario-level assertions parsed and unused. |
| RUN-003 | Open and closed workload models | Partial | `ExecutionConfig.mode: "open"` accepted; only open-model injection steps emitted. Closed model not implemented. |
| RUN-004 | smoke, baseline, load, stress, spike, soak | Partial | `smoke`, `ramp-and-hold`, `soak`, `constant` implemented. `baseline`, `spike`, `stress` missing. |
| RUN-005 | Run metadata for reporting/traceability | Partial | `MetadataConfig` parsed; `RunMetadataWriter` exists but is **not wired** (D1). `changeReference` not emitted (D13). |
| RUN-006 | Dry-run validation without executing load | Partial | `validateLoadTest` Gradle task exists. `run.safety.dryRun` parsed but does not gate `gatlingRun`. |
| RUN-007 | Disabled scenarios remain in file | Done | `ScenarioRef.enabled` honoured in `RampageSimulation`. |

## Scenario Requirements

| ID | Requirement | Status | Evidence / Gap |
|---|---|---|---|
| SCN-001 | GraphQL POST | Done | `ScenarioFactory.build` always POSTs to `/<endpointRef>`. |
| SCN-002 | External GraphQL query files | Done | `RequestConfig.graphqlQueryFile`; loaded by `ConfigLoader.loadResource`. |
| SCN-003 | GraphQL variables from feeders | Partial | `${feeder:X}` is rewritten to Gatling EL `#{X}`. Type safety is broken (D6) — booleans/numbers become strings. |
| SCN-004 | SQL-backed JDBC feeders | Done | `FeederFactory.loadFromSql` reads SQL from filesystem or classpath. |
| SCN-005 | Feeder strategies (queue, shuffle, random, circular) | Partial | `circular` and `random/shuffle` (shuffle-once). `queue` not implemented. Random is misnamed (D14). |
| SCN-006 | Request-specific headers | Done | `ScenarioConfig.headers` are applied in `ScenarioFactory.build`. |
| SCN-007 | HTTP status + JSONPath checks | Partial | `httpStatus`, `exists`, `absentOrEmpty`, `equalsSession` supported. No regex, no XPath, no JSON schema, no header checks. |
| SCN-008 | Scenario-specific workload overrides | Missing | TODO in `RampageSimulation.java:71-72` (D2). |
| SCN-009 | Mutating vs read-only marker | Partial | `ScenarioSafetyConfig.mutating` parsed but unused. No enforcement against `requireApprovalForMutatingRequests`. |
| SCN-010 | Tags for filtering and reporting | Partial | `tags` parsed; not used for filtering or in `RunMetadataWriter`. |

## Feeder Requirements

| ID | Requirement | Status | Evidence / Gap |
|---|---|---|---|
| FDR-001 | JDBC feeder from SQL files | Done | `FeederFactory.loadFromSql`. |
| FDR-002 | Validate required feeder columns | Missing | `ColumnConfig.required` parsed but `FeederFactory` does not check the result set against it. |
| FDR-003 | Fail fast on empty + `failIfEmpty` | Done | `FeederFactory.loadFromSql` throws when empty and flag set. |
| FDR-004 | queue / shuffle / random / circular | Partial | See SCN-005. |
| FDR-005 | Preload feeder data | Done | Default `preload: true`; loaded into `List<Map<String,Object>>`. |
| FDR-006 | Maximum row limit | Missing | No row cap; a runaway query can OOM the JVM. |
| FDR-007 | Log row count, not values | Done | `log.info("Loaded {} feeder rows...", rows.size())`. |

## Security Requirements

| ID | Requirement | Status | Evidence / Gap |
|---|---|---|---|
| SEC-001 | No raw secrets in source control | Done by convention | Sample YAMLs use env-var refs; no automated lint. |
| SEC-002 | Env-var or secret-manager refs | Partial | Env vars supported. Secret-manager source returns `***REDACTED***` (stub). |
| SEC-003 | Reports/snapshots redact secrets | Partial | `RunMetadataWriter` claims `redacted: true` but never holds resolved secrets. Config snapshot not generated at all (D10). |
| SEC-004 | Authorization injected from environment | Done | `HttpProtocolFactory` adds `Authorization: Bearer <token>` for `bearer-token` mode. |
| SEC-005 | Per-scenario headers on top of environment | Partial | Scenario headers applied; no enforcement against overriding `Authorization` (D12). |
| SEC-006 | Token refresh / generation for long runs | Missing | Token is read once at simulation init. |
| SEC-007 | Prevent accidental production execution | Partial | `ConfigValidator` checks `env.id.contains("prod")`. `run.safety.failIfEnvironmentAllowsProduction` parsed but unused. |
| SEC-008 | Avoid logging full request bodies with sensitive data | Missing | Gatling default logging used; no body redaction layer. |

## Config Resolution Rules (PRD §14)

| Rule | Status | Notes |
|---|---|---|
| Built-in defaults | Partial | Some defaults are in model classes (e.g. `HttpConfig.connectTimeoutMillis = 5000`); some only in factories. No central defaults table. |
| Environment → Run → Scenario YAML layering | Partial | Read separately; no merge logic. Scenario workload override is incomplete (D2). |
| JVM system property overrides | Done | `-Dloadtest.env`, `-Dloadtest.run` in `ConfigLoader`. |
| Env-var overrides | Partial | Used for secrets only, not for arbitrary YAML keys. |
| Secret resolver output | Partial | Implemented for `CredentialConfig` and `TokenConfig`; no resolution of `${secret:...}` placeholders inside arbitrary header values. |
| Header precedence | Missing | No layering, no Authorization protection. |

## CLI (PRD §15)

| Command | Status | Notes |
|---|---|---|
| `gradle21w validateLoadTest -Dloadtest.env=... -Dloadtest.run=...` | Done | Registered in `build.gradle.kts`; `ConfigValidatorMain` exits 0/1. |
| `gradle21w gatlingRun -Dloadtest.env=... -Dloadtest.run=...` | Done | Provided by Gatling plugin; properties consumed by `ConfigLoader`. |
| `gradle21w gatlingRun-<simulationClass>` | Done | Standard plugin behaviour. |
| `-Dloadtest.dryRun=true` | Missing | Property name not recognised; `RunSafetyConfig.dryRun` not honoured by `gatlingRun`. |

## Gradle Build Requirements (PRD §16)

| Item | Status |
|---|---|
| Gradle Wrapper | Done (`gradlew`, `gradlew.bat`) |
| Java 25 toolchain | Done |
| Gatling plugin | Done (3.13.5 — older than PRD-recommended 3.15.0.2) |
| Jackson YAML | Done |
| JDBC drivers | Partial — only H2; production drivers must be user-supplied |
| `validateLoadTest` task | Done |
| CI-friendly reports | Missing |

## Validation Rules (PRD §18)

| # | Rule | Status |
|---|---|---|
| 1 | Required YAML files missing | Done (`ConfigLoader` throws) |
| 2 | Unknown required fields / invalid enums | Partial — `FAIL_ON_UNKNOWN_PROPERTIES=false`; no enum validation |
| 3 | Missing GraphQL/SQL files | Missing — failure happens at simulation init, not validation |
| 4 | Empty JDBC feeder with `failIfEmpty=true` | Partial — caught at feeder load, not in `ConfigValidator` |
| 5 | Required secret cannot be resolved | Missing — currently returns `""` |
| 6 | Mutating scenario vs env disallowing mutation | Missing |
| 7 | Production target without approval | Partial — string-match only |
| 8 | Invalid workload duration/rate | Missing — parser falls back to default silently |
| 9 | Malformed assertions | Missing |
| 10 | Scenario with no checks | Missing |

## Reporting (PRD §19)

| Output | Status |
|---|---|
| Gatling HTML report | Done — produced by plugin |
| Run metadata JSON | Partial — code exists, not wired (D1) |
| Sanitised resolved config snapshot | Missing |
| Scenario list + effective workload summary | Missing |
| Feeder row counts | Partial — logged, not in report metadata |
| Assertion results | Partial — Gatling-native; not in custom metadata |
| Build/commit metadata | Partial — `gitCommit` captured, but `RunMetadataWriter` not invoked |

## MVP Acceptance Criteria (PRD §20)

| # | Criterion | Status |
|---|---|---|
| 1 | Smoke test from Gradle on Java 25 | Done |
| 2 | Loads env + run + scenario YAML | Done |
| 3 | Executes GraphQL with external query | Done |
| 4 | Variables from SQL feeder | Partial — works for strings only (D6) |
| 5 | Ramp-up, hold, request rate | Done |
| 6 | Injects auth headers from secret refs | Done (env-var path only) |
| 7 | Fails fast on unavailable secrets | Missing (D8) |
| 8 | Generates Gatling report | Done |
| 9 | At least one global assertion | Done |
| 10 | Redacts secrets in logs/metadata | Partial — flag only, no behaviour (D10) |

## Post-MVP Acceptance Criteria (PRD §20)

| # | Criterion | Status |
|---|---|---|
| 1 | Multiple scenarios in one run | Partial — runs them, but HTTP protocol picks first endpoint (D3) |
| 2 | Scenario weighting | Missing |
| 3 | OAuth client-credentials token | Missing |
| 4 | Token refresh for long runs | Missing |
| 5 | CI/CD report artifacts | Missing |
| 6 | Scenario templates via CLI task | Missing |
| 7 | Metadata: branch, commit, build URL, change ref | Partial — only short commit |
| 8 | Auto-generated config schema docs | Missing |
