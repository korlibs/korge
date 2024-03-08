package korlibs.io.file.std

import korlibs.datastructure.linkedHashMapOf
import korlibs.io.file.Vfs
import korlibs.io.file.VfsCachedStatContext
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.VfsStat
import korlibs.io.lang.FileNotFoundException
import korlibs.io.lang.invalidOp
import korlibs.io.lang.unsupported
import korlibs.io.net.MimeType
import korlibs.io.net.URL
import korlibs.io.net.http.Http
import korlibs.io.net.http.HttpClient
import korlibs.io.net.http.createHttpClient
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.AsyncStreamBase
import korlibs.io.stream.buffered
import korlibs.io.stream.openAsync
import korlibs.io.stream.readAll
import korlibs.io.stream.toAsyncStream
import korlibs.io.util.LONG_ZERO_TO_MAX_RANGE
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.coroutineContext

fun UrlVfs(url: String, client: HttpClient = createHttpClient(), failFromStatus: Boolean = true): VfsFile =
    UrlVfs(URL(url), client, failFromStatus)

fun UrlVfs(url: URL, client: HttpClient = createHttpClient(), failFromStatus: Boolean = true): VfsFile =
	UrlVfs(url.copy(path = "", query = null).fullUrl, Unit, client, failFromStatus)[url.path]

fun UrlVfsJailed(url: String, client: HttpClient = createHttpClient(), failFromStatus: Boolean = true): VfsFile =
    UrlVfsJailed(URL(url), client, failFromStatus)

fun UrlVfsJailed(url: URL, client: HttpClient = createHttpClient(), failFromStatus: Boolean = true): VfsFile =
	UrlVfs(url.fullUrl, Unit, client, failFromStatus)[url.path]

class UrlVfs(
    val url: String, val dummy: Unit, val client: HttpClient = createHttpClient(),
    val failFromStatus: Boolean = true,
) : Vfs() {
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

			val stat = coroutineContext[VfsCachedStatContext]?.stat ?: stat(path)
			val response = stat.extraInfo as? HttpClient.Response

			if (!stat.exists) {
				throw FileNotFoundException("Unexistant $fullUrl : $response")
			}

			return object : AsyncStreamBase() {
				override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					if (len == 0) return 0
					val res = _readRangeBase(fullUrl, position until (position + len))
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

                override suspend fun hasLength(): Boolean = stat.size >= 0L
                override suspend fun getLength(): Long = if (hasLength()) stat.size else unsupported()
			}.toAsyncStream().buffered()
			//}.toAsyncStream()
		} catch (e: RuntimeException) {
			throw FileNotFoundException(e.message ?: "error")
		}
	}

	override suspend fun openInputStream(path: String): AsyncInputStream =
        _readRangeBase(getFullUrl(path), LONG_ZERO_TO_MAX_RANGE).content

    private suspend fun _readRangeBase(fullUrl: String, range: LongRange): HttpClient.Response {
        return client.request(
            Http.Method.GET,
            fullUrl,
            Http.Headers(if (range == LONG_ZERO_TO_MAX_RANGE) LinkedHashMap() else linkedHashMapOf("range" to "bytes=${range.start}-${range.endInclusive}"))
        ).also {
            if (failFromStatus) {
                if (it.status == 404) throw FileNotFoundException("$fullUrl not found")
                it.checkErrors()
            }
        }
    }

	override suspend fun readRange(path: String, range: LongRange): ByteArray = _readRangeBase(getFullUrl(path), range).content.readAll()

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
					size = result.headers[Http.Headers.ContentLength]?.toLongOrNull() ?: -1L,
					extraInfo = result
				)
			} else {
				createNonExistsStat(path, extraInfo = result)
			}
		}
	}

    override suspend fun listFlow(path: String): Flow<VfsFile> {
        unsupported()
    }

    override fun toString(): String = "UrlVfs"
}
