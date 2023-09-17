package korlibs.korge.gradle.targets.all

import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.*

val Project.korgeGradlePluginResources: File get() = File(rootProject.projectDir, "buildSrc/src/main/resources")

fun Project.rootEnableFeaturesOnAllTargets() {
    rootProject.subprojectsThis {
        enableFeaturesOnAllTargets()
    }
}

object AddFreeCompilerArgs {
    @JvmStatic
    fun addFreeCompilerArgs(project: Project, target: KotlinTarget) {
        target.compilations.configureEach { compilation ->
            val options = compilation.compilerOptions.options
            options.suppressWarnings.set(true)
            options.freeCompilerArgs.apply {
                add("-Xskip-prerelease-check")
                if (project.findProperty("enableMFVC") == "true") add("-Xvalue-classes")
                if (target.name == "android" || target.name == "jvm") add("-Xno-param-assertions")
                add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}

fun Project.enableFeaturesOnAllTargets() {
    fun KotlinTarget.configureTarget() {
        AddFreeCompilerArgs.addFreeCompilerArgs(project, this)
    }

    extensions.findByType(KotlinSingleTargetExtension::class.java)?.also {
        it.target.configureTarget()
    }

    extensions.findByType(KotlinMultiplatformExtension::class.java)?.also {
        it.targets.configureEach { target ->
            target.configureTarget()
        }
    }
}
