# F-027 — YAML placeholder expansion

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** §8.2, §9.2, §14
**Traces to:** §3.3 gap in `docs/review/code-review.md`

## Summary

Sample environment YAMLs use placeholders such as `${run:id}`, `${RUN_ID}`, and `${secret:PERF_API_JWT}` inside header values. `SecretResolver.resolve(String)` handles bare `ENV:` and `SM:` prefixes but does not parse the more readable `${...}` syntax used in the YAMLs. The values are passed through verbatim and reach the SUT as literal `${run:id}`.

## Acceptance Criteria

- [x] After parsing each YAML, a post-processing step walks the model and expands placeholders in string fields:
  - `${run:<key>}` → value from the loaded `RunConfig` (`id`, `name`, `version`, `environment`, etc.)
  - `${env:<NAME>}` and `${ENV:<NAME>}` → environment variable
  - `${secret:<path>}` → resolved via `SecretResolver`
  - `${sys:<NAME>}` → JVM system property
- [x] Unresolved placeholders cause a validation failure (consistent with F-002).
- [x] Expansion is applied to: env headers, run-level headers, scenario headers, scenario `description`, request `bodyTemplate`, and any other string field where it makes sense (whitelisted to avoid unintended substitution in GraphQL queries).
- [x] Escaped placeholders (`\${...}`) pass through literally.
- [ ] Tests cover each placeholder type and the escape syntax. _(not implemented — `${run:...}`, `${env:...}`, `${sys:...}` and the escape syntax are tested; `${secret:...}` is not)_

## Implementation Notes

- Use a single `Substitutor` class with a regex pattern. Pre-compile.
- Apply substitution to `SecurityConfig.headers`, `EnvironmentConfig.observability`, `RunConfig` headers (new in F-015), `ScenarioConfig.headers`.
- Do **not** apply substitution to GraphQL query bodies — those have their own `${feeder:X}` syntax handled in `ScenarioFactory`.

## Out of scope

- Recursive expansion (a substitution result containing another placeholder is not re-expanded).
- Default values (`${env:NAME:-default}`).

## Suggested labels

`area:config`, `type:feature`, `milestone:M3`
