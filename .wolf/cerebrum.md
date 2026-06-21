# Cerebrum

> OpenWolf's learning memory. Updated automatically as the AI learns from interactions.
> Do not edit manually unless correcting an error.
> Last updated: 2026-06-21

## User Preferences

<!-- How the user likes things done. Code style, tools, patterns, communication. -->

## Key Learnings

- **Project:** adoptu
- **Description:** A Kotlin pet adoption web application with **FIDO2/WebAuthn** passwordless authentication. Multi-page application with HTML rendered from Kotlin (kotlinx.html).
- **Gradle multi-module:** Root `build.gradle.kts` uses `apply false` plugins + explicit `tasks.register("test")` delegating to `:backend:test` AND `:frontend:jsTest`. The root project never applies Kotlin directly.
- **Kotlin/JS yarn tasks:** `kotlin.js.yarn=false` in `gradle.properties` does NOT work in Kotlin 2.3.20 — yarn still runs (`kotlinYarnSetup`, `kotlinNpmInstall`). `KotlinNpmInstallTask` extends `DefaultTask` (not `Exec`), so `.environment()` is unavailable. The only way to inject `NODE_OPTIONS` is in `gradlew` before the JVM starts.
- **Node.js 22 + yarn 1.x:** DEP0169 (`url.parse()`) and DEP0040 (`punycode`) warnings come from yarn 1.22.17 internals. Fixed by exporting `NODE_OPTIONS="--no-deprecation"` in `gradlew`.
- **Frontend test task name:** Kotlin/JS with `js(IR)` target uses `jsTest` (not `test`) — the root test task must `dependsOn(":backend:test", ":frontend:jsTest")`.

- **DatabaseFactory.listOfTables must be kept in sync with Models.kt:** Every `Table` object defined in `Models.kt` must be added to `listOfTables` in `DatabaseFactory.kt` — omitting one means `SchemaUtils.create()` never creates that table, causing a PSQLException at runtime. Tables previously missing: `UserSterilizationLocations`, `UserShelters`, `PasswordResetTokens`, `EmailChangeTokens`.

## Do-Not-Repeat

<!-- Mistakes made and corrected. Each entry prevents the same mistake recurring. -->
<!-- Format: [YYYY-MM-DD] Description of what went wrong and what to do instead. -->
- [2026-06-21] Do NOT add `kotlin.js.yarn=false` to `gradle.properties` — it has no effect in Kotlin 2.3.20 and yarn tasks still run. For Node.js 22 deprecation warnings from yarn, set `NODE_OPTIONS=--no-deprecation` in `gradlew` instead.
- [2026-06-21] Do NOT forget `:frontend:jsTest` when registering root `test` task — using only `:backend:test` silently skips all frontend tests.

- [2026-06-21] Do NOT define a new `Table` object in `Models.kt` without also adding it to `listOfTables` in `DatabaseFactory.kt`. Missing entries silently skip table creation and cause `PSQLException: relation "..." does not exist` at runtime.

## Decision Log

<!-- Significant technical decisions with rationale. Why X was chosen over Y. -->
