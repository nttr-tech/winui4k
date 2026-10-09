plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-sample-ribbon-shell"))
}

application {
    mainClass = "com.appkitbox.winui4k.sample.ribbon.powerpoint.MainForRibbonPowerPointKt"
}
