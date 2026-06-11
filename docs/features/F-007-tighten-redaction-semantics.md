# F-007 — Tighten redaction semantics in RunMetadataWriter

**Milestone:** M1 — MVP Honesty
**PRD references:** SEC-003, MVP-AC-10
**Traces to:** D10 in `docs/review/code-review.md`

## Summary

`RunMetadataWriter` writes `"redacted": true` as a literal field, but it has no visibility of the resolved secret values and no logic to redact anything. The flag is misleading. Either the writer must hold and actively redact secrets, or the field must be removed.

## Acceptance Criteria

- [x] Either:
  - (a) `RunMetadataWriter` receives a `Map<String, Object>` of "potentially-sensitive" resolved values from `SecretResolver`, and redacts any occurrence of those substrings in the serialised metadata; OR
  - (b) the `"redacted"` key is renamed to `"redactSecretsEnabled"` and reflects only the run config flag, with a docstring stating that no values are serialised that would need redaction in the first place.
- [x] Option (b) is acceptable only after F-029 has shipped, since the config snapshot is the file most at risk of leaking secrets.
- [ ] Tests assert: no env-var-sourced secret value ever appears in `run-metadata.json` for a representative config. _(not implemented)_
- [x] `SecretResolver` exposes a `Set<String> getSensitiveValues()` (or equivalent) so callers can build a redaction pattern.

## Implementation Notes

- Walk the YAML model, collect all `CredentialConfig` / `TokenConfig` references that resolve to non-empty values, store them in a redaction set.
- Apply the redaction at serialisation time (Jackson `JsonSerializer<String>` or post-process the JSON string).
- Never include the redaction set itself in the output.

## Out of scope

- Body redaction in Gatling logs (F-036).

## Suggested labels

`area:reporting`, `area:security`, `priority:medium`, `type:bug`, `milestone:M1`
