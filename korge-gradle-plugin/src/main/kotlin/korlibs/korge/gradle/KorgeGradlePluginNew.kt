package korlibs.korge.gradle

import com.soywiz.kproject.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.kotlin.plugin.*
import org.gradle.api.*

abstract class KorgeGradleAbstractPlugin(val projectType: ProjectType) : Plugin<Project> {
    override fun apply(project: Project) {
        project.configureAutoVersions()
        project.configureBuildScriptClasspathTasks()
        KorgeGradleApply(project, projectType).apply(includeIndirectAndroid = true)
        project.plugins.applyOnce(KorgeKotlinCompilerPlugin::class.java)
    }
}

open class KorgeGradlePlugin : KorgeGradleAbstractPlugin(projectType = ProjectType.EXECUTABLE)
open class KorgeLibraryGradlePlugin : KorgeGradleAbstractPlugin(projectType = ProjectType.LIBRARY)
