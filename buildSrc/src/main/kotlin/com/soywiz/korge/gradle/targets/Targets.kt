package com.soywiz.korge.gradle.targets

import com.soywiz.korlibs.modules.doEnableKotlinRaspberryPi
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

val supportKotlinNative: Boolean get() {
    // Linux and Windows ARM hosts doesn't have K/N toolchains
    if ((isLinux || isWindows) && isArm) return false
    return true
}

val isWindows get() = Os.isFamily(Os.FAMILY_WINDOWS)
val isMacos get() = Os.isFamily(Os.FAMILY_MAC)
val isLinux get() = Os.isFamily(Os.FAMILY_UNIX) && !isMacos
val isArm get() = listOf("arm", "arm64", "aarch64").any { Os.isArch(it) }
val inCI: Boolean get() = !System.getenv("CI").isNullOrBlank() || !System.getProperty("CI").isNullOrBlank()

//val ALL_NATIVE_TARGETS = listOf("iosX64", "iosArm64", "mingwX64", "linuxX64", "linuxArm32Hfp", "macosX64", "macosArm64")

val KotlinTarget.isJvm get() = name in setOf("jvm")
val KotlinTarget.isJs get() = name in setOf("js")
val KotlinTarget.isAndroid get() = name in setOf("android")
val KotlinTarget.isJvmOrAndroid get() = isJvm || isAndroid
val KotlinTarget.isIos get() = name.startsWith("ios")
val KotlinTarget.isTvos get() = name.startsWith("tvos")
val KotlinTarget.isWatchos get() = name.startsWith("watchos")
val KotlinTarget.isMacos get() = name.startsWith("macos")
val KotlinTarget.isLinux get() = name.startsWith("linux")
val KotlinTarget.isMingw get() = name.startsWith("mingw")
val KotlinTarget.isNativeDesktop get() = isMingw || isLinux || isMacos
val KotlinTarget.isNativeMobile get() = isIos || isTvos || isWatchos
val KotlinTarget.isApple get() = isIos || isTvos || isWatchos || isMacos
val KotlinTarget.isIosTvos get() = isIos || isTvos
val KotlinTarget.isIosWatchos get() = isIos || isWatchos
val KotlinTarget.isIosTvosWatchos get() = isIos || isTvos || isWatchos
val KotlinTarget.isNativePosix get() = isApple || isLinux
val KotlinTarget.isNative get() = isNativeDesktop || isNativeMobile

val KotlinTarget.isX64: Boolean get() = this.name.endsWith("X64")
val KotlinTarget.isX86: Boolean get() = this.name.endsWith("X86")
val KotlinTarget.isArm32: Boolean get() = this.name.endsWith("Arm32")
val KotlinTarget.isArm64: Boolean get() = this.name.endsWith("Arm64") || !this.name.endsWith("SimulatorArm64")
val KotlinTarget.isArm32Hfp: Boolean get() = this.name.endsWith("Arm32Hfp")
val KotlinTarget.isSimulatorArm64: Boolean get() = this.name.endsWith("SimulatorArm64")

val KotlinTarget.isLinuxX64: Boolean get() = this.name == "linuxX64"
val KotlinTarget.isLinuxArm64: Boolean get() = this.name == "linuxArm64"
val KotlinTarget.isLinuxArm32Hfp: Boolean get() = this.name == "linuxArm32Hfp" && project.doEnableKotlinRaspberryPi
//val KotlinTarget.isLinux: Boolean get() = isLinuxX64 || isLinuxArm32Hfp || isLinuxArm64
val KotlinTarget.isWin: Boolean get() = this.name == "mingwX64" || this.name == "mingwArm64"
val KotlinTarget.isMacosX64: Boolean get() = this.name == "macosX64"
val KotlinTarget.isMacosArm64: Boolean get() = this.name == "macosArm64"
//val KotlinTarget.isMacos: Boolean get() = isMacosX64 || isMacosArm64
val KotlinTarget.isIosArm32: Boolean get() = this.name == "iosArm32"
val KotlinTarget.isIosArm64: Boolean get() = this.name == "iosArm64"
val KotlinTarget.isIosX64: Boolean get() = this.name == "iosX64"
val KotlinTarget.isIosSimulatorArm64: Boolean get() = this.name == "iosSimulatorArm64"
//val KotlinTarget.isIos: Boolean get() = isIosArm32 || isIosArm64 || isIosX64 || isIosSimulatorArm64
val KotlinTarget.isWatchosX86: Boolean get() = this.name == "watchosX86"
val KotlinTarget.isWatchosArm32: Boolean get() = this.name == "watchosArm32"
val KotlinTarget.isWatchosArm64: Boolean get() = this.name == "watchosArm64"
//val KotlinTarget.isWatchos: Boolean get() = isWatchosX86 || isWatchosArm32 || isWatchosArm64
val KotlinTarget.isTvosX64: Boolean get() = this.name == "tvosX64"
val KotlinTarget.isTvosArm64: Boolean get() = this.name == "tvosArm64"
//val KotlinTarget.isTvos: Boolean get() = isTvosX64 || isTvosArm64
val KotlinTarget.isDesktop: Boolean get() = isWin || isLinux || isMacos
val KotlinTarget.isPosix: Boolean get() = this is KotlinNativeTarget && !this.isWin
//val KotlinTarget.isApple: Boolean get() = isMacos || isIos || isWatchos || isTvos

fun NamedDomainObjectContainer<KotlinSourceSet>.createPairSourceSet(name: String, vararg dependencies: PairSourceSet, block: KotlinSourceSet.(test: Boolean) -> Unit = { }): PairSourceSet {
    val main = maybeCreate("${name}Main").apply { block(false) }
    val test = maybeCreate("${name}Test").apply { block(true) }
    return PairSourceSet(main, test).also {
        for (dependency in dependencies) {
            it.dependsOn(dependency)
        }
    }
}

data class PairSourceSet(val main: KotlinSourceSet, val test: KotlinSourceSet) {
    fun get(test: Boolean) = if (test) this.test else this.main
    fun dependsOn(other: PairSourceSet) {
        main.dependsOn(other.main)
        test.dependsOn(other.test)
    }
}
