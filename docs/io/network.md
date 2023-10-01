---
permalink: /io/network/
group: io
layout: default
title: Network
title_prefix: KorIO
description: "TCP sockets, HTTP and WebSocket client and server, URL, QueryString, MimeType utils..."
fa-icon: fa-network-wired
priority: 10
---

KorIO has utilities for handling network.



## MimeType

```kotlin
fun VfsFile.mimeType(): MimeType

class MimeType(val mime: String, val exts: List<String>) : Vfs.Attribute {
	companion object {
		val APPLICATION_OCTET_STREAM = MimeType("application/octet-stream", listOf("bin"))
		val APPLICATION_JSON = MimeType("application/json", listOf("json"))
		val IMAGE_PNG = MimeType("image/png", listOf("png"))
		val IMAGE_JPEG = MimeType("image/jpeg", listOf("jpg", "jpeg"))
		val IMAGE_GIF = MimeType("image/gif", listOf("gif"))
		val TEXT_HTML = MimeType("text/html", listOf("htm", "html"))
		val TEXT_PLAIN = MimeType("text/plain", listOf("txt", "text"))
		val TEXT_CSS = MimeType("text/css", listOf("css"))
		val TEXT_JS = MimeType("application/javascript", listOf("js"))

		fun register(mimeType: MimeType)
		fun register(vararg mimeTypes: MimeType)
		fun register(mime: String, vararg exsts: String)

		fun getByExtension(ext: String, default: MimeType
	}
}
```

## QueryString

```kotlin
object QueryString {
	fun decode(str: CharSequence): Map<String, List<String>>
	fun encode(map: Map<String, List<String>>): String
	fun encode(vararg items: Pair<String, String>): String
}
```

## HostWithPort

```kotlin
data class HostWithPort(val host: String, val port: Int) {
	companion object {
		fun parse(str: String, defaultPort: Int): HostWithPort
	}
}
```

## URL

```kotlin
fun createBase64URLForData(data: ByteArray, contentType: String): String

fun URL(url: String): URL
fun URL(
    scheme: String?,
    userInfo: String?,
    host: String?,
    path: String,
    query: String?,
    fragment: String?,
    opaque: Boolean = false,
    port: Int = DEFAULT_PORT
): URL

data class URL {
    val isOpaque: Boolean,
    val scheme: String?,
    val userInfo: String?,
    val host: String?,
    val path: String,
    val query: String?,
    val fragment: String?,
    val defaultPort: Int

    val user: String?
    val password: String?
    val isHierarchical: Boolean

    val port: Int
    val fullUrl: String

    val fullUrlWithoutScheme: String
    val pathWithQuery: String

    fun toUrlString(includeScheme: Boolean = true, out: StringBuilder = StringBuilder()): StringBuilder

    val isAbsolute: Boolean

    override fun toString(): String
    fun toComponentString(): String
    fun resolve(path: URL): URL

    companion object {
        val DEFAULT_PORT = 0
        fun isAbsolute(url: String): Boolean
        fun resolve(base: String, access: String): String
        fun decodeComponent(s: String, charset: Charset = UTF8, formUrlEncoded: Boolean = false): String
        fun encodeComponent(s: String, charset: Charset = UTF8, formUrlEncoded: Boolean = false): String
    }
}
```

## TCP Client and Server

```kotlin
suspend fun createTcpClient(secure: Boolean = false): AsyncClient
suspend fun createTcpServer(port: Int = AsyncServer.ANY_PORT, host: String = "127.0.0.1", backlog: Int = 511, secure: Boolean = false): AsyncServer
suspend fun createTcpClient(host: String, port: Int, secure: Boolean = false): AsyncClient

interface AsyncClient : AsyncInputStream, AsyncOutputStream, AsyncCloseable {
    val connected: Boolean
    suspend fun connect(host: String, port: Int)
    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int)
    override suspend fun close()

    object Stats {
        val writeCountStart: AtomicLong
        val writeCountEnd: AtomicLong
        val writeCountError: AtomicLong
    }

    companion object {
        suspend operator fun invoke(host: String, port: Int, secure: Boolean = false): AsyncClient
        suspend fun create(secure: Boolean = false): AsyncClient
        suspend fun createAndConnect(host: String, port: Int, secure: Boolean = false): AsyncClient
    }
}

interface AsyncServer {
    val requestPort: Int
    val host: String
    val backlog: Int
    val port: Int

    companion object {
        val ANY_PORT = 0
        suspend operator fun invoke(port: Int, host: String = "127.0.0.1", backlog: Int = -1): AsyncServer
    }

    suspend fun accept(): AsyncClient
    suspend fun listen(handler: suspend (AsyncClient) -> Unit): Closeable
    suspend fun listen(): ReceiveChannel<AsyncClient>
}
```

## Http Client and Server

### Common

```kotlin
interface Http {
    companion object {
        val Date = DateFormat("EEE, dd MMM yyyy HH:mm:ss z")
        fun TemporalRedirect(uri: String): RedirectException
        fun PermanentRedirect(uri: String): RedirectException
    }

    enum class Methods : Method {
        ALL, OPTIONS, GET, HEAD,
        POST, PUT, DELETE,
        TRACE, CONNECT, PATCH,
    }

    interface Method {
        val name: String

        companion object {
            val OPTIONS = Methods.OPTIONS
            val GET = Methods.GET
            val HEAD = Methods.HEAD
            val POST = Methods.POST
            val PUT = Methods.PUT
            val DELETE = Methods.DELETE
            val TRACE = Methods.TRACE
            val CONNECT = Methods.CONNECT
            val PATCH = Methods.PATCH

            fun values(): List<Method>
            val valuesMap: Map<String, Method>

            operator fun get(name: String): Method
            operator fun invoke(name: String): Method = this[name]
        }
    }

    data class CustomMethod(val _name: String) : Method {
        val nameUC: String
        override val name: String
        override fun toString(): String
    }

    open class HttpException(
        val statusCode: Int,
        val msg: String = "Error$statusCode",
        val statusText: String = HttpStatusMessage.CODES[statusCode] ?: "Error$statusCode",
        val headers: Http.Headers = Http.Headers()
    ) : IOException("$statusCode $statusText - $msg") {
        companion object {
            fun unauthorizedBasic(realm: String = "Realm", msg: String = "Unauthorized"): Nothing
        }
    }

    data class Auth(
        val user: String,
        val pass: String,
        val digest: String
    ) {
        companion object {
            fun parse(auth: String): Auth
        }

        fun validate(expectedUser: String, expectedPass: String, realm: String
        suspend fun checkBasic(realm: String = "Realm", check: suspend Auth.() -> Boolean)
    }

    class Request(val uri: String, val headers: Http.Headers) {
        val path: String
        val queryString: String
        val getParams: QueryString
        val absoluteURI: String
    }

    class Response {
        val headers = arrayListOf<Pair<String, String>>()
        fun header(key: String, value: String)
    }

    data class Headers(val items: List<Pair<String, String>>) : Iterable<Pair<String, String>> {
        constructor(vararg items: Pair<String, String>)
        constructor(map: Map<String, String>)
        constructor(str: String?)

        override fun iterator(): Iterator<Pair<String, String>>

        operator fun get(key: String): String?
        fun getAll(key: String): List<String>
        fun getFirst(key: String): String?

        fun toListGrouped(): List<Pair<String, List<String>>>

        fun withAppendedHeaders(newHeaders: List<Pair<String, String>>): Headers

        fun withReplaceHeaders(newHeaders: List<Pair<String, String>>): Headers
        fun withAppendedHeaders(vararg newHeaders: Pair<String, String>): Headers
        fun withReplaceHeaders(vararg newHeaders: Pair<String, String>): Headers
        fun containsAll(other: Http.Headers): Boolean

        operator fun plus(that: Headers): Headers

        companion object {
            fun fromListMap(map: Map<String?, List<String>>): Headers
            fun parse(str: String?): Headers
            val ContentLength = "Content-Length"
            val ContentType = "Content-Type"
        }
    }

    data class RedirectException(val code: Int = 307, val redirectUri: String) : Http.HttpException(code, HttpStatusMessage(code))
}
```

### Client

```kotlin
fun createHttpClient() = defaultHttpFactory.createClient()

abstract class HttpClient protected constructor() {
    var ignoreSslCertificates: Boolean = false

    data class Response(
        val status: Int,
        val statusText: String,
        val headers: Http.Headers,
        val content: AsyncInputStream
    ) {
        val success: Boolean = status < 400
        suspend fun readAllBytes(): ByteArray
        val responseCharset: Charset
        suspend fun readAllString(charset: Charset = responseCharset): String
        suspend fun checkErrors(): Response

        fun withStringResponse(str: String, charset: Charset = UTF8): Response
        fun <T> toCompletedResponse(content: T): CompletedResponse
    }

    data class CompletedResponse<T>(
        val status: Int,
        val statusText: String,
        val headers: Http.Headers,
        val content: T
    ) {
        val success = status < 400
    }

    data class RequestConfig(
        val followRedirects: Boolean = true,
        val throwErrors: Boolean = false,
        val maxRedirects: Int = 10,
        val referer: String? = null,
        val simulateBrowser: Boolean = false
    )

    suspend fun request(
        method: Http.Method,
        url: String,
        headers: Http.Headers = Http.Headers(),
        content: AsyncStream? = null,
        config: RequestConfig = RequestConfig()
    ): Response

    suspend fun requestAsString(
        method: Http.Method,
        url: String,
        headers: Http.Headers = Http.Headers(),
        content: AsyncStream? = null,
        config: RequestConfig = RequestConfig()
    ): CompletedResponse<String>

    suspend fun requestAsBytes(
        method: Http.Method,
        url: String,
        headers: Http.Headers = Http.Headers(),
        content: AsyncStream? = null,
        config: RequestConfig = RequestConfig()
    ): CompletedResponse<ByteArray>

    suspend fun readBytes(url: String, config: RequestConfig = RequestConfig()): ByteArray
    suspend fun readString(url: String, config: RequestConfig = RequestConfig()): String
    suspend fun readJson(url: String, config: RequestConfig = RequestConfig()): Any?
}

// Delayed
fun HttpClient.delayed(ms: Long) = DelayedHttpClient(ms, this)
open class DelayedHttpClient(val delayMs: Long, val parent: HttpClient) : HttpClient()

class FakeHttpClient(val redirect: HttpClient? = null) : HttpClient() {
    val log = arrayListOf<String>()
    fun getAndClearLog(): List<String>

    var defaultResponse =
        HttpClient.Response(200, "OK", Http.Headers(), "LogHttpClient.response".toByteArray(UTF8).openAsync())

    class ResponseBuilder {
        private var responseCode = 200
        private var responseContent = "LogHttpClient.response".toByteArray(UTF8)
        private var responseHeaders = Http.Headers()

        fun response(content: String, code: Int = 200, charset: Charset = UTF8)
        fun response(content: ByteArray, code: Int = 200)
        fun redirect(url: String, code: Int = 302): Unit

        fun ok(content: String)
        fun notFound(content: String = "404 - Not Found")
        fun internalServerError(content: String = "500 - Internal Server Error")
    }

    data class Rule(
        val method: Http.Method?,
        val url: String? = null,
        val headers: Http.Headers? = null
    ) {
        fun matches(method: Http.Method, url: String, headers: Http.Headers, content: ByteArray?): Boolean
    }

    fun onRequest(
        method: Http.Method? = null,
        url: String? = null,
        headers: Http.Headers? = null
    ): ResponseBuilder
}

fun LogHttpClient() = FakeHttpClient()

object HttpStatusMessage {
    val CODES: Map<Int, String>
    operator fun invoke(code: Int): String
}

object HttpStats {
    val connections: AtomicLong
    val disconnections: AtomicLong
    override fun toString(): String
}

interface HttpFactory {
    fun createClient(): HttpClient
    fun createServer(): HttpServer
}

class ProxiedHttpFactory(var parent: HttpFactory) : HttpFactory by parent

fun setDefaultHttpFactory(factory: HttpFactory)
fun httpError(code: Int, msg: String): Nothing
```

#### Endpoint and Rest

```kotlin
fun HttpFactory.createClientEndpoint(endpoint: String)
fun createHttpClientEndpoint(endpoint: String) = createHttpClient().endpoint(endpoint)

interface HttpClientEndpoint {
    suspend fun request(
        method: Http.Method,
        path: String,
        headers: Http.Headers = Http.Headers(),
        content: AsyncStream? = null,
        config: HttpClient.RequestConfig = HttpClient.RequestConfig()
    ): HttpClient.Response
}

internal data class Request(
    val method: Http.Method,
    val path: String,
    val headers: Http.Headers,
    val content: AsyncStream?
) {
    suspend fun format(format: String = "{METHOD}:{PATH}:{CONTENT}"): String
}

class FakeHttpClientEndpoint(val defaultMessage: String = "{}") : HttpClientEndpoint {
	private val log: ArrayList<Request>
	private var responsePointer = 0
	private val responses: ArrayList<HttpClient.Response>()

	fun addResponse(code: Int, content: String)
	fun addOkResponse(content: String)
	fun addNotFoundResponse(content: String)
	override suspend fun request(
		method: Http.Method,
		path: String,
		headers: Http.Headers,
		content: AsyncStream?,
		config: HttpClient.RequestConfig
	): HttpClient.Response

	suspend fun capture(format: String = "{METHOD}:{PATH}:{CONTENT}", callback: suspend () -> Unit): List<String>
}

fun HttpClient.endpoint(endpoint: String): HttpClientEndpoint
```

```kotlin
fun HttpClientEndpoint.rest(): HttpRestClient
fun HttpClient.rest(endpoint: String): HttpRestClient
fun HttpFactory.createRestClient(endpoint: String, mapper: ObjectMapper): HttpRestClient

class HttpRestClient(val endpoint: HttpClientEndpoint) {
    suspend fun request(method: Http.Method, path: String, request: Any?, mapper: ObjectMapper = Mapper): Any

    suspend fun head(path: String): Any
    suspend fun delete(path: String): Any
    suspend fun get(path: String): Any
    suspend fun put(path: String, request: Any): Any
    suspend fun post(path: String, request: Any): Any
}
```

### Server (HTTP and WebSockets)

```kotlin
fun createHttpServer() = defaultHttpFactory.createServer()

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
        abstract fun reject()

        abstract fun close()
        abstract fun onStringMessage(handler: suspend (String) -> Unit)
        abstract fun onBinaryMessage(handler: suspend (ByteArray) -> Unit)
        abstract fun onClose(handler: suspend () -> Unit)
        abstract fun send(msg: String)
        abstract fun send(msg: ByteArray)

        fun sendSafe(msg: String)
        fun sendSafe(msg: ByteArray)

        fun stringMessageStream(): ReceiveChannel<String>
        fun binaryMessageStream(): ReceiveChannel<ByteArray>
        fun anyMessageStream(): ReceiveChannel<Any>
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

        fun getHeader(key: String): String?
        fun getHeaderList(key: String): List<String>
        fun removeHeader(key: String)
        fun addHeader(key: String, value: String)
        fun replaceHeader(key: String, value: String)

        protected abstract suspend fun _handler(handler: (ByteArray) -> Unit)
        protected abstract suspend fun _endHandler(handler: () -> Unit)
        protected abstract suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers)
        protected abstract suspend fun _write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset)
        protected abstract suspend fun _end()

        suspend fun handler(handler: (ByteArray) -> Unit)
        suspend fun endHandler(handler: () -> Unit)

        suspend fun readRawBody(maxSize: Int = 0x1000): ByteArray
        fun setStatus(code: Int, message: String = HttpStatusMessage(code))

        override suspend fun write(buffer: ByteArray, offset: Int, len: Int)
        suspend fun end()
        suspend fun end(data: ByteArray)
        suspend fun write(data: String, charset: Charset = UTF8)
        suspend fun end(data: String, charset: Charset = UTF8)
        override suspend fun close()
    }

    suspend fun allHandler(handler: suspend (BaseRequest) -> Unit)
    open val actualPort: Int = 0

    suspend fun websocketHandler(handler: suspend (WsRequest) -> Unit): HttpServer
    suspend fun httpHandler(handler: suspend (Request) -> Unit): HttpServer
    suspend fun listen(port: Int = 0, host: String = "127.0.0.1"): HttpServer
    suspend fun listen(port: Int = 0, host: String = "127.0.0.1", handler: suspend (Request) -> Unit): HttpServer
    final override suspend fun close()
}

class FakeRequest(
    method: Http.Method,
    uri: String,
    headers: Http.Headers = Http.Headers(),
    val body: ByteArray = EMPTY_BYTE_ARRAY,
    requestConfig: HttpServer.RequestConfig
) : HttpServer.Request(method, uri, headers, requestConfig) {
    var outputHeaders: Http.Headers = Http.Headers()
    var outputStatusCode: Int = 0
    var outputStatusMessage: String = ""
    var output: String = ""
    val log = arrayListOf<String>()
}
```

## WebSocket Client

```kotlin
suspend fun WebSocketClient(
    url: String,
    protocols: List<String>? = null,
    origin: String? = null,
    wskey: String? = "wskey",
    debug: Boolean = false
): WebSocketClient

abstract class WebSocketClient {
    val url: String
    val protocols: List<String>?

    val onOpen: Signal<Unit>
    val onError: Signal<Throwable>
    val onClose: Signal<Unit>

    val onBinaryMessage: Signal<ByteArray>
    val onStringMessage: Signal<String>
    val onAnyMessage: Signal<Any>

    open fun close(code: Int = 0, reason: String = ""): Unit
    open suspend fun send(message: String): Unit
    open suspend fun send(message: ByteArray): Unit
}

suspend fun WebSocketClient.readString(): String
suspend fun WebSocketClient.readBinary(): ByteArray

class WebSocketException(message: String) : IOException(message)
```
