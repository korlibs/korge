package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import kotlin.coroutines.*

// @TODO: Try to prevent webpack to not get confused about this
private external val require: dynamic
//private external fun require(name: String): dynamic
private val require_req: dynamic by lazy { require }
internal fun require_node(name: String): dynamic = require_req(name)

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
	global.asDynamic().Buffer.from(this, offset, size).unsafeCast<NodeJsBuffer>()

class HttpClientNodeJs : HttpClient() {
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
				val r = global.asDynamic().Buffer.concat(body)
				val u8array = Int8Array(r.unsafeCast<ArrayBuffer>())
				val out = ByteArray(u8array.length)
				for (n in 0 until u8array.length) out[n] = u8array[n]
				val response = Response(
					status = statusCode,
					statusText = statusMessage,
					headers = Http.Headers(
						(jsToObjectMap(jsHeadersObj) ?: LinkedHashMap()).mapValues { "${it.value}" }
					),
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

class HttpSeverNodeJs : HttpServer() {
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

class NodeJsAsyncClient(val coroutineContext: CoroutineContext) : AsyncClient {
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

class NodeJsAsyncServer : AsyncServer {
	override val requestPort: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val host: String
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val backlog: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val port: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

	override suspend fun accept(): AsyncClient {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	suspend fun init(port: Int, host: String, backlog: Int): AsyncServer = this.apply {
	}

    override suspend fun close() = Unit
}


class NodeJsLocalVfs : LocalVfs() {
	val fs = require_node("fs")

	interface FD

	private fun getFullPath(path: String): String {
		return path.pathInfo.normalize()
	}


	//fun String.escapeShellCmd(): String {
	//	// @TODO: escapeShellArg @TODO: Consider all cases and windows
	//	return this
	//}
	//
	//fun String.escapeShellArg(): String {
	//	// @TODO: escapeShellArg @TODO: Consider all cases and windows
	//	return "'" + this.replace("'", "\'") + "'"
	//}

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
		process.stderr.on("data") { data:  NodeJsBuffer -> queue { handler.onErr(data.toByteArray()) } }
		process.on("close") { code: Int -> exitCodeDeferred.complete(code) }

		return exitCodeDeferred.await()
	}

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = suspendCoroutine { c ->
		fs.mkdir(getFullPath(path), "777".toInt(8)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	override suspend fun rename(src: String, dst: String): Boolean = suspendCoroutine { c ->
		fs.rename(getFullPath(src), getFullPath(dst)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	override suspend fun delete(path: String): Boolean = suspendCoroutine { c ->
		fs.unlink(getFullPath(path)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	override suspend fun rmdir(path: String): Boolean = suspendCoroutine { c ->
		fs.rmdir(getFullPath(path)) { err ->
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
			fs.open(getFullPath(path), cmode) { err: Any?, fd: FD? ->
				//println("OPENED path=$path, cmode=$cmode, err=$err, fd=$fd")
				if (err != null || fd == null) {
					cc.resumeWithException(FileNotFoundException("Can't open '$path' with mode '$cmode': err=$err"))
				} else {
					cc.resume(NodeFDStream(file, fs, fd).toAsyncStream())
				}
				Unit
			}
			Unit
		}
	}

	override fun toString(): String = "NodeJsLocalVfs"
}

class NodeFDStream(val file: VfsFile, val fs: dynamic, var fd: NodeJsLocalVfs.FD?) : AsyncStreamBase() {
	private fun checkFd() {
		if (fd == null) error("File $file already closed")
	}

	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutine { c ->
		checkFd()
		fs.read(fd, buffer.toNodeJsBuffer(), offset, len, position.toDouble()) { err, bytesRead, buf ->
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
		fs.write(fd, buffer.toNodeJsBuffer(), offset, len, position.toDouble()) { err, bytesWritten, buffer ->
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
		fs.ftruncate(fd, value.toDouble()) { err ->
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
		fs.fstat(fd) { err, stats ->
			if (err != null) {
				c.resumeWithException(IOException("Error getting length from $file :: err=$err"))
			} else {
				//println("NODE READ getLength: ${stats.size}")
				c.resume((stats.size as Double).toLong())
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
				fs.close(fd) { err ->
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
