package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korlibs.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.*
import java.io.*
import java.lang.management.*

internal var _webServer: DecoratedHttpServer? = null

fun Project.configureJavascriptRun() {
    fun runServer(blocking: Boolean) {
        if (_webServer == null) {
            val address = korge.webBindAddress
            val port = korge.webBindPort
            val server = staticHttpServer(File(project.buildDir, "www"), address = address, port = port)
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

    val runJsRelease = project.tasks.createThis<Task>(name = "runJsRelease") {
        group = GROUP_KORGE_RUN
        dependsOn("browserReleaseEsbuild")
        doLast {
            runServer(!project.gradle.startParameter.isContinuous)
        }
    }

    val runJsDebug = project.tasks.createThis<Task>("runJsDebug") {
        group = GROUP_KORGE_RUN
        dependsOn("browserDebugEsbuild")
        doLast {
            runServer(!project.gradle.startParameter.isContinuous)
        }
    }

    // @TODO: jsBrowserProductionRun is much faster than jsBrowserDevelopmentRun at runtime. Why is that??
    val runJs = project.tasks.createThis<Task>("runJs") {
        group = GROUP_KORGE_RUN
        dependsOn(runJsRelease)
    }

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
