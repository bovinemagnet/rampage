package io.rampage.factory;

import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.rampage.config.model.ExecutionConfig;
import io.rampage.config.model.RateConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioWorkloadConfig;
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

    private RunConfig runWith(WorkloadConfig wc) {
        RunConfig run = new RunConfig();
        ExecutionConfig exec = new ExecutionConfig();
        exec.setWorkload(wc);
        run.setExecution(exec);
        return run;
    }

    @Test
    void effectiveWorkload_inheritsFromRunByDefault() {
        WorkloadConfig runWorkload = rampAndHold(0.0, 10.0, "60s", "300s");
        ScenarioConfig sc = new ScenarioConfig();

        WorkloadConfig effective = WorkloadFactory.effectiveWorkload(runWith(runWorkload), sc);

        assertSame(runWorkload, effective);
    }

    @Test
    void effectiveWorkload_usesScenarioOverrideWhenInheritFalse() {
        WorkloadConfig runWorkload = rampAndHold(0.0, 10.0, "60s", "300s");
        ScenarioWorkloadConfig scenarioWorkload = new ScenarioWorkloadConfig();
        scenarioWorkload.setInheritFromRun(false);
        scenarioWorkload.setType("constant");
        RateConfig rate = new RateConfig();
        rate.setTo(2.0);
        scenarioWorkload.setRate(rate);
        scenarioWorkload.setHoldFor("45s");

        ScenarioConfig sc = new ScenarioConfig();
        sc.setWorkload(scenarioWorkload);

        WorkloadConfig effective = WorkloadFactory.effectiveWorkload(runWith(runWorkload), sc);

        assertEquals("constant", effective.getType());
        assertEquals("45s", effective.getHoldFor());
        assertEquals(2.0, effective.getRate().getTo());
    }

    @Test
    void buildInjection_baseline_returnsConstantStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("baseline");
        RateConfig rate = new RateConfig();
        rate.setTo(5.0);
        workload.setRate(rate);
        workload.setHoldFor("90s");

        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);

        assertEquals(1, steps.length);
        assertNotNull(steps[0]);
    }

    @Test
    void buildInjection_spike_returnsFourSteps() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("spike");
        RateConfig rate = new RateConfig();
        rate.setFrom(1.0);
        rate.setTo(50.0);
        workload.setRate(rate);
        workload.setRampUp("30s");
        workload.setSpikeDuration("2s");
        workload.setHoldFor("60s");

        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);

        // warm-up + ramp-up + hold + ramp-down
        assertEquals(4, steps.length);
        for (OpenInjectionStep s : steps) {
            assertNotNull(s);
        }
    }

    @Test
    void buildInjection_spike_usesBaselineRateFieldWhenSet() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("spike");
        RateConfig rate = new RateConfig();
        rate.setTo(50.0);
        workload.setRate(rate);
        workload.setBaselineRate(5.0);
        workload.setRampUp("10s");
        workload.setSpikeDuration("1s");
        workload.setHoldFor("10s");

        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);

        assertEquals(4, steps.length);
    }

    @Test
    void buildInjection_stress_returnsSteppedSeries() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("stress");
        RateConfig rate = new RateConfig();
        rate.setFrom(1.0);
        rate.setTo(10.0);
        workload.setRate(rate);
        workload.setStepRate(2.0);
        workload.setStepDuration("30s");
        workload.setMaxRate(10.0);

        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);

        // 1, 3, 5, 7, 9, 10 → 6 steps
        assertEquals(6, steps.length);
    }

    @Test
    void buildInjection_stress_requiresPositiveStepRate() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("stress");
        workload.setStepRate(0.0);

        assertThrows(IllegalArgumentException.class,
            () -> workloadFactory.buildInjection(workload));
    }

    @Test
    void buildClosedInjection_rampAndHold_returnsTwoSteps() {
        WorkloadConfig workload = rampAndHold(0.0, 5.0, "30s", "60s");
        ClosedInjectionStep[] steps = workloadFactory.buildClosedInjection(workload);
        assertEquals(2, steps.length);
    }

    @Test
    void buildClosedInjection_smoke_returnsSingleStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("smoke");
        workload.setUsers(3);
        workload.setDuration("10s");

        ClosedInjectionStep[] steps = workloadFactory.buildClosedInjection(workload);
        assertEquals(1, steps.length);
    }

    @Test
    void buildClosedInjection_constant_returnsSingleStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("constant");
        RateConfig rate = new RateConfig();
        rate.setTo(4.0);
        workload.setRate(rate);
        workload.setHoldFor("90s");

        ClosedInjectionStep[] steps = workloadFactory.buildClosedInjection(workload);
        assertEquals(1, steps.length);
    }

    @Test
    void buildClosedInjection_rejectsSpike() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("spike");

        assertThrows(IllegalArgumentException.class,
            () -> workloadFactory.buildClosedInjection(workload));
    }

    @Test
    void buildClosedInjection_rejectsStress() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("stress");

        assertThrows(IllegalArgumentException.class,
            () -> workloadFactory.buildClosedInjection(workload));
    }

    @Test
    void scaleWorkload_halvesRate() {
        WorkloadConfig base = rampAndHold(2.0, 20.0, "60s", "300s");
        WorkloadConfig scaled = WorkloadFactory.scaleWorkload(base, 0.5);

        assertEquals(1.0, scaled.getRate().getFrom());
        assertEquals(10.0, scaled.getRate().getTo());
        assertEquals("60s", scaled.getRampUp());
        assertNotSame(base, scaled, "Should return a new instance");
    }

    @Test
    void scaleWorkload_scalesUsersWithFloorOfOne() {
        WorkloadConfig base = new WorkloadConfig();
        base.setType("smoke");
        base.setUsers(10);

        WorkloadConfig scaled = WorkloadFactory.scaleWorkload(base, 0.05);
        assertEquals(1, scaled.getUsers(), "Floor at 1 user");
    }

    @Test
    void scaleWorkload_scalesAuxiliaryRates() {
        WorkloadConfig base = new WorkloadConfig();
        base.setType("spike");
        base.setBaselineRate(2.0);
        base.setStepRate(5.0);
        base.setMaxRate(100.0);

        WorkloadConfig scaled = WorkloadFactory.scaleWorkload(base, 0.5);
        assertEquals(1.0, scaled.getBaselineRate());
        assertEquals(2.5, scaled.getStepRate());
        assertEquals(50.0, scaled.getMaxRate());
    }

    @Test
    void scaleWorkload_handlesNull() {
        assertNull(WorkloadFactory.scaleWorkload(null, 0.5));
    }

    @Test
    void effectiveWorkload_fallsBackToSmokeWhenNothingConfigured() {
        WorkloadConfig effective = WorkloadFactory.effectiveWorkload(new RunConfig(), new ScenarioConfig());
        assertEquals("smoke", effective.getType());
        assertEquals(1, effective.getUsers());
    }

    @Test
    void resolveHoldDuration_prefersDurationOverDefault() {
        WorkloadConfig wc = new WorkloadConfig();
        wc.setDuration("1h");
        assertEquals(Duration.ofHours(1),
            WorkloadFactory.resolveHoldDuration(wc, Duration.ofSeconds(60)));
    }

    @Test
    void resolveHoldDuration_fallsBackToHoldForWhenDurationAbsent() {
        WorkloadConfig wc = new WorkloadConfig();
        wc.setHoldFor("45s");
        assertEquals(Duration.ofSeconds(45),
            WorkloadFactory.resolveHoldDuration(wc, Duration.ofSeconds(60)));
    }

    @Test
    void resolveHoldDuration_durationWinsWhenBothSet() {
        WorkloadConfig wc = new WorkloadConfig();
        wc.setDuration("1h");
        wc.setHoldFor("45s");
        assertEquals(Duration.ofHours(1),
            WorkloadFactory.resolveHoldDuration(wc, Duration.ofSeconds(60)));
    }

    @Test
    void resolveHoldDuration_usesDefaultWhenNeitherSet() {
        WorkloadConfig wc = new WorkloadConfig();
        Duration def = Duration.ofSeconds(60);
        assertEquals(def, WorkloadFactory.resolveHoldDuration(wc, def));
    }

    @Test
    void buildInjection_constant_withDuration_returnsSingleStep() {
        WorkloadConfig workload = new WorkloadConfig();
        workload.setType("constant");
        RateConfig rate = new RateConfig();
        rate.setTo(4.0);
        workload.setRate(rate);
        workload.setDuration("1h");

        OpenInjectionStep[] steps = workloadFactory.buildInjection(workload);

        assertNotNull(steps);
        assertEquals(1, steps.length);
    }
}
