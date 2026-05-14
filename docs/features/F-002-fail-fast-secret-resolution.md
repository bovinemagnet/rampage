# F-002 — Fail-fast secret resolution

**Milestone:** M1 — MVP Honesty
**PRD references:** SEC-007, MVP-AC-7, §18 rule 5
**Traces to:** D8 in `docs/review/code-review.md`

## Summary

`SecretResolver` currently logs a warning and returns `""` when an env-var-sourced credential is missing. A missing `API_TOKEN` therefore produces `Authorization: Bearer ` (literal empty token) and the load test runs against an unauthenticated endpoint, or worse, against an endpoint that accepts it silently. PRD §18 requires this to be a hard fail before traffic starts.

## Acceptance Criteria

- [ ] When a `CredentialConfig` or `TokenConfig` declares `source: env` and the named env var is unset, `SecretResolver` throws a checked-or-runtime exception carrying the env var name and the config path (e.g. `environment.security.token`).
- [ ] The exception is surfaced by `ConfigValidator` so `validateLoadTest` exits non-zero with a list of unresolved secrets.
- [ ] `RampageSimulation` initialisation halts before any Gatling injection step is configured.
- [ ] `secret-manager` source remains a stub but throws if `secretPath` is unset.
- [ ] A new optional field `required: false` on `CredentialConfig` / `TokenConfig` allows callers to opt out of fail-fast (used for optional headers).
- [ ] Tests cover: missing env var with `required=true`, missing env var with `required=false`, unset `envVar` field, `secret-manager` stub.

## Implementation Notes

- Add a `required` flag (default `true`) to both `CredentialConfig` and `TokenConfig`.
- Add a `SecretResolutionException` that lists all unresolved references (so all missing secrets are reported in one error, matching `ConfigValidator`'s aggregation style).
- Wire the resolution check into `ConfigValidator.validate` so `validateLoadTest` and `gatlingRun` agree.
- Update `config/environments/local.yaml` and `src/gatling/resources/environment.yaml` to mark token/db credentials as `required: true`.

## Out of scope

- Real Secret Manager / Vault integration (later milestone).
- Token refresh (F-022).

## Suggested labels

`area:security`, `priority:high`, `type:bug`, `milestone:M1`
