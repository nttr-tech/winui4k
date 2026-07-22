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

rootProject.name = "winui4k"

include(":winui4k")
include(":winui4k-panama")
include(":winui4k-jna")
include(":winui4k-jnr")
include(":winui4k-coroutines")
include(":winui4k-sample-gallery")
