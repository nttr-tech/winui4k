plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-all"))
}

application {
    mainClass = "com.appkitbox.winui4k.sample.formwithmiglayout.MainForFormWithMigLayoutKt"
}
