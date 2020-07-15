package com.soywiz.korio.net.ws

import com.soywiz.korio.async.suspendTestNoJs
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.net.FakeAsyncClient
import com.soywiz.korio.net.URL
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.encoding.hex
import kotlin.test.Test
import kotlin.test.assertEquals

class RawRawSocketWebSocketClient {
    @Test
    fun test() = suspendTestNoJs {
        assertEquals(
            "GET / HTTP/1.1",
            (RawSocketWebSocketClient(
                "ws://127.0.0.1:8081/",
                connect = false
            ) as RawSocketWebSocketClient).buildHeader().split("\r\n").first()
        )
    }

    @Test
    fun test2() = suspendTestNoJs {
        val ws =
            RawSocketWebSocketClient("ws://127.0.0.1:8081/path?id=someId", connect = false) as RawSocketWebSocketClient
        assertEquals(
            "GET /path?id=someId HTTP/1.1\r\n" +
                "Host: 127.0.0.1:8081\r\n" +
                "Pragma: no-cache\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Upgrade: websocket\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: d3NrZXk=\r\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36\r\n" +
                "\r\n",
            ws.buildHeader()
        )
    }

    @Test
    fun test3() = suspendTestNoJs {
        val ws =
            RawSocketWebSocketClient("ws://127.0.0.1:8081/path?id=someId", connect = false, headers = Http.Headers.build {
                put("user-agent", "myagent!")
            }) as RawSocketWebSocketClient
        assertEquals(
            "GET /path?id=someId HTTP/1.1\r\n" +
                "Host: 127.0.0.1:8081\r\n" +
                "Pragma: no-cache\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Upgrade: websocket\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: d3NrZXk=\r\n" +
                "user-agent: myagent!\r\n" +
                "\r\n",
            ws.buildHeader()
        )
    }

    @Test
    fun testClose() = suspendTestNoJs {
        val log = arrayListOf<String>()
        val client = FakeAsyncClient()
        client.serverToClient.writeBytes(WsFrame("hello".toByteArray(), WsOpcode.Text, masked = false).toByteArray())
        val ws = RawSocketWebSocketClient(coroutineContext, client, URL("ws://127.0.0.1:8081/"))
        ws.onOpen { log += "open" }
        ws.onClose { log += "close" }
        ws.onStringMessage { log += "'$it'" }
        ws.internalReadPackets()
        ws.close()
        val frame = RawSocketWebSocketClient.readWsFrame(client.clientToServer.toAsync())
        val message = frame.data.openSync()
        assertEquals("open,'hello',close", log.joinToString(","))
        assertEquals(1000, message.readU16BE())
        assertEquals("OK", message.readString(2))
    }

    @Test
    fun testContinuation() = suspendTestNoJs {
        val log = arrayListOf<String>()
        val client = FakeAsyncClient()
        val a = "あ"
        val aData = a.toByteArray()
        client.serverToClient.writeBytes(WsFrame(aData.sliceArray(0..0), WsOpcode.Text, isFinal = false, masked = false).toByteArray())
        client.serverToClient.writeBytes(WsFrame(aData.sliceArray(1 until aData.size), WsOpcode.Continuation, isFinal = true, masked = false).toByteArray())
        val ws = RawSocketWebSocketClient(coroutineContext, client, URL("ws://127.0.0.1:8081/"))
        ws.onOpen { log += "open" }
        ws.onClose { log += "close" }
        ws.onBinaryMessage { log += "#${it.hex}#" }
        ws.onStringMessage { log += "'$it'" }
        ws.onAnyMessage.add { log += "[$it]" }
        ws.internalReadPackets()
        ws.close()
        assertEquals("open,'あ',[あ],close", log.joinToString(","))
    }

    @Test
    fun testServerCloseResponse() = suspendTestNoJs {
        val log = arrayListOf<String>()
        val client = FakeAsyncClient()
        client.serverToClient.writeBytes(WsFrame(MemorySyncStreamToByteArray {
            write16BE(WebSocketClient.CloseReasons.PROTOCOL_ERROR)
            writeString("testing!")
        }, WsOpcode.Close, isFinal = true, masked = false).toByteArray())
        val ws = RawSocketWebSocketClient(coroutineContext, client, URL("ws://127.0.0.1:8081/"))
        ws.onOpen { log += "open" }
        ws.onClose { log += "close[${it.code},${it.message}]" }
        ws.onBinaryMessage { log += "#${it.hex}#" }
        ws.onStringMessage { log += "'$it'" }
        ws.onAnyMessage.add { log += "[$it]" }
        ws.internalReadPackets()
        ws.close()
        assertEquals("open,close[1002,testing!]", log.joinToString(","))
    }
}
