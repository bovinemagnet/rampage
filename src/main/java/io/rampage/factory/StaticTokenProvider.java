package io.rampage.factory;

public class StaticTokenProvider implements TokenProvider {
    private final String token;

    public StaticTokenProvider(String token) {
        this.token = token;
    }

    @Override
    public String currentToken() {
        return token;
    }
}
