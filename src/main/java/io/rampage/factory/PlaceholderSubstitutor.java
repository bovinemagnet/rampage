package io.rampage.factory;

import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.FeederConfig;
import io.rampage.config.model.MetadataConfig;
import io.rampage.config.model.RequestConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.StepConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands {@code ${run:<key>}}, {@code ${env:<NAME>}}, {@code ${sys:<NAME>}}, and
 * {@code ${secret:<path>}} placeholders inside YAML string fields. Escaped placeholders
 * ({@code \${...}}) pass through literally as {@code ${...}}.
 */
public final class PlaceholderSubstitutor {

    private PlaceholderSubstitutor() {}

    private static final Pattern ESCAPED = Pattern.compile("\\\\\\$\\{([^}]+)\\}");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z]+):([^}]+)\\}");
    private static final String ESC_OPEN = "RAMPAGE_ESC_OPEN";
    private static final String ESC_CLOSE = "RAMPAGE_ESC_CLOSE";

    public static String expand(String value, EnvironmentConfig env, RunConfig run, SecretResolver secretResolver) {
        List<String> errors = new ArrayList<>();
        String result = expand(value, env, run, secretResolver, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Unresolved placeholders: " + errors);
        }
        return result;
    }

    public static String expand(String value, EnvironmentConfig env, RunConfig run,
                                SecretResolver secretResolver, List<String> errors) {
        if (value == null) return null;

        StringBuilder pre = new StringBuilder();
        Matcher escMatcher = ESCAPED.matcher(value);
        while (escMatcher.find()) {
            escMatcher.appendReplacement(pre,
                Matcher.quoteReplacement(ESC_OPEN + escMatcher.group(1) + ESC_CLOSE));
        }
        escMatcher.appendTail(pre);

        StringBuilder out = new StringBuilder();
        Matcher m = PLACEHOLDER.matcher(pre.toString());
        while (m.find()) {
            String kind = m.group(1).toLowerCase();
            String key = m.group(2);
            String replacement = resolve(kind, key, env, run, secretResolver, errors);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);

        return out.toString()
            .replace(ESC_OPEN, "${")
            .replace(ESC_CLOSE, "}");
    }

    public static Map<String, String> expandAll(Map<String, String> input, EnvironmentConfig env,
                                                RunConfig run, SecretResolver secretResolver,
                                                List<String> errors) {
        if (input == null) return null;
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : input.entrySet()) {
            out.put(e.getKey(), expand(e.getValue(), env, run, secretResolver, errors));
        }
        return out;
    }

    public static List<String> expandInPlace(EnvironmentConfig env, RunConfig run,
                                             List<ScenarioConfig> scenarios, SecretResolver secretResolver) {
        List<String> errors = new ArrayList<>();
        if (env != null) {
            expandEnvironment(env, run, secretResolver, errors);
        }
        if (run != null) {
            expandRun(run, env, secretResolver, errors);
        }
        if (scenarios != null) {
            for (ScenarioConfig sc : scenarios) {
                if (sc == null) continue;
                expandScenario(sc, env, run, secretResolver, errors);
            }
        }
        return errors;
    }

    private static void expandEnvironment(EnvironmentConfig env, RunConfig run,
                                          SecretResolver secretResolver, List<String> errors) {
        env.setBaseUrls(expandAll(env.getBaseUrls(), env, run, secretResolver, errors));
        if (env.getSecurity() != null) {
            env.getSecurity().setHeaders(
                expandAll(env.getSecurity().getHeaders(), env, run, secretResolver, errors));
        }
        if (env.getDatabases() != null) {
            for (DatabaseConfig db : env.getDatabases().values()) {
                if (db == null) continue;
                db.setJdbcUrl(expand(db.getJdbcUrl(), env, run, secretResolver, errors));
                db.setDriverClassName(expand(db.getDriverClassName(), env, run, secretResolver, errors));
            }
        }
    }

    private static void expandRun(RunConfig run, EnvironmentConfig env,
                                  SecretResolver secretResolver, List<String> errors) {
        run.setHeaders(expandAll(run.getHeaders(), env, run, secretResolver, errors));
        MetadataConfig md = run.getMetadata();
        if (md != null) {
            md.setOwner(expand(md.getOwner(), env, run, secretResolver, errors));
            md.setApplication(expand(md.getApplication(), env, run, secretResolver, errors));
            md.setService(expand(md.getService(), env, run, secretResolver, errors));
            md.setChangeReference(expand(md.getChangeReference(), env, run, secretResolver, errors));
            md.setDescription(expand(md.getDescription(), env, run, secretResolver, errors));
        }
    }

    private static void expandScenario(ScenarioConfig sc, EnvironmentConfig env, RunConfig run,
                                       SecretResolver secretResolver, List<String> errors) {
        sc.setHeaders(expandAll(sc.getHeaders(), env, run, secretResolver, errors));
        sc.setDescription(expand(sc.getDescription(), env, run, secretResolver, errors));
        expandRequest(sc.getRequest(), env, run, secretResolver, errors);
        expandFeeder(sc.getFeeder(), env, run, secretResolver, errors);
        if (sc.getSteps() != null) {
            for (StepConfig step : sc.getSteps()) {
                if (step == null) continue;
                expandRequest(step.getRequest(), env, run, secretResolver, errors);
            }
        }
    }

    private static void expandRequest(RequestConfig req, EnvironmentConfig env, RunConfig run,
                                       SecretResolver secretResolver, List<String> errors) {
        if (req == null) return;
        req.setPath(expand(req.getPath(), env, run, secretResolver, errors));
        req.setBody(expand(req.getBody(), env, run, secretResolver, errors));
        req.setBodyFile(expand(req.getBodyFile(), env, run, secretResolver, errors));
        req.setBodyTemplate(expand(req.getBodyTemplate(), env, run, secretResolver, errors));
        req.setGraphqlQueryFile(expand(req.getGraphqlQueryFile(), env, run, secretResolver, errors));
        req.setQueryParams(expandAll(req.getQueryParams(), env, run, secretResolver, errors));
        req.setFormParams(expandAll(req.getFormParams(), env, run, secretResolver, errors));
    }

    private static void expandFeeder(FeederConfig feeder, EnvironmentConfig env, RunConfig run,
                                      SecretResolver secretResolver, List<String> errors) {
        if (feeder == null) return;
        feeder.setSqlFile(expand(feeder.getSqlFile(), env, run, secretResolver, errors));
    }

    private static String resolve(String kind, String key, EnvironmentConfig env, RunConfig run,
                                  SecretResolver secretResolver, List<String> errors) {
        switch (kind) {
            case "run":
                if (run == null) {
                    errors.add("${run:" + key + "} but no run config available");
                    return "";
                }
                return resolveRunField(key, run, errors);
            case "env":
                String envVal = System.getenv(key);
                if (envVal == null) {
                    errors.add("${env:" + key + "} environment variable not set");
                    return "";
                }
                return envVal;
            case "sys":
                String sysVal = System.getProperty(key);
                if (sysVal == null) {
                    errors.add("${sys:" + key + "} system property not set");
                    return "";
                }
                return sysVal;
            case "secret":
                if (secretResolver == null) {
                    errors.add("${secret:" + key + "} but no secret resolver available");
                    return "";
                }
                return secretResolver.resolve("SM:" + key);
            default:
                errors.add("Unknown placeholder kind '" + kind + "' in ${" + kind + ":" + key + "}");
                return "";
        }
    }

    private static String resolveRunField(String key, RunConfig run, List<String> errors) {
        return switch (key) {
            case "id" -> run.getId() != null ? run.getId() : "";
            case "name" -> run.getName() != null ? run.getName() : "";
            case "environment" -> run.getEnvironment() != null ? run.getEnvironment() : "";
            case "version" -> String.valueOf(run.getVersion());
            default -> {
                errors.add("${run:" + key + "} unknown field; supported: id, name, environment, version");
                yield "";
            }
        };
    }
}
