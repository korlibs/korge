package korlibs.korge.gradle.targets.js

import org.gradle.api.*

fun Project.configureWebpackFixes() {
// @TODO: HACK for webpack: https://youtrack.jetbrains.com/issue/KT-48273#focus=Comments-27-5122487.0-0
    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
        //rootProject.extensions.getByType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class.java).versions.webpackDevServer.version = "4.0.0-rc.0"
    }
}
