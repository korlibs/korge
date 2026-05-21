package korlibs.korge.gradle.targets.js

import java.io.File
import java.lang.management.ManagementFactory
import korlibs.allThis
import korlibs.korge.gradle.korge
import korlibs.korge.gradle.targets.GROUP_KORGE_RUN
import korlibs.korge.gradle.util.DecoratedHttpServer
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.openBrowser
import korlibs.korge.gradle.util.staticHttpServer
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

internal var _webServer: DecoratedHttpServer? = null

@DisableCachingByDefault
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

fun Project.fullPathName(): String {
    if (this.parent == null) return this.name
    return this.parent!!.fullPathName() + ":" + this.name
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
