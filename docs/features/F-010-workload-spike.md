# F-010 — Workload profile: spike

**Milestone:** M2 — Operational Readiness
**PRD references:** §11 (Workload Model), RUN-004

## Summary

`WorkloadFactory` does not implement the `spike` workload type. Spike tests apply a sudden load increase to test shock absorption.

## Acceptance Criteria

- [x] `WorkloadConfig.type: spike` is accepted and produces a Gatling injection sequence: warmup at a low constant rate, sudden jump to peak rate, hold for `holdFor`, drop back, optional recovery hold.
- [x] New `WorkloadConfig` fields: `baselineRate` (optional, default `rate.from`), `spikeDuration` (default `1s`).
- [x] Tests assert the produced `OpenInjectionStep[]` matches the expected step sequence for representative inputs.
- [ ] Sample `config/runs/spike.yaml` demonstrates the profile. _(not implemented)_

## Implementation Notes

- A reasonable shape: `constantUsersPerSec(baseline).during(rampUp), rampUsersPerSec(baseline).to(peak).during(spikeDuration), constantUsersPerSec(peak).during(holdFor), rampUsersPerSec(peak).to(baseline).during(spikeDuration)`.
- All durations default-fall-through to existing `parseDuration` defaults; reuse F-004 strict parsing.

## Out of scope

- Closed-model spike (covered by F-012 closed-model + composition).

## Suggested labels

`area:workload`, `type:feature`, `milestone:M2`
