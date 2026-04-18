plugins {
    kotlin("multiplatform") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("io.miret.etienne.sass") version "1.6.0"
    id("com.gradleup.shadow") version "8.3.10"
}

kotlin {
    jvm()
    js(IR) {
        browser {
            binaries.executable()
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-server-sessions:$ktorVersion")
                implementation("io.ktor:ktor-server-auth:$ktorVersion")
                implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
                implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
                implementation("com.webauthn4j:webauthn4j-core:0.31.0.RELEASE")
                implementation("org.postgresql:postgresql:$postgresVersion")
                implementation("org.checkerframework:checker-qual:3.47.0")
                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-migration-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-migration-jdbc:$exposedVersion")
                implementation("io.insert-koin:koin-ktor:$koinVersion")
                implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
                implementation("ch.qos.logback:logback-classic:1.5.31")
                implementation("software.amazon.awssdk:s3:2.42.23")
                implementation("software.amazon.awssdk:ses:2.42.23")
                implementation("software.amazon.awssdk:sesv2:2.42.23")
                implementation("org.apache.commons:commons-email:1.6.0")
                implementation("com.password4j:password4j:1.8.4")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("net.bytebuddy:byte-buddy:$byteBuddyVersion")
                implementation("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.10.2")
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                implementation("io.kotest:kotest-property:$kotestVersion")
                implementation("org.junit.jupiter:junit-jupiter:5.11.4")
                implementation("io.mockk:mockk:1.14.10")
                implementation("io.ktor:ktor-server-test-host:$ktorVersion")
                implementation("com.h2database:h2:2.3.232")
                implementation("org.testcontainers:testcontainers:1.21.3")
                implementation("org.testcontainers:junit-jupiter:1.21.3")
                implementation("org.testcontainers:localstack:1.21.3")
                implementation("org.testcontainers:postgresql:1.21.3")
                implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("com.microsoft.playwright:playwright:$playwrightVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            }
        }
    }
}

group = "com.adoptu"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "3.4.2"
val exposedVersion = "1.2.0"
val postgresVersion = "42.7.10"
val koinVersion = "4.1.1"
val kotlinxDatetimeVersion = "0.7.0"
val byteBuddyVersion = "1.17.5"
val kotestVersion = "6.1.3"
val playwrightVersion = "1.58.0"

val run by tasks.registering(JavaExec::class) {
    group = "application"
    description = "Runs this application."
    mainClass.set("com.adoptu.ApplicationKt")
    classpath(tasks.named("jvmJar").map { it.outputs.files })
    workingDir = project.projectDir
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

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.named<ProcessResources>("processResources") {
    from(layout.buildDirectory.dir("sass")) {
        include("*.css")
        into("static/css")
    }
    dependsOn("compileSass")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
    outputs.upToDateWhen { false }
}

tasks.register<Exec>("dockerUp") {
    group = "docker"
    description = "Start Docker containers for integration tests"
    workingDir = rootProject.rootDir
    environment["DOCKER_HOST"] = "unix:///run/docker.sock"
    commandLine("sh", "-c", "docker compose -f docker-compose.test.yml up -d")
}

tasks.register<Exec>("dockerDown") {
    group = "docker"
    description = "Stop Docker containers for integration tests"
    workingDir = rootProject.rootDir
    commandLine("docker", "compose", "-f", "docker-compose.test.yml", "down", "-v")
}

tasks.register<Copy>("copyJsToResources") {
    group = "build"
    description = "Copy Kotlin/JS bundle to resources"
    from(layout.buildDirectory.dir("compileSync/js/main/productionExecutable/kotlin"))
    into("src/main/resources/static/js")
    include("*.js")
}

tasks.named("run") {
    dependsOn("copyJsToResources")
}

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
        includeTestsMatching(".*IT")
    }
}
