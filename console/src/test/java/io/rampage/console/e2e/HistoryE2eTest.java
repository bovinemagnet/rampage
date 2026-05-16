package io.rampage.console.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.ScenarioStat;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser-driven coverage of the results-store UI. Tagged {@code e2e}; runs via
 * {@code :console:e2eTest}. Seeds the database directly so it does not depend on
 * a working Gatling installation.
 */
@QuarkusTest
@Tag("e2e")
class HistoryE2eTest {

    private static Playwright playwright;
    private static Browser browser;

    @TestHTTPResource("/history")
    URL historyUrl;

    @Inject
    StoredRunRepository repository;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void shutdown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    void seed(String id, String name) {
        QuarkusTransaction.requiringNew().run(() -> {
            if (repository.findById(id) != null) {
                return;
            }
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = name;
            run.environmentId = "local";
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.runConfigKey = "local::smoke";
            run.startedAt = Instant.now();
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = 142.0;
            s.errorPercent = 0.0;
            s.requestsPerSecond = 50.0;
            s.requestCount = 300L;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void historyListsSeededRunAndDetailPageOpens() {
        seed("e2e-history-run", "E2E History Run");
        try (Page page = browser.newPage()) {
            page.navigate(historyUrl.toString());
            assertThat(page.locator("h1").textContent()).isEqualTo("Run history");
            assertThat(page.content()).contains("E2E History Run");

            page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("E2E History Run")).click();
            page.waitForSelector("text=Scenario metrics");
            assertThat(page.content()).contains("Quick GET");
        }
    }

    @Test
    void trendsPageRendersChartForSeededConfig() {
        seed("e2e-trend-run", "E2E Trend Run");
        try (Page page = browser.newPage()) {
            String base = historyUrl.toString().replaceFirst("/history$", "");
            page.navigate(base + "/history/trends?runConfigKey=local::smoke");
            page.waitForSelector("#trend-chart canvas",
                    new Page.WaitForSelectorOptions().setTimeout(10_000));
            assertThat(page.locator("#trend-chart canvas").count()).isGreaterThan(0);
        }
    }
}
