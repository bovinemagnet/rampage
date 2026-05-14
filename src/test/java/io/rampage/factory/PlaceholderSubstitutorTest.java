package io.rampage.factory;

import io.rampage.config.model.RunConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderSubstitutorTest {

    private RunConfig run() {
        RunConfig run = new RunConfig();
        run.setId("test-run-id");
        run.setName("Test Run");
        run.setVersion(7);
        run.setEnvironment("perf");
        return run;
    }

    @Test
    void expand_runFieldId() {
        String result = PlaceholderSubstitutor.expand("X-Run-Id: ${run:id}", null, run(), new SecretResolver());
        assertEquals("X-Run-Id: test-run-id", result);
    }

    @Test
    void expand_runFieldVersion() {
        String result = PlaceholderSubstitutor.expand("v=${run:version}", null, run(), new SecretResolver());
        assertEquals("v=7", result);
    }

    @Test
    void expand_systemProperty() {
        System.setProperty("rampage.test.placeholder", "from-sys");
        try {
            String result = PlaceholderSubstitutor.expand("sys=${sys:rampage.test.placeholder}",
                null, run(), new SecretResolver());
            assertEquals("sys=from-sys", result);
        } finally {
            System.clearProperty("rampage.test.placeholder");
        }
    }

    @Test
    void expand_throwsOnUnknownRunField() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> PlaceholderSubstitutor.expand("${run:owner}", null, run(), new SecretResolver()));
        assertTrue(ex.getMessage().contains("owner"));
    }

    @Test
    void expand_collectsErrorsWhenMissing() {
        List<String> errors = new ArrayList<>();
        String result = PlaceholderSubstitutor.expand(
            "missing=${env:RAMPAGE_TEST_DEFINITELY_NOT_SET}",
            null, run(), new SecretResolver(), errors);
        assertEquals(1, errors.size());
        assertEquals("missing=", result);
    }

    @Test
    void expand_escapedPlaceholderPassesThrough() {
        String result = PlaceholderSubstitutor.expand("literal=\\${run:id}", null, run(), new SecretResolver());
        assertEquals("literal=${run:id}", result);
    }

    @Test
    void expand_handlesMultiplePlaceholders() {
        String result = PlaceholderSubstitutor.expand("id=${run:id} name=${run:name}",
            null, run(), new SecretResolver());
        assertEquals("id=test-run-id name=Test Run", result);
    }

    @Test
    void expand_passesThroughTextWithoutPlaceholders() {
        assertEquals("plain text", PlaceholderSubstitutor.expand("plain text", null, run(), new SecretResolver()));
    }

    @Test
    void expand_returnsNullForNullInput() {
        assertNull(PlaceholderSubstitutor.expand(null, null, run(), new SecretResolver()));
    }
}
