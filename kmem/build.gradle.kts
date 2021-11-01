description = "Memory utilities for Kotlin"

/*
project.ext.props = [
    "project.scm.url" : "https://github.com/korlibs/klogger",
"project.description" : "Logger system for Kotlin",
"project.license.name" : "MIT License",
"project.license.url" : "https://raw.githubusercontent.com/korlibs/klogger/master/LICENSE",
"project.author.id" : "soywiz",
"project.author.name" : "Carlos Ballesteros Velasco",
"project.author.email" : "soywiz@gmail.com",
]
 */

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val enableKotlinRaspberryPi: String by project
val doEnableKotlinRaspberryPi get() = enableKotlinRaspberryPi == "true"

val isWindows get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)

dependencies {
    //add("androidMainApi", "com.implimentz:unsafe:0.0.6")
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.nativeTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return when {
        isWindows -> listOf(mingwX64())
        isMacos -> listOf(macosX64(), macosArm64(), iosArm64(), iosX64())
        else -> listOfNotNull(
            linuxX64(),
            if (doEnableKotlinRaspberryPi) linuxArm32Hfp() else null,
            mingwX64(), macosX64(), macosArm64(), iosArm64(), iosX64()
        )
    }
}

kotlin {
    for (target in nativeTargets() ) {
        target.compilations["main"].cinterops {
            maybeCreate("fastmem")
        }
    }
}
