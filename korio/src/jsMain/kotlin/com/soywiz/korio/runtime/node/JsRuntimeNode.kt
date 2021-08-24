package com.soywiz.korio.runtime.node

import com.soywiz.korio.*
import com.soywiz.korio.async.AsyncByteArrayDeque
import com.soywiz.korio.async.AsyncQueue
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.async.withContext
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.LocalVfs
import com.soywiz.korio.lang.FileAlreadyExistsException
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.lang.IOException
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.net.AsyncServer
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.runtime.JsRuntime
import com.soywiz.korio.stream.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.coroutines.*

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

typealias NodeJsBuffer = Uint8Array

fun NodeJsBuffer.toByteArray() = Int8Array(this.unsafeCast<Int8Array>()).unsafeCast<ByteArray>()
//fun ByteArray.toNodeJsBufferU8(): NodeBuffer = Uint8Array(this.unsafeCast<ArrayBuffer>()).asDynamic()

fun ByteArray.asInt8Array(): Int8Array = this.unsafeCast<Int8Array>()
fun ByteArray.asUint8Array(): Uint8Array {
    val i = this.asInt8Array()
    return Uint8Array(i.buffer, i.byteOffset, i.length)
}

fun ByteArray.toNodeJsBuffer(): NodeJsBuffer = this.asUint8Array().unsafeCast<NodeJsBuffer>()
fun ByteArray.toNodeJsBuffer(offset: Int, size: Int): NodeJsBuffer =
    jsGlobal.asDynamic().Buffer.from(this, offset, size).unsafeCast<NodeJsBuffer>()

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//private external val require: dynamic
//private val require_req: dynamic by lazy { require }
//private fun require_node(name: String): dynamic = require_req(name)

// DIRTY HACK to prevent webpack to mess with our code
val REQ get() = "req"
private external val eval: dynamic
private fun require_node(name: String): dynamic = eval("(${REQ}uire('$name'))")

private external val process: dynamic // node.js

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

object JsRuntimeNode : JsRuntime() {
    val nodeProcess get() = process
    override val rawOsName: String = process.platform.unsafeCast<String>()
    private val fs by lazy { require_node("fs") }
    private val path by lazy { require_node("path") }

    override fun existsSync(path: String): Boolean = fs.existsSync(path)
    override fun currentDir(): String = path.resolve(".")
    override fun env(key: String): String? = process.env[key]
    override fun envs() = jsObjectToMap(process.env)
    override fun openVfs(path: String): VfsFile = NodeJsLocalVfs()[path]

    override suspend fun createClient(secure: Boolean): AsyncClient = NodeJsAsyncClient(coroutineContext)
    override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
        NodeJsAsyncServer().init(port, host, backlog)

    override fun createHttpClient(): HttpClient = HttpClientNodeJs()
    override fun createHttpServer(): HttpServer = HttpSeverNodeJs()
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private class NodeJsAsyncClient(val coroutineContext: CoroutineContext) : AsyncClient {
    private val net = require_node("net")
    private var connection: dynamic = null
    private val input = AsyncByteArrayDeque()

    override var connected: Boolean = false; private set
    private val task = AsyncQueue().withContext(coroutineContext)

    override suspend fun connect(host: String, port: Int): Unit = suspendCoroutine { c ->
        connection = net.createConnection(port, host) {
            connected = true
            connection?.pause()
            connection?.on("data") { it ->
                val bytes = it.unsafeCast<ByteArray>().copyOf()
                task {
                    input.write(bytes)
                }
            }
            c.resume(Unit)
        }
        Unit
    }

    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        connection?.resume()
        try {
            return input.read(buffer, offset, len)
        } finally {
            connection?.pause()
        }
    }

    override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutine { c ->
        connection?.write(buffer.toNodeJsBuffer(offset, len)) {
            c.resume(Unit)
        }
        Unit
    }

    override suspend fun close() {
        connection?.close()
    }
}

private class NodeJsAsyncServer : AsyncServer {
    override val requestPort: Int
        get() = TODO("not implemented")
    override val host: String
        get() = TODO("not implemented")
    override val backlog: Int
        get() = TODO("not implemented")
    override val port: Int
        get() = TODO("not implemented")

    override suspend fun accept(): AsyncClient {
        TODO("not implemented")
    }

    suspend fun init(port: Int, host: String, backlog: Int): AsyncServer = this.apply {
    }

    override suspend fun close() = Unit
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private external interface NodeFD

private external interface NodeFileStat {
    val size: Double
}

private external interface NodeFS {
    fun mkdir(path: String, mode: Int, callback: (Error?) -> Unit)
    fun rename(src: String, dst: String, callback: (Error?) -> Unit)
    fun unlink(path: String, callback: (Error?) -> Unit)
    fun rmdir(path: String, callback: (Error?) -> Unit)

    fun open(path: String, cmode: String, callback: (Error?, NodeFD?) -> Unit)
    fun read(fd: NodeFD?, buffer: NodeJsBuffer, offset: Int, len: Int, position: Double, callback: (Error?, Int, NodeJsBuffer) -> Unit)
    fun write(fd: NodeFD?, buffer: NodeJsBuffer, offset: Int, len: Int, position: Double, callback: (Error?, Int, NodeJsBuffer) -> Unit)
    fun ftruncate(fd: NodeFD?, length: Double, callback: (Error?) -> Unit)
    fun fstat(fd: NodeFD?, callback: (Error?, NodeFileStat) -> Unit)
    fun close(fd: NodeFD?, callback: (Error?) -> Unit)
}

private val nodeFS: NodeFS by lazy { require_node("fs") }

private class NodeJsLocalVfs : LocalVfs() {
    private fun getFullPath(path: String): String {
        return path.pathInfo.normalize()
    }

    override suspend fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int {
        val process = require_node("child_process").spawn(cmdAndArgs.first(), cmdAndArgs.drop(1).toTypedArray(), jsObject(
            "cwd" to path,
            "env" to env.toJsObject(),
            "encoding" to "buffer",
            "shell" to true
        ))

        val queue = AsyncQueue().withContext(coroutineContext)
        val exitCodeDeferred = CompletableDeferred<Int>()

        process.stdout.on("data") { data: NodeJsBuffer -> queue { handler.onOut(data.toByteArray()) } }
        process.stderr.on("data") { data: NodeJsBuffer -> queue { handler.onErr(data.toByteArray()) } }
        process.on("close") { code: Int -> exitCodeDeferred.complete(code) }

        return exitCodeDeferred.await()
    }

    override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = suspendCoroutine { c ->
        nodeFS.mkdir(getFullPath(path), "777".toInt(8)) { err ->
            c.resume((err == null))
            Unit
        }
        Unit
    }

    override suspend fun rename(src: String, dst: String): Boolean = suspendCoroutine { c ->
        nodeFS.rename(getFullPath(src), getFullPath(dst)) { err ->
            c.resume((err == null))
            Unit
        }
        Unit
    }

    override suspend fun delete(path: String): Boolean = suspendCoroutine { c ->
        nodeFS.unlink(getFullPath(path)) { err ->
            c.resume((err == null))
            Unit
        }
        Unit
    }

    override suspend fun rmdir(path: String): Boolean = suspendCoroutine { c ->
        nodeFS.rmdir(getFullPath(path)) { err ->
            c.resume((err == null))
            Unit
        }
        Unit
    }

    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        val cmode = when (mode) {
            VfsOpenMode.READ -> "r"
            VfsOpenMode.WRITE -> "r+"
            VfsOpenMode.CREATE_OR_TRUNCATE -> "w+"
            VfsOpenMode.CREATE_NEW -> {
                if (stat(path).exists) throw FileAlreadyExistsException(path)
                "w+"
            }
            VfsOpenMode.CREATE -> "wx+"
            VfsOpenMode.APPEND -> "a"
        }

        return _open(path, cmode)
    }

    suspend fun _open(path: String, cmode: String): AsyncStream {
        val file = this.file(path)
        return suspendCoroutine { cc ->
            nodeFS.open(getFullPath(path), cmode) { err: Error?, fd: NodeFD? ->
                //println("OPENED path=$path, cmode=$cmode, err=$err, fd=$fd")
                if (err != null || fd == null) {
                    cc.resumeWithException(FileNotFoundException("Can't open '$path' with mode '$cmode': err=$err"))
                } else {
                    cc.resume(NodeFDStream(file, fd).toAsyncStream())
                }
                Unit
            }
            Unit
        }
    }

    override fun toString(): String = "NodeJsLocalVfs"
}

private class NodeFDStream(val file: VfsFile, var fd: NodeFD?) : AsyncStreamBase() {
    private fun checkFd() {
        if (fd == null) error("File $file already closed")
    }

    override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutine { c ->
        checkFd()
        nodeFS.read(fd, buffer.toNodeJsBuffer(), offset, len, position.toDouble()) { err, bytesRead, buf ->
            if (err != null) {
                c.resumeWithException(IOException("Error reading from $file :: err=$err"))
            } else {
                //println("NODE READ[$file] read: ${bytesRead} : ${buffer.sliceArray(0 until min(buffer.size, 5)).contentToString()}")
                c.resume(bytesRead)
            }
            Unit
        }
        Unit
    }

    override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutine { c ->
        checkFd()
        nodeFS.write(fd, buffer.toNodeJsBuffer(), offset, len, position.toDouble()) { err, bytesWritten, buffer ->
            if (err != null) {
                c.resumeWithException(IOException("Error writting to $file :: err=$err"))
            } else {
                c.resume(Unit)
            }
            Unit
        }
        Unit
    }

    override suspend fun setLength(value: Long): Unit = suspendCoroutine { c ->
        checkFd()
        nodeFS.ftruncate(fd, value.toDouble()) { err ->
            if (err != null) {
                c.resumeWithException(IOException("Error setting length to $file :: err=$err"))
            } else {
                c.resume(Unit)
            }
            Unit
        }
        Unit
    }

    override suspend fun getLength(): Long = suspendCoroutine { c ->
        checkFd()
        nodeFS.fstat(fd) { err, stats ->
            if (err != null) {
                c.resumeWithException(IOException("Error getting length from $file :: err=$err"))
            } else {
                //println("NODE READ getLength: ${stats.size}")
                c.resume(stats.size.toLong())
            }
            Unit
        }
        Unit
    }

    //private var closed = false

    override suspend fun close(): Unit {
        //if (closed) error("File already closed")
        //closed = true
        if (fd != null) {
            return suspendCoroutine { c ->
                nodeFS.close(fd) { err ->
                    fd = null
                    if (err != null) {
                        //c.resumeWithException(IOException("Error closing err=$err"))
                        c.resume(Unit) // Allow to close several times
                    } else {
                        c.resume(Unit)
                    }
                    Unit
                }
                Unit
            }
        }
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private class HttpClientNodeJs : HttpClient() {
    override suspend fun requestInternal(
        method: Http.Method,
        url: String,
        headers: Http.Headers,
        content: AsyncStream?
    ): Response {
        val deferred = CompletableDeferred<Response>(Job())
        //println(url)

        val http = require_node("http")
        val jsurl = require_node("url")
        val info = jsurl.parse(url)
        val reqHeaders = jsEmptyObj()

        for (header in headers) {
            reqHeaders[header.first] = header.second
        }

        val req = jsEmptyObj()
        req.method = method.name
        req.host = info["hostname"]
        req.port = info["port"]
        req.path = info["path"]
        req.agent = false
        req.encoding = null
        req.headers = reqHeaders

        val r = http.request(req) { res ->
            val statusCode: Int = res.statusCode
            val statusMessage: String = res.statusMessage ?: ""
            val jsHeadersObj = res.headers
            val body = jsEmptyArray()
            res.on("data") { d -> body.push(d) }
            res.on("end") {
                val r = jsGlobal.asDynamic().Buffer.concat(body)
                val u8array = Int8Array(r.unsafeCast<ArrayBuffer>())
                val out = ByteArray(u8array.length)
                for (n in 0 until u8array.length) out[n] = u8array[n]
                val response = Response(
                    status = statusCode,
                    statusText = statusMessage,
                    headers = Http.Headers(
                        (jsToObjectMap(jsHeadersObj) ?: LinkedHashMap()).mapValues { "${it.value}" }
                    ),
                    rawContent = out.openAsync(),
                    content = out.openAsync()
                )

                //println(response.headers)

                deferred.complete(response)
            }
        }.on("error") { e ->
            deferred.completeExceptionally(kotlin.RuntimeException("Error: $e"))
        }

        deferred.invokeOnCompletion {
            if (deferred.isCancelled) {
                r.abort()
            }
        }

        if (content != null) {
            r.end(content.readAll().toTypedArray())
        } else {
            r.end()
        }

        return deferred.await()
    }
}

private class HttpSeverNodeJs : HttpServer() {
    private var context: CoroutineContext = EmptyCoroutineContext
    private var handler: suspend (req: dynamic, res: dynamic) -> Unit = { req, res -> }

    val http = require_node("http")
    val server = http.createServer { req, res ->
        launchImmediately(context) {
            handler(req, res)
        }
    }

    override suspend fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
        super.websocketHandlerInternal(handler)
    }

    override suspend fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
        context = coroutineContext
        this.handler = { req, res ->
            // req: https://nodejs.org/api/http.html#http_class_http_incomingmessage
            // res: https://nodejs.org/api/http.html#http_class_http_serverresponse

            val method = Http.Method[req.method.unsafeCast<String>()]
            val url = req.url.unsafeCast<String>()
            val headers = Http.Headers(jsToArray(req.rawHeaders).map { "$it" }.zipWithNext())
            handler(object : Request(method, url, headers, RequestConfig()) {
                override suspend fun _handler(handler: (ByteArray) -> Unit) {
                    req.on("data") { chunk ->
                        handler(Int8Array(chunk.unsafeCast<Uint8Array>().buffer).unsafeCast<ByteArray>())
                    }
                }

                override suspend fun _endHandler(handler: () -> Unit) {
                    req.on("end") {
                        handler()
                    }
                    req.on("error") {
                        handler()
                    }
                }

                override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
                    res.statusCode = code
                    res.statusMessage = message
                    for (header in headers) {
                        res.setHeader(header.first, header.second)
                    }
                }

                override suspend fun _write(data: ByteArray, offset: Int, size: Int): Unit = suspendCoroutine { c ->
                    res.write(data.toNodeJsBuffer(offset, size)) {
                        c.resume(Unit)
                    }
                    Unit
                }

                override suspend fun _end(): Unit = suspendCoroutine { c ->
                    res.end {
                        c.resume(Unit)
                    }
                    Unit
                }
            })
        }
    }

    override suspend fun listenInternal(port: Int, host: String) = suspendCoroutine<Unit> { c ->
        context = c.context
        server.listen(port, host, 511) {
            c.resume(Unit)
        }
    }

    override val actualPort: Int
        get() {
            //com.soywiz.korio.lang.Console.log(server)
            return jsEnsureInt(server.address().port)
        }

    override suspend fun closeInternal() = suspendCoroutine<Unit> { c ->
        context = c.context
        server.close {
            c.resume(Unit)
        }
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
