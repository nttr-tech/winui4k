import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-library`
}

// Build with JDK 25, but guarantee only Java 8 bytecode + Java 8 API
// (the FFI implementations have been split out into winui4k-panama / winui4k-jna, so the core has no JDK-specific dependency)
kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=8")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}
