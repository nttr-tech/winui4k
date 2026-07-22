import org.gradle.api.provider.Property

/** Extension for modules to adjust the winui4k convention plugin. */
abstract class Winui4kExtension {
    /** The Java version to compile against. Defaults to 8 (winui4k-ffi-panama alone uses 22). */
    abstract val targetJavaVersion: Property<Int>
}
