# F-044 — Per-scenario and per-step request timeout override

**Milestone:** Platform evolution — Theme 6 (Engine-level gaps)
**PRD references:** ENV-006 (HTTP defaults)
**Traces to:** 2026-06 codebase review

## Summary

`environment.http.requestTimeoutMillis` applies to every request in a run. There is no way to give a single slow endpoint (a heavy report query, a mutating flow) a longer or shorter budget than the rest of the run. `RequestBuilder` already applies the timeout per request, so the plumbing point exists.

## Acceptance Criteria

- [ ] Optional `requestTimeoutMillis` on `ScenarioConfig` and on `StepConfig.request`.
- [ ] Precedence: step &gt; scenario &gt; environment; absent levels fall through.
- [ ] `ConfigValidator` rejects non-positive values.
- [ ] Unit tests for the precedence resolution (pure logic — testable without the Gatling runtime).
- [ ] Schema docs regenerated and the Antora configuration reference updated.

## Implementation Notes

- Resolution helper alongside `RequestBuilder.resolvePath(...)` keeps the precedence logic testable.
- The Gatling call site is the existing `requestTimeout(Duration)` application in `RequestBuilder`.

## Out of scope

- Per-step connect-timeout overrides (connection pooling makes these misleading).

## Suggested labels

`area:http`, `type:feature`, `priority:low`
