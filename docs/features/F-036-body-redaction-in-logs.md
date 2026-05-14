# F-036 — Body redaction in logs

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** SEC-008

## Summary

Gatling's default behaviour can log request and response bodies on failure. If those bodies contain feeder-sourced PII or secret-derived headers, the logs leak sensitive data. SEC-008 requires the framework to "avoid logging full request bodies when they contain sensitive data".

## Acceptance Criteria

- [ ] A new `ScenarioConfig.security.sensitiveFields: List<String>` enumerates JSONPath expressions whose values must be redacted from any logged body.
- [ ] A logback filter (or Gatling logger interceptor) replaces matching values with `***REDACTED***` in stdout, file, and HTML report failure dumps.
- [ ] When `reporting.redactSecrets: true`, the redaction layer is active by default — `sensitiveFields` adds to the base set (feeder column values that map to declared columns with `sensitive: true`).
- [ ] New optional flag on `ColumnConfig`: `sensitive: boolean` — feeder values for such columns are added to the redaction set.
- [ ] Tests cover the redaction transform on representative bodies.

## Implementation Notes

- The hardest part is intercepting Gatling's internal body logging. A simpler MVP: only redact bodies in our own `RunMetadataWriter` / snapshot output and in framework-emitted log lines, and document that Gatling-native body dumps may still contain raw values (with a recommendation to keep `gatling.conf` set to not log bodies).

## Out of scope

- TLS interception / capture redaction (not in scope).

## Suggested labels

`area:security`, `area:logging`, `type:feature`, `milestone:M4`
