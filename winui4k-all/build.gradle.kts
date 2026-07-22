import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Aggregate module that pulls in every module other than the samples in one shot
// (the API is exposed via api, and the FFI backends are propagated onto the runtime classpath via runtimeOnly)
plugins {
    kotlin("jvm")
    `java-library`
}

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

dependencies {
    api(project(":winui4k"))
    api(project(":winui4k-extension-coroutines"))
    api(project(":winui4k-extension-miglayout"))
    runtimeOnly(project(":winui4k-panama"))
    runtimeOnly(project(":winui4k-jna"))
    runtimeOnly(project(":winui4k-jnr"))
}

// This module targets Java 8, but its runtime classpath should still include winui4k-panama
// (which targets Java 22) — on a Java 8 run, Ffi skips it at the ServiceLoader level
listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
    it.configure {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}
