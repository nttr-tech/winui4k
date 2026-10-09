import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// Common settings for E2E tests that actually launch WinUI (requires Windows + the WinAppSDK runtime).
// UiTestHarness lives in winui4k's testFixtures; modules other than winui4k reference it with
// testImplementation(testFixtures(project(":winui4k")))
plugins {
    id("winui4k.kotlin-common")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "testImplementation"(libs.findLibrary("kotest-runner-junit5").get())
    "testImplementation"(libs.findLibrary("kotest-assertions-core").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
    // JDK 22+ selects the Panama backend; JDK 8/9, which can't load Panama, select JNA
    "testRuntimeOnly"(project(":winui4k-ffi-panama"))
    "testRuntimeOnly"(project(":winui4k-ffi-jna"))
}

// Also include the Java 22-targeted winui4k-ffi-panama on the test runtime classpath
// (the tests themselves target Java 8, but run on JDK 25)
targetJvm25AtRuntime("testRuntimeClasspath")

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// Show per-test-case results in CI logs (applies to all Test tasks, including testOnJavaXX)
tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// In addition to tasks.test (the toolchain's JDK 25), run the same tests on the minimum
// supported JDK (8), the JDK right after the module system was introduced (9), and the JDK
// where Panama was finalized (22). If a given JDK version isn't installed locally, the
// foojay resolver downloads it automatically.
val testSourceSet = the<SourceSetContainer>().named("test")
val testOnJavaTasks = listOf(8, 9, 22).map { version ->
    tasks.register<Test>("testOnJava$version") {
        description = "Runs the tests on JDK $version"
        group = "verification"
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(version)
        }
        testClassesDirs = testSourceSet.get().output.classesDirs
        classpath = testSourceSet.get().runtimeClasspath
        useJUnitPlatform()
        if (version >= 22) {
            // --enable-native-access doesn't exist as a flag on JDK 8/9, so only pass it on Panama-capable JDKs
            jvmArgs("--enable-native-access=ALL-UNNAMED")
        }
    }
}

tasks.register("testOnAllJavaVersions") {
    description = "Runs the tests on every supported JDK version (8 / 9 / 22 / 25)"
    group = "verification"
    dependsOn(tasks.named("test"))
    dependsOn(testOnJavaTasks)
}
