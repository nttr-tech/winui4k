plugins {
    id("winui4k.kotlin-library")
}

dependencies {
    api(project(":winui4k"))
    api(libs.miglayout.core) // the 5.x series is the last one compatible with Java 8
}
