plugins {
    java
    id("io.gatling.gradle") version "3.13.5"
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

val jacksonVersion = "2.18.3"
val junitVersion = "5.11.4"
val gatlingVersion = "3.13.5"

dependencies {
    // Jackson YAML
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")

    // SLF4J + Logback
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // H2 for SQL feeder (test DB)
    implementation("com.h2database:h2:2.2.224")

    // HikariCP connection pool
    implementation("com.zaxxer:HikariCP:6.3.0")

    // Gatling on main compile classpath (factories use Gatling APIs)
    compileOnly("io.gatling.highcharts:gatling-charts-highcharts:$gatlingVersion")
    // Gatling for the gatling source set
    gatling("io.gatling.highcharts:gatling-charts-highcharts:$gatlingVersion")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:$gatlingVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
