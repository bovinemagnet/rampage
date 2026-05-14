# F-024 — Direct JDBC feeder mode (streaming)

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** §12.1, FDR-001
**Traces to:** FDR-001 row in `docs/review/requirements-traceability.md`

## Summary

PRD §12.1 names two execution modes: preload and direct JDBC. Today only preload is implemented; the entire result set is materialised into a `List<Map<String,Object>>` before the run starts. Large datasets exhaust memory and slow startup.

## Acceptance Criteria

- [ ] When `feeder.preload: false`, the feeder streams rows from the JDBC connection lazily, one row per Gatling pull.
- [ ] The connection is borrowed from the HikariCP pool (depends on F-009), held by a `ResultSet` cursor, and released when the result set is exhausted or the simulation ends.
- [ ] Concurrency is bounded by the pool size; if N scenarios use direct mode against the same `databaseRef`, they share connections via the pool, not by opening one each.
- [ ] PRD §12.2 recommends preload as the default — preserve that default behaviour.
- [ ] Tests cover: streaming exhaustion behaviour, pool sharing, error paths.

## Implementation Notes

- Wrap each direct-mode feeder in a `java.util.Iterator<Map<String,Object>>` that lazily advances the `ResultSet`. Implements `hasNext`/`next` with thread-safety since Gatling pulls from multiple injectors.
- Document the trade-off in the Antora docs: direct mode means the source database is on the critical path.

## Out of scope

- Cursor-based pagination (LIMIT / OFFSET) — out of scope; rely on JDBC fetch size.

## Suggested labels

`area:feeder`, `type:feature`, `milestone:M3`
