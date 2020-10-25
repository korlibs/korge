package com.soywiz.korge.gradle.util

import org.apache.tools.ant.taskdefs.condition.*
import org.gradle.api.*
import org.gradle.process.*
import java.io.*

fun Project.debugExecSpec(exec: ExecSpec) {
	logger.info("COMMAND: ${exec.commandLine.joinToString(" ")}")
}

fun Project.execLogger(action: (ExecSpec) -> Unit) {
	exec {
		action(it)
		debugExecSpec(it)
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
		it.commandLine(*cmds)
		it.standardOutput = stdout
		if (log) {
			debugExecSpec(it)
		}
	}
	return stdout.toString("UTF-8")
}

