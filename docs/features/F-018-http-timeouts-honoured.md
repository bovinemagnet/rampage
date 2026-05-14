# F-018 — HTTP timeouts honoured

**Milestone:** M2 — Operational Readiness
**PRD references:** ENV-006
**Traces to:** ENV-006 row in `docs/review/requirements-traceability.md`

## Summary

`HttpConfig.connectTimeoutMillis` and `requestTimeoutMillis` are parsed but never passed to Gatling's `HttpProtocolBuilder`. The defaults baked into Gatling are used regardless of YAML.

## Acceptance Criteria

- [ ] `HttpProtocolFactory.build(env, ...)` calls `.requestTimeout(Duration.ofMillis(env.http.requestTimeoutMillis))` when configured.
- [ ] `.connectTimeout(...)` is applied similarly (note: Gatling's `connectionTimeout` lives on the underlying client config).
- [ ] `HttpConfig.followRedirects` is honoured via the Gatling builder.
- [ ] Tests verify the builder is constructed with the expected values for representative `HttpConfig` inputs.

## Implementation Notes

- Check the Gatling Java DSL exact method names in the version we are on (3.13.5). Some properties live on `gatling.conf` rather than on the builder; in that case, document and use the conf file (overlaps with F-016).

## Out of scope

- Per-scenario or per-request timeout overrides.

## Suggested labels

`area:http`, `type:bug`, `milestone:M2`
