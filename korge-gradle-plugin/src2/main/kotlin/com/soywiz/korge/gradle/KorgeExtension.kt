package com.soywiz.korge.gradle

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

class KorgeExtension(val project: Project) {
    fun jvm() = project.kotlin.jvm {
        compilations.all {
            it.kotlinOptions.jvmTarget = "1.8"
        }
    }
    fun js() = project.kotlin.js {
        browser {
            compilations.all {
                it.kotlinOptions.sourceMap = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
            // Executable
            binaries.executable()
        }
    }
    fun linux() {
        linuxX64()
        linuxArm64()
    }
    fun windows() {
        mingwX64()
        mingwX86()
    }
    fun macos() {
        macosX64()
        //macosArm64()
    }
    fun ios() {
        iosArm64()
        iosX64()
    }
    fun tvos() {
        tvosArm64()
        tvosX64()
    }
    fun watchos() {
        watchosArm32()
        watchosArm64()
        watchosX86()
    }
    fun android() = project.kotlin.android()

    fun iosArm64() = project.kotlin.iosArm64().korgeConfigure()
    fun iosX64() = project.kotlin.iosX64().korgeConfigure()

    fun tvosArm64() = project.kotlin.tvosArm64().korgeConfigure()
    fun tvosX64() = project.kotlin.tvosX64().korgeConfigure()

    fun watchosArm32() = project.kotlin.watchosArm32().korgeConfigure()
    fun watchosArm64() = project.kotlin.watchosArm64().korgeConfigure()
    fun watchosX86() = project.kotlin.watchosX86().korgeConfigure()

    fun linuxX64() = project.kotlin.linuxX64().korgeConfigure()
    fun linuxArm64() = project.kotlin.linuxArm64().korgeConfigure()

    fun mingwX64() = project.kotlin.mingwX86().korgeConfigure()
    fun mingwX86() = project.kotlin.mingwX64().korgeConfigure()

    fun macosX64() = project.kotlin.macosX64().korgeConfigure()

    fun KotlinNativeTarget.korgeConfigure() {
        compilations.all {
            it.kotlinOptions.freeCompilerArgs = listOf("-Xallocator=mimalloc")
        }
    }
    //fun macosArm64() = project.kotlin.macosArm64()
}
