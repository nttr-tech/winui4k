import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "com.appkitbox.winui4k"
version = "0.1.0"

val winui4k = extensions.create<Winui4kExtension>("winui4k").apply {
    targetJavaVersion.convention(8)
}

// Build with JDK 25, but only guarantee the targetJavaVersion bytecode + API
kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(winui4k.targetJavaVersion.map { JvmTarget.fromTarget(if (it == 8) "1.8" else it.toString()) })
        freeCompilerArgs.add(winui4k.targetJavaVersion.map { "-Xjdk-release=$it" })
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(winui4k.targetJavaVersion)
}
