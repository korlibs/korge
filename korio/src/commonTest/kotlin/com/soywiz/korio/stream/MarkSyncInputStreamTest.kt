package com.soywiz.korio.stream

import kotlin.test.*

class MarkSyncInputStreamTest {
    @Test
    fun testMarkReset() {
        val sync = DequeSyncStream()
        sync.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        val syncRead = sync.markable()
        syncRead.mark(4)
        assertEquals(1, syncRead.read())
        assertEquals(2, syncRead.read())
        assertEquals(3, syncRead.read())
        assertEquals(4, syncRead.read())
        syncRead.reset()
        assertEquals(1, syncRead.read())
        assertEquals(2, syncRead.read())
        assertEquals(3, syncRead.read())
        assertEquals(4, syncRead.read())
        assertEquals(5, syncRead.read())
        assertEquals(6, syncRead.read())
    }

    @Test
    fun testMarkReset2() {
        val sync = DequeSyncStream()
        sync.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        val syncRead = sync.markable()
        syncRead.mark(4)
        assertEquals(listOf(1, 2, 3, 4), syncRead.readBytes(4).map { it.toInt() })
        syncRead.reset()
        assertEquals(listOf(1, 2, 3, 4), syncRead.readBytes(6).map { it.toInt() })
        assertEquals(listOf(5, 6), syncRead.readBytes(6).map { it.toInt() })
    }

    @Test
    fun testMarkReset3() {
        val sync = DequeSyncStream()
        sync.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        val syncRead = sync.markable()
        syncRead.mark(4)
        assertEquals(listOf(1, 2, 3, 4), syncRead.readBytes(4).map { it.toInt() })
        syncRead.reset()
        assertEquals(listOf(1, 2, 3, 4, 5, 6), syncRead.readBytesExact(6).map { it.toInt() })
    }
}
