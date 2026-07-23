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
    // Lets the convention plugin (winui4k.kotlin-library) and the root build.gradle.kts
    // apply the nmcp plugins for publishing to Maven Central
    implementation(libs.nmcp.gradle.plugin)
    implementation(libs.nmcp.aggregation.gradle.plugin)
}
