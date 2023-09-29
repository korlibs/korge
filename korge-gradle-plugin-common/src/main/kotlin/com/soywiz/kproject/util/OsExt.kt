package com.soywiz.kproject.util

import org.apache.tools.ant.taskdefs.condition.*

val isWindows: Boolean get() = Os.isFamily(Os.FAMILY_WINDOWS)
val isMacos: Boolean get() = Os.isFamily(Os.FAMILY_MAC)
val isLinux: Boolean get() = Os.isFamily(Os.FAMILY_UNIX) && !isMacos
val isArm: Boolean get() = setOf("arm", "arm64", "aarch64").any { Os.isArch(it) }
val inCI: Boolean get() = !System.getenv("CI").isNullOrBlank() || !System.getProperty("CI").isNullOrBlank()

val isWindowsArm: Boolean get() = isWindows && isArm
val isLinuxArm: Boolean get() = isLinux && isArm
val isWindowsOrLinuxArm: Boolean get() = (isWindows || isLinux) && isArm
