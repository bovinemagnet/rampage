package io.rampage.console.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsAggregatorTest {

    @Test
    void firstTickReturnsNullUntilBoundaryCrossed() {
        MetricsAggregator agg = new MetricsAggregator();

        assertThat(agg.ingest("rampage.sim.users.allUsers.active 5 1700000000")).isNull();
        assertThat(agg.ingest("rampage.sim.allRequests.all.count 12 1700000000")).isNull();
    }

    @Test
    void crossingTickBoundaryEmitsSnapshotOfPriorTick() {
        MetricsAggregator agg = new MetricsAggregator();
        agg.ingest("rampage.sim.users.allUsers.active 5 1700000000");
        agg.ingest("rampage.sim.allRequests.all.count 12 1700000000");
        agg.ingest("rampage.sim.allRequests.ko.count 1 1700000000");
        agg.ingest("rampage.sim.allRequests.all.percentiles50 30 1700000000");
        agg.ingest("rampage.sim.allRequests.all.percentiles95 90 1700000000");

        MetricSnapshot snap = agg.ingest("rampage.sim.users.allUsers.active 6 1700000001");

        assertThat(snap).isNotNull();
        assertThat(snap.tick()).isEqualTo(1700000000L);
        assertThat(snap.activeUsers()).isEqualTo(5);
        assertThat(snap.requestsPerTick()).isEqualTo(12);
        assertThat(snap.errorsPerTick()).isEqualTo(1);
        assertThat(snap.p50ResponseMs()).isEqualTo(30.0);
        assertThat(snap.p95ResponseMs()).isEqualTo(90.0);
    }

    @Test
    void unknownMetricsAreCapturedInRawMap() {
        MetricsAggregator agg = new MetricsAggregator();
        agg.ingest("rampage.sim.foo.bar 99 1700000000");
        MetricSnapshot snap = agg.ingest("rampage.sim.users.allUsers.active 1 1700000001");

        assertThat(snap.raw()).containsEntry("rampage.sim.foo.bar", 99.0);
        assertThat(snap.activeUsers()).isZero();
    }

    @Test
    void malformedLinesAreIgnored() {
        MetricsAggregator agg = new MetricsAggregator();
        assertThat(agg.ingest("garbage")).isNull();
        assertThat(agg.ingest("name notanumber 1700000000")).isNull();
        assertThat(agg.ingest("name 1.0 notatimestamp")).isNull();
        assertThat(agg.ingest("")).isNull();
        assertThat(agg.ingest(null)).isNull();
    }

    @Test
    void flushEmitsCurrentTickEvenWithoutBoundary() {
        MetricsAggregator agg = new MetricsAggregator();
        agg.ingest("rampage.sim.users.allUsers.active 7 1700000000");
        MetricSnapshot snap = agg.flush();
        assertThat(snap).isNotNull();
        assertThat(snap.activeUsers()).isEqualTo(7);
    }

    @Test
    void flushBeforeAnyIngestReturnsNull() {
        assertThat(new MetricsAggregator().flush()).isNull();
    }

    @Test
    void multiplePathsMatchingSuffixUseFirstSeen() {
        MetricsAggregator agg = new MetricsAggregator();
        agg.ingest("rampage.simA.users.allUsers.active 3 1700000000");
        agg.ingest("rampage.simB.users.allUsers.active 11 1700000000");
        MetricSnapshot snap = agg.ingest("rampage.x.tick 0 1700000001");
        // Suffix matching is intentionally first-wins; documents the contract.
        assertThat(snap.activeUsers()).isEqualTo(3);
    }
}
