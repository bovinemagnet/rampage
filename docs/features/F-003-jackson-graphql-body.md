# F-003 — Jackson-based GraphQL body construction

**Milestone:** M1 — MVP Honesty
**PRD references:** SCN-003, MVP-AC-4
**Traces to:** D6 in `docs/review/code-review.md`

## Summary

`ScenarioFactory.build` hand-rolls a JSON body by escaping the GraphQL query and concatenating variable strings. Non-string variables (booleans, numbers, nulls) are silently quoted, producing invalid JSON or wrong types. PRD §10.2 shows `includeInactive: false` as a boolean — Rampage would currently serialise this as `"false"`. Embedded quotes, backslashes, and Unicode in the GraphQL query are also fragile.

## Acceptance Criteria

- [ ] GraphQL request body is built with a `Map<String,Object>` and serialised by Jackson `ObjectMapper`.
- [ ] YAML-typed values (boolean, number, null, string, list, map) round-trip into JSON with the correct type.
- [ ] `${feeder:name}` placeholders remain Gatling EL `#{name}` after serialisation — the EL string survives Jackson without further escaping (note: Jackson will quote `#{name}` which is what we want, since Gatling will substitute the string after body assembly).
- [ ] The `operationName` field on `ScenarioConfig`, currently unused, is included in the JSON body when set.
- [ ] Unicode (e.g. `"naïve"`) in the GraphQL query string is preserved verbatim in the JSON body.
- [ ] New `ScenarioFactoryTest` covers: string/boolean/number/null variables, Unicode in query, presence/absence of `operationName`, presence/absence of feeder placeholders.

## Implementation Notes

- Allow `RequestConfig.variables` to be `Map<String, Object>` rather than `Map<String, String>` so YAML typing flows through.
- For values matching `${feeder:X}`, replace with the literal string `#{X}` before serialisation, so Gatling EL substitution sees a string token.
- Cache the constructed body **template** per scenario; only the feeder placeholders should be late-bound.

## Out of scope

- Non-GraphQL request bodies (later, if `protocol: rest` is honoured).
- Multipart / form-encoded bodies.

## Suggested labels

`area:simulation`, `priority:high`, `type:bug`, `milestone:M1`
