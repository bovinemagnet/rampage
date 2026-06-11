package io.rampage.factory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaticTokenProviderTest {

    @Test
    void currentToken_returnsConstructorValue() {
        assertEquals("abc-123", new StaticTokenProvider("abc-123").currentToken());
    }

    @Test
    void currentToken_returnsSameValueOnEveryCall() {
        StaticTokenProvider provider = new StaticTokenProvider("stable");
        assertEquals(provider.currentToken(), provider.currentToken());
    }

    @Test
    void currentToken_passesThroughNullAndBlank() {
        assertNull(new StaticTokenProvider(null).currentToken());
        assertEquals("", new StaticTokenProvider("").currentToken());
    }
}
