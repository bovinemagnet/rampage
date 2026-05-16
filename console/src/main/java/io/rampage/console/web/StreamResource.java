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

@Path("/stream")
public class StreamResource {

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

    @GET
    @Path("/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    public Multi<String> statusStream() {
        return status.stream()
                .onOverflow().drop()
                .map(event -> Templates.status(event).render());
    }

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
