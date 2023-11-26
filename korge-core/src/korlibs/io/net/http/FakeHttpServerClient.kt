package korlibs.io.net.http

import korlibs.io.net.URL
import korlibs.io.stream.MemorySyncStream
import korlibs.io.stream.openAsync
import korlibs.io.stream.toByteArray
import kotlinx.coroutines.CompletableDeferred

open class FakeHttpServer : HttpServer() {
    var errorHandler: (suspend (Throwable) -> Unit)? = null
    var httpHandler: (suspend (Request) -> Unit)? = null
    var wsHandler: (suspend (WsRequest) -> Unit)? = null

    override suspend fun errorHandlerInternal(handler: suspend (Throwable) -> Unit) {
        errorHandler = handler
    }

    override suspend fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
        wsHandler = handler
    }

    override suspend fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
        httpHandler = handler
    }

    override suspend fun listenInternal(port: Int, host: String) {
    }

    val client: FakeHttpClientWithServer = FakeHttpClientWithServer(this).apply {
        onRequest().handler { method, url, headers, content ->
            val path = URL(url).pathWithQuery
            val contentBytes = content
            val temp = MemorySyncStream()
            val completed = CompletableDeferred<Unit>()
            var responseStatus = 500
            var responseStatusText = "Internal Server Error"
            var responseHeaders = Http.Headers()
            val request = object : HttpServer.Request(method, path, headers, HttpServer.RequestConfig()) {
                override suspend fun _handler(handler: (ByteArray) -> Unit) {
                    if (contentBytes != null) handler(contentBytes)
                }

                override suspend fun _endHandler(handler: () -> Unit) {
                    // @TODO: defer this call, so _handler can be called before
                    handler()
                }

                override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
                    responseStatus = code
                    responseStatusText = message
                    responseHeaders = headers
                }

                override suspend fun _write(data: ByteArray, offset: Int, size: Int) {
                    temp.write(data, offset, size)
                }

                override suspend fun _end() {
                    completed.complete(Unit)
                }
            }
            httpHandler?.invoke(request)
            completed.await()
            HttpClient.Response(responseStatus, responseStatusText, responseHeaders, temp.toByteArray().openAsync())
        }
    }
}

open class FakeHttpClientWithServer(val server: FakeHttpServer) : FakeHttpClient()

inline fun FakeHttpServerClient(block: FakeHttpServer.() -> Unit): FakeHttpClientWithServer {
    val server = FakeHttpServer()
    block(server)
    return server.client
}
