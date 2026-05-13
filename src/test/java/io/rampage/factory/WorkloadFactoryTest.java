package io.rampage.factory;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.rampage.config.model.RateConfig;
import io.rampage.config.model.WorkloadConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class WorkloadFactoryTest {
    private WorkloadFactory workloadFactory;

    @BeforeEach
    void setUp() {
        workloadFactory = new WorkloadFactory();
    }

    private WorkloadConfig rampAndHold(double fromRate, double toRate, String rampUp, String holdFor) {
        WorkloadConfig wc = new WorkloadConfig();
        wc.setType("ramp-and-hold");
        RateConfig rate = new RateConfig();
        rate.setFrom(fromRate);
        rate.setTo(toRate);
        wc.setRate(rate);
        wc.setRampUp(rampUp);
        wc.setHoldFor(holdFor);
        return wc;
    }

    @Test
    void buildInjection_rampAndHold_returnsTwoSteps() {
        WorkloadConfig workload = rampAndHold(0.0, 10.0, "60s", "300s");
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps);
        assertEquals(2, steps.length);
    }

    @Test
    void buildInjection_rampAndHold_stepsNotNull() {
        WorkloadConfig workload = rampAndHold(0.0, 5.0, "30s", "120s");
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps[0]);
        assertNotNull(steps[1]);
    }

    @Test
    void buildInjection_smoke_returnsSingleStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("smoke");
        workload.setUsers(1);
        workload.setDuration("30s");
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps);
        assertEquals(1, steps.length);
    }

    @Test
    void buildInjection_soak_returnsSingleStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("soak");
        RateConfig rate = new RateConfig();
        rate.setTo(2.0);
        workload.setRate(rate);
        workload.setHoldFor("1h");
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps);
        assertEquals(1, steps.length);
    }

    @Test
    void buildInjection_unknownModel_returnsSingleStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("unknown-model");
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps);
        assertEquals(1, steps.length);
    }

    @Test
    void buildInjection_withHighTargetRate() {
        WorkloadConfig workload = rampAndHold(0.0, 50.0, "2m", "10m");
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertEquals(2, steps.length);
    }

    @Test
    void parseDuration_seconds() {
        assertEquals(Duration.ofSeconds(30), WorkloadFactory.parseDuration("30s", Duration.ZERO));
    }

    @Test
    void parseDuration_minutes() {
        assertEquals(Duration.ofMinutes(5), WorkloadFactory.parseDuration("5m", Duration.ZERO));
    }

    @Test
    void parseDuration_hours() {
        assertEquals(Duration.ofHours(2), WorkloadFactory.parseDuration("2h", Duration.ZERO));
    }

    @Test
    void parseDuration_nullReturnsDefault() {
        Duration def = Duration.ofSeconds(60);
        assertEquals(def, WorkloadFactory.parseDuration(null, def));
    }

    @Test
    void parseDuration_invalidReturnsDefault() {
        Duration def = Duration.ofSeconds(60);
        assertEquals(def, WorkloadFactory.parseDuration("invalid", def));
    }
}
