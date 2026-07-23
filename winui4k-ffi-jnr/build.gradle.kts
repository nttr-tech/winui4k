// The backend that also works on Java 8
plugins {
    id("winui4k.kotlin-library")
}

description = "WinUI4K FFI backend using JNR-FFI for Java 8+"

dependencies {
    implementation(project(":winui4k"))
    // Uses the bundled low-level layer jffi (com.kenai.jffi) directly. Runtime-determined function
    // pointers like COM vtables can't be called through jnr-ffi's high-level API (LibraryLoader)
    implementation(libs.jnr.ffi)
}
