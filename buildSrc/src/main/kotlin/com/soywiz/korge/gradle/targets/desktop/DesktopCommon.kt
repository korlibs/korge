package com.soywiz.korge.gradle.targets.desktop

import com.soywiz.korge.gradle.gkotlin
import com.soywiz.korge.gradle.korge
import com.soywiz.korge.gradle.kotlin
import com.soywiz.korge.gradle.targets.isArm
import com.soywiz.korge.gradle.targets.isLinux
import com.soywiz.korge.gradle.targets.isMacos
import com.soywiz.korge.gradle.targets.isWindows
import com.soywiz.korge.gradle.util.Indenter
import com.soywiz.korge.gradle.util.createOnce
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTestsPreset
import java.io.File

val Project.prepareKotlinNativeBootstrap: Task get() = tasks.createOnce("prepareKotlinNativeBootstrap") {
    val output = nativeDesktopBootstrapFile
    outputs.file(output)
    doLast {
        output.parentFile.mkdirs()

        val text = Indenter {
            //line("package korge.bootstrap")
            line("import ${korge.realEntryPoint}")
            line("fun main(args: Array<String>): Unit = RootGameMain.runMain(args)")
            line("object RootGameMain") {
                line("fun runMain() = runMain(arrayOf())")
                line("@Suppress(\"UNUSED_PARAMETER\") fun runMain(args: Array<String>): Unit = com.soywiz.korio.Korio { ${korge.realEntryPoint}() }")
            }
        }
        if (!output.exists() || output.readText() != text) output.writeText(text)
    }
}

val Project.DESKTOP_NATIVE_TARGET get() = when {
    isWindows -> "mingwX64"
    isMacos -> {
        when {
            isArm -> "macosArm64"
            else -> "macosX64"
        }
    } // @TODO: Check if we are on ARM
    isLinux -> "linuxX64"
    else -> "unknownX64"
}

val Project.DESKTOP_NATIVE_CROSS_TARGETS: List<String> get() = when {
    isWindows -> listOfNotNull()
    isMacos -> listOfNotNull("mingwX64", "linuxX64")
    isLinux -> listOfNotNull()
    else -> listOfNotNull()
}

val Project.DESKTOP_NATIVE_TARGETS get() = when {
    isWindows -> listOfNotNull("mingwX64")
    isMacos -> listOfNotNull("macosX64", "macosArm64")
    isLinux -> listOfNotNull("linuxX64", "linuxArm32Hfp".takeIf { korge.enableLinuxArm })
    else -> listOfNotNull(
        "mingwX64",
        "linuxX64",
        "linuxArm32Hfp".takeIf { korge.enableLinuxArm },
        "macosX64", "macosArm64"
    )
}

fun Project.convertNamesToNativeTargets(names: List<String>): List<KotlinNativeTarget> {
    return names.map {
        (gkotlin.targets.findByName(it) as? KotlinNativeTarget) ?: ((gkotlin.presets.getAt(it) as AbstractKotlinNativeTargetPreset<*>).createTarget(it).also {
            gkotlin.targets.add(it)
        })
    }
}

val Project.cnativeTarget get() = DESKTOP_NATIVE_TARGET.capitalize()

val Project.nativeDesktopBootstrapFile get() = File(buildDir, "platforms/native-desktop/bootstrap.kt")
