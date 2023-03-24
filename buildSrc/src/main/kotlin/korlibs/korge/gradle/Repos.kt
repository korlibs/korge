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
        if (kotlinVersionIsDev) {
            maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
            maven { it.url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        }
        //println("kotlinVersion=$kotlinVersion")
	}
}
