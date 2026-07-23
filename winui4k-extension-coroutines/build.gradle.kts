plugins {
    id("winui4k.kotlin-library")
}

description = "Kotlin coroutines extensions for WinUI4K"

dependencies {
    api(project(":winui4k"))
    api(libs.kotlinx.coroutines.core) // Java 8 compatible
}
