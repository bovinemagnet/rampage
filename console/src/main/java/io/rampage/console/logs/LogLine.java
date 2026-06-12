package io.rampage.console.logs;

import java.time.Instant;

/**
 * A single line of output captured from a Gatling simulation process.
 *
 * @param runId the identifier of the run that produced this line.
 * @param at    the wall-clock instant at which the line was captured.
 * @param text  the raw text content of the line.
 */
public record LogLine(String runId, Instant at, String text) {

    /**
     * Creates a {@code LogLine} timestamped to the current instant.
     *
     * @param runId the identifier of the run that produced this line.
     * @param text  the raw text content of the line.
     * @return a new {@code LogLine} with {@code at} set to {@link Instant#now()}.
     */
    public static LogLine of(String runId, String text) {
        return new LogLine(runId, Instant.now(), text);
    }
}
