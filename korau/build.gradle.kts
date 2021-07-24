description = "Portable Audio library for Kotlin"

val jnaVersion: String by project

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val enableKotlinRaspberryPi: String by project
val doEnableKotlinRaspberryPi get() = enableKotlinRaspberryPi == "true"

val isWindows get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.nativeTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return when {
        isWindows -> listOf(mingwX64())
        isMacos -> listOf(macosX64(), iosArm64(), iosX64())
        else -> listOfNotNull(
            linuxX64(),
            if (doEnableKotlinRaspberryPi) linuxArm32Hfp() else null,
            mingwX64(), macosX64(), iosArm64(), iosX64()
        )
    }
}

if (doEnableKotlinNative) {
    kotlin {
        for (target in nativeTargets()) {
            target.compilations["main"].cinterops {
                if (target.name == "mingwX64") maybeCreate("win32_winmm")
                if (target.name == "linuxX64") maybeCreate("linux_OpenAL")
                if (target.name == "linuxArm32Hfp") maybeCreate("linux_rpi_OpenAL")
                maybeCreate("minimp3")
                maybeCreate("stb_vorbis")
            }
        }

    }
}

dependencies {
    add("commonMainApi", project(":korio"))
    add("jvmMainApi", "net.java.dev.jna:jna:$jnaVersion")
    add("jvmMainApi", "net.java.dev.jna:jna-platform:$jnaVersion")
}

/*
import org.apache.tools.ant.taskdefs.condition.Os

apply plugin: com.soywiz.korlibs.KorlibsPlugin

korlibs {
    exposeVersion()
    def nativeTargets = [
        "mingwX64", "macosX64",
        *(korlibs.linuxEnabled ? ["linuxX64"] : []),
        "iosX64", "iosArm64",
        "tvosX64", "tvosArm64",
        "watchosX86", "watchosArm32", "watchosArm64"
    ]
    dependencyCInterops("minimp3",    nativeTargets)
    dependencyCInterops("stb_vorbis", nativeTargets)
    dependencyCInterops("win32_winmm", ["mingwX64"])
    if (korlibs.linuxEnabled) {
        dependencyCInterops("linux_OpenAL", ["linuxX64"])
    }
    dependencyCInterops("mac_OpenAL", ["macosX64"])

    if (System.getProperty("idea.version") != null) {
        if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)) {
            dependencyCInterops("minimp3", ["nativeCommon", "nativePosix"])
            dependencyCInterops("stb_vorbis", ["nativeCommon", "nativePosix"])
            dependencyCInterops("AVFoundation", ["nativeCommon", "nativePosix"])
            dependencyCInterops("mac_OpenAL", ["nativeCommon", "nativePosix"])
        }
    }
}

kotlin {
    if (korlibs.linuxEnabled) {
        linuxX64 {
            compilations.main {
                cinterops {
                    linux_OpenAL {
                        // warning: -linker-option(s)/-linkerOpts/-lopt option is not supported by cinterop. Please add linker options to .def file or binary compilation instead.

                        //print(it)
                        //def linuxFolder = new File(rootDir, "nlib/linuxX64")
                        //compilerOpts("-I/usr/include", "-I/usr/include/x86_64-linux-gnu/", "-I${new File(linuxFolder, "include").absolutePath}")
                        //linkerOpts("-L/usr/X11R6/lib", "-L/usr/lib/x86_64-linux-gnu", "-L${new File(linuxFolder, "lib/x86_64-linux-gnu").absolutePath}", "-lopenal")
                    }
                }
            }
        }
    }
}

task runSample(type: JavaExec) {
    classpath = kotlin.targets.jvm.compilations.test.runtimeDependencyFiles
    if (Os.isFamily(Os.FAMILY_MAC)) {
        //jvmArgs "-XstartOnFirstThread"
    }
    main = 'com.soywiz.korau.impl.jna.JnaSoundProviderSample'
}
*/
