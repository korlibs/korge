package com.soywiz.korge.gradle.targets.native

import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

object NativeBuildTypes {
    val TYPES = listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)
}

val NativeBuildType.nameType: String get() = when (this) {
    NativeBuildType.DEBUG -> "Debug"
    NativeBuildType.RELEASE -> "Release"
}
