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

- **`Country` (backend/src/main/kotlin/com/adoptu/common/Country.kt) is the single source of truth for country values.** The dropdown (`Shared.kt: countrySelect()`), DB storage (`Models.kt` columns use `enumerationByName("country", 100, Country::class)`), and validation (every repository touching a `country` column calls `Country.fromDisplayName()` before reading/writing) all derive from this enum. DTOs stay `String` on the wire (`country: String`) — repositories parse at the boundary, throwing `IllegalArgumentException` on insert/update with an unrecognized value, and returning `emptyList()` on a search filter with an unrecognized value. `Country.fromDisplayName()` does accent/case-insensitive fallback matching, so stray input (e.g. `"México"` vs `"Mexico"`) still resolves instead of silently returning zero results.
- **Test fixtures must use real canonical country names from `Country.kt`, not placeholders.** Several test files used `"USA"` as a stand-in country value — it was never a real dropdown option (the canonical value is `"United States"`) and broke once country columns became enum-backed. Always use an exact `Country.entries` `displayName` in test data.

## Do-Not-Repeat

<!-- Mistakes made and corrected. Each entry prevents the same mistake recurring. -->
<!-- Format: [YYYY-MM-DD] Description of what went wrong and what to do instead. -->
- [2026-06-21] Do NOT add `kotlin.js.yarn=false` to `gradle.properties` — it has no effect in Kotlin 2.3.20 and yarn tasks still run. For Node.js 22 deprecation warnings from yarn, set `NODE_OPTIONS=--no-deprecation` in `gradlew` instead.
- [2026-06-21] Do NOT forget `:frontend:jsTest` when registering root `test` task — using only `:backend:test` silently skips all frontend tests.

- [2026-06-21] Do NOT define a new `Table` object in `Models.kt` without also adding it to `listOfTables` in `DatabaseFactory.kt`. Missing entries silently skip table creation and cause `PSQLException: relation "..." does not exist` at runtime.

- [2026-06-22] Do NOT edit CSS files directly. All styles live in `backend/src/main/scss/`. Shared/nav styles go in `_layout.scss`; page-specific styles in their own `.scss` file. After editing SCSS, recompile: `npx sass backend/src/main/scss/<name>.scss backend/src/main/resources/static/css/<name>.css --no-source-map`

- [2026-06-30] Do NOT add a free-text/hardcoded country list anywhere in this codebase again. Use `Country.entries` (backend/src/main/kotlin/com/adoptu/common/Country.kt) for any dropdown, and `Country.fromDisplayName()` to validate/normalize any incoming country string before it touches a DB column or a search filter. The original bug (Mexican shelter search returning zero results) was caused by exactly this: a hardcoded dropdown list drifting out of sync with free-text DB values (`"Mexico"` vs `"México"`).

## Decision Log

<!-- Significant technical decisions with rationale. Why X was chosen over Y. -->
- [2026-06-30] Country values switched from free-text `varchar` columns to an Exposed `enumerationByName` column backed by a new `Country` enum, after a Mexico search returned zero results due to an accent mismatch (`"Mexico"` dropdown value vs `"México"` stored in 2 shelter rows). Considered: (1) normalize the bad DB rows only, (2) make the filter comparison accent-insensitive, (3) make `Country` an enum and the single source of truth end-to-end. Chose (3), per explicit user direction, to eliminate the whole class of dropdown/DB drift rather than patch one symptom — affects 6 tables (shelters, sterilization locations, user shelters, user sterilization locations, temporal homes, photographers) that all shared the same free-text-country + exact-match-filter pattern.
