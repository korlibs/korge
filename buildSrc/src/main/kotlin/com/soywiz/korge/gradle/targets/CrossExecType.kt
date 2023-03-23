package com.soywiz.korge.gradle.targets

import com.soywiz.korge.gradle.util.*
import org.gradle.configurationcache.extensions.*

object WineHQ {
    val EXEC = "wine64"
}

object Box64 {
    val VERSION = "v0.2.2"

    fun exec(vararg params: String): SystemExecResult {
        return executeSystemCommand(ArrayList<String>().apply {
            when {
                isWindows -> add("wsl")
                isMacos -> add("lima")
            }
            addAll(params)
        }.toTypedArray())
    }

    fun whichBox64(): String? = exec("which", "box64").let { it.takeIf { it.success }?.stdout }

    fun ensureBox64(): String {
        val box64 = whichBox64()
        if (box64 == null) {
            exec("mkdir", "-p", "/tmp/box64").stdout.trim()
            val box64Path = exec("realpath", "/tmp/box64").stdout.trim()

            exec("sudo", "apt", "update")
            exec("sudo", "apt", "-y", "install", "git", "build-essential", "cmake")
            exec("git", "clone", "-b", VERSION, "https://github.com/ptitSeb/box64.git", box64Path)
            exec("cmake", "-S", box64Path, "-B", box64Path)
            exec("make", "-C", box64Path)
            exec("sudo", "make", "-C", box64Path, "install")
        }
        return box64 ?: whichBox64() ?: error("Couldn't install box64")
    }
}

class CommandLineCrossResult(val hasBox64: Boolean) {
    fun ensure() {
        if (hasBox64) {
            Box64.ensureBox64()
        }
    }
}

enum class CrossExecType(val cname: String, val interp: String, val arch: String = "X64") {
    WINDOWS("mingw", "wine"),
    LINUX("linux", "lima"),
    LINUX_ARM("linux", "lima", arch = "Arm64"),
    ;

    val valid: Boolean get() = when (this) {
        WINDOWS -> !isWindows
        LINUX -> !isLinux
        LINUX_ARM -> !isLinux && isArm
    }

    val archNoX64: String = if (arch == "X64") "" else arch
    val interpCapital = interp.capitalized() + archNoX64
    val nameWithArch = "${cname}$arch"
    val nameWithArchCapital = nameWithArch.capitalized()

    fun commands(vararg args: String): Pair<CommandLineCrossResult, Array<String>> {
        var hasBox64 = false
        val array = ArrayList<String>().apply {
            when (this@CrossExecType) {
                WINDOWS -> {
                    if (isArm && !isMacos) {
                        add("box64")
                        hasBox64 = true
                    } // wine on macOS can run x64 apps via rosetta, but linux needs box64 emulator
                    add(WineHQ.EXEC)
                }
                LINUX, LINUX_ARM -> {
                    // @TODO: WSL
                    if (isWindows) add("wsl") else add("lima")
                    if (isArm && this@CrossExecType != LINUX_ARM) {
                        add("box64")
                        hasBox64 = true
                    }
                }
            }
            addAll(args)
        }.toTypedArray()
        return CommandLineCrossResult(hasBox64) to array
    }

    companion object {
        val VALID_LIST: List<CrossExecType> = values().filter { it.valid }
    }
}
