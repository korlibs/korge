package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.util.FileList
import korlibs.korge.gradle.util.execLogger
import korlibs.korge.gradle.util.projectExtension
import korlibs.korge.gradle.util.takeIfExists
import org.gradle.api.Project
import java.io.File

val Project.iosXcodegenExt by projectExtension {
    IosXcodegen(this)
}

class IosXcodegen(val project: Project) {
    //val xcodeGenGitTag = "2.25.0"
    val xcodeGenGitTag = "2.35.0"
    val korlibsFolder = File(System.getProperty("user.home") + "/.korlibs").apply { mkdirs() }
    val xcodeGenFolder = File(korlibsFolder, "XcodeGen-$xcodeGenGitTag")
    val xcodeGenLocalExecutable = File("/usr/local/bin/xcodegen")
    val xcodeGenExecutable = FileList(
        File(xcodeGenFolder, ".build/release/xcodegen"),
        File(xcodeGenFolder, ".build/apple/Products/Release/xcodegen"),
    )
    val xcodeGenExe: File
        get() = xcodeGenExecutable.takeIfExists() ?: xcodeGenLocalExecutable.takeIfExists() ?: error("Can't find xcodegen")

    fun isInstalled(): Boolean = xcodeGenLocalExecutable.exists() || xcodeGenExecutable.exists()
    fun install() {
        if (!File(xcodeGenFolder, ".git").isDirectory) {
            project.execLogger {
                //it.commandLine("git", "clone", "--depth", "1", "--branch", xcodeGenGitTag, "https://github.com/yonaskolb/XcodeGen.git")
                it.commandLine("git", "clone", "https://github.com/yonaskolb/XcodeGen.git", xcodeGenFolder)
                it.workingDir(korlibsFolder)
            }
        }
        project.execLogger {
            it.commandLine("git", "pull")
            it.workingDir(xcodeGenFolder)
        }
        project.execLogger {
            it.commandLine("git", "checkout", xcodeGenGitTag)
            it.workingDir(xcodeGenFolder)
        }
        project.execLogger {
            it.commandLine("make", "build")
            it.workingDir(xcodeGenFolder)
        }
    }
}
