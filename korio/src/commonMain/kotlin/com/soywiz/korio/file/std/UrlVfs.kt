package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

fun UrlVfs(url: String, client: HttpClient = createHttpClient()): VfsFile = UrlVfs(URL(url), client)

fun UrlVfs(url: URL, client: HttpClient = createHttpClient()): VfsFile =
	UrlVfs(url.copy(path = "", query = null).fullUrl, Unit, client)[url.path]

fun UrlVfsJailed(url: String, client: HttpClient = createHttpClient()): VfsFile = UrlVfsJailed(URL(url), client)

fun UrlVfsJailed(url: URL, client: HttpClient = createHttpClient()): VfsFile =
	UrlVfs(url.fullUrl, Unit, client)[url.path]

class UrlVfs(val url: String, val dummy: Unit, val client: HttpClient = createHttpClient()) : Vfs() {
	override val absolutePath: String = url

	fun getFullUrl(path: String): String {
		val result = url.trim('/') + '/' + path.trim('/')
		//println("UrlVfs.getFullUrl: url=$url, path=$path, result=$result")
		return result
	}

	//suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
	//	return if (mode.write) {
	//		TODO()
	//	} else {
	//		client.request(HttpClient.Method.GET, getFullUrl(path)).content.toAsyncStream()
	//	}
	//}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		try {
			val fullUrl = getFullUrl(path)

			// For file: it is useless to try to get the size and to use ranges. So we just read it completely.
			if (fullUrl.startsWith("file:")) {
				return client.readBytes(fullUrl).openAsync()
			}

			val stat = stat(path)
			val response = stat.extraInfo as? HttpClient.Response

			if (!stat.exists) {
				throw FileNotFoundException("Unexistant $fullUrl : $response")
			}

			return object : AsyncStreamBase() {
				override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					if (len == 0) return 0
					val res = client.request(
						Http.Method.GET,
						fullUrl,
						Http.Headers(linkedMapOf("range" to "bytes=$position-${position + len - 1}"))
					)
					val s = res.content
					var coffset = offset
					var pending = len
					var totalRead = 0
					while (pending > 0) {
						val read = s.read(buffer, coffset, pending)
						if (read < 0 && totalRead == 0) return read
						if (read <= 0) break
						pending -= read
						totalRead += read
						coffset += read
					}
					return totalRead
				}

				override suspend fun getLength(): Long = stat.size
			}.toAsyncStream().buffered()
			//}.toAsyncStream()
		} catch (e: RuntimeException) {
			throw FileNotFoundException(e.message ?: "error")
		}
	}

	override suspend fun openInputStream(path: String): AsyncInputStream {
		return client.request(Http.Method.GET, getFullUrl(path)).content
	}

	override suspend fun readRange(path: String, range: LongRange): ByteArray = client.requestAsBytes(
		Http.Method.GET,
		getFullUrl(path),
		Http.Headers(if (range == LONG_ZERO_TO_MAX_RANGE) LinkedHashMap() else linkedHashMapOf("range" to "bytes=${range.start}-${range.endInclusive}"))
	).content

	class HttpHeaders(val headers: Http.Headers) : Attribute

	override suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
		if (content !is AsyncStream) invalidOp("UrlVfs.put requires content to be AsyncStream")
		val headers = attributes.get<HttpHeaders>()
		val mimeType = attributes.get<MimeType>() ?: MimeType.APPLICATION_JSON
		val hheaders = headers?.headers ?: Http.Headers()
		val contentLength = content.getLength()

		client.request(
			Http.Method.PUT, getFullUrl(path), hheaders.withReplaceHeaders(
				Http.Headers.ContentLength to "$contentLength",
				Http.Headers.ContentType to mimeType.mime
			), content
		)

		return content.getLength()
	}

	override suspend fun stat(path: String): VfsStat {
		val fullUrl = getFullUrl(path)

		//println("STAT URL: $fullUrl")

		return if (fullUrl.startsWith("file:")) {
			// file: protocol won't respond with content-length
			try {
				val size = client.readBytes(fullUrl).size.toLong()
				//println("SIZE FOR $fullUrl -> $size")
				createExistsStat(
					path,
					isDirectory = false,
					size = size,
					extraInfo = null
				)
			} catch (e: Throwable) {
				e.printStackTrace()
				createNonExistsStat(path)
			}
		} else {
			val result = client.request(Http.Method.HEAD, fullUrl)

			//println("STAT URL HEADERS: ${result.headers}")

			if (result.success) {
				createExistsStat(
					path,
					isDirectory = false,
					size = result.headers[Http.Headers.ContentLength]?.toLongOrNull() ?: 0L,
					extraInfo = result
				)
			} else {
				createNonExistsStat(path, extraInfo = result)
			}
		}
	}

	override fun toString(): String = "UrlVfs"
}
