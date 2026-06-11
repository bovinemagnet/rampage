# F-030 — Effective workload + scenario summary in run metadata

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** §19

## Summary

`run-metadata.json` today lists scenarios by id/name. PRD §19 calls for a "scenario list and effective workload summary" — readers should be able to tell what the run *actually did* (after weighting, overrides, and dry-run substitutions) without running it.

## Acceptance Criteria

- [x] `RunMetadataWriter` includes a per-scenario block:
  - `effectiveWorkload`: type, rampUp, holdFor, rate (or users), source (`run|scenario-override`)
  - `effectiveAssertions`: scenario-level assertions actually applied
  - `feederRowCount`: rows preloaded (or `streaming` for direct mode)
  - `tags`
- [x] The aggregate block at the top of the file includes:
  - `totalScenarios` (enabled), `totalRate` (sum across open-mode scenarios), `totalUsers` (sum across closed-mode scenarios)
- [x] Tests verify the schema.

## Implementation Notes

- Compute the effective workload using the same code path as the simulation. Extract into a `SimulationPlan` data object so dry-run (F-013) and reporting share it.

## Out of scope

- Live progress metadata during the run.

## Suggested labels

`area:reporting`, `type:enhancement`, `milestone:M4`
