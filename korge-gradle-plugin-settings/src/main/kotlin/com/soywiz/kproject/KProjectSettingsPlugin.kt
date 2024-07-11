package com.soywiz.kproject

import com.soywiz.kproject.util.*
import kproject
import org.gradle.api.*
import org.gradle.api.initialization.*
import java.io.*

@Suppress("unused")
class KProjectSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        //println("KProjectSettingsPlugin: $settings")
        if (File(settings.rootDir, "deps.kproject.yml").exists()) {
            settings.kproject("./deps")
        }

        val projectDir = settings.rootProject.projectDir
        val buildFileKts = File(projectDir, "build.gradle.kts")
        val buildFileGroovy = File(projectDir, "build.gradle")
        if (!buildFileKts.exists() && !buildFileGroovy.exists()) {
            buildFileKts.writeTextIfNew("""
            import korlibs.korge.gradle.*
            plugins { id("com.soywiz.korge") version korlibs.korge.gradle.common.KorgeGradlePluginVersion.VERSION }
            korge { id = "org.korge.unknown.game"; loadYaml(file("korge.yaml")) }
            dependencies { findProject(":deps")?.also { add("commonMainApi", it) } }
            val baseFile = file("build.extra.gradle.kts").takeIf { it.exists() }?.also { apply(from = it) }
        """.trimIndent())
        }

    }
}
