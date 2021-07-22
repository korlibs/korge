val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val isWindows get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)

dependencies {
    //add("androidMainApi", "com.implimentz:unsafe:0.0.6")
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.nativeTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return when {
        isWindows -> listOf(mingwX64())
        isMacos -> listOf(macosX64(), iosArm64(), iosX64())
        else -> listOf(linuxX64(), linuxArm32Hfp(), mingwX64(), macosX64(), iosArm64(), iosX64())
    }
}

kotlin {
    for (target in nativeTargets() ) {
        target.compilations["main"].cinterops {
            maybeCreate("fastmem")
        }
    }

}
