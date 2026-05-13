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

val jacksonVersion = "2.17.2"
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

    // Gatling on main compile classpath (factories use Gatling APIs)
    compileOnly("io.gatling.highcharts:gatling-charts-highcharts:$gatlingVersion")
    // Gatling for the gatling source set
    gatling("io.gatling.highcharts:gatling-charts-highcharts:$gatlingVersion")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:$gatlingVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}
