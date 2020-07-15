package com.soywiz.korte.vertx

import com.soywiz.korte.*
import com.soywiz.korte.vertx.internal.*
import io.vertx.core.*
import io.vertx.core.http.*
import io.vertx.ext.web.*
import io.vertx.ext.web.handler.*
import org.junit.Test
import kotlin.coroutines.*
import kotlin.test.*

class KorteVertxTemplateEngineTest {
    @Test
    fun test() = runBlocking {
        val port = 0
        val host = "127.0.0.1"
        val vertx = Vertx.vertx()
        val router = Router.router(vertx)
        val template = TemplateHandler.create(
            KorteVertxTemplateEngine(
                coroutineContext, Templates(
                    TemplateProvider(
                        "index.html" to "hello world {{ 1 + 2 }}!",
                        "hello.html" to "Nice :)!"
                    )
                )
            )
        )

        router.get("/*").handler(template)

        val server: HttpServer = run {
            val server = vertx.createHttpServer()
            server.requestHandler(router)
            vx { server.listen(port, host, it) }
        }
        val actualPort = server.actualPort()

        try {
            val client = vertx.createHttpClient()
            assertEquals("hello world 3!", client.get(actualPort, "127.0.0.1", "/").readString())
            assertEquals("Nice :)!", client.get(actualPort, "127.0.0.1", "/hello").readString())
        } finally {
            server.close()
        }
    }
}
