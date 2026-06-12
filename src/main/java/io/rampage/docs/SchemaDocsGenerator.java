package io.rampage.docs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Reflection-based schema docs generator. Walks the {@code io.rampage.config.model}
 * classes and emits an AsciiDoc reference page plus a coarse JSON Schema summary.
 */
public final class SchemaDocsGenerator {

    private static final ObjectMapper JSON = new ObjectMapper().enable(
        SerializationFeature.INDENT_OUTPUT
    );

    private final Path outputDir;

    /**
     * Constructs a {@code SchemaDocsGenerator} that writes its output to the specified directory.
     *
     * @param outputDir the directory into which {@code reference.adoc} and {@code schemas.json}
     *                  are written; the directory is created by {@link #generate()} if absent
     */
    public SchemaDocsGenerator(Path outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * CLI entry point. Accepts an optional first argument specifying the output directory
     * (defaults to {@code build/schema}).
     *
     * @param args optional; {@code args[0]} overrides the output directory path
     * @throws Exception if schema generation fails
     */
    public static void main(String[] args) throws Exception {
        Path out = args.length > 0 ? Path.of(args[0]) : Path.of("build/schema");
        new SchemaDocsGenerator(out).generate();
    }

    /**
     * Generates {@code reference.adoc} and {@code schemas.json} in the output directory
     * by reflecting over {@code EnvironmentConfig}, {@code RunConfig}, and
     * {@code ScenarioConfig} and all transitively reachable {@code io.rampage.config.model}
     * classes.
     *
     * @throws Exception if a configuration model class cannot be instantiated, or if the
     *                   output files cannot be written
     */
    public void generate() throws Exception {
        Files.createDirectories(outputDir);

        List<Class<?>> roots = List.of(
            io.rampage.config.model.EnvironmentConfig.class,
            io.rampage.config.model.RunConfig.class,
            io.rampage.config.model.ScenarioConfig.class
        );

        StringBuilder adoc = new StringBuilder();
        adoc.append("= Rampage Configuration Reference\n");
        adoc.append(":author: Paul Snow\n");
        adoc.append(":version: 0.1.0\n\n");
        adoc.append(
            "Auto-generated from `io.rampage.config.model` POJOs by `SchemaDocsGenerator`.\n\n"
        );

        Map<String, Object> schemas = new LinkedHashMap<>();

        for (Class<?> root : roots) {
            TreeSet<Class<?>> visited = new TreeSet<>((a, b) ->
                a.getName().compareTo(b.getName())
            );
            collectClasses(root, visited);

            adoc.append("== ").append(root.getSimpleName()).append("\n\n");
            adoc.append("YAML root for `")
                .append(yamlFile(root))
                .append("`.\n\n");
            for (Class<?> cls : visited) {
                appendAdocSection(adoc, cls);
            }

            schemas.put(
                root.getSimpleName(),
                buildJsonSchema(root, new java.util.HashSet<>())
            );
        }

        Files.writeString(outputDir.resolve("reference.adoc"), adoc.toString());
        Files.writeString(
            outputDir.resolve("schemas.json"),
            JSON.writeValueAsString(schemas)
        );
    }

    private static String yamlFile(Class<?> root) {
        return switch (root.getSimpleName()) {
            case "EnvironmentConfig" -> "environment.yaml";
            case "RunConfig" -> "run.yaml";
            case "ScenarioConfig" -> "scenarios/<id>.yaml";
            default -> "<unknown>";
        };
    }

    private static void appendAdocSection(StringBuilder adoc, Class<?> cls) {
        adoc.append("=== ").append(cls.getSimpleName()).append("\n\n");
        adoc.append("[cols=\"2,2,1,4\",options=\"header\"]\n");
        adoc.append("|===\n");
        adoc.append("|Field |Type |Required |Notes\n\n");
        Object defaults = newInstanceQuiet(cls);
        for (Field f : cls.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            JsonProperty jp = f.getAnnotation(JsonProperty.class);
            String name =
                jp != null && !jp.value().isEmpty() ? jp.value() : f.getName();
            String type = describeType(f);
            Object def = readField(f, defaults);
            String required = jp != null && jp.required() ? "yes" : "no";
            String notes = def != null ? "default: `" + def + "`" : "";
            adoc.append("|`").append(name).append("`\n");
            adoc.append("|").append(type).append("\n");
            adoc.append("|").append(required).append("\n");
            adoc.append("|").append(notes).append("\n\n");
        }
        adoc.append("|===\n\n");
    }

    private static String describeType(Field f) {
        Class<?> raw = f.getType();
        if (raw == String.class) return "string";
        if (raw == int.class || raw == Integer.class) return "int";
        if (raw == long.class || raw == Long.class) return "long";
        if (raw == double.class || raw == Double.class) return "double";
        if (raw == boolean.class || raw == Boolean.class) return "boolean";
        if (Collection.class.isAssignableFrom(raw)) {
            return "list of " + describeGeneric(f);
        }
        if (Map.class.isAssignableFrom(raw)) {
            return "map of " + describeGeneric(f);
        }
        return "<<" + raw.getSimpleName() + ">>";
    }

    private static String describeGeneric(Field f) {
        if (f.getGenericType() instanceof ParameterizedType pt) {
            java.lang.reflect.Type[] args = pt.getActualTypeArguments();
            if (args.length == 1) {
                return classOfType(args[0]).getSimpleName();
            }
            if (args.length == 2) {
                return (
                    classOfType(args[0]).getSimpleName() +
                    " → " +
                    classOfType(args[1]).getSimpleName()
                );
            }
        }
        return "unknown";
    }

    private static Class<?> classOfType(java.lang.reflect.Type t) {
        if (t instanceof Class<?> c) return c;
        if (t instanceof ParameterizedType pt) return (Class<
            ?
        >) pt.getRawType();
        return Object.class;
    }

    private static void collectClasses(Class<?> cls, TreeSet<Class<?>> out) {
        if (!cls.getName().startsWith("io.rampage.config.model.")) return;
        if (!out.add(cls)) return;
        for (Field f : cls.getDeclaredFields()) {
            Class<?> ft = f.getType();
            if (ft.getName().startsWith("io.rampage.config.model.")) {
                collectClasses(ft, out);
            } else if (
                Collection.class.isAssignableFrom(ft) ||
                Map.class.isAssignableFrom(ft)
            ) {
                if (f.getGenericType() instanceof ParameterizedType pt) {
                    for (java.lang.reflect.Type arg : pt.getActualTypeArguments()) {
                        Class<?> argClass = classOfType(arg);
                        if (
                            argClass
                                .getName()
                                .startsWith("io.rampage.config.model.")
                        ) {
                            collectClasses(argClass, out);
                        }
                    }
                }
            }
        }
    }

    private static Object newInstanceQuiet(Class<?> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private static Object readField(Field f, Object instance) {
        if (instance == null) return null;
        try {
            f.setAccessible(true);
            Object v = f.get(instance);
            if (v == null) return null;
            if (v instanceof Collection<?> c && c.isEmpty()) return null;
            if (v instanceof Map<?, ?> m && m.isEmpty()) return null;
            if (v.getClass().getName().startsWith("io.rampage.")) return null;
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Object> buildJsonSchema(
        Class<?> cls,
        java.util.Set<Class<?>> visited
    ) {
        if (!visited.add(cls)) return Map.of("$ref", cls.getSimpleName());
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("title", cls.getSimpleName());
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        Object defaults = newInstanceQuiet(cls);
        for (Field f : cls.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            JsonProperty jp = f.getAnnotation(JsonProperty.class);
            String name =
                jp != null && !jp.value().isEmpty() ? jp.value() : f.getName();
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("type", describeType(f));
            Object def = readField(f, defaults);
            if (def != null) prop.put("default", def);
            properties.put(name, prop);
            if (jp != null && jp.required()) required.add(name);
        }
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        return schema;
    }
}
