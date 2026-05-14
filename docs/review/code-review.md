# Rampage — Code Review

Author: Paul Snow
Version: 0.0.0
Date: 2026-05-14
Scope: Full repository review against `docs/prd/initial-prd.md`.

## 1. Executive Summary

Rampage is at the end of **PRD Phase 2 (YAML Loading)** with substantial Phase 3, 4, and 5 work already in tree. The framework is functional end-to-end for the MVP happy path: a single read-only GraphQL scenario with a preloaded JDBC feeder and a ramp-and-hold workload. Test coverage is good for the factories (42 passing JUnit tests).

However, the codebase has not yet met the MVP acceptance criteria as a whole. The most significant gaps are:

1. **Reporting is not wired.** `RunMetadataWriter` exists and is unit-tested, but `RampageSimulation` never calls it, so `reporting.writeRunMetadata: true` is silently ignored. This breaks MVP-AC-10 and AC-8 partially.
2. **Validation is shallow.** `ConfigValidator` checks structural presence but not referential integrity. Missing GraphQL files, missing SQL files, undefined `databaseRef`, malformed durations, mutating-vs-production conflicts, and unresolved secrets are all caught (if at all) at runtime — far after the framework should have failed fast (PRD §18).
3. **Secret handling is permissive.** `SecretResolver` returns `""` for a missing required env var rather than failing the run. A missing API token currently produces a literal `Authorization: Bearer ` header. This contradicts MVP-AC-7 and SEC-007.
4. **JSON body construction is unsafe.** `ScenarioFactory.buildVariablesJson` hand-rolls a JSON string from string concatenation. Non-string variables, embedded quotes, and Unicode round-trip incorrectly. Use Jackson.
5. **Phase 6 (Reporting and CI) is essentially untouched** beyond the disconnected `RunMetadataWriter`. No sanitised config snapshot, no CI artifact publishing, no assertion-driven build failure beyond Gatling's own behaviour.

The architecture established in `RampageSimulation` plus the `*Factory` set is clean and matches PRD §5 ("Key Design Opinion"). Continued investment in factories is the right path; resist the temptation to push YAML interpretation into the simulation class itself.

## 2. Repository State Snapshot

| Aspect | Observed |
|---|---|
| Build | Gradle Kotlin DSL, Gradle Wrapper 9.5.1 (via `gradle21w`), Gatling plugin 3.13.5, Java 25 (Adoptium) |
| Source sets | `main` (engine), `gatling` (simulation + classpath YAML), `test` (JUnit 5, AssertJ, Mockito) |
| Tests | 42 passing — `ConfigLoaderTest`, `ConfigValidatorTest`, `FeederFactoryTest`, `SecretResolverTest`, `WorkloadFactoryTest`, `RunMetadataWriterTest` |
| Sample configs | `src/gatling/resources/{environment,run,scenarios/*}.yaml` (classpath) and `config/{environments,runs,scenarios,queries,graphql}/*` (filesystem) |
| Tasks | `validateLoadTest` registered; `gatlingRun` provided by the plugin |
| Dependencies on classpath but unused | HikariCP (declared but `FeederFactory` uses bare `DriverManager`) |

## 3. Architecture Review

### 3.1 What is good

- **Thin simulation, fat factories.** `RampageSimulation` orchestrates; the factories own behaviour. This matches PRD §5 and makes per-feature changes localised.
- **POJO models with Jackson annotations** are appropriate for a Java 25 framework that needs to tolerate field-by-field YAML evolution. `FAIL_ON_UNKNOWN_PROPERTIES=false` lets new YAML fields ship before model classes exist.
- **`ConfigValidator` collects all errors** instead of failing on the first. Good ergonomics for engineers triaging YAML.
- **Three-file split** (environment / run / scenario) is faithfully implemented.

### 3.2 Defects (functional bugs)

| # | File:Line | Issue | Impact |
|---|---|---|---|
| D1 | `RampageSimulation.java:29-92` | `RunMetadataWriter` is never invoked. `reporting.writeRunMetadata: true` has no effect. | MVP-AC-10 fails; SEC-003 fails for the metadata file. |
| D2 | `RampageSimulation.java:71-72` | Scenario-level workload override is a TODO; the branch is empty when `inheritFromRun=false`. | SCN-008 fails silently. |
| D3 | `RampageSimulation.java:41-43` | HTTP protocol is built using `scenarioConfigs.get(0).getEndpointRef()` only. Multi-scenario runs against different endpoints all use the first scenario's base URL. | Blocks post-MVP multi-scenario. |
| D4 | `HttpProtocolFactory.java:60` | Adds header `correlationIdHeader: #{correlationId}` but nothing populates the `correlationId` session var. | Outbound header is unset/empty. |
| D5 | `WorkloadFactory.java:30-35` | `smoke` workload uses `users` but ignores `duration`. The provided `config/runs/smoke.yaml` sets `duration: 30s` — ignored. | RUN-004 partial. |
| D6 | `ScenarioFactory.java:25-36` and `:59-76` | JSON body is hand-built by string escaping. Variables are always quoted as strings; PRD example uses `includeInactive: false` (a boolean) which would produce invalid JSON `"false"`. Embedded backslash/quote handling is naive and likely to break on Unicode. | SCN-003 fragile. |
| D7 | `FeederFactory.java:35-37` | Uses `DriverManager` directly. `PoolConfig` is parsed but never honoured. HikariCP is on the classpath but unused. | FDR-005 partial; pool config ignored. |
| D8 | `SecretResolver.java:24-28, 45-48, 66-70` | Missing env var → returns `""` with a warn. Per SEC-007 / MVP-AC-7, must fail fast when a required secret is unresolved. | Security failure mode. |
| D9 | `ConfigValidatorMain.java:28` vs `RampageSimulation.java:34` | Validator loads scenarios from `ref.getFile()` (filesystem path); simulation loads from `scenarios/<id>.yaml` (classpath). The two can pass-then-fail or vice versa. | `validateLoadTest` is not a reliable preflight. |
| D10 | `RunMetadataWriter.java:31` | `redacted: true` is asserted unconditionally, but the writer never receives the resolved secret state and has no logic to actually redact anything. The flag is a label, not behaviour. | SEC-003 misleading. |
| D11 | `ConfigValidator.java:30-35` | Production check is string-match on `env.id.contains("prod")` only. PRD SEC-007 / RUN failsafe expects a stronger signal. | Brittle safety net. |
| D12 | `HttpProtocolFactory.java:52-56` | Security headers are added via `builder.header(...)`, layered on top of `Authorization`, but per PRD §14 scenario headers must not override `Authorization` unless explicitly allowed. No enforcement exists. | SEC-005 partial. |
| D13 | `RunMetadataWriter.java:65-67` | `ProcessBuilder("git", ...)` runs on every report write. Process spawn is fine for one run, but the writer is reused. Cache the value. | Minor. |
| D14 | `FeederFactory.java:91-96` | "random" strategy shuffles once and then iterates circularly — that is not "random per pull". For long runs, every iteration sees the same shuffled order. | FDR-004 partial. |

### 3.3 Gaps (PRD features not yet started)

- **Workload models**: spike, stress, baseline, closed-mode, rampDown — not implemented (PRD §11, RUN-003, RUN-004).
- **Token refresh / generation** for long-running tests (SEC-006).
- **OAuth client credentials** mode (ENV-003 partial; only bearer-from-env implemented).
- **Body redaction in logs** (SEC-008).
- **Sanitised config snapshot** in reports (§19.3, AC-3).
- **Header precedence enforcement** (§14).
- **Scenario weighting** in multi-scenario runs (post-MVP-AC-2/3; `ScenarioRef.weight` is parsed but unused).
- **Closed-model injection** (`atOnceUsers`, `rampUsers`, etc. — needed for closed model, RUN-003).
- **Dry-run mode** (`run.safety.dryRun` parsed but unused).
- **`failIfEnvironmentAllowsProduction`** (parsed but unused).
- **Feeder validation**: required column presence vs SQL result columns (FDR-002).
- **Feeder row cap** (FDR-006).
- **Variable substitution** in YAML values (`${run:id}`, `${secret:...}`) — referenced in sample YAMLs but `SecretResolver` only handles bare credential refs.
- **Direct JDBC feeder mode** alongside preload (FDR-001/FDR-005 — preload is the only path).
- **`gatling.conf` / `logback.xml`** in `src/gatling/resources/` (PRD §6) — not present, framework runs on Gatling defaults.
- **Reporting**: assertion result publishing, build failure semantics, CI artifact step (Phase 6).
- **Antora documentation site** in `src/docs/` (per repo conventions) — not yet created.

### 3.4 Inconsistencies and minor issues

- `scenario-one.yaml` exists in two locations with slightly different paths (`queries/get-user.graphql` classpath vs `config/graphql/get-user.graphql` filesystem). Document which is canonical.
- `src/main/resources/queries/get-user.graphql` and `config/graphql/get-user.graphql` are byte-identical — duplication risk.
- `ScenarioConfig.protocol` field exists but the code only ever builds GraphQL POST. Either honour `protocol: rest` or document the field as informational only.
- `ScenarioConfig.operationName` is parsed but unused. Either embed in the GraphQL body (`operationName` key) or remove.
- `RequestConfig.bodyTemplate` is parsed but unused.
- `PausesConfig` / `AfterRequestPause` are parsed but not wired into `ScenarioFactory`.
- `ScenarioConfig.tags` are parsed but unused for filtering or reporting.
- The Gatling plugin version `3.13.5` is older than the PRD-recommended `3.15.0.2` (§16). Worth a planned upgrade.
- `tasks.test` opens `java.base/java.lang` via `--add-opens` — fine, but document why (Mockito on Java 25).
- `MetadataConfig.changeReference` is parsed but `RunMetadataWriter.write` does not emit it.

### 3.5 Test coverage gaps

Existing tests are well-targeted at the factories. Missing:

- `ScenarioFactory` — no tests at all. JSON body construction in particular needs property-based or table-driven tests including null, boolean, number, quoted, and Unicode variable values.
- `HttpProtocolFactory` — no tests; should cover endpoint-ref fallback, bearer header injection, missing token, header layering.
- `RampageSimulation` integration — no test exercising the wiring (`gatlingRun` happy path against a local HTTP stub like WireMock).
- `ConfigValidatorMain` — no test for the CLI entry, exit codes, or filesystem-vs-classpath path resolution.

## 4. Build and Dependencies

- Java 25 toolchain is correct per PRD §16.
- Gatling Highcharts 3.13.5 — PRD suggests 3.15.0.2; upgrade is low-risk.
- Jackson 2.18.3, JUnit 5.11.4, AssertJ 3.27.3, Mockito 5.14.2 — fine.
- HikariCP 6.3.0 declared but unreferenced in code. Either wire it into `FeederFactory` or drop the dependency.
- H2 declared as `implementation` (correct, since the validator/feeder may use it as a sample driver) and again as `testImplementation` — pragmatic for the MVP demo.
- `slf4j-api 2.0.16` + `logback-classic 1.5.12` are aligned.
- No JDBC drivers for the PRD-referenced PostgreSQL or other production-shaped databases. Worth documenting that real databases require the user to add their own driver.

## 5. Recommendations (Priority Order)

### Must-fix before declaring MVP

1. Wire `RunMetadataWriter` into `RampageSimulation` and honour `reporting.writeRunMetadata` / `redactSecrets`.
2. Tighten `SecretResolver`: fail when an env-var-sourced credential is required and unset.
3. Replace hand-built JSON in `ScenarioFactory` with Jackson serialisation; treat `variables` map values as typed (boolean, number, string, null) using YAML's native typing.
4. Add file-existence validation for `graphqlQueryFile` and `feeder.sqlFile` in `ConfigValidator`.
5. Unify scenario path resolution between `ConfigValidatorMain` and `RampageSimulation`.
6. Wire `correlationId` session initialisation (or stop adding the header).

### Should-fix in the next milestone

7. Implement scenario-level workload override (the TODO at `RampageSimulation.java:71-72`).
8. Switch JDBC feeder to HikariCP and honour `PoolConfig`.
9. Implement spike, stress, closed-mode workload profiles.
10. Implement dry-run mode and `failIfEnvironmentAllowsProduction`.
11. Enforce header precedence and protect `Authorization` from scenario override.
12. Add Antora docs scaffold under `src/docs/` and migrate this review there.
13. Add `gatling.conf` and `logback.xml` to make logging predictable in CI.

### Nice-to-have

14. Direct JDBC feeder mode (streaming) for very large source datasets.
15. Multi-scenario base URL routing.
16. Scenario weighting.
17. Token refresh + OAuth client-credentials.
18. CI artifact publishing recipe (GitHub Actions).
19. Auto-generated config schema docs.

## 6. Conclusion

The MVP backbone is sound. The remaining work to honestly meet `## 20. Acceptance Criteria — MVP` in the PRD is roughly **two focused sprints**: one closing the validation/security/reporting gaps (items 1–6 above), one closing the workload and operational gaps (items 7–13). See `docs/roadmap/roadmap.md` for the phased plan, and `docs/features/` for ticket-shaped briefs.
