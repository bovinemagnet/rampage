# Code Review

Date: 2026-05-28

Scope: full repository scan of the Rampage Gradle JVM project, including the core engine, Gatling simulation, Quarkus console, build configuration, tests, and documentation/runtime behaviour alignment.

## Findings

### High: Step-Level `endpointRef` Does Not Route to Per-Step Base URLs

Documentation states that each step may set `endpointRef` to route to a different `baseUrls` entry, but runtime wiring selects one `HttpProtocolBuilder` per scenario in `src/gatling/java/io/rampage/simulation/RampageSimulation.java`. `src/main/java/io/rampage/factory/RequestBuilder.java` only uses step `endpointRef` when deriving a default path, not when selecting the base URL.

Impact: multi-service flows send all steps to the scenario-level base URL.

Recommendation: either split multi-endpoint steps into separate scenario populations with distinct protocols, or enforce one endpoint per scenario and update docs/config validation accordingly.

### High: Report Metadata Is Written Outside Simulation Directories

`RunMetadataWriter` writes `run-metadata.json` directly to `build/reports/gatling`, while `RunResultIngestor` reads metadata from each `rampagesimulation-*` directory. This means console-imported runs miss their metadata, and each run can overwrite the previous root-level metadata file.

Impact: run history can lose run name, environment, git commit, branch, and config key data.

Recommendation: write metadata and config snapshots into the actual Gatling simulation directory, or teach the ingestor how to associate root-level metadata safely without overwrite risk.

### High: Feeder Failures Are Swallowed

`FeederFactory` throws for empty feeders, missing required columns, SQL failures, and row-limit violations. `RampageSimulation` catches all feeder exceptions, logs a warning, and continues without attaching a feeder.

Impact: runs may pass while requests contain unresolved `#{...}` values or use invalid data, defeating preflight safety options like `failIfEmpty=true`.

Recommendation: treat feeder setup failures as fatal during simulation initialisation unless a scenario explicitly opts into best-effort feeder behaviour.

### Medium: YAML Placeholder Expansion Is Narrower Than Documented

Docs say string values in environment, run, and scenario YAML are expanded. `PlaceholderSubstitutor.expandInPlace` currently expands only security headers, run headers, scenario headers, and scenario descriptions.

Impact: placeholders in `baseUrls`, request paths/bodies, metadata, SQL paths, GraphQL paths, or other string fields remain literal.

Recommendation: implement model-wide string expansion, or narrow the documentation and validator messages to the fields that are actually supported.

### Medium: Step-Level GraphQL Query Files Are Ignored

`RampageSimulation` loads only the top-level scenario `request.graphqlQueryFile`, then passes the same query to every step. `RequestBuilder.buildGraphqlBody` also reads variables from the top-level scenario request instead of the current step request.

Impact: multi-step GraphQL scenarios send the wrong query or an empty query for step-local GraphQL requests.

Recommendation: load body/query resources per step and build GraphQL envelopes from the step request when steps are present.

### Medium: Unknown `endpointRef` Values Silently Fall Back

`HttpProtocolFactory` falls back to `rest`, then the first configured base URL, then localhost when an `endpointRef` is missing. Documentation says `endpointRef` must match `environment.baseUrls`.

Impact: traffic can be sent to the wrong target instead of failing fast.

Recommendation: add validation for scenario and step endpoint references against `environment.baseUrls`, and remove silent fallback for explicitly configured refs.

## Verification

- `./gradlew test :console:test --no-daemon` passed.
- `./gradlew validateLoadTest -Dloadtest.env=config/environments/local.yaml -Dloadtest.run=config/runs/smoke.yaml --no-daemon` passed after allowing Gradle wrapper/cache access.
- Playwright E2E tests were not run.

## Notes

The findings above focus on behavioural risks and doc/runtime mismatches. No code changes were made as part of this review document.
