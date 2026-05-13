package io.rampage.factory;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.rampage.config.model.EnvironmentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.gatling.javaapi.http.HttpDsl.http;

public class HttpProtocolFactory {
    private static final Logger log = LoggerFactory.getLogger(HttpProtocolFactory.class);

    public HttpProtocolBuilder build(EnvironmentConfig.Environment env, SecretResolver secretResolver) {
        log.info("Building HTTP protocol for base URL: {}", env.getBaseUrl());

        HttpProtocolBuilder builder = http.baseUrl(env.getBaseUrl());

        // Set shared headers
        if (env.getHttpHeaders() != null) {
            Map<String, String> resolvedHeaders = SecretResolver.resolveHeaders(env.getHttpHeaders(), secretResolver);
            for (Map.Entry<String, String> entry : resolvedHeaders.entrySet()) {
                builder = builder.header(entry.getKey(), entry.getValue());
            }
        }

        // Set auth header
        if (env.getAuth() != null) {
            String mode = env.getAuth().getMode();
            if ("bearer".equalsIgnoreCase(mode) && env.getAuth().getTokenRef() != null) {
                String token = secretResolver.resolve(env.getAuth().getTokenRef());
                builder = builder.header("Authorization", "Bearer " + token);
                log.debug("Added Bearer authorization header");
            } else if ("basic".equalsIgnoreCase(mode) && env.getAuth().getTokenRef() != null) {
                String credentials = secretResolver.resolve(env.getAuth().getTokenRef());
                builder = builder.header("Authorization", "Basic " + credentials);
                log.debug("Added Basic authorization header");
            }
        }

        builder = builder.connectionHeader("keep-alive");

        return builder;
    }
}
