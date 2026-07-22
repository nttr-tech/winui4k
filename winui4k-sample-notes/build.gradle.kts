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
    implementation(project(":winui4k"))
    implementation(project(":winui4k-extension-coroutines"))
    runtimeOnly(project(":winui4k-panama"))
    runtimeOnly(project(":winui4k-jna"))
    runtimeOnly(project(":winui4k-jnr"))
}

listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
    it.configure {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}

application {
    mainClass = "com.appkitbox.winui4k.sample.notes.MainForNotesKt"
}

tasks.named<JavaExec>("run") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
