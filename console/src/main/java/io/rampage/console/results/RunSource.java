package io.rampage.console.results;

/** How a {@link StoredRun} entered the results store. */
public enum RunSource {
    /** Ingested live when a console-launched run finished. */
    CONSOLE,
    /** Discovered by the startup backfill scan of build/reports/gatling/. */
    IMPORTED
}
