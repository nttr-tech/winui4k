plugins {
    id("winui4k.kotlin-library")
    id("winui4k.ui-test")
}

description = "MigLayout layout manager integration for WinUI4K"

dependencies {
    api(project(":winui4k"))
    api(libs.miglayout.core) // the 5.x series is the last one compatible with Java 8
    testImplementation(testFixtures(project(":winui4k")))
}
