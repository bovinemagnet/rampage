import java.time.Duration

plugins {
    java
    alias(libs.plugins.quarkus)
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
    implementation(platform("io.quarkus.platform:quarkus-bom:${libs.versions.quarkus.get()}"))

    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-qute")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-smallrye-graphql")
    implementation("io.quarkus:quarkus-qute")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-vertx")
    implementation("io.quarkus:quarkus-mutiny")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-h2")

    // Re-use the engine's YAML config loader / validator / model classes.
    // Root project's main jar deliberately does not bundle Gatling (compileOnly),
    // so this dependency stays free of Akka/Netty drift.
    implementation(project(":"))

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation(libs.assertj.core)
    testImplementation("com.microsoft.playwright:playwright:1.50.0")
}

// Default `test` task: skip the slower browser-driven suite. Run it with
// `./gradlew :console:e2eTest`.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

tasks.register<Test>("e2eTest") {
    description = "Runs the Playwright-driven end-to-end browser tests against the console."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("e2e")
    }
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    // Playwright downloads browser binaries on first run; allow plenty of time.
    timeout.set(Duration.ofMinutes(15))
    // Don't UP-TO-DATE skip — these tests want to re-verify the live UI each invocation.
    outputs.upToDateWhen { false }
}

tasks.withType<JavaCompile>().configureEach {
    // Qute @CheckedTemplate inspects parameter names via reflection.
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
