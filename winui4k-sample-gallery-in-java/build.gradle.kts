plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-all"))
}

application {
    // Unlike the Kotlin version (MainForGalleryKt), the Java version uses the static main of the MainForGallery class
    mainClass = "com.appkitbox.winui4k.sample.gallery.MainForGallery"
}
