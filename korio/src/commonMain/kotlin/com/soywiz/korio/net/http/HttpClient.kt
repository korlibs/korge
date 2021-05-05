package com.soywiz.korio.net.http

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.concurrent.atomic.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*

abstract class HttpClient protected constructor() {
	var ignoreSslCertificates = false

	protected abstract suspend fun requestInternal(
		method: Http.Method,
		url: String,
		headers: Http.Headers = Http.Headers(),
		content: AsyncStream? = null
	): Response

	data class Response(
		val status: Int,
		val statusText: String,
		val headers: Http.Headers,
		val content: AsyncInputStream
	) {
		val success = status < 400
		suspend fun readAllBytes(): ByteArray {
			//println(content)
			val allContent = content.readAll()
			//println("Response.readAllBytes:" + allContent)
			//Debugger.enterDebugger()
			return allContent
		}

		val responseCharset by lazy {
			// @TODO: Detect charset from headers with default to UTF-8
			UTF8
		}

		suspend fun readAllString(charset: Charset = responseCharset): String {
			val bytes = readAllBytes()
			//Debugger.enterDebugger()
			return bytes.toString(charset)
		}

		suspend fun checkErrors(): Response = this.apply {
			if (!success) throw Http.HttpException(status, readAllString(), statusText)
		}

		fun withStringResponse(str: String, charset: Charset = UTF8) =
			this.copy(content = str.toByteArray(charset).openAsync())

		fun <T> toCompletedResponse(content: T) = CompletedResponse(status, statusText, headers, content)
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

	private fun mergeUrls(base: String, append: String): String = URL.resolve(base, append)

	suspend fun request(
		method: Http.Method,
		url: String,
		headers: Http.Headers = Http.Headers(),
		content: AsyncStream? = null,
		config: RequestConfig = RequestConfig()
	): Response {
		//println("HttpClient.request: $method:$url")
		val contentLength = content?.getLength() ?: 0L
		var actualHeaders = headers

		if (content != null && !headers.any { it.first.equals(Http.Headers.ContentLength, ignoreCase = true) }) {
			actualHeaders = actualHeaders.withReplaceHeaders(Http.Headers.ContentLength to "$contentLength")
		}

		if (config.simulateBrowser) {
            actualHeaders = combineHeadersForHost(actualHeaders, null)
		}

		val response =
			requestInternal(method, url, actualHeaders, content).apply { if (config.throwErrors) checkErrors() }
		if (config.followRedirects && config.maxRedirects >= 0) {
			val redirectLocation = response.headers["location"]
			if (redirectLocation != null) {
				return request(
					method, mergeUrls(url, redirectLocation), headers.withReplaceHeaders(
						"Referer" to url
					), content, config.copy(maxRedirects = config.maxRedirects - 1)
				)
			}
		}
		return response
	}

	suspend fun requestAsString(
		method: Http.Method,
		url: String,
		headers: Http.Headers = Http.Headers(),
		content: AsyncStream? = null,
		config: RequestConfig = RequestConfig()
	): CompletedResponse<String> {
		val res = request(method, url, headers, content, config = config)
		return res.toCompletedResponse(res.readAllString())
	}

	suspend fun requestAsBytes(
		method: Http.Method,
		url: String,
		headers: Http.Headers = Http.Headers(),
		content: AsyncStream? = null,
		config: RequestConfig = RequestConfig()
	): CompletedResponse<ByteArray> {
		val res = request(method, url, headers, content, config = config)
		return res.toCompletedResponse(res.readAllBytes())
	}

	suspend fun readBytes(url: String, config: RequestConfig = RequestConfig()): ByteArray =
		requestAsBytes(Http.Method.GET, url, config = config.copy(throwErrors = true)).content

	suspend fun readString(url: String, config: RequestConfig = RequestConfig()): String =
		requestAsString(Http.Method.GET, url, config = config.copy(throwErrors = true)).content

	suspend fun readJson(url: String, config: RequestConfig = RequestConfig()): Any? =
		Json.parse(requestAsString(Http.Method.GET, url, config = config.copy(throwErrors = true)).content)

	companion object {
        val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36"
        val DEFAULT_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
        val DEFAULT_LANGUAGE = "en-us"
        val DEFAULT_ENCODING = "gzip, deflate"
        val DEFAULT_CONNECTION = "Close"

        fun combineHeadersForHost(headers: Http.Headers, host: String?): Http.Headers {
            val out = Http.Headers(
                "User-Agent" to DEFAULT_USER_AGENT,
                "Accept" to DEFAULT_ACCEPT,
                "Accept-Language" to DEFAULT_LANGUAGE,
                "Accept-Encoding" to DEFAULT_ENCODING,
                "Connection" to DEFAULT_CONNECTION
            ).withReplaceHeaders(headers)
            return if (host != null) out.withReplaceHeaders("Host" to host) else out
        }

		operator fun invoke() = defaultHttpFactory.createClient()
	}
}

open class DelayedHttpClient(val delayMs: Long, val parent: HttpClient) : HttpClient() {
	private val queue = AsyncThread()

	override suspend fun requestInternal(
		method: Http.Method,
		url: String,
		headers: Http.Headers,
		content: AsyncStream?
	): Response = queue {
		println("Waiting $delayMs milliseconds for $url...")
		delay(delayMs)
		parent.request(method, url, headers, content)
	}
}

fun HttpClient.delayed(ms: Long) = DelayedHttpClient(ms, this)

class FakeHttpClient(val redirect: HttpClient? = null) : HttpClient() {
	val log = arrayListOf<String>()
	var defaultResponse =
		HttpClient.Response(200, "OK", Http.Headers(), "LogHttpClient.response".toByteArray(UTF8).openAsync())
	private val rules = LinkedHashMap<Rule, ArrayList<ResponseBuilder>>()

	override suspend fun requestInternal(
		method: Http.Method,
		url: String,
		headers: Http.Headers,
		content: AsyncStream?
	): Response {
		val contentString = content?.sliceStart()?.readAll()?.toString(UTF8)
		val requestNumber = log.size
		log += "$method, $url, $headers, $contentString"
		if (redirect != null) return redirect.request(method, url, headers, content)
		val readedContent = content?.readAll()
		val matchedRules = rules.entries.reversed().filter { it.key.matches(method, url, headers, readedContent) }
		val rule = matchedRules.firstOrNull()
		return rule?.value?.getCyclic(requestNumber)?.buildResponse() ?: defaultResponse
	}

	class ResponseBuilder {
		private var responseCode = 200
		private var responseContent = "LogHttpClient.response".toByteArray(UTF8)
		private var responseHeaders = Http.Headers()

		fun response(content: String, code: Int = 200, charset: Charset = UTF8) = this.apply {
			responseCode = code
			responseContent = content.toByteArray(charset)
		}

		fun response(content: ByteArray, code: Int = 200) = this.apply {
			responseCode = code
			responseContent = content
		}

		fun redirect(url: String, code: Int = 302) = this.apply {
			responseCode = code
			responseHeaders += Http.Headers("Location" to url)
		}

        fun header(key: String, value: Any) = this.apply {
            responseHeaders += Http.Headers(key to "$value")
        }

        fun headers(headers: Http.Headers) = this.apply {
            responseHeaders += headers
        }

        fun ok(content: String) = response(content, code = 200)
        fun ok(content: ByteArray) = response(content, code = 200)
		fun notFound(content: String = "404 - Not Found") = response(content, code = 404)
		fun internalServerError(content: String = "500 - Internal Server Error") = response(content, code = 500)

		internal fun buildResponse() = HttpClient.Response(
            responseCode,
            HttpStatusMessage(responseCode),
            responseHeaders,
            responseContent.openAsync()
        )
	}

	data class Rule(
		val method: Http.Method?,
		val url: String? = null,
		val headers: Http.Headers? = null
	) {
		fun matches(method: Http.Method, url: String, headers: Http.Headers, content: ByteArray?): Boolean {
			if (this.method != null && this.method != method) return false
			if (this.url != null && this.url != url) return false
			if (this.headers != null && !headers.containsAll(this.headers)) return false
			return true
		}
	}

	fun onRequest(
		method: Http.Method? = null,
		url: String? = null,
		headers: Http.Headers? = null
	): ResponseBuilder {
		val responseBuilders = rules.getOrPut(Rule(method, url, headers)) { arrayListOf() }
		val responseBuilder = ResponseBuilder()
		responseBuilders += responseBuilder
		return responseBuilder
	}

	fun getAndClearLog() = log.toList().apply { log.clear() }
}

fun LogHttpClient() = FakeHttpClient()

object HttpStatusMessage {
	val CODES = linkedMapOf(
		100 to "Continue",
		101 to "Switching Protocols",
		200 to "OK",
		201 to "Created",
		202 to "Accepted",
		203 to "Non-Authoritative Information",
		204 to "No Content",
		205 to "Reset Content",
		206 to "Partial Content",
		300 to "Multiple Choices",
		301 to "Moved Permanently",
		302 to "Found",
		303 to "See Other",
		304 to "Not Modified",
		305 to "Use Proxy",
		307 to "Temporary Redirect",
		400 to "Bad Request",
		401 to "Unauthorized",
		402 to "Payment Required",
		403 to "Forbidden",
		404 to "Not Found",
		405 to "Method Not Allowed",
		406 to "Not Acceptable",
		407 to "Proxy Authentication Required",
		408 to "Request Timeout",
		409 to "Conflict",
		410 to "Gone",
		411 to "Length Required",
		412 to "Precondition Failed",
		413 to "Request Entity Too Large",
		414 to "Request-URI Too Long",
		415 to "Unsupported Media Type",
		416 to "Requested Range Not Satisfiable",
		417 to "Expectation Failed",
		418 to "I'm a teapot",
		422 to "Unprocessable Entity (WebDAV - RFC 4918)",
		423 to "Locked (WebDAV - RFC 4918)",
		424 to "Failed Dependency (WebDAV) (RFC 4918)",
		425 to "Unassigned",
		426 to "Upgrade Required (RFC 7231)",
		428 to "Precondition Required",
		429 to "Too Many Requests",
		431 to "Request Header Fileds Too Large)",
		449 to "Error449",
		451 to "Unavailable for Legal Reasons",
		500 to "Internal Server Error",
		501 to "Not Implemented",
		502 to "Bad Gateway",
		503 to "Service Unavailable",
		504 to "Gateway Timeout",
		505 to "HTTP Version Not Supported",
		506 to "Variant Also Negotiates (RFC 2295)",
		507 to "Insufficient Storage (WebDAV - RFC 4918)",
		508 to "Loop Detected (WebDAV)",
		509 to "Bandwidth Limit Exceeded",
		510 to "Not Extended (RFC 2774)",
		511 to "Network Authentication Required"
	)

	operator fun invoke(code: Int) = CODES.getOrElse(code) { "Error$code" }
}

object HttpStats {
	val connections = korAtomic(0L)
	val disconnections = korAtomic(0L)

	override fun toString(): String = "HttpStats(connections=$connections, Disconnections=$disconnections)"
}

interface HttpFactory {
	fun createClient(): HttpClient
	fun createServer(): HttpServer
}

class ProxiedHttpFactory(var parent: HttpFactory) : HttpFactory by parent

internal val _defaultHttpFactory: ProxiedHttpFactory by lazy { ProxiedHttpFactory(httpFactory) }
val defaultHttpFactory: HttpFactory get() = _defaultHttpFactory

internal expect val httpFactory: HttpFactory

fun setDefaultHttpFactory(factory: HttpFactory) {
	_defaultHttpFactory.parent = factory
}

fun HttpFactory.createClientEndpoint(endpoint: String) = createClient().endpoint(endpoint)

fun createHttpClient() = defaultHttpFactory.createClient()
fun createHttpServer() = defaultHttpFactory.createServer()
fun createHttpClientEndpoint(endpoint: String) = createHttpClient().endpoint(endpoint)

fun httpError(code: Int, msg: String): Nothing = throw Http.HttpException(code, msg)
