# F-020 — Scenario weighting in run config

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** Post-MVP-AC-2

## Summary

`ScenarioRef.weight` is parsed (default 100) but never consulted. Multi-scenario runs today inject all scenarios at full configured load, ignoring intended traffic mix.

## Acceptance Criteria

- [x] Weights are interpreted as relative proportions of the run-level rate.
- [x] For open-model, the per-scenario rate is `runRate * weight / sum(weights)`.
- [x] For closed-model, the per-scenario user count is `runUsers * weight / sum(weights)`, with at least 1 user per enabled scenario.
- [x] Disabled scenarios are excluded from the weight sum.
- [x] Scenarios with their own workload override (F-017) are excluded from the weighted split and their override is honoured as-is.
- [ ] Tests cover: equal weights, skewed weights, single scenario, mixed override+weighted. _(not implemented — only the `scaleWorkload` maths is unit-tested; the weighted-split cases in `RampageSimulation` are not covered)_

## Implementation Notes

- Apply weighting inside `WorkloadFactory` by scaling the `RateConfig` / `WorkloadConfig.users` after the run-vs-scenario decision in F-017.
- Document the rounding rule (floor with at least 1).

## Out of scope

- Time-varying weights (e.g. ramp up scenario A first, then B).

## Suggested labels

`area:workload`, `type:feature`, `milestone:M3`
