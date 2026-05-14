package io.rampage.factory;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderRewriterTest {

    @Test
    void rewriteString_replacesFeederPlaceholdersInline() {
        assertEquals("/users/#{userId}/orders",
            PlaceholderRewriter.rewriteString("/users/${feeder:userId}/orders"));
    }

    @Test
    void rewriteString_replacesSessionPlaceholdersInline() {
        assertEquals("/orders/#{orderId}",
            PlaceholderRewriter.rewriteString("/orders/${session:orderId}"));
    }

    @Test
    void rewriteString_replacesMultipleMixedPlaceholders() {
        assertEquals("/users/#{userId}/orders/#{orderId}",
            PlaceholderRewriter.rewriteString("/users/${feeder:userId}/orders/${session:orderId}"));
    }

    @Test
    void rewriteString_isNullSafe() {
        assertNull(PlaceholderRewriter.rewriteString(null));
    }

    @Test
    void rewriteString_passesThroughLiteralsUnchanged() {
        assertEquals("/health", PlaceholderRewriter.rewriteString("/health"));
    }

    @Test
    void rewriteString_handlesPlaceholderWithSpecialReplacementChars() {
        // Matcher.quoteReplacement should keep "$" / "\" literal in the substituted name.
        assertEquals("#{a$b}", PlaceholderRewriter.rewriteString("${feeder:a$b}"));
    }

    @Test
    void rewriteVariableMap_onlyExactMatchValuesAreRewritten() {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("plain", "hello");
        in.put("exact", "${feeder:userId}");
        in.put("partial", "prefix-${feeder:userId}-suffix");
        Map<String, Object> out = PlaceholderRewriter.rewriteVariableMap(in);
        assertEquals("hello", out.get("plain"));
        assertEquals("#{userId}", out.get("exact"));
        assertEquals("prefix-${feeder:userId}-suffix", out.get("partial"));
    }

    @Test
    void rewriteVariableMap_preservesNonStringValues() {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("active", true);
        in.put("limit", 50);
        in.put("ratio", 1.5);
        in.put("optional", null);
        Map<String, Object> out = PlaceholderRewriter.rewriteVariableMap(in);
        assertEquals(Boolean.TRUE, out.get("active"));
        assertEquals(50, ((Number) out.get("limit")).intValue());
        assertEquals(1.5, ((Number) out.get("ratio")).doubleValue());
        assertTrue(out.containsKey("optional"));
        assertNull(out.get("optional"));
    }

    @Test
    void rewriteStringMap_rewritesAllValues() {
        Map<String, String> in = new LinkedHashMap<>();
        in.put("X-User", "${feeder:userId}");
        in.put("X-Trace", "trace-${session:traceId}");
        in.put("X-Plain", "literal");
        Map<String, String> out = PlaceholderRewriter.rewriteStringMap(in);
        assertEquals("#{userId}", out.get("X-User"));
        assertEquals("trace-#{traceId}", out.get("X-Trace"));
        assertEquals("literal", out.get("X-Plain"));
    }

    @Test
    void referencedSessionKeys_findsAllSessionRefs() {
        List<String> keys = PlaceholderRewriter.referencedSessionKeys(
            "/orders/${session:orderId}/items/${feeder:itemId}/from/${session:userId}");
        assertEquals(List.of("orderId", "userId"), keys);
    }

    @Test
    void referencedSessionKeys_returnsEmptyForNullOrLiteral() {
        assertTrue(PlaceholderRewriter.referencedSessionKeys(null).isEmpty());
        assertTrue(PlaceholderRewriter.referencedSessionKeys("/health").isEmpty());
    }
}
