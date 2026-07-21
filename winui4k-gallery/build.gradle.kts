import java.net.URI
import java.util.zip.ZipFile

plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":winui4k"))
    implementation(project(":winui4k-coroutines"))
}

application {
    mainClass = "jp.hisano.winui4k.gallery.GalleryAppKt"
}

// ---------------------------------------------------------------------------
// Fetch the Windows App SDK bootstrap DLL from NuGet
// (required to call MddBootstrapInitialize2 at runtime; the runtime itself is installed separately)
// ---------------------------------------------------------------------------

// Windows App SDK 2.x splits the NuGet package, and the bootstrap DLL is
// included in Microsoft.WindowsAppSDK.Foundation, which the meta-package
// Microsoft.WindowsAppSDK (2.2.0) depends on
val windowsAppSdkFoundationVersion = "2.1.0" // Dependency of Microsoft.WindowsAppSDK 2.2.0; matches 0x00020000 / 2.2.0.0 in Toolkit.kt

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

tasks.named<JavaExec>("run") {
    dependsOn(fetchBootstrap)

    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")

    // Let Toolkit load the bootstrap DLL via an absolute path
    val dllPath = nativeDir.map { it.file("Microsoft.WindowsAppRuntime.Bootstrap.dll").asFile.absolutePath }
    doFirst {
        systemProperty("winui4k.bootstrap.dll", dllPath.get())
    }
}
