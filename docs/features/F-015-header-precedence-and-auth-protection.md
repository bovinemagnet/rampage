# F-015 — Header precedence layering and Authorization protection

**Milestone:** M2 — Operational Readiness
**PRD references:** §14, SEC-004, SEC-005
**Traces to:** D12 in `docs/review/code-review.md`

## Summary

PRD §14 specifies a five-level header precedence: framework-required, environment, run, scenario, request. Today, scenario headers are layered last via Gatling's `request.header(...)` which silently overrides `Authorization` and other environment headers. PRD also requires unsafe overrides (e.g. scenario replacing `Authorization`) to be **rejected**, not silently honoured.

## Acceptance Criteria

- [x] A merge step computes the effective headers for each scenario request, layered framework → env → run → scenario → request.
- [x] If a scenario attempts to set a header in a protected set (default: `Authorization`, the configured `correlationIdHeader`), validation fails unless `scenario.security.allowAuthOverride: true`.
- [x] Run-level headers are supported on `RunConfig` (new field `headers: Map<String,String>`).
- [ ] The effective headers are logged once per scenario at simulation start, with secret values redacted. _(not implemented)_
- [ ] Tests cover: env-only, env+scenario, scenario tries to set Authorization (rejected), scenario sets Authorization with override flag. _(not implemented)_

## Implementation Notes

- Header layering should happen once at simulation init, producing an immutable `Map<String,String>` per scenario.
- Apply to the `HttpRequestActionBuilder` in `ScenarioFactory`; do not also add scenario headers via the request builder (avoid duplication).

## Out of scope

- Per-request header overrides driven by feeders (out of scope; can be added later via `${feeder:HEADER}` substitution).

## Suggested labels

`area:security`, `area:simulation`, `type:enhancement`, `milestone:M2`
