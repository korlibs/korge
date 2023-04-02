package korlibs.korge.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.*
import java.net.URI

fun Project.configureRepositories() {
    fun ArtifactRepository.config() {
        content { it.excludeGroup("Kotlin/Native") }
    }

    repositories.apply {
        mavenLocal().config()
        mavenCentral().config()
        google().config()
        maven { it.url = uri("https://plugins.gradle.org/m2/") }.config()
        if (kotlinVersionIsDev) {
            maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap") }.config()
            maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }.config()
            maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }.config()
            maven { it.url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }.config()
            maven { it.url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }.config()
            maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
            maven { it.url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        }
        //println("kotlinVersion=$kotlinVersion")
	}
}
