package com.soywiz.korlibs.modules

import com.soywiz.korlibs.get
import com.sun.net.httpserver.*
import org.gradle.api.Project
import java.io.*
import java.net.*
import java.nio.file.*
import kotlin.math.min

fun staticHttpServer(folder: File, port: Int = 0, callback: (server: HttpServer) -> Unit) {
    val absFolder = folder.absoluteFile
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    println("Listening at http://127.0.0.1:${server.address.port}/")
    server.createContext("/") { t ->
        val requested = File(folder, t.requestURI.path).absoluteFile

        if (requested.absolutePath.startsWith(absFolder.absolutePath)) {
            val req = if (requested.exists() && requested.isDirectory) requested["index.html"] else requested
            when {
                req.exists() && !req.isDirectory -> t.respond(FileContent(req))
                else -> t.respond(ByteArrayContent("<h1>404 - Not Found</h1>".toByteArray(Charsets.UTF_8), "text/html"), code = 404)
            }
        } else {
            t.respond(ByteArrayContent("<h1>500 - Internal Server Error</h1>".toByteArray(Charsets.UTF_8), "text/html"), code = 500)
        }
    }
    server.start()
    try {
        callback(server)
    } finally {
        server.stop(0)
    }
}

interface RangedContent {
    val length: Long
    val contentType: String
    fun write(out: OutputStream, range: LongRange)
}

class FileContent(val file: File) : RangedContent {
    override val length: Long by lazy { file.length() }
    override val contentType: String by lazy { file.miniMimeType() }

    override fun write(out: OutputStream, range: LongRange) {
        val len = (range.endInclusive - range.start) + 1
        //println("range=$range, len=$len")
        FileInputStream(file).use { f ->
            f.skip(range.start)
            var remaining = len
            val temp = ByteArray(64 * 1024)
            while (remaining > 0L) {
                val read = f.read(temp, 0, min(remaining, temp.size.toLong()).toInt())
                if (read <= 0) break
                //println("write $read")
                out.write(temp, 0, read)
                remaining -= read
            }
            //println("end")
        }
    }
}

class ByteArrayContent(val data: ByteArray, override val contentType: String) : RangedContent {
    override val length: Long get() = data.size.toLong()

    override fun write(out: OutputStream, range: LongRange) {
        out.write(data, range.start.toInt(), ((range.endInclusive - range.start) + 1).toInt())
    }
}

fun HttpExchange.respond(content: RangedContent, headers: List<Pair<String, String>> = listOf(), code: Int? = null) {
    try {
        val range = requestHeaders.getFirst("Range")
        val reqRange = if (range != null) {
            val rangeStr = range.removePrefix("bytes=")
            val parts = rangeStr.split("-", limit = 2)
            val start = parts.getOrNull(0)?.toLongOrNull() ?: error("Invalid request")
            val endInclusive = parts.getOrNull(1)?.toLongOrNull() ?: error("Invalid request")
            start.coerceIn(0L, content.length)..endInclusive.coerceIn(0L, content.length - 1)
        } else {
            null
        }
        val totalRange = 0L until content.length

        val partial = reqRange != null
        val length = if (partial) reqRange!!.endInclusive - reqRange.start + 1 else content.length

        responseHeaders.add("Content-Length", "$length")
        responseHeaders.add("Content-Type", content.contentType)
        responseHeaders.add("Accept-Ranges", "bytes")
        sendHeaders(headers)
        sendResponseHeaders(if (partial) 206 else code ?: 200, length)
        if (partial) {
            responseHeaders.set(
                "Content-Range",
                "bytes ${reqRange!!.start}-${reqRange!!.endInclusive}/${content.length}"
            )
        }

        // Send body if not HEAD
        if (!this.requestMethod.equals("HEAD", ignoreCase = true)) {
            //println("${this.requestMethod}")
            responseBody.use { os ->
                content.write(os, reqRange ?: totalRange)
            }
        } else {
            //println("HEAD")
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

fun HttpExchange.sendHeaders(headers: List<Pair<String, String>>) {
    for (header in headers) {
        responseHeaders.add(header.first, header.second)
    }
}

fun File.miniMimeType() = when (this.extension.toLowerCase()) {
    "htm", "html" -> "text/html"
    "css" -> "text/css"
    "txt" -> "text/plain"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    else -> if (this.exists()) Files.probeContentType(this.toPath()) ?: "application/octet-stream" else "text/plain"
}

fun Project.openBrowser(url: String) {
    exec {
        it.commandLine("open", url)
    }
}
