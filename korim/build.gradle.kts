description = "Korim: Kotlin cORoutines IMaging utilities for JVM, JS, Native and Common"

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val enableKotlinRaspberryPi: String by project
val doEnableKotlinRaspberryPi get() = enableKotlinRaspberryPi == "true"

val isWindows get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.nativeTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return when {
        isWindows -> listOfNotNull(mingwX64())
        isMacos -> listOfNotNull(macosX64(), macosArm64(), iosArm64(), iosX64())
        else -> listOfNotNull(
            linuxX64(),
            if (doEnableKotlinRaspberryPi) linuxArm32Hfp() else null,
            mingwX64(), macosX64(), macosArm64(), iosArm64(), iosX64()
        )
    }
}

if (doEnableKotlinNative) {
    kotlin {
        for (target in nativeTargets()) {
            target.compilations["main"].cinterops {
                maybeCreate("stb_image")
            }
        }
    }
}

dependencies {
	add("commonMainApi", project(":korio"))
	add("commonMainApi", project(":korma"))
    //add("commonTestApi", project(":korma-shape"))
}
