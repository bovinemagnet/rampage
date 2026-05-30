# F-038 — RampageSimulation integration test against WireMock

**Milestone:** M4 — Reporting, CI, and DX
**Traces to:** §3.5 in `docs/review/code-review.md`

## Summary

The factories are well-tested in isolation but `RampageSimulation` itself has no integration test. Regressions in wiring (e.g. the D1 bug) slip through because no test exercises a full happy-path run.

## Acceptance Criteria

- [x] A new test (or a separate Gradle task `integrationTest`) starts a WireMock server, configures it to respond to the GraphQL POST, runs `RampageSimulation` against it with a smoke workload, and asserts:
  - [x] The HTTP report directory exists.
  - [x] `run-metadata.json` is produced (depends on F-001).
  - [x] WireMock received at least N requests.
  - [x] The request body parses as valid JSON with the expected `query` and `variables`.
- [x] The test does not require network access.
- [x] Runs in under 30 seconds.
- [x] Wired into CI (F-031).

## Status

Complete. `src/test/java/io/rampage/integration/RampageSimulationWireMockIntegrationTest.java`
(tagged `integration`) drives Gatling in-process via `io.gatling.app.Gatling.fromArgs`, which runs
synchronously and returns a status code without calling `System.exit`. The test writes temporary
environment / run / scenario YAML into a `@TempDir`, points `baseUrls.graphql` at WireMock's dynamic
port through `-Dloadtest.env` / `-Dloadtest.run`, and uses a feeder-less smoke scenario so no database
is required.

It is excluded from the fast `test` suite and run by the dedicated `integrationTest` Gradle task,
which adds the `gatling` source set (where `RampageSimulation` lives) to the test classpath. CI runs
it as a separate step after `build test`.

## Implementation Notes

- Either drive Gatling programmatically with a `setUp` invocation that returns synchronously, or invoke `gatlingRun` via the Gradle test runner. The former is faster.
- Use `wiremock-jre8` or the newer `wiremock-standalone`; pick whichever has a Java 25-compatible release.

## Out of scope

- Performance regression detection (Gatling's own assertions cover that).

## Suggested labels

`area:simulation`, `area:test`, `type:test`, `milestone:M4`
