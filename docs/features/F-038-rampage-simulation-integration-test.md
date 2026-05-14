# F-038 — RampageSimulation integration test against WireMock

**Milestone:** M4 — Reporting, CI, and DX
**Traces to:** §3.5 in `docs/review/code-review.md`

## Summary

The factories are well-tested in isolation but `RampageSimulation` itself has no integration test. Regressions in wiring (e.g. the D1 bug) slip through because no test exercises a full happy-path run.

## Acceptance Criteria

- [ ] A new test (or a separate Gradle task `integrationTest`) starts a WireMock server, configures it to respond to the GraphQL POST, runs `RampageSimulation` against it with a smoke workload, and asserts:
  - The HTTP report directory exists.
  - `run-metadata.json` is produced (depends on F-001).
  - WireMock received at least N requests.
  - The request body parses as valid JSON with the expected `query` and `variables`.
- [ ] The test does not require network access.
- [ ] Runs in under 30 seconds.
- [ ] Wired into CI (F-031).

## Implementation Notes

- Either drive Gatling programmatically with a `setUp` invocation that returns synchronously, or invoke `gatlingRun` via the Gradle test runner. The former is faster.
- Use `wiremock-jre8` or the newer `wiremock-standalone`; pick whichever has a Java 25-compatible release.

## Out of scope

- Performance regression detection (Gatling's own assertions cover that).

## Suggested labels

`area:simulation`, `area:test`, `type:test`, `milestone:M4`
