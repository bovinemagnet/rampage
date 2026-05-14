package io.rampage.factory;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BodyRedactorTest {

    @Test
    void redact_replacesJsonFieldByPath() {
        String body = "{\"data\":{\"user\":{\"email\":\"alice@example.com\",\"name\":\"Alice\"}}}";
        String result = BodyRedactor.redact(body, List.of("$.data.user.email"), Set.of());

        assertTrue(result.contains("***REDACTED***"));
        assertFalse(result.contains("alice@example.com"));
        assertTrue(result.contains("Alice"), "Non-targeted fields preserved");
    }

    @Test
    void redact_replacesSubstringSensitiveValues() {
        String body = "Token=super-secret-token, other=plain";
        String result = BodyRedactor.redact(body, List.of(), Set.of("super-secret-token"));

        assertTrue(result.contains("***REDACTED***"));
        assertFalse(result.contains("super-secret-token"));
        assertTrue(result.contains("plain"));
    }

    @Test
    void redact_handlesNonJsonBody() {
        String body = "plain text";
        String result = BodyRedactor.redact(body, List.of("$.foo"), Set.of());
        assertEquals("plain text", result);
    }

    @Test
    void redact_handlesNullInputs() {
        assertNull(BodyRedactor.redact(null, List.of(), Set.of()));
        assertEquals("", BodyRedactor.redact("", List.of("$.foo"), Set.of()));
    }

    @Test
    void redact_combinesJsonAndSubstring() {
        String body = "{\"token\":\"abc\",\"key\":\"xyz\"}";
        String result = BodyRedactor.redact(body, List.of("$.token"), Set.of("xyz"));
        assertFalse(result.contains("\"abc\""));
        assertFalse(result.contains("xyz"));
        assertEquals(2, countOccurrences(result, "***REDACTED***"));
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
