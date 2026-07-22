import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-library`
}

// The backend that also works on Java 8. Build with JDK 25, target Java 8 bytecode
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
    implementation(project(":winui4k"))
    // Uses the bundled low-level layer jffi (com.kenai.jffi) directly. Runtime-determined function
    // pointers like COM vtables can't be called through jnr-ffi's high-level API (LibraryLoader)
    implementation("com.github.jnr:jnr-ffi:2.2.17")
}
