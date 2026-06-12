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

/**
 * JAX-RS resource for enqueueing and cancelling load-test runs under {@code /runs}.
 * Delegates lifecycle management to the {@code RunOrchestrator}.
 */
@Path("/runs")
public class RunsResource {

    /**
     * Creates a new {@code RunsResource} instance. CDI-managed; no arguments required.
     */
    public RunsResource() {}

    @Inject
    RunOrchestrator orchestrator;

    @Inject
    VerificationConfig verification;

    /**
     * Enqueues a new load-test run using the supplied environment and run config paths.
     * Returns HTTP 400 when either path is absent or blank.
     *
     * @param envPath path to the environment YAML config file
     * @param runPath path to the run YAML config file
     * @return HTTP 200 with a confirmation message, or HTTP 400 on missing input
     */
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

    /**
     * Enqueues the built-in verification run using the staged echo-service config.
     * Returns HTTP 503 when the verification config has not been successfully staged.
     *
     * @return HTTP 200 with a confirmation message, or HTTP 503 when the verification
     *         config is not ready
     */
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

    /**
     * Requests cancellation of the active or queued run identified by {@code id}.
     * Returns HTTP 204 on success, or HTTP 404 when no matching run exists.
     *
     * @param id the run identifier to cancel
     * @return HTTP 204 when the run was found and killed, HTTP 404 otherwise
     */
    @DELETE
    @Path("/{id}")
    public Response kill(@PathParam("id") String id) {
        return orchestrator.kill(id)
                .map(r -> Response.noContent().build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
