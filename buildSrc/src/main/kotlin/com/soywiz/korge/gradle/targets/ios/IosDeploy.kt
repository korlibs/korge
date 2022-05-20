package com.soywiz.korge.gradle.targets.ios

import org.gradle.api.Project
import com.soywiz.korge.gradle.util.projectExtension
import com.soywiz.korge.gradle.util.execLogger
import java.io.File

val Project.iosDeployExt by projectExtension {
    IosDeploy(this)
}

val korgeCacheDir get() = File(System.getProperty("user.home"), ".korge").apply { mkdirs() }

class IosDeploy(val project: Project) {
    val iosDeployDir = File(korgeCacheDir, "ios-deploy")
    val iosDeployCmd = File(iosDeployDir, "build/Release/ios-deploy")

    val isInstalled get() = iosDeployCmd.exists()

    fun command(vararg cmds: String) {
        project.execLogger {
            it.commandLine(iosDeployCmd, *cmds)
            it.standardInput = System.`in`
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
        if (!File(iosDeployDir, ".git").exists()) clone()
        if (!isInstalled) build()
    }

    fun build() {
        project.execLogger {
            it.commandLine("xcodebuild", "-target", "ios-deploy")
            it.workingDir = iosDeployDir
        }
    }
}
