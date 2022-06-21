package com.soywiz.korge.gradle.util

import com.soywiz.korge.gradle.util.KDynamic.Companion.toLongOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object KorgeReloadNotifier {
    @JvmStatic
    fun beforeBuild(timeBeforeCompilationFile: File) {
        timeBeforeCompilationFile.writeText("${System.currentTimeMillis()}")
    }

    @JvmStatic
    fun afterBuild(timeBeforeCompilationFile: File, httpPort: Int) {
        val startTime = timeBeforeCompilationFile.readText().toLongOrNull() ?: 0L
        val endTime = System.currentTimeMillis()
        try {
            println("REPLY FROM SERVER: " + URL("http://127.0.0.1:$httpPort/?startTime=$startTime&endTime=$endTime").readText())
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        //println("NOTIFY_AND_DO_REFRESH: $startTime->${System.currentTimeMillis()}")
        //val bytes = ByteArray(8)
        //val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer()
        //data.put(0, startTime)
        //DatagramSocket(21_111, InetAddress.getByName("127.0.0.1")).send(DatagramPacket(bytes, bytes.size))
    }
}
