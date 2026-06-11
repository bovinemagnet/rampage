# F-043 — HTTP status range checks

**Milestone:** Platform evolution — Theme 6 (Engine-level gaps)
**PRD references:** SCN-007 (checks)
**Traces to:** 2026-06 codebase review

## Summary

`ChecksConfig.httpStatus` supports exact status equality only (`status().is(...)`). Scenarios that legitimately accept a class of responses ("any 2xx", "200 or 201 or 204") must either pick one code or omit the check. Gatling supports `status().in(...)` natively.

## Acceptance Criteria

- [ ] `checks.httpStatusIn: [200, 201, 204]` — list membership check.
- [ ] `checks.httpStatusClass: 2xx` (also `3xx`, `4xx`, `5xx`) — translated to the corresponding range.
- [ ] Existing `checks.httpStatus: 200` behaviour unchanged; configuring more than one of the three is a validation error.
- [ ] `ConfigValidator` rejects malformed class values and empty lists.
- [ ] Tests: validator rules plus end-to-end coverage in the WireMock integration test (the check kinds cannot be unit-tested — Gatling DSL statics require a Gatling-bootstrapped run; see `RampageSimulationWireMockIntegrationTest`).
- [ ] Schema docs regenerated (`generateSchemaDocs`) and Antora checks page updated.

## Implementation Notes

- Model: add `httpStatusIn` (List&lt;Integer&gt;) and `httpStatusClass` (String) to `ChecksConfig`.
- `CheckFactory`: `status().in(list)` for the list; for a class, expand to the 100-code range with `status().in(IntStream.range(...))` or Gatling's range support.

## Out of scope

- Negated checks ("anything but 404").

## Suggested labels

`area:assertions`, `type:feature`, `priority:low`
