package com.soywiz.korio.net.ws

import com.soywiz.klock.seconds
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.async.waitOne
import com.soywiz.korio.net.http.createHttpServer
import kotlinx.coroutines.cancel
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class WebSocketServerTest {
    @Test
    @Ignore // @TODO: Somehow this runs forever / until timeout?
    fun test() = suspendTest {
        val server = createHttpServer()
        val log = arrayListOf<String>()
        val onCloseReceived = Signal<Unit>()
        server.websocketHandler { req ->
            //println(req.headers)
            req.onBinaryMessage {
                log += "server:bytes(${it.size})"
                println("$log")
            }
            req.onStringMessage {
                log += "server:$it"
                req.send("Received $it")
                println("$log")
            }
            req.onClose {
                log += "server:$it"
                println("$log")
                onCloseReceived(Unit)
            }
        }
        server.listen()

        val client = WebSocketClient("ws://127.0.0.1:${server.actualPort}/demo")
        client.send("HELLO WORLD!")
        client.close()

        println("[1]")
        onCloseReceived.waitOne()
        println("[2]")

        server.close()

        //println("log.joinToString(\",\")=${log.joinToString(",")}")

        assertEquals(
            "server:HELLO WORLD!,server:WsCloseInfo(code=1000, reason=Normal close)",
            log.joinToString(",")
        )

        //cancel()
    }
}
