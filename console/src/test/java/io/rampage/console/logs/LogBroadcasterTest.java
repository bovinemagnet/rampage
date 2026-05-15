package io.rampage.console.logs;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LogBroadcasterTest {

    @Test
    void publishDoesNotThrowWhenSubscriberCannotKeepUp() {
        LogBroadcaster b = new LogBroadcaster();

        // Subscribe but never request — request(0) means downstream is full.
        Cancellable nonRequester = b.stream().subscribe().with(line -> {}, t -> {});

        // Hammer the broadcaster — the slow subscriber would normally trigger
        // BackPressureFailure on the very next onNext.
        assertThatCode(() -> {
            for (int i = 0; i < 1_000; i++) {
                b.publish(LogLine.of("r", "line " + i));
            }
        }).doesNotThrowAnyException();

        nonRequester.cancel();
    }

    @Test
    void fastSubscribersStillReceiveAfterSlowOneIsPresent() throws Exception {
        LogBroadcaster b = new LogBroadcaster();

        // Slow subscriber (no request).
        Cancellable slow = b.stream().subscribe().with(line -> {}, t -> {});

        // Fast subscriber with overflow handling.
        ConcurrentLinkedQueue<LogLine> received = new ConcurrentLinkedQueue<>();
        CountDownLatch latch = new CountDownLatch(3);
        Cancellable fast = Multi.createFrom().publisher(b.stream())
                .onOverflow().drop()
                .subscribe().with(line -> {
                    received.add(line);
                    latch.countDown();
                });

        b.publish(LogLine.of("r", "one"));
        b.publish(LogLine.of("r", "two"));
        b.publish(LogLine.of("r", "three"));

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).extracting(LogLine::text).contains("one", "two", "three");

        slow.cancel();
        fast.cancel();
    }
}
