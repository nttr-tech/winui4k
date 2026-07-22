import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// Produce a single fatJar (<baseName>-<version>-all.jar) combining all dependencies
// (kotlin-stdlib / coroutines / miglayout / JNA / JNR, etc.)
plugins {
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    // The three FFI backends have same-named META-INF/services files, so merging is required
    // (with the default EXCLUDE strategy, duplicate files get discarded before reaching the transformer)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    // Module descriptors from dependency libraries become invalid in a fatJar, so exclude them
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
    manifest {
        // Enable version-specific classes from multi-release JARs like kotlin-stdlib
        attributes("Multi-Release" to "true")
    }
}
