package io.rampage.factory;

import com.sun.net.httpserver.HttpServer;
import io.rampage.config.model.CredentialConfig;
import io.rampage.config.model.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OAuthClientCredentialsTokenProviderTest {
    private HttpServer server;
    private int port;
    private AtomicInteger requestCount;
    private AtomicReference<String> lastBody;
    private AtomicReference<String> lastAuth;

    @BeforeEach
    void setUp() throws IOException {
        requestCount = new AtomicInteger();
        lastBody = new AtomicReference<>();
        lastAuth = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/token", exchange -> {
            requestCount.incrementAndGet();
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] reqBody = exchange.getRequestBody().readAllBytes();
            lastBody.set(new String(reqBody, StandardCharsets.UTF_8));

            byte[] response = "{\"access_token\": \"abc-123\", \"expires_in\": 3600}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.createContext("/bad", exchange -> {
            byte[] response = "{\"error\": \"unauthorized\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private SecurityConfig oauthConfig() {
        SecurityConfig sec = new SecurityConfig();
        sec.setMode("oauth-client-credentials");
        sec.setTokenUrl("http://127.0.0.1:" + port + "/token");
        CredentialConfig clientId = new CredentialConfig();
        clientId.setSource("plain");
        clientId.setValue("my-client");
        sec.setClientId(clientId);
        CredentialConfig clientSecret = new CredentialConfig();
        clientSecret.setSource("plain");
        clientSecret.setValue("my-secret");
        sec.setClientSecret(clientSecret);
        sec.setScope("api:read");
        return sec;
    }

    @Test
    void fetchToken_returnsAccessTokenFromResponse() {
        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            oauthConfig(), new SecretResolver());

        String token = provider.fetchToken();

        assertEquals("abc-123", token);
        assertEquals(1, requestCount.get());
        assertTrue(lastBody.get().contains("grant_type=client_credentials"));
        assertTrue(lastBody.get().contains("scope=api%3Aread"));
        assertNotNull(lastAuth.get());
        assertTrue(lastAuth.get().startsWith("Basic "));
    }

    @Test
    void currentToken_cachesAcrossCalls() {
        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            oauthConfig(), new SecretResolver());

        provider.currentToken();
        provider.currentToken();

        assertEquals(1, requestCount.get(), "Token should be cached and not re-fetched while still valid");
    }

    @Test
    void currentToken_fetchesOnceUnderConcurrentCallers() throws InterruptedException {
        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            oauthConfig(), new SecretResolver());

        int threads = 16;
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    provider.currentToken();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            t.start();
        }
        start.countDown();
        assertTrue(done.await(10, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(1, requestCount.get(),
            "Concurrent callers hitting an empty cache must trigger only a single token fetch");
    }

    @Test
    void fetchToken_throwsOnHttpError() {
        SecurityConfig sec = oauthConfig();
        sec.setTokenUrl("http://127.0.0.1:" + port + "/bad");

        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            sec, new SecretResolver());

        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::fetchToken);
        assertTrue(ex.getMessage().contains("401"));
    }

    @Test
    void fetchToken_throwsWhenTokenUrlMissing() {
        SecurityConfig sec = oauthConfig();
        sec.setTokenUrl(null);

        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            sec, new SecretResolver());

        assertThrows(IllegalStateException.class, provider::fetchToken);
    }
}
