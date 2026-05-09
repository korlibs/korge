package korlibs.korge.gradle.util

import org.apache.tools.ant.taskdefs.condition.*
import org.gradle.api.*
import org.gradle.process.*
import java.io.*
import javax.inject.Inject

private interface InjectedExecOps {
    @get:Inject
    val execOps: ExecOperations
}

fun Project.debugExecSpec(exec: ExecSpec) {
    logger.warn("COMMAND: ${exec.commandLine.joinToString(" ")}")
}

fun Project.execThis(block: ExecSpec.() -> Unit): ExecResult =
    objects.newInstance(InjectedExecOps::class.java).execOps.exec(block)

fun Project.execLogger(action: (ExecSpec) -> Unit): ExecResult {
    return execThis {
        action(this)
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
    execThis {
        commandLineCompat(*cmds)
        standardOutput = stdout
        //errorOutput = stdout
        if (log) {
            debugExecSpec(this)
        }
    }
    return stdout.toString("UTF-8")
}
