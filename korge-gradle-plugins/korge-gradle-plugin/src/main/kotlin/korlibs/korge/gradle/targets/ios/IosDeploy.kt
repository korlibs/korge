package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*

val Project.iosTvosDeployExt by projectExtension {
    IosDeploy(this)
}

val korgeCacheDir get() = File(System.getProperty("user.home"), ".korge").apply { mkdirs() }

class IosDeploy(val project: Project) {
    val iosDeployVersion = "1.12.2"
    //val iosDeployRepo = "https://github.com/korlibs/ios-deploy.git"
    val iosDeployRepo = "https://github.com/ios-control/ios-deploy.git"
    val iosDeployDir = File(korgeCacheDir, "ios-deploy-$iosDeployVersion")
    val iosDeployCmd = File(iosDeployDir, "build/Release/ios-deploy")

    val isInstalled get() = iosDeployCmd.exists()

    // https://github.com/ios-control/ios-deploy/issues/588
    // no ios-deploy required anymore?
    // xcrun devicectl list devices -j /tmp/devices.json
    // xcrun devicectl device install app --device 00008110-001XXXXXXXXXX ./korge-sandbox/build/platforms/ios/app.xcodeproj/build/Build/Products/Debug-iphoneos/unnamed.app
    // crun devicectl device process launch --device 00008110-001XXXXXXXXXX file:///private/var/containers/Bundle/Application/1604D2D5-35F3-4E43-8B47-1DEF5D778480/nilo.app

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
