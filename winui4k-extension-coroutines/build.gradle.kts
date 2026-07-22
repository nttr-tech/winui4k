plugins {
    id("winui4k.kotlin-library")
}

dependencies {
    api(project(":winui4k"))
    api(libs.kotlinx.coroutines.core) // Java 8 compatible
}
