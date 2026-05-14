# F-028 — Scenario-level assertions

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** RUN-002, §9.2

## Summary

`AssertionsConfig.scenarios: Map<String, ScenarioAssertionConfig>` is parsed but never translated into Gatling assertions. Only global assertions reach the `setUp(...).assertions(...)` call.

## Acceptance Criteria

- [ ] For each enabled scenario with an entry in `assertions.scenarios`, the framework emits Gatling assertions scoped to that scenario name (e.g. `details(scenarioName).responseTime().percentile(95).lt(...)`).
- [ ] When a scenario in `assertions.scenarios` is not present in `run.scenarios`, validation fails (configuration error).
- [ ] Tests cover the translation from `ScenarioAssertionConfig` into a Gatling `Assertion[]`.

## Implementation Notes

- Extend `RampageSimulation.buildAssertions` (currently private and global-only) to take both the global block and the scenarios map plus the live `List<ScenarioConfig>`.
- Move the assertion builder into a dedicated `AssertionFactory` class for testability.

## Out of scope

- Custom check-based assertions (e.g. "fail if any request returns 500"). Gatling's `failedRequests().percent()` already covers this.

## Suggested labels

`area:assertions`, `type:feature`, `milestone:M3`
