package io.rampage.console.metrics;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.parsetools.RecordParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Quarkus-managed Vert.x TCP server that speaks the Graphite Carbon plain-text
 * protocol. Gatling's stock GraphiteDataWriter pushes per-tick metric lines to
 * this socket once per write period (1 s by default) when the console launches
 * the simulation with {@code -Drampage.console.extra-writer=graphite}.
 *
 * Each line is fed through a per-connection {@link MetricsAggregator}; tick
 * boundaries flush a {@link MetricSnapshot} onto the {@link MetricsBroadcaster}.
 */
@ApplicationScoped
public class CarbonReceiver {

    private static final Logger log = LoggerFactory.getLogger(CarbonReceiver.class);

    @Inject
    Vertx vertx;

    @Inject
    MetricsBroadcaster broadcaster;

    @ConfigProperty(name = "rampage.console.carbon-port", defaultValue = "2003")
    int port;

    @ConfigProperty(name = "rampage.console.carbon-host", defaultValue = "0.0.0.0")
    String host;

    private NetServer server;
    private volatile int boundPort = -1;

    /** @return the port the listener actually bound to, or -1 if not yet listening. */
    public int boundPort() {
        return boundPort;
    }

    void onStart(@Observes StartupEvent ev) {
        server = vertx.createNetServer();
        server.connectHandler(socket -> {
            log.info("Carbon receiver: connection from {}", socket.remoteAddress());
            MetricsAggregator agg = new MetricsAggregator();
            RecordParser parser = RecordParser.newDelimited("\n", buffer -> {
                String line = buffer.toString(StandardCharsets.UTF_8);
                MetricSnapshot snap = agg.ingest(line);
                if (snap != null) {
                    broadcaster.publish(snap);
                }
            });
            socket.handler(parser);
            socket.closeHandler(v -> {
                MetricSnapshot tail = agg.flush();
                if (tail != null) {
                    broadcaster.publish(tail);
                }
                log.info("Carbon receiver: connection closed");
            });
            socket.exceptionHandler(t ->
                    log.warn("Carbon receiver: socket error: {}", t.getMessage()));
        });
        server.listen(port, host)
                .onSuccess(s -> {
                    boundPort = s.actualPort();
                    log.info("Carbon receiver listening on {}:{}", host, boundPort);
                })
                .onFailure(t -> log.error("Carbon receiver failed to bind {}:{}: {}", host, port, t.getMessage()));
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (server != null) {
            server.close();
        }
    }
}
