package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.SecurityConfig;

public interface TokenProvider {
    /**
     * Current bearer token. Implementations may refresh in the background and
     * return whatever value is current. Returns null when no token is configured.
     */
    String currentToken();

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
