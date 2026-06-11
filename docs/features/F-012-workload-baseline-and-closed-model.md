# F-012 — Workload profile: baseline + closed-model injection

**Milestone:** M2 — Operational Readiness
**PRD references:** §11, RUN-003, RUN-004

## Summary

PRD §11 lists `baseline` as a workload type, and RUN-003 requires closed-model support. Neither is implemented. Closed model (fixed concurrent users, repeating) is a fundamentally different injection style than open model (arrivals per second).

## Acceptance Criteria

- [x] `WorkloadConfig.type: baseline` is accepted and emits a constant-rate profile sized to the established "normal" traffic for the SUT (no special logic; alias for `constant` with documentation).
- [x] `ExecutionConfig.mode: closed` is accepted alongside `open`.
- [x] In closed mode, `WorkloadFactory` emits `ClosedInjectionStep[]` instead of `OpenInjectionStep[]`. Supported shapes:
  - `atOnceUsers(N)` for smoke
  - `constantConcurrentUsers(N).during(D)` for soak/constant
  - `rampConcurrentUsers(from).to(to).during(D)` for ramp-and-hold
- [x] `RampageSimulation` chooses `injectOpen` vs `injectClosed` based on `ExecutionConfig.mode`.
- [x] Tests cover open and closed expansions for each workload type that supports both.

## Implementation Notes

- Define two sibling builder methods in `WorkloadFactory`: `buildOpenInjection(...)` and `buildClosedInjection(...)`. The dispatch happens in `RampageSimulation` (or a new `InjectionStrategy` interface).
- For workload types that only make sense in one mode (e.g. `spike` is awkward in closed model), fail validation rather than emit nonsense.

## Out of scope

- Mixed open/closed within one run (not in PRD).

## Suggested labels

`area:workload`, `type:feature`, `milestone:M2`
