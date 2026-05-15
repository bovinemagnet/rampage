package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.rampage.console.history.RunHistoryEntry;
import io.rampage.console.history.RunHistoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/history")
public class HistoryResource {

    @CheckedTemplate(basePath = "History")
    static class Templates {
        public static native TemplateInstance index(List<RunHistoryEntry> entries);
    }

    @Inject
    RunHistoryService history;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.index(history.listRecent(50));
    }
}
