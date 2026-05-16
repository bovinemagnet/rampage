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

    private final AtomicLong throttleWindowStart = new AtomicLong(0);
    private final AtomicLong throttleWindowCount = new AtomicLong(0);

    @GET
    @Path("/echo")
    @Produces(MediaType.APPLICATION_JSON)
    public EchoPayload echoGet(@QueryParam("msg") String msg) {
        return new EchoPayload(msg == null ? "pong" : msg, System.currentTimeMillis());
    }

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

    @GET
    @Path("/slow")
    @Produces(MediaType.APPLICATION_JSON)
    public EchoPayload slow(@QueryParam("ms") Integer ms) throws InterruptedException {
        int sleep = ms == null ? 250 : Math.min(Math.max(ms, 0), 30_000);
        Thread.sleep(sleep);
        return new EchoPayload("slept-" + sleep + "ms", System.currentTimeMillis());
    }

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
