package com.soywiz.korge.gradle.targets.windows

import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.util.get
import com.soywiz.korlibs.*
import org.gradle.api.*
import org.gradle.process.*
import java.io.*
import java.net.*

/**
 * @NOTE: We have to call compileKotlinMingw first at least once so the toolchain is downloaded before doing stuff
 */
object WindowsToolchain {
    val depsDir: File by lazy { File("${System.getProperty("user.home")}/.konan/dependencies") }
    val resourceHackerZip: File by lazy {
        println("Downloading tool for replacing resources in executable..")
        File(depsDir, "resource_hacker.zip").also { it.writeBytes(URL("https://github.com/korlibs/korge-tools/releases/download/resourcehacker/resource_hacker.zip").readBytes()) }
    }
    val resourceHackerDir: File by lazy { File(depsDir, "resourcehacker") }
    val resourceHackerExe: File by lazy {
        File(resourceHackerDir, "ResourceHacker.exe").also {
            if (!it.exists()) {
                resourceHackerDir.mkdirs()
                unzipTo(resourceHackerDir, resourceHackerZip)
            }
        }
    }
    val msysDir by lazy { depsDir.getFirstRegexOrFail(Regex("^msys2-mingw")) }
    val msys2 by lazy { depsDir.getFirstRegexOrNull(Regex("^msys2-mingw-w64-x86_64-clang")) }
    val path by lazy { msysDir["bin"] }
    val path2 by lazy {
        msys2?.get("lib/gcc/x86_64-w64-mingw32")?.getFirstRegexOrFail(Regex("^\\d+\\.\\d+\\.\\d+$"))
    }
    val windres by lazy { path["windres.exe"] }
	val strip by lazy { path["strip.exe"] }
    //val rcOrNull by lazy { msys2?.get("bin/llvm-rc.exe") }
    //val rc by lazy { rcOrNull ?: error("Can't find llvm-rc.exe") }
}

fun Project.compileWindowsRC(rcFile: File, objFile: File, log: Boolean = true): File {
    execThis {
        commandLine(WindowsToolchain.windres.absolutePath, rcFile.path, "-O", "coff", objFile.absolutePath)
		workingDir(rcFile.parentFile)
        environment("PATH", System.getenv("PATH") + ";" + listOfNotNull(WindowsToolchain.path.absolutePath, WindowsToolchain.path2?.absolutePath).joinToString(";"))
		if (log) {
            logger.info("WindowsToolchain.path.absolutePath: ${WindowsToolchain.path.absolutePath}")
            logger.info("WindowsToolchain.path2.absolutePath: ${WindowsToolchain.path2?.absolutePath}")
			debugExecSpec(this)
		}
    }
    return objFile
}

fun Project.compileWindowsRES(rcFile: File, resFile: File, log: Boolean = true): File {
    /*
    execThis {
        commandLine(WindowsToolchain.rc.absolutePath, rcFile.path)
        workingDir(rcFile.parentFile)
        environment("PATH", System.getenv("PATH") + ";" + listOfNotNull(WindowsToolchain.path.absolutePath, WindowsToolchain.path2?.absolutePath).joinToString(";"))
    }
    return File(rcFile.parentFile, "${rcFile.nameWithoutExtension}.res")
     */
    execThis {
    //rh.exe -open .\in\resources.rc -save .\out\resources.res -action compile -log NUL
        workingDir = rcFile.parentFile
        commandLineWindowsExe(
            WindowsToolchain.resourceHackerExe.absolutePath,
            "-open", rcFile.path,
            "-save", resFile.path,
            "-action", "compile",
            //"-log", "NUL",
        )
    }
    return resFile
}

fun ExecSpec.commandLineWindowsExe(vararg values: Any?): ExecSpec {
    return this.commandLine(
        *(if (!isWindows) arrayOf(WineHQ.EXEC) else emptyArray()),
        *values
    )
}

fun Project.replaceExeWithRes(exe: File, res: File) {
    execThis {
        commandLineWindowsExe(
            WindowsToolchain.resourceHackerExe.absolutePath,
            "-open", exe.path,
            "-save", exe.path,
            "-action", "addoverwrite",
            "-res", res.absolutePath,
        )
    }

}

fun Project.stripWindowsExe(exe: File, log: Boolean = true): File {
	execThis {
        commandLine(WindowsToolchain.strip.absolutePath, exe.absolutePath)
        workingDir(exe.parentFile)
        environment("PATH", System.getenv("PATH") + ";" + WindowsToolchain.path.absolutePath)
		if (log) {
			debugExecSpec(this)
		}
	}
	return exe
}
