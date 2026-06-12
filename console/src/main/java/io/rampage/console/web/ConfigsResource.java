package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.rampage.console.config.ConfigBrowser;
import io.rampage.console.config.ConfigEditor;
import io.rampage.console.config.ConfigEntry;
import io.rampage.console.config.ValidationResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

/**
 * JAX-RS resource serving the YAML configuration browser and editor under {@code /configs}.
 * Delegates file discovery to {@code ConfigBrowser} and read/write operations to
 * {@code ConfigEditor}.
 */
@Path("/configs")
public class ConfigsResource {

    /**
     * Creates a new {@code ConfigsResource} instance. CDI-managed; no arguments required.
     */
    public ConfigsResource() {}

    @CheckedTemplate(basePath = "Configs")
    static class Templates {
        public static native TemplateInstance index(
                List<ConfigEntry> environments,
                List<ConfigEntry> runs,
                List<ConfigEntry> scenarios);

        public static native TemplateInstance edit(String relativePath, String body);

        public static native TemplateInstance saveResult(ValidationResult result);
    }

    @Inject
    ConfigBrowser browser;

    @Inject
    ConfigEditor editor;

    /**
     * Renders the configuration browser listing all known environment, run, and scenario files.
     *
     * @return the rendered {@code Configs/index} template
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.index(browser.environments(), browser.runs(), browser.scenarios());
    }

    /**
     * Renders the editor form pre-populated with the content of the file identified by
     * {@code path}. Returns HTTP 400 when {@code path} is absent or blank, HTTP 404 when
     * the file cannot be read, and HTTP 400 when the path is rejected by the security checks
     * in {@code ConfigEditor}.
     *
     * @param path relative path of the config file to edit; must not be blank
     * @return the rendered {@code Configs/edit} template, or an error response
     */
    @GET
    @Path("/edit")
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@QueryParam("path") String path) {
        if (path == null || path.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("path query parameter is required")
                    .build();
        }
        try {
            String body = editor.read(path);
            return Response.ok(Templates.edit(path, body)).build();
        } catch (IOException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("File not readable: " + e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Validates and writes the submitted YAML body to the file identified by {@code path},
     * then renders the save-result fragment indicating success or validation errors.
     *
     * @param path relative path of the config file to overwrite
     * @param body the new YAML content; treated as an empty string when {@code null}
     * @return the rendered {@code Configs/saveResult} template
     */
    @POST
    @Path("/save")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance save(@FormParam("path") String path,
                                 @FormParam("body") String body) {
        ValidationResult result = editor.validateAndSave(path, body == null ? "" : body);
        return Templates.saveResult(result);
    }
}
