package com.soywiz.korge.gradle.targets.windows

import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import com.soywiz.korge.gradle.util.get

/**
 * @NOTE: We have to call compileKotlinMingw first at least once so the toolchain is downloaded before doing stuff
 */
object WindowsToolchain {
    val depsDir by lazy { File("${System.getProperty("user.home")}/.konan/dependencies") }
    val msysDir by lazy { depsDir.getFirstRegexOrFail(Regex("^msys2-mingw")) }
    val msys2 by lazy { depsDir.getFirstRegexOrNull(Regex("^msys2-mingw-w64-x86_64-clang")) }
    val path by lazy { msysDir["bin"] }
    val path2 by lazy {
        msys2?.get("lib/gcc/x86_64-w64-mingw32")?.getFirstRegexOrFail(Regex("^\\d+\\.\\d+\\.\\d+$"))
    }
    val windres by lazy { path["windres.exe"] }
	val strip by lazy { path["strip.exe"] }
}

fun Project.compileWindowsRC(rcFile: File, objFile: File, log: Boolean = true): File {
    exec {
        it.commandLine(WindowsToolchain.windres.absolutePath, rcFile.path, "-O", "coff", objFile.absolutePath)
		it.workingDir(rcFile.parentFile)
		it.environment("PATH", System.getenv("PATH") + ";" + listOfNotNull(WindowsToolchain.path.absolutePath, WindowsToolchain.path2?.absolutePath).joinToString(";"))
		if (log) {
            logger.info("WindowsToolchain.path.absolutePath: ${WindowsToolchain.path.absolutePath}")
            logger.info("WindowsToolchain.path2.absolutePath: ${WindowsToolchain.path2?.absolutePath}")
			debugExecSpec(it)
		}
    }
    return objFile
}

fun Project.stripWindowsExe(exe: File, log: Boolean = true): File {
	exec {
		it.commandLine(WindowsToolchain.strip.absolutePath, exe.absolutePath)
		it.workingDir(exe.parentFile)
		it.environment("PATH", System.getenv("PATH") + ";" + WindowsToolchain.path.absolutePath)
		if (log) {
			debugExecSpec(it)
		}
	}
	return exe
}
