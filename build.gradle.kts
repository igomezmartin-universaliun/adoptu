plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
    id("io.miret.etienne.sass") version "1.6.0"
}

group = "com.adoptu"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
        val ktorVersion = "3.4.0"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")

    // WebAuthn/FIDO2 - webauthn4j for server-side verification
    implementation("com.webauthn4j:webauthn4j-core:0.31.0.RELEASE")

    // Database
    val exposedVersion = "1.1.1"
    val postgresVersion = "42.7.9"
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-json:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-migration-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-migration-jdbc:${exposedVersion}")

    // Koin
    val koinVersion = "4.1.1"
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.3")

    // AWS S3
    implementation("software.amazon.awssdk:s3:2.29.0")

    // Email
    implementation("com.sun.mail:javax.mail:1.6.2")

    // Testing
    val kotestVersion = "5.8.1"
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") // Corrected from ktor-server-mocks-host
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
}

application {
    mainClass.set("com.adoptu.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.compileSass {
    sourceDir = file("src/main/scss")
    outputDir = file("${layout.buildDirectory.get()}/sass")
    entryPoint("style.scss", "style.css")
}

tasks.processResources {
    dependsOn(tasks.compileSass)
    from(tasks.compileSass.get().outputDir) {
        into("static/css")
    }
}