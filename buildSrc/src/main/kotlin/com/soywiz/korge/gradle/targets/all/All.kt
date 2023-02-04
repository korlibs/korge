package com.soywiz.korge.gradle.targets.all

import com.soywiz.korlibs.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*

fun Project.rootEnableFeaturesOnAllTargets() {
    rootProject.subprojectsThis {
        enableFeaturesOnAllTargets()
    }
}

fun Project.enableFeaturesOnAllTargets() {
    if (extensions.findByType(KotlinMultiplatformExtension::class.java) != null) {
        kotlin.targets.configureEach { target ->
            target.compilations.configureEach { compilation ->
                //println("initAllTargets: $target - $compilation")
                compilation.compilerOptions.options.suppressWarnings.set(true)
                //compilation.compilerOptions.options.freeCompilerArgs.add("-Xvalue-classes")
            }
        }
    }
}
