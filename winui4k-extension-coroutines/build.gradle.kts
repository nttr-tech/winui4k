import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-library`
}

// Build with JDK 25, target Java 8 bytecode (kotlinx-coroutines-core is Java 8 compatible)
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
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
