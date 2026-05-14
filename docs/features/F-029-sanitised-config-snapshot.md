# F-029 — Sanitised config snapshot writer

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** §19, MVP-AC-3 (implied), §9.2 `includeConfigSnapshot`

## Summary

PRD §19 calls for a "sanitised resolved configuration snapshot" alongside the run metadata. Today, the resolved configuration is only present in memory and in logs. CI artefacts are incomplete without a record of what was actually run.

## Acceptance Criteria

- [ ] When `reporting.includeConfigSnapshot: true`, a file `config-snapshot.yaml` (or `.json`) is written to the reporting directory containing the merged, resolved env/run/scenario configuration.
- [ ] Secret values are redacted using the mechanism delivered in F-007.
- [ ] The snapshot includes the resolved effective workload per scenario (computed by F-017 / F-020).
- [ ] When `reporting.includeConfigSnapshot: false`, no snapshot is written (default `false` for backwards compatibility, but recommend `true` in new sample YAMLs).
- [ ] Tests cover the redaction and the per-scenario effective-workload section.

## Implementation Notes

- A `ConfigSnapshotWriter` in `io.rampage.reporting` mirroring `RunMetadataWriter`. They can share serialiser configuration.
- Output YAML by default to make diffs across runs human-readable.

## Out of scope

- Schema versioning of the snapshot format.

## Suggested labels

`area:reporting`, `area:security`, `type:feature`, `milestone:M4`
