package korlibs.korge.gradle.targets

import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

// Only mac has ios/tvos targets but since CI exports multiplatform on linux
val supportKotlinNative: Boolean get() {
    // Linux and Windows ARM hosts doesn't have K/N toolchains
    if (isArm && (isLinux || isWindows)) return false
    // We can also try to disable it manually
    if (System.getenv("DISABLE_KOTLIN_NATIVE") == "true") return false
    // On Mac, CI or when FORCE_ENABLE_KOTLIN_NATIVE=true, let's enable it
    return true
}

val isWindows get() = Os.isFamily(Os.FAMILY_WINDOWS)
val isMacos get() = Os.isFamily(Os.FAMILY_MAC)
val isLinux get() = Os.isFamily(Os.FAMILY_UNIX) && !isMacos
val isArm get() = listOf("arm", "arm64", "aarch64").any { Os.isArch(it) }
val inCI: Boolean get() = !System.getenv("CI").isNullOrBlank() || !System.getProperty("CI").isNullOrBlank()

val KotlinTarget.isIos get() = name.startsWith("ios")
val KotlinTarget.isTvos get() = name.startsWith("tvos")
val KotlinTarget.isLinux get() = name.startsWith("linux")
val KotlinTarget.isMingw get() = name.startsWith("mingw")
val KotlinTarget.isMacos get() = name.startsWith("macos")
