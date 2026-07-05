package korlibs.korge.gradle

import groovy.lang.GroovySystem
import korlibs.korge.gradle.util.createThis
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

object KorgeVersionsTask {
    fun registerShowKorgeVersions(project: Project) {
        project.tasks.createThis<Task>("showKorgeVersions") {
            doLast {
                println("Build-time:")
                for ((key, value) in mapOf(
                    "os.name" to System.getProperty("os.name"),
                    "os.version" to System.getProperty("os.version"),
                    "java.vendor" to System.getProperty("java.vendor"),
                    "java.version" to System.getProperty("java.version"),
                    "gradle.version" to GradleVersion.current(),
                    "groovy.version" to GroovySystem.getVersion(),
                    "kotlin.runtime.version" to KotlinVersion.CURRENT,
                    "kotlin.gradle.plugin.version" to getKotlinPluginVersion(logger),
                )) {
                    println(" - $key: $value")
                }
            }
        }
    }
}
