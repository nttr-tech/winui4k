import org.gradle.api.Project
import org.gradle.api.attributes.java.TargetJvmVersion

/**
 * Raises the TargetJvmVersion attribute of the given configurations to 25.
 *
 * Lets Java 8-targeted modules still include the Java 22-targeted winui4k-ffi-panama
 * on the runtime classpath (at Java 8 runtime, Ffi skips it at the ServiceLoader level).
 */
fun Project.targetJvm25AtRuntime(
    vararg configurationNames: String = arrayOf("runtimeClasspath", "testRuntimeClasspath"),
) {
    for (name in configurationNames) {
        configurations.named(name) {
            attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}
