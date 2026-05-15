package korlibs.korge.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.*
import java.net.URI

fun Project.configureRepositories() {
    fun ArtifactRepository.config() {
        content { excludeGroup("Kotlin/Native") }
    }

    repositories.apply {
        mavenLocal().config()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }.config()
        mavenCentral().config()
        google().config()
        maven { url = uri("https://plugins.gradle.org/m2/") }.config()
	}
}
