plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-all"))

    // Tests for the strings (messages*.properties). No window is shown
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "com.appkitbox.winui4k.sample.notes.MainForNotesKt"
}
