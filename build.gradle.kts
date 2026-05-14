plugins {
    java
    alias(libs.plugins.gatling)
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencies {
    // Jackson YAML
    implementation(libs.bundles.jackson)

    // SLF4J + Logback
    implementation(libs.bundles.logging)

    // H2 for SQL feeder (test DB)
    implementation(libs.h2)

    // HikariCP connection pool
    implementation(libs.hikaricp)

    // Gatling on main compile classpath (factories use Gatling APIs)
    compileOnly(libs.gatling.charts.highcharts)
    // Gatling for the gatling source set
    gatling(libs.gatling.charts.highcharts)

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
