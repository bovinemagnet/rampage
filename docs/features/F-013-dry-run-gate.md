# F-013 — Dry-run gate on gatlingRun

**Milestone:** M2 — Operational Readiness
**PRD references:** RUN-006, §15
**Traces to:** §3.3 gap in `docs/review/code-review.md`

## Summary

`RunSafetyConfig.dryRun` is parsed but never consulted. PRD §15 also references `-Dloadtest.dryRun=true`. Today, the only way to validate without firing traffic is the separate `validateLoadTest` task.

## Acceptance Criteria

- [ ] When `run.safety.dryRun: true` (or `-Dloadtest.dryRun=true`), `RampageSimulation` performs all loading, validation, feeder preload, and protocol/scenario construction, then exits **before** calling `setUp(...)`. _(not implemented)_
- [ ] A summary is printed to stdout: env id, run id, scenarios, expected workload shape, feeder row counts, resolved global assertions. _(not implemented)_
- [x] The summary is also written as `dry-run-summary.json` under `reporting.outputDirectory`.
- [x] The Gradle task `validateLoadTest` calls the same dry-run code path (or is documented as equivalent).
- [ ] Tests cover the dry-run path with and without each toggle. _(not implemented)_

## Implementation Notes

- Easiest approach: extract the initializer body of `RampageSimulation` into a separate `SimulationAssembly` builder that returns a "would-be" `Setup`. On dry-run, do not call `setUp(...)`; instead serialise the assembly to JSON.
- The system property override takes precedence over the YAML field.

## Out of scope

- Replay / record of historical runs.

## Suggested labels

`area:safety`, `area:cli`, `type:feature`, `milestone:M2`
