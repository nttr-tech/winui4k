// Common settings for each module live in the buildSrc convention plugins
// (winui4k.kotlin-common / winui4k.kotlin-library / winui4k.kotlin-application).
// Dependency and plugin versions are centrally managed in gradle/libs.versions.toml.

plugins {
    id("com.diffplug.spotless")
    // Lets the detektFile task acquire the JDK 21 toolchain
    id("jvm-toolchains")
}

// detekt 1.23's embedded Kotlin compiler doesn't run on JDK 25, so run the CLI in a
// separate process on JDK 21 (the same approach as winui4k.kotlin-common's detekt task)
val detektCli by configurations.creating

dependencies {
    detektCli("io.gitlab.arturbosch.detekt:detekt-cli:${libs.versions.detekt.get()}")
}

// A task for Claude Code Hooks to quickly check just the one file that was edited.
// Usage: ./gradlew.bat detektFile -PdetektFile=<the .kt/.kts file to check>
// Checking a whole module is handled by each module's detekt task (included in check).
tasks.register<JavaExec>("detektFile") {
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    val targetFile = providers.gradleProperty("detektFile")
    val detektConfigFile = file("config/detekt/detekt.yml")
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "--config",
                detektConfigFile.absolutePath,
                "--build-upon-default-config",
                "--input",
                targetFile.get(),
            )
        },
    )
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
