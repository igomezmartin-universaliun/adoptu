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
    val ktorVersion = "3.4.2"
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
    val exposedVersion = "1.2.0"
    val postgresVersion = "42.7.10"
    implementation("org.postgresql:postgresql:$postgresVersion")
    // Checker framework for nullness annotations (required by PostgreSQL JDBC driver)
    annotationProcessor("org.checkerframework:checker:3.47.0")
    implementation("org.checkerframework:checker-qual:3.47.0")
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
    val kotlinxDatetimeVersion = "0.6.1"
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.31")

    // AWS S3
    implementation(platform("software.amazon.awssdk:bom:2.42.23"))
    implementation("software.amazon.awssdk:s3") {
        exclude(group = "net.bytebuddy")
    }
    implementation("software.amazon.awssdk:ses") {
        exclude(group = "net.bytebuddy")
    }
    implementation("software.amazon.awssdk:sesv2") {
        exclude(group = "net.bytebuddy")
    }

    // Email
    implementation("org.apache.commons:commons-email:1.6.0")

    // Argon2id password hashing
    implementation("com.password4j:password4j:1.8.4")

    // Testing
    val byteBuddyVersion = "1.17.0"
    testImplementation("net.bytebuddy:byte-buddy:$byteBuddyVersion")
    testImplementation("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.10.2") {
        exclude(group = "net.bytebuddy")
    }
    val kotestVersion = "6.1.3"
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.14.9") {
        exclude(group = "net.bytebuddy")
        exclude(group = "net.bytebuddy", module = "byte-buddy")
        exclude(group = "net.bytebuddy", module = "byte-buddy-agent")
    }
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:localstack:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Playwright for E2E frontend tests
    val playwrightVersion = "1.58.0"
    testImplementation("com.microsoft.playwright:playwright:$playwrightVersion")
}

application {
    mainClass.set("com.adoptu.ApplicationKt")
}

tasks.run<JavaExec> {
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/java.math=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/java.math=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED"
    )
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    forkEvery = 1
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/java.math=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "jdk.unsupported/sun.misc=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "--add-opens", "java.base/sun.security.ssl=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
        "-Dkotest.framework.classpath.scanning.config.disable=true",
        "-Dkotest.framework.classpath.scanning.autoscan.disable=true"
    )
    systemProperty("jdk.module.illegalAccess", "permit")
    systemProperty("jdk.suppressUnsupportedWarningWarnings", "true")
    systemProperty("jdk.module.illegalAccess", "permit")

    doFirst {
        // Ensure test dependencies are started each run; non-blocking
        project.exec {
            environment["DOCKER_HOST"] = "unix:///run/docker.sock"
            commandLine("sh", "-c", "docker compose -f docker-compose.test.yml up -d")
        }
    }
}

tasks.compileSass {
    sourceDir = file("src/main/scss")
    outputDir = file("${layout.buildDirectory.get()}/sass")
    entryPoint("style.scss", "style.css")
    entryPoint("temporal-home.scss", "temporal-home.css")
    entryPoint("photographers.scss", "photographers.css")
    entryPoint("pet-food.scss", "pet-food.css")
    entryPoint("shelters.scss", "shelters.css")
    entryPoint("sterilization.scss", "sterilization.css")
    entryPoint("policy.scss", "policy.css")
    entryPoint("mypets.scss", "mypets.css")
    entryPoint("pet-detail.scss", "pet-detail.css")
}

tasks.compileSass {
    outputs.upToDateWhen { false }
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

// Docker Compose tasks for integration tests
tasks.register<Exec>("dockerUp") {
    group = "docker"
    description = "Start Docker containers for integration tests"
    workingDir = rootProject.rootDir
    environment["DOCKER_HOST"] = "unix:///run/docker.sock"
    commandLine("sh", "-c", 
        "docker compose -f docker-compose.test.yml up -d"
    )
}

tasks.register<Exec>("dockerDown") {
    group = "docker"
    description = "Stop Docker containers for integration tests"
    workingDir = rootProject.rootDir
    commandLine("docker", "compose", "-f", "docker-compose.test.yml", "down", "-v")
}

// Run integration tests with Docker: ./gradlew integrationTest
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Run integration tests with Docker"
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/java.math=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "jdk.unsupported/sun.misc=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "--add-opens", "java.base/sun.security.ssl=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
        "-Dkotest.framework.classpath.scanning.config.disable=true",
        "-Dkotest.framework.classpath.scanning.autoscan.disable=true"
    )
    systemProperty("jdk.module.illegalAccess", "permit")
    systemProperty("jdk.suppressUnsupportedWarningWarnings", "true")
    dependsOn("dockerUp")
    filter {
        // Only run IT tests (integration tests)
        includeTestsMatching(".*IT")
    }
}