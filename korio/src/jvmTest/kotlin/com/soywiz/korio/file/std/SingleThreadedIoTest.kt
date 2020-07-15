package com.soywiz.korio.file.std

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

class SingleThreadedIoTest {
	@Test
	fun test() = suspendTest {
		val thread1 = currentThreadId
		localCurrentDirVfs["temp.txt"].writeString("temp")
		val thread2 = currentThreadId
		assertEquals(thread1, thread2)
		localCurrentDirVfs["temp.txt"].delete()
	}
}