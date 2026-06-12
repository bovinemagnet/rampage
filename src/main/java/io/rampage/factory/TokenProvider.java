package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.SecurityConfig;

/**
 * Supplies a bearer token for outgoing HTTP requests.
 *
 * <p>Implementations may hold a static token ({@link StaticTokenProvider}) or
 * obtain one dynamically from an OAuth endpoint
 * ({@link OAuthClientCredentialsTokenProvider}). The simulation calls
 * {@link #currentToken()} once per virtual user iteration to populate the
 * Gatling session.
 */
public interface TokenProvider {
    /**
     * Returns the current bearer token. Implementations may refresh in the
     * background and return whatever value is current.
     *
     * @return the current token, or {@code null} when no token is configured
     */
    String currentToken();

    /**
     * Creates a {@link TokenProvider} appropriate for the security mode
     * declared in the environment configuration.
     *
     * <p>Mode resolution:
     * <ul>
     *   <li>{@code bearer-token} — returns a {@link StaticTokenProvider}
     *       whose token value is resolved via {@code secretResolver}.</li>
     *   <li>{@code oauth-client-credentials} — returns an
     *       {@link OAuthClientCredentialsTokenProvider} that fetches tokens
     *       from the configured token endpoint.</li>
     *   <li>Any other mode, or a {@code null} environment or security config,
     *       returns a {@link StaticTokenProvider} with a {@code null} token.</li>
     * </ul>
     *
     * @param env            the environment configuration; may be {@code null}
     * @param secretResolver the resolver used to obtain credential values
     * @return a non-null {@link TokenProvider} appropriate for the configured
     *         security mode
     */
    static TokenProvider fromEnvironment(EnvironmentConfig env, SecretResolver secretResolver) {
        if (env == null || env.getSecurity() == null) return new StaticTokenProvider(null);
        SecurityConfig sec = env.getSecurity();
        String mode = sec.getMode();
        if ("bearer-token".equalsIgnoreCase(mode)) {
            String value = secretResolver.resolveToken(sec.getToken(), "environment.security.token");
            return new StaticTokenProvider(value);
        }
        if ("oauth-client-credentials".equalsIgnoreCase(mode)) {
            return new OAuthClientCredentialsTokenProvider(sec, secretResolver);
        }
        return new StaticTokenProvider(null);
    }
}
