package korlibs.io.net.http

import korlibs.datastructure.Extra
import korlibs.memory.ByteArrayBuilder
import korlibs.io.async.AsyncCloseable
import korlibs.io.async.launchImmediately
import korlibs.io.file.VfsFile
import korlibs.io.lang.Charset
import korlibs.io.lang.IOException
import korlibs.io.lang.UTF8
import korlibs.io.lang.invalidOp
import korlibs.io.lang.toByteArray
import korlibs.io.lang.toString
import korlibs.io.net.AsyncAddress
import korlibs.io.net.QueryString
import korlibs.io.net.ws.WsCloseInfo
import korlibs.io.stream.AsyncGetLengthStream
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncOutputStream
import korlibs.io.stream.EMPTY_BYTE_ARRAY
import korlibs.io.stream.copyTo
import korlibs.io.stream.slice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

open class HttpServer protected constructor() : AsyncCloseable {
	companion object {
		operator fun invoke() = defaultHttpFactory.createServer()
	}

	abstract class BaseRequest(
		val uri: String,
		val headers: Http.Headers
	) : Extra by Extra.Mixin() {
		private val parts by lazy { uri.split('?', limit = 2) }
		val path: String by lazy { parts[0] }
		val queryString: String by lazy { parts.getOrElse(1) { "" } }
		val getParams by lazy { QueryString.decode(queryString) }
		val absoluteURI: String by lazy { uri }
	}

	abstract class WsRequest(
		uri: String,
		headers: Http.Headers,
		val scope: CoroutineScope
	) : BaseRequest(uri, headers) {
        open val address: AsyncAddress get() = AsyncAddress("0.0.0.0", 0)

        abstract fun reject()
		open fun accept(headers: Http.Headers) = Unit

		abstract fun close()
		abstract fun onStringMessage(handler: suspend (String) -> Unit)
		abstract fun onBinaryMessage(handler: suspend (ByteArray) -> Unit)
		abstract fun onClose(handler: suspend (WsCloseInfo) -> Unit)
		abstract fun send(msg: String)
		abstract fun send(msg: ByteArray)

		fun sendSafe(msg: String) {
			try {
				send(msg)
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}

		fun sendSafe(msg: ByteArray) {
			try {
				send(msg)
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}

		//suspend fun stringMessageStream(): SuspendingSequence<String> {
		//	val emitter = AsyncSequenceEmitter<String>()
		//	onStringMessage { emitter.emit(it) }
		//	onClose { emitter.close() }
		//	return emitter.toSequence()
		//}

		fun stringMessageStream() = scope.produce<String> {
			onStringMessage { send(it) }
			onClose { close() }
		}

		fun binaryMessageStream() = scope.produce<ByteArray> {
			onBinaryMessage { send(it) }
			onClose { close() }
		}

		fun anyMessageStream() = scope.produce<Any> {
			onStringMessage { send(it) }
			onBinaryMessage { send(it) }
			onClose { close() }
		}
	}

	val requestConfig = RequestConfig()

	data class RequestConfig(
		val beforeSendHeadersInterceptors: MutableMap<String, suspend (Request) -> Unit> = LinkedHashMap()
	) : Extra by Extra.Mixin() {
		// TODO:
		fun registerComponent(component: Any, dependsOn: List<Any>): Unit = TODO()
	}

	abstract class Request constructor(
		val method: Http.Method,
		uri: String,
		headers: Http.Headers,
		val requestConfig: RequestConfig = RequestConfig()
	) : BaseRequest(uri, headers), AsyncOutputStream {
		val finalizers = arrayListOf<suspend () -> Unit>()

		fun getHeader(key: String): String? = headers[key]

		fun getHeaderList(key: String): List<String> = headers.getAll(key)

		private var headersSent = false
		private var finalizingHeaders = false
		private val resHeaders = ArrayList<Pair<String, String>>()
		private var code: Int = 200
		private var message: String = "OK"

		private fun ensureHeadersNotSent() {
			if (headersSent) {
				println("Sent headers: $resHeaders")
				throw IOException("Headers already sent")
			}
		}

		fun removeHeader(key: String) {
			ensureHeadersNotSent()
			resHeaders.removeAll { it.first.equals(key, ignoreCase = true) }
		}

		fun addHeader(key: String, value: String) {
			ensureHeadersNotSent()
			resHeaders += key to value
		}

		fun replaceHeader(key: String, value: String) {
			ensureHeadersNotSent()
			removeHeader(key)
			addHeader(key, value)
		}

        protected open val _output: AsyncOutputStream by lazy { object : AsyncOutputStream {
            override suspend fun write(buffer: ByteArray, offset: Int, len: Int) = _write(buffer, offset, len)
            override suspend fun close() = _end()
        } }
		protected abstract suspend fun _handler(handler: (ByteArray) -> Unit)
		protected abstract suspend fun _endHandler(handler: () -> Unit)
		protected abstract suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers)
		protected abstract suspend fun _write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset)
		protected abstract suspend fun _end()

		suspend fun handler(handler: (ByteArray) -> Unit) {
			_handler(handler)
		}

		suspend fun endHandler(handler: () -> Unit) {
			_endHandler(handler)
		}

		suspend fun readRawBody(maxSize: Int = 0x1000): ByteArray = suspendCancellableCoroutine { c ->
			val out = ByteArrayBuilder()
			launchImmediately(c.context) {
				handler {
					if (out.size + it.size > maxSize) {
						out.clear()
					} else {
						out.append(it)
					}
				}
				endHandler {
					c.resume(out.toByteArray())
				}
			}
            c.invokeOnCancellation {
                //c.cancel()
            }
		}

		fun setStatus(code: Int, message: String = HttpStatusMessage(code)) {
			ensureHeadersNotSent()
			this.code = code
			this.message = message
		}

		private suspend fun flushHeaders() {
			//println("flushHeaders")
			if (headersSent) return
			if (finalizingHeaders) invalidOp("Can't write while finalizing headers")
			finalizingHeaders = true
			for (interceptor in requestConfig.beforeSendHeadersInterceptors) {
				interceptor.value(this)
			}
			headersSent = true
			//println("----HEADERS-----\n" + resHeaders.joinToString("\n"))
			_sendHeader(this.code, this.message, Http.Headers(resHeaders))
		}

		override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
			flushHeaders()
			_write(buffer, offset, len)
		}

		suspend fun end() {
			//println("END")
			flushHeaders()
			_end()
			for (finalizer in finalizers) finalizer()
		}

        suspend fun end(file: VfsFile) {
            file.openUse { end(this) }
        }

        suspend fun end(file: VfsFile, range: LongRange) {
            file.openUse { end(this.slice(range)) }
        }

        suspend fun end(stream: AsyncInputStream) {
            if (stream is AsyncGetLengthStream) {
                replaceHeader(Http.Headers.ContentLength, "${stream.getLength()}")
                flushHeaders()
            }
            stream.copyTo(_output)
            end()
        }

		suspend fun end(data: ByteArray, offset: Int = 0, size: Int = data.size - offset) {
			replaceHeader(Http.Headers.ContentLength, "$size")
			flushHeaders()
			_write(data, offset, size)
			end()
		}

		suspend fun write(data: String, charset: Charset = UTF8) {
			flushHeaders()
			_write(data.toByteArray(charset))
		}

		suspend fun end(data: String, charset: Charset = UTF8) {
			end(data.toByteArray(charset))
		}

		override suspend fun close() {
			end()
		}
	}

    protected open suspend fun errorHandlerInternal(handler: suspend (Throwable) -> Unit) {
    }

    protected open suspend fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
	}

	protected open suspend fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
	}

	suspend fun allHandler(handler: suspend (BaseRequest) -> Unit) = this.apply {
		websocketHandler { handler(it) }
		httpHandler { handler(it) }
	}

	protected open suspend fun listenInternal(port: Int, host: String = "127.0.0.1") {
		val deferred = CompletableDeferred<Unit>(Job())
        //onListening(port)
		deferred.await()
	}

	open val actualPort: Int get() = 0
    open val actualHost: String get() = "127.0.0.1"
    //val onListening = Signal<Int>()

	protected open suspend fun closeInternal() {
	}

    suspend fun errorHandler(handler: suspend (Throwable) -> Unit): HttpServer {
        errorHandlerInternal(handler)
        return this
    }

    suspend fun websocketHandler(handler: suspend (WsRequest) -> Unit): HttpServer {
		websocketHandlerInternal(handler)
		return this
	}

	suspend fun httpHandler(handler: suspend (Request) -> Unit): HttpServer {
		httpHandlerInternal(handler)
		return this
	}

	suspend fun listen(port: Int = 0, host: String = "127.0.0.1"): HttpServer {
		listenInternal(port, host)
		return this
	}

	suspend fun listen(port: Int = 0, host: String = "127.0.0.1", handler: suspend (Request) -> Unit): HttpServer {
		httpHandler(handler)
		listen(port, host)
		return this
	}

	final override suspend fun close() {
		closeInternal()
	}
}

class FakeHttpServerRequest(
	method: Http.Method,
	uri: String,
	headers: Http.Headers = Http.Headers(),
	val body: ByteArray = EMPTY_BYTE_ARRAY,
	requestConfig: HttpServer.RequestConfig
) : HttpServer.Request(method, uri, headers, requestConfig) {
	private val buf = ByteArrayBuilder()
	var outputHeaders: Http.Headers = Http.Headers()
	var outputStatusCode: Int = 0
	var outputStatusMessage: String = ""
	var output: String = ""
	val log = arrayListOf<String>()

	override suspend fun _handler(handler: (ByteArray) -> Unit) {
		log += "_handler()"
		handler(body)
	}

	override suspend fun _endHandler(handler: () -> Unit) {
		log += "_endHandler()"
		handler()
	}

	override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
		log += "_setStatus($code, $message)"
		outputStatusCode = code
		outputStatusMessage = message
		log += "_sendHeaders($headers)"
		outputHeaders = headers
	}

	override suspend fun _write(data: ByteArray, offset: Int, size: Int) {
		log += "_write(${data.copyOfRange(offset, offset + size).toString(UTF8)})"
		buf.append(data, offset, size)
	}

	override suspend fun _end() {
		log += "_end()"
		output = buf.toByteArray().toString(UTF8)
	}

	override fun toString(): String = "$outputStatusCode:$outputStatusMessage:$outputHeaders:$output"
}
