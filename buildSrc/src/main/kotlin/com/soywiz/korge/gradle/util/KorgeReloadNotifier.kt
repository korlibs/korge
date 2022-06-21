package com.soywiz.korge.gradle.util

import com.soywiz.korge.gradle.util.KDynamic.Companion.toLongOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object KorgeReloadNotifier {
    @JvmStatic
    fun beforeBuild(timeBeforeCompilationFile: File) {
        timeBeforeCompilationFile.writeText("${System.currentTimeMillis()}")
    }

    @JvmStatic
    fun afterBuild(timeBeforeCompilationFile: File, httpPort: Int) {
        val startTime = timeBeforeCompilationFile.readText().toLongOrNull() ?: 0L
        val endTime = System.currentTimeMillis()
        val url = "http://127.0.0.1:$httpPort/?startTime=$startTime&endTime=$endTime"
        try {
            println("REPLY FROM KORGE REFRESH: " + URL(url).readText() + " :: $url")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
