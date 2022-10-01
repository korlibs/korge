package com.soywiz.korge.gradle.targets.native

import com.soywiz.korge.gradle.targets.CrossExecType
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option

fun Exec.commandLineCross(vararg args: String, type: CrossExecType) {
    commandLine(*type.commands(*args))
}

open class KotlinNativeCrossTest : org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest() {
    @Input
    @Option(option = "type", description = "Sets the executable cross type")
    lateinit var type: CrossExecType

    @Internal
    var debugMode = false

    @get:Internal
    override val testCommand: TestCommand = object : TestCommand() {
        val commands get() = type.commands()

        override val executable: String
            get() = commands.first()

        override fun cliArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>
        ): List<String> =
            listOfNotNull(
                *commands.drop(1).toTypedArray(),
                this@KotlinNativeCrossTest.executable.absolutePath,
            ) +
                testArgs(testLogger, checkExitCode, testGradleFilter, testNegativeGradleFilter, userArgs)
    }
}
