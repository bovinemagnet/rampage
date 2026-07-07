package io.rampage.factory;

import io.rampage.config.model.CredentialConfig;
import io.rampage.config.model.TokenConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves credential and token references from environment variables or
 * secret-manager paths.
 *
 * <p>Two inline prefix forms are supported in raw string references:
 * <ul>
 *   <li>{@code ENV:VAR_NAME} — reads the named environment variable and
 *       throws {@link SecretResolutionException} if it is not set.</li>
 *   <li>{@code SM:path} — intended for Secret Manager; throws
 *       {@link SecretResolutionException} because Secret Manager integration is
 *       not yet implemented (returning a placeholder would be used as a live
 *       credential).</li>
 * </ul>
 *
 * <p>Structured {@code CredentialConfig} and {@code TokenConfig} values are
 * resolved via {@link #resolveCredential} and {@link #resolveToken}
 * respectively, with support for {@code required} flags and optional
 * fallback behaviour.
 *
 * <p>Resolved secret values are tracked internally; use
 * {@link #getSensitiveValues()} to obtain the set for log-redaction purposes.
 */
public class SecretResolver {
    private static final Logger log = LoggerFactory.getLogger(SecretResolver.class);
    private static final String ENV_PREFIX = "ENV:";
    private static final String SM_PREFIX = "SM:";
    private static final String REDACTED = "***REDACTED***";
    private static final String SM_NOT_IMPLEMENTED = "Secret Manager integration is not implemented";

    private final Set<String> sensitiveValues = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a new {@code SecretResolver} with an empty sensitive-values
     * tracking set.
     */
    public SecretResolver() {
    }

    /**
     * Returns an unmodifiable snapshot of all secret values that have been
     * resolved and tracked by this instance.
     *
     * @return an unmodifiable set of sensitive string values; never {@code null}
     */
    public Set<String> getSensitiveValues() {
        return Set.copyOf(sensitiveValues);
    }

    /**
     * Registers {@code value} in the sensitive-values tracking set and returns it.
     *
     * <p>Values that are {@code null}, empty, or equal to the redacted placeholder
     * are not tracked.
     *
     * @param value the value to track; may be {@code null}
     * @return the same {@code value} passed in
     */
    public String trackSensitive(String value) {
        if (value != null && !value.isEmpty() && !REDACTED.equals(value)) {
            sensitiveValues.add(value);
        }
        return value;
    }

    /**
     * Resolves an inline secret reference string.
     *
     * <ul>
     *   <li>A reference prefixed with {@code ENV:} reads the named environment
     *       variable and throws {@link SecretResolutionException} if it is not
     *       set.</li>
     *   <li>A reference prefixed with {@code SM:} throws
     *       {@link SecretResolutionException} because Secret Manager integration
     *       is not yet implemented.</li>
     *   <li>All other values are returned unchanged.</li>
     * </ul>
     *
     * @param ref the reference string; may be {@code null}, in which case
     *            {@code null} is returned
     * @return the resolved value, or {@code null} if {@code ref} is {@code null}
     * @throws SecretResolutionException if {@code ref} starts with {@code ENV:}
     *                                   and the environment variable is not set,
     *                                   or if {@code ref} starts with {@code SM:}
     */
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
            // Do not silently return a redacted placeholder: it would be used as a live
            // credential value (e.g. a JDBC password), producing auth failures that point
            // at the target system rather than at this unimplemented feature.
            throw new SecretResolutionException(
                SM_NOT_IMPLEMENTED + " (inline 'SM:' reference '" + ref + "')");
        }
        return ref;
    }

    /**
     * Resolves a credential reference using {@code "credential"} as the config
     * path label for error messages.
     *
     * @param cred the credential configuration to resolve; may be {@code null},
     *             in which case {@code null} is returned
     * @return the resolved credential value, or {@code null} if {@code cred} is
     *         {@code null}
     * @throws SecretResolutionException if a required credential cannot be
     *                                   resolved
     */
    public String resolveCredential(CredentialConfig cred) {
        return resolveCredential(cred, "credential");
    }

    /**
     * Resolves a credential reference, using {@code configPath} in error messages
     * to identify the field that could not be resolved.
     *
     * <p>Source types:
     * <ul>
     *   <li>{@code env} — reads the environment variable named by
     *       {@code CredentialConfig.envVar}.</li>
     *   <li>{@code secret-manager} / {@code sm} — throws
     *       {@link SecretResolutionException} because Secret Manager integration
     *       is not yet implemented.</li>
     *   <li>{@code plain} or unrecognised — returns the literal {@code value}
     *       field, or an empty string if it is {@code null}.</li>
     * </ul>
     *
     * @param cred       the credential configuration to resolve; may be
     *                   {@code null}, in which case {@code null} is returned
     * @param configPath a human-readable identifier for the config field, used
     *                   in exception messages
     * @return the resolved credential value; never {@code null} when {@code cred}
     *         is non-null
     * @throws SecretResolutionException if the credential is marked
     *                                   {@code required} and cannot be resolved
     */
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
            throw new SecretResolutionException(
                "Credential at '" + configPath + "' uses source 'secret-manager' but " + SM_NOT_IMPLEMENTED);
        }
        if (!"plain".equalsIgnoreCase(source) && source != null) {
            log.warn("Unrecognized credential source type '{}', treating as plain value", source);
        }
        return cred.getValue() != null ? trackSensitive(cred.getValue()) : "";
    }

    /**
     * Resolves a token reference using {@code "token"} as the config path label
     * for error messages.
     *
     * @param token the token configuration to resolve; may be {@code null},
     *              in which case {@code null} is returned
     * @return the resolved token value, or {@code null} if {@code token} is
     *         {@code null}
     * @throws SecretResolutionException if a required token cannot be resolved
     */
    public String resolveToken(TokenConfig token) {
        return resolveToken(token, "token");
    }

    /**
     * Resolves a token reference, using {@code configPath} in error messages to
     * identify the field that could not be resolved.
     *
     * <p>Source types:
     * <ul>
     *   <li>{@code env} — reads the environment variable named by
     *       {@code TokenConfig.envVar}.</li>
     *   <li>{@code secret-manager} — throws {@link SecretResolutionException}
     *       because Secret Manager integration is not yet implemented.</li>
     *   <li>Any other source, or a missing source — returns an empty string when
     *       the token is optional, or throws when it is {@code required}.</li>
     * </ul>
     *
     * @param token      the token configuration to resolve; may be {@code null},
     *                   in which case {@code null} is returned
     * @param configPath a human-readable identifier for the config field, used
     *                   in exception messages
     * @return the resolved token value; never {@code null} when {@code token} is
     *         non-null
     * @throws SecretResolutionException if the token is marked {@code required}
     *                                   and cannot be resolved, or if its source
     *                                   is {@code secret-manager}
     */
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
            throw new SecretResolutionException(
                "Token at '" + configPath + "' uses source 'secret-manager' but " + SM_NOT_IMPLEMENTED);
        }
        // Unrecognised or missing source. A required token silently resolving to "" would send an
        // empty 'Authorization: Bearer ' header on every request, so fail fast rather than quietly
        // running an unauthenticated load test.
        if (token.isRequired()) {
            throw new SecretResolutionException(
                "Token at '" + configPath + "' has unrecognised or missing source '" + source
                    + "'; expected 'env' or 'secret-manager'");
        }
        return "";
    }

    /**
     * Returns the redacted placeholder string, regardless of the value passed in.
     *
     * @param value the value to redact; unused
     * @return the constant redacted placeholder {@code ***REDACTED***}
     */
    public String redact(String value) {
        return REDACTED;
    }

    /**
     * Returns {@code true} if {@code value} is an inline secret reference that
     * requires resolution (i.e. starts with {@code ENV:} or {@code SM:}).
     *
     * @param value the string to test; may be {@code null}
     * @return {@code true} if the value starts with a known secret prefix
     */
    public boolean isSecretRef(String value) {
        if (value == null) return false;
        return value.startsWith(ENV_PREFIX) || value.startsWith(SM_PREFIX);
    }

    /**
     * Resolves each value in a headers map through the given {@link SecretResolver}.
     *
     * <p>Values prefixed with {@code ENV:} are replaced by the corresponding
     * environment variable; other values pass through unchanged. Returns an empty
     * map when {@code headerRefs} is {@code null}.
     *
     * @param headerRefs a map of header name to raw value (possibly an inline
     *                   {@code ENV:} reference); may be {@code null}
     * @param resolver   the resolver to use for each value
     * @return a new map of resolved header name-value pairs; never {@code null}
     * @throws SecretResolutionException if any {@code ENV:} reference names an
     *                                   environment variable that is not set
     */
    public static Map<String, String> resolveHeaders(Map<String, String> headerRefs, SecretResolver resolver) {
        if (headerRefs == null) return new HashMap<>();
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : headerRefs.entrySet()) {
            resolved.put(entry.getKey(), resolver.resolve(entry.getValue()));
        }
        return resolved;
    }
}
