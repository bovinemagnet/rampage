# F-016 — logback.xml + gatling.conf for predictable logs

**Milestone:** M2 — Operational Readiness
**PRD references:** §6 (Repository Layout)
**Traces to:** §3.3 gap in `docs/review/code-review.md`

## Summary

PRD §6 lists `src/gatling/resources/{gatling.conf, logback.xml}` as expected files. Neither exists. The framework runs on Gatling defaults, which produce noisy logs in CI and inconsistent reporter behaviour across machines.

## Acceptance Criteria

- [ ] `src/gatling/resources/logback.xml` configures:
  - `INFO` for `io.rampage.*`
  - `WARN` for `io.gatling.*` and `io.netty.*`
  - `WARN` for `com.zaxxer.hikari.*`
  - A console appender suitable for CI (no ANSI by default; `LOGBACK_ANSI` env var enables ANSI for local terminals). _(not implemented)_
  - Optional file appender to `build/reports/gatling/rampage.log`.
- [x] `src/gatling/resources/gatling.conf` overrides defaults to:
  - Use the Highcharts reporter
  - Set reasonable connection pool sizes
  - Disable Gatling's "press enter" prompt on completion
- [x] CI run produces ≤ 50 lines of log noise per test run (excluding the run metadata summary).
- [ ] Documented in `CLAUDE.md`. _(not implemented)_

## Implementation Notes

- Keep `logback-test.xml` in `src/test/resources` separately for unit tests (already-implicit Spring/Slf4j convention).
- `gatling.conf` follows Typesafe Config format; only override what we need.

## Out of scope

- Structured logging (JSON) — wait until F-031 to decide if CI consumers need it.

## Suggested labels

`area:logging`, `type:enhancement`, `milestone:M2`
