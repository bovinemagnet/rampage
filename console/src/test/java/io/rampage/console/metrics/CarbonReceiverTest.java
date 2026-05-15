package io.rampage.console.metrics;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(CarbonReceiverTest.RandomPortProfile.class)
class CarbonReceiverTest {

    public static class RandomPortProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // Port 0 asks Vert.x to pick a free port; the receiver exposes the
            // actual bound port via boundPort().
            return Map.of(
                    "rampage.console.carbon-port", "0",
                    "rampage.console.carbon-host", "127.0.0.1");
        }
    }

    @Inject
    MetricsBroadcaster broadcaster;

    @Inject
    CarbonReceiver receiver;

    @Test
    void receivesAndParsesCarbonLines() throws Exception {
        // The Vert.x server starts on the configured port (defaulting to 2003 in
        // application.properties). We connect, send one tick worth of lines plus
        // the first line of the next tick to flush a snapshot.
        AtomicReference<MetricSnapshot> received = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Cancellable sub = broadcaster.stream().subscribe().with(snap -> {
            if (received.compareAndSet(null, snap)) {
                latch.countDown();
            }
        });

        // Wait briefly for the StartupEvent observer to finish binding.
        for (int i = 0; i < 50 && receiver.boundPort() < 0; i++) {
            Thread.sleep(20);
        }
        assertThat(receiver.boundPort()).as("receiver bound to a port").isPositive();

        try (Socket socket = new Socket("127.0.0.1", receiver.boundPort());
             OutputStream out = socket.getOutputStream()) {
            String tick0 = String.join("",
                    "rampage.sim.users.allUsers.active 7 1700000010\n",
                    "rampage.sim.allRequests.all.count 50 1700000010\n",
                    "rampage.sim.allRequests.ko.count 2 1700000010\n",
                    "rampage.sim.allRequests.all.percentiles50 25 1700000010\n",
                    "rampage.sim.allRequests.all.percentiles95 80 1700000010\n");
            String tick1Probe = "rampage.sim.users.allUsers.active 8 1700000011\n";
            out.write(tick0.getBytes(StandardCharsets.UTF_8));
            out.write(tick1Probe.getBytes(StandardCharsets.UTF_8));
            out.flush();

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            sub.cancel();
        }

        MetricSnapshot snap = received.get();
        assertThat(snap).isNotNull();
        assertThat(snap.tick()).isEqualTo(1700000010L);
        assertThat(snap.activeUsers()).isEqualTo(7);
        assertThat(snap.requestsPerTick()).isEqualTo(50);
        assertThat(snap.errorsPerTick()).isEqualTo(2);
        assertThat(snap.p50ResponseMs()).isEqualTo(25.0);
        assertThat(snap.p95ResponseMs()).isEqualTo(80.0);
    }
}
