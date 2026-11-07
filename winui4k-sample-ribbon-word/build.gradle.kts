plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-sample-ribbon-common"))
    // Tests for the strings (messages.properties) (no window is shown)
    testImplementation(testFixtures(project(":winui4k-sample-ribbon-common")))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass = "com.appkitbox.winui4k.sample.ribbon.word.MainForRibbonWordKt"
}
