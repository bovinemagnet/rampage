# F-025 — Feeder row cap

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** FDR-006

## Summary

A misconfigured SQL query (e.g. forgotten `WHERE`) can preload millions of rows and OOM the JVM. No row cap is enforced today.

## Acceptance Criteria

- [x] New field `FeederConfig.maxRows: int` (default `10000`).
- [x] When a preload query returns more rows than `maxRows`, the loader stops at the cap and logs WARN.
- [x] When `failIfOverLimit: true` (new optional field), the loader throws instead of truncating.
- [x] Direct mode (F-024) also honours `maxRows` by counting pulls.
- [ ] Tests cover truncate, fail-on-limit, and unlimited (`maxRows: 0`). _(not implemented — truncate and fail-on-limit are tested; the unlimited `maxRows: 0` case is not)_

## Implementation Notes

- For preload, `LIMIT` cannot be appended naively to arbitrary SQL — instead, stop reading the result set once the cap is hit.
- Encourage SQL-side `LIMIT` in the docs; this cap is a safety net, not a performance feature.

## Out of scope

- Per-column truncation for large blobs.

## Suggested labels

`area:feeder`, `area:safety`, `type:feature`, `milestone:M3`
