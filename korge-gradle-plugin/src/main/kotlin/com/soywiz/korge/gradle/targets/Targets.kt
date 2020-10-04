package com.soywiz.korge.gradle.targets

import org.apache.tools.ant.taskdefs.condition.Os

val isWindows get() = Os.isFamily(Os.FAMILY_WINDOWS)
val isMacos get() = Os.isFamily(Os.FAMILY_MAC)
val isLinux get() = Os.isFamily(Os.FAMILY_UNIX) && !isMacos

val ALL_NATIVE_TARGETS = listOf("iosX64", "iosArm64", "mingwX64", "linuxX64", "macosX64")
