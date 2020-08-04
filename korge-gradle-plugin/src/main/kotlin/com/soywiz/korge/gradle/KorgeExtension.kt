package com.soywiz.korge.gradle

import org.gradle.api.*

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
    fun linuxX64() = project.kotlin.linuxX64()
    fun mingwX64() = project.kotlin.mingwX64()
    fun macosX64() = project.kotlin.macosX64()
}
