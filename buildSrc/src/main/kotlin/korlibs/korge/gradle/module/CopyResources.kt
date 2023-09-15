package korlibs.korge.gradle.module

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.desktop.*
import korlibs.korge.gradle.targets.native.*
import korlibs.korge.gradle.targets.windows.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

/*
fun Project.createCopyToExecutableTarget(target: String) {
    for (build in KNTargetWithBuildType.buildList(project, target)) {
        val copyTask = project.tasks.createThis<Copy>(build.copyTaskName) {
            run {
                val processResourcesTaskName = getProcessResourcesTaskName(build.target, build.compilation.name)
                dependsOn(processResourcesTaskName)
                afterEvaluate {
                    afterEvaluate {
                        afterEvaluate {
                            val korgeGenerateResourcesTask = tasks.findByName(processResourcesTaskName) as? Copy?
                            //korgeGenerateResourcesTask?.korgeGeneratedFolder?.let { from(it) }
                            from(korgeGenerateResourcesTask?.outputs)
                        }
                    }
                }
            }
            run {
                val processResourcesTaskName = getKorgeProcessResourcesTaskName(build.target, build.compilation.name)
                dependsOn(processResourcesTaskName)
                afterEvaluate {
                    afterEvaluate {
                        afterEvaluate {
                            val korgeGenerateResourcesTask =
                                tasks.findByName(processResourcesTaskName) as? KorgeGenerateResourcesTask?
                            //korgeGenerateResourcesTask?.korgeGeneratedFolder?.let { from(it) }
                            korgeGenerateResourcesTask?.addToCopySpec(this@createThis, addFrom = false)
                        }
                    }
                }
            }
            dependsOn(build.compilation.compileKotlinTask)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            for (sourceSet in project.gkotlin.sourceSets) {
                from(sourceSet.resources)
            }
            //println("executableFile.parentFile: ${executableFile.parentFile}")
            into(build.executableFile.parentFile)
        }

        afterEvaluate {
            try {
                val linkTask = build.compilation.getLinkTask(NativeOutputKind.EXECUTABLE, build.buildType, project)
                linkTask.dependsOn(copyTask)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
*/
