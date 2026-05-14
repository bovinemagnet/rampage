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

import static org.junit.jupiter.api.Assertions.*;

class TokenRefresherTest {
    private HttpServer server;
    private int port;
    private AtomicInteger requestCount;
    private volatile int responseStatus = 200;

    @BeforeEach
    void setUp() throws IOException {
        requestCount = new AtomicInteger();
        responseStatus = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/token", exchange -> {
            requestCount.incrementAndGet();
            if (responseStatus != 200) {
                exchange.sendResponseHeaders(responseStatus, 0);
                exchange.close();
                return;
            }
            byte[] response = ("{\"access_token\": \"tok-" + requestCount.get()
                + "\", \"expires_in\": 3600}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
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
        clientId.setValue("id");
        sec.setClientId(clientId);
        CredentialConfig clientSecret = new CredentialConfig();
        clientSecret.setSource("plain");
        clientSecret.setValue("secret");
        sec.setClientSecret(clientSecret);
        return sec;
    }

    @Test
    void refreshOnce_callsTokenEndpoint() {
        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            oauthConfig(), new SecretResolver());
        try (TokenRefresher refresher = new TokenRefresher(provider, 60, TokenRefresher.Mode.CONTINUE)) {
            refresher.refreshOnce();
            assertEquals(1, requestCount.get());
            assertFalse(refresher.isStopped());
        }
    }

    @Test
    void refreshOnce_continuesOnFailure() {
        responseStatus = 500;
        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            oauthConfig(), new SecretResolver());
        try (TokenRefresher refresher = new TokenRefresher(provider, 60, TokenRefresher.Mode.CONTINUE)) {
            refresher.refreshOnce();
            assertFalse(refresher.isStopped());
        }
    }

    @Test
    void refreshOnce_stopsOnFailureInStopMode() {
        responseStatus = 500;
        OAuthClientCredentialsTokenProvider provider = new OAuthClientCredentialsTokenProvider(
            oauthConfig(), new SecretResolver());
        try (TokenRefresher refresher = new TokenRefresher(provider, 60, TokenRefresher.Mode.STOP)) {
            refresher.refreshOnce();
            assertTrue(refresher.isStopped());
        }
    }

    @Test
    void parseFailureMode_defaultsToContinue() {
        assertEquals(TokenRefresher.Mode.CONTINUE, TokenRefresher.parseFailureMode(null));
        assertEquals(TokenRefresher.Mode.CONTINUE, TokenRefresher.parseFailureMode("continue"));
        assertEquals(TokenRefresher.Mode.STOP, TokenRefresher.parseFailureMode("stop"));
        assertEquals(TokenRefresher.Mode.STOP, TokenRefresher.parseFailureMode("STOP"));
    }
}
