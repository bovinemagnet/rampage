# F-005 — Unified scenario path resolution

**Milestone:** M1 — MVP Honesty
**Traces to:** D9 in `docs/review/code-review.md`

## Summary

`ConfigValidatorMain` loads scenarios using `ScenarioRef.file` as a filesystem path; `RampageSimulation` loads them using `scenarios/<id>.yaml` as a classpath resource. The two can disagree: a config that passes `validateLoadTest` may fail at `gatlingRun` (or vice versa) because one path resolves and the other does not. This makes the validator unreliable as a preflight.

## Acceptance Criteria

- [ ] `ConfigLoader` gains a single `loadScenario(ScenarioRef)` method that tries, in order: (1) the explicit `ref.file` as filesystem path, (2) the explicit `ref.file` as classpath path, (3) `scenarios/<ref.id>.yaml` as classpath path.
- [ ] Both `ConfigValidatorMain` and `RampageSimulation` use this single method.
- [ ] When the scenario cannot be loaded, the same exception type is thrown by both call sites, carrying the resolution attempts in the message.
- [ ] Tests verify each of the three resolution paths and the failure mode.
- [ ] Documentation in `CLAUDE.md` is updated to describe the resolution order.

## Implementation Notes

- Move all `getResourceStream(...)` / `FileInputStream(...)` branching into `ConfigLoader`; do not let callers do path arithmetic.
- The same unification should apply to `feeder.sqlFile` and `request.graphqlQueryFile` (already partially handled in `loadResource`; verify and add tests).

## Out of scope

- Variable substitution inside path strings (F-027).

## Suggested labels

`area:config`, `priority:medium`, `type:refactor`, `milestone:M1`
