package com.soywiz.korge.gradle.targets.windows

import com.soywiz.korio.file.std.*
import org.gradle.api.*
import java.io.*

/**
 * @NOTE: We have to call compileKotlinMingw first at least once so the toolchain is downloaded before doing stuff
 */
object WindowsToolchain {
    val path by lazy {
        val depsDir = File(System.getProperty("user.home") + "/.konan/dependencies")
        val msysDir = depsDir.listFiles { dir, name -> name.startsWith("msys2-mingw") }.firstOrNull() ?: error("Can't find msys2 in $depsDir")
        msysDir["bin"]
    }
    val windres by lazy { path["windres.exe"] }
	val strip by lazy { path["strip.exe"] }
}

fun Project.compileWindowsRC(rcFile: File, objFile: File): File {
    exec {
        it.commandLine(WindowsToolchain.windres.absolutePath, rcFile.path, "-O", "coff", objFile.absolutePath)
		it.workingDir(rcFile.parentFile)
		it.environment("PATH", System.getenv("PATH") + ";" + WindowsToolchain.path.absolutePath)
    }
    return objFile
}

fun Project.stripWindowsExe(exe: File): File {
	exec {
		it.commandLine(WindowsToolchain.strip.absolutePath, exe.absolutePath)
		it.workingDir(exe.parentFile)
		it.environment("PATH", System.getenv("PATH") + ";" + WindowsToolchain.path.absolutePath)
	}
	return exe
}
