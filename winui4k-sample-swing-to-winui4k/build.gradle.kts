// Samples for doc/swing_to_winui4k.md. Swing code is paired with the same code written with WinUI4K.
// src/main/java/com/appkitbox/winui4k/swing_to_winui4k/<component>/<use case>/<use case>Swing.java / <use case>WinUI4K.java
//
// Run them one at a time (give sample the class name below swing_to_winui4k; in PowerShell, quote "-Psample=..."):
//   .\gradlew :winui4k-sample-swing-to-winui4k:run "-Psample=jbutton.click.ClickSwing"
//   .\gradlew :winui4k-sample-swing-to-winui4k:run "-Psample=jbutton.click.ClickWinUI4K"
// The working directory is the component's folder (relative paths such as icons/save.png are read from there).
plugins {
    id("winui4k.kotlin-application")
}

dependencies {
    implementation(project(":winui4k-all"))
}

// Referencing top-level script values from a lambda keeps it out of the configuration cache, so the strings are written directly in each lambda
application {
    mainClass = providers.gradleProperty("sample")
        .map { "com.appkitbox.winui4k.swing_to_winui4k.$it" }
        .orElse("com.appkitbox.winui4k.swing_to_winui4k.jbutton.click.ClickWinUI4K")
}

tasks.named<JavaExec>("run") {
    // The component's folder (e.g. .../swing_to_winui4k/jbutton for jbutton.click.ClickSwing)
    val component = providers.gradleProperty("sample").map { it.substringBefore('.') }.orElse("jbutton")
    workingDir(layout.projectDirectory.dir("src/main/java/com/appkitbox/winui4k/swing_to_winui4k").dir(component))
}
