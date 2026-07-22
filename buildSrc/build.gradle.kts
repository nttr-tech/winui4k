plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Lets the convention plugin (winui4k.kotlin-common) apply the Kotlin plugin
    implementation(libs.kotlin.gradle.plugin)
}
