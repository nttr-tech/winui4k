import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI
import java.util.UUID
import java.util.zip.ZipInputStream

plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-all"))
}

application {
    mainClass = "com.appkitbox.winui4k.sample.gallery.MainForGalleryKt"
}

winui4k {
    // The root's `gradlew run` launches only the Gallery
    runFromRoot = true
}

// Launches the gallery on Java 8 (auto-fetched by the foojay resolver) with the JNA backend, to verify Java 8 support on real hardware
tasks.register<JavaExec>("runJna") {
    description = "Launches the gallery with Java 8 + the JNA backend"
    group = "application"

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    }
    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath

    systemProperty("winui4k.ffi", "jna")
}

// ---- Standalone-distribution exe installer generation ----
// Usage: gradlew.bat :winui4k-sample-gallery:packageExe
// Output: winui4k-sample-gallery/build/jpackage/winui4k-gallery-installer.exe (bundles a JRE, runs with no Java install needed)

// jpackage's exe generation needs the WiX Toolset. Fetch the binary release (no admin rights
// required) from a GitHub release and unpack it into build/wix
val wixDir = layout.buildDirectory.dir("wix")
val downloadWix = tasks.register("downloadWix") {
    description = "Downloads the WiX Toolset (binary release) needed for jpackage's exe generation"
    group = "distribution"
    outputs.dir(wixDir)
    val wixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip"
    val targetDir = wixDir.map { it.asFile }
    onlyIf { !targetDir.get().resolve("candle.exe").exists() }
    doLast {
        val dir = targetDir.get()
        URI(wixUrl).toURL().openStream().buffered().use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val outFile = dir.resolve(entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile.mkdirs()
                        outFile.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
        }
    }
}

// jpackage's --input bundles the whole directory into the app, so set up a directory containing just the fat JAR
val jpackageInputDir = layout.buildDirectory.dir("jpackage-input")
val jpackageInput = tasks.register<Sync>("jpackageInput") {
    from(tasks.named<ShadowJar>("shadowJar"))
    into(jpackageInputDir)
}

tasks.register<Exec>("packageExe") {
    description = "Generates the Gallery's JRE-bundled Windows installer (exe) via jpackage"
    group = "distribution"

    dependsOn(downloadWix)
    inputs.files(jpackageInput)
    val destDir = layout.buildDirectory.dir("jpackage")
    outputs.dir(destDir)

    // Use the jpackage bundled with the JDK 25 used for the build
    val launcher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
    val jarName = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFileName }
    val mainClassName = application.mainClass
    val appVersion = version.toString()
    // Windows jpackage has no option to specify a reverse-DNS-style app ID directly, so derive a
    // UUID deterministically from the ID string and use it as the upgrade UUID (the package's identity marker)
    val appId = "com.appkitbox.winui4k.sample.gallery"
    val upgradeUuid = UUID.nameUUIDFromBytes(appId.toByteArray()).toString()
    val iconFile = layout.projectDirectory.file("src/main/resources/GalleryIcon.ico")
    inputs.file(iconFile)
    // Referencing a top-level property directly from a doFirst lambda captures the script object,
    // which breaks configuration cache, so copy it into a local variable first
    val wixDirProvider = wixDir
    val inputDirProvider = jpackageInputDir

    doFirst {
        // Prepend to PATH so jpackage can find candle.exe / light.exe
        environment("PATH", wixDirProvider.get().asFile.absolutePath + File.pathSeparator + System.getenv("PATH"))
        // Recreate the output every time, since leftover output from a previous run can make
        // jpackage fail or leave a stale exe mixed in. The exe jpackage generates has the
        // read-only attribute set, which makes File.delete (deleteRecursively) fail, so clear
        // the attribute before deleting
        val dest = destDir.get().asFile
        if (dest.exists()) {
            dest.walkBottomUp().forEach { file ->
                file.setWritable(true)
                check(file.delete()) { "Could not delete the previous output $file. Please try again" }
            }
        }
        commandLine(
            launcher.get().metadata.installationPath.file("bin/jpackage.exe").asFile.absolutePath,
            "--type", "exe",
            "--name", "WinUI4K Gallery",
            "--app-version", appVersion,
            "--vendor", "appkitbox",
            "--icon", iconFile.asFile.absolutePath,
            "--win-upgrade-uuid", upgradeUuid,
            "--input", inputDirProvider.get().asFile.absolutePath,
            "--main-jar", jarName.get(),
            "--main-class", mainClassName.get(),
            // Allow Panama's restricted methods, same as the run task
            "--java-options", "--enable-native-access=ALL-UNNAMED",
            "--dest", destDir.get().asFile.absolutePath,
            // Per-user install so it can be installed without admin rights
            "--win-per-user-install",
            "--win-menu",
            "--win-shortcut",
        )
    }

    // jpackage's output file name is fixed as "<name>-<version>.exe", so rename it after generation
    doLast {
        val dest = destDir.get().asFile
        val generated = dest.resolve("WinUI4K Gallery-$appVersion.exe")
        val renamed = dest.resolve("winui4k-gallery-installer.exe")
        check(generated.renameTo(renamed)) { "Could not rename $generated to $renamed" }
    }
}

// Launches the gallery on Java 8 with the JNR backend, to verify Java 8 support on real hardware (the JNR counterpart to runJna)
tasks.register<JavaExec>("runJnr") {
    description = "Launches the gallery with Java 8 + the JNR backend"
    group = "application"

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    }
    mainClass = application.mainClass
    classpath = sourceSets.main.get().runtimeClasspath

    systemProperty("winui4k.ffi", "jnr")
}
