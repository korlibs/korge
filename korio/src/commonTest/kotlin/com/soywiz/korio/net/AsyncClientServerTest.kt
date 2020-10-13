package com.soywiz.korio.net

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*
import kotlin.test.*

@ExperimentalCoroutinesApi
class AsyncClientServerTest {
	companion object {
		val UUIDLength = 36
	}

    @Test
	fun testClientServer() = suspendTest({ OS.isJvm || OS.isNativeDesktop }) {
        val server = AsyncServer(port = 0)
        //val server = AsyncServer(port = 29_999)

        //val clientsCount = 2000
        val clientsCount = 10
        var counter = 0
        val correctEchoes = Deque<Boolean>()

        println("Server listening at... ${server.port} (requested ${server.requestPort})")

        val listenJob = launchImmediately {
            server.listenFlow().take(clientsCount).collect { client ->
                println("Client connected to server")
                val msg = client.readString(UUIDLength)
                client.writeString(msg)
                counter++
            }
        }

        val clients = (0 until clientsCount).map { clientId ->
            asyncImmediately {
                try {
                    println("Connected[$clientId]...")
                    val client = AsyncClient.createAndConnect("127.0.0.1", server.port)
                    val msg = UUID.randomUUID().toString()
                    client.writeString(msg)
                    println("Written[$clientId]")
                    val echo = client.readString(UUIDLength)
                    println("Read[$clientId]: $echo")

                    correctEchoes.add(msg == echo)
                    println("Completed[$clientId]")
                    client.close()
                } catch (e: Throwable) {
                    println("Failed[$clientId]")
                    e.printStackTrace()
                }
            }
        }

        clients.awaitAll()
        println("Clients completed")
        listenJob.join()

        println("[a]")
        assertEquals(clientsCount, counter)
        assertEquals(clientsCount, correctEchoes.size)
        assertTrue(correctEchoes.all { it })
        println("[c]")
        server.close()
	}
}
