// Common settings for each module live in the buildSrc convention plugins
// (winui4k.kotlin-common / winui4k.kotlin-library / winui4k.kotlin-application).
// Dependency and plugin versions are centrally managed in gradle/libs.versions.toml.

plugins {
    id("com.diffplug.spotless")
}

// Each module's sources are formatted by Spotless in the convention plugin (winui4k.kotlin-common).
// This only targets build scripts that don't belong to any module.
spotless {
    // The repo standardizes on LF for every file (don't let Windows convert it to native line endings)
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/*.gradle.kts", "buildSrc/src/main/kotlin/*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "intellij_idea",
                "max_line_length" to "off",
            ),
        )
    }
}
