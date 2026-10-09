plugins {
    id("winui4k.kotlin-common")
    `java-library`
}

description = "Shared window frame for the ribbon demo apps (Word / Excel / PowerPoint / CAD / Tools)"

dependencies {
    api(project(":winui4k-all"))
}
