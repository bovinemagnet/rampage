# Load Testing Framework Product Specification

## 1. Purpose

Build a reusable, configuration-driven load testing framework for API and GraphQL workloads using Java 25, Gradle, YAML configuration files, and Gatling as the execution engine.

The framework should let engineers define environments, runs, and scenarios without modifying Java simulation code for every new test. Java should provide the runtime engine, validation, YAML parsing, data feeder integration, request construction, Gatling simulation assembly, and reporting hooks.

## 2. Product Goals

1. Provide a repeatable load testing framework that can run locally, in CI/CD, and eventually in a dedicated performance test environment.
2. Separate environment configuration from run configuration and scenario configuration.
3. Support GraphQL and HTTP API testing using Gatling Java DSL.
4. Support database-backed source data using SQL queries as scenario feeders.
5. Support secure configuration practices for passwords, JWTs, API keys, and authorization headers.
6. Support multiple workload models such as constant rate, ramp-up, spike, stress, soak, and smoke tests.
7. Produce useful run metadata, Gatling reports, logs, and pass/fail quality gates.
8. Make scenario definitions simple enough that developers and testers can add tests without becoming Gatling experts.

## 3. Non-Goals

1. This framework is not a replacement for functional API testing.
2. This framework is not intended to generate production traffic without explicit approval.
3. This framework should not store real secrets directly in repository YAML files.
4. This framework should not mutate production data unless the scenario explicitly declares that behaviour and has governance approval.
5. This framework should not depend on a live source database during peak-load execution unless the database load is intentionally part of the test design.

## 4. Recommended Technology Stack

| Area | Recommendation |
|---|---|
| Language | Java 25 |
| Build Tool | Gradle 9.x using Gradle Wrapper |
| Load Engine | Gatling Java DSL |
| Configuration | YAML parsed into strongly typed Java records/classes |
| YAML Parser | Jackson YAML |
| Validation | Jakarta Validation or custom validator layer |
| Database Access | JDBC or HikariCP for feeder preparation |
| Reports | Gatling HTML reports, JSON summaries, CI artifacts |
| Secrets | Environment variables, CI secret store, Vault, AWS Secrets Manager, Azure Key Vault, or equivalent |

## 5. Key Design Opinion

Do not make Gatling simulations read complex YAML directly everywhere. Keep Gatling code thin, but not YAML-driven to the point of becoming an undocumented mini-language.

The recommended architecture is:

1. YAML describes environment, run, scenario, feeder, and workload intent.
2. Java loads and validates YAML into typed configuration objects.
3. Java maps typed configuration into Gatling protocols, feeders, requests, checks, and injection profiles.
4. Scenario Java classes remain generic and reusable.
5. Complex behaviour that cannot be expressed clearly in YAML should be implemented as Java scenario components.

This avoids the common trap where a YAML test framework becomes harder to understand than normal code.

## 6. Repository Layout

```text
load-testing/
  settings.gradle.kts
  build.gradle.kts
  gradle/
  gradlew
  gradlew.bat

  src/
    gatling/
      java/
        com/example/loadtest/
          ConfigDrivenSimulation.java
          config/
            EnvironmentConfig.java
            RunConfig.java
            ScenarioConfig.java
            ConfigLoader.java
            ConfigValidator.java
          engine/
            SimulationFactory.java
            ScenarioFactory.java
            FeederFactory.java
            WorkloadFactory.java
            HttpProtocolFactory.java
          security/
            SecretResolver.java
            TokenProvider.java
          reporting/
            RunMetadataWriter.java
      resources/
        gatling.conf
        logback.xml

  config/
    environments/
      local.yaml
      dev.yaml
      test.yaml
      perf.yaml
    runs/
      smoke.yaml
      baseline.yaml
      load.yaml
      soak.yaml
      stress.yaml
    scenarios/
      scenario-one.yaml
      scenario-two.yaml
    queries/
      scenario-one-data.sql
    graphql/
      scenario-one.graphql

  docs/
    usage.md
    scenario-authoring.md
    security.md

  build/
    reports/
```

## 7. Configuration Model

The framework should use three primary YAML file types:

1. `environment.yaml` — defines where and how the framework connects.
2. `run.yaml` — defines what is being run, at what scale, and with which quality gates.
3. `scenario-one.yaml` — defines the individual scenario, request, feeder, checks, and workload hints.

A run may reference one or more scenarios. A scenario may reference GraphQL files, SQL files, request templates, and checks.

## 8. Environment Configuration

### 8.1 Purpose

Environment configuration describes target systems, shared headers, authentication strategy, database connections, timeouts, and environment-specific defaults.

It should not contain raw passwords, long-lived JWTs, or production credentials directly. It may contain secret references.

### 8.2 Example: `config/environments/perf.yaml`

```yaml
id: perf
name: Performance Test Environment

baseUrls:
  graphql: https://api-perf.example.com/graphql
  rest: https://api-perf.example.com

http:
  connectTimeoutMillis: 5000
  requestTimeoutMillis: 30000
  followRedirects: false
  acceptHeader: application/json
  contentTypeHeader: application/json
  userAgent: gatling-load-test/${RUN_ID}

security:
  mode: bearer-token
  token:
    source: env
    envVar: PERF_API_JWT
  headers:
    Authorization: Bearer ${secret:PERF_API_JWT}
    X-Correlation-Source: gatling
    X-Test-Run-Id: ${run:id}

# Prefer secret references, not raw passwords.
databases:
  sourceData:
    driverClassName: org.postgresql.Driver
    jdbcUrl: jdbc:postgresql://perf-db.example.com:5432/source_data
    username:
      source: env
      envVar: PERF_DB_USERNAME
    password:
      source: env
      envVar: PERF_DB_PASSWORD
    pool:
      maximumPoolSize: 5
      connectionTimeoutMillis: 5000
      idleTimeoutMillis: 30000

observability:
  correlationIdHeader: X-Correlation-Id
  includeRunMetadataHeaders: true

safety:
  allowProduction: false
  requireApprovalForMutatingRequests: true
```

### 8.3 Environment Requirements

| ID | Requirement |
|---|---|
| ENV-001 | Environment files must support multiple named base URLs. |
| ENV-002 | Environment files must support shared HTTP headers. |
| ENV-003 | Environment files must support authentication by static bearer token, generated JWT, OAuth client credentials, or custom token provider. |
| ENV-004 | Environment files must support database connection definitions for feeder data. |
| ENV-005 | Environment files must not require raw secrets to be committed to source control. |
| ENV-006 | Environment files must support request timeout and connection timeout settings. |
| ENV-007 | Environment files must support safety controls such as production protection and mutating request approval. |

## 9. Run Configuration

### 9.1 Purpose

Run configuration defines what constitutes a test run: scenarios, scale, duration, workload profile, reporting, tags, and pass/fail criteria.

A run should be executable from Gradle using simple parameters:

```bash
./gradlew gatlingRun \
  -Denv=perf \
  -Drun=load
```

or:

```bash
./gradlew gatlingRun-com.example.loadtest.ConfigDrivenSimulation \
  -Dloadtest.env=config/environments/perf.yaml \
  -Dloadtest.run=config/runs/load.yaml
```

### 9.2 Example: `config/runs/load.yaml`

```yaml
id: customer-graphql-load
name: Customer GraphQL Load Test
version: 1

environment: perf

metadata:
  owner: platform-performance
  application: customer-api
  service: customer-graphql
  changeReference: CHG-123456
  description: Baseline customer GraphQL query load test.

defaults:
  protocol: graphql
  maxDuration: 30m
  pause:
    strategy: uniform
    minMillis: 250
    maxMillis: 1500

scenarios:
  - id: scenario-one
    file: config/scenarios/scenario-one.yaml
    enabled: true
    weight: 100

execution:
  mode: open
  workload:
    type: ramp-and-hold
    rampUp: 5m
    holdFor: 20m
    rampDown: 5m
    rate:
      unit: per-second
      from: 1
      to: 50

assertions:
  global:
    maxResponseTimeP95Millis: 1000
    maxResponseTimeP99Millis: 2500
    maxErrorPercentage: 1.0
  scenarios:
    scenario-one:
      maxResponseTimeP95Millis: 800
      maxErrorPercentage: 0.5

reporting:
  outputDirectory: build/reports/gatling
  writeRunMetadata: true
  includeConfigSnapshot: true
  redactSecrets: true

safety:
  dryRun: false
  requireConfirmation: false
  failIfEnvironmentAllowsProduction: true
```

### 9.3 Run Requirements

| ID | Requirement |
|---|---|
| RUN-001 | A run file must reference one or more scenario files. |
| RUN-002 | A run file must support global and scenario-level assertions. |
| RUN-003 | A run file must support open and closed workload models where Gatling supports them. |
| RUN-004 | A run file must support smoke, baseline, load, stress, spike, and soak profiles. |
| RUN-005 | A run file must support run metadata for reporting and traceability. |
| RUN-006 | A run file must support dry-run validation without executing load. |
| RUN-007 | A run file must allow disabled scenarios to remain in the file for future use. |

## 10. Scenario Configuration

### 10.1 Purpose

A scenario configuration defines a business or technical interaction to exercise under load. For GraphQL, it should define the operation, query file, variables, headers, feeder, request checks, and scenario-specific workload overrides.

### 10.2 Example: `config/scenarios/scenario-one.yaml`

```yaml
id: scenario-one
name: Customer lookup by account number
protocol: graphql
method: POST
endpointRef: graphql
operationName: CustomerByAccountNumber

description: >
  Executes a GraphQL customer lookup using account numbers read from
  the source data database.

headers:
  X-Scenario-Id: scenario-one
  X-Business-Process: customer-lookup

request:
  graphqlQueryFile: config/graphql/customer-by-account-number.graphql
  variables:
    accountNumber: ${feeder:account_number}
    includeInactive: false
  bodyTemplate: graphql-json

feeder:
  type: jdbc
  databaseRef: sourceData
  sqlFile: config/queries/customer-account-numbers.sql
  strategy: circular
  preload: true
  failIfEmpty: true
  columns:
    account_number:
      type: string
      required: true
      sessionKey: account_number

checks:
  httpStatus: 200
  jsonPath:
    - path: $.errors
      expectation: absentOrEmpty
    - path: $.data.customer.accountNumber
      expectation: equalsSession
      sessionKey: account_number
    - path: $.data.customer.id
      expectation: exists

workload:
  inheritFromRun: true
  rate:
    unit: per-second
    from: 1
    to: 50
  rampUp: 5m
  holdFor: 20m

pauses:
  beforeRequestMillis: 0
  afterRequest:
    strategy: uniform
    minMillis: 250
    maxMillis: 1500

tags:
  - graphql
  - customer
  - read-only

safety:
  mutating: false
  idempotent: true
```

### 10.3 Example GraphQL Query File

`config/graphql/customer-by-account-number.graphql`

```graphql
query CustomerByAccountNumber($accountNumber: String!, $includeInactive: Boolean!) {
  customer(accountNumber: $accountNumber, includeInactive: $includeInactive) {
    id
    accountNumber
    status
    name
  }
}
```

### 10.4 Example SQL Feeder Query

`config/queries/customer-account-numbers.sql`

```sql
select account_number
from load_test_customer_accounts
where active = true
order by account_number;
```

### 10.5 Scenario Requirements

| ID | Requirement |
|---|---|
| SCN-001 | Scenario files must support GraphQL POST requests. |
| SCN-002 | Scenario files must support external GraphQL query files. |
| SCN-003 | Scenario files must support GraphQL variables populated from feeders. |
| SCN-004 | Scenario files must support SQL-backed JDBC feeders. |
| SCN-005 | Scenario files must support feeder strategies such as queue, shuffle, random, and circular where supported. |
| SCN-006 | Scenario files must support request-specific headers. |
| SCN-007 | Scenario files must support HTTP status checks and JSONPath checks. |
| SCN-008 | Scenario files must support scenario-specific workload overrides. |
| SCN-009 | Scenario files must indicate whether the scenario is mutating or read-only. |
| SCN-010 | Scenario files must support tags for filtering and reporting. |

## 11. Workload Model

The framework should expose workload intent in YAML and translate it into Gatling injection profiles.

Supported workload types:

| Type | Description |
|---|---|
| smoke | Minimal traffic to validate wiring, auth, query syntax, and data feeder availability. |
| baseline | Known normal traffic pattern used as a comparison point. |
| ramp-and-hold | Gradually increase traffic and hold at a target rate. |
| spike | Sudden increase in traffic to test shock absorption. |
| stress | Increase traffic until SLA breach or system saturation. |
| soak | Sustained traffic over a long duration to detect memory leaks, pool exhaustion, or degradation. |

Recommended initial implementation:

1. Smoke
2. Ramp-and-hold
3. Soak
4. Stress

Spike can come later because it tends to need more careful operational coordination.

## 12. Data Feeder Strategy

### 12.1 JDBC Feeder

The scenario can use SQL to obtain source data from a configured database. The framework should support two execution modes:

1. Direct JDBC feeder mode — Gatling reads from JDBC feeder at runtime.
2. Preload mode — framework executes SQL before the run, stores records in memory or a temporary CSV/JSON feeder, and Gatling consumes that prepared dataset.

### 12.2 Recommended Default

Use preload mode by default.

Reason: load tests should primarily test the target API, not accidentally overload the source data database. Direct JDBC feeders are useful, but they make the source database part of the test path and can distort results.

### 12.3 Feeder Requirements

| ID | Requirement |
|---|---|
| FDR-001 | The framework must support JDBC feeder data from SQL files. |
| FDR-002 | The framework must validate that required feeder columns are present. |
| FDR-003 | The framework must fail fast if a feeder marked `failIfEmpty` returns zero rows. |
| FDR-004 | The framework must support queue, shuffle, random, and circular strategies where applicable. |
| FDR-005 | The framework should support preloading feeder data before starting traffic. |
| FDR-006 | The framework should support maximum row limits to prevent accidental huge feeder loads. |
| FDR-007 | The framework should log feeder row counts without logging sensitive row values. |

## 13. Security Requirements

| ID | Requirement |
|---|---|
| SEC-001 | Raw passwords, JWTs, API keys, and client secrets must not be committed to source control. |
| SEC-002 | YAML files may reference secrets by environment variable name or secret-store path. |
| SEC-003 | Reports and config snapshots must redact secret values. |
| SEC-004 | Authorization headers must be injectable from environment-level security configuration. |
| SEC-005 | The framework must support per-scenario headers layered on top of environment headers. |
| SEC-006 | The framework must support token refresh or token generation for long-running tests. |
| SEC-007 | The framework must prevent accidental production execution unless explicitly allowed. |
| SEC-008 | The framework must avoid logging full request bodies when they contain sensitive data. |

## 14. Configuration Resolution Rules

Configuration should be resolved in this order:

1. Built-in defaults.
2. Environment YAML.
3. Run YAML.
4. Scenario YAML.
5. JVM system properties, such as `-Dloadtest.env=...`.
6. Environment variables.
7. Secret resolver output.

Later layers override earlier layers only where explicitly allowed.

Header precedence should be:

1. Framework-required headers.
2. Environment shared headers.
3. Run-level headers.
4. Scenario-level headers.
5. Request-specific headers.

The framework should detect and reject unsafe overrides, such as replacing `Authorization` in a scenario unless the scenario explicitly permits it.

## 15. Command-Line Interface

Minimum useful commands:

```bash
# Validate all configuration without running load
./gradlew validateLoadTest \
  -Dloadtest.env=config/environments/perf.yaml \
  -Dloadtest.run=config/runs/load.yaml

# Run a smoke test
./gradlew gatlingRun \
  -Dloadtest.env=config/environments/perf.yaml \
  -Dloadtest.run=config/runs/smoke.yaml

# Run a named simulation
./gradlew gatlingRun-com.example.loadtest.ConfigDrivenSimulation \
  -Dloadtest.env=config/environments/perf.yaml \
  -Dloadtest.run=config/runs/load.yaml

# Dry-run scenario expansion and feeder validation
./gradlew validateLoadTest \
  -Dloadtest.env=config/environments/perf.yaml \
  -Dloadtest.run=config/runs/load.yaml \
  -Dloadtest.dryRun=true
```

## 16. Gradle Build Requirements

The Gradle build should:

1. Use the Gradle Wrapper.
2. Use Java toolchains targeting Java 25.
3. Apply the Gatling Gradle plugin.
4. Include Jackson YAML.
5. Include the required JDBC drivers.
6. Include validation and test dependencies.
7. Expose a `validateLoadTest` task.
8. Produce CI-friendly reports and artifacts.

Example `build.gradle.kts` skeleton:

```kotlin
plugins {
    java
    id("io.gatling.gradle") version "3.15.0.2"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    gatling("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.20.0")
    gatling("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
    gatling("org.postgresql:postgresql:42.7.8")
    gatling("com.zaxxer:HikariCP:7.0.2")
    gatling("org.slf4j:slf4j-api:2.0.17")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
    testImplementation("org.assertj:assertj-core:3.27.6")
}

tasks.register("validateLoadTest", JavaExec::class) {
    group = "verification"
    description = "Validates load test YAML configuration and feeder availability."
    classpath = sourceSets["gatling"].runtimeClasspath
    mainClass.set("com.example.loadtest.config.ConfigValidatorMain")
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
}
```

Dependency versions should be managed centrally and reviewed regularly.

## 17. Runtime Flow

1. Gradle starts `ConfigDrivenSimulation`.
2. Simulation reads system properties to locate environment and run YAML files.
3. Config loader parses YAML into typed Java objects.
4. Config validator validates environment, run, scenario, security, feeder, and workload configuration.
5. Secret resolver resolves secret references.
6. Feeder factory prepares scenario feeders.
7. HTTP protocol factory builds Gatling protocol configuration.
8. Scenario factory builds Gatling scenario chains.
9. Workload factory maps YAML workload into Gatling injection steps.
10. Gatling executes simulation.
11. Reports and run metadata are written.
12. Assertions determine pass/fail status.

## 18. Validation Rules

The framework must fail before starting load if:

1. Required YAML files are missing.
2. YAML contains unknown required fields or invalid enum values.
3. A scenario references a missing GraphQL file or SQL file.
4. A JDBC feeder query returns no rows and `failIfEmpty` is true.
5. A required secret cannot be resolved.
6. A mutating scenario targets an environment that disallows mutation.
7. A run attempts to target production without explicit approval.
8. Workload duration or rate values are invalid.
9. Assertions are malformed.
10. A scenario has no checks at all unless explicitly permitted.

## 19. Reporting

Each run should produce:

1. Standard Gatling HTML report.
2. Machine-readable run metadata JSON.
3. Sanitised resolved configuration snapshot.
4. Scenario list and effective workload summary.
5. Feeder row counts.
6. Assertion results.
7. Build/commit metadata where available.

Example run metadata:

```json
{
  "runId": "customer-graphql-load-20260514-101530",
  "environment": "perf",
  "runConfig": "config/runs/load.yaml",
  "scenarios": ["scenario-one"],
  "gitCommit": "abc1234",
  "startedAt": "2026-05-14T10:15:30+10:00",
  "redacted": true
}
```

## 20. Acceptance Criteria

### MVP Acceptance Criteria

1. A developer can run a smoke test from Gradle using Java 25.
2. The simulation can load `environment.yaml`, `run.yaml`, and one scenario YAML file.
3. The scenario can execute a GraphQL request using a query loaded from a `.graphql` file.
4. The scenario can populate GraphQL variables from a SQL-backed feeder.
5. The run can define ramp-up, hold duration, and request rate.
6. The framework can inject authorization headers from secret references.
7. The framework fails fast when required secrets are unavailable.
8. The framework generates a Gatling report.
9. The framework applies at least one global assertion for error percentage or response time.
10. The framework redacts secrets in logs and report metadata.

### Post-MVP Acceptance Criteria

1. Multiple scenarios can be executed in one run.
2. Scenario weighting is supported.
3. OAuth client-credentials token generation is supported.
4. Token refresh is supported for long-running tests.
5. CI/CD pipeline integration publishes reports as build artifacts.
6. Scenario templates can be generated from a command-line task.
7. Report metadata includes git branch, commit hash, build URL, and change reference.
8. Config schema documentation is generated automatically.

## 21. Implementation Phases

### Phase 1 — Skeleton

1. Create Gradle project.
2. Add Gatling Gradle plugin.
3. Add Java 25 toolchain.
4. Add sample `ConfigDrivenSimulation`.
5. Add a hard-coded GraphQL smoke test.

### Phase 2 — YAML Loading

1. Add Jackson YAML.
2. Create typed config classes.
3. Load environment, run, and scenario files.
4. Add validation and useful error messages.

### Phase 3 — Security and Headers

1. Implement secret resolution from environment variables.
2. Implement authorization header injection.
3. Implement redaction utilities.
4. Add safety controls for mutating requests and production protection.

### Phase 4 — Feeders

1. Add JDBC connection support.
2. Add SQL file loading.
3. Add feeder preloading.
4. Validate required columns.
5. Support queue, random, shuffle, and circular strategies.

### Phase 5 — Workload Profiles

1. Implement smoke profile.
2. Implement ramp-and-hold profile.
3. Implement soak profile.
4. Implement stress profile.
5. Add global and scenario assertions.

### Phase 6 — Reporting and CI

1. Write run metadata JSON.
2. Save sanitised config snapshot.
3. Publish Gatling report artifacts in CI.
4. Add build failure on assertion failure.
5. Add documentation.

## 22. Open Questions

1. Which environments are allowed for load testing?
2. Is production load testing ever permitted?
3. Which secret manager should be used beyond environment variables?
4. Should SQL feeders be direct JDBC feeders or preloaded snapshots by default?
5. How much source data is required for realistic test runs?
6. Are GraphQL mutations in scope?
7. What are the initial response time and error-rate SLOs?
8. Should the framework support REST scenarios in the MVP, or only GraphQL?
9. Will tests run only locally/CI, or also in Gatling Enterprise/private load generators?
10. What observability platform should the run metadata integrate with?

## 23. Initial Recommendation

Build the MVP around one read-only GraphQL scenario with a preloaded JDBC feeder and a ramp-and-hold workload. This gives a useful vertical slice without overengineering the YAML language too early.

Once the first scenario is working end to end, add multiple scenarios, token refresh, CI publishing, and richer workload models.


## 24. Short Build Overview for Copilot

Build a Java 25, Gradle-based load testing framework that uses Gatling Java DSL as the execution engine and YAML files as the test definition layer.

The framework should load three main configuration types:

1. `environment.yaml` — defines the target environment, base URLs, shared HTTP headers, authentication mode, secret references, source database connections, timeout settings, and safety controls.
2. `run.yaml` — defines the test run, selected scenarios, workload model, ramp-up, hold duration, request rate, assertions, reporting options, and run metadata.
3. `scenario-one.yaml` — defines one executable scenario, including the GraphQL endpoint, GraphQL query file, variables, SQL feeder query, request headers, checks, tags, and scenario-specific workload settings.

Use Java classes to parse the YAML into strongly typed configuration objects, validate the configuration before execution, resolve secrets from environment variables or an external secret provider, prepare feeder data from SQL, and then translate the resolved configuration into Gatling protocols, scenarios, feeders, injection profiles, and assertions.

Do not store raw passwords, JWTs, API keys, or authorization tokens in YAML. YAML should only contain references to secrets, such as environment variable names or secret-manager paths. Logs, reports, and resolved configuration snapshots must redact sensitive values.

The initial MVP should support one read-only GraphQL scenario using a `.graphql` query file and a SQL-backed feeder. It should support a ramp-and-hold workload, configurable calls per second, basic JSONPath checks, global response-time and error-rate assertions, and standard Gatling HTML report output.

Recommended implementation order:

1. Create the Gradle project with Java 25 toolchain and Gatling plugin.
2. Add a minimal hard-coded Gatling GraphQL smoke test.
3. Add YAML parsing with Jackson YAML.
4. Add typed config objects for environment, run, scenario, feeder, security, and workload settings.
5. Add validation with clear startup errors.
6. Add secret resolution and header construction.
7. Add SQL feeder loading and preloading.
8. Add workload mapping from YAML to Gatling injection profiles.
9. Add Gatling assertions and report metadata output.
10. Add tests for YAML parsing, validation, secret redaction, and workload mapping.

The code should keep Gatling simulation classes thin. Business-specific behaviour should live in reusable factories such as `ConfigLoader`, `ConfigValidator`, `SecretResolver`, `FeederFactory`, `HttpProtocolFactory`, `ScenarioFactory`, and `WorkloadFactory`.
