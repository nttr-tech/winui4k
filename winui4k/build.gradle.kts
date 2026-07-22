import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.util.zip.ZipFile

plugins {
    kotlin("jvm")
    `java-library`
}

// Build with JDK 25, but guarantee only Java 8 bytecode + Java 8 API
// (the FFI implementations have been split out into winui4k-panama / winui4k-jna, so the core has no JDK-specific dependency)
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

// ---------------------------------------------------------------------------
// Fetch the Windows App SDK bootstrap DLL from NuGet and bundle it as a JAR resource
// ---------------------------------------------------------------------------

val windowsAppSdkFoundationVersion = "2.1.0"

val nativeResourceDir: Provider<Directory> =
    layout.buildDirectory.dir("generated-resources/bootstrap")

val fetchBootstrap = tasks.register("fetchBootstrap") {
    description = "Extracts Microsoft.WindowsAppRuntime.Bootstrap.dll (x64/arm64) from the NuGet package"
    val version = windowsAppSdkFoundationVersion
    val outputDir = nativeResourceDir
    inputs.property("foundationVersion", version)
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile
        val nupkg = dir.resolve("foundation.nupkg")
        val rids = listOf("win-x86", "win-x64", "win-arm64")
        val alreadyExtracted = rids.all { rid ->
            dir.resolve("native/$rid/Microsoft.WindowsAppRuntime.Bootstrap.dll").exists()
        }
        if (alreadyExtracted) return@doLast

        val url = "https://api.nuget.org/v3-flatcontainer/microsoft.windowsappsdk.foundation/" +
            "$version/microsoft.windowsappsdk.foundation.$version.nupkg"
        logger.lifecycle("Downloading Windows App SDK Foundation $version ...")
        URI(url).toURL().openStream().use { input ->
            nupkg.outputStream().use { input.copyTo(it) }
        }

        ZipFile(nupkg).use { zip ->
            for (rid in rids) {
                val entryPath = "runtimes/$rid/native/Microsoft.WindowsAppRuntime.Bootstrap.dll"
                val entry = zip.getEntry(entryPath)
                    ?: error("bootstrap dll not found in nupkg for $rid")
                val dest = dir.resolve("native/$rid/Microsoft.WindowsAppRuntime.Bootstrap.dll")
                dest.parentFile.mkdirs()
                zip.getInputStream(entry).use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                logger.lifecycle("Extracted: $dest")
            }
        }
        nupkg.delete()
    }
}

sourceSets.main {
    resources.srcDir(nativeResourceDir)
}

tasks.named("processResources") {
    dependsOn(fetchBootstrap)
}

// ---------------------------------------------------------------------------
// Download the Windows App SDK runtime installers
// ---------------------------------------------------------------------------

val windowsAppSdkVersion = "2.2.0"

tasks.register("downloadInstallers") {
    description = "Downloads the Windows App SDK $windowsAppSdkVersion runtime installers (x86/x64/arm64)"
    group = "build"
    val version = windowsAppSdkVersion
    val outputDir = layout.projectDirectory.dir("installer")
    inputs.property("windowsAppSdkVersion", version)
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.asFile.apply { mkdirs() }
        for (arch in listOf("x86", "x64", "arm64")) {
            val fileName = "WindowsAppRuntimeInstall-$arch.exe"
            val dest = dir.resolve(fileName)
            if (dest.exists()) {
                logger.lifecycle("Already exists: $dest")
                continue
            }
            val url = "https://aka.ms/windowsappsdk/2.2/$version/windowsappruntimeinstall-$arch.exe"
            logger.lifecycle("Downloading $fileName ...")
            URI(url).toURL().openStream().use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            logger.lifecycle("Downloaded: $dest")
        }
    }
}
