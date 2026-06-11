import java.time.Duration

plugins {
    java
    `maven-publish`
    alias(libs.plugins.gatling)
    alias(libs.plugins.antora)
}

group = "io.rampage"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // Jackson YAML
    implementation(libs.bundles.jackson)

    // SLF4J + Logback
    implementation(libs.bundles.logging)

    // OpenAPI parser (used by the importOpenApi scaffolding task)
    implementation(libs.swagger.parser)

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
    gatlingImplementation(libs.postgresql)
    gatlingImplementation(libs.hikaricp)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.h2)
    testImplementation(libs.gatling.charts.highcharts)
    testImplementation(libs.wiremock.standalone)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Ship RampageSimulation in the library JAR so consumers can run or subclass it.
// Only the compiled classes are included — the gatling source set's resources
// (gatling.conf, logback.xml, default YAML) are local run configuration and must
// not leak into the published artefact.
tasks.jar {
    from(sourceSets["gatling"].output.classesDirs)
}

tasks.named<Jar>("sourcesJar") {
    from(sourceSets["gatling"].allJava)
}

tasks.test {
    // Keep the default suite fast: the Gatling-driven integration test is tagged "integration"
    // and run separately via the integrationTest task.
    useJUnitPlatform {
        excludeTags("integration")
    }
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

// Runs the RampageSimulation integration test (F-038) against a WireMock stub by driving Gatling
// in-process. RampageSimulation lives in the `gatling` source set, which the default `test` source
// set cannot see, so its output and runtime classpath are added explicitly here.
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs the RampageSimulation integration test against WireMock (in-process Gatling)."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath +
        sourceSets["gatling"].output +
        sourceSets["gatling"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    shouldRunAfter(tasks.test)
    timeout.set(Duration.ofMinutes(2))
    // Always re-run: a green simulation wiring check is cheap and worth re-verifying.
    outputs.upToDateWhen { false }
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

tasks.register("promoteRunMetadata", JavaExec::class) {
    description = "Moves run-metadata.json and config-snapshot.json from the reports root into the simulation directory just created by gatlingRun."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.rampage.reporting.RunMetadataPromoter")
    args(layout.buildDirectory.dir("reports/gatling").get().asFile.absolutePath)
}

tasks.named("gatlingRun") {
    finalizedBy("promoteRunMetadata")
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

// Builds the documentation site from src/docs into build/site via `gradle antora`.
// The plugin provisions its own Node.js runtime, so no local Node install is needed.
antora {
    version = libs.versions.antora.asProvider().get()
    playbook = file("antora-playbook.yml")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Rampage"
                description = "Configuration-driven Gatling load testing framework: " +
                    "scenarios are defined in YAML, GraphQL, and SQL rather than Java."
                url = "https://github.com/bovinemagnet/rampage"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "Paul Snow"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/bovinemagnet/rampage.git"
                    developerConnection = "scm:git:git@github.com:bovinemagnet/rampage.git"
                    url = "https://github.com/bovinemagnet/rampage"
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bovinemagnet/rampage")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
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
