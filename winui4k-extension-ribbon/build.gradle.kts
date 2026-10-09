import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("winui4k.kotlin-library")
    id("winui4k.ui-test")
}

description = "Office-style ribbon (WRibbon and its bars) for WinUI4K"

dependencies {
    api(project(":winui4k"))
    testImplementation(testFixtures(project(":winui4k")))
}

// The ribbon is a custom-drawn control that uses the core's internal APIs (XamlInterop, ComPtr, WComponent's uiElement,
// etc.) directly. To avoid making them public API, the core is compiled as a friend module (Kotlin's -Xfriend-paths, the
// same mechanism Gradle uses for a module's own test → main)
fun coreArtifacts(configuration: String) =
    configurations.named(configuration).get().incoming.artifactView {
        componentFilter { it is ProjectComponentIdentifier && it.projectPath == ":winui4k" }
    }.files

tasks.named<KotlinCompile>("compileKotlin") {
    friendPaths.from(coreArtifacts("compileClasspath"))
}
tasks.named<KotlinCompile>("compileTestKotlin") {
    friendPaths.from(coreArtifacts("testCompileClasspath"))
}
