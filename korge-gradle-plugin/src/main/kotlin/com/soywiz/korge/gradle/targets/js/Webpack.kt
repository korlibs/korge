package com.soywiz.korge.gradle.targets.js

import org.gradle.api.*
import org.gradle.kotlin.dsl.*

fun Project.configureWebpackFixes() {
// @TODO: HACK for webpack: https://youtrack.jetbrains.com/issue/KT-48273#focus=Comments-27-5122487.0-0
    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().versions.webpackDevServer.version = "4.0.0-rc.0"
    }
}
