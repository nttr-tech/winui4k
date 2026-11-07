plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-all"))
    // Tests for the strings (messages*.properties). They show no window, so the E2E winui4k.ui-test is not used
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Read the test sources as UTF-8
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

application {
    // Unlike the Kotlin version (MainForGalleryKt), the Java version uses the static main of the MainForGallery class
    mainClass = "com.appkitbox.winui4k.sample.gallery.MainForGallery"
}
