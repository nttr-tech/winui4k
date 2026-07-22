// The backend that also works on Java 8
plugins {
    id("winui4k.kotlin-library")
}

dependencies {
    implementation(project(":winui4k"))
    implementation(libs.jna) // Needs 5.12+ for Memory.close()
}
