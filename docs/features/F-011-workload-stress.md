# F-011 — Workload profile: stress

**Milestone:** M2 — Operational Readiness
**PRD references:** §11, RUN-004

## Summary

Stress tests progressively increase load until SLA breach or saturation. `WorkloadFactory` does not implement this profile.

## Acceptance Criteria

- [ ] `WorkloadConfig.type: stress` is accepted and produces a stepped or linearly-increasing injection profile.
- [ ] New fields on `WorkloadConfig` or a new `StressConfig`: `stepRate` (rate increment per step), `stepDuration` (how long to hold each step), `maxRate` (cap).
- [ ] Tests assert the step shape.
- [ ] When combined with global assertions (e.g. `maxResponseTimeP95Millis`), the run fails as soon as the assertion is breached; document that this is the expected use.
- [ ] Sample `config/runs/stress.yaml` demonstrates the profile.

## Implementation Notes

- Two viable shapes:
  1. **Stepped** — `incrementUsersPerSec(stepRate).times(N).eachLevelLasting(stepDuration).startingFrom(start)` (Gatling has helpers for this).
  2. **Linear** — `rampUsersPerSec(from).to(to).during(totalDuration)`.
- Recommend stepped — easier to correlate breach to a load level.

## Out of scope

- Auto-detect-and-back-off behaviour (out of scope for the framework; rely on Gatling assertions).

## Suggested labels

`area:workload`, `type:feature`, `milestone:M2`
