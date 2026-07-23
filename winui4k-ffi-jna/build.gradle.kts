// The backend that also works on Java 8
plugins {
    id("winui4k.kotlin-library")
}

description = "WinUI4K FFI backend using JNA for Java 8+"

dependencies {
    implementation(project(":winui4k"))
    implementation(libs.jna) // Needs 5.12+ for Memory.close()
}
