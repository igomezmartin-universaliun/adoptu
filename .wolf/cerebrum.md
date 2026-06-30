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

- [2026-06-22] Do NOT edit CSS files directly. All styles live in `backend/src/main/scss/`. Shared/nav styles go in `_layout.scss`; page-specific styles in their own `.scss` file. After editing SCSS, recompile: `npx sass backend/src/main/scss/<name>.scss backend/src/main/resources/static/css/<name>.css --no-source-map`
- [2026-06-30] Do NOT add a `t('newKey')` call in a page's static JS (e.g. `shelters.js`) without also adding `"newKey" to "..."` for all 5 languages in `frontend/src/jsMain/kotlin/com/adoptu/frontend/I18n.kt`. The live translation system is the Kotlin/JS `I18n` object compiled into `common.js` and exposed as `window.AdoptuI18n`/`window.t` (wired in `Shared.kt`'s `commonScripts`) — it falls back to rendering the raw key string when a key is missing, which is invisible until someone hits that code path. The legacy `backend/src/main/resources/static/js/i18n/i18n.js` + `i18n-*.js` files are DEAD CODE — no page template loads them — so editing them has zero effect; always edit `I18n.kt`. After editing `I18n.kt`, rebuild with `./gradlew :frontend:jsBrowserProductionWebpack` to regenerate `common.js` (it auto-copies into `backend/src/main/resources/static/js/`). Recurring bug class — see bug-051.
- [2026-06-30] New git worktrees in this repo are missing `gradle/wrapper/gradle-wrapper.jar` (it's untracked by git, only `gradle-wrapper.properties` is). Before running `./gradlew` in a fresh worktree, `cp` the jar from the main checkout's `gradle/wrapper/gradle-wrapper.jar` into the worktree first, or `./gradlew` fails with "Unable to access jarfile".
- [2026-06-30] Merging a worktree branch that touches `frontend/src/jsMain/kotlin/**` into a `main` that has since regenerated its own `common.js`/`common.js.map` WILL conflict on those two generated bundle files even when the Kotlin source merges cleanly — they're build output, not source. Resolve with `git checkout --ours` on the bundle files (keep main's pre-merge bundle) and then rebuild via `./gradlew :frontend:jsBrowserProductionWebpack` post-merge to regenerate them from the merged source, rather than trying to hand-resolve the minified diff.
- [2026-06-30] Do NOT mix Base64 encoder/decoder variants in CryptoService. `encrypt()` uses `Base64.getUrlEncoder()` (produces URL-safe `_` and `-`). Both `decrypt()` and `decryptWithKey()` MUST use `Base64.getUrlDecoder()`. Using the standard `Base64.getDecoder()` silently returns null and causes all password operations to fail (15 tests).

## Decision Log

<!-- Significant technical decisions with rationale. Why X was chosen over Y. -->
