// Aggregate module that pulls in every module other than the samples in one shot
// (the API is exposed via api, and the FFI backends are propagated onto the runtime classpath via runtimeOnly)
plugins {
    id("winui4k.kotlin-library")
    id("winui4k.fat-jar")
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

tasks.shadowJar {
    // Avoid the duplication between the module name (winui4k-all) and the classifier (all); use winui4k-<version>-all.jar instead
    archiveBaseName = "winui4k"
}
