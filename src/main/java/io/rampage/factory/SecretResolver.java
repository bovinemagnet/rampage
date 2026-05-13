package io.rampage.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SecretResolver {
    private static final Logger log = LoggerFactory.getLogger(SecretResolver.class);
    private static final String ENV_PREFIX = "ENV:";
    private static final String SM_PREFIX = "SM:";
    private static final String REDACTED = "***REDACTED***";

    public String resolve(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith(ENV_PREFIX)) {
            String varName = ref.substring(ENV_PREFIX.length());
            String value = System.getenv(varName);
            if (value == null) {
                log.warn("Environment variable '{}' not set", varName);
                return "";
            }
            return value;
        }
        if (ref.startsWith(SM_PREFIX)) {
            log.debug("Secret Manager resolution not implemented in MVP, returning redacted for: {}", ref);
            return REDACTED;
        }
        return ref;
    }

    public String redact(String value) {
        return REDACTED;
    }

    public boolean isSecretRef(String value) {
        if (value == null) return false;
        return value.startsWith(ENV_PREFIX) || value.startsWith(SM_PREFIX);
    }

    public static Map<String, String> resolveHeaders(Map<String, String> headerRefs, SecretResolver resolver) {
        if (headerRefs == null) return new HashMap<>();
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : headerRefs.entrySet()) {
            resolved.put(entry.getKey(), resolver.resolve(entry.getValue()));
        }
        return resolved;
    }
}
