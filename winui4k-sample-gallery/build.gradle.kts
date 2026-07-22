plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-all"))
}

application {
    mainClass = "com.appkitbox.winui4k.sample.gallery.MainForGalleryKt"
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
