# F-014 — Production-environment guardrails

**Milestone:** M2 — Operational Readiness
**PRD references:** SEC-007, ENV-007, §18 rules 6 and 7
**Traces to:** D11 in `docs/review/code-review.md`

## Summary

Production protection today is a string-match on `env.id.contains("prod")` inside `ConfigValidator`. `RunSafetyConfig.failIfEnvironmentAllowsProduction` is parsed but unused. Mutating scenarios are never checked against `SafetyConfig.requireApprovalForMutatingRequests`.

## Acceptance Criteria

- [ ] New field `EnvironmentConfig.safety.isProduction: boolean` (default `false`); the validator stops relying on the id string.
- [ ] If `env.safety.isProduction: true` and `env.safety.allowProduction: false`, validation fails.
- [ ] If `run.safety.failIfEnvironmentAllowsProduction: true` and `env.safety.allowProduction: true`, validation fails — even if `isProduction: false`.
- [ ] If any enabled scenario has `safety.mutating: true` and the environment has `safety.requireApprovalForMutatingRequests: true`, validation fails unless `run.safety.approveMutatingRequests: true` is set in this specific run.
- [ ] Tests cover every truth-table combination.
- [ ] Sample `config/environments/perf.yaml` (new) and `config/environments/local.yaml` set the new flags appropriately.

## Implementation Notes

- Add `boolean isProduction` to `SafetyConfig` and a parallel new `RunSafetyConfig.approveMutatingRequests`.
- Errors should name the offending fields by full YAML path.

## Out of scope

- Two-person review enforcement (out of scope for the framework — handled by GitHub branch protections).

## Suggested labels

`area:safety`, `priority:high`, `type:enhancement`, `milestone:M2`
