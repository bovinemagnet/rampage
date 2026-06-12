package io.rampage.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rampage.config.model.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link TokenProvider} that obtains a bearer token from an OAuth 2.0
 * {@code client_credentials} token endpoint and caches it until 30 seconds
 * before its reported expiry.
 *
 * <p>The token is fetched synchronously on the first call to
 * {@link #currentToken()} and on any subsequent call when the cached token is
 * within 30 seconds of expiry. {@link #fetchToken()} is also called periodically
 * by {@link TokenRefresher} when proactive refreshing is configured.
 */
public class OAuthClientCredentialsTokenProvider implements TokenProvider {
    private static final Logger log = LoggerFactory.getLogger(OAuthClientCredentialsTokenProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SecurityConfig config;
    private final SecretResolver secretResolver;
    private final HttpClient httpClient;
    private final AtomicReference<String> currentToken = new AtomicReference<>();
    private volatile Instant expiresAt = Instant.EPOCH;

    /**
     * Constructs a provider using a default {@code HttpClient} with a 10-second
     * connection timeout.
     *
     * @param config         the security configuration containing the token URL,
     *                       client credentials, scope, and audience
     * @param secretResolver resolver used to obtain the client ID and secret values
     */
    public OAuthClientCredentialsTokenProvider(SecurityConfig config, SecretResolver secretResolver) {
        this(config, secretResolver, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build());
    }

    OAuthClientCredentialsTokenProvider(SecurityConfig config, SecretResolver secretResolver, HttpClient httpClient) {
        this.config = config;
        this.secretResolver = secretResolver;
        this.httpClient = httpClient;
    }

    /**
     * Returns the current bearer token, fetching a new one from the token
     * endpoint if the cached token is absent or within 30 seconds of expiry.
     *
     * @return the current access token; never {@code null} when the token
     *         endpoint is reachable
     */
    @Override
    public String currentToken() {
        String token = currentToken.get();
        if (token == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
            return fetchToken();
        }
        return token;
    }

    /**
     * Forces an immediate token fetch from the configured token endpoint and
     * updates the cached token and expiry time.
     *
     * <p>Visible for testing — production code should call {@link #currentToken()}
     * which only fetches when the cached token has expired or is about to expire.
     *
     * @return the newly fetched access token
     * @throws IllegalStateException if {@code tokenUrl} is not configured, if the
     *                               token endpoint returns a non-2xx response, or
     *                               if the response does not contain an
     *                               {@code access_token} field
     */
    public synchronized String fetchToken() {
        if (config.getTokenUrl() == null || config.getTokenUrl().isBlank()) {
            throw new IllegalStateException("oauth-client-credentials requires environment.security.tokenUrl");
        }
        String clientId = secretResolver.resolveCredential(config.getClientId(), "environment.security.clientId");
        String clientSecret = secretResolver.resolveCredential(config.getClientSecret(), "environment.security.clientSecret");

        StringBuilder body = new StringBuilder();
        body.append("grant_type=client_credentials");
        if (config.getScope() != null && !config.getScope().isBlank()) {
            body.append("&scope=").append(URLEncoder.encode(config.getScope(), StandardCharsets.UTF_8));
        }
        if (config.getAudience() != null && !config.getAudience().isBlank()) {
            body.append("&audience=").append(URLEncoder.encode(config.getAudience(), StandardCharsets.UTF_8));
        }

        String basic = Base64.getEncoder().encodeToString(
            (safe(clientId) + ":" + safe(clientSecret)).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getTokenUrl()))
            .header("Authorization", "Basic " + basic)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Token endpoint " + config.getTokenUrl()
                    + " returned HTTP " + response.statusCode() + ": " + response.body());
            }
            JsonNode json = MAPPER.readTree(response.body());
            JsonNode tokenNode = json.get("access_token");
            if (tokenNode == null || tokenNode.asText().isBlank()) {
                throw new IllegalStateException("Token endpoint response missing access_token");
            }
            String token = tokenNode.asText();
            long ttlSeconds = json.path("expires_in").asLong(3600);
            currentToken.set(token);
            expiresAt = Instant.now().plusSeconds(ttlSeconds);
            log.info("Fetched OAuth access token from {} (expires in {}s)", config.getTokenUrl(), ttlSeconds);
            return token;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch OAuth token: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the instant at which the currently cached token expires.
     *
     * <p>Returns {@code Instant.EPOCH} when no token has been fetched yet.
     *
     * @return the token expiry instant
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
