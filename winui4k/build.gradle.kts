import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.net.URI
import java.util.zip.ZipFile

// Core targets Java 8
// (the FFI implementations have been split out into winui4k-ffi-panama / winui4k-ffi-jna / winui4k-ffi-jnr, so the core has no JDK-specific dependency)
plugins {
    id("winui4k.kotlin-library")
}

// ---------------------------------------------------------------------------
// Fetch the Windows App SDK bootstrap DLL from NuGet and bundle it as a JAR resource
// ---------------------------------------------------------------------------

val windowsAppSdkFoundationVersion = libs.versions.windowsAppSdkFoundation.get()

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

// ---------------------------------------------------------------------------
// Fetch the WebView2 Core WinRT implementation DLL from NuGet and bundle it as a JAR resource
// (since WinAppSDK 1.2, the runtime package no longer bundles Microsoft.Web.WebView2.Core.dll and
//  expects the app to place it, so winui4k bundles and pre-loads it instead)
// ---------------------------------------------------------------------------

/** The version of Microsoft.Web.WebView2 that Microsoft.WindowsAppSDK.WinUI depends on. */
val webView2Version = libs.versions.webView2.get()

val webView2ResourceDir: Provider<Directory> =
    layout.buildDirectory.dir("generated-resources/webview2")

val fetchWebView2 = tasks.register("fetchWebView2") {
    description = "Extracts Microsoft.Web.WebView2.Core.dll (x86/x64/arm64) from the NuGet package"
    val version = webView2Version
    val outputDir = webView2ResourceDir
    inputs.property("webView2Version", version)
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile
        val nupkg = dir.resolve("webview2.nupkg")
        val rids = listOf("win-x86", "win-x64", "win-arm64")
        val alreadyExtracted = rids.all { rid ->
            dir.resolve("native/$rid/Microsoft.Web.WebView2.Core.dll").exists()
        }
        if (alreadyExtracted) return@doLast

        val url = "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/" +
            "$version/microsoft.web.webview2.$version.nupkg"
        logger.lifecycle("Downloading Microsoft.Web.WebView2 $version ...")
        URI(url).toURL().openStream().use { input ->
            nupkg.outputStream().use { input.copyTo(it) }
        }

        ZipFile(nupkg).use { zip ->
            for (rid in rids) {
                // The DLL under native_uap is the WinRT (Microsoft.Web.WebView2.Core namespace) implementation
                val entryPath = "runtimes/$rid/native_uap/Microsoft.Web.WebView2.Core.dll"
                val entry = zip.getEntry(entryPath)
                    ?: error("webview2 core dll not found in nupkg for $rid")
                val dest = dir.resolve("native/$rid/Microsoft.Web.WebView2.Core.dll")
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
    resources.srcDir(webView2ResourceDir)
}

tasks.named("processResources") {
    dependsOn(fetchBootstrap)
    dependsOn(fetchWebView2)
}

// ---------------------------------------------------------------------------
// Tests (real E2E that launches actual WinUI. Requires Windows + the WinAppSDK runtime)
// ---------------------------------------------------------------------------

dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit.platform.launcher)
    // JDK 22+ selects the Panama backend; JDK 8/9, which can't load Panama, select JNA
    testRuntimeOnly(project(":winui4k-ffi-panama"))
    testRuntimeOnly(project(":winui4k-ffi-jna"))
}

// Also include the Java 22-targeted winui4k-ffi-panama on the test runtime classpath
// (the tests themselves target Java 8, but run on JDK 25)
targetJvm25AtRuntime("testRuntimeClasspath")

tasks.test {
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
val javaToolchains = extensions.getByType<JavaToolchainService>()
val testOnJavaTasks = listOf(8, 9, 22).map { version ->
    tasks.register<Test>("testOnJava$version") {
        description = "Runs the tests on JDK $version"
        group = "verification"
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(version)
        }
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
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
    dependsOn(tasks.test)
    dependsOn(testOnJavaTasks)
}

// ---------------------------------------------------------------------------
// Download the Windows App SDK runtime installers
// ---------------------------------------------------------------------------

val windowsAppSdkVersion = libs.versions.windowsAppSdk.get()

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
