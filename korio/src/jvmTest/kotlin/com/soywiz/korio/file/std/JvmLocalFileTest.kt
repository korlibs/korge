package com.soywiz.korio.file.std

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.baseName
import kotlinx.coroutines.channels.toList
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class JvmLocalFileTest {
	@Test
	fun test() = suspendTest {
		val tmpVfs = File(System.getProperty("java.io.tmpdir")).toVfs().jail()
		tmpVfs["korio-test"].mkdir()
		tmpVfs["korio-test"]["demo.txt"].writeString("HELLO")
		assertEquals(listOf("demo.txt"), tmpVfs["korio-test"].listRecursive().toList().map { it.baseName })
	}
}