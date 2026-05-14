package io.rampage.factory;

import io.rampage.config.model.ChecksConfig;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.ExecutionConfig;
import io.rampage.config.model.ExtractConfig;
import io.rampage.config.model.HttpConfig;
import io.rampage.config.model.RateConfig;
import io.rampage.config.model.RequestConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.SafetyConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioRef;
import io.rampage.config.model.StepConfig;
import io.rampage.config.model.WorkloadConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the step/REST/extraction validation rules added alongside multi-step support.
 * Kept in a dedicated file from the legacy {@code ConfigValidatorTest} so the original
 * suite stays focused on the pre-existing rules.
 */
class ConfigValidatorStepsTest {

    private ConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigValidator();
    }

    private EnvironmentConfig env() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        env.setName("Test Environment");
        env.setBaseUrls(Map.of("rest", "http://localhost:8080"));
        HttpConfig http = new HttpConfig();
        http.setConnectTimeoutMillis(5000);
        http.setRequestTimeoutMillis(30000);
        env.setHttp(http);
        SafetyConfig safety = new SafetyConfig();
        safety.setAllowProduction(false);
        env.setSafety(safety);
        return env;
    }

    private RunConfig run(String scenarioId) {
        RunConfig run = new RunConfig();
        run.setId("run-id");
        run.setName("run-name");
        ScenarioRef ref = new ScenarioRef();
        ref.setId(scenarioId);
        ref.setEnabled(true);
        run.setScenarios(List.of(ref));
        ExecutionConfig exec = new ExecutionConfig();
        WorkloadConfig wc = new WorkloadConfig();
        wc.setType("smoke");
        RateConfig rate = new RateConfig();
        rate.setFrom(1);
        rate.setTo(1);
        wc.setRate(rate);
        exec.setWorkload(wc);
        run.setExecution(exec);
        return run;
    }

    private ScenarioConfig scenario(String id) {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId(id);
        sc.setName(id);
        return sc;
    }

    private StepConfig step(String name, String method, String path) {
        StepConfig s = new StepConfig();
        s.setName(name);
        RequestConfig r = new RequestConfig();
        r.setMethod(method);
        r.setPath(path);
        r.setBodyType("none");
        s.setRequest(r);
        return s;
    }

    @Test
    void singleStepRestScenarioValidates() {
        ScenarioConfig sc = scenario("rest-single");
        sc.setSteps(List.of(step("get-user", "GET", "/users/123")));
        assertDoesNotThrow(() -> validator.validate(env(), run("rest-single"), List.of(sc)));
    }

    @Test
    void unknownHttpMethodFails() {
        ScenarioConfig sc = scenario("bad-method");
        sc.setSteps(List.of(step("step-1", "FETCH", "/users")));
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("bad-method"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("FETCH")));
    }

    @Test
    void unknownBodyTypeFails() {
        ScenarioConfig sc = scenario("bad-body-type");
        StepConfig step = step("step-1", "POST", "/x");
        step.getRequest().setBodyType("xml");
        sc.setSteps(List.of(step));
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("bad-body-type"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("xml")));
    }

    @Test
    void jsonBodyTypeRequiresBodyOrBodyFile() {
        ScenarioConfig sc = scenario("missing-body");
        StepConfig step = step("step-1", "POST", "/x");
        step.getRequest().setBodyType("json");
        sc.setSteps(List.of(step));
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("missing-body"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("requires .body or .bodyFile")));
    }

    @Test
    void formBodyTypeRequiresFormParams() {
        ScenarioConfig sc = scenario("missing-form-params");
        StepConfig step = step("step-1", "POST", "/x");
        step.getRequest().setBodyType("form");
        sc.setSteps(List.of(step));
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("missing-form-params"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("requires .formParams")));
    }

    @Test
    void duplicateStepNamesFail() {
        ScenarioConfig sc = scenario("dup-names");
        sc.setSteps(List.of(
            step("get-user", "GET", "/users/1"),
            step("get-user", "GET", "/users/2")));
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("dup-names"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("duplicated")));
    }

    @Test
    void blankStepNameFails() {
        ScenarioConfig sc = scenario("blank-name");
        StepConfig step = step("  ", "GET", "/x");
        sc.setSteps(List.of(step));
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("blank-name"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains(".name must not be blank")));
    }

    @Test
    void extractMissingSessionKeyFails() {
        ScenarioConfig sc = scenario("bad-extract");
        StepConfig step = step("get-user", "GET", "/users/1");
        ExtractConfig ex = new ExtractConfig();
        ex.setPath("$.id");
        // sessionKey omitted
        step.setExtract(List.of(ex));
        sc.setSteps(List.of(step));
        var thrown = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("bad-extract"), List.of(sc)));
        assertTrue(thrown.getErrors().stream().anyMatch(e -> e.contains("sessionKey must not be blank")));
    }

    @Test
    void laterStepCanReferenceEarlierExtractedSessionKey() {
        ScenarioConfig sc = scenario("flow");
        StepConfig step1 = step("get-user", "GET", "/users/1");
        ExtractConfig ex = new ExtractConfig();
        ex.setType("jsonPath");
        ex.setPath("$.orderId");
        ex.setSessionKey("orderId");
        step1.setExtract(List.of(ex));

        StepConfig step2 = step("cancel-order", "POST", "/orders/${session:orderId}/cancel");
        sc.setSteps(List.of(step1, step2));

        assertDoesNotThrow(() -> validator.validate(env(), run("flow"), List.of(sc)));
    }

    @Test
    void referenceToUnknownSessionKeyFails() {
        ScenarioConfig sc = scenario("dangling-ref");
        StepConfig step1 = step("get-user", "GET", "/users/1");
        StepConfig step2 = step("cancel-order", "POST", "/orders/${session:orderId}/cancel");
        sc.setSteps(List.of(step1, step2));
        var thrown = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("dangling-ref"), List.of(sc)));
        assertTrue(thrown.getErrors().stream().anyMatch(e -> e.contains("orderId")));
    }

    @Test
    void multiStepBodyFileMustExist() {
        ScenarioConfig sc = scenario("missing-body-file");
        StepConfig step = step("post-data", "POST", "/x");
        step.getRequest().setBodyType("json");
        step.getRequest().setBodyFile("nonexistent/payload.json");
        sc.setSteps(List.of(step));
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("missing-body-file"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("nonexistent/payload.json")));
    }

    @Test
    void richChecksOnStepValidatesWithoutErrors() {
        ScenarioConfig sc = scenario("rich-checks");
        StepConfig step = step("get-user", "GET", "/users/1");
        ChecksConfig checks = new ChecksConfig();
        checks.setHttpStatus(200);
        checks.setBodyContains(List.of("\"id\""));
        checks.setResponseTimeMillis(500L);
        step.setChecks(checks);
        sc.setSteps(List.of(step));
        assertDoesNotThrow(() -> validator.validate(env(), run("rich-checks"), List.of(sc)));
    }

    @Test
    void singleRequestLegacyScenarioStillValidates() {
        // No `steps` block — top-level request is treated as a single synthesised step
        // and must not introduce any new errors for an otherwise-valid scenario.
        ScenarioConfig sc = scenario("legacy");
        // No request set — validator should still accept (matches existing behaviour).
        assertDoesNotThrow(() -> validator.validate(env(), run("legacy"), List.of(sc)));
    }

    @Test
    void steps_listOfNullStepsFlaggedClearly() {
        ScenarioConfig sc = scenario("null-step");
        List<StepConfig> steps = new ArrayList<>();
        steps.add(null);
        sc.setSteps(steps);
        var ex = assertThrows(ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env(), run("null-step"), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("must not be null")));
    }
}
