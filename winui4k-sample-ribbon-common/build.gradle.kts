plugins {
    id("winui4k.kotlin-common")
    `java-library`
    `java-test-fixtures`
}

description = "Shared parts of the ribbon demo apps (Word / Excel / PowerPoint / CAD / Tools): the window frame and helpers for writing ribbon models concisely"

// winui4k-all includes winui4k-ffi-panama, which targets Java 22, so, like the app modules,
// the runtime classpath (including tests) resolves it as JVM 25
targetJvm25AtRuntime()

dependencies {
    api(project(":winui4k-all"))
    // Check helpers used by the tests of the strings (messages.properties) of each app module
    testFixturesImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
