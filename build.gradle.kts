plugins {
    kotlin("multiplatform") version "2.4.0" apply false
    kotlin("jvm") version "2.4.0" apply false
    kotlin("plugin.serialization") version "2.4.0" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.8" apply false
}

tasks.register("test") {
    group = "verification"
    description = "Run all unit tests (backend JVM + frontend JS)"
    dependsOn(":backend:test", ":frontend:jsTest")
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Run backend integration tests"
    dependsOn(":backend:integrationTest")
}
