package com.soywiz.korge.gradle.util

import org.apache.tools.ant.taskdefs.condition.*
import org.gradle.api.*
import org.gradle.process.*
import java.io.*

fun Project.debugExecSpec(exec: ExecSpec) {
	logger.warn("COMMAND: ${exec.commandLine.joinToString(" ")}")
    //println("COMMAND: ${exec.commandLine.joinToString(" ")}")
}

/*
class LoggerOutputStream(val logger: org.gradle.api.logging.Logger, val prefix: String) : OutputStream() {
    val buffer = ByteArrayOutputStream()

    override fun write(b: Int) {
        if (b == 13 || b == 10) {
            val line = buffer.toString("UTF-8")
            println("$prefix: $line")
            buffer.reset()
        } else {
            buffer.write(b)
        }
    }
}
*/

fun Project.execLogger(action: (ExecSpec) -> Unit): ExecResult {
	return exec {
		action(this)
        //standardOutput = LoggerOutputStream(logger, "OUT")
        //errorOutput = LoggerOutputStream(logger, "ERR")
		debugExecSpec(this)
	}
}

fun ExecSpec.commandLineCompat(vararg args: String): ExecSpec {
	return when {
		Os.isFamily(Os.FAMILY_WINDOWS) -> commandLine("cmd", "/c", *args)
		else -> commandLine(*args)
	}
}

fun Project.execOutput(vararg cmds: String, log: Boolean = true): String {
	val stdout = ByteArrayOutputStream()
	exec {
		commandLineCompat(*cmds)
		standardOutput = stdout
        //errorOutput = stdout
		if (log) {
			debugExecSpec(this)
		}
	}
	return stdout.toString("UTF-8")
}

