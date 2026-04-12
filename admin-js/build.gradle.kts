plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            commonWebpackConfig {
                outputFileName = "admin.js"
            }
        }
    }
    sourceSets {
        val main by getting {
            kotlin.srcDir("src/main/kotlin")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

tasks.register<Copy>("copyAdminJsToRootResources") {
    dependsOn("browserProductionWebpack")
    from(layout.buildDirectory.dir("distributions/admin.js"))
    into(rootProject.layout.projectDirectory.dir("src/main/resources/static/js"))
    rename { "admin.js" }
}
