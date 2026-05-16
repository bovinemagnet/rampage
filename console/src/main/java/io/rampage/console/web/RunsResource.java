package io.rampage.console.web;

import io.rampage.console.orchestrator.RunOrchestrator;
import io.rampage.console.orchestrator.RunRecord;
import io.rampage.console.verification.VerificationConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/runs")
public class RunsResource {

    @Inject
    RunOrchestrator orchestrator;

    @Inject
    VerificationConfig verification;

    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response enqueue(@FormParam("envPath") String envPath,
                            @FormParam("runPath") String runPath) {
        if (envPath == null || envPath.isBlank() || runPath == null || runPath.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("envPath and runPath are required")
                    .build();
        }
        RunRecord rec = orchestrator.enqueue(envPath, runPath);
        return Response.ok("Queued " + rec.id()).build();
    }

    @POST
    @Path("/verify")
    @Produces(MediaType.TEXT_HTML)
    public Response verify() {
        if (!verification.isReady()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Verification config did not stage successfully — check the console logs.")
                    .build();
        }
        RunRecord rec = orchestrator.enqueue(
                verification.envPath().toString(),
                verification.runPath().toString());
        return Response.ok("Verify queued: " + rec.id()).build();
    }

    @DELETE
    @Path("/{id}")
    public Response kill(@PathParam("id") String id) {
        return orchestrator.kill(id)
                .map(r -> Response.noContent().build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
