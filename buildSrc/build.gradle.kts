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
    // Lets the convention plugin (winui4k.fat-jar) apply the Shadow plugin
    implementation(libs.shadow.gradle.plugin)
    // Lets the convention plugin (winui4k.kotlin-common) apply the Spotless plugin
    implementation(libs.spotless.gradle.plugin)
}
