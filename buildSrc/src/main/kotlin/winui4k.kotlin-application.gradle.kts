import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("winui4k.kotlin-common")
    id("winui4k.fat-jar")
    application
}

// Samples target Java 8, but the runtime classpath should still include the Java 22-targeted
// winui4k-ffi-panama (at Java 8 runtime, Ffi skips it at the ServiceLoader level)
targetJvm25AtRuntime()

the<Winui4kExtension>().runFromRoot.convention(false)

tasks.named<JavaExec>("run") {
    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")

    // The root's `gradlew run` launches every app module's run task by name match, so run it
    // only for the module with runFromRoot = true (Gallery), or when explicitly path-specified
    // (e.g. `gradlew :module:run`).
    // Referencing the script object directly from the onlyIf lambda would break configuration
    // cache, so capture only the Property and the Boolean locally
    val runFromRoot = project.the<Winui4kExtension>().runFromRoot
    val requestedExplicitly = gradle.startParameter.taskNames.any { it.contains(project.name) }
    onlyIf("runs only when runFromRoot = true, or explicitly specified by task path") {
        runFromRoot.get() || requestedExplicitly
    }
}

tasks.named<ShadowJar>("shadowJar") {
    manifest {
        // Also enable the equivalent of the run task's --enable-native-access when launched via java -jar (JDK 24+; ignored on earlier versions)
        attributes("Enable-Native-Access" to "ALL-UNNAMED")
    }
}
