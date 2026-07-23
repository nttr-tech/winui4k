plugins {
    id("winui4k.kotlin-common")
    `java-library`
    `maven-publish`
    signing
    // For publishing releases to Maven Central (Central Portal Publisher API).
    // The nmcpAggregation block in the root build.gradle.kts aggregates this module's publication
    id("com.gradleup.nmcp")
}

// ---------------------------------------------------------------------------
// Publishing configuration for Maven repositories
// (For now this only publishes to the Maven Central SNAPSHOT repository as practice.
//  Publishing to the real Maven Central additionally requires GPG signing and
//  Central Publisher API support)
// ---------------------------------------------------------------------------

// java.withSourcesJar() would also include resources (the winui4k native DLLs),
// so define our own sources jar that contains only the Kotlin/Java sources
val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier = "sources"
    from(kotlin.sourceSets.named("main").map { it.kotlin })
}

// Maven Central requires a javadoc jar to exist. Javadoc can't be generated for Kotlin,
// so publish an empty jar as is customary (replace with Dokka if it becomes necessary)
val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier = "javadoc"
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name = project.name
                description = providers.provider { project.description ?: "WinUI4K is a Kotlin library for building WinUI applications" }
                url = "https://github.com/nttr-tech/winui4k"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "hisano"
                        name = "Koji Hisano"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/nttr-tech/winui4k.git"
                    developerConnection = "scm:git:https://github.com/nttr-tech/winui4k.git"
                    url = "https://github.com/nttr-tech/winui4k"
                }
            }
        }
    }
    repositories {
        maven {
            name = "centralSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials {
                // User token generated on the Central Portal (central.sonatype.com)
                username = providers.environmentVariable("CENTRAL_USERNAME").orNull
                password = providers.environmentVariable("CENTRAL_PASSWORD").orNull
            }
        }
    }
}

// GPG signing for Maven Central publishing (a hard requirement for releases).
// The key is passed via environment variables; signing is skipped for local builds
// without them and for SNAPSHOT publishing
val signingKey = providers.environmentVariable("SIGNING_KEY")
if (signingKey.isPresent) {
    signing {
        useInMemoryPgpKeys(signingKey.get(), providers.environmentVariable("SIGNING_PASSWORD").orNull)
        sign(publishing.publications)
    }
}

// The SNAPSHOT repository rejects release versions, so detect accidental publishing before it runs
tasks.withType<PublishToMavenRepository>().configureEach {
    val projectVersion = version.toString()
    doFirst {
        val publishTask = this as PublishToMavenRepository
        if (publishTask.repository.name == "centralSnapshots") {
            check(projectVersion.endsWith("-SNAPSHOT")) {
                "Only versions ending in -SNAPSHOT can be published to the SNAPSHOT repository (current: $projectVersion)"
            }
        }
    }
}
