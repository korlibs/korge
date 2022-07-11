package com.soywiz.korio.file.std

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.lang.tempPath
import kotlin.test.Test
import kotlin.test.assertEquals

class AsynchronousFileChannelVfsTest {
    @Test
    fun test() = suspendTest {
        val vfs = AsynchronousFileChannelVfs()
        val file = vfs["${Environment.tempPath}/AsynchronousFileChannelVfsTest.test.txt"]
        file.writeString("HELLO")
        assertEquals("HELLO", file.readString())
    }
}
