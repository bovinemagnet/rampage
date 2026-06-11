# F-008 — ScenarioFactory test coverage

**Milestone:** M1 — MVP Honesty
**Traces to:** §3.5 in `docs/review/code-review.md`

## Summary

`ScenarioFactory` is the most subtle factory in the codebase (string escaping, EL rewriting, JSON body construction, check translation) and has zero unit tests. It is also the place where F-003 and F-006 will make changes — the existing lack of tests makes those changes risky.

## Acceptance Criteria

- [ ] `ScenarioFactoryTest` exists and covers:
  - building a scenario with no feeder placeholders
  - building a scenario with `${feeder:X}` placeholders
  - boolean and number variables (depends on F-003)
  - missing `request` block → empty body or sensible default
  - `httpStatus`, `exists`, `absentOrEmpty`, `equalsSession` checks
  - missing `checks` block → no checks attached (until F-004 forbids it)
  - scenario headers applied
  - `operationName` included when set
- [ ] Tests do not require a running HTTP server; assert on the `HttpRequestActionBuilder` shape or extract a testable seam.
- [ ] Tests run under existing `./gradlew test` invocation.

## Implementation Notes

- Where Gatling builder objects are opaque, consider extracting body-building and variables-rendering into package-private static methods that take/return `String`/`Map`. The original `build()` then composes them.
- Use AssertJ for fluent assertions consistent with the rest of the test suite.

## Out of scope

- Integration test against a real HTTP stub (F-038).

## Suggested labels

`area:simulation`, `type:test`, `milestone:M1`
