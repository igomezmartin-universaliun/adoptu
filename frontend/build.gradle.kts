plugins {
    kotlin("multiplatform") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
}

group = "com.adoptu"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            commonWebpackConfig {
                outputFileName = "common.js"
            }
        }
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-opt-in=kotlin.js.ExperimentalJsExport"))
        }
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}

tasks.register<Copy>("copyJsToBackendResources") {
    dependsOn(tasks.named("jsBrowserProductionWebpack"))
    from(layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    into(project(":backend").layout.projectDirectory.dir("src/main/resources/static/js"))
    include("*.js")
    include("*.js.map")
}

tasks.named("jsBrowserProductionWebpack") {
    finalizedBy("copyJsToBackendResources")
}
