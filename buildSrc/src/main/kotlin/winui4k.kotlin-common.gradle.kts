import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
}

group = "com.appkitbox.winui4k"
// The release workflow overrides this with the tag's version via -Pwinui4kVersion
version = providers.gradleProperty("winui4kVersion").getOrElse("0.1.0")

val winui4k = extensions.create<Winui4kExtension>("winui4k").apply {
    targetJavaVersion.convention(8)
}

// Build with JDK 25, but only guarantee the targetJavaVersion bytecode + API
kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(winui4k.targetJavaVersion.map { JvmTarget.fromTarget(if (it == 8) "1.8" else it.toString()) })
        freeCompilerArgs.add(winui4k.targetJavaVersion.map { "-Xjdk-release=$it" })
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(winui4k.targetJavaVersion)
}

// Formatting follows ktlint. Keep the rule settings in sync with the root .editorconfig
// (Spotless doesn't reliably pick up .editorconfig, so pass it explicitly via editorConfigOverride)
val ktlintVersion = the<VersionCatalogsExtension>().named("libs").findVersion("ktlint").get().requiredVersion
val ktlintOverrides = mapOf(
    // The existing code follows IntelliJ IDEA's formatting output, so use this instead of ktlint_official
    "ktlint_code_style" to "intellij_idea",
    "max_line_length" to "off",
    // Disable rules that would rewrite the existing code on a large scale, like block body <-> expression body
    "ktlint_standard_function-expression-body" to "disabled",
    "ktlint_standard_function-signature" to "disabled",
    "ktlint_standard_class-signature" to "disabled",
    // Disabled because inserting blank lines between when branches can break comment placement in some cases
    "ktlint_standard_blank-line-between-when-conditions" to "disabled",
    // Disable lint-style rules that require renaming file names or property names; that's out of scope for formatting
    "ktlint_standard_filename" to "disabled",
    "ktlint_standard_property-naming" to "disabled",
    // Disabled to preserve column-aligned trailing comments (e.g. the vtable slot tables)
    "ktlint_standard_no-multi-spaces" to "disabled",
)

// Static analysis follows detekt. Formatting and naming are handled by ktlint (Spotless), so
// rules that overlap are disabled in the root config/detekt/detekt.yml.
// detekt 1.23's embedded Kotlin compiler doesn't run on JDK 25 (JavaVersion.parse can't parse
// "25.0.3"), so run the CLI in a separate process on JDK 21 instead of the Gradle plugin's in-process execution
val detektVersion = the<VersionCatalogsExtension>().named("libs").findVersion("detekt").get().requiredVersion
val detektCli by configurations.creating

dependencies {
    detektCli("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
}

val detektConfigFile = rootProject.file("config/detekt/detekt.yml")
val detektReportFile = layout.buildDirectory.file("reports/detekt/detekt.txt")

val detektTask = tasks.register<JavaExec>("detekt") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs detekt static analysis on Kotlin sources."
    // Capturing a script object in the onlyIf lambda would break configuration cache, so capture only the File
    val srcDir = file("src")
    onlyIf { srcDir.exists() }
    inputs.files(fileTree("src") { include("**/*.kt", "**/*.kts") }).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(detektConfigFile).withPathSensitivity(PathSensitivity.NONE)
    outputs.file(detektReportFile)
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    args(
        "--config",
        detektConfigFile.absolutePath,
        "--build-upon-default-config",
        "--input",
        file("src").absolutePath,
        "--report",
        "txt:${detektReportFile.get().asFile.absolutePath}",
    )
}

tasks.named("check") {
    dependsOn(detektTask)
}

spotless {
    // The repo standardizes on LF for every file (don't let Windows convert it to native line endings)
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    kotlin {
        ktlint(ktlintVersion).editorConfigOverride(ktlintOverrides)
    }
    kotlinGradle {
        ktlint(ktlintVersion).editorConfigOverride(ktlintOverrides)
    }
}
