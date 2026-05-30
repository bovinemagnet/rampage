package io.rampage.factory;

import io.rampage.config.model.CredentialConfig;
import io.rampage.config.model.TokenConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SecretResolver {
    private static final Logger log = LoggerFactory.getLogger(SecretResolver.class);
    private static final String ENV_PREFIX = "ENV:";
    private static final String SM_PREFIX = "SM:";
    private static final String REDACTED = "***REDACTED***";

    private final Set<String> sensitiveValues = ConcurrentHashMap.newKeySet();

    public Set<String> getSensitiveValues() {
        return Set.copyOf(sensitiveValues);
    }

    public String trackSensitive(String value) {
        if (value != null && !value.isEmpty() && !REDACTED.equals(value)) {
            sensitiveValues.add(value);
        }
        return value;
    }

    public String resolve(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith(ENV_PREFIX)) {
            String varName = ref.substring(ENV_PREFIX.length());
            String value = System.getenv(varName);
            if (value == null) {
                // Fail fast rather than silently returning "" — a missing inline ENV: reference would
                // otherwise produce a malformed value (e.g. "Authorization: Bearer "). Genuinely optional
                // values should be declared via CredentialConfig/TokenConfig with required:false, not as
                // raw ENV: strings. resolveHeaders() is currently the only caller of this branch.
                throw new SecretResolutionException(
                    "Environment variable '" + varName + "' (inline 'ENV:' reference) is not set");
            }
            return trackSensitive(value);
        }
        if (ref.startsWith(SM_PREFIX)) {
            log.debug("Secret Manager resolution not implemented in MVP, returning redacted for: {}", ref);
            return REDACTED;
        }
        return ref;
    }

    public String resolveCredential(CredentialConfig cred) {
        return resolveCredential(cred, "credential");
    }

    public String resolveCredential(CredentialConfig cred, String configPath) {
        if (cred == null) return null;
        String source = cred.getSource();
        if ("env".equalsIgnoreCase(source)) {
            String varName = cred.getEnvVar();
            if (varName == null || varName.isBlank()) {
                if (cred.isRequired()) {
                    throw new SecretResolutionException(
                        "Credential at '" + configPath + "' has source 'env' but no envVar set");
                }
                return "";
            }
            String value = System.getenv(varName);
            if (value == null) {
                if (cred.isRequired()) {
                    throw new SecretResolutionException(
                        "Required environment variable '" + varName
                            + "' (referenced by '" + configPath + "') is not set");
                }
                log.warn("Optional environment variable '{}' not set", varName);
                return "";
            }
            return trackSensitive(value);
        }
        if ("secret-manager".equalsIgnoreCase(source) || "sm".equalsIgnoreCase(source)) {
            if (cred.getSecretPath() == null || cred.getSecretPath().isBlank()) {
                throw new SecretResolutionException(
                    "Credential at '" + configPath + "' has source 'secret-manager' but no secretPath set");
            }
            log.debug("Secret Manager resolution not implemented, returning redacted for: {}", cred.getSecretPath());
            return REDACTED;
        }
        if (!"plain".equalsIgnoreCase(source) && source != null) {
            log.warn("Unrecognized credential source type '{}', treating as plain value", source);
        }
        return cred.getValue() != null ? trackSensitive(cred.getValue()) : "";
    }

    public String resolveToken(TokenConfig token) {
        return resolveToken(token, "token");
    }

    public String resolveToken(TokenConfig token, String configPath) {
        if (token == null) return null;
        String source = token.getSource();
        if ("env".equalsIgnoreCase(source)) {
            String varName = token.getEnvVar();
            if (varName == null || varName.isBlank()) {
                if (token.isRequired()) {
                    throw new SecretResolutionException(
                        "Token at '" + configPath + "' has source 'env' but no envVar set");
                }
                return "";
            }
            String value = System.getenv(varName);
            if (value == null) {
                if (token.isRequired()) {
                    throw new SecretResolutionException(
                        "Required environment variable '" + varName
                            + "' (referenced by '" + configPath + "') is not set");
                }
                log.warn("Optional environment variable '{}' not set for token", varName);
                return "";
            }
            return trackSensitive(value);
        }
        if ("secret-manager".equalsIgnoreCase(source)) {
            if (token.getSecretPath() == null || token.getSecretPath().isBlank()) {
                throw new SecretResolutionException(
                    "Token at '" + configPath + "' has source 'secret-manager' but no secretPath set");
            }
            return REDACTED;
        }
        return "";
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
