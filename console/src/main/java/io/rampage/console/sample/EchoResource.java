package io.rampage.console.sample;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sample REST endpoints exposed under {@code /verification/rest} for local
 * sanity checks (curl, browsers, smoke tests). Note: Rampage's current
 * {@code ScenarioFactory} only wires the GraphQL POST path, so these endpoints
 * are not load-tested by the verification scenario — see {@link EchoGraphQL}
 * for that. They remain useful for demos, manual probes, and as targets for
 * future REST-protocol scenarios.
 */
@Path("/verification/rest")
public class EchoResource {

    /**
     * Creates a new {@code EchoResource} instance. CDI-managed; no arguments required.
     */
    public EchoResource() {}

    private final AtomicLong throttleWindowStart = new AtomicLong(0);
    private final AtomicLong throttleWindowCount = new AtomicLong(0);

    /**
     * Returns an echo payload containing the supplied message and the current server timestamp.
     * Defaults to {@code "pong"} when no {@code msg} query parameter is provided.
     *
     * @param msg the message to echo; may be {@code null}
     * @return the echoed payload with a server-side timestamp
     */
    @GET
    @Path("/echo")
    @Produces(MediaType.APPLICATION_JSON)
    public EchoPayload echoGet(@QueryParam("msg") String msg) {
        return new EchoPayload(msg == null ? "pong" : msg, System.currentTimeMillis());
    }

    /**
     * Returns the posted payload unchanged, stamping a server-side timestamp when
     * the request body omits one.
     *
     * @param body the request payload; treated as an empty payload when {@code null}
     * @return the (possibly timestamped) payload
     */
    @POST
    @Path("/echo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public EchoPayload echoPost(EchoPayload body) {
        if (body == null) {
            body = new EchoPayload();
        }
        if (body.timestamp == null) {
            body.timestamp = System.currentTimeMillis();
        }
        return body;
    }

    /**
     * Sleeps for the requested number of milliseconds before returning an echo payload.
     * The sleep duration is clamped to the range {@code [0, 30 000]}; defaults to 250 ms
     * when the {@code ms} parameter is absent.
     *
     * @param ms the sleep duration in milliseconds; clamped and defaulted as described
     * @return an echo payload whose {@code msg} encodes the actual sleep duration
     * @throws InterruptedException if the sleeping thread is interrupted
     */
    @GET
    @Path("/slow")
    @Produces(MediaType.APPLICATION_JSON)
    public EchoPayload slow(@QueryParam("ms") Integer ms) throws InterruptedException {
        int sleep = ms == null ? 250 : Math.min(Math.max(ms, 0), 30_000);
        Thread.sleep(sleep);
        return new EchoPayload("slept-" + sleep + "ms", System.currentTimeMillis());
    }

    /**
     * Returns a response bearing the requested HTTP status code and a JSON error body.
     * Defaults to 500 when the {@code code} parameter is absent; also defaults to 500
     * when the supplied value falls outside the valid range {@code [100, 599]}.
     *
     * @param code the HTTP status code to force; defaults to 500 when {@code null} or invalid
     * @return a response with the forced status and a JSON {@code {"error":...}} body
     */
    @GET
    @Path("/fail")
    public Response fail(@QueryParam("code") Integer code) {
        int status = code == null ? 500 : code;
        if (status < 100 || status > 599) {
            status = 500;
        }
        return Response.status(status)
                .entity("{\"error\":\"forced status " + status + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Returns 429 once a per-second cap is exceeded. Useful for proving
     * Rampage's error-rate assertions and back-pressure handling.
     *
     * @param qps the maximum number of successful requests per second; defaults to 5 when
     *            {@code null}; enforced to a minimum of 1
     * @return HTTP 200 with an echo payload when within the cap, HTTP 429 otherwise
     */
    @GET
    @Path("/throttle")
    @Produces(MediaType.APPLICATION_JSON)
    public Response throttle(@QueryParam("qps") Integer qps) {
        int cap = qps == null ? 5 : Math.max(qps, 1);
        long now = System.currentTimeMillis() / 1000;
        long windowStart = throttleWindowStart.get();
        if (now != windowStart) {
            throttleWindowStart.set(now);
            throttleWindowCount.set(0);
        }
        long count = throttleWindowCount.incrementAndGet();
        if (count > cap) {
            return Response.status(429)
                    .entity("{\"error\":\"throttled\",\"qps\":" + cap + "}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.ok(new EchoPayload("ok", System.currentTimeMillis())).build();
    }
}
