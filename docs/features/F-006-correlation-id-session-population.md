# F-006 — Correlation ID session population

**Milestone:** M1 — MVP Honesty
**PRD references:** observability headers in §8
**Traces to:** D4 in `docs/review/code-review.md`

## Summary

`HttpProtocolFactory` adds an HTTP header with value `#{correlationId}` (Gatling EL), but nothing in the framework writes a `correlationId` into the session. The outbound header is therefore empty or causes Gatling to log a missing-attribute warning.

## Acceptance Criteria

- [x] When `environment.observability.correlationIdHeader` is set, every scenario virtual user has a unique `correlationId` available as a session attribute by the time the request is sent.
- [ ] The format is `<runId>-<scenarioId>-<uuid>` (configurable later, but a reasonable default). _(not implemented)_
- [ ] When `includeRunMetadataHeaders: true`, additional headers `X-Run-Id` and `X-Scenario-Id` are populated from session, not env. _(not implemented)_
- [ ] Tests verify the session is populated and the header reaches the request builder. _(not implemented — see [F-042](F-042-per-request-correlation-id.md))_

## Implementation Notes

- Two practical options:
  1. Prepend a Gatling `exec` step in `ScenarioFactory.build` that sets `correlationId` from a UUID supplier.
  2. Use a Gatling feeder of UUIDs that increments per virtual user.
- Option 1 is simpler and lives in `ScenarioFactory.build`.
- The header value should still use Gatling EL `#{correlationId}`; do not interpolate into a Java string.

## Out of scope

- Distributed tracing integration (W3C `traceparent`, OpenTelemetry).

## Suggested labels

`area:simulation`, `priority:medium`, `type:bug`, `milestone:M1`
