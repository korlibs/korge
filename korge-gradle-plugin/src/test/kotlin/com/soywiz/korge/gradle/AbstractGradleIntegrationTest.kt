package com.soywiz.korge.gradle

import groovy.lang.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.process.*
import org.gradle.testfixtures.*
import java.io.*
import java.nio.file.*

class TestableExecSpec : ExecSpec {
    var _executable: Any? = null
    var _args: MutableList<String> = mutableListOf()
    var _workingDir: File = File(".")
    var environments = mutableMapOf<String, Any>()
    var _ignoreExitValue: Boolean = false
    var _input: InputStream = System.`in`
    var _output: OutputStream = System.`out`
    var _error: OutputStream = System.`err`

    override fun getExecutable(): String = _executable?.toString() ?: ""
    override fun setExecutable(executable: String) { _executable = executable }
    override fun setExecutable(executable: Any) { _executable = executable }
    override fun executable(executable: Any): ProcessForkOptions = this.apply { _executable = executable }
    override fun getWorkingDir(): File = _workingDir
    override fun setWorkingDir(dir: File) { _workingDir = dir }
    override fun setWorkingDir(dir: Any) { _workingDir = dir as File }
    override fun workingDir(dir: Any?): ProcessForkOptions = this.apply { _workingDir = dir as File }
    override fun getEnvironment(): MutableMap<String, Any> = environments
    override fun setEnvironment(environmentVariables: MutableMap<String, *>) { environments.putAll(environmentVariables as MutableMap<String, Any>) }
    override fun environment(environmentVariables: MutableMap<String, *>): ProcessForkOptions = this.apply { setEnvironment(environmentVariables) }
    override fun environment(name: String, value: Any): ProcessForkOptions = this.apply { environments[name] = value }

    override fun copyTo(out: ProcessForkOptions): ProcessForkOptions = this.apply {
        out.executable = this.executable
        out.workingDir = this.workingDir
        out.environment = this.environment
    }

    override fun setIgnoreExitValue(ignoreExitValue: Boolean): BaseExecSpec = this.apply { _ignoreExitValue = ignoreExitValue }
    override fun isIgnoreExitValue(): Boolean = _ignoreExitValue

    override fun setStandardInput(inputStream: InputStream): BaseExecSpec = this.apply { _input = inputStream }
    override fun getStandardInput(): InputStream = _input

    override fun setStandardOutput(outputStream: OutputStream): BaseExecSpec = this.apply { _output = outputStream }
    override fun getStandardOutput(): OutputStream = _output

    override fun setErrorOutput(outputStream: OutputStream): BaseExecSpec = this.apply { _error = outputStream }
    override fun getErrorOutput(): OutputStream = _error

    override fun getCommandLine(): MutableList<String> = (listOf(executable) + _args).toMutableList()
    override fun setCommandLine(args: MutableList<String>) {
        executable = args.first()
        _args = args.drop(1).toMutableList()
    }

    override fun setCommandLine(vararg args: Any) { commandLine = args.map { it.toString() }.toMutableList() }
    override fun setCommandLine(args: MutableIterable<*>) { commandLine = args.map { it.toString() }.toMutableList() }
    override fun commandLine(vararg args: Any): ExecSpec = this.apply { setCommandLine(args.toMutableList()) }
    override fun commandLine(args: MutableIterable<*>): ExecSpec = this.apply { setCommandLine(args) }
    override fun args(vararg args: Any): ExecSpec = this.apply { setArgs(args.toMutableList()) }
    override fun args(args: MutableIterable<*>): ExecSpec = this.apply { setArgs(args) }
    override fun setArgs(args: MutableList<String>): ExecSpec = this.apply { _args = args.toMutableList() }
    override fun setArgs(args: MutableIterable<*>): ExecSpec = this.apply { _args = args.map { it.toString() }.toMutableList() }

    override fun getArgs(): MutableList<String> = _args

    override fun getArgumentProviders(): MutableList<CommandLineArgumentProvider> = TODO()
}

data class TestableExecResult(val stdout: String, val stderr: String = "", val exitCode: Int = 0) : ExecResult {
    override fun getExitValue(): Int = exitCode
    override fun assertNormalExitValue(): ExecResult = this.apply { assert(exitValue == 0) }
    override fun rethrowFailure(): ExecResult = this.apply { assertNormalExitValue() }
}

data class TestableExecRequest(val commandLine: List<String>, val workingDir: File) {
    val normalizedCommandLine = normalizeCommandLine(commandLine)

    companion object {
        fun normalizeCommandLine(list: List<String>): List<String> {
            if (list.size >= 2 && list[0] == "cmd" && list[1] == "/c") {
                return list.slice(2 until list.size)
            }
            return list
        }
    }
}

class TestableProject : Project by ProjectBuilder.builder().build() {
    //private val myexecTask by lazy { tasks.create("myexectask", Exec::class.java) }

    val execResults = LinkedHashMap<List<String>, (TestableExecRequest) -> TestableExecResult>()

    fun defineExecResult(vararg commandLine: String, result: (request: TestableExecRequest) -> TestableExecResult) {
        // This handles windows cmd /c to simplify tests
        execResults[TestableExecRequest.normalizeCommandLine(commandLine.toList())] = result
    }

    fun defineExecResult(vararg commandLine: String, result: List<TestableExecResult>) {
        var time = 0
        defineExecResult(*commandLine) { result[time++] }
    }

    fun defineExecResult(vararg commandLine: String, result: TestableExecResult) {
        defineExecResult(*commandLine) { result }
    }

    fun defineExecResult(vararg commandLine: String, stdout: String, stderr: String = "", exitCode: Int = 0) {
        defineExecResult(*commandLine, result = TestableExecResult(stdout, stderr, exitCode))
    }

    override fun exec(closure: Closure<*>): ExecResult = exec { closure.call(it) }
    override fun exec(action: Action<in ExecSpec>): ExecResult {
        val spec = TestableExecSpec()
        action.execute(spec)
        val request = TestableExecRequest(spec.commandLine.toList(), spec.workingDir)
        val resultFactory = execResults[request.normalizedCommandLine] ?: error("Can't find output for $request")
        val result = resultFactory(request)
        spec.standardOutput.write(result.stdout.toByteArray(Charsets.UTF_8))
        return result
    }
}

open class AbstractGradleIntegrationTest {
    val project = TestableProject()

    inline fun createTempDirectory(block: (dir: File) -> Unit) {
        val dir = Files.createTempDirectory("temp").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    val Task.dependsOnNames: List<String> get() = this.dependsOn.map { when (it) {
        is Task -> it.name
        else -> it.toString()
    } }

    val Task.dependsOnResolved: List<Task> get() = this.dependsOn.map { when (it) {
        is Task -> it
        else -> project.tasks.getByName(it.toString())
    } }

    fun Project.executeTasks(list: Iterable<Task>) {
        for (task in list) {
            task.execute()
        }
    }

    fun Task.execute() {
        project.executeTasks(this.dependsOnResolved)
        for (action in actions) {
            action.execute(this)
        }
        project.executeTasks(this.finalizedBy.getDependencies(this))
    }

    fun captureStdout(block: () -> Unit): String {
        val out = ByteArrayOutputStream()
        val tempOut = System.out
        System.setOut(PrintStream(out, true, "UTF-8"))
        try {
            block()
        } finally {
            System.setOut(tempOut)
        }
        return out.toByteArray().toString(Charsets.UTF_8)
    }
}
