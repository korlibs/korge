package com.soywiz.korlibs.modules

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

val Project.supportKotlinNative: Boolean get() {
    // Linux and Windows ARM hosts doesn't have K/N toolchains
    if ((isLinux || isWindows) && isArm) return false
    return false
}

val Project.doEnableKotlinNative: Boolean get() = supportKotlinNative && rootProject.findProperty("enableKotlinNative") == "true"
val Project.doEnableKotlinAndroid: Boolean get() = rootProject.findProperty("enableKotlinAndroid") == "true"
val Project.doEnableKotlinMobile: Boolean get() = doEnableKotlinNative && rootProject.findProperty("enableKotlinMobile") == "true"
val Project.doEnableKotlinMobileTvos: Boolean get() = doEnableKotlinMobile && rootProject.findProperty("enableKotlinMobileTvos") == "true"
val Project.doEnableKotlinMobileWatchos: Boolean get() = doEnableKotlinMobile && rootProject.findProperty("enableKotlinMobileWatchos") == "true"
val Project.doEnableKotlinRaspberryPi: Boolean get() = doEnableKotlinNative && rootProject.findProperty("enableKotlinRaspberryPi") == "true"

val KotlinTarget.isX64: Boolean get() = this.name.endsWith("X64")
val KotlinTarget.isX86: Boolean get() = this.name.endsWith("X86")
val KotlinTarget.isArm32: Boolean get() = this.name.endsWith("Arm32")
val KotlinTarget.isArm64: Boolean get() = this.name.endsWith("Arm64") || !this.name.endsWith("SimulatorArm64")
val KotlinTarget.isArm32Hfp: Boolean get() = this.name.endsWith("Arm32Hfp")
val KotlinTarget.isSimulatorArm64: Boolean get() = this.name.endsWith("SimulatorArm64")

val KotlinTarget.isLinuxX64: Boolean get() = this.name == "linuxX64"
val KotlinTarget.isLinuxArm64: Boolean get() = this.name == "linuxArm64"
val KotlinTarget.isLinuxArm32Hfp: Boolean get() = this.name == "linuxArm32Hfp" && project.doEnableKotlinRaspberryPi
val KotlinTarget.isLinux: Boolean get() = isLinuxX64 || isLinuxArm32Hfp || isLinuxArm64
val KotlinTarget.isWin: Boolean get() = this.name == "mingwX64" || this.name == "mingwArm64"
val KotlinTarget.isMacosX64: Boolean get() = this.name == "macosX64"
val KotlinTarget.isMacosArm64: Boolean get() = this.name == "macosArm64"
val KotlinTarget.isMacos: Boolean get() = isMacosX64 || isMacosArm64
val KotlinTarget.isIosArm32: Boolean get() = this.name == "iosArm32"
val KotlinTarget.isIosArm64: Boolean get() = this.name == "iosArm64"
val KotlinTarget.isIosX64: Boolean get() = this.name == "iosX64"
val KotlinTarget.isIosSimulatorArm64: Boolean get() = this.name == "iosSimulatorArm64"
val KotlinTarget.isIos: Boolean get() = isIosArm32 || isIosArm64 || isIosX64 || isIosSimulatorArm64
val KotlinTarget.isWatchosX86: Boolean get() = this.name == "watchosX86"
val KotlinTarget.isWatchosArm32: Boolean get() = this.name == "watchosArm32"
val KotlinTarget.isWatchosArm64: Boolean get() = this.name == "watchosArm64"
val KotlinTarget.isWatchos: Boolean get() = isWatchosX86 || isWatchosArm32 || isWatchosArm64
val KotlinTarget.isTvosX64: Boolean get() = this.name == "tvosX64"
val KotlinTarget.isTvosArm64: Boolean get() = this.name == "tvosArm64"
val KotlinTarget.isTvos: Boolean get() = isTvosX64 || isTvosArm64
val KotlinTarget.isDesktop: Boolean get() = isWin || isLinux || isMacos
val KotlinTarget.isPosix: Boolean get() = this is KotlinNativeTarget && !this.isWin
val KotlinTarget.isApple: Boolean get() = isMacos || isIos || isWatchos || isTvos

val isWindows: Boolean get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos: Boolean get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)
val isArm: Boolean get() = listOf("arm", "arm64", "aarch64").any { org.apache.tools.ant.taskdefs.condition.Os.isArch(it) }
val isLinux: Boolean get() = !isWindows && !isMacos
val inCI: Boolean get() = System.getProperty("CI") == "true"

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
