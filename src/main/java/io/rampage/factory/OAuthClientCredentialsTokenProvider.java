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

public class OAuthClientCredentialsTokenProvider implements TokenProvider {
    private static final Logger log = LoggerFactory.getLogger(OAuthClientCredentialsTokenProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SecurityConfig config;
    private final SecretResolver secretResolver;
    private final HttpClient httpClient;
    private final AtomicReference<String> currentToken = new AtomicReference<>();
    private volatile Instant expiresAt = Instant.EPOCH;

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

    @Override
    public String currentToken() {
        String token = currentToken.get();
        if (token == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
            return fetchToken();
        }
        return token;
    }

    /** Visible for testing — forces a refresh now. */
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
