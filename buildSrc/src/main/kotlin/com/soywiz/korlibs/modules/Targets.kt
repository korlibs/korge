package com.soywiz.korlibs.modules

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import com.soywiz.korge.gradle.targets.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

val Project.supportKotlinNative: Boolean get() {
    // Linux and Windows ARM hosts doesn't have K/N toolchains
    if ((isLinux || isWindows) && isArm) return false
    return true
}

val Project.doEnableKotlinNative: Boolean get() = supportKotlinNative && rootProject.findProperty("enableKotlinNative") == "true"
val Project.doEnableKotlinAndroid: Boolean get() = rootProject.findProperty("enableKotlinAndroid") == "true"
val Project.doEnableKotlinMobile: Boolean get() = doEnableKotlinNative && rootProject.findProperty("enableKotlinMobile") == "true"
val Project.doEnableKotlinMobileTvos: Boolean get() = doEnableKotlinMobile && rootProject.findProperty("enableKotlinMobileTvos") == "true"
val Project.doEnableKotlinMobileWatchos: Boolean get() = doEnableKotlinMobile && rootProject.findProperty("enableKotlinMobileWatchos") == "true"
val Project.doEnableKotlinRaspberryPi: Boolean get() = doEnableKotlinNative && rootProject.findProperty("enableKotlinRaspberryPi") == "true"

val Project.hasAndroid get() = extensions.findByName("android") != null

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.currentPlatformNativeTarget(project: Project): org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget {
    return when {
        isWindows -> mingwX64()
        isMacos -> if (isArm) macosArm64() else macosX64()
        else -> linuxX64()
    }
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.nativeTargets(project: Project): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return when {
        isWindows -> listOfNotNull(mingwX64()) + (when {
            inCI -> emptyList()
            else -> listOfNotNull(
                linuxX64(),
            )
        })
        isMacos -> listOfNotNull(
            macosX64(), macosArm64(),
        ) + (when {
            inCI -> emptyList()
            else -> listOfNotNull(
                linuxX64(),
                mingwX64(),
                if (project.doEnableKotlinRaspberryPi) linuxArm32Hfp() else null,
            )
        })
        else -> listOfNotNull(
            linuxX64(),
            mingwX64(),
            macosX64(), macosArm64(),
            if (project.doEnableKotlinRaspberryPi) linuxArm32Hfp() else null
        )
    }
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.mobileTargets(project: Project): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    if (!project.doEnableKotlinMobile) return listOf()

    val out = arrayListOf<KotlinNativeTarget>()
    out.addAll(listOf(iosX64(), iosArm32(), iosArm64(), iosSimulatorArm64()))
    if (project.doEnableKotlinMobileWatchos) {
        out.addAll(listOf(watchosX86(), watchosX64(), watchosArm32(), watchosArm64(), watchosSimulatorArm64()))
    }
    if (project.doEnableKotlinMobileTvos) {
        out.addAll(listOf(tvosX64(), tvosArm64(), tvosSimulatorArm64()))
    }
    return out
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.allNativeTargets(project: Project): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return nativeTargets(project) + mobileTargets(project)
}
