package korlibs.modules

import org.gradle.api.*
import korlibs.korge.gradle.targets.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

// Only mac has ios/tvos targets
val Project.supportKotlinNative: Boolean get() = isMacos && System.getenv("DISABLE_KOTLIN_NATIVE") != "true"

val Project.doEnableKotlinNative: Boolean get() = supportKotlinNative
val Project.doEnableKotlinAndroid: Boolean get() = rootProject.findProperty("enableKotlinAndroid") == "true" && System.getenv("DISABLE_KOTLIN_ANDROID") != "true"
val Project.doEnableKotlinMobile: Boolean get() = doEnableKotlinNative && rootProject.findProperty("enableKotlinMobile") == "true"
val Project.doEnableKotlinMobileTvos: Boolean get() = doEnableKotlinMobile && rootProject.findProperty("enableKotlinMobileTvos") == "true"

val Project.hasAndroid get() = extensions.findByName("android") != null

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.mobileTargets(project: Project): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    if (!project.doEnableKotlinMobile) return listOf()

    val out = arrayListOf<KotlinNativeTarget>()
    out.addAll(listOf(iosArm64(), iosX64(), iosSimulatorArm64()))
    if (project.doEnableKotlinMobileTvos) {
        out.addAll(listOf(tvosArm64(), tvosX64(), tvosSimulatorArm64()))
    }
    return out
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.allNativeTargets(project: Project): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return mobileTargets(project)
}
