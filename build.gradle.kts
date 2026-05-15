plugins {
    java
    alias(libs.plugins.gatling)
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencies {
    // Jackson YAML
    implementation(libs.bundles.jackson)

    // SLF4J + Logback
    implementation(libs.bundles.logging)

    // OpenAPI parser (used by the importOpenApi scaffolding task)
    implementation(libs.swagger.parser)

    // H2 for SQL feeder (test DB)
    implementation(libs.h2)

    // HikariCP connection pool
    implementation(libs.hikaricp)

    // Gatling on main compile classpath (factories use Gatling APIs)
    compileOnly(libs.gatling.charts.highcharts)
    // Gatling for the gatling source set
    gatling(libs.gatling.charts.highcharts)

    // Runtime dependencies for the gatling source set: RampageSimulation transitively
    // pulls in Jackson YAML (via ConfigLoader), logging, JDBC, and HikariCP. The
    // gatling configuration does not inherit from main's `implementation`, so these
    // must be declared explicitly to be on the gatlingRun runtime classpath.
    gatlingImplementation(libs.bundles.jackson)
    gatlingImplementation(libs.bundles.logging)
    gatlingImplementation(libs.h2)
    gatlingImplementation(libs.hikaricp)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.h2)
    testImplementation(libs.gatling.charts.highcharts)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

// Forward selected -D system properties from the Gradle invocation through to
// the forked Gatling JVM. The console subproject relies on this to propagate
// loadtest.* (env/run paths) and rampage.console.* (Carbon writer overrides).
gatling {
    val forwarded = System.getProperties()
        .stringPropertyNames()
        .filter { name ->
            name.startsWith("loadtest.") ||
                name.startsWith("rampage.") ||
                name.startsWith("gatling.data.")
        }
        .associateWith { System.getProperty(it) as Any }
    systemProperties = forwarded.toMutableMap()
}

tasks.register("validateLoadTest", JavaExec::class) {
    group = "verification"
    description = "Validates load test YAML configuration and feeder availability."
    classpath = sourceSets["gatling"].runtimeClasspath + sourceSets["main"].runtimeClasspath
    mainClass.set("io.rampage.config.ConfigValidatorMain")
    @Suppress("UNCHECKED_CAST")
    systemProperties(System.getProperties()
        .filter { (key, _) -> key.toString().startsWith("loadtest.") }
        .mapKeys { it.key.toString() } as Map<String, Any>)
}

tasks.register("importHar", JavaExec::class) {
    group = "scaffolding"
    description = "Generates a Rampage scenario YAML from a HAR file. " +
        "Usage: gradle importHar -Drampage.har.file=path/to/x.har -Drampage.scenario.id=foo " +
        "[-Drampage.har.host=api.example.com] [-Drampage.har.methods=GET,POST]"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.rampage.scaffold.HarImporter")
    @Suppress("UNCHECKED_CAST")
    systemProperties(System.getProperties()
        .filter { (key, _) -> key.toString().startsWith("rampage.") }
        .mapKeys { it.key.toString() } as Map<String, Any>)
}

tasks.register("importOpenApi", JavaExec::class) {
    group = "scaffolding"
    description = "Generates Rampage scenario YAMLs from an OpenAPI 3.x spec. " +
        "Usage: gradle importOpenApi -Drampage.openapi.file=path/to/openapi.yaml " +
        "[-Drampage.scenario.prefix=foo-]"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.rampage.scaffold.OpenApiImporter")
    @Suppress("UNCHECKED_CAST")
    systemProperties(System.getProperties()
        .filter { (key, _) -> key.toString().startsWith("rampage.") }
        .mapKeys { it.key.toString() } as Map<String, Any>)
}

tasks.register("summariseRun", JavaExec::class) {
    group = "verification"
    description = "Parses the latest Gatling report and writes run-summary.json + run-summary.md."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.rampage.reporting.RunSummaryMain")
    @Suppress("UNCHECKED_CAST")
    systemProperties(System.getProperties()
        .filter { (key, _) -> key.toString().startsWith("rampage.") }
        .mapKeys { it.key.toString() } as Map<String, Any>)
}

tasks.register("generateSchemaDocs", JavaExec::class) {
    group = "documentation"
    description = "Reflects over config model classes and emits build/schema/reference.adoc + schemas.json."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.rampage.docs.SchemaDocsGenerator")
    args(layout.buildDirectory.dir("schema").get().asFile.toString())
}

tasks.register("newScenario") {
    group = "scaffolding"
    description = "Bootstraps a new scenario from templates. Usage: gradle newScenario -PscenarioId=foo [-PallowOverwrite=true]"

    doLast {
        val scenarioId = (project.findProperty("scenarioId") as? String)?.trim()
            ?: throw GradleException("scenarioId property is required: -PscenarioId=foo-bar")
        require(scenarioId.matches(Regex("^[a-z0-9][a-z0-9-]*$"))) {
            "scenarioId must be lowercase kebab-case: $scenarioId"
        }
        val allowOverwrite = (project.findProperty("allowOverwrite") as? String)?.equals("true", ignoreCase = true) ?: false

        val targets = listOf(
            Triple("config/templates/scenario.yaml.tpl",    "config/scenarios/$scenarioId.yaml",     "scenario YAML"),
            Triple("config/templates/scenario.graphql.tpl", "config/graphql/$scenarioId.graphql",    "GraphQL query"),
            Triple("config/templates/scenario.sql.tpl",     "config/queries/$scenarioId-data.sql",   "feeder SQL")
        )

        for ((tplPath, outPath, label) in targets) {
            val tpl = file(tplPath)
            if (!tpl.exists()) {
                throw GradleException("Missing template: $tplPath")
            }
            val out = file(outPath)
            if (out.exists() && !allowOverwrite) {
                throw GradleException("Refusing to overwrite $outPath (pass -PallowOverwrite=true to force)")
            }
            out.parentFile.mkdirs()
            out.writeText(tpl.readText().replace("__SCENARIO_ID__", scenarioId))
            logger.lifecycle("Created $label: $outPath")
        }

        logger.lifecycle("Scenario '$scenarioId' scaffolded. Next steps:")
        logger.lifecycle("  1. Fill in TODOs in config/scenarios/$scenarioId.yaml")
        logger.lifecycle("  2. Replace the stub GraphQL in config/graphql/$scenarioId.graphql")
        logger.lifecycle("  3. Update the feeder SQL in config/queries/$scenarioId-data.sql")
        logger.lifecycle("  4. Add the scenario to your run.yaml under run.scenarios")
    }
}
