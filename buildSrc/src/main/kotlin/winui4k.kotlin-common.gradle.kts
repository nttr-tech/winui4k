import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
}

group = "com.appkitbox.winui4k"
version = "0.1.0"

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
