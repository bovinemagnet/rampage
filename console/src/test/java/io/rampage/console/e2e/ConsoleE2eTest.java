package io.rampage.console.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunOrchestrator;
import io.rampage.console.orchestrator.ProcessLauncher;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser-driven smoke tests for the console UI. Excluded from the default
 * {@code :console:test} task and run via {@code :console:e2eTest}; tagged
 * {@code "e2e"}.
 *
 * The orchestrator's process launcher is swapped for a fake that produces an
 * instantly-completing stub process so the test does not depend on Gradle,
 * Java toolchains, or a working Gatling installation on the CI host. The full
 * live-Gatling path is exercised manually.
 */
@QuarkusTest
@Tag("e2e")
class ConsoleE2eTest {

    private static Playwright playwright;
    private static Browser browser;

    @TestHTTPResource("/")
    URL dashboardUrl;

    @Inject
    RunOrchestrator orchestrator;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void shutdown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void installFakeLauncher() {
        orchestrator.setProcessLauncher(fastSuccessLauncher());
    }

    @Test
    void dashboardRendersHeaderAndLauncher() {
        try (Page page = browser.newPage()) {
            page.navigate(dashboardUrl.toString());
            assertThat(page.title()).isEqualTo("Rampage Console");
            assertThat(page.locator("h1").textContent()).isEqualTo("Rampage Console");
            assertThat(page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Run")).count()).isEqualTo(1);
            assertThat(page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Verify console")).count()).isEqualTo(1);
        }
    }

    @Test
    void verifyButtonEnqueuesAndStatusFeedShowsCompleted() {
        try (Page page = browser.newPage()) {
            page.navigate(dashboardUrl.toString());
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Verify console")).click();

            page.waitForSelector("text=Verify queued");
            // SSE status feed should eventually render a COMPLETED row.
            page.waitForFunction(
                    "document.querySelector('.status-COMPLETED, [class*=COMPLETED]') !== null",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(15_000));

            String content = page.content();
            assertThat(content).contains("COMPLETED");
        }
    }

    @Test
    void configsPageListsCategories() {
        try (Page page = browser.newPage()) {
            page.navigate(dashboardUrl.toString().replaceFirst("/$", "") + "/configs");
            List<String> headings = page.locator("h2").allInnerTexts();
            assertThat(headings).contains("Environments", "Runs", "Scenarios");
        }
    }

    @Test
    void historyPageRendersWithoutError() {
        try (Page page = browser.newPage()) {
            page.navigate(dashboardUrl.toString().replaceFirst("/$", "") + "/history");
            assertThat(page.locator("h1").textContent()).isEqualTo("Run history");
        }
    }

    /**
     * Fake launcher that pretends to spawn a Gatling process — actually returns
     * a sh subprocess that prints one line and exits 0 within a few ms. Avoids
     * any dependency on Gradle, Java toolchains, or Gatling itself in CI.
     */
    private static ProcessLauncher fastSuccessLauncher() {
        return (cmd, dir) -> new ProcessBuilder("sh", "-c", "echo 'fake-verify-stdout'; exit 0")
                .redirectErrorStream(true)
                .start();
    }

    private interface Launcher {
        Process launch(java.util.List<String> cmd, java.nio.file.Path dir) throws IOException;
    }
}
