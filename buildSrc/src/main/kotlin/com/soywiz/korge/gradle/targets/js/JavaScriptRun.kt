package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.*
import java.lang.management.*

internal var _webServer: DecoratedHttpServer? = null

fun Project.configureJavascriptRun() {
    fun runServer(blocking: Boolean) {
        if (_webServer == null) {
            val address = korge.webBindAddress
            val port = korge.webBindPort
            val server = staticHttpServer(project.buildDir["www"], address = address, port = port)
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

    val runJsRelease = project.addTask<Task>(name = "runJsRelease") { task ->
        task.group = GROUP_KORGE_RUN
        task.dependsOn("browserReleaseEsbuild")
        doLast {
            runServer(!project.gradle.startParameter.isContinuous)
        }
    }

    val runJsDebug = project.addTask<Task>(name = "runJsDebug") { task ->
        task.group = GROUP_KORGE_RUN
        task.dependsOn("browserDebugEsbuild")
        doLast {
            runServer(!project.gradle.startParameter.isContinuous)
        }
    }

    // @TODO: jsBrowserProductionRun is much faster than jsBrowserDevelopmentRun at runtime. Why is that??
    val runJs = project.addTask<Task>(name = "runJs") { task ->
        task.group = GROUP_KORGE_RUN
        dependsOn(runJsRelease)
    }

    val runJsWebpack = project.addTask<Task>(name = "runJsWebpack") { task ->
        task.group = GROUP_KORGE_RUN
        dependsOn("jsBrowserProductionRun")
    }

    val runJsWebpackDebug = project.addTask<Task>(name = "runJsWebpackDebug") { task ->
        task.group = GROUP_KORGE_RUN
        dependsOn("jsBrowserDevelopmentRun")
    }

    val runJsWebpackRelease = project.addTask<Task>(name = "runJsWebpackRelease") { task ->
        task.group = GROUP_KORGE_RUN
        dependsOn("jsBrowserProductionRun")
    }

    val jsStopWeb = project.addTask<Task>(name = "jsStopWeb") { task ->
        task.doLast {
            println("jsStopWeb: ${ManagementFactory.getRuntimeMXBean().name}-${Thread.currentThread()}")
            _webServer?.server?.stop(0)
            _webServer = null
        }
    }

    // https://blog.jetbrains.com/kotlin/2021/10/control-over-npm-dependencies-in-kotlin-js/
    allprojects {
        tasks.withType<KotlinNpmInstallTask> {
            args += "--ignore-scripts"
        }
    }
}
