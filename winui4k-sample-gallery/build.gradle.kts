import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.util.zip.ZipFile

plugins {
    kotlin("jvm")
    application
}

// Build with JDK 25, target Java 8 bytecode (so the runJna task can verify Java 8 on real hardware)
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
    implementation(project(":winui4k-coroutines"))
    runtimeOnly(project(":winui4k-panama"))
    runtimeOnly(project(":winui4k-jna"))
    runtimeOnly(project(":winui4k-jnr"))
}

// gallery targets Java 8, but its runtime classpath should still include winui4k-panama
// (which targets Java 22) — on a Java 8 run, Ffi skips it at the ServiceLoader level
listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
    it.configure {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}

application {
    mainClass = "com.appkitbox.winui4k.sample.gallery.GalleryAppKt"
}

// ---------------------------------------------------------------------------
// Fetch the Windows App SDK bootstrap DLL from NuGet
// (required to call MddBootstrapInitialize2 at runtime; the runtime itself is installed separately)
// ---------------------------------------------------------------------------

// Windows App SDK 2.x splits the NuGet package, and the bootstrap DLL is
// included in Microsoft.WindowsAppSDK.Foundation, which the meta-package
// Microsoft.WindowsAppSDK (2.2.0) depends on
val windowsAppSdkFoundationVersion = "2.1.0" // Dependency of Microsoft.WindowsAppSDK 2.2.0; matches 0x00020000 / 2.2.0.0 in WinUiUtilities.kt

val nativeDir: Provider<Directory> =
    layout.buildDirectory.dir("native/$windowsAppSdkFoundationVersion")

val fetchBootstrap = tasks.register("fetchBootstrap") {
    description = "Extracts Microsoft.WindowsAppRuntime.Bootstrap.dll from the NuGet package"
    val version = windowsAppSdkFoundationVersion
    val outputDir = nativeDir
    inputs.property("foundationVersion", version)
    outputs.dir(outputDir)

    doLast {
        val arch = System.getProperty("os.arch").lowercase()
        val rid = if (arch == "aarch64" || arch == "arm64") "win-arm64" else "win-x64"

        val dir = outputDir.get().asFile.apply { mkdirs() }
        val dll = dir.resolve("Microsoft.WindowsAppRuntime.Bootstrap.dll")
        if (dll.exists()) return@doLast

        val nupkg = dir.resolve("windowsappsdk.foundation.nupkg")
        val url = "https://api.nuget.org/v3-flatcontainer/microsoft.windowsappsdk.foundation/" +
            "$version/microsoft.windowsappsdk.foundation.$version.nupkg"
        logger.lifecycle("Downloading Windows App SDK Foundation $version (about 6 MB) ...")
        URI(url).toURL().openStream().use { input ->
            nupkg.outputStream().use { input.copyTo(it) }
        }

        ZipFile(nupkg).use { zip ->
            val entry = zip.getEntry("runtimes/$rid/native/Microsoft.WindowsAppRuntime.Bootstrap.dll")
                ?: error("bootstrap dll not found in nupkg for $rid")
            zip.getInputStream(entry).use { input ->
                dll.outputStream().use { input.copyTo(it) }
            }
        }
        nupkg.delete()
        logger.lifecycle("Extracted: $dll")
    }
}

// Let WinUiUtilities load the bootstrap DLL via an absolute path
val dllPath: Provider<String> =
    nativeDir.map { it.file("Microsoft.WindowsAppRuntime.Bootstrap.dll").asFile.absolutePath }

tasks.named<JavaExec>("run") {
    dependsOn(fetchBootstrap)

    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")

    // Referencing a script property directly inside doFirst captures the whole script object,
    // which then can't be stored in the configuration cache, so copy it to a local first
    val dll = dllPath
    doFirst {
        systemProperty("winui4k.bootstrap.dll", dll.get())
    }
}

// Launches the gallery on Java 8 (auto-fetched by the foojay resolver) with the JNA backend, to verify Java 8 support on real hardware
tasks.register<JavaExec>("runJna") {
    description = "Launches the gallery with Java 8 + the JNA backend"
    group = "application"
    dependsOn(fetchBootstrap)

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    }
    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath

    systemProperty("winui4k.ffi", "jna")
    val dll = dllPath
    doFirst {
        systemProperty("winui4k.bootstrap.dll", dll.get())
    }
}

// Launches the gallery on Java 8 with the JNR backend, to verify Java 8 support on real hardware (the JNR counterpart to runJna)
tasks.register<JavaExec>("runJnr") {
    description = "Launches the gallery with Java 8 + the JNR backend"
    group = "application"
    dependsOn(fetchBootstrap)

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    }
    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath

    systemProperty("winui4k.ffi", "jnr")
    val dll = dllPath
    doFirst {
        systemProperty("winui4k.bootstrap.dll", dll.get())
    }
}
