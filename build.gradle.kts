plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    application
    id("io.miret.etienne.sass") version "1.6.0"
    id("com.gradleup.shadow") version "8.3.10"
}

group = "com.adoptu"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    val ktorVersion = "3.4.1"
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

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // AWS S3
    implementation(platform("software.amazon.awssdk:bom:2.29.0"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:ses")
    implementation("software.amazon.awssdk:sesv2")

    // Email
    implementation("org.apache.commons:commons-email:1.6.0")

    // Argon2id password hashing
    implementation("com.password4j:password4j:1.8.4")

    // Testing
    val kotestVersion = "5.8.1"
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:localstack")
    testImplementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Playwright for E2E frontend tests
    testImplementation("com.microsoft.playwright:playwright:1.51.0")
}

application {
    mainClass.set("com.adoptu.ApplicationKt")
}

tasks.run<JavaExec> {
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.withType<JavaExec> {
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
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

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.adoptu.ApplicationKt"
        attributes["Implementation-Version"] = version
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}