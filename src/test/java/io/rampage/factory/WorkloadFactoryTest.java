package io.rampage.factory;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.rampage.config.model.WorkloadConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkloadFactoryTest {
    private WorkloadFactory workloadFactory;

    @BeforeEach
    void setUp() {
        workloadFactory = new WorkloadFactory();
    }

    private WorkloadConfig rampAndHold(double targetCPS, long rampSecs, long holdSecs) {
        WorkloadConfig wc = new WorkloadConfig();
        wc.setModel("ramp-and-hold");
        wc.setTargetCallsPerSecond(targetCPS);
        wc.setRampDurationSeconds(rampSecs);
        wc.setHoldDurationSeconds(holdSecs);
        return wc;
    }

    @Test
    void buildInjection_rampAndHold_returnsTwoSteps() {
        WorkloadConfig workload = rampAndHold(10.0, 60, 300);
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps);
        assertEquals(2, steps.length);
    }

    @Test
    void buildInjection_rampAndHold_stepsNotNull() {
        WorkloadConfig workload = rampAndHold(5.0, 30, 120);
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps[0]);
        assertNotNull(steps[1]);
    }

    @Test
    void buildInjection_unknownModel_returnsSingleStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setModel("unknown-model");
        workload.setTargetCallsPerSecond(5.0);
        workload.setHoldDurationSeconds(60);
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertNotNull(steps);
        assertEquals(1, steps.length);
    }

    @Test
    void buildInjection_withHighTargetCPS() {
        WorkloadConfig workload = rampAndHold(50.0, 120, 600);
        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);
        assertEquals(2, steps.length);
    }
}
