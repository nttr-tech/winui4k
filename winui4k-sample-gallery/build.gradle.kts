import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

// Build with JDK 25, target Java 8 bytecode (so the runJna task can verify Java 8 on real hardware)
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
    implementation(project(":winui4k-all"))
}

// gallery targets Java 8, but its runtime classpath should still include winui4k-panama
// (which targets Java 22) — on a Java 8 run, Ffi skips it at the ServiceLoader level
listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
    it.configure {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}

application {
    mainClass = "com.appkitbox.winui4k.sample.gallery.MainForGalleryKt"
}

tasks.named<JavaExec>("run") {
    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// Launches the gallery on Java 8 (auto-fetched by the foojay resolver) with the JNA backend, to verify Java 8 support on real hardware
tasks.register<JavaExec>("runJna") {
    description = "Launches the gallery with Java 8 + the JNA backend"
    group = "application"

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    }
    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath

    systemProperty("winui4k.ffi", "jna")
}

// Launches the gallery on Java 8 with the JNR backend, to verify Java 8 support on real hardware (the JNR counterpart to runJna)
tasks.register<JavaExec>("runJnr") {
    description = "Launches the gallery with Java 8 + the JNR backend"
    group = "application"

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    }
    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath

    systemProperty("winui4k.ffi", "jnr")
}
