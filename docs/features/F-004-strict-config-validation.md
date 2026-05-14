# F-004 — Strict config validation

**Milestone:** M1 — MVP Honesty
**PRD references:** §18 (validation rules), MVP-AC-7
**Traces to:** D11, gaps 3/5/8/9/10 in `docs/review/code-review.md`

## Summary

`ConfigValidator` checks structural presence but not referential integrity. PRD §18 lists ten conditions that must cause a hard fail **before** traffic starts. Several of those are not enforced today, including missing GraphQL/SQL files, malformed durations, mutating scenarios against environments that disallow mutation, and scenarios with no checks.

## Acceptance Criteria

The validator throws a `ConfigValidationException` aggregating all errors when any of the following hold:

- [ ] A scenario's `request.graphqlQueryFile` does not exist on filesystem or classpath.
- [ ] A scenario's `feeder.sqlFile` does not exist on filesystem or classpath.
- [ ] A scenario's `feeder.databaseRef` is not defined in `environment.databases`.
- [ ] A `WorkloadConfig.type` is unknown (not in `smoke|baseline|ramp-and-hold|spike|stress|soak|constant`).
- [ ] A `WorkloadConfig.rampUp` / `holdFor` / `duration` is a non-empty string that fails to parse (instead of silently defaulting).
- [ ] A scenario with `safety.mutating: true` is targeted at an env where `safety.requireApprovalForMutatingRequests: true` and no explicit approval is provided (define an `approval` field on `ScenarioRef` or `run.safety`).
- [ ] `RunSafetyConfig.failIfEnvironmentAllowsProduction: true` and `env.safety.allowProduction: true` together fail validation.
- [ ] A scenario has no `checks` at all, unless it sets a new `safety.allowNoChecks: true` flag.
- [ ] Production detection is stronger than `id.contains("prod")` — use an explicit `env.safety.isProduction: true` flag combined with the existing `allowProduction` check.
- [ ] Tests cover each new validation rule with both failing and passing fixtures.

## Implementation Notes

- Add a helper `validateScenarioReferences(env, scenario)` that performs file-existence and `databaseRef` checks.
- File-existence check should match the resolution logic in `ConfigLoader.loadResource` (filesystem first, classpath fallback).
- Introduce `WorkloadType` and `Protocol` enums; use `@JsonValue` / case-insensitive deserialisation for tolerance.
- Reuse `WorkloadFactory.parseDuration` but expose a "strict" variant that throws on failure.

## Out of scope

- Schema-level YAML validation (JSON Schema generation is F-035).
- Runtime feeder column validation (F-026).

## Suggested labels

`area:validation`, `priority:high`, `type:enhancement`, `milestone:M1`
