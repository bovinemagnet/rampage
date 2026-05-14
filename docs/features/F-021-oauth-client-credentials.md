# F-021 — OAuth client-credentials token source

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** ENV-003, Post-MVP-AC-3

## Summary

The framework only supports static bearer tokens sourced from environment variables. PRD ENV-003 calls for JWT generation and OAuth client-credentials. Many real APIs require client-credentials flow.

## Acceptance Criteria

- [ ] New `SecurityConfig.mode: oauth-client-credentials` is accepted, with fields:
  - `tokenUrl: String`
  - `clientId: CredentialConfig`
  - `clientSecret: CredentialConfig`
  - `scope: String` (optional)
  - `audience: String` (optional)
- [ ] On simulation init, the framework performs the token request (JDK `HttpClient`), parses the response (Jackson), and stores the access token.
- [ ] The token is injected as `Authorization: Bearer <token>` by `HttpProtocolFactory`.
- [ ] If the token endpoint returns an error, validation fails (or dry-run prints the error and exits non-zero).
- [ ] Tests use WireMock or a small embedded HTTP server to simulate the token endpoint.

## Implementation Notes

- A new `TokenProvider` interface (named in the PRD §6) with implementations `StaticTokenProvider` and `OAuthClientCredentialsTokenProvider`. `SecretResolver.resolveToken` becomes a thin wrapper that selects the right provider.
- Cache the token in-memory; token refresh is F-022.

## Out of scope

- Authorization Code, PKCE, device code flows.
- Token refresh (F-022).

## Suggested labels

`area:security`, `type:feature`, `milestone:M3`
