pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-downloads JDKs that aren't installed locally, for tasks like runJna (JDK 8)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "winui4k"

include(":winui4k")
include(":winui4k-ffi-panama")
include(":winui4k-ffi-jna")
include(":winui4k-ffi-jnr")
include(":winui4k-extension-coroutines")
include(":winui4k-extension-miglayout")
include(":winui4k-all")
include(":winui4k-sample-form-with-miglayout")
include(":winui4k-sample-gallery")
include(":winui4k-sample-notes")
