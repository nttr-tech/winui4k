// Aggregate module that pulls in every module other than the samples in one shot
// (the API is exposed via api, and the FFI backends are propagated onto the runtime classpath via runtimeOnly)
plugins {
    id("winui4k.kotlin-library")
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":winui4k"))
    api(project(":winui4k-extension-coroutines"))
    api(project(":winui4k-extension-miglayout"))
    runtimeOnly(project(":winui4k-ffi-panama"))
    runtimeOnly(project(":winui4k-ffi-jna"))
    runtimeOnly(project(":winui4k-ffi-jnr"))
}

// This module targets Java 8, but its runtime classpath should still include winui4k-ffi-panama
// (which targets Java 22) — on a Java 8 run, Ffi skips it at the ServiceLoader level
targetJvm25AtRuntime()

// Produce a single fatJar (winui4k-<version>-all.jar) combining all modules + external
// dependencies (kotlin-stdlib / coroutines / miglayout / JNA / JNR)
tasks.shadowJar {
    // Avoid the duplication between the module name (winui4k-all) and the classifier (all); use winui4k-<version>-all.jar instead
    archiveBaseName = "winui4k"
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
