import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=8")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

dependencies {
    implementation(project(":winui4k-all"))
}

// the sample targets Java 8, but its runtime classpath should still include winui4k-ffi-panama
// (which targets Java 22) — on a Java 8 run, Ffi skips it at the ServiceLoader level
listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
    it.configure {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}

application {
    mainClass = "com.appkitbox.winui4k.sample.formwithmiglayout.MainForFormWithMigLayoutKt"
}

tasks.named<JavaExec>("run") {
    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
