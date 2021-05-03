package com.soywiz.korge.gradle.targets.ios

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*

val Project.iosDeployExt by projectExtension {
    IosDeploy(this)
}

class IosDeploy(val project: Project) {
    val iosDeployDir = project.korgeCacheDir["ios-deploy"]
    val iosDeployCmd = iosDeployDir["build/Release/ios-deploy"]

    val isInstalled get() = iosDeployCmd.exists()

    fun command(vararg cmds: String) {
        project.execLogger {
            it.commandLine(iosDeployCmd, *cmds)
        }
    }

    fun update() {
        installIfRequired()
        project.execLogger {
            it.workingDir = iosDeployDir
            it.commandLine("git", "pull")
        }
        build()
    }

    fun clone() {
        iosDeployDir.mkdirs()
        project.execLogger {
            it.commandLine("git", "clone", "https://github.com/korlibs/ios-deploy.git", iosDeployDir.absolutePath)
        }
    }

    fun installIfRequired() {
        if (!iosDeployDir[".git"].exists()) clone()
        if (!isInstalled) build()
    }

    fun build() {
        project.execLogger {
            it.commandLine("xcodebuild", "-target", "ios-deploy")
            it.workingDir = iosDeployDir
        }
    }
}
