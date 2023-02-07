package com.soywiz.korge.gradle

import com.soywiz.korge.gradle.util.*
import groovy.lang.*
import org.gradle.api.*
import org.gradle.util.*
import org.jetbrains.kotlin.gradle.plugin.*

object KorgeVersionsTask {
    fun registerShowKorgeVersions(project: Project) {
        project.tasks.createThis<Task>("showKorgeVersions") {
            doLast {
                println("Build-time:")
                for ((key, value) in mapOf(
                    "os.name" to System.getProperty("os.name"),
                    "os.version" to System.getProperty("os.version"),
                    "java.vendor" to System.getProperty("java.vendor"),
                    "java.version" to System.getProperty("java.version"),
                    "gradle.version" to GradleVersion.current(),
                    "groovy.version" to GroovySystem.getVersion(),
                    "kotlin.runtime.version" to KotlinVersion.CURRENT,
                    "kotlin.gradle.plugin.version" to getKotlinPluginVersion(logger),
                )) {
                    println(" - $key: $value")
                }
                println("Korge Gradle plugin:")
                for ((key, value) in BuildVersions.ALL) {
                    println(" - $key: $value")
                }
            }
        }
    }
}
