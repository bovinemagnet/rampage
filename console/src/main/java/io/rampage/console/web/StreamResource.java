package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.rampage.console.logs.LogBroadcaster;
import io.rampage.console.logs.LogLine;
import io.rampage.console.metrics.MetricSnapshot;
import io.rampage.console.metrics.MetricsBroadcaster;
import io.rampage.console.orchestrator.OrchestratorView;
import io.rampage.console.orchestrator.RunOrchestrator;
import io.rampage.console.orchestrator.RunStatusBroadcaster;
import io.rampage.console.orchestrator.RunStatusEvent;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

/**
 * JAX-RS resource exposing Server-Sent Event streams under {@code /stream}.
 * Each endpoint pushes pre-rendered Qute HTML fragments to connected browsers,
 * enabling live dashboard updates without full-page reloads.
 */
@Path("/stream")
public class StreamResource {

    /**
     * Creates a new {@code StreamResource} instance. CDI-managed; no arguments required.
     */
    public StreamResource() {}

    @CheckedTemplate(basePath = "Stream")
    static class Templates {
        public static native TemplateInstance logLine(LogLine line);
        public static native TemplateInstance status(RunStatusEvent event);
        public static native TemplateInstance metrics(MetricSnapshot snapshot);
        public static native TemplateInstance queue(OrchestratorView view);
    }

    @Inject
    LogBroadcaster logs;

    @Inject
    RunStatusBroadcaster status;

    @Inject
    MetricsBroadcaster metrics;

    @Inject
    RunOrchestrator orchestrator;

    /**
     * Streams rendered log-line HTML fragments as Server-Sent Events.
     * Excess items are silently dropped on overflow; the live log tail is inherently lossy.
     *
     * @return a reactive stream of rendered log-line HTML fragments
     */
    @GET
    @Path("/logs")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    public Multi<String> logStream() {
        // Lossy is fine for the live log tail — Gatling's stdout can spike
        // faster than any one browser will request items.
        return logs.stream()
                .onOverflow().drop()
                .map(line -> Templates.logLine(line).render());
    }

    /**
     * Streams rendered run-status HTML fragments as Server-Sent Events.
     * Excess items are silently dropped on overflow.
     *
     * @return a reactive stream of rendered run-status HTML fragments
     */
    @GET
    @Path("/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    public Multi<String> statusStream() {
        return status.stream()
                .onOverflow().drop()
                .map(event -> Templates.status(event).render());
    }

    /**
     * Streams rendered metrics-snapshot HTML fragments as Server-Sent Events.
     * Only the latest snapshot is emitted when the downstream cannot keep up;
     * previous items are coalesced on overflow.
     *
     * @return a reactive stream of rendered metrics-snapshot HTML fragments
     */
    @GET
    @Path("/metrics")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    public Multi<String> metricsStream() {
        // Only the latest snapshot matters; coalesce on overflow.
        return metrics.stream()
                .onOverflow().dropPreviousItems()
                .map(snap -> Templates.metrics(snap).render());
    }

    /**
     * Re-renders the queue panel each time a run transitions state. Avoids a
     * dedicated broadcaster — every queue change goes hand-in-hand with a
     * status event already.
     *
     * @return a reactive stream of rendered queue-panel HTML fragments
     */
    @GET
    @Path("/queue")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    public Multi<String> queueStream() {
        return status.stream()
                .onOverflow().dropPreviousItems()
                .map(event -> Templates.queue(orchestrator.snapshot()).render());
    }
}
