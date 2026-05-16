package io.rampage.console.logs;

import java.time.Instant;

public record LogLine(String runId, Instant at, String text) {

    public static LogLine of(String runId, String text) {
        return new LogLine(runId, Instant.now(), text);
    }
}
