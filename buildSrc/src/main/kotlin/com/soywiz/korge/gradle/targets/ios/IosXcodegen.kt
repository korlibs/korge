package com.soywiz.korge.gradle.targets.ios

import com.soywiz.korge.gradle.util.FileList
import com.soywiz.korge.gradle.util.execLogger
import com.soywiz.korge.gradle.util.get
import com.soywiz.korge.gradle.util.projectExtension
import com.soywiz.korge.gradle.util.takeIfExists
import org.gradle.api.Project
import java.io.File

val Project.iosXcodegenExt by projectExtension {
    IosXcodegen(this)
}

class IosXcodegen(val project: Project) {
    val korlibsFolder = File(System.getProperty("user.home") + "/.korlibs").apply { mkdirs() }
    val xcodeGenFolder = korlibsFolder["XcodeGen"]
    val xcodeGenLocalExecutable = File("/usr/local/bin/xcodegen")
    val xcodeGenExecutable = FileList(
        xcodeGenFolder[".build/release/xcodegen"],
        xcodeGenFolder[".build/apple/Products/Release/xcodegen"],
    )
    val xcodeGenGitTag = "2.25.0"
    val xcodeGenExe: File
        get() = xcodeGenExecutable.takeIfExists() ?: xcodeGenLocalExecutable.takeIfExists() ?: error("Can't find xcodegen")

    fun isInstalled(): Boolean = xcodeGenLocalExecutable.exists() || xcodeGenExecutable.exists()
    fun install() {
        if (!xcodeGenFolder[".git"].isDirectory) {
            project.execLogger {
                //it.commandLine("git", "clone", "--depth", "1", "--branch", xcodeGenGitTag, "https://github.com/yonaskolb/XcodeGen.git")
                it.commandLine("git", "clone", "https://github.com/yonaskolb/XcodeGen.git")
                it.workingDir(korlibsFolder)
            }
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
