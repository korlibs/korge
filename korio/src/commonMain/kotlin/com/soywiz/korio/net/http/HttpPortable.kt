package com.soywiz.korio.net.http

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

internal object HttpPortable {
    internal fun computeHeader(method: Http.Method, url: URL, rheaders2: Http.Headers): String = buildString {
        val EOL = "\r\n"
        append("$method ${url.pathWithQuery} HTTP/1.1$EOL")
        for (header in rheaders2) {
            append("${header.first}: ${header.second}$EOL")
        }
        append(EOL)
    }

	fun createClient(): HttpClient {
		return object : HttpClient() {
			override suspend fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
				val url = URL(url)
				val secure = url.scheme == "https"
				//println("HTTP CLIENT: host=${url.host}, port=${url.port}, secure=$secure")
				val client = createTcpClient(url.host!!, url.port, secure)

				val rheaders = combineHeadersForHost(headers, url.host)
				val rheaders2 = if (content != null)
                    rheaders.withReplaceHeaders(Http.Headers(Http.Headers.ContentLength to content.getLength().toString()))
                else
                    rheaders
                client.writeString(computeHeader(method, url, rheaders2))
				content?.copyTo(client)

				//println("SENT RESPONSE")

				val firstLine = client.readLine()
				val responseInfo = Regex("HTTP/1.\\d+ (\\d+) (.*)").find(firstLine) ?: error("Invalid HTTP response $firstLine")

				//println("FIRST LINE: $firstLine")

				val responseCode = responseInfo.groupValues[1].toInt()
				val responseMessage = responseInfo.groupValues[2]

				val headers = arrayListOf<String>()
				while (true) {
					val line = client.readLine().trim()
					//println("line: $line")
					if (line.isEmpty()) break
					headers += line
				}

				val responseHeaders = Http.Headers(headers.map {
					val parts = it.split(':', limit = 2)
					parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }
				})

				return Response(responseCode, responseMessage, responseHeaders, client)
			}
		}
	}

	fun createServer(): HttpServer {
		val HeaderRegex = Regex("^(\\w+)\\s+(.*)\\s+(HTTP/1.[01])$")

		return object : HttpServer() {
			val BodyChunkSize = 1024
			val LimitRequestFieldSize = 8190
			val LimitRequestFields = 100

			var wshandler: suspend (WsRequest) -> Unit = {}
			var handler: suspend (Request) -> Unit = {}
			val onClose = Signal<Unit>()
			override var actualPort: Int = -1; private set

			override suspend fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
				this.wshandler = handler
			}

			override suspend fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
				this.handler = handler
			}

			override suspend fun listenInternal(port: Int, host: String) {
				val context = coroutineContext
				val socket = createTcpServer(port, host)
				actualPort = socket.port
				val close = socket.listen { client ->
					while (true) {
						//println("Connected! : $client : ${KorioNative.currentThreadId}")
						val cb = client.bufferedInput()
						//val cb = client

						//val header = cb.readBufferedLine().trim()
						//val fline = cb.readBufferedUntil('\n'.toByte()).toString(UTF8).trim()
						val fline = cb.readUntil('\n'.toByte(), limit = LimitRequestFieldSize).toString(UTF8).trim()
						//println("fline: $fline")
						val match = HeaderRegex.matchEntire(fline)
							?: throw IllegalStateException("Not a valid request '$fline'")
						val method = match.groupValues[1]
						val url = match.groupValues[2]
						val httpVersion = match.groupValues[3]
						val headerList = arrayListOf<Pair<String, String>>()
						for (n in 0 until LimitRequestFields) { // up to 1024 headers
							val line = cb.readUntil('\n'.toByte(), limit = LimitRequestFieldSize).toString(UTF8).trim()
							if (line.isEmpty()) break
							val parts = line.split(':', limit = 2)
							headerList += parts.getOrElse(0) { "" }.trim() to parts.getOrElse(1) { "" }.trim()
						}
						val headers = Http.Headers(headerList)
						val keepAlive = headers["connection"]?.toLowerCase() == "keep-alive"
						val contentLength = headers[Http.Headers.ContentLength]?.toLongOrNull()

						//println("REQ: $method, $url, $headerList")

						val requestCompleted = CompletableDeferred<Unit>(Job())

						var bodyHandler: (ByteArray) -> Unit = {}
						var endHandler: () -> Unit = {}

						launchImmediately(coroutineContext) {
							handler(object : Request(Http.Method(method), url, headers) {
								override suspend fun _handler(handler: (ByteArray) -> Unit) =
									run { bodyHandler = handler }

								override suspend fun _endHandler(handler: () -> Unit) = run { endHandler = handler }

								override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
									val sb = StringBuilder()
									sb.append("$httpVersion $code $message\r\n")
									for (header in headers) sb.append("${header.first}: ${header.second}\r\n")
									sb.append("\r\n")
									client.write(sb.toString().toByteArray(UTF8))
								}

								override suspend fun _write(data: ByteArray, offset: Int, size: Int) {
									client.write(data, offset, size)
								}

								override suspend fun _end() {
									requestCompleted.complete(Unit)
								}
							})
						}

						//println("Content-Length: '${headers["content-length"]}'")
						//println("Content-Length: $contentLength")
						if (contentLength != null) {
							var remaining = contentLength
							while (remaining > 0) {
								val toRead = min(BodyChunkSize.toLong(), remaining).toInt()
								val read = cb.readBytesUpToFirst(toRead)
								bodyHandler(read)
								remaining -= read.size
							}
						}
						endHandler()

						requestCompleted.await()

						if (keepAlive) continue

						client.close()
						break
					}
				}

				onClose {
					close.close()
				}
			}

			override suspend fun closeInternal() {
				onClose()
			}
		}
	}
}
