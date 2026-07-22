import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("winui4k.kotlin-common")
    id("winui4k.fat-jar")
    application
}

// Samples target Java 8, but the runtime classpath should still include the Java 22-targeted
// winui4k-ffi-panama (at Java 8 runtime, Ffi skips it at the ServiceLoader level)
targetJvm25AtRuntime()

tasks.named<JavaExec>("run") {
    // Allow Panama's restricted methods (libraryLookup / reinterpret / upcallStub)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.named<ShadowJar>("shadowJar") {
    manifest {
        // Also enable the equivalent of the run task's --enable-native-access when launched via java -jar (JDK 24+; ignored on earlier versions)
        attributes("Enable-Native-Access" to "ALL-UNNAMED")
    }
}
