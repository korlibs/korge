package com.soywiz.korlibs

import com.soywiz.korge.gradle.targets.isLinux
import com.soywiz.korge.gradle.targets.isMingw
import com.soywiz.korlibs.modules.*
import org.gradle.api.*

object NativeTools {
    @JvmStatic
    fun configureCInteropWin32(project: Project, name: String) {
        if (project.doEnableKotlinNative) {
            project.kotlin {
                for (target in nativeTargets(project)) {
                    if (target.isMingw) {
                        target.compilations["main"].cinterops {
                            maybeCreate(name)
                        }
                    }
                }
            }
        }
    }


    @JvmStatic
    fun configureCInteropLinux(project: Project, name: String) {
        if (project.doEnableKotlinNative) {
            project.kotlin {
                for (target in nativeTargets(project)) {
                    if (target.isLinux) {
                        target.compilations["main"].cinterops {
                            maybeCreate(name)
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun configureAllCInterop(project: Project, name: String) {
        if (project.doEnableKotlinNative) {
            project.kotlin {
                for (target in allNativeTargets(project)) {
                    target.compilations["main"].cinterops {
                        maybeCreate(name)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun configureAndroidDependency(project: Project, dep: Any) {
        project.dependencies {
            project.afterEvaluate {
                if (project.configurations.findByName("androidMainApi") != null) {
                    add("androidMainApi", dep)
                }
            }
        }
    }

    @JvmStatic
    fun groovyConfigurePublishing(project: Project, multiplatform: Boolean) {
        project.configurePublishing(multiplatform = multiplatform)
    }

    @JvmStatic
    fun groovyConfigureSigning(project: Project) {
        project.configureSigning()
    }
}
