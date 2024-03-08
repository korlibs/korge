package korlibs.io.net.ws

import korlibs.io.async.suspendTestNoJs
import korlibs.io.lang.toByteArray
import korlibs.io.net.FakeAsyncClient
import korlibs.io.net.URL
import korlibs.io.net.http.Http
import korlibs.io.stream.MemorySyncStreamToByteArray
import korlibs.io.stream.openSync
import korlibs.io.stream.readString
import korlibs.io.stream.readU16BE
import korlibs.io.stream.toAsync
import korlibs.io.stream.write16BE
import korlibs.io.stream.writeBytes
import korlibs.io.stream.writeString
import korlibs.encoding.hex
import kotlin.test.*

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
                "Sec-WebSocket-Key: bXl3c2tleTEyMzQ1YWRmZw==\r\n" +
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
                "Sec-WebSocket-Key: bXl3c2tleTEyMzQ1YWRmZw==\r\n" +
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
        ws.onError { it.printStackTrace(); log += "error" }
        ws.onStringMessage { log += "'$it'" }
        ws.internalReadPackets()
        ws.close()
        val frame = RawSocketWebSocketClient.readWsFrameOrNull(client.clientToServer.toAsync())
        val message = frame!!.data.openSync()
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
        ws.onError { it.printStackTrace(); log += "error" }
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
        ws.onError { it.printStackTrace(); log += "error" }
        ws.onBinaryMessage { log += "#${it.hex}#" }
        ws.onStringMessage { log += "'$it'" }
        ws.onAnyMessage.add { log += "[$it]" }
        ws.internalReadPackets()
        ws.close()
        assertEquals("open,close[1002,testing!]", log.joinToString(","))
    }

    @Test
    fun testWrongSizeWSKey() = suspendTestNoJs {
        assertFailsWith<IllegalStateException> {
            RawSocketWebSocketClient("ws://127.0.0.1/test", wskey = "...", connect = false)
        }
    }
}
