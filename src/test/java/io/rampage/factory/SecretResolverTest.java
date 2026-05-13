package io.rampage.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecretResolverTest {
    private SecretResolver secretResolver;

    @BeforeEach
    void setUp() {
        secretResolver = new SecretResolver();
    }

    @Test
    void resolve_plainStringReturnedAsIs() {
        assertEquals("plain-value", secretResolver.resolve("plain-value"));
    }

    @Test
    void resolve_nullReturnsNull() {
        assertNull(secretResolver.resolve(null));
    }

    @Test
    void resolve_smPrefixReturnsRedacted() {
        assertEquals("***REDACTED***", secretResolver.resolve("SM:secret/my-secret"));
    }

    @Test
    void resolve_envPrefixReturnsEmpty_whenEnvVarNotSet() {
        // Assume TEST_NONEXISTENT_VAR_12345 is not set
        assertEquals("", secretResolver.resolve("ENV:TEST_NONEXISTENT_VAR_12345"));
    }

    @Test
    void redact_alwaysReturnsRedacted() {
        assertEquals("***REDACTED***", secretResolver.redact("any-value"));
        assertEquals("***REDACTED***", secretResolver.redact(""));
        assertEquals("***REDACTED***", secretResolver.redact(null));
    }

    @Test
    void isSecretRef_trueForEnvPrefix() {
        assertTrue(secretResolver.isSecretRef("ENV:MY_VAR"));
    }

    @Test
    void isSecretRef_trueForSmPrefix() {
        assertTrue(secretResolver.isSecretRef("SM:my/secret"));
    }

    @Test
    void isSecretRef_falseForPlainString() {
        assertFalse(secretResolver.isSecretRef("plain-value"));
    }

    @Test
    void isSecretRef_falseForNull() {
        assertFalse(secretResolver.isSecretRef(null));
    }

    @Test
    void resolveHeaders_resolvesAllValues() {
        Map<String, String> headers = Map.of(
            "X-Plain", "plain-value",
            "X-Secret", "SM:my-secret"
        );
        Map<String, String> resolved = SecretResolver.resolveHeaders(headers, secretResolver);
        assertEquals("plain-value", resolved.get("X-Plain"));
        assertEquals("***REDACTED***", resolved.get("X-Secret"));
    }

    @Test
    void resolveHeaders_handlesNullInput() {
        Map<String, String> resolved = SecretResolver.resolveHeaders(null, secretResolver);
        assertNotNull(resolved);
        assertTrue(resolved.isEmpty());
    }
}
