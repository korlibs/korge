package korlibs.korge.gradle.targets.ios

import java.io.File
import korlibs.korge.gradle.util.execLogger
import korlibs.korge.gradle.util.projectExtension
import org.gradle.api.Project

val Project.iosTvosDeployExt by projectExtension {
    IosDeploy(this)
}

val korgeCacheDir get() = File(System.getProperty("user.home"), ".korge").apply { mkdirs() }

class IosDeploy(val project: Project) {
    val iosDeployVersion = "1.12.2"
    val iosDeployRepo = "https://github.com/ios-control/ios-deploy.git"
    val iosDeployDir = File(korgeCacheDir, "ios-deploy-$iosDeployVersion")
    val iosDeployCmd = File(iosDeployDir, "build/Release/ios-deploy")

    val isInstalled get() = iosDeployCmd.exists()

    fun install(localAppPath: String) {
        command("--bundle", localAppPath)
    }

    fun installAndRun(localAppPath: String) {
        command("--noninteractive", "-d", "--bundle", localAppPath)
    }

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
            it.workingDir(iosDeployDir.absolutePath)
            it.commandLine("git", "clone", iosDeployRepo, iosDeployDir.absolutePath)
        }
        project.execLogger {
            it.workingDir(iosDeployDir.absolutePath)
            it.commandLine("git", "checkout", iosDeployVersion)
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
