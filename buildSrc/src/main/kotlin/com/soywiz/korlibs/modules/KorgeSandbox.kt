package com.soywiz.korlibs.modules

import com.soywiz.korge.gradle.BuildVersions
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

object KorgeSandbox {
    @JvmStatic
    fun configure(project: Project) {
        //project.dependencies { add("commonMainApi", "com.soywiz.korlibs.korge2:korge-compose:${BuildVersions.KORGE}") }
        //project.plugins.apply("org.jetbrains.compose")
    }
}
