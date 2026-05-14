# F-017 — Scenario-level workload override

**Milestone:** M2 — Operational Readiness
**PRD references:** SCN-008
**Traces to:** D2 in `docs/review/code-review.md`

## Summary

`RampageSimulation.java:71-72` contains an explicit TODO — the branch handling `scenario.workload.inheritFromRun: false` is empty. Scenario-level workload overrides are advertised but do nothing.

## Acceptance Criteria

- [ ] When a scenario sets `workload.inheritFromRun: false`, `WorkloadFactory.buildInjection` is called with that scenario's `ScenarioWorkloadConfig` translated into a `WorkloadConfig`.
- [ ] When `inheritFromRun: true` (default), the run-level workload is used (existing behaviour).
- [ ] When `inheritFromRun: false` and the scenario has no usable workload fields (no type, no rate), validation fails (F-004 territory).
- [ ] Tests cover all three cases.

## Implementation Notes

- Add a helper `WorkloadConfig.fromScenarioOverride(ScenarioWorkloadConfig)` that copies type/rate/rampUp/holdFor onto a `WorkloadConfig`.
- The promotion can also live in `WorkloadFactory` as `effectiveWorkload(RunConfig, ScenarioConfig)`.

## Out of scope

- Mixed scenarios where some override and some inherit — that already works once this is implemented (each scenario is resolved independently).

## Suggested labels

`area:workload`, `area:simulation`, `type:bug`, `milestone:M2`
