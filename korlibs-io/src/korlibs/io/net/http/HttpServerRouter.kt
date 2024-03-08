package korlibs.io.net.http

import korlibs.math.clamp
import korlibs.io.file.VfsFile

suspend inline fun HttpServer.router(block: HttpServerRouter.() -> Unit) {
    val server = HttpServerRouter(this)
    block(server)
    httpHandler { server.handle(it) }
}

typealias HttpServerRequestHandler = suspend (request: HttpServer.Request) -> Unit

class HttpServerRouter(val server: HttpServer) {
    class Rule(val path: String, val handler: HttpServerRequestHandler) {
        fun match(request: HttpServer.Request): Boolean {
            if (request.path.startsWith(this.path)) {
                return true
            }
            return false
        }
    }
    private val prehooks = arrayListOf<HttpServerRequestHandler>()
    private val rules = arrayListOf<Rule>()

    suspend fun handle(request: HttpServer.Request) {
        for (hook in prehooks) hook(request)
        for (rule in rules) {
            if (rule.match(request)) {
                return rule.handler(request)
            }
        }
    }

    fun handler(path: String, handler: HttpServerRequestHandler) {
        rules.add(Rule(path, handler))
    }

    fun prehook(handler: HttpServerRequestHandler) {
        prehooks.add(handler)
    }

    fun static(path: String, vfs: VfsFile) {
        handler(path, HttpServerRequestHandlerStatic(vfs))
    }
}

fun HttpServerRequestHandlerStatic(vfs: VfsFile): HttpServerRequestHandler {
    val jailedVfs = vfs.jail()
    return { request ->
        val file = jailedVfs[request.path]
        if (!file.exists()) {
            request.setStatus(404)
            request.end()
        } else {
            request.setStatus(200)
            //println(request.path)
            //println(request.headers)
            val range = request.headers.getFirst("range")
            val length = file.size()
            val lengthM1 = length - 1
            if (range != null) {
                val parts = range.removePrefix("bytes=").split('-')
                val startReq = parts.getOrNull(0)?.toLong() ?: 0L
                val endReq = parts.getOrNull(1)?.toLong() ?: lengthM1
                val start = startReq.clamp(0L, lengthM1)
                val end = endReq.clamp(start, lengthM1)

                request.addHeader("Content-Range", "bytes $start-$end/$length")
                request.addHeader("Content-Length", "${end - start + 1}")
                request.end(file, start..end)
            } else {
                request.addHeader("Content-Length", "$length")
                when (request.method) {
                    Http.Method.HEAD -> request.end()
                    else -> request.end(file)
                }
            }
        }
        //request.end("MISSING STATIC HANDLER")
    }
}
