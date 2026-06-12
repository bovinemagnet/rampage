package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.rampage.console.config.ConfigBrowser;
import io.rampage.console.config.ConfigEntry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * JAX-RS resource serving the main dashboard page at the application root ({@code /}).
 * Provides an overview of available environment and run configuration files so the
 * operator can quickly select and launch a load test.
 */
@Path("/")
public class DashboardResource {

    /**
     * Creates a new {@code DashboardResource} instance. CDI-managed; no arguments required.
     */
    public DashboardResource() {}

    @CheckedTemplate(basePath = "Dashboard")
    static class Templates {
        public static native TemplateInstance dashboard(
                List<ConfigEntry> environments,
                List<ConfigEntry> runs);
    }

    @Inject
    ConfigBrowser configs;

    /**
     * Renders the main dashboard, listing all discovered environment and run config files.
     *
     * @return the rendered {@code Dashboard/dashboard} template
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        return Templates.dashboard(configs.environments(), configs.runs());
    }
}
