# F-026 — Feeder column validation

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** FDR-002

## Summary

`FeederConfig.columns[].required` is parsed and exists on `ColumnConfig`. The loader never checks that the SQL result set actually contains the declared columns, nor that required column values are non-null.

## Acceptance Criteria

- [x] After the first row is read, the loader verifies that every declared column in `feeder.columns` is present in `ResultSetMetaData` (case-insensitive label match). Missing columns fail the load.
- [x] For each row, columns with `required: true` must have non-null values. Null values fail the load (or skip the row, configurable via `onMissingRequired: fail|skip`, default `fail`).
- [ ] Column type is loosely validated (`type: string` accepts anything stringifiable; `type: integer` requires numeric; etc.) — start with `string|integer|long|boolean|date` and document. _(not implemented)_
- [x] `sessionKey` is honoured: rows are rewritten so the named key (rather than the SQL column label) appears in the Gatling session.
- [ ] Tests cover: missing column, null required value, type mismatch, `sessionKey` mapping. _(not implemented — missing-column and `sessionKey` tests exist; null-required and type-mismatch tests do not)_

## Implementation Notes

- The session-key remapping currently does not happen at all; rows are pushed verbatim into `listFeeder`. Add this transform inside `FeederFactory` before returning.
- For type checking, do a one-row validation pass at preload; for direct mode (F-024), validate the first row and trust subsequent rows.

## Out of scope

- JSON schema-style nested validation.

## Suggested labels

`area:feeder`, `area:validation`, `type:enhancement`, `milestone:M3`
