package io.rampage.factory;

/**
 * {@link TokenProvider} that always returns a fixed, pre-resolved token string.
 *
 * <p>Used for the {@code bearer-token} security mode where the token value is
 * known at configuration time and does not require periodic refresh.
 */
public class StaticTokenProvider implements TokenProvider {
    private final String token;

    /**
     * Constructs a provider that will always return {@code token}.
     *
     * @param token the fixed bearer token; may be {@code null} to indicate that
     *              no token is configured
     */
    public StaticTokenProvider(String token) {
        this.token = token;
    }

    /**
     * Returns the fixed token supplied at construction time.
     *
     * @return the token string; may be {@code null}
     */
    @Override
    public String currentToken() {
        return token;
    }
}
