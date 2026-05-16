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

@Path("/")
public class DashboardResource {

    @CheckedTemplate(basePath = "Dashboard")
    static class Templates {
        public static native TemplateInstance dashboard(
                List<ConfigEntry> environments,
                List<ConfigEntry> runs);
    }

    @Inject
    ConfigBrowser configs;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        return Templates.dashboard(configs.environments(), configs.runs());
    }
}
