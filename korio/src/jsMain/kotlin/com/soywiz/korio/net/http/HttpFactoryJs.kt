package com.soywiz.korio.net.http

import com.soywiz.korio.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.w3c.xhr.*

internal actual val httpFactory: HttpFactory by lazy {
	object : HttpFactory {
		//override fun createClient(): HttpClient = if (OS.isJsNodeJs) HttpClientNodeJs() else HttpClientBrowserJs()
		//override fun createServer(): HttpServer = HttpSeverNodeJs()
		override fun createClient(): HttpClient = HttpClientBrowserJs()
		override fun createServer(): HttpServer = error("HttpServer not available on Browser")
	}
}

class HttpClientBrowserJs : HttpClient() {
	override suspend fun requestInternal(
		method: Http.Method,
		url: String,
		headers: Http.Headers,
		content: AsyncStream?
	): Response {
		val deferred = CompletableDeferred<Response>(Job())
		val xhr = XMLHttpRequest()
		xhr.open(method.name, url, true)
		xhr.responseType = XMLHttpRequestResponseType.ARRAYBUFFER

		xhr.onload = { e ->
			//val u8array = Uint8Array(xhr.response as ArrayBuffer)
			//val out = ByteArray(u8array.length)
			//for (n in out.indices) out[n] = u8array[n]

			val out = Int8Array(xhr.response.unsafeCast<ArrayBuffer>()).unsafeCast<ByteArray>()

			//js("debugger;")
			deferred.complete(
				Response(
					status = xhr.status.toInt(),
					statusText = xhr.statusText,
					headers = Http.Headers(xhr.getAllResponseHeaders()),
					rawContent = out.openAsync(),
                    content = out.openAsync(),
				)
			)
		}

		xhr.onerror = { e ->
			deferred.completeExceptionally(kotlin.RuntimeException("Error status=${xhr.status},'${xhr.statusText}' opening $url"))
		}

		for (header in headers) {
			val hnname = header.first.toLowerCase().trim()
			when (hnname) {
				"connection", Http.Headers.ContentLength -> Unit // Refused to set unsafe header
				else -> xhr.setRequestHeader(header.first, header.second)
			}
		}

		deferred.invokeOnCompletion {
			if (deferred.isCancelled) {
				xhr.abort()
			}
		}

		if (content != null) {
			xhr.send(content.readAll())
		} else {
			xhr.send()
		}
		return deferred.await()
	}
}
