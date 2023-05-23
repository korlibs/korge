package korlibs.korge.gradle.targets.all

import korlibs.*
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
                if (project.findProperty("enableMFVC") == "true") {
                    add("-Xvalue-classes")
                    add("-Xskip-prerelease-check")
                }
                add("-Xno-param-assertions")
                add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
                if (target.name == "mingwX64") {
                    add("-Xpartial-linkage=disable") // https://youtrack.jetbrains.com/issue/KT-58837/Illegal-char-at-index-0-org.jetbrains.kotlinxatomicfu-cinterop-interop-CTypeDefinitions#focus=Comments-27-7362451.0-0
                }
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
