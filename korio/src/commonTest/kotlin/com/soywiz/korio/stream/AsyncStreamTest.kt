package com.soywiz.korio.stream

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlin.test.*

class AsyncStreamTest {
	@Test
	fun name() = suspendTest {
		val mem = FillSyncStream(0).toAsync()
		println(mem.readU8())
	}

	@Test
	fun name2() = suspendTest {
		val data = "HELLO WORLD\u0000TEST".toByteArray()
		assertEquals("HELLO WORLD", data.openAsync().readStringz())
	}

	@Test
	fun name3() = suspendTest {
		val bytes = "HELLO WORLD\u0000TEST".toByteArray()
		val data = bytes.openAsync()
		data.position = 1000
		assertEquals(listOf(), data.readBytesUpTo(20).toList())
		data.position = bytes.size.toLong()
		assertEquals(listOf(), data.readBytesUpTo(20).toList())
		data.position = bytes.size.toLong() - 1
		assertEquals(listOf('T'.toByte()), data.readBytesUpTo(20).toList())
	}

	@Test
	fun name4() = suspendTest {
		assertTrue(byteArrayOf(1, 2, 3).openSync().toAsync().base is MemoryAsyncStreamBase)
		assertTrue(byteArrayOf(1, 2, 3).openAsync().base is MemoryAsyncStreamBase)
	}

	//@Test
	//fun closeRefCount() = suspendTest {
	//	val log = arrayListOf<String>()
//
	//	val stream = object : AsyncStreamBase() {
	//		override suspend fun getLength(): Long = 20L
//
	//		override suspend fun close() {
	//			log += "close"
	//		}
	//	}.toAsyncStream()
//
	//	val a = stream.slice(0 until 10)
	//	val b = stream.slice(0 until 10)
//
	//	assertEquals(listOf<String>(), log)
	//	a.close()
	//	assertEquals(listOf<String>(), log)
	//	b.close()
	//	assertEquals(listOf("close"), log)
	//}
}