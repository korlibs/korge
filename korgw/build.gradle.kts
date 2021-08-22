description = "Portable UI with accelerated graphics support for Kotlin"

val jnaVersion: String by project

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val enableKotlinRaspberryPi: String by project
val doEnableKotlinRaspberryPi get() = enableKotlinRaspberryPi == "true"

val isWindows get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)
val hasAndroid = project.extensions.findByName("android") != null

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.nativeTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return when {
        isWindows -> listOfNotNull(mingwX64())
        isMacos -> listOfNotNull(macosX64(), iosArm64(), iosX64())
        else -> listOfNotNull(
            linuxX64(),
            if (doEnableKotlinRaspberryPi) linuxArm32Hfp() else null,
            mingwX64(), macosX64()
        )
    }
}

if (doEnableKotlinNative) {
    kotlin {
        for (target in nativeTargets()) {
            target.compilations["main"].cinterops {
                if (target.name == "linuxX64") {
                    maybeCreate("GL")
                    maybeCreate("SDL2")
                }
                if (target.name == "linuxArm32Hfp") {
                    maybeCreate("GL_rpi")
                    maybeCreate("SDL2_rpi")
                }
                //if (target.name == "linuxX64") maybeCreate("X11")
            }
        }
    }
}

dependencies {
    add("commonMainApi", project(":korim"))
    add("jvmMainApi", "net.java.dev.jna:jna:$jnaVersion")
    add("jvmMainApi", "net.java.dev.jna:jna-platform:$jnaVersion")
    if (hasAndroid) {
        add("androidMainApi", "com.android.support:appcompat-v7:28.0.0")
    }
}

/*
import org.apache.tools.ant.taskdefs.condition.Os

apply plugin: com.soywiz.korlibs.KorlibsPlugin

korlibs {
    exposeVersion()
    //dependencyCInteropsExternal("com.soywiz.korlibs.korim:korim:$korimVersion", "stb_image", ["linuxX64", "iosX64", "iosArm64"])
    //dependencyCInterops("GL", ["linuxX64"])

    dependencies {
        if (hasAndroid) {
            add("androidMainApi", "com.android.support:appcompat-v7:28.0.0")
        }
    }
}

kotlin {
    if (korlibs.linuxEnabled) {
        linuxX64 {
            compilations.main {
                cinterops {
                    GL {
                        //print(it)
                        //def linuxFolder = new File(rootDir, "nlib/linuxX64")
                        //compilerOpts("-I${new File(linuxFolder, "include").absolutePath}")
                        //linkerOpts("-L${new File(linuxFolder, "lib/x86_64-linux-gnu").absolutePath}")
                    }
                    //gtk3 {
                    //    ["/opt/local/include", "/usr/include", "/usr/local/include"].forEach {
                    //        includeDirs(
                    //            "$it/atk-1.0",
                    //            "$it/gdk-pixbuf-2.0",
                    //            "$it/cairo",
                    //            "$it/harfbuzz",
                    //            "$it/pango-1.0",
                    //            "$it/gtk-3.0",
                    //            "$it/glib-2.0"
                    //        )
                    //    }
//
                    //    includeDirs(
                    //        "/opt/local/lib/glib-2.0/include",
                    //        "/usr/lib/x86_64-linux-gnu/glib-2.0/include",
                    //        "/usr/local/lib/glib-2.0/include"
                    //    )
                    //}
                }
            }
        }
    }
}

kotlin.sourceSets {
    jvmMain {
        resources.srcDir("libs")
    }
}

task runSample(type: JavaExec) {
    classpath = kotlin.targets.jvm.compilations.test.runtimeDependencyFiles
    main = 'com.soywiz.korgw.TestGameWindow'
}

task runSampleFirstThread(type: JavaExec) {
    classpath = kotlin.targets.jvm.compilations.test.runtimeDependencyFiles
    if (Os.isFamily(Os.FAMILY_MAC)) {
        jvmArgs "-XstartOnFirstThread"
    }
    main = 'com.soywiz.korgw.TestGameWindow'
}

task runSampleMainThread {
    dependsOn runSampleFirstThread
}
*/
