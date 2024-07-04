package korlibs.korge.gradle.targets.js

import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.*
import java.io.*
import java.lang.management.*

internal var _webServer: DecoratedHttpServer? = null

open class RunJsServer : DefaultTask() {
    @get:Input
    var blocking: Boolean = true

    private var webBindAddress: String = "127.0.0.1"
    private var webBindPort: Int = 8083

    private lateinit var wwwFolder: File

    init {
        project.afterEvaluate {
            webBindAddress = project.korge.webBindAddress
            webBindPort = project.korge.webBindPort
            wwwFolder = File(project.buildDir, "www")
        }
    }

    @TaskAction
    open fun run() {
        if (_webServer == null) {
            val address = webBindAddress
            val port = webBindPort
            val server = staticHttpServer(wwwFolder, address = address, port = port)
            _webServer = server
            try {
                val openAddress = when (address) {
                    "0.0.0.0" -> "127.0.0.1"
                    else -> address
                }
                openBrowser("http://$openAddress:${server.port}/index.html")
                if (blocking) {
                    while (true) {
                        Thread.sleep(1000L)
                    }
                }
            } finally {
                if (blocking) {
                    println("Stopping web server...")
                    server.server.stop(0)
                    _webServer = null
                }
            }
        }
        _webServer?.updateVersion?.incrementAndGet()
    }
}

fun Project.configureDenoTest() {
    afterEvaluate {
        if (tasks.findByName("compileTestDevelopmentExecutableKotlinJs") == null) return@afterEvaluate

        val jsDenoTest = project.tasks.createThis<Exec>("jsDenoTest") {
            fun fullPathName(project: Project): String {
                if (project.parent == null) return project.name
                return fullPathName(project.parent!!) + ":" + project.name
            }
            val baseTestFileNameBase = fullPathName(project).trim(':').replace(':', '-') + "-test"
            val baseTestFileName = "$baseTestFileNameBase.mjs"

            //build\js\packages\korge-root-korge-test\kotlin

            //val runFile = file("build/compileSync/js/test/testDevelopmentExecutable/kotlin/$baseTestFileName.deno.mjs")
            val runFile = File(rootProject.rootDir, "build/js/packages/$baseTestFileNameBase/kotlin/$baseTestFileName.deno.mjs")

            // compileTestDevelopmentExecutableKotlinJs
            dependsOn("compileTestDevelopmentExecutableKotlinJs")
            //commandLine("deno", "test", "--unstable-ffi", "-A", "src/test/kotlin")

            //rootProject.
            commandLine("deno", "test", "--unstable-ffi", "--unstable-webgpu", "-A", runFile)
            workingDir(runFile.parentFile.absolutePath)

            doFirst {
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
                    const file = './$baseTestFileName';
                    if (exists(file)) await import(file)
                """.trimIndent())
            }
        }
    }
}

fun Project.configureDenoRun() {
}

fun Project.configureJavascriptRun() {
    val runJsRelease = project.tasks.createThis<RunJsServer>(name = "runJsRelease") {
        group = GROUP_KORGE_RUN
        dependsOn("browserReleaseEsbuild")
        blocking = !project.gradle.startParameter.isContinuous
    }

    val runJsDebug = project.tasks.createThis<RunJsServer>("runJsDebug") {
        group = GROUP_KORGE_RUN
        dependsOn("browserDebugEsbuild")
        blocking = !project.gradle.startParameter.isContinuous
    }

    // @TODO: jsBrowserProductionRun is much faster than jsBrowserDevelopmentRun at runtime. Why is that??
    val runJs = project.tasks.createThis<Task>("runJs") {
        group = GROUP_KORGE_RUN
        dependsOn(runJsDebug)
    }

    /*
    val runJsWebpack = project.tasks.createThis<Task>(name = "runJsWebpack") {
        group = GROUP_KORGE_RUN
        dependsOn("jsBrowserProductionRun")
    }

    val runJsWebpackDebug = project.tasks.createThis<Task>(name = "runJsWebpackDebug") {
        group = GROUP_KORGE_RUN
        dependsOn("jsBrowserDevelopmentRun")
    }

    val runJsWebpackRelease = project.tasks.createThis<Task>(name = "runJsWebpackRelease") {
        group = GROUP_KORGE_RUN
        dependsOn("jsBrowserProductionRun")
    }
    */

    val jsStopWeb = project.tasks.createThis<Task>(name = "jsStopWeb") {
        doLast {
            println("jsStopWeb: ${ManagementFactory.getRuntimeMXBean().name}-${Thread.currentThread()}")
            _webServer?.server?.stop(0)
            _webServer = null
        }
    }

    // https://blog.jetbrains.com/kotlin/2021/10/control-over-npm-dependencies-in-kotlin-js/
    allprojects {
        tasks.withType(KotlinNpmInstallTask::class.java).allThis {
            args += "--ignore-scripts"
        }
    }
}
