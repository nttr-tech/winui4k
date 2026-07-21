plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":winui4k"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
