package korlibs.korge.gradle.targets.all

import korlibs.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

fun Project.rootEnableFeaturesOnAllTargets() {
    rootProject.subprojectsThis {
        enableFeaturesOnAllTargets()
    }
}

fun Project.enableFeaturesOnAllTargets() {
    fun KotlinTarget.configureTarget() {
        val target = this
        target.compilations.configureEach { compilation ->
            //println("initAllTargets: $target - $compilation")
            val options = compilation.compilerOptions.options
            options.suppressWarnings.set(true)
            options.freeCompilerArgs.apply {
                add("-Xvalue-classes")
                add("-Xskip-prerelease-check")
            }
        }
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