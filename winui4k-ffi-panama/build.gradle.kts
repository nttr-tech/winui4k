plugins {
    id("winui4k.kotlin-library")
}

// Target Java 22, where java.lang.foreign was finalized (build with JDK 25)
winui4k {
    targetJavaVersion = 22
}

dependencies {
    implementation(project(":winui4k"))
}
