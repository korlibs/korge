package com.soywiz.korge.gradle.targets

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*

val isWindows get() = Os.isFamily(Os.FAMILY_WINDOWS)
val isMacos get() = Os.isFamily(Os.FAMILY_MAC)
val isLinux get() = Os.isFamily(Os.FAMILY_UNIX) && !isMacos
val isArm get() = listOf("arm", "arm64", "aarch64").any { Os.isArch(it) }

//val ALL_NATIVE_TARGETS = listOf("iosX64", "iosArm64", "mingwX64", "linuxX64", "linuxArm32Hfp", "macosX64")

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
