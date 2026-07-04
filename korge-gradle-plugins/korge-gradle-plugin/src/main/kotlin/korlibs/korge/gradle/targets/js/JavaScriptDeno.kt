package korlibs.korge.gradle.targets.js

import java.io.File
import korlibs.korge.gradle.targets.GROUP_KORGE_PACKAGE
import korlibs.korge.gradle.targets.GROUP_KORGE_RUN
import korlibs.korge.gradle.util.createThis
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.DefaultTestFailure
import org.gradle.api.internal.tasks.testing.DefaultTestMethodDescriptor
import org.gradle.api.internal.tasks.testing.DefaultTestOutputEvent
import org.gradle.api.internal.tasks.testing.DefaultTestSuiteDescriptor
import org.gradle.api.internal.tasks.testing.TestCompleteEvent
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.TestStartEvent
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult
import org.gradle.work.DisableCachingByDefault

fun Project.configureDenoTest() {
    afterEvaluate {
        if (tasks.findByName("compileTestDevelopmentExecutableKotlinJs") == null) return@afterEvaluate

        val jsDenoTest = project.tasks.createThis<DenoTestTask>("jsDenoTest") {
        }
    }
}

fun Project.configureDenoRun() {
    afterEvaluate {
        if (tasks.findByName("compileDevelopmentExecutableKotlinJs") == null) return@afterEvaluate

        val baseRunFileNameBase = project.fullPathName().trim(':').replace(':', '-')
        val baseRunFileName = "$baseRunFileNameBase.mjs"
        val runFile = File(rootProject.rootDir, "build/js/packages/$baseRunFileNameBase/kotlin/$baseRunFileName")

        val runDeno = project.tasks.createThis<Exec>("runDeno") {
            group = GROUP_KORGE_RUN
            dependsOn("compileDevelopmentExecutableKotlinJs")
            commandLine("deno", "run", "--unstable-ffi", "--unstable-webgpu", "-A", runFile)
            workingDir(runFile.parentFile.absolutePath)
        }

        val packageDeno = project.tasks.createThis<Exec>("packageDeno") {
            group = GROUP_KORGE_PACKAGE
            dependsOn("compileDevelopmentExecutableKotlinJs")
            commandLine("deno", "compile", "--unstable-ffi", "--unstable-webgpu", "-A", runFile)
            workingDir(runFile.parentFile.absolutePath)
        }
    }
}

@DisableCachingByDefault
abstract class DenoTestTask : AbstractTestTask() {
    init {
        this.group = "verification"
        this.dependsOn("compileTestDevelopmentExecutableKotlinJs")
    }

    init {
        this.reports {
            junitXml.outputLocation.set(project.file("build/test-results/jsDenoTest/"))
            html.outputLocation.set(project.file("build/reports/tests/jsDenoTest/"))
        }
        binaryResultsDirectory.set(project.file("build/test-results/jsDenoTest/binary"))
    }

    override fun createTestExecuter(): TestExecuter<out TestExecutionSpec> {
        return DenoTestExecuter(this.project, this.filter)
    }
    override fun createTestExecutionSpec(): TestExecutionSpec = DenoTestExecutionSpec()

    init {
        outputs.upToDateWhen { false }
    }

    class DenoTestExecuter(val project: Project, val filter: TestFilter) : TestExecuter<DenoTestExecutionSpec> {
        private fun Project.fullPathName(): String {
            //KotlinTest
            if (this.parent == null) return this.name
            return this.parent!!.fullPathName() + ":" + this.name
        }

        override fun execute(testExecutionSpec: DenoTestExecutionSpec, testResultProcessor: TestResultProcessor) {
            val baseTestFileNameBase = this.project.fullPathName().trim(':').replace(':', '-') + "-test"
            val baseTestFileName = "$baseTestFileNameBase.mjs"
            val runFile = File(this.project.rootProject.rootDir, "build/js/packages/$baseTestFileNameBase/kotlin/$baseTestFileName.deno.mjs")

            runFile.parentFile.mkdirs()
            runFile.writeText(
                //language=js
                """
                    var describeStack = []
                    globalThis.describe = (name, callback) => { describeStack.push(name); try { callback() } finally { describeStack.pop() } }
                    globalThis.it = (name, callback) => { return Deno.test({ name: describeStack.join(".") + "." + name, fn: callback}) }
                    globalThis.xit = (name, callback) => { return Deno.test({ name: describeStack.join(".") + "." + name, ignore: true, fn: callback}) }
                    function exists(path) { try { Deno.statSync(path); return true } catch (e) { return false } }
                    // Polyfill required for kotlinx-coroutines that detects window 
                    window.postMessage = (message, targetOrigin) => { const ev = new Event('message'); ev.source = window; ev.data = message; window.dispatchEvent(ev); }
                    const file = './${baseTestFileName}';
                    if (exists(file)) await import(file)
                """.trimIndent())

            val process = ProcessBuilder(buildList<String> {
                add("deno")
                add("test")
                add("--unstable-ffi")
                add("--unstable-webgpu")
                add("-A")
                if (filter.includePatterns.isEmpty()) {
                    add("--filter=${filter.includePatterns.joinToString(",")}")
                }
                add("--junit-path=${project.file("build/test-results/jsDenoTest/junit.xml").absolutePath}")
                add(runFile.absolutePath)
            }).directory(runFile.parentFile)
                .start()
            var id = 0
            val buffered = process.inputStream.bufferedReader()
            var capturingOutput = false
            var currentTestId: String? = null
            var currentTestExtra = "ok"
            var failedCount = 0

            fun flush() {
                if (currentTestId != null) {
                    try {
                        val type = when {
                            currentTestExtra.contains("skip", ignoreCase = true) || currentTestExtra.contains("ignored", ignoreCase = true) -> TestResult.ResultType.SKIPPED
                            currentTestExtra.contains("error", ignoreCase = true) || currentTestExtra.contains("failed", ignoreCase = true) -> TestResult.ResultType.FAILURE
                            currentTestExtra.contains("ok", ignoreCase = true) -> TestResult.ResultType.SUCCESS
                            else -> TestResult.ResultType.SUCCESS
                        }
                        if (type == TestResult.ResultType.FAILURE) {
                            testResultProcessor.output(currentTestId, DefaultTestOutputEvent(TestOutputEvent.Destination.StdErr, "FAILED\n"))
                            testResultProcessor.failure(currentTestId, DefaultTestFailure.fromTestFrameworkFailure(Exception("FAILED").also { it.stackTrace = arrayOf() }, null))
                            failedCount++
                        }
                        testResultProcessor.completed(currentTestId, TestCompleteEvent(System.currentTimeMillis(), type))
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    currentTestId = null
                }
            }

            testResultProcessor.started(DefaultTestSuiteDescriptor("deno", "deno"), TestStartEvent(System.currentTimeMillis()))

            for (line in buffered.lines()) {
                println("::: $line")
                when {
                    line == "------- output -------" -> {
                        capturingOutput = true
                    }
                    line == "----- output end -----" -> {
                        capturingOutput = false
                    }
                    capturingOutput -> {
                        testResultProcessor.output(currentTestId, DefaultTestOutputEvent(TestOutputEvent.Destination.StdOut, "$line\n"))
                    }
                    line.contains("...") -> {
                        flush()
                        val (name, extra) = line.split("...").map { it.trim() }
                        currentTestId = "deno.myid${id++}"

                        val descriptor = DefaultTestMethodDescriptor(currentTestId, name.substringBeforeLast('.'), name)
                        currentTestExtra = extra
                        testResultProcessor.started(
                            descriptor,
                            TestStartEvent(System.currentTimeMillis())
                        )
                    }
                }
            }
            flush()

            testResultProcessor.completed("deno", TestCompleteEvent(System.currentTimeMillis(), if (failedCount == 0) TestResult.ResultType.SUCCESS else TestResult.ResultType.FAILURE))

            process.waitFor()
            System.err.print(process.errorStream.readBytes().decodeToString())
        }

        override fun stopNow() {
        }
    }

    class DenoTestExecutionSpec : TestExecutionSpec
}
