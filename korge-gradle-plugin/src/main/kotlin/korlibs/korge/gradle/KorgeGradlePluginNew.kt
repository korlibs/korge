package korlibs.korge.gradle

import korlibs.korge.gradle.targets.*
import org.gradle.api.*

abstract class KorgeGradleAbstractPlugin(val projectType: ProjectType) : Plugin<Project> {
    override fun apply(project: Project) {
        project.configureAutoVersions()
        project.configureBuildScriptClasspathTasks()
        KorgeGradleApply(project, projectType).apply(includeIndirectAndroid = true)
    }
}

open class KorgeGradlePlugin : KorgeGradleAbstractPlugin(projectType = ProjectType.EXECUTABLE)
open class KorgeLibraryGradlePlugin : KorgeGradleAbstractPlugin(projectType = ProjectType.LIBRARY)
