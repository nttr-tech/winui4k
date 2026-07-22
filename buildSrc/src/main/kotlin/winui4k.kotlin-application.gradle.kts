plugins {
    id("winui4k.kotlin-common")
    application
}

// Samples target Java 8, but the runtime classpath should still include the Java 22-targeted
// winui4k-ffi-panama (at Java 8 runtime, Ffi skips it at the ServiceLoader level)
targetJvm25AtRuntime()

tasks.named<JavaExec>("run") {
    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
