package io.rampage.factory;

import io.rampage.config.model.CredentialConfig;
import io.rampage.config.model.TokenConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    void resolve_envPrefixThrows_whenEnvVarNotSet() {
        // Assume TEST_NONEXISTENT_VAR_12345 is not set; a missing inline ENV: reference must fail fast.
        SecretResolutionException ex = assertThrows(SecretResolutionException.class,
            () -> secretResolver.resolve("ENV:TEST_NONEXISTENT_VAR_12345"));
        assertTrue(ex.getMessage().contains("TEST_NONEXISTENT_VAR_12345"));
    }

    @Test
    void resolve_envPrefixReturnsValue_whenEnvVarSet() {
        // Env vars cannot be set portably in-process, so assert against one that is already present.
        assumeTrue(System.getenv("PATH") != null, "PATH must be set for this test");
        String resolved = secretResolver.resolve("ENV:PATH");
        assertNotNull(resolved);
        assertFalse(resolved.isBlank());
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

    @Test
    void resolveCredential_throwsWhenRequiredEnvVarUnset() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("env");
        cred.setEnvVar("RAMPAGE_TEST_UNSET_VAR_XYZ");
        // required defaults to true

        SecretResolutionException ex = assertThrows(SecretResolutionException.class,
            () -> secretResolver.resolveCredential(cred, "environment.databases.sourceData.password"));
        assertTrue(ex.getMessage().contains("RAMPAGE_TEST_UNSET_VAR_XYZ"));
        assertTrue(ex.getMessage().contains("environment.databases.sourceData.password"));
    }

    @Test
    void resolveCredential_returnsEmptyWhenOptionalEnvVarUnset() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("env");
        cred.setEnvVar("RAMPAGE_TEST_UNSET_VAR_XYZ");
        cred.setRequired(false);

        assertEquals("", secretResolver.resolveCredential(cred, "test.path"));
    }

    @Test
    void resolveCredential_throwsWhenRequiredEnvVarNameBlank() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("env");
        cred.setEnvVar("");

        assertThrows(SecretResolutionException.class,
            () -> secretResolver.resolveCredential(cred, "test.path"));
    }

    @Test
    void resolveCredential_throwsWhenSecretManagerPathMissing() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("secret-manager");

        assertThrows(SecretResolutionException.class,
            () -> secretResolver.resolveCredential(cred, "test.path"));
    }

    @Test
    void resolveCredential_returnsPlainValue() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("plain");
        cred.setValue("hello");

        assertEquals("hello", secretResolver.resolveCredential(cred, "test"));
    }

    @Test
    void resolveToken_throwsWhenRequiredEnvVarUnset() {
        TokenConfig token = new TokenConfig();
        token.setSource("env");
        token.setEnvVar("RAMPAGE_TEST_UNSET_TOKEN_XYZ");

        SecretResolutionException ex = assertThrows(SecretResolutionException.class,
            () -> secretResolver.resolveToken(token, "environment.security.token"));
        assertTrue(ex.getMessage().contains("RAMPAGE_TEST_UNSET_TOKEN_XYZ"));
        assertTrue(ex.getMessage().contains("environment.security.token"));
    }

    @Test
    void resolveToken_returnsEmptyWhenOptionalAndUnset() {
        TokenConfig token = new TokenConfig();
        token.setSource("env");
        token.setEnvVar("RAMPAGE_TEST_UNSET_TOKEN_XYZ");
        token.setRequired(false);

        assertEquals("", secretResolver.resolveToken(token, "test"));
    }

    @Test
    void getSensitiveValues_tracksResolvedPlainCredentials() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("plain");
        cred.setValue("super-secret-pwd");

        secretResolver.resolveCredential(cred, "test.password");

        assertTrue(secretResolver.getSensitiveValues().contains("super-secret-pwd"));
    }

    @Test
    void getSensitiveValues_doesNotTrackEmpty() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("plain");
        cred.setValue("");

        secretResolver.resolveCredential(cred, "test.password");

        assertFalse(secretResolver.getSensitiveValues().contains(""));
    }

    @Test
    void getSensitiveValues_doesNotTrackRedactedPlaceholder() {
        CredentialConfig cred = new CredentialConfig();
        cred.setSource("secret-manager");
        cred.setSecretPath("vault/secret");

        secretResolver.resolveCredential(cred, "test.password");

        assertFalse(secretResolver.getSensitiveValues().stream().anyMatch(v -> v.equals("***REDACTED***")));
    }
}
