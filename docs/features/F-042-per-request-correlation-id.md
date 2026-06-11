# F-042 — Optional per-request correlation ID

**Milestone:** Platform evolution — Theme 6 (Engine-level gaps)
**PRD references:** §14 (observability), OBS-002
**Traces to:** 2026-06 codebase review

## Summary

`ScenarioFactory` sets the `correlationId` session attribute once at the start of each scenario execution, so every request in a multi-step scenario shares one correlation ID. That is a reasonable default (it groups a business flow under one trace), but some observability set-ups expect a distinct ID per HTTP request. There is currently no way to choose.

## Acceptance Criteria

- [ ] New observability option, e.g. `environment.observability.correlationIdScope: scenario | request` (default `scenario`, preserving current behaviour).
- [ ] With `request` scope, a fresh UUID is set in the session immediately before each step's request is built.
- [ ] `ConfigValidator` rejects unknown scope values.
- [ ] Behaviour covered by tests (session-prep chain for `scenario`; a WireMock integration assertion that two steps carry different header values for `request`).
- [ ] Documented in the Antora observability page.

## Implementation Notes

- The header itself is attached in `HttpProtocolFactory` via the Gatling EL `#{correlationId}` — only the session population point changes.
- For `request` scope, prepend a small `exec(session -> session.set("correlationId", ...))` in `StepBuilder` rather than touching the protocol layer.

## Out of scope

- Propagating a parent/child trace relationship (W3C traceparent); single header only.

## Suggested labels

`area:http`, `type:enhancement`, `priority:low`
